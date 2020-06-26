package com.pchauvet.heardreality;

import android.content.Context;
import android.util.Log;

import com.google.vr.sdk.audio.GvrAudioEngine;
import com.pchauvet.heardreality.MathUtils.Quaternion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class AudioProcess {
    private GvrAudioEngine mAudioEngine;

    private Map<String, Integer> mLoadedSounds;  // Maps file names to IDs in the audio engine

    public AudioProcess(Context context){
        mAudioEngine = new GvrAudioEngine(context, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);
        mLoadedSounds = new HashMap<>();
    }

    public void playFile(final File file, final float x, final float y, final float z, final boolean looping){
        final String path = file.getAbsolutePath();
        new Thread(
            new Runnable() {
                @Override
                public void run() {
                    mAudioEngine.preloadSoundFile(path);
                    Log.v( "", "Preloaded sound");
                    // Create the 'sourceId' sound object and saves it in the Map
                    int sourceId = mAudioEngine.createSoundObject(path);
                    mLoadedSounds.put(path, sourceId);
                    // Set the sound object position
                    mAudioEngine.setSoundObjectPosition(sourceId, x, y, z);
                    // Start audio playback of the sound object sound file
                    mAudioEngine.playSound(sourceId, looping);
                }
            })
            .start();
    }

    public void rotateHead(Quaternion orientation){
        mAudioEngine.setHeadRotation(orientation.x, orientation.y, orientation.z, orientation.w);
        mAudioEngine.update();
    }

    public void moveListener(float x, float y, float z){
        mAudioEngine.setHeadPosition(x,y,z);
    }

    // X = EAST, Y = NORTH, Z = UP
    public void moveSound(String file, float x, float y, float z){
        Integer id = mLoadedSounds.get(file);
        if(id != null && mAudioEngine.isSourceIdValid(id)){
            mAudioEngine.setSoundObjectPosition(id, x, y, z);
        }
    }

    public void pause(){
        mAudioEngine.pause();
    }

    public void resume(){
        mAudioEngine.resume();
    }
}
