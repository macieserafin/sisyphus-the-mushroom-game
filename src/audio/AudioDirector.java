package audio;
import main.GamePanel;
import main.GameState;
import entity.enemy.Enemy_BYT;
import entity.enemy.Enemy_SlimeKing;
import java.util.Set;

/** Resolves scenes; the independent PCM mixer owns playback and all envelopes. */
public final class AudioDirector implements AutoCloseable {
    public static final int SILENCE = -2;
    public static final String[] TRACKS = {"title", "void", "tutorial", "byt_dialogue", "byt_battle",
            "lobby", "interior", "sage", "forest", "forest_combat", "anchor", "king_1", "king_2",
            "king_3", "victory", "rebirth", "descent", "vn_street", "vn_house", "vn_room", "vn_stairs",
            "vn_basement", "vn_mushrooms", "vn_closeup", "vn_tunnel", "vn_forest"};
    private final GamePanel gp;
    private final PcmMixer mixer;
    private int preview=-1, sceneTrack=-1, current=-1;
    private double scanSeconds;
    private float waterNear,fireNear;
    private boolean closed;
    private long lastUpdate;
    public AudioDirector(GamePanel gp){this.gp=gp;mixer=new PcmMixer(new Sound(),java.awt.GraphicsEnvironment.isHeadless());}
    public PcmMixer mixer(){return mixer;}
    public void start(){
        mixer.prepare(Sound.SCORE_START);
        for(int id=0;id<=Sound.REWARD;id++)if(id!=Sound.BACKGROUND&&id!=Sound.CUTSCENE_MUSIC&&id!=Sound.BOSS_MUSIC)mixer.prepare(id);
        for(int i=0;i<TRACKS.length;i++)if(!isChapterZeroTrack(i))mixer.prepare(Sound.SCORE_START+i);
        for(int i=0;i<3;i++)mixer.prepare(Sound.AMBIENT_START+i);
        mixer.start();
    }
    public int contextTrack() {
        if (gp.gameState == GameState.MENU) return 0;
        String map = gp.tileM.currentMapPath();
        if (gp.levelManager.getActiveBoss() instanceof Enemy_BYT
                || map != null && (map.contains("prologue") || map.contains("byt_arena"))
                || gp.gameState == GameState.CUTSCENE && !gp.storyFlags.isPrologueBytEncounterCompleted())
            return SILENCE;
        if (gp.narrative != null && gp.narrative.musicHeld()) return SILENCE;
        if (gp.gameState == GameState.DEAD || gp.campaign.rebirthing()) return 15;
        if (gp.gameState == GameState.CUTSCENE) {
            return 5;
        }
        if (gp.levelManager.getActiveBoss() instanceof Enemy_SlimeKing king) return 10 + king.audioPhase();
        if (map == null) return 0;
        if (gp.campaign.inArena() && gp.campaign.bossCleared()) return 14;
        if (map.contains("interior_sage")) return 7;
        if (map.contains("interior")) return 6;
        if (map.contains("village")) return 5;
        if (gp.campaign.inForest()) {
            if (gp.campaign.inCombat()) return 9;
            if (gp.campaign.anchorOpen() && gp.player.worldX / gp.tileSize > 140
                    && gp.player.worldY / gp.tileSize < 15) return 10;
            return 8;
        }
        return 16;
    }


    public static boolean isChapterZeroTrack(int track){return track>=1&&track<=4||track>=17;}
    public void preview(int track){int value=Math.floorMod(track,TRACKS.length);if(!isChapterZeroTrack(value))preview=value;}
    public void automatic(){preview=-1;}
    public int currentTrack(){return current;}
    public boolean isTransitioning(){return mixer.transitioning();}
    public String status(){int playing=mixer.playing();return (current==SILENCE?"Cisza — rozdział 0":current<0?"—":TRACKS[current])+(current>=0&&playing!=Sound.SCORE_START+current?" (przygotowanie)":"")+(preview>=0?" (odsłuch)":"");}
    public String diagnostics(){return mixer.diagnostics();}
    public String device(){return mixer.device();}
    public void update(){long now=System.nanoTime();double dt=lastUpdate==0?1.0/60:(now-lastUpdate)/1e9;lastUpdate=now;update(dt);}
    public void update(double seconds){
        if(closed)return;
        double dt=Math.max(0,Math.min(1,seconds));
        if(gp.gameState!=GameState.MOD_MENU)preview=-1;
        boolean overlay=gp.gameState==GameState.PAUSED||gp.gameState==GameState.MOD_MENU||gp.gameState==GameState.SHOP||gp.gameState==GameState.QUESTS||gp.gameState==GameState.SKILLS||gp.gameState==GameState.MAP||gp.gameState==GameState.CHOICE;
        if(!overlay||sceneTrack<0)sceneTrack=contextTrack();
        int wanted=preview>=0?preview:sceneTrack;
        if(wanted!=current){
            current=wanted;
            double fade=wanted==SILENCE?.012:wanted>=11&&wanted<=13?.2:wanted==9?.2:.55;
            mixer.music(wanted<0?SILENCE:Sound.SCORE_START+wanted,fade);
        }
        mixer.volumes(gp.audio.musicGain(),gp.audio.effectsGain(),overlay?.5f:gp.dialogueManager.hasActiveLine()?.72f:1);
        updateAmbience(overlay,dt);
        if(java.awt.GraphicsEnvironment.isHeadless())mixer.render(new byte[Math.max(4,(int)(dt*PcmMixer.RATE)*4)]);
    }
    private void updateAmbience(boolean overlay, double dt) {

        String map = gp.tileM.currentMapPath();
        boolean outside = map != null && (map.contains("village") || gp.campaign.inForest());
        boolean audible = outside && !overlay && gp.gameState != GameState.CUTSCENE && gp.gameState != GameState.DEAD;
        if ((scanSeconds -= dt) <= 0) {
            scanSeconds = .1;
            waterNear = fireNear = 0;
            if (audible) {
                int px = gp.player.worldX / gp.tileSize, py = gp.player.worldY / gp.tileSize;
                for (int c=Math.max(0,px-7);c<Math.min(gp.tileM.getWorldCols(),px+8);c++)
                    for(int r=Math.max(0,py-7);r<Math.min(gp.tileM.getWorldRows(),py+8);r++) {
                        float near = (float)Math.max(0, 1-Math.hypot(c-px,r-py)/8);
                        String ground=gp.tileM.tile[gp.tileM.mapTileNum[c][r]].name;
                        if (Set.of("Water","Deep water","Lily pad").contains(ground)) waterNear=Math.max(waterNear,near);
                        int obj=gp.tileM.objectTileNum[c][r];
                        if(obj>=0 && Set.of("Campfire","Cave wall (torch)","Void brazier").contains(gp.tileM.tile[obj].name)) fireNear=Math.max(fireNear,near);
                    }
            }
        }

        mixer.ambient(Sound.AMBIENT_START,audible?.22f:0);
        mixer.ambient(Sound.AMBIENT_START+1,audible?waterNear*.42f:0);
        mixer.ambient(Sound.AMBIENT_START+2,audible?fireNear*.36f:0);
    }

    public void text(char character,String speaker){if(gp.audio.textSounds()&&Character.isLetterOrDigit(character)){int variant="BYT".equalsIgnoreCase(speaker)?3:Math.floorMod(speaker.hashCode(),3);gp.soundEffects().playEffect(Sound.TEXT_SOFT+variant);}}
    public void dialogueEffect(int id){gp.soundEffects().playEffect(id);}
    @Override public void close(){closed=true;mixer.close();}
}
