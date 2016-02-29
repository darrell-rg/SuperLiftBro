package org.superliftbro;

//import processing.core.PApplet;

import org.influxdb.dto.BatchPoints;
import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;


import KinectPV2.KJoint;
import KinectPV2.*;
import org.influxdb.*;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;



public class Arm extends PApplet {

    /**
     * Arm.
     *
     * The angle of each segment is controlled with the mouseX and
     * mouseY position. The transformations applied to the first segment
     * are also applied to the second segment because they are inside
     * the same pushMatrix() and popMatrix() group.
     */




    KinectPV2 kinect;
    InfluxDB influxDB;




    java.lang.String JointNames[] = {
            "SpineBase",
            "SpineMid",
            "Neck",
            "Head",
            "ShoulderLeft",
            "ElbowLeft",
            "WristLeft",
            "HandLeft",
            "ShoulderRight",
            "ElbowRight",
            "HandRight",
            "HipLeft",
            "KneeLeft",
            "AnkleLeft",
            "FootLeft",
            "HipRight",
            "KneeRight",
            "AnkleRight",
            "FootRight",
            "SpineShoulder",
            "HandTipLeft",
            "ThumbLeft",
            "HandTipRight",
            "ThumbRight"
    };


    public class Producer implements Runnable{

        private BlockingQueue queue;

