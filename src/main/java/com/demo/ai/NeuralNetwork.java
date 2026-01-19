package com.demo.ai;

import java.io.*;
import java.util.Random;

public class NeuralNetwork implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private double[][] weightsInputHidden;
    private double[][] weightsHiddenOutput;
    private double[] biasHidden;
    private double[] biasOutput;
    private int inputSize;
    private int hiddenSize;
    private int outputSize;
    private double learningRate;
    private transient Random random;
    
    private double[] lastInputs;
    private int lastAction;
    private double[] hiddenActivations;
    
    public NeuralNetwork(int inputSize, int hiddenSize, int outputSize, double learningRate) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;
        this.outputSize = outputSize;
        this.learningRate = learningRate;
        this.random = new Random();
        
        weightsInputHidden = new double[inputSize][hiddenSize];
        weightsHiddenOutput = new double[hiddenSize][outputSize];
        biasHidden = new double[hiddenSize];
        biasOutput = new double[outputSize];
        
        initializeWeights();
    }
    
    private void initializeWeights() {
        if (random == null) random = new Random();
        
        double scale1 = Math.sqrt(2.0 / inputSize);
        double scale2 = Math.sqrt(2.0 / hiddenSize);
        
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                weightsInputHidden[i][j] = random.nextGaussian() * scale1;
            }
        }
        
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                weightsHiddenOutput[i][j] = random.nextGaussian() * scale2;
            }
        }
        
        for (int i = 0; i < hiddenSize; i++) {
            biasHidden[i] = 0.01;
        }
        
        for (int i = 0; i < outputSize; i++) {
            biasOutput[i] = 0.01;
        }
    }
    
    private double relu(double x) {
        return Math.max(0, x);
    }
    
    public double[] forward(double[] inputs) {
        lastInputs = inputs.clone();
        hiddenActivations = new double[hiddenSize];
        
        for (int j = 0; j < hiddenSize; j++) {
            double sum = biasHidden[j];
            for (int i = 0; i < inputSize; i++) {
                sum += inputs[i] * weightsInputHidden[i][j];
            }
            hiddenActivations[j] = relu(sum);
        }
        
        double[] outputs = new double[outputSize];
        for (int j = 0; j < outputSize; j++) {
            double sum = biasOutput[j];
            for (int i = 0; i < hiddenSize; i++) {
                sum += hiddenActivations[i] * weightsHiddenOutput[i][j];
            }
            outputs[j] = sum;
        }
        
        return outputs;
    }
    
    public int selectAction(double[] qValues, double epsilon) {
        if (random == null) random = new Random();
        
        if (random.nextDouble() < epsilon) {
            lastAction = random.nextInt(outputSize);
        } else {
            int maxIndex = 0;
            double maxValue = qValues[0];
            for (int i = 1; i < qValues.length; i++) {
                if (qValues[i] > maxValue) {
                    maxValue = qValues[i];
                    maxIndex = i;
                }
            }
            lastAction = maxIndex;
        }
        return lastAction;
    }
    
    public void learn(double reward, double[] nextState, double gamma, boolean done) {
        if (lastInputs == null) return;
        
        double[] nextQValues = forward(nextState);
        double maxNextQ = done ? 0 : getMax(nextQValues);
        
        double[] hidden = new double[hiddenSize];
        for (int j = 0; j < hiddenSize; j++) {
            double sum = biasHidden[j];
            for (int i = 0; i < inputSize; i++) {
                sum += lastInputs[i] * weightsInputHidden[i][j];
            }
            hidden[j] = relu(sum);
        }
        
        double currentQ = biasOutput[lastAction];
        for (int i = 0; i < hiddenSize; i++) {
            currentQ += hidden[i] * weightsHiddenOutput[i][lastAction];
        }
        
        double targetQ = reward + gamma * maxNextQ;
        double tdError = targetQ - currentQ;
        tdError = Math.max(-1.0, Math.min(1.0, tdError));
        
        for (int i = 0; i < hiddenSize; i++) {
            weightsHiddenOutput[i][lastAction] += learningRate * tdError * hidden[i];
        }
        biasOutput[lastAction] += learningRate * tdError;
        
        for (int j = 0; j < hiddenSize; j++) {
            if (hidden[j] > 0) { 
                double gradient = tdError * weightsHiddenOutput[j][lastAction];
                for (int i = 0; i < inputSize; i++) {
                    weightsInputHidden[i][j] += learningRate * gradient * lastInputs[i];
                }
                biasHidden[j] += learningRate * gradient;
            }
        }
    }
    
    private double getMax(double[] arr) {
        double max = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > max) max = arr[i];
        }
        return max;
    }
    
    public void save(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static NeuralNetwork load(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            NeuralNetwork nn = (NeuralNetwork) ois.readObject();
            nn.random = new Random();
            return nn;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
}
