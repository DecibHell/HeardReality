package com.pchauvet.heardreality.fragments;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.maps.model.LatLng;
import com.pchauvet.heardreality.AudioProcess;
import com.pchauvet.heardreality.OffTriggerRunnable;
import com.pchauvet.heardreality.OnTriggerRunnable;
import com.pchauvet.heardreality.R;
import com.pchauvet.heardreality.Utils;
import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.Range;
import com.pchauvet.heardreality.objects.Sound;
import com.pchauvet.heardreality.objects.Source;
import com.pchauvet.heardreality.objects.Trigger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayingProjectFragment extends Fragment {

    private HeardProject project;

    private RadioGroup mTabs;

    private FragmentTransaction fragmentTransaction;
    private FragmentManager mFragmentManager;

    private Location ref;

    private List<Sound> triggeredOnce;
    private Map<Sound, Thread> runningThreads;
    private Map<Sound, Thread> killingThreads;

    public PlayingProjectFragment (HeardProject project, Location ref){
        this.project = project;
        this.ref = ref;
        triggeredOnce = new ArrayList<>();
        runningThreads = new HashMap<>();
        killingThreads = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playing_project, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFragmentManager = getChildFragmentManager();

        initFragments();

        mTabs = view.findViewById(R.id.ppf_tabs);
        mTabs.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.ppf_tab_text) {
                openFragment("text");
                closeFragment("map");
            } else{
                openFragment("map");
                closeFragment("text");
            }
        });

        // Starting audio Threads
        for (Sound sound : project.getSounds()){
            // Find the source and the trigger
            Trigger onTrigger = sound.getOnTrigger();
            Trigger offTrigger = sound.getOffTrigger();

            if (onTrigger.getType().equals("TIME_AFTER_START")) {
                float[] coordinates = new float[3];
                if (sound.getSource() != null){
                    Source source = project.getSourceById(sound.getSource());
                    coordinates = getCoordinates(Utils.getLocationFromGeoPoint(source.getPosition()), source.getAltitude());
                }
                Thread thread = new Thread(new OnTriggerRunnable(onTrigger.getDelay(), project, sound, coordinates, runningThreads));
                runningThreads.put(sound, thread);
                thread.start();
            }

            if (offTrigger != null && offTrigger.getType().equals("TIME_AFTER_START")) {
                Thread thread = new Thread(new OffTriggerRunnable(offTrigger.getDelay(), project, sound, runningThreads, killingThreads));
                killingThreads.put(sound, thread);
                thread.start();
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        for (Object thread : runningThreads.values().toArray()){
            ((Thread)thread).interrupt();
        }
        for (Object thread : killingThreads.values().toArray()){
            ((Thread)thread).interrupt();
        }
        AudioProcess.stopAllSounds();
        AudioProcess.unloadAllSounds(requireContext());
    }

    public void onUserMoved(Location newLocation){
        // Move the user in the audio space
        float[] coordinates = getCoordinates(newLocation, 0);
        AudioProcess.moveListener(coordinates[0], coordinates[1], coordinates[2]);

        // Check if some ranges have been triggered
        for (Sound sound : project.getSounds()){
            Trigger onTrigger = sound.getOnTrigger();
            Trigger offTrigger = sound.getOffTrigger();

            // Check that the trigger is a RANGE trigger, that it hasn't been executed yet for a trigger with onlyOnce=true, and that it isn't already playing or waiting
            if (onTrigger.getType().equals("RANGE") && !(onTrigger.isOnlyOnce() && triggeredOnce.contains(sound)) && !(runningThreads.containsKey(sound) || AudioProcess.isSoundPlaying(project, sound))){
                Range range = project.getRangeById(onTrigger.getTrigger().toString());
                // Detect user in range
                if (range != null && range.isLatLngInRange(Utils.getLatLngFromLocation(newLocation))) {
                    // Remember this has been triggered
                    triggeredOnce.add(sound);
                    // Find the coordinates of the source if there is one
                    if (sound.getSource() != null){
                        Source source = project.getSourceById(sound.getSource());
                        coordinates = getCoordinates(Utils.getLocationFromGeoPoint(source.getPosition()), source.getAltitude());
                    }
                    // Create the Thread
                    Thread thread = new Thread(new OnTriggerRunnable(onTrigger.getDelay(), project, sound, coordinates, runningThreads));
                    runningThreads.put(sound, thread);
                    thread.start();
                }
            }
            // Check that there is an offTrigger, that it's a RANGE trigger, that the sound is running or waiting, and that the killer isn't already running
            if (offTrigger != null && offTrigger.getType().equals("RANGE") && (runningThreads.containsKey(sound) || AudioProcess.isSoundPlaying(project, sound)) && !killingThreads.containsKey(sound)){
                Range range = project.getRangeById(offTrigger.getTrigger().toString());
                // Detect user in range
                if (range != null && range.isLatLngInRange(Utils.getLatLngFromLocation(newLocation))) {
                    Thread thread = new Thread(new OffTriggerRunnable(offTrigger.getDelay(), project, sound, runningThreads, killingThreads));
                    killingThreads.put(sound, thread);
                    thread.start();
                }
            }
        }
    }

    public void initFragments(){
        fragmentTransaction = mFragmentManager.beginTransaction();
        // Init the Text Fragment
        Fragment fragment = new PlayingProjectTextFragment(project);
        fragmentTransaction.add(R.id.ppf_fragment, fragment, "text");
        // Detach it
        fragmentTransaction.hide(fragment);
        // Init the Map Fragment
        fragmentTransaction.add(R.id.ppf_fragment,  new PlayingProjectMapFragment(project), "map");
        fragmentTransaction.commit();
    }

    public void openFragment(String type){
        fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.show(mFragmentManager.findFragmentByTag(type));
        fragmentTransaction.commit();
    }

    public void closeFragment(String type){
        fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.hide(mFragmentManager.findFragmentByTag(type));
        fragmentTransaction.commit();
    }

    private float[] getCoordinates(Location position, float extraAltitude){
        float[] coordinates = new float[3];

        Location axisDifference = new Location("");

        // Compares latitudes
        axisDifference.setLatitude(position.getLatitude());
        axisDifference.setLongitude(ref.getLongitude());
        coordinates[1] = ref.distanceTo(axisDifference);
        // Coordinate should be negative if the position has a lower latitude (is more west) than the reference.
        if(position.getLatitude() < ref.getLatitude()){
            coordinates[1] = -coordinates[1];
        }

        // Compares longitudes
        axisDifference.setLatitude(ref.getLatitude());
        axisDifference.setLongitude(position.getLongitude());
        coordinates[0] = ref.distanceTo(axisDifference);
        // Coordinate should be negative if the position has a lower longitude (is more south) than the reference.
        if(position.getLongitude() < ref.getLongitude()){
            coordinates[0] = -coordinates[0];
        }

        // Compares altitudes
        if(position.hasAltitude()){
            axisDifference.setLatitude(ref.getLatitude());
            axisDifference.setLongitude(ref.getLongitude());
            axisDifference.setAltitude(position.getAltitude());
            coordinates[2] = ref.distanceTo(axisDifference);
            // Coordinate should be negative if the position has a lower altitude than the reference.
            if(position.getAltitude() < ref.getAltitude()){
                coordinates[2] = -coordinates[2];
            }
            coordinates[2] += extraAltitude;
        } else {
            coordinates[2] = extraAltitude;
        }
        return coordinates;
    }
}