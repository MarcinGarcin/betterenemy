package com.demo.entity;

import com.demo.GamePanel;
import com.demo.KeyHandler;

public class Player extends Fighter {
    private KeyHandler keyH;

    public Player(GamePanel gp, KeyHandler keyH) {
        super(gp);
        this.keyH = keyH;
        setDefaultValues();
        loadSprites();
    }

    @Override
    protected String getSpritePath() {
        return "/assets/character/Player";
    }

    @Override
    protected int getStartX() {
        return 100;
    }

    @Override
    protected boolean getDefaultFacingRight() {
        return true;
    }

    @Override
    protected void handleInput() {
        resetActions();
        moveLeft = keyH.leftPressed;
        moveRight = keyH.rightPressed;
        jumpAction = keyH.upPressed;
        dashDown = keyH.downPressed;
        
        if (keyH.attack1Pressed) {
            slowAttack = true;
            keyH.attack1Pressed = false;
        }
        if (keyH.attack2Pressed) {
            fastAttack = true;
            keyH.attack2Pressed = false;
        }
    }

    @Override
    protected void onUpdateStart() {
    }

    @Override
    protected void onUpdateEnd() {
    }

    @Override
    protected void handleMovement() {
        super.handleMovement();
        if (moveLeft) {
            facingRight = false;
        }
        if (moveRight) {
            facingRight = true;
        }
    }

    @Override
    protected void determineState() {
        if (isAttacking) {
            return;
        }
        
        if (!onGround) {
            currentState = velocityY < 0 ? State.JUMPING : State.FALLING;
        } else if (keyH.leftPressed || keyH.rightPressed) {
            currentState = State.RUNNING;
        } else {
            currentState = State.IDLE;
        }
    }
}