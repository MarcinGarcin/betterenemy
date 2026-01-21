package com.demo.entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import com.demo.GamePanel;

public abstract class Fighter extends Entity {
    protected GamePanel gp;
    public int width = 300;
    public int height = 300;
    public State currentState = State.IDLE;
    
    protected double velocityY = 0;
    protected final double GRAVITY = 0.5;
    protected final double JUMP_STRENGTH = -12;
    protected boolean onGround = true;
    protected boolean facingRight = true;
    protected boolean isAttacking = false;
    protected boolean isTakingHit = false;
    protected BufferedImage[] currentSprites;
    protected boolean attackHit = false;
    
    protected boolean moveLeft, moveRight, jumpAction, dashDown;
    protected boolean fastAttack, slowAttack;

    public Fighter(GamePanel gp) {
        this.gp = gp;
    }

    protected abstract String getSpritePath();
    protected abstract int getStartX();
    protected abstract boolean getDefaultFacingRight();
    protected abstract void handleInput();
    protected abstract void onUpdateStart();
    protected abstract void onUpdateEnd();

    public void setDefaultValues() {
        width = 300;
        height = 300;
        health = 100;
        x = getStartX();
        y = gp.groundY;
        speed = 4;
        facingRight = getDefaultFacingRight();
        currentSprites = idleSprites;
    }

    public void loadSprites() {
        try {
            String path = getSpritePath();
            idleSprites = setup(path + "/Idle.png", 8, 200, 200);
            runSprites = setup(path + "/Run.png", 8, 200, 200);
            jumpSprites = setup(path + "/Jump.png", 2, 200, 200);
            fallSprites = setup(path + "/Fall.png", 2, 200, 200);
            slowAttackSprites = setup(path + "/Attack1.png", 6, 200, 200);
            fastAttackSprites = setup(path + "/Attack2.png", 3, 200, 200);
            deathSprites = setup(path + "/Death.png", 6, 200, 200);
            takeHitSprites = setup(path + "/Take Hit.png", 4, 200, 200);
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
        
        onUpdateStart();
        handleInput();

        if (isAttacking) {
            updateAnimation();
            applyGravity();
            onUpdateEnd();
            return;
        }

        handleMovement();
        handleJump();
        handleDashDown();
        handleAttack();
        applyGravity();
        determineState();

        if (previousState != currentState) {
            spriteNum = 0;
            spriteCounter = 0;
            updateCurrentSprites();
        }

        updateAnimation();
        onUpdateEnd();
    }

    protected void resetActions() {
        moveLeft = false;
        moveRight = false;
        jumpAction = false;
        dashDown = false;
        fastAttack = false;
        slowAttack = false;
    }

    protected void handleMovement() {
        if (moveLeft) {
            x -= speed;
        }
        if (moveRight) {
            x += speed;
        }
        
        if (x < -128) x = -128;
        if (x > gp.screenWidth - 172) x = gp.screenWidth - 172;
    }

    protected void handleJump() {
        if (jumpAction && onGround) {
            velocityY = JUMP_STRENGTH;
            onGround = false;
        }
    }

    protected void handleDashDown() {
        if (dashDown && !onGround) {
            velocityY += GRAVITY * 2;
        }
    }

    protected void handleAttack() {
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

    protected void applyGravity() {
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

    protected void determineState() {
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

    protected void updateCurrentSprites() {
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

    protected void updateAnimation() {
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
            int attackWidth = 130;
            int attackHeight = 80;
            
            Rectangle body = getHitbox();
            int attackY = body.y - 10;
            
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
        x = getStartX();
        y = gp.groundY;
        currentState = State.IDLE;
        currentSprites = idleSprites;
        spriteNum = 0;
        spriteCounter = 0;
        isAttacking = false;
        isTakingHit = false;
        onGround = true;
        velocityY = 0;
        facingRight = getDefaultFacingRight();
        attackHit = false;
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public int getAttackDamage() {
        if (currentState == State.ATTACKING_FAST) {
            return 8;
        } else if (currentState == State.ATTACKING_SLOW) {
            return 15;
        }
        return 0;
    }
}