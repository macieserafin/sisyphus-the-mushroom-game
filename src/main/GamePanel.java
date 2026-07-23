package main;

import entity.Entity;
import entity.Player;
import entity.enemy.Enemy;
import entity.npc.NPC;
import object.SuperObject;
import tile.TileManager;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel implements Runnable {

// SCREEN SETTINGS

    public final int originalTitleSize = 16; //16x16 tile default
    public final int scale = 3; //16x16 will look like 48x48

    public final int tileSize = originalTitleSize * scale; //48x48 tile
    public final int maxScreenCol = 16;
    public final int maxScreenRow = 12;  //4x3
    public final int screenWidth = tileSize * maxScreenCol; //768 pixels
    public final int screenHeight = tileSize * maxScreenRow; // 576 pixels

    public final int maxWorldCol = 50;
    public final int maxWorldRow = 50;

    int FPS = 60;

    public KeyHandler keyH = new KeyHandler(this);
    Sound music = new Sound();
    Sound se = new Sound();

    public CollisionChecker cChecker = new CollisionChecker(this);
    public AssetSetter aSetter = new AssetSetter(this);
    public UI ui = new UI(this);
    Thread gameThread;
    public TileManager tileM = new TileManager(this);
    public BulletManager bulletManager = new BulletManager(this);
    public LevelManager levelManager = new LevelManager(this);


    //entity
    public Player player = new Player(this, keyH);
    public SuperObject[] obj = new SuperObject[30]; //how many object u can display on the same time
    public NPC[] npc = new NPC[10];
    public Enemy[] enemy = new Enemy[80];


    //game state
    public GameState gameState;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
    }

    public void setupGame(){
        levelManager.loadHub();

        playMusic(0);
        gameState = GameState.PLAYING;
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {

        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;
        long timer = 0;
        int drawCount = 0;

        while(gameThread != null) {

            currentTime = System.nanoTime();

            delta += (currentTime - lastTime) / drawInterval;
            timer += (currentTime - lastTime);
            lastTime = currentTime;

            if (delta >= 1) {

                // 1 UPDATE: update information such as character positions
                update();

                // 2 DRAW: draw the screen with the updated information
                repaint();

                delta--;
                drawCount++;
            }

            if (timer >= 1000000000) {
                System.out.println("FPS: " + FPS);
                drawCount = 0;
                timer = 0;
            }
        }
    }

    public void update() {
        if (gameState == GameState.PLAYING) {
            player.update();
            bulletManager.update();

            for(int i = 0; i < npc.length; i++) {
                if(npc[i] != null) {
                    npc[i].update();
                }
            }

            for(int i = 0; i < enemy.length; i++) {
                if(enemy[i] != null) {
                    if(enemy[i].alive) {
                        enemy[i].update();
                    }
                    else {
                        enemy[i] = null;
                    }
                }
            }
        }
        if (gameState == GameState.PAUSED) {
            /// ///////
        }


    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        tileM.draw(g2);

        bulletManager.draw(g2);

        for(int i = 0; i < obj.length; i++) {
            if(obj[i] != null) {
                obj[i].draw(g2, this);
            }
        }
        for(int i = 0; i < npc.length; i++) {
            if(npc[i] != null) {
                npc[i].draw(g2);
            }
        }
        for(int i = 0; i < enemy.length; i++) {
            if(enemy[i] != null) {
                enemy[i].draw(g2);
            }
        }

        player.draw(g2);

        ui.draw(g2);

        g2.dispose();
    }

    public void playMusic(int i) {
        music.setFile(i);
        music.play();
        music.loop();
    }

    public void pauseMusic(int i) {
        music.stop();
    }

    public void playSoundEffect(int i) {
        se.setFile(i);
        se.play();
    }
}
