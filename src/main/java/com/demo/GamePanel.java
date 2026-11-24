package com.demo;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import com.demo.entity.AIPlayer;
import com.demo.entity.Player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable {

    private BufferedImage backgroundImage;
    private BufferedImage floorImage;
    Thread gameThread;
    KeyHandler keyHandler = new KeyHandler();

    public int screenWidth = 1600;
    public int screenHeight = 900;
    public int groundY;

    Player player;
    AIPlayer aiPlayer;

    private int currentFPS = 0;
    private int frameCount = 0;
    private long lastFPSTime = 0;
    
    private int roundDelay = 0;
    private boolean roundOver = false;
    private String roundMessage = "";
    private int playerWins = 0;
    private int aiWins = 0;
    
    private boolean showHitboxes = false; 

    private ArrayList<Leaf> leaves = new ArrayList<>();
    private Random random = new Random();
    private final int MAX_LEAVES = 60;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setDoubleBuffered(true);
        this.addKeyListener(keyHandler);
        this.setFocusable(true);

        groundY = 640;

        player = new Player(this, keyHandler);
        aiPlayer = new AIPlayer(this, player);

        init();
        initLeaves();
    }

    private void init() {
        try {
            backgroundImage = ImageIO.read(getClass().getResourceAsStream("/assets/background.png"));
            floorImage = ImageIO.read(getClass().getResourceAsStream("/assets/floor.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initLeaves() {
        for (int i = 0; i < MAX_LEAVES; i++) {
            leaves.add(new Leaf(random.nextInt(screenWidth), random.nextInt(screenHeight - 100)));
        }
    }

    public void update() {
        updateLeaves();

        if (roundOver) {
            roundDelay--;
            if (roundDelay <= 0) {
                resetRound();
            }
            return;
        }
        
        player.update();
        aiPlayer.update();
        
        checkCombat();
        checkRoundEnd();
    }

    private void updateLeaves() {
        for (Leaf leaf : leaves) {
            leaf.update(screenWidth, screenHeight);
        }
    }
    
    private void checkCombat() {
        Rectangle playerAttack = player.getAttackHitbox();
        Rectangle aiHitbox = aiPlayer.getHitbox();
        
        if (playerAttack != null && aiHitbox != null) {
            if (playerAttack.intersects(aiHitbox)) {
                int damage = player.getAttackDamage();
                aiPlayer.takeHit(damage);
                player.setAttackHit(true);
            }
        }
        
        Rectangle aiAttack = aiPlayer.getAttackHitbox();
        Rectangle playerHitbox = player.getHitbox();
        
        if (aiAttack != null && playerHitbox != null) {
            if (aiAttack.intersects(playerHitbox)) {
                int damage = aiPlayer.getAttackDamage();
                player.takeHit(damage);
                aiPlayer.setAttackHit(true);
            }
        }
    }
    
    private void checkRoundEnd() {
        if (player.health <= 0) {
            roundOver = true;
            roundDelay = 180;
            roundMessage = "AI WINS!";
            aiWins++;
            aiPlayer.onRoundEnd(true);
        } else if (aiPlayer.health <= 0) {
            roundOver = true;
            roundDelay = 180;
            roundMessage = "PLAYER WINS!";
            playerWins++;
            aiPlayer.onRoundEnd(false);
        }
    }
    
    private void resetRound() {
        roundOver = false;
        roundMessage = "";
        player.reset();
        aiPlayer.reset();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, screenWidth, screenHeight, null);
        } else {
            g2.setColor(new Color(50, 50, 80));
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }

        drawLeaves(g2);
        
        player.draw(g2);
        aiPlayer.draw(g2);

        

        if (floorImage != null) {
            g2.drawImage(floorImage, 0, screenHeight - floorImage.getHeight(), screenWidth, floorImage.getHeight(), null);
        } else {
            g2.setColor(new Color(50, 50, 80));
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }

        
        
        if (showHitboxes) {
            drawHitboxes(g2);
        }
        
        drawHealthBars(g2);
        
        if (roundOver) {
            drawRoundMessage(g2);
        }
    }

    private void drawLeaves(Graphics2D g2) {
        AffineTransform originalTransform = g2.getTransform();

        for (Leaf leaf : leaves) {
            g2.setColor(leaf.color);
            
            g2.translate(leaf.x, leaf.y);
            g2.rotate(Math.toRadians(leaf.angle));
            
            g2.fillRect(0, 0, leaf.size, leaf.size);
            
            g2.setTransform(originalTransform);
        }
    }
    
    private void drawHitboxes(Graphics2D g2) {
    
    Rectangle pBody = player.getHitbox();
    if (pBody != null) {
        g2.setColor(new Color(0, 255, 0, 80));
        g2.fillRect(pBody.x, pBody.y, pBody.width, pBody.height);
        g2.setColor(Color.GREEN);
        g2.setStroke(new java.awt.BasicStroke(2));
        g2.drawRect(pBody.x, pBody.y, pBody.width, pBody.height);
    }
    
    Rectangle pAttack = player.getAttackHitbox();
        if (pAttack != null) {
            g2.setColor(new Color(255, 0, 0, 120));
            g2.fillRect(pAttack.x, pAttack.y, pAttack.width, pAttack.height);
            g2.setColor(Color.RED);
            g2.setStroke(new java.awt.BasicStroke(3));
            g2.drawRect(pAttack.x, pAttack.y, pAttack.width, pAttack.height);
        }

        Rectangle aiBody = aiPlayer.getHitbox();
        if (aiBody != null) {
            g2.setColor(new Color(0, 255, 255, 80));
            g2.fillRect(aiBody.x, aiBody.y, aiBody.width, aiBody.height);
            g2.setColor(Color.CYAN);
            g2.setStroke(new java.awt.BasicStroke(2));
            g2.drawRect(aiBody.x, aiBody.y, aiBody.width, aiBody.height);
        }
        
        Rectangle aiAttack = aiPlayer.getAttackHitbox();
        if (aiAttack != null) {
            g2.setColor(new Color(255, 165, 0, 120));
            g2.fillRect(aiAttack.x, aiAttack.y, aiAttack.width, aiAttack.height);
            g2.setColor(Color.ORANGE);
            g2.setStroke(new java.awt.BasicStroke(3));
            g2.drawRect(aiAttack.x, aiAttack.y, aiAttack.width, aiAttack.height);
        }
        
        g2.setStroke(new java.awt.BasicStroke(1));
    }
    
    private void drawHealthBars(Graphics2D g2) {
        int barWidth = 400;
        int barHeight = 30;
        int margin = 50;
        int yPos = 30;
        
        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(margin, yPos, barWidth, barHeight, 10, 10);
        
        g2.setColor(new Color(220, 50, 50));
        int playerHealthWidth = (int) ((player.health / 100.0) * (barWidth - 4));
        g2.fillRoundRect(margin + 2, yPos + 2, Math.max(0, playerHealthWidth), barHeight - 4, 8, 8);
        
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("PLAYER: " + player.health + " HP", margin + 10, yPos + 22);
        
        int aiBarX = screenWidth - margin - barWidth;
        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(aiBarX, yPos, barWidth, barHeight, 10, 10);
        
        g2.setColor(new Color(50, 150, 220));
        int aiHealthWidth = (int) ((aiPlayer.health / 100.0) * (barWidth - 4));
        g2.fillRoundRect(aiBarX + barWidth - 2 - Math.max(0, aiHealthWidth), yPos + 2, Math.max(0, aiHealthWidth), barHeight - 4, 8, 8);
        
        g2.setColor(Color.WHITE);
        String aiText = "AI: " + aiPlayer.health + " HP";
        int textWidth = g2.getFontMetrics().stringWidth(aiText);
        g2.drawString(aiText, aiBarX + barWidth - textWidth - 10, yPos + 22);
        
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        String score = playerWins + " - " + aiWins;
        int scoreWidth = g2.getFontMetrics().stringWidth(score);
        g2.drawString(score, (screenWidth - scoreWidth) / 2, yPos + 22);
    }
    
    private void drawRoundMessage(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, screenHeight/2 - 60, screenWidth, 120);
        
        g2.setFont(new Font("Arial", Font.BOLD, 72));
        g2.setColor(Color.WHITE);
        int textWidth = g2.getFontMetrics().stringWidth(roundMessage);
        g2.drawString(roundMessage, (screenWidth - textWidth) / 2, screenHeight/2 + 20);
        
        g2.setFont(new Font("Arial", Font.PLAIN, 24));
        String subMessage = "Next round in " + (roundDelay / 60 + 1) + "...";
        int subWidth = g2.getFontMetrics().stringWidth(subMessage);
        g2.drawString(subMessage, (screenWidth - subWidth) / 2, screenHeight/2 + 50);
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / 60;
        double nextDrawTime = System.nanoTime() + drawInterval;

        lastFPSTime = System.currentTimeMillis();

        while (gameThread != null) {
            update();
            repaint();

            frameCount++;
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastFPSTime >= 1000) {
                currentFPS = frameCount;
                frameCount = 0;
                lastFPSTime = currentTime;
            }

            try {
                double remainingTime = (nextDrawTime - System.nanoTime()) / 1000000;
                if (remainingTime < 0) remainingTime = 0;

                Thread.sleep((long) remainingTime);
                nextDrawTime += drawInterval;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
        this.requestFocusInWindow();
    }
}