package com.demo;

import java.awt.Color;
import java.util.Random;

public class Leaf {
    public double x, y;
    public double speedX, speedY;
    public double angle;
    public double rotationSpeed;
    public int size;
    public Color color;
    
    private Random random = new Random();

    public Leaf(double startX, double startY) {
        reset(startX, startY);
    }

    public void update(int screenWidth, int screenHeight) {
        x += speedX;
        y += speedY;
        angle += rotationSpeed;

        if (x < -20 || y > screenHeight + 20) {
            reset(screenWidth + random.nextInt(200), random.nextInt(screenHeight - 200));
        }
    }

    private void reset(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        
        this.speedX = -1.5 - random.nextDouble() * 2.0; 
        this.speedY = 0.5 + random.nextDouble(); 
        
        this.angle = random.nextDouble() * 360;
        this.rotationSpeed = -2 + random.nextDouble() * 4;
        
        this.size = 4 + random.nextInt(5);
        
        int colorType = random.nextInt(3);
        if (colorType == 0) this.color = new Color(50, 100, 70); 
        else if (colorType == 1) this.color = new Color(30, 60, 40);
        else this.color = new Color(70, 120, 90); 
    }
}