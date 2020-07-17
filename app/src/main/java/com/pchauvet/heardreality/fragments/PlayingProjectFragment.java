package com.pchauvet.heardreality.fragments;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import com.pchauvet.heardreality.R;
import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.ui.main.SectionsPagerAdapter;

public class PlayingProjectFragment extends Fragment {

    HeardProject project;

    public PlayingProjectFragment (HeardProject project){
        this.project = project;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.fragment_playing_project);
//        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
//        ViewPager viewPager = findViewById(R.id.view_pager);
//        viewPager.setAdapter(sectionsPagerAdapter);
//        TabLayout tabs = findViewById(R.id.tabs);
//        tabs.setupWithViewPager(viewPager);
    }
}