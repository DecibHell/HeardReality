package com.pchauvet.heardreality.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.pchauvet.heardreality.AudioProcess;
import com.pchauvet.heardreality.FirestoreManager;
import com.pchauvet.heardreality.MainActivity;
import com.pchauvet.heardreality.R;
import com.pchauvet.heardreality.objects.HeardProject;

import androidx.fragment.app.Fragment;

public class PlayingProjectTextFragment extends Fragment {

    private HeardProject project;

    private TextView title;
    private TextView description;

    private Button stopButton;

    public PlayingProjectTextFragment (HeardProject project){
        this.project = project;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playing_project_text, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        title = view.findViewById(R.id.pptf_title);
        title.setText(getString(R.string.ppf_now_playing, project.getName(), FirestoreManager.getUser(project.getOwner()).getName()));

        description = view.findViewById(R.id.pptf_description);
        description.setText(project.getDescription());

        stopButton = view.findViewById(R.id.pptf_stop);
        stopButton.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).openWorldMapFragment();
        });
    }
}
