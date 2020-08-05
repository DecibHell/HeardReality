package com.pchauvet.heardreality;

import android.content.Context;
import android.util.Log;

import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.audio.GvrAudioSurround;
import com.pchauvet.heardreality.MathUtils.Quaternion;
import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.Sound;
import com.pchauvet.heardreality.objects.Source;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class AudioProcess {
    private static GvrAudioEngine mAudioEngine;

    private static Map<File, Integer> mLoadedSounds = new HashMap<>();  // Maps file names to IDs in the audio engine

    public static void initAudioEngine(Context context){
        mAudioEngine = new GvrAudioEngine(context, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
    }

    private static Thread preloadThread = new Thread();

    public static void preloadProject(final HeardProject project, Runnable onFinish){
        if(project.getSounds() != null && !project.getSounds().isEmpty()) {
            preloadThread = new Thread(() -> {
                for (Sound sound : project.getSounds()) {
                    if(Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    File file = StorageManager.getSoundFile(project.getOwner(), project.getId(), sound.getSourceFile());
                    final String path = file.getAbsolutePath();
                    // Try to preload the file twice, then crash if none succeeded
                    if (!mAudioEngine.preloadSoundFile(path)){
                        if (!mAudioEngine.preloadSoundFile(path)){
                            Log.e("", "Error preloading the file");
                            return;
                        }
                    }

                    if(Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    // Create the 'sourceId' sound object and saves it in the Map
                    int sourceId;
                    if (sound.getSource() == null){
                        sourceId = mAudioEngine.createStereoSound(path);
                    } else {
                        sourceId = mAudioEngine.createSoundObject(path);
                    }
                    mLoadedSounds.put(file, sourceId);
                }
                Log.v("", "Preloaded all sounds");
                onFinish.run();
            });
            preloadThread.start();
        } else {
            onFinish.run();
        }
    }

    public static void unloadSound(HeardProject project, Sound sound){
        File file = StorageManager.getSoundFile(project.getOwner(), project.getId(), sound.getSourceFile());
        Integer id = mLoadedSounds.remove(file);
        mAudioEngine.unloadSoundFile(file.getAbsolutePath());
    }

    public static void unloadAllSounds(){
        // Stop the preloading thread
        preloadThread.interrupt();
        // Unload the sound files
        for (Object obj : mLoadedSounds.keySet().toArray()) {
            File file = (File) obj;
            Integer id = mLoadedSounds.remove(file);
            Log.e(id+"", file.getAbsolutePath());
            // Why doesn't this erase the files from memory?!?!
            mAudioEngine.stopSound(id);
            mAudioEngine.unloadSoundFile(file.getAbsolutePath());
        }
    }

    // X = EAST, Y = NORTH, Z = UP
    public static void playSound(HeardProject project, Sound sound, float x, float y, float z){
        File file = StorageManager.getSoundFile(project.getOwner(), project.getId(), sound.getSourceFile());
        Integer id = mLoadedSounds.get(file);
        if(id != null && mAudioEngine.isSourceIdValid(id)){
            Log.v(sound.getName(), "Now playing sound ");
            // Set the sound object position only if its source is defined
            Source source = project.getSourceById(sound.getSource());
            if (source != null) {
                mAudioEngine.setSoundObjectPosition(id, x, y, z);
                if(source.getDistanceModel() != null){
                    mAudioEngine.setSoundObjectDistanceRolloffModel(id, source.getDistanceModel(), source.getDistanceMin(), source.getDistanceMax());
                }
            }
            mAudioEngine.setSoundVolume(id, sound.getVolume());
            // Start audio playback of the sound object sound file
            mAudioEngine.playSound(id, sound.isLoop());

        } else {
            Log.e(sound.getName(), "Sound isn't loaded correctly");
        }
    }

    public static boolean isSoundPlaying(HeardProject project, Sound sound){
        File file = StorageManager.getSoundFile(project.getOwner(), project.getId(), sound.getSourceFile());
        Integer id = mLoadedSounds.get(file);
        return id!=null && mAudioEngine.isSoundPlaying(id);
    }

    public static void stopSound(HeardProject project, Sound sound){
        File file = StorageManager.getSoundFile(project.getOwner(), project.getId(), sound.getSourceFile());
        Integer id = mLoadedSounds.get(file);
        if(id != null && mAudioEngine.isSourceIdValid(id)){
            Log.v(sound.getName(), "Stopping sound");
            // Stop the audio playback
            mAudioEngine.stopSound(id);
        } else {
            Log.e(sound.getName(), "Sound can't be stopped if it doesn't exist");
        }
    }

    public static void stopAllSounds(){
        Log.v("", "Stopping all sounds");
        for (Integer id : mLoadedSounds.values()) {
            if (id != null && mAudioEngine.isSourceIdValid(id)) {
                // Stop the audio playback
                mAudioEngine.stopSound(id);
            }
        }
    }

    public static void rotateHead(Quaternion orientation){
        mAudioEngine.setHeadRotation(orientation.x, orientation.y, orientation.z, orientation.w);
        mAudioEngine.update();
    }

    // X = EAST, Y = NORTH, Z = UP
    public static void moveListener(float x, float y, float z){
        mAudioEngine.setHeadPosition(x,y,z);
    }

    // X = EAST, Y = NORTH, Z = UP
    public static void moveSound(File file, float x, float y, float z){
        Integer id = mLoadedSounds.get(file);
        if(id != null && mAudioEngine.isSourceIdValid(id)){
            mAudioEngine.setSoundObjectPosition(id, x, y, z);
        }
    }

    public static void pause(){
        mAudioEngine.pause();
    }

    public static void resume(){
        mAudioEngine.resume();
    }
}
