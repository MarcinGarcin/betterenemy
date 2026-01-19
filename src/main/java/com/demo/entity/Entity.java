package com.demo.entity;

import java.awt.image.BufferedImage;

public class Entity {
    public int x, y;
    public int speed;
    public int health;
    
    public BufferedImage[] slowAttackSprites;
    public BufferedImage[] fastAttackSprites;
    public BufferedImage[] jumpSprites;
    public BufferedImage[] fallSprites;
    public BufferedImage[] deathSprites;
    public BufferedImage[] idleSprites;
    public BufferedImage[] runSprites;
    public BufferedImage[] takeHitSprites;

    public int spriteCounter = 0;
    public int spriteNum = 0;

    public enum State {
        IDLE, RUNNING, JUMPING, FALLING, ATTACKING_SLOW, ATTACKING_FAST, TAKING_HIT, DEAD
    }
}