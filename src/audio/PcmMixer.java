package audio;

import javax.sound.sampled.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** One continuously running device, with sample-clock envelopes and immutable decoded assets. */
public final class PcmMixer implements AutoCloseable {
    public static final int RATE = 44100, BLOCK = 256, MAX_EFFECTS = 40;
    private static final long CACHE_LIMIT = 192L * 1024 * 1024;
    private static final AudioFormat FORMAT = new AudioFormat(RATE,16,2,true,false);
    public record Sample(short[] pcm) { public int frames(){return pcm.length/2;} }
    private static final class Voice {
        int id, frame, ramp, priority;
        Sample sample;
        boolean loop, music, ambient;
        float gain, target, step;
        Voice(int id,Sample sample){this.id=id;this.sample=sample;}
        void ramp(float value,double seconds){target=value;ramp=Math.max(1,(int)(seconds*RATE));step=(target-gain)/ramp;}
    }
    private final Sound catalog;
    private final boolean offline;
    private final java.util.function.IntFunction<Sample> offlineSamples;
    private final ExecutorService decoder=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"audio-decoder");t.setDaemon(true);return t;});
    private final ConcurrentHashMap<Integer,CompletableFuture<Sample>> cache=new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Runnable> commands=new ConcurrentLinkedQueue<>();
    private final ArrayList<Voice> voices=new ArrayList<>();
    private final HashMap<Integer,Long> repeats=new HashMap<>();
    private final AtomicLong bytes=new AtomicLong();
    private volatile boolean closed, started;
    private volatile SourceDataLine line;
    private Thread worker;
    private volatile int requested=-2, playing=-2, voiceCount, underruns, dropped;
    private volatile long requestNanos, lastStartMicros;
    private volatile String device="not started", failure="";
    private long frames;
    private float musicVolume=1,effectVolume=1,duck=1,duckTarget=1;
    private float musicTarget=1,effectTarget=1,musicStep,effectStep;
    private int volumeRamp;
    private double transition=.5;
    public PcmMixer(Sound catalog,boolean offline){this(catalog,offline,id->new Sample(new short[]{1000,1000,1000,1000}));}
    public PcmMixer(Sound catalog,boolean offline,java.util.function.IntFunction<Sample> samples){this.catalog=catalog;this.offline=offline;this.offlineSamples=samples;}

    public CompletableFuture<Sample> prepare(int id){
        return cache.computeIfAbsent(id,key->{
            if(offline)return CompletableFuture.completedFuture(offlineSamples.apply(key));
            return CompletableFuture.supplyAsync(()->decode(key),decoder);
        });
    }
    private Sample decode(int id){
        URL url=catalog.resource(id);if(url==null){failure="Missing audio resource: "+id;return null;}
        try(var input=AudioSystem.getAudioInputStream(url);var converted=AudioSystem.getAudioInputStream(FORMAT,input)){
            byte[] data=converted.readAllBytes();
            if(bytes.get()+data.length>CACHE_LIMIT){failure="Audio cache budget exceeded";return null;}
            short[] pcm=new short[data.length/2];
            for(int i=0;i<pcm.length;i++)pcm[i]=(short)((data[i*2]&255)|(data[i*2+1]<<8));
            bytes.addAndGet(data.length);return pcm.length>=2?new Sample(pcm):null;
        }catch(Exception e){failure="Decode "+id+": "+e.getMessage();return null;}
    }
    public void start(){
        if(started||closed)return;started=true;if(offline)return;
        worker=new Thread(this::run,"audio-output");worker.setDaemon(true);worker.start();
    }
    private void run(){
        try{
            SourceDataLine output=AudioSystem.getSourceDataLine(FORMAT);line=output;
            output.open(FORMAT,2048*4);device=output.getLineInfo()+" / buffer "+output.getBufferSize()/4+" frames";
            byte[] block=new byte[BLOCK*4];output.start();boolean fed=false;
            while(!closed){
                if(fed&&output.available()==output.getBufferSize())underruns++;
                render(block);
                int written=0;while(!closed&&written<block.length)written+=output.write(block,written,block.length-written);
                fed=true;
            }
        }catch(Exception e){if(!closed)failure="Device: "+e.getMessage();}
        finally{SourceDataLine output=line;if(output!=null){output.stop();output.flush();output.close();}line=null;}
    }
    public void music(int id,double seconds){
        long submitted=System.nanoTime();
        if(id>=0)prepare(id);
        commands.add(()->{
            if(requested==id)return;
            requested=id;requestNanos=submitted;
            transition=seconds;
            // Silence invalidates even a decode that is still pending.
            if(id<0){for(Voice v:voices)if(v.music)v.ramp(0,.012);playing=id;}
        });
    }
    public void volumes(float music,float effects,float attenuation){commands.add(()->{
        float mt=clamp(music),et=clamp(effects);
        if(mt!=musicTarget||et!=effectTarget){
            musicTarget=mt;effectTarget=et;volumeRamp=RATE/200;
            musicStep=(musicTarget-musicVolume)/volumeRamp;effectStep=(effectTarget-effectVolume)/volumeRamp;
        }
        duckTarget=clamp(attenuation);
    });}
    public void ambient(int id,float gain){
        if(gain>0)prepare(id);
        commands.add(()->{
            Voice existing=voices.stream().filter(v->v.ambient&&v.id==id).findFirst().orElse(null);
            Sample sample=ready(id);
            if(existing==null&&gain>0&&sample!=null){existing=new Voice(id,sample);existing.ambient=true;existing.loop=true;voices.add(existing);}
            if(existing!=null)existing.ramp(gain,.25);
        });
    }
    public void effect(int id,float gain){
        long submitted=System.nanoTime();prepare(id);
        commands.add(()->{
            if(System.nanoTime()-submitted>100_000_000L){dropped++;return;}
            Sample sample=ready(id);if(sample==null){dropped++;return;}
            int priority=priority(id);long interval=interval(id);
            if(frames-repeats.getOrDefault(id,-RATE*10L)<interval){dropped++;return;}
            long count=voices.stream().filter(v->!v.loop).count();
            if(count>=MAX_EFFECTS){
                Voice victim=voices.stream().filter(v->!v.loop).min(Comparator.comparingInt(v->v.priority)).orElse(null);
                if(victim==null||victim.priority>=priority){dropped++;return;}
                if(count>=MAX_EFFECTS+4)voices.remove(victim);else victim.ramp(0,.004);
            }
            Voice v=new Voice(id,sample);v.gain=0;v.priority=priority;v.ramp(gain,.001);voices.add(v);repeats.put(id,frames);
        });
    }
    public void clearEffects(){commands.add(()->{for(Voice v:voices)if(!v.loop)v.ramp(0,.005);});}
    private Sample ready(int id){var future=cache.get(id);return future==null?null:future.getNow(null);}
    private void reconcileMusic(){
        int wanted=requested;if(wanted<0||wanted==playing)return;
        Sample sample=ready(wanted);if(sample==null)return;
        Voice incoming=null;
        for(Voice v:voices)if(v.music){if(v.id==wanted)incoming=v;v.ramp(0,transition);}
        if(incoming==null){incoming=new Voice(wanted,sample);incoming.music=true;incoming.loop=true;voices.add(incoming);}
        incoming.ramp(.85f,transition);
        playing=wanted;lastStartMicros=(System.nanoTime()-requestNanos)/1000;
    }
    /** Also used by deterministic tests: the identical mixer with no physical output. */
    public void render(byte[] output){
        Runnable command;while((command=commands.poll())!=null)command.run();
        reconcileMusic();
        for(int frame=0;frame<output.length/4;frame++){
            if(volumeRamp>0){musicVolume+=musicStep;effectVolume+=effectStep;if(--volumeRamp==0){musicVolume=musicTarget;effectVolume=effectTarget;}}
            float left=0,right=0;duck+=(duckTarget-duck)/(.12f*RATE);
            for(int j=voices.size()-1;j>=0;j--){
                Voice v=voices.get(j);
                if(v.ramp>0){v.gain+=v.step;if(--v.ramp==0)v.gain=v.target;}
                if(v.ramp==0&&v.target==0){voices.remove(j);continue;}
                if(v.frame>=v.sample.frames()){if(v.loop)v.frame=0;else{voices.remove(j);continue;}}
                float gain=v.gain*(v.music?musicVolume*duck:effectVolume);
                left+=v.sample.pcm[v.frame*2]/32768f*gain;right+=v.sample.pcm[v.frame*2+1]/32768f*gain;v.frame++;
            }
            // Headroom plus a soft knee protects overlapping transitions and combat peaks.
            int l=encode(left),r=encode(right),i=frame*4;
            output[i]=(byte)l;output[i+1]=(byte)(l>>8);output[i+2]=(byte)r;output[i+3]=(byte)(r>>8);
            frames++;
        }
        voiceCount=voices.size();
        voicesSnapshotTransition=voices.stream().anyMatch(v->v.music&&v.ramp>0);
    }
    private static int encode(float value){float a=Math.abs(value);return Math.round(Math.copySign(a<=.9f?a:.9f+.1f*(float)Math.tanh((a-.9f)*10),value)*32767);}
    private static float clamp(float x){return Math.max(0,Math.min(1,x));}
    private static int priority(int id){
        if(id==Sound.PLAYER_DEATH||id==Sound.BYT_SNAP||id==Sound.CHARGE_WARNING||id==Sound.PLAYER_HURT_1||id==Sound.PLAYER_HURT_2)return 3;
        if(id>=Sound.UI_MOVE&&id<=Sound.UI_ERROR||id==Sound.BARRIER_BREAK||id==Sound.SHIELD_BREAK)return 2;
        return id==Sound.STEP_1||id==Sound.STEP_2||id==Sound.ENEMY_SHOT||id>=Sound.TEXT_SOFT&&id<=Sound.TEXT_BYT?0:1;
    }
    private static long interval(int id){
        if(id>=Sound.TEXT_SOFT&&id<=Sound.TEXT_BYT)return RATE*65/1000;
        if(id==Sound.ENEMY_SHOT)return RATE*35/1000;
        if(id==Sound.STEP_1||id==Sound.STEP_2)return RATE*60/1000;
        return 0;
    }
    public int playing(){return playing;}
    public int requested(){return requested;}
    public boolean transitioning(){return voicesSnapshotTransition;}
    private volatile boolean voicesSnapshotTransition;
    public String diagnostics(){return "Głosy: "+voiceCount+" / start: "+lastStartMicros/1000+" ms / underrun: "+underruns+" / pominięte: "+dropped+" / cache: "+bytes.get()/1048576+" MB";}
    public String device(){return device+(failure.isEmpty()?"":" / "+failure);}
    public long startMicros(){return lastStartMicros;}
    public int voices(){return voiceCount;}
    public CompletableFuture<Void> prepared(){return CompletableFuture.allOf(cache.values().toArray(CompletableFuture[]::new));}
    @Override public void close(){closed=true;decoder.shutdownNow();SourceDataLine output=line;if(output!=null)output.close();if(worker!=null)worker.interrupt();}
}
