package com.pchauvet.heardreality;

import android.util.Log;

import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.Sound;

import java.util.Map;

public class OnTriggerRunnable implements Runnable {
    private long delay;
    private HeardProject project;
    private Sound sound;
    private float[] coordinates;
    private Map<Sound, Thread> runningThreads;

    public OnTriggerRunnable(long delay, HeardProject project, Sound sound, float[] coordinates, Map<Sound, Thread> runningThreads){
        this.delay = delay;
        this.project = project;
        this.sound = sound;
        this.coordinates = coordinates;
        this.runningThreads = runningThreads;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        AudioProcess.playSound(project, sound, coordinates[0], coordinates[1], coordinates[2]);
        runningThreads.remove(sound);
    }
}
