package com.demo;

import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame();
        window.setTitle("BetterEnemy");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        
        window.setSize(1600, 900);
        window.setLocationRelativeTo(null);
        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        window.pack();

        gamePanel.startGameThread();
        window.setVisible(true);
    }
}