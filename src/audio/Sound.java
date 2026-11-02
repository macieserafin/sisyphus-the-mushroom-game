package audio;

import java.net.URL;

public class Sound {
    public static final int BACKGROUND = 0;
    public static final int GUNSHOT = 1;
    public static final int UNLOCK = 2;
    public static final int COIN = 3;
    public static final int POWERUP = 4;
    public static final int SWORD = 5;
    public static final int PLAYER_HAPPY = 6;
    public static final int PLAYER_HURT_1 = 7;
    public static final int PLAYER_HURT_2 = 8;
    public static final int POWERUP_BOOTS = 9;
    public static final int POWERUP_KEY = 10;
    public static final int SLIME_DEATH = 11;
    public static final int PLAYER_DEATH = 12;
    public static final int CUTSCENE_MUSIC = 13;
    public static final int BOSS_MUSIC = 14;
    public static final int BYT_SNAP = 15;
    // effects added later; their files are listed in EFFECT_FILES in this order, starting at UI_MOVE
    public static final int UI_MOVE = 16;
    public static final int UI_CONFIRM = 17;
    public static final int UI_BACK = 18;
    public static final int UI_ERROR = 19;
    public static final int MUSHROOM_1 = 20;
    public static final int MUSHROOM_2 = 21;
    public static final int MUSHROOM_3 = 22;
    public static final int HEART = 23;
    public static final int PURCHASE = 24;
    public static final int BANK = 25;
    public static final int CHEST = 26;
    public static final int SKILL_LEARN = 27;
    public static final int QUEST_START = 28;
    public static final int QUEST_DONE = 29;
    public static final int PORTAL = 30;
    public static final int DOOR = 31;
    public static final int ROLL = 32;
    public static final int DASH = 33;
    public static final int SWAP = 34;
    public static final int SHIELD_BLOCK = 35;
    public static final int SHIELD_BREAK = 36;
    public static final int STEP_1 = 37;
    public static final int STEP_2 = 38;
    public static final int HIT_1 = 39;
    public static final int HIT_2 = 40;
    public static final int HIT_3 = 41;
    public static final int ENEMY_SHOT = 42;
    public static final int BYT_FIRE = 43;
    public static final int SLIME_ALERT = 44;
    public static final int SLIME_LUNGE = 45;
    public static final int KING_LAND = 46;
    public static final int BOSS_SLAM = 47;
    public static final int BOSS_ROAR = 48;
    public static final int BOSS_DEATH = 49;

    public static final int TEXT_SOFT = 50;
    public static final int TEXT_WARM = 51;
    public static final int TEXT_LOW = 52;
    public static final int TEXT_BYT = 53;
    public static final int DIALOGUE_OPEN = 54;
    public static final int DIALOGUE_NEXT = 55;
    public static final int DIALOGUE_CLOSE = 56;
    public static final int NOTICE = 57;
    public static final int PAPER = 58;
    public static final int MINE = 59;
    public static final int SUPPLIES = 60;
    public static final int SHORTCUT = 61;
    public static final int ANCHOR_BIND = 62;
    public static final int REST = 63;
    public static final int PROJECTILE_CUT = 64;
    public static final int PROJECTILE_REFLECT = 65;
    public static final int MARK = 66;
    public static final int DETONATE = 67;
    public static final int SHIELD_RESTORE = 68;
    public static final int SHELL_CHANGE = 69;
    public static final int BARRIER_BREAK = 70;
    public static final int BARRIER_UP = 71;
    public static final int GUARD_STUN = 72;
    public static final int CHARGE_WARNING = 73;
    public static final int WALL_CRASH = 74;
    public static final int PHASE_CHANGE = 75;
    public static final int TRANSITION = 76;
    public static final int AWAKEN = 77;
    public static final int REWARD = 78;
    public static final int SCORE_START = 80, AMBIENT_START = 120;

    private static final String[] EFFECT_FILES = {"ui_move", "ui_confirm", "ui_back", "ui_error", "mushroom_1",
            "mushroom_2", "mushroom_3", "heart", "purchase", "bank", "chest", "skill_learn", "quest_start", "quest_done",
            "portal", "door", "roll", "dash", "swap", "shield_block", "shield_break", "step_1", "step_2", "hit_1",
            "hit_2", "hit_3", "enemy_shot", "byt_fire", "slime_alert", "slime_lunge", "king_land", "boss_slam",
            "boss_roar", "boss_death"};

