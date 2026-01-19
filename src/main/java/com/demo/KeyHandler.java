package com.demo;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {
    
    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean attack1Pressed, attack2Pressed;
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_SPACE) {
            upPressed = true;
        }
        if (code == KeyEvent.VK_S) {
            downPressed = true;
        }
        if (code == KeyEvent.VK_A) {
            leftPressed = true;
        }
        if (code == KeyEvent.VK_D) {
            rightPressed = true;
        }
        if (code == KeyEvent.VK_J) {
            attack1Pressed = true;
        }
        if (code == KeyEvent.VK_K) {
            attack2Pressed = true;
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_SPACE) {
            upPressed = false;
        }
        if (code == KeyEvent.VK_S) {
            downPressed = false;
        }
        if (code == KeyEvent.VK_A) {
            leftPressed = false;
        }
        if (code == KeyEvent.VK_D) {
            rightPressed = false;
        }
        if (code == KeyEvent.VK_J) {
            attack1Pressed = false;
        }
        if (code == KeyEvent.VK_K) {
            attack2Pressed = false;
        }
    }
}