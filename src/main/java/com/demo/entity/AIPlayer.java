package com.demo.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import com.demo.GamePanel;
import com.demo.ai.NeuralNetwork;

public class AIPlayer extends Entity {
    GamePanel gp;
    public int width;
    public int height;
    public State currentState = State.IDLE;
    private double velocityY = 0;
    private final double GRAVITY = 0.5;
    private final double JUMP_STRENGTH = -12;
    private boolean onGround = true;
    private boolean facingRight = false;
    private boolean isAttacking = false;
    private boolean isTakingHit = false;
    private BufferedImage[] currentSprites;
    
    private NeuralNetwork brain;
    private double epsilon = 0.3;
    private final double EPSILON_DECAY = 0.995;
    private final double EPSILON_MIN = 0.05;
    private final double GAMMA = 0.95;
    
    private Player opponent;
    
    private int lastHealth;
    private int lastOpponentHealth;
    private double totalReward;
    
    private boolean moveLeft, moveRight, jumpAction;
    private boolean fastAttack, slowAttack;
    
    private boolean attackHit = false;
    
    private int wins = 0;
    private int losses = 0;
    private int rounds = 0;
    
    

    public AIPlayer(GamePanel gp, Player opponent) {
        this.gp = gp;
        this.opponent = opponent;
        
        brain = NeuralNetwork.load("ai_brain.dat");
        if (brain == null) {
            brain = new NeuralNetwork(10, 64, 6, 0.001);
        }
        
        setDefaultValues();
        loadPlayerSprites();
    }

    public void setDefaultValues() {
        width = 300;
        height = 300;
        health = 100;
        x = gp.screenWidth - 400;
        y = gp.groundY;
        speed = 4;
        lastHealth = health;
        lastOpponentHealth = opponent.health;
    }

    public void loadPlayerSprites() {
        try {
            idleSprites = setup("/assets/character/AI/Idle.png", 8, 200, 200);
            runSprites = setup("/assets/character/AI/Run.png", 8, 200, 200);
            jumpSprites = setup("/assets/character/AI/Jump.png", 2, 200, 200);
            fallSprites = setup("/assets/character/AI/Fall.png", 2, 200, 200);
            slowAttackSprites = setup("/assets/character/AI/Attack1.png", 6, 200, 200);
            fastAttackSprites = setup("/assets/character/AI/Attack2.png", 3, 200, 200);
            deathSprites = setup("/assets/character/AI/Death.png", 6, 200, 200);
            takeHitSprites = setup("/assets/character/AI/Take Hit.png", 4, 200, 200);
            currentSprites = idleSprites;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BufferedImage[] setup(String imagePath, int frameCount, int frameWidth, int frameHeight) throws IOException {
        BufferedImage[] images = new BufferedImage[frameCount];
        BufferedImage spriteSheet = ImageIO.read(getClass().getResourceAsStream(imagePath));
        
        for (int i = 0; i < frameCount; i++) {
            images[i] = spriteSheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
        }
        return images;
    }

    private double[] getState() {
        double[] state = new double[10];
        
        int myCenterX = x + width / 2;
        int opponentCenterX = opponent.x + opponent.width / 2;
        
        double dx = (opponentCenterX - myCenterX) / 800.0;
        double dy = (opponent.y - y) / 400.0;
        double distance = Math.abs(opponentCenterX - myCenterX) / 400.0;
        
        state[0] = dx;
        state[1] = dy;
        state[2] = health / 100.0;
        state[3] = opponent.health / 100.0;
        state[4] = onGround ? 1.0 : -1.0;
        state[5] = isAttacking ? 1.0 : -1.0;
        state[6] = opponent.isAttacking() ? 1.0 : -1.0;
        state[7] = velocityY / 15.0;
        state[8] = distance < 0.5 ? 1.0 : -1.0; 
        state[9] = (opponentCenterX > myCenterX) == facingRight ? 1.0 : -1.0;
        
        return state;
    }

    public void update() {
        if (currentState == State.DEAD) {
            updateAnimation();
            return;
        }
        
        if (isTakingHit) {
            updateAnimation();
            applyGravity();
            if (spriteNum >= takeHitSprites.length - 1) {
                isTakingHit = false;
                currentState = State.IDLE;
                currentSprites = idleSprites;
                spriteNum = 0;
            }
            return;
        }
        
        State previousState = currentState;
        
        if (!isAttacking) {
            facingRight = opponent.x + opponent.width / 2 > x + width / 2;
        }
        
        double[] state = getState();
        double[] qValues = brain.forward(state);
        int action = brain.selectAction(qValues, epsilon);
        
        resetActions();
        applyAction(action);
        
        if (isAttacking) {
            updateAnimation();
            applyGravity();
            
            double reward = calculateReward();
            totalReward += reward;
            double[] nextState = getState();
            boolean done = health <= 0 || opponent.health <= 0;
            brain.learn(reward, nextState, GAMMA, done);
            
            lastHealth = health;
            lastOpponentHealth = opponent.health;
            return;
        }
        
        handleMovement();
        handleJump();
        handleAttack();
        applyGravity();
        determineState();
        
        if (previousState != currentState) {
            spriteNum = 0;
            spriteCounter = 0;
            updateCurrentSprites();
        }
        
        updateAnimation();
        
        double reward = calculateReward();
        totalReward += reward;
        double[] nextState = getState();
        boolean done = health <= 0 || opponent.health <= 0;
        brain.learn(reward, nextState, GAMMA, done);
        
        lastHealth = health;
        lastOpponentHealth = opponent.health;
    }

    private void applyAction(int action) {
        switch (action) {
            case 0: moveLeft = true; break;
            case 1: moveRight = true; break;
            case 2: jumpAction = true; break;
            case 3: break; 
            case 4: fastAttack = true; break;
            case 5: slowAttack = true; break;
        }
    }

    private void resetActions() {
        moveLeft = false;
        moveRight = false;
        jumpAction = false;
        fastAttack = false;
        slowAttack = false;
    }

    private double calculateReward() {
        double reward = 0;
        
        int opponentDamage = lastOpponentHealth - opponent.health;
        if (opponentDamage > 0) {
            reward += opponentDamage * 3.0;
        }
        
        int selfDamage = lastHealth - health;
        if (selfDamage > 0) {
            reward -= selfDamage * 2.0;
        }
        
        int myCenterX = x + width / 2;
        int opponentCenterX = opponent.x + opponent.width / 2;
        double distance = Math.abs(opponentCenterX - myCenterX);
        
        if (distance < 200 && distance > 80) {
            reward += 0.1;
        }
        
        if (opponent.health <= 0) {
            reward += 100.0;
        }
        if (health <= 0) {
            reward -= 100.0;
        }
        
        return reward;
    }

    private void handleMovement() {
        if (moveLeft) {
            x -= speed;
        }
        if (moveRight) {
            x += speed;
        }
        
        if (x < -128) x = -128;
        if (x > gp.screenWidth - 172) x = gp.screenWidth - 172;
    }

    private void handleJump() {
        if (jumpAction && onGround) {
            velocityY = JUMP_STRENGTH;
            onGround = false;
        }
    }

    private void handleAttack() {
        if (slowAttack && onGround && !isAttacking) {
            isAttacking = true;
            attackHit = false;
            currentState = State.ATTACKING_SLOW;
            spriteNum = 0;
            spriteCounter = 0;
            currentSprites = slowAttackSprites;
        } else if (fastAttack && onGround && !isAttacking) {
            isAttacking = true;
            attackHit = false;
            currentState = State.ATTACKING_FAST;
            spriteNum = 0;
            spriteCounter = 0;
            currentSprites = fastAttackSprites;
        }
    }

    private void applyGravity() {
        if (!onGround) {
            velocityY += GRAVITY;
            y += (int) velocityY;
            
            if (y >= gp.groundY) {
                y = gp.groundY;
                velocityY = 0;
                onGround = true;
            }
        }
    }

    private void determineState() {
        if (isAttacking) {
            return;  
        }
        
        if (!onGround) {
            currentState = velocityY < 0 ? State.JUMPING : State.FALLING;
        } else if (moveLeft || moveRight) {
            currentState = State.RUNNING;
        } else {
            currentState = State.IDLE;
        }
    }

    private void updateCurrentSprites() {
        switch (currentState) {
            case IDLE: currentSprites = idleSprites; break;
            case RUNNING: currentSprites = runSprites; break;
            case JUMPING: currentSprites = jumpSprites; break;
            case FALLING: currentSprites = fallSprites; break;
            case ATTACKING_SLOW: currentSprites = slowAttackSprites; break;
            case ATTACKING_FAST: currentSprites = fastAttackSprites; break;
            case TAKING_HIT: currentSprites = takeHitSprites; break;
            case DEAD: currentSprites = deathSprites; break;
        }
    }

    private void updateAnimation() {
        spriteCounter++;
        int animationSpeed = isAttacking ? 8 : 10;
        
        if (spriteCounter > animationSpeed) {
            spriteNum++;
            
            if (currentSprites != null && spriteNum >= currentSprites.length) {
                if (isAttacking) {
                    isAttacking = false;
                    attackHit = false;
                    currentState = State.IDLE;
                    currentSprites = idleSprites;
                } else if (currentState == State.DEAD) {
                    spriteNum = currentSprites.length - 1;
                    return;
                }
                spriteNum = 0;
            }
            spriteCounter = 0;
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = null;

        if (currentSprites != null && spriteNum < currentSprites.length) {
            image = currentSprites[spriteNum];
        }

        if (image != null) {
            if (facingRight) {
                g2.drawImage(image, x, y, width, height, null);
            } else {
                g2.drawImage(image, x + width, y, -width, height, null);
            }
        } else {
            g2.setColor(Color.BLUE);
            g2.fillRect(x + 100, y + 50, 100, 200);
        }
    }

    public Rectangle getHitbox() {
        int hitboxWidth = 60;
        int hitboxHeight = 100;
        int hitboxX = x + (width - hitboxWidth) / 2;
        int hitboxY = y + 110;
        
        return new Rectangle(hitboxX, hitboxY, hitboxWidth, hitboxHeight);
    }
    public Rectangle getAttackHitbox() {
        if (!isAttacking || attackHit) {
            return null;
        }
        
        boolean isActiveFrame = false;
        
        if (currentState == State.ATTACKING_FAST) {
            isActiveFrame = (spriteNum >= 1);
        } else if (currentState == State.ATTACKING_SLOW) {
            isActiveFrame = (spriteNum >= 2 && spriteNum <= 4);
        }
        
        if (isActiveFrame) {
            int attackWidth = 100;
            int attackHeight = 80;
            
            Rectangle body = getHitbox();
            int attackY = body.y;  
            
            int attackX;
            if (facingRight) {
                attackX = body.x + body.width - 20;  
            } else {
                
                attackX = body.x + 20 - attackWidth;  
            }
            
            return new Rectangle(attackX, attackY, attackWidth, attackHeight);
        }
        
        return null;
    }

    public void setAttackHit(boolean hit) {
        this.attackHit = hit;
    }

    public void takeHit(int damage) {
        if (currentState == State.DEAD || isTakingHit) return;
        
        health -= damage;
        
        if (health <= 0) {
            health = 0;
            currentState = State.DEAD;
            currentSprites = deathSprites;
            spriteNum = 0;
            spriteCounter = 0;
            isAttacking = false;
        } else {
            isTakingHit = true;
            currentState = State.TAKING_HIT;
            currentSprites = takeHitSprites;
            spriteNum = 0;
            spriteCounter = 0;
            isAttacking = false;
        }
    }

    public void reset() {
        health = 100;
        x = gp.screenWidth - 400;
        y = gp.groundY;
        currentState = State.IDLE;
        currentSprites = idleSprites;
        spriteNum = 0;
        spriteCounter = 0;
        isAttacking = false;
        isTakingHit = false;
        onGround = true;
        velocityY = 0;
        facingRight = false;
        attackHit = false;
        lastHealth = health;
        lastOpponentHealth = opponent.health;
    }

    public void onRoundEnd(boolean won) {
        rounds++;
        if (won) wins++; else losses++;
        
        epsilon = Math.max(EPSILON_MIN, epsilon * EPSILON_DECAY);
        
        if (rounds % 10 == 0) {
            brain.save("ai_brain.dat");
        }
        
        System.out.printf("Round %d - AI %s | W/L: %d/%d | Eps: %.3f%n",
                rounds, won ? "WON" : "LOST", wins, losses, epsilon);
        
        totalReward = 0;
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    public int getAttackDamage() {
        return currentState == State.ATTACKING_FAST ? 8 : 
               currentState == State.ATTACKING_SLOW ? 15 : 0;
    }

    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getRounds() { return rounds; }
    public double getEpsilon() { return epsilon; }
}