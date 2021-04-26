
package DetectLight;

import edu.cmu.ri.createlab.terk.robot.finch.Finch;
import java.awt.Color;
import java.util.ArrayList;
import java.util.stream.IntStream;

public class DemoFinch extends Finch{
    
    private ArrayList<int[]> lightReadingList;
    private long startTime;
    private int lightDetectinCount;
    
    private final float offSetError = 0.8f;
    private int[] ambientLight;
    private int[] lightOffSet = {0,0};
    private int[] currentAdjustedLight = {0, 0};
    private final int threshold = 10; 
    private final int gap = 15; 
    private final int slowVelocity = 100;
    private final int normalVelocity = 150;
    private int fourSecondCounter = 0;
    
    public DemoFinch() {
        this.lightReadingList = new ArrayList<int[]>();
        this.startTime = System.currentTimeMillis();
        this.lightDetectinCount = 0;
        this.resetAmbientLight();
    }
    
    public int[] resetAmbientLight(){
        this.ambientLight = getLightSensors();
        this.calculateLightOffSet();
        return ambientLight;
    }

    
    
    public int[] getAmbientLight() {
        this.ambientLight = getLightSensors();
        return ambientLight;
    }

    public int[] getLightOffSet() {
        return lightOffSet;
    }

    public void calculateLightOffSet() {
        this.lightOffSet[0] = Math.round(this.ambientLight[0] * offSetError);
        this.lightOffSet[1] = Math.round(this.ambientLight[1] * offSetError);
    }
    
    public void moveSlowly(){
        System.out.println(this.slowVelocity);
        this.setWheelVelocities(this.slowVelocity, this.slowVelocity);
    }
    
    public void moveNormal(){
        System.out.println(this.normalVelocity);
        this.setWheelVelocities(this.normalVelocity, this.normalVelocity);
    }
    
    public int[] calculateAdjustedLight() {
        this.currentAdjustedLight[0] =  this.getLeftLightSensor() - this.lightOffSet[0];
        this.currentAdjustedLight[1] =  this.getRightLightSensor() - this.lightOffSet[1];
        this.lightReadingList.add(this.getLightSensors());
        return this.currentAdjustedLight;
    }
    
    public int[] getAdjustedLight() {
        return this.currentAdjustedLight;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getFourSecondCounter() {
        return fourSecondCounter;
    }

    public void incrementFourSecondCounter(int add) {
        this.fourSecondCounter += add;
    }
    
    public void resetFourSecondCounter() {
        this.fourSecondCounter = 0;
    }
    
    public int checkAdjustedLight() {
        int result = 0;
        this.calculateAdjustedLight();
        int difference = this.currentAdjustedLight[1] - this.currentAdjustedLight[0];
        System.out.printf("Difference: %d\n", difference);
        System.out.printf("Light Offset: %d\t%d\n", this.lightOffSet[0], this.lightOffSet[1]);
        System.out.printf("Light current reading: %d\t%d\n", this.getLeftLightSensor(), this.getRightLightSensor());
        System.out.printf("Light adjusted reading: %d\t%d\n", this.currentAdjustedLight[0], this.currentAdjustedLight[1]);
        if (Math.abs(difference) > this.gap){
            result = difference;
        }
        System.out.printf("Differencce: %d\n", difference);
        System.out.printf("result: %d\n", result);
        return result;
    }
    
    public void adjustDirection(){
        int left = 0;
        int right = 0;
        int difference = this.checkAdjustedLight();
        
        while (difference != 0 && this.isBeakUp() != true) {
            difference = this.checkAdjustedLight();
            System.out.printf("Differencce: %d\n", difference);
            if (difference > 0) {
                left = (difference*5);
                left = (left > 255) ? 255 : left;
                right = 25;
            }
            else if (difference < 0) {
                left = 25;
                right = (Math.abs(difference)*5);
                right = (right > 255) ? 255 : right;
            }
            System.out.printf("left: %d\tright: %d\n", left, right);
            this.setWheelVelocities(left, right);
            this.sleep(500);
        }
    }
    
    public void approachLight() {
       lightDetectinCount++; 
       while (this.checkThreshold() && this.isBeakUp() != true) {
           System.out.println("Inside approachLight");
           this.approachLightLedColour();
           this.adjustDirection();
           this.moveNormal();
           this.sleep(500);
       }  
       this.stopWheels();
       this.setLED(Color.yellow);
       this.resetAmbientLight();
       this.resetFourSecondCounter();
    }
    
    public void approachLightLedColour() {
        int intensity = (this.currentAdjustedLight[0] + this.currentAdjustedLight[0]) * 3;
        intensity = (intensity > 255) ? 255 : intensity;
        this.setLED(intensity,0,0);
    }
    
    public boolean checkThreshold() {
        this.calculateAdjustedLight();
        return (this.currentAdjustedLight[0] + this.currentAdjustedLight[1]) > this.threshold;
    }
    
    public void turnRightAngle() {
        this.setWheelVelocities(200, 50, 1000);
    }
    
    private int[] calculateLightAverage(){
        int left = 0;
        int right = 0;
        int[] averages = {0,0,0};
        int count = 0;
        for (int[] reading : this.lightReadingList) {
            left += reading[0];
            right += reading[1];
            count++;
        }
        
        averages[0] = left/count;
        averages[1] = right/count;
        averages[2] = (left+right)/(2*count);
        
        return averages;
    }
    
    private int[] calculateLightMinMax(){
        int[] lowest = this.lightReadingList.get(0); 
        int[] stats = {0,0,0,lowest[0],lowest[1],(lowest[0]+lowest[1])/2}; 
        for (int[] reading : this.lightReadingList) {
            stats[0] = (stats[0] < reading[0]) ? reading[0] : stats[0]; 
            stats[1] = (stats[1] < reading[1]) ? reading[1] : stats[1];
            stats[2] = (stats[2] < (reading[0] + reading[1])) ? (reading[0] + reading[1]) : stats[2];
            stats[3] = (stats[3] > reading[0]) ? reading[0] : stats[3]; 
            stats[4] = (stats[4] > reading[1]) ? reading[1] : stats[4];
            stats[5] = (stats[2] > (reading[0] + reading[1])) ? (reading[0] + reading[1]) : stats[5];
        }
        
        return stats;
    }

    public void printLog(){
        int[] initialLightReading = this.lightReadingList.get(0);
        int[] averages = this.calculateLightAverage();
        int[] minMax = this.calculateLightMinMax();
        long executionTime = System.currentTimeMillis() - this.startTime;
        
        System.out.println("\tInitial\t\t\tHighest\t\t\tLowest\t\t\tAverages\tDuration\tCount");
        System.out.println("Left\t\tRight\tLeft\tRight\tBoth\tLeft\tRight\tBoth\tLeft\tRight\tBoth\t(ms)");
        System.out.printf("%d\t\t%d\t", initialLightReading[0], initialLightReading[1]);
        System.out.printf("%d\t%d\t%d\t%d\t%d\t%d\t", minMax[0],minMax[1],minMax[2],minMax[3],minMax[4],minMax[5]);
        System.out.printf("%d\t%d\t%d\t", averages[0], averages[1], averages[2]);
        System.out.printf("%d\t", executionTime);
        System.out.printf("\t%d\n", this.lightDetectinCount);
        
    }
}
