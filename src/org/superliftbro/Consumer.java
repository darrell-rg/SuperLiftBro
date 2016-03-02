package org.superliftbro;

import KinectPV2.*;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

/**
 * Created by darrell on 3/1/2016.
 */
public class Consumer implements Runnable {

    private BlockingQueue queue;

    public Consumer(BlockingQueue queue) {
        this.queue = queue;
    }

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



    void logToInflux(ArrayList<KinectPV2.KSkeleton> skeletonArray){
        //print("connecting");
        //InfluxDB influxDB = InfluxDBFactory.connect("https://gigawatt-mcfly-77.c.influxdb.com:8083", "SuperLiftBro", "squatbooty");

        long time = System.currentTimeMillis();
        //individual JOINTS
        for (int i = 0; i < skeletonArray.size(); i++) {
            KinectPV2.KSkeleton skeleton = (KinectPV2.KSkeleton) skeletonArray.get(i);
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



    @Override
    public void run() {
        influxDB = InfluxDBFactory.connect("https://gigawatt-mcfly-77.c.influxdb.com:8086", "influxdb", "3c3029a818feb378");
        // As long as there are empty positions in our array,
        // we want to check what's going on.
//        while (queue.remainingCapacity() > 0) {
            while (True) {
                System.out.println("Queue size: " + queue.size() +
                        ", remaining capacity: " + queue.remainingCapacity());

                try {
//                Thread.sleep(500);
                    logToInflux((ArrayList<KinectPV2.KSkeleton>)queue.take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
