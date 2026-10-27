package story;

import dialogue.DialogueSpeaker;
import entity.npc.NPC;
import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

public final class StorySequences {
    private static final String PLAYER_NEUTRAL_SPLASH = "splash/player_neutral.png";
    private static final String PLAYER_CONFUSED_SPLASH = "splash/player_confused.png";
    private static final String PLAYER_SCARED_SPLASH = "splash/player_scared.png";
    private static final String FRIEND_NEUTRAL_SPLASH = "splash/friend_neutral.png";
    private static final String FRIEND_EXCITED_SPLASH = "splash/friend_excited.png";
    private static final String FRIEND_NERVOUS_SPLASH = "splash/friend_nervous.png";
    private static final String BYT_SHADOW_SPLASH = "splash/BYT_shadow.png";
    private static final String VN_STREET_EVENING = "backgrounds/vn_street_evening.png";
    private static final String VN_FRIEND_HOUSE = "backgrounds/vn_friend_house.png";
    private static final String VN_FRIEND_ROOM = "backgrounds/vn_friend_room.png";
    private static final String VN_BASEMENT_STAIRS = "backgrounds/vn_basement_stairs.png";
    private static final String VN_BASEMENT_NORMAL = "backgrounds/vn_basement_normal.png";
    private static final String VN_BASEMENT_MUSHROOMS = "backgrounds/vn_basement_mushrooms.png";
    private static final String VN_MUSHROOM_CLOSEUP = "backgrounds/vn_mushroom_closeup.png";
    private static final String VN_FUNGAL_TUNNEL = "backgrounds/vn_fungal_tunnel.png";
    private static final String VN_MUSHROOM_FOREST = "backgrounds/vn_mushroom_forest.png";

    private StorySequences() {
    }