        public Producer(BlockingQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run() {

            // We are adding elements using offer() in order to check if
            // it actually managed to insert them.
            for (int i = 0; i < 8; i++) {
                System.out.println("Trying to add to queue: String " + i +
                        " and the result was " + queue.offer("String " + i));

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public class Consumer implements Runnable{

        private BlockingQueue queue;

        public Consumer(BlockingQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run() {

            // As long as there are empty positions in our array,
            // we want to check what's going on.
            while (queue.remainingCapacity() > 0) {
                System.out.println("Queue size: " + queue.size() +
                        ", remaining capacity: " + queue.remainingCapacity());

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    org.influxdb.dto.Point makePoint(KJoint[] joints , String jointType, long time){

        int jointIndex = 0;
        try {
            jointIndex = KinectPV2.class.getField("JointType_"+jointType).getInt(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        org.influxdb.dto.Point point1 = org.influxdb.dto.Point.measurement(jointType)
                .time(time, java.util.concurrent.TimeUnit.MILLISECONDS)
                .field("x", joints[jointIndex].getX())
                .field("y",joints[jointIndex].getY())
                .field("z", joints[jointIndex].getZ())
                .build();

        return point1;
    }



    void logToInflux(ArrayList<KSkeleton> skeletonArray){
        //print("connecting");
        //InfluxDB influxDB = InfluxDBFactory.connect("https://gigawatt-mcfly-77.c.influxdb.com:8083", "SuperLiftBro", "squatbooty");

        long time = System.currentTimeMillis();
        //individual JOINTS
        for (int i = 0; i < skeletonArray.size(); i++) {
            KSkeleton skeleton = (KSkeleton) skeletonArray.get(i);
            if (skeleton.isTracked()) {
                KJoint[] joints = skeleton.getJoints();
                BatchPoints batchPoints = BatchPoints
                        .database("SuperLiftBro")
                        //.tag("async", "true")
                        //.retentionPolicy("default")
                        //.consistency(org.influxdb.InfluxDB.ConsistencyLevel.ONE)
                        .build();
                for (int j = 0; j < KinectPV2.JointType_Count; i++) {

                    batchPoints.point(makePoint(joints,JointNames[j],time));
                }
                try{
                    influxDB.write(batchPoints);
                }
                catch(Exception e){

                    println("Exception: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
                //println("Write to inlfux ok");
            }
        }
    }



    //DRAW BODY
    void drawBody(KJoint[] joints) {
        pushMatrix();

        drawBone(joints, KinectPV2.JointType_Head, KinectPV2.JointType_Neck);
        drawBone(joints, KinectPV2.JointType_Neck, KinectPV2.JointType_SpineShoulder);
        drawBone(joints, KinectPV2.JointType_SpineShoulder, KinectPV2.JointType_SpineMid);
        drawBone(joints, KinectPV2.JointType_SpineMid, KinectPV2.JointType_SpineBase);
        drawBone(joints, KinectPV2.JointType_SpineShoulder, KinectPV2.JointType_ShoulderRight);
        drawBone(joints, KinectPV2.JointType_SpineShoulder, KinectPV2.JointType_ShoulderLeft);
        drawBone(joints, KinectPV2.JointType_SpineBase, KinectPV2.JointType_HipRight);
        drawBone(joints, KinectPV2.JointType_SpineBase, KinectPV2.JointType_HipLeft);

        // Right Arm
        drawBone(joints, KinectPV2.JointType_ShoulderRight, KinectPV2.JointType_ElbowRight);
        drawBone(joints, KinectPV2.JointType_ElbowRight, KinectPV2.JointType_WristRight);
        drawBone(joints, KinectPV2.JointType_WristRight, KinectPV2.JointType_HandRight);
        drawBone(joints, KinectPV2.JointType_HandRight, KinectPV2.JointType_HandTipRight);
        drawBone(joints, KinectPV2.JointType_WristRight, KinectPV2.JointType_ThumbRight);

        // Left Arm
        drawBone(joints, KinectPV2.JointType_ShoulderLeft, KinectPV2.JointType_ElbowLeft);
        drawBone(joints, KinectPV2.JointType_ElbowLeft, KinectPV2.JointType_WristLeft);
        drawBone(joints, KinectPV2.JointType_WristLeft, KinectPV2.JointType_HandLeft);
        drawBone(joints, KinectPV2.JointType_HandLeft, KinectPV2.JointType_HandTipLeft);
        drawBone(joints, KinectPV2.JointType_WristLeft, KinectPV2.JointType_ThumbLeft);

        // Right Leg
        drawBone(joints, KinectPV2.JointType_HipRight, KinectPV2.JointType_KneeRight);
        drawBone(joints, KinectPV2.JointType_KneeRight, KinectPV2.JointType_AnkleRight);
        drawBone(joints, KinectPV2.JointType_AnkleRight, KinectPV2.JointType_FootRight);

        // Left Leg
        drawBone(joints, KinectPV2.JointType_HipLeft, KinectPV2.JointType_KneeLeft);
        drawBone(joints, KinectPV2.JointType_KneeLeft, KinectPV2.JointType_AnkleLeft);
        drawBone(joints, KinectPV2.JointType_AnkleLeft, KinectPV2.JointType_FootLeft);

        drawJoint(joints, KinectPV2.JointType_HandTipLeft);
        drawJoint(joints, KinectPV2.JointType_HandTipRight);
        drawJoint(joints, KinectPV2.JointType_FootLeft);
        drawJoint(joints, KinectPV2.JointType_FootRight);

        drawJoint(joints, KinectPV2.JointType_ThumbLeft);
        drawJoint(joints, KinectPV2.JointType_ThumbRight);

        drawJoint(joints, KinectPV2.JointType_Head);
        popMatrix();
    }

    //draw joint
    void drawJoint(KJoint[] joints, int jointType) {
        pushMatrix();
        translate(joints[jointType].getX(), joints[jointType].getY(), joints[jointType].getZ());
        ellipse(0, 0, 25, 25);
        popMatrix();
    }

    //draw bone
    void drawBone(KJoint[] joints, int jointType1, int jointType2) {
        pushMatrix();
        translate(joints[jointType1].getX(), joints[jointType1].getY(), joints[jointType1].getZ());
        ellipse(0, 0, 25, 25);
        popMatrix();
        line(joints[jointType1].getX(), joints[jointType1].getY(), joints[jointType1].getZ(), joints[jointType2].getX(), joints[jointType2].getY(), joints[jointType2].getZ());
    }

    //draw hand state
    void drawHandState(KJoint joint) {
        noStroke();
        handState(joint.getState());
        pushMatrix();
        translate(joint.getX(), joint.getY(), joint.getZ());
        ellipse(0, 0, 70, 70);
        popMatrix();
    }

    /*
    Different hand state
     KinectPV2.HandState_Open
     KinectPV2.HandState_Closed
     KinectPV2.HandState_Lasso
     KinectPV2.HandState_NotTracked
     */
    void handState(int handState) {
        switch(handState) {
            case KinectPV2.HandState_Open:
                fill(0, 255, 0);
                break;
            case KinectPV2.HandState_Closed:
                fill(255, 0, 0);
                break;
            case KinectPV2.HandState_Lasso:
                fill(0, 0, 255);
                break;
            case KinectPV2.HandState_NotTracked:
                fill(255, 255, 255);
                break;
        }
    }


    public void setup() {
        size(1280, 720, P3D);

        kinect = new KinectPV2(this);

        kinect.enableSkeletonColorMap(true);
        kinect.enableColorImg(true);
        influxDB = InfluxDBFactory.connect("https://gigawatt-mcfly-77.c.influxdb.com:8086", "influxdb", "3c3029a818feb378");
        kinect.init();
        BlockingQueue queue = new ArrayBlockingQueue<>(5);

        // The two threads will access the same queue, in order
        // to test its blocking capabilities.
        Thread producer = new Thread(new Producer(queue));
        Thread consumer = new Thread(new Consumer(queue));

        producer.start();
        consumer.start();

    }



    public void draw() {
        background(0);

        image(kinect.getColorImage(), 0, 0, width, height);

        ArrayList<KSkeleton> skeletonArray =  kinect.getSkeletonColorMap();
        logToInflux(skeletonArray);
        //logToInflux();
        //individual JOINTS
        for (int i = 0; i < skeletonArray.size(); i++) {
            KSkeleton skeleton = (KSkeleton) skeletonArray.get(i);
            if (skeleton.isTracked()) {
                KJoint[] joints = skeleton.getJoints();

                int col  = skeleton.getIndexColor();
                fill(col);
                stroke(col);

                pushMatrix();
                scale(width/1920.0f);
                drawBody(joints);

                //draw different color for each hand state
                drawHandState(joints[KinectPV2.JointType_HandRight]);
                drawHandState(joints[KinectPV2.JointType_HandLeft]);
                popMatrix();
            }
        }

        fill(255, 0, 0);
        text(frameRate, 50, 50);
    }



    public void settings() {  size(1280, 720); }

    static public void main(String[] args) {
        PApplet.main(Arm.class.getName());
    }
}