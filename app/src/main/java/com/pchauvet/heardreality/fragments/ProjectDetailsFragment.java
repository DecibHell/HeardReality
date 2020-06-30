package com.pchauvet.heardreality.fragments;

import android.location.Address;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.pchauvet.heardreality.FirestoreManager;
import com.pchauvet.heardreality.R;
import com.pchauvet.heardreality.StorageManager;
import com.pchauvet.heardreality.Utils;
import com.pchauvet.heardreality.dialogs.WaitingScreen;
import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.User;

import androidx.fragment.app.Fragment;

public class ProjectDetailsFragment  extends Fragment {

    private HeardProject project;

    private ImageButton closeButton;

    private TextView nameText;
    private TextView ownerText;
    private TextView locationText;
    private TextView durationText;
    private TextView genreText;
    private TextView descriptionText;
    private TextView downloadsText;

    private Button downloadButton;
    private Button startButton;
    private Button deleteButton;

    public ProjectDetailsFragment(HeardProject project){
        this.project = project;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_details, container, false);

        closeButton = view.findViewById(R.id.pdf_close);
        closeButton.setOnClickListener(v -> {
            for (Fragment fragment : getParentFragmentManager().getFragments()){
                if (fragment instanceof ProjectDetailsFragment){
                    getParentFragmentManager().beginTransaction().remove(fragment).commit();
                }
            }
        });

        nameText = view.findViewById(R.id.pdf_name);
        nameText.setText(project.getName());

        ownerText = view.findViewById(R.id.pdf_owner);
        User owner = FirestoreManager.getUser(project.getOwner());
        if(owner != null){
            ownerText.setText(getResources().getString(R.string.pdf_owner, owner.getName()));
        } else {
            ownerText.setVisibility(View.GONE);
        }

        locationText = view.findViewById(R.id.pdf_location);
        LatLng startingPoint = Utils.getProjectStartingPoint(project);
        if(startingPoint != null){
            Address address = Utils.getAddressFromLatLng(requireContext(), startingPoint);
            if(address!=null) {
                String locality = address.getLocality() == null ? "?" : address.getLocality();
                String country = address.getCountryName() == null ? "?" : address.getCountryName();
                locationText.setText(getResources().getString(R.string.pdf_location, locality, country));
            }
        }

        durationText = view.findViewById(R.id.pdf_duration);
        if(project.getDuration() > 0){
            durationText.setText(getResources().getString(R.string.pdf_duration, project.getDuration()));
        } else {
            durationText.setVisibility(View.GONE);
            view.findViewById(R.id.pdf_duration_icon).setVisibility(View.GONE);
        }

        genreText = view.findViewById(R.id.pdf_genre);
        if(project.getGenre() != null){
            genreText.setText(getResources().getString(R.string.pdf_genre, project.getGenre()));
        }else {
            genreText.setVisibility(View.GONE);
        }

        descriptionText = view.findViewById(R.id.pdf_description);
        if(project.getDescription() != null){
            descriptionText.setText(getResources().getString(R.string.pdf_description, project.getDescription()));
        }else {
            descriptionText.setVisibility(View.GONE);
        }

        downloadsText = view.findViewById(R.id.pdf_downloads);

        downloadsText.setText(getResources().getString(R.string.pdf_downloads, project.getDownloads()));

        downloadButton = view.findViewById(R.id.pdf_download);
        downloadButton.setOnClickListener(v -> {
            // Show a waiting screen
            final WaitingScreen mWaitingScreen = new WaitingScreen();
            mWaitingScreen.show(getParentFragmentManager(), null);
            // Try to download the project
            StorageManager.downloadProject(requireContext(), project.getId(), this::updateInterface, () -> Log.e("", "Error while downloading the project"), mWaitingScreen);
        });

        startButton = view.findViewById(R.id.pdf_start);
        startButton.setOnClickListener(v -> {

        });

        deleteButton = view.findViewById(R.id.pdf_delete);
        deleteButton.setOnClickListener(v -> {
            if(StorageManager.deleteProject(requireContext(), project.getId())){
                updateInterface();
            }
        });

        if(StorageManager.downloadedProjects.contains(project.getId())){
            downloadButton.setVisibility(View.GONE);
        } else {
            startButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }

        return view;
    }

    private void updateInterface(){
        getParentFragmentManager().beginTransaction().replace(R.id.wmf_project_details_placeholder, new ProjectDetailsFragment(project)).commit();
        for (Fragment fragment : getParentFragmentManager().getFragments()){
            if(fragment instanceof WorldMapFragment){
                ((WorldMapFragment) fragment).updateProjectList();
            }
        }
    }
}
