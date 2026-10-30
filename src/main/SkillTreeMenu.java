package main;

import entity.npc.NPC;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SkillTreeMenu {
    private static final int WIDTH = 700;
    private static final int HEIGHT = 470;
    private static final int NODE = 52;
    private static final int COL_W = 104;
    private static final int ROW_H = 80;
    private static final Color LINE_LIT = new Color(0xE0A838);
    private static final Color LINE_DIM = new Color(0x4A3A30);
    private static final Color NODE_FILL = new Color(0x1B1418);
    private static final Color BORDER_LEARNED = new Color(0xF0C050);
    private static final Color BORDER_OPEN = new Color(0xB89060);
    private static final Color BORDER_SEALED = new Color(0x4E4040);

    // One node of the tree: a skill, or a sealed placeholder (skill == null). Positions are in columns and rows.
    private record Node(Skill skill, double col, int row, int parent, int secondParent) {
        boolean sealed() {
            return skill == null;
        }
    }

    private static final int ROOT = 0;
    private static final List<Node> NODES = List.of(
            new Node(null, 0, 0, -1, -1),
            new Node(Skill.ROLL, -2.6, 1, ROOT, -1),
            new Node(Skill.DASH, -1.4, 1, ROOT, -1),
            new Node(null, -2, 2, 1, 2),
            new Node(null, -2, 3, 3, -1),
            new Node(Skill.PROJECTILE_CUT, 0, 1, ROOT, -1),
            new Node(Skill.PROJECTILE_REFLECT, 0, 2, 5, -1),
            new Node(null, 0, 3, 6, -1),
            new Node(null, 2, 1, ROOT, -1),
            new Node(null, 2, 2, 8, -1),
            new Node(null, 2, 3, 9, -1)
    );
    private static final String[][] BRANCHES = {{"MOVEMENT", "-2"}, {"BLADE", "0"}, {"GUN", "2"}};

    private final GamePanel gp;
    private final ConcurrentLinkedQueue<Integer> pressedKeys = new ConcurrentLinkedQueue<>();
    private final Font titleFont = UiFonts.of(Font.BOLD, 17);
    private final Font textFont = UiFonts.of(Font.PLAIN, 13);
    private final Font smallFont = UiFonts.of(Font.BOLD, 12);

    private NPC trainer;
    private int selected = 1;
    private String feedback = "";
    private boolean feedbackBad;

    public SkillTreeMenu(GamePanel gp) {
        this.gp = gp;
    }

    public void open(NPC trainer) {
        this.trainer = trainer;
        selected = 1;
        feedback = "";
        pressedKeys.clear();
        gp.keyH.clearGameplayInput();
        gp.gameState = GameState.SKILLS;
    }

    public Skill selectedSkill() {
        return NODES.get(selected).skill();
    }

    public void keyPressed(int code) {
        pressedKeys.add(code);
    }

    public void update() {
        Integer code;
        while (gp.gameState == GameState.SKILLS && (code = pressedKeys.poll()) != null) {
            handle(code);
        }
    }

    private void handle(int code) {
        switch (code) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> move(-1, 0);
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> move(1, 0);
            case KeyEvent.VK_UP, KeyEvent.VK_W -> move(0, -1);
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> move(0, 1);
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> learn(NODES.get(selected));
            case KeyEvent.VK_ESCAPE -> gp.shopMenu.openTrainer(trainer, true);
            default -> {
            }
        }
    }

    private void move(int dx, int dy) {
        Node from = NODES.get(selected);
        int best = -1;
        double bestScore = Double.MAX_VALUE;
        for (int i = 1; i < NODES.size(); i++) {
            Node n = NODES.get(i);
            double ddx = n.col() - from.col(), ddy = n.row() - from.row();
            double along = dx != 0 ? ddx * dx : ddy * dy;
            double across = dx != 0 ? Math.abs(ddy) : Math.abs(ddx);
            if (i == selected || along <= 0.01) {
                continue;
            }
            double score = along + across * 2.5;
            if (score < bestScore) {
                bestScore = score;
                best = i;
            }
        }
        if (best >= 0) {
            selected = best;
            feedback = "";
            gp.playSoundEffect(Sound.UI_MOVE);
        }
    }

    void learn(Node node) {
        Skill skill = node.skill();
        if (skill == null) {
            say("Not yet. Master Kuro will teach this later.", true);
            return;
        }
        if (gp.progression.hasSkill(skill)) {
            say("You already know " + skill.title + ".", true);
            return;
        }
        if (!gp.progression.canLearn(skill)) {
            say("Learn " + skill.requires.title + " first.", true);
            return;
        }
        if (!gp.progression.purchaseSkill(skill)) {
            say("Not enough skill points. You need " + skill.cost + ".", true);
            return;
        }
        say("Learned " + skill.title + ".", false);
        gp.playSoundEffect(Sound.SKILL_LEARN);
    }

    private void say(String text, boolean bad) {
        if (bad) {
            gp.playSoundEffect(Sound.UI_ERROR);
        }
        feedback = text;
        feedbackBad = bad;
    }


    public void draw(Graphics2D g2) {
        Font oldFont = g2.getFont();
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        Object oldAa = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(new Color(10, 4, 6, 170));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        int x = (gp.screenWidth - WIDTH) / 2;
        int y = (gp.screenHeight - HEIGHT) / 2 - 12;
        UiSkin.frame(g2, x, y, WIDTH, HEIGHT);
        UiSkin.header(g2, gp.screenWidth / 2, y - 14, "SKILL TREE");
        g2.setFont(smallFont);
        g2.setColor(UiSkin.GOLD);
        g2.drawString("LEVEL " + gp.progression.level() + "   |   SKILL POINTS: "
                + gp.progression.skillPoints(), x + 20, y + 24);

        int cx = gp.screenWidth / 2;
        int top = y + 34;
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        for (int i = 1; i < NODES.size(); i++) {
            Node n = NODES.get(i);
            drawLink(g2, n.parent(), i, cx, top);
            if (n.secondParent() >= 0) {
                drawLink(g2, n.secondParent(), i, cx, top);
            }
        }
        g2.setStroke(oldStroke);

        g2.setFont(smallFont);
        for (String[] branch : BRANCHES) {
            int bx = nodeX(Double.parseDouble(branch[1]), cx);
            int w = g2.getFontMetrics().stringWidth(branch[0]);
            g2.setColor(NODE_FILL);
            g2.fillRect(bx - w / 2 - 6, top + ROW_H - 23, w + 12, 17);
            g2.setColor(UiSkin.TEXT_DIM);
            g2.drawString(branch[0], bx - w / 2, top + ROW_H - 10);
        }
        for (int i = 0; i < NODES.size(); i++) {
            drawNode(g2, i, cx, top);
        }
        drawDetails(g2, x + 24, y + HEIGHT - 112, WIDTH - 48);

        drawHints(g2, "ARROWS  MOVE", "ENTER  LEARN", "ESC  BACK");
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAa);
        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }

    private static int nodeX(double col, int cx) {
        return cx + (int) Math.round(col * COL_W);
    }

    private static int nodeY(int row, int top) {
        return top + row * ROW_H + (row > 0 ? 6 : 0);
    }

    private void drawLink(Graphics2D g2, int parent, int child, int cx, int top) {
        Node p = NODES.get(parent), c = NODES.get(child);
        int px = nodeX(p.col(), cx), py = nodeY(p.row(), top) + NODE;
        int chx = nodeX(c.col(), cx), chy = nodeY(c.row(), top);
        int midY = (py + chy) / 2 + (c.row() == 1 ? 6 : 0);
        boolean lit = c.skill() != null && gp.progression.hasSkill(c.skill());
        g2.setColor(lit ? LINE_LIT : LINE_DIM);
        g2.drawLine(px, py, px, midY);
        g2.drawLine(px, midY, chx, midY);
        g2.drawLine(chx, midY, chx, chy);
    }

    private void drawNode(Graphics2D g2, int index, int cx, int top) {
        Node node = NODES.get(index);
        int nx = nodeX(node.col(), cx) - NODE / 2;
        int ny = nodeY(node.row(), top);
        boolean root = index == ROOT;
        boolean learned = root || (!node.sealed() && gp.progression.hasSkill(node.skill()));
        boolean open = !node.sealed() && gp.progression.canLearn(node.skill());

        if (index == selected) {
            g2.setColor(new Color(0xD04A36));
            g2.fillRect(nx - 6, ny - 6, NODE + 12, NODE + 12);
        }
        g2.setColor(learned ? BORDER_LEARNED : open ? BORDER_OPEN : BORDER_SEALED);
        g2.fillRect(nx - 3, ny - 3, NODE + 6, NODE + 6);
        g2.setColor(NODE_FILL);
        g2.fillRect(nx + 1, ny + 1, NODE - 2, NODE - 2);

        if (root) {
            UiSkin.icon(g2, UiSkin.Icon.MUSHROOM, nx + NODE / 2, ny + NODE / 2, 2, true);
        } else if (node.sealed()) {
            UiSkin.PixelFont.draw(g2, "?", nx + NODE / 2 - 4, ny + NODE / 2 - 7, 2, new Color(0x6A5C52));
        } else {
            UiSkin.icon(g2, node.skill().icon, nx + NODE / 2, ny + NODE / 2, 2, learned);
            if (!learned) {
                g2.setFont(smallFont);
                String cost = node.skill().cost + " SP";
                int w = g2.getFontMetrics().stringWidth(cost);
                int tx = nx + NODE / 2 - w / 2;
                g2.setColor(gp.progression.skillPoints() >= node.skill().cost ? UiSkin.GOLD : UiSkin.TEXT_RED);
                g2.drawString(cost, tx, ny + NODE + 18);
            }
        }
    }

    private void drawDetails(Graphics2D g2, int x, int y, int w) {
        UiSkin.inset(g2, x, y, w, 90);
        Node node = NODES.get(selected);
        int tx = x + 16;
        if (node.sealed()) {
            g2.setFont(titleFont);
            g2.setColor(UiSkin.TEXT_DIM);
            g2.drawString("Sealed technique", tx, y + 28);
            g2.setFont(textFont);
            g2.drawString("Master Kuro has not shown you this yet. The tree grows as you do.", tx, y + 50);
        } else {
            Skill skill = node.skill();
            boolean learned = gp.progression.hasSkill(skill);
            g2.setFont(titleFont);
            g2.setColor(UiSkin.TEXT);
            g2.drawString(skill.title, tx, y + 28);
            String status = learned ? "LEARNED" : gp.progression.canLearn(skill) ? "Cost: " + skill.cost + " SP" : "Needs " + skill.requires.title;
            g2.setFont(smallFont);
            g2.setColor(learned ? BORDER_LEARNED : UiSkin.GOLD);
            int sw = g2.getFontMetrics().stringWidth(status);
            g2.drawString(status, x + w - 16 - sw, y + 28);
            g2.setFont(textFont);
            g2.setColor(UiSkin.TEXT_DIM);
            QuestMenu.wrap(g2, skill.description, tx, y + 50, w - 32, 17);
        }
        g2.setFont(smallFont);
        if (!feedback.isEmpty()) {
            g2.setColor(feedbackBad ? UiSkin.TEXT_RED : UiSkin.GOLD);
            g2.drawString(feedback, tx, y + 84);
        } else {
            g2.setColor(UiSkin.GOLD);
            g2.drawString("Defeat enemies to level up. Each level grants 1 skill point.", tx, y + 84);
        }
    }

    private void drawHints(Graphics2D g2, String... hints) {
        int gap = 12;
        int total = -gap;
        for (String hint : hints) {
            total += hintWidth(hint) + gap;
        }
        int hx = (gp.screenWidth - total) / 2;
        for (String hint : hints) {
            UiSkin.plaque(g2, hx, gp.screenHeight - 42, hintWidth(hint), 27, hint, UiSkin.DIM);
            hx += hintWidth(hint) + gap;
        }
    }

    private static int hintWidth(String hint) {
        int width = UiSkin.PixelFont.width(hint, 2) + 30;
        return width + (UiSkin.P - width % UiSkin.P) % UiSkin.P;
    }
}