    private final URL[] soundURL = new URL[128];
    private PcmMixer mixer;
    private boolean ownsMixer;
    private int selected=-1;
    private float volume=1;
    public Sound() {
        soundURL[BACKGROUND] = getClass().getResource("/sounds/music_forest.wav");
        soundURL[GUNSHOT] = getClass().getResource("/sounds/gunshot.wav");
        soundURL[UNLOCK] = getClass().getResource("/sounds/unlock.wav");
        soundURL[COIN] = getClass().getResource("/sounds/coin.wav");
        soundURL[POWERUP] = getClass().getResource("/sounds/powerup.wav");
        soundURL[SWORD] = getClass().getResource("/sounds/sword.wav");
        soundURL[PLAYER_HAPPY] = getClass().getResource("/sounds/playerhappy.wav");
        soundURL[PLAYER_HURT_1] = getClass().getResource("/sounds/playerhurt1.wav");
        soundURL[PLAYER_HURT_2] = getClass().getResource("/sounds/playerhurt2.wav");
        soundURL[POWERUP_BOOTS] = getClass().getResource("/sounds/powerup2.wav");
        soundURL[POWERUP_KEY] = getClass().getResource("/sounds/powerup1.wav");
        soundURL[SLIME_DEATH] = getClass().getResource("/sounds/slimedeath.wav");
        soundURL[PLAYER_DEATH] = getClass().getResource("/sounds/playerdeath.wav");
        soundURL[CUTSCENE_MUSIC] = getClass().getResource("/sounds/music_void.wav");
        soundURL[BOSS_MUSIC] = getClass().getResource("/sounds/music_king_1.wav");
        String[] additions = {"text_soft","text_warm","text_low","text_byt","dialogue_open","dialogue_next","dialogue_close","notice","paper","mine","supplies","shortcut","anchor_bind","rest","projectile_cut","projectile_reflect","mark","detonate","shield_restore","shell_change","barrier_break","barrier_up","guard_stun","charge_warning","wall_crash","phase_change","transition","awaken","reward"};
        for(int n=0;n<additions.length;n++) soundURL[50+n]=getClass().getResource("/sounds/"+additions[n]+".wav");
        for(int n=0;n<AudioDirector.TRACKS.length;n++) soundURL[SCORE_START+n]=getClass().getResource("/sounds/music_"+AudioDirector.TRACKS[n]+".wav");
        String[] ambience={"wind","water","fire"};
        for(int n=0;n<ambience.length;n++) soundURL[AMBIENT_START+n]=getClass().getResource("/sounds/ambient_"+ambience[n]+".wav");
        soundURL[BYT_SNAP] = getClass().getResource("/sounds/byt_snap.wav");
        for (int n = 0; n < EFFECT_FILES.length; n++) {
            soundURL[UI_MOVE + n] = getClass().getResource("/sounds/" + EFFECT_FILES[n] + ".wav");
        }

    }


    public URL resource(int id){return id>=0&&id<soundURL.length?soundURL[id]:null;}
    public boolean hasResource(int id){return resource(id)!=null;}
    public String effectName(int id){URL url=resource(id);if(url==null)return Integer.toString(id);String p=url.getPath();return p.substring(p.lastIndexOf('/')+1).replace(".wav","").replace('_',' ');}
    public void bind(PcmMixer mixer){this.mixer=mixer;}
    private PcmMixer mixer(){if(mixer==null){mixer=new PcmMixer(this,java.awt.GraphicsEnvironment.isHeadless());ownsMixer=true;mixer.start();}return mixer;}
    public void setFile(int id){selected=id;mixer().prepare(id);}
    public void play(){if(selected>=0)mixer().effect(selected,volume);}
    public void loop(){if(selected>=0)mixer().music(selected,.02);}
    public void stop(){if(mixer!=null)mixer.music(-2,.01);}
    public void playEffect(int id){mixer().effect(id,volume);}
    public void setVolume(float value){volume=Math.max(0,Math.min(1,value));}
    public void stopAllEffects(){if(mixer!=null)mixer.clearEffects();}
    public boolean hasLoadedClip(){return selected>=0&&hasResource(selected);}
    public double playbackSeconds(){return 0;}
    public void close(){if(ownsMixer&&mixer!=null)mixer.close();}
}