    public static StorySequence openingIntro(GamePanel gp) {
        Color playerAccent = new Color(110, 210, 255);
        Color friendAccent = new Color(255, 185, 85);
        Color bytAccent = new Color(190, 80, 230);

        DialogueSpeaker player = createSpeaker(
                "Player",
                PLAYER_NEUTRAL_SPLASH,
                playerAccent
        );
        DialogueSpeaker playerConfused = createSpeaker(
                "Player",
                PLAYER_CONFUSED_SPLASH,
                playerAccent
        );
        DialogueSpeaker playerScared = createSpeaker(
                "Player",
                PLAYER_SCARED_SPLASH,
                playerAccent
        );
        DialogueSpeaker friend = createSpeaker(
                "Homie",
                FRIEND_NEUTRAL_SPLASH,
                friendAccent
        );
        DialogueSpeaker friendExcited = createSpeaker(
                "Homie",
                FRIEND_EXCITED_SPLASH,
                friendAccent
        );
        DialogueSpeaker friendNervous = createSpeaker(
                "Homie",
                FRIEND_NERVOUS_SPLASH,
                friendAccent
        );
        DialogueSpeaker byt = createSpeaker(
                "BYT",
                BYT_SHADOW_SPLASH,
                bytAccent
        );

        return StorySequence.of(
                "opening_intro",
                StorySequenceStep.visualNovelDialogueWithCastAndBackground(
                        friend,
                        Arrays.asList(player, friend),
                        VN_STREET_EVENING,
                        "You straight?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "Yeah."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "You been real quiet."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "I'm high."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "We both high. I'm still talkin'."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "That's 'cause you never stop."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "Somebody gotta carry the conversation."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "You ain't carryin' shit. You just makin' noise."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "See? There he is."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "What time is it?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "Early."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "That ain't a time."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "You got somewhere to be?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "Nah."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "Then come back to the crib."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "For what?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "I wanna show you somethin'."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "What kinda somethin'?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "Just come see."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "Every bad idea you ever had started like that."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "And you still came."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "Yeah. That's my bad."
                ),
                StorySequenceStep.visualNovelDialogueWithBackground(
                        player,
                        VN_FRIEND_HOUSE,
                        "Your place still smell weird."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "That's dinner, hater."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "Dinner lost the fight."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "Man, shut your bitch ass."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "Where your people at?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "Out."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "So it's just you?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "And you."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "That's never comforting."
                ),
                StorySequenceStep.visualNovelDialogueWithBackground(
                        player,
                        VN_FRIEND_ROOM,
                        "Where we goin'?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "Downstairs."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "Downstairs where?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "Basement."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "You got a basement?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "Bro, yes."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "Since when?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "Since the house was built."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "You know what I mean."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "You just never had a reason to go down there."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "And now I do?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "Yeah."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "That's not makin' me feel better."
                ),
                StorySequenceStep.visualNovelDialogueWithBackground(
                        playerConfused,
                        VN_BASEMENT_STAIRS,
                        "Why it smell like that?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "Like what?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Wet."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "It's a basement."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Nah. Basement smell dusty. This smell... undesirable."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "You draggin' it."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "These stairs damp?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "Old house."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Your whole house old. This different."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "You always get weird when you high?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Only when the floor feel sick."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friend,
                        "The floor don't feel sick."
                ),
                StorySequenceStep.visualNovelDialogue(
                        player,
                        "Man."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "Aight... maybe it's a little off."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "A little?"
                ),
                StorySequenceStep.visualNovelDialogueWithBackground(
                        friendNervous,
                        VN_BASEMENT_NORMAL,
                        "Just keep comin'."
                ),
                StorySequenceStep.visualNovelDialogueWithBackground(
                        playerScared,
                        VN_BASEMENT_MUSHROOMS,
                        "...What the hell is that?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "There it go."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Don't \"there it go\" me. What is that?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "I don't know."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "You brought me down here for some shit you don't know?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "I brought you 'cause I knew you'd see it too."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "See what? That your basement got a disease?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "Look at it."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "I am lookin' at it. I don't like it."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "It wasn't here before."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Before when?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "Like... a few days ago."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "A few days?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "It started small."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Mold start small. This look like it's thinkin'."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "You see that glow, right?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Yeah."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "I knew I wasn't trippin'."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "You still might be."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "Man, for real."
                ),
                StorySequenceStep.visualNovelDialogueWithBackground(
                        friendNervous,
                        VN_MUSHROOM_CLOSEUP,
                        "I took a little piece earlier."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "You what?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "Just a tiny bit."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Man, you dumb as hell."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "Nothin' happened."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Yet."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "I'm good."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "You in a wet basement talkin' to glowin' fungus."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "And I'm still standin'."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "That is not proof of anything."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "C'mon. One little piece."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "No."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "You don't even know what it do."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Exactly."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "You scared?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Man, shut up."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "There it is."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Don't do that."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "We split it. Same rules. If it's bad, it's bad for both of us."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "That's not how trust work."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "You in or not?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "...",
                        "Tiny piece."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendExcited,
                        "Knew it."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "If my heart stop, I'm killin' you."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "Fair."
                ),
                StorySequenceStep.visualNovelDialogueWithBackground(
                        playerScared,
                        VN_FUNGAL_TUNNEL,
                        "...",
                        "Yo."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "You feel that?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Why you askin' me like this normal?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "The room weird."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Weird?",
                        "Bro, the room movin'."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "I know."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "The walls was not that far a second ago."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "I know."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Stop sayin' that and do somethin'."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "Like what?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "I don't know. Turn a light on. Turn a light off. Somethin'."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "The lights already on."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "...",
                        "Why it so quiet?",
                        "I can't hear nothin'."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "Me neither."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Nah. Nah, nah."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "Look at me."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "I am lookin' at you."
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "Stay with me."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Why your face-",
                        "...",
                        "Why your face still movin' when you ain't talkin'?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        friendNervous,
                        "What?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Man, what the fuck-",
                        "Hold on-",
                        "I can't-",
                        "..."
                ),
                StorySequenceStep.visualNovelDialogueWithCastAndBackground(
                        playerScared,
                        Collections.singletonList(playerScared),
                        VN_MUSHROOM_FOREST,
                        "Ah, shit.",
                        "...",
                        "This ain't his house.",
                        "This ain't the basement.",
                        "Where the fuck am I?",
                        "Yo!",
                        "Ay!",
                        "Where you at?!",
                        "Answer me!",
                        "...",
                        "Man...",
                        "This gotta be a dream.",
                        "Or I'm dead.",
                        "Or I'm still high.",
                        "...Fuck."
                ),
                StorySequenceStep.visualNovelDialogueWithCastAndBackground(
                        byt,
                        Arrays.asList(playerScared, byt),
                        VN_MUSHROOM_FOREST,
                        "Still deciding?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "...",
                        "Who said that?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Dream. Death. Drugs.",
                        "You were doing so well."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Yeah, real funny."
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "I thought so."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Where are you?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Close enough."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "That's not what I asked."
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "No."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "...",
                        "Where's my boy?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Not with you."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "I can see that."
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Can you?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Man...",
                        "What the fuck is this place?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "There it is."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "What?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Panic."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "I ain't panicking."
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Of course not."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "I'm confused."
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Much better."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "You gonna answer anything?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Eventually."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Great."
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "You hide it well."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Hide what?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "How badly you want to wake up."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "...",
                        "If this is a dream, you're annoying as hell."
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "And if it isn't?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Then I got a bigger problem."
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Finally."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Finally what?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "You're paying attention."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Kinda hard not to."
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Good.",
                        "Walk."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerConfused,
                        "Where?"
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "You'll see."
                ),
                StorySequenceStep.visualNovelDialogue(
                        playerScared,
                        "Man, I hate that answer."
                ),
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "I know."
                ),
                StorySequenceStep.action(gp.storyFlags::completeIntroCutscene)
        );
    }

    public static StorySequence prologueBytEncounterIntro(GamePanel gp) {
        DialogueSpeaker byt = createSpeaker(
                "BYT",
                BYT_SHADOW_SPLASH,
                new Color(190, 80, 230)
        );

        return StorySequence.of(
                "prologue_byt_encounter_intro",
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Enough rehearsal.",
                        "You have learned how to move. Now learn what moving cannot save you from.",
                        "Come closer."
                ),
                StorySequenceStep.action(gp.levelManager::enterPrologueBytArena)
        );
    }

    public static StorySequence prologueBytScriptedDefeat(GamePanel gp) {
        DialogueSpeaker byt = createSpeaker(
                "BYT",
                BYT_SHADOW_SPLASH,
                new Color(190, 80, 230)
        );
        DialogueSpeaker narrator = new DialogueSpeaker(
                "Narrator",
                null,
                new Color(210, 210, 210)
        );

        return StorySequence.of(
                "prologue_byt_scripted_defeat",
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Oh.",
                        "You thought the red bar meant hope.",
                        "That is almost sweet."
                ),
                StorySequenceStep.visualNovelDialogue(
                        narrator,
                        "The shape in front of you stops pretending.",
                        "The world folds inward.",
                        "You were dead before the sound of it reached you."
                ),
                StorySequenceStep.action(gp.levelManager::finishPrologueBytEncounter)
        );
    }

    /**
     * First time in the village after BYT: the player opens their eyes on the plaza with old Aldous
     * standing over them. He welcomes them and asks them to come by his house; one blink later he is gone.
     */
    public static StorySequence villageAwakening(GamePanel gp) {
        DialogueSpeaker aldous = createSpeaker("Old Aldous", "npc/sage_down_1.png", new Color(200, 205, 220));
        NPC[] sage = new NPC[1];

        return StorySequence.of(
                "village_awakening",
                StorySequenceStep.action(() -> {
                    gp.ui.blink(0, 40, 70);
                    sage[0] = gp.aSetter.spawnAwakeningSage();
                }),
                StorySequenceStep.waitFrames(120),
                StorySequenceStep.dialogue(
                        aldous,
                        "Easy. Easy now. Breathe. There you are.",
                        "You died out there, child. Everyone here has, at least once. And yet you woke up. That's the village for you.",
                        "Welcome. I'm Aldous. I've been waiting for someone like you for longer than you'd believe.",
                        "Come and see me when your legs remember how to work. My house is at the bottom of the village, the old brick one with the broken roof."
                ),
                StorySequenceStep.action(() -> gp.ui.blink(22, 26, 40)),
                StorySequenceStep.waitFrames(34),
                StorySequenceStep.action(() -> gp.entities.removeNPC(sage[0])),
                StorySequenceStep.waitFrames(60)
        );
    }

    public static StorySequence prologueBytPlayerFell(GamePanel gp) {
        DialogueSpeaker byt = createSpeaker(
                "BYT",
                BYT_SHADOW_SPLASH,
                new Color(190, 80, 230)
        );

        return StorySequence.of(
                "prologue_byt_player_fell",
                StorySequenceStep.visualNovelDialogue(
                        byt,
                        "Already?",
                        "I had not even started trying.",
                        "Get up. Somewhere else. I will find you there too."
                ),
                StorySequenceStep.action(gp.levelManager::finishPrologueBytEncounter)
        );
    }

    private static DialogueSpeaker createSpeaker(String name, String portraitPath, Color accentColor) {
        return new DialogueSpeaker(name, loadImage(portraitPath), accentColor);
    }

    private static BufferedImage loadImage(String path) {
        try (InputStream stream = StorySequences.class.getClassLoader().getResourceAsStream(path)) {
            if (stream != null) {
                return ImageIO.read(stream);
            }
        } catch (IOException e) {
            System.err.println("Could not load story image " + path + ": " + e.getMessage());
            return null;
        }

        File file = new File("res", path);
        if (!file.isFile()) {
            System.err.println("Missing story image: " + path);
            return null;
        }

        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            System.err.println("Could not load story image file " + file + ": " + e.getMessage());
            return null;
        }
    }
}
