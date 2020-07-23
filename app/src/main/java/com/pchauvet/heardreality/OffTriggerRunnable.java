package com.pchauvet.heardreality;

import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.Sound;

import java.util.Map;

public class OffTriggerRunnable implements Runnable {
    private long delay;
    private HeardProject project;
    private Sound sound;
    private Map<Sound, Thread> runningThreads;
    private Map<Sound, Thread> killingThreads;

    public OffTriggerRunnable(long delay, HeardProject project, Sound sound, Map<Sound, Thread> runningThreads, Map<Sound, Thread> killingThreads) {
        this.delay = delay;
        this.project = project;
        this.sound = sound;
        this.runningThreads = runningThreads;
        this.killingThreads = killingThreads;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        AudioProcess.stopSound(project, sound);
        Thread runningThread = runningThreads.remove(sound);
        if(runningThread != null){
            runningThread.interrupt();
        }
        killingThreads.remove(sound);
    }
}
