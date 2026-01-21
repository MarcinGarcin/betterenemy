package com.demo.entity;

import com.demo.GamePanel;
import com.demo.ai.NeuralNetwork;

public class AIPlayer extends Fighter {
    private NeuralNetwork brain;
    private double epsilon = 0.3;
    private final double EPSILON_DECAY = 0.995;
    private final double EPSILON_MIN = 0.05;
    private final double GAMMA = 0.95;
    
    private Player opponent;
    
    private int lastHealth;
    private int lastOpponentHealth;
    private double lastDistance;
    private double totalReward;
    
    private int wins = 0;
    private int losses = 0;
    private int rounds = 0;

    public AIPlayer(GamePanel gp, Player opponent) {
        super(gp);
        this.opponent = opponent;
        
        brain = NeuralNetwork.load("ai_brain.dat");
        if (brain == null) {
            brain = new NeuralNetwork(10, 64, 6, 0.01);
        }
        
        setDefaultValues();
        loadSprites();
        initializeTracking();
    }

    @Override
    protected String getSpritePath() {
        return "/assets/character/AI";
    }

    @Override
    protected int getStartX() {
        return gp.screenWidth - 400;
    }

    @Override
    protected boolean getDefaultFacingRight() {
        return false;
    }

    @Override
    public void setDefaultValues() {
        super.setDefaultValues();
        initializeTracking();
    }

    private void initializeTracking() {
        lastHealth = health;
        lastOpponentHealth = opponent != null ? opponent.health : 100;
        
        int myCenterX = x + width / 2;
        int opponentCenterX = opponent != null ? opponent.x + opponent.width / 2 : 0;
        lastDistance = Math.abs(opponentCenterX - myCenterX);
    }

    @Override
    protected void handleInput() {
        if (!isAttacking) {
            facingRight = opponent.x + opponent.width / 2 > x + width / 2;
        }
        
        double[] state = getState();
        double[] qValues = brain.forward(state);
        int action = brain.selectAction(qValues, epsilon);
        
        resetActions();
        applyAction(action);
    }

    @Override
    protected void onUpdateStart() {
    }

    @Override
    protected void onUpdateEnd() {
        double reward = calculateReward();
        totalReward += reward;
        double[] nextState = getState();
        boolean done = health <= 0 || opponent.health <= 0;
        brain.learn(reward, nextState, GAMMA, done);
        
        lastHealth = health;
        lastOpponentHealth = opponent.health;
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

    private void applyAction(int action) {
        switch (action) {
            case 0: moveLeft = true; break;
            case 1: moveRight = true; break;
            case 2: jumpAction = true; break;
            case 3: dashDown = true; break;
            case 4: fastAttack = true; break;
            case 5: slowAttack = true; break;
        }
    }

    private double calculateReward() {
        double reward = 0;
        int myCenterX = x + width / 2;
        int opponentCenterX = opponent.x + opponent.width / 2;
        double distance = Math.abs(opponentCenterX - myCenterX);
        
        int opponentDamage = lastOpponentHealth - opponent.health;
        if (opponentDamage > 0) {
            reward += opponentDamage * 3.0;
        }
        
        int selfDamage = lastHealth - health;
        if (selfDamage > 0) {
            reward -= selfDamage * 2.0;
        }
        
        double distanceChange = lastDistance - distance;
        reward += distanceChange * 0.1;
        
        double maxDistance = 600.0;
        double normalizedDistance = Math.min(distance / maxDistance, 1.0);
        reward -= normalizedDistance * 0.15;
        
        if (distance < 150) {
            reward += 0.3;
        } else if (distance < 250) {
            reward += 0.1;
        } else if (distance > 400) {
            reward -= 0.4;
        }
        
        if (distanceChange < -5 && !opponent.isAttacking()) {
            reward -= 0.2;
        }
        
        if (distanceChange > 5 && opponent.isAttacking() && distance > 150) {
            reward += 0.15;
        }
        
        if (opponent.health <= 0) {
            reward += 100.0;
        }
        if (health <= 0) {
            reward -= 100.0;
        }
        
        lastDistance = distance;
        
        return reward;
    }

    @Override
    public void reset() {
        super.reset();
        initializeTracking();
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

    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getRounds() { return rounds; }
    public double getEpsilon() { return epsilon; }
}