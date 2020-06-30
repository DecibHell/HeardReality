package com.pchauvet.heardreality.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.pchauvet.heardreality.R;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class WaitingScreen extends DialogFragment {

    private ImageView mRotatingIcon;

    private Map<String,Float> progress;         // Maps each task's id to its progress
    private Map<String,Float> progressGoal;
    private TextView progressText;

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireContext());
        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.waiting_screen, null);

        mRotatingIcon = dialogView.findViewById(R.id.ws_rotating_icon);
        Animation rotation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_refresh);
        rotation.setRepeatCount(Animation.INFINITE);
        mRotatingIcon.startAnimation(rotation);

        progress = new HashMap<>();
        progressGoal = new HashMap<>();
        progressText = dialogView.findViewById(R.id.ws_progress);

        this.setCancelable(false);
        dialog.setContentView(dialogView);

        return dialog;
    }

    public void setProgress(String key, float value){
        this.progress.put(key, value);
        displayProgress();
    }

    public void setProgressGoal(String key, float value){
        this.progressGoal.put(key, value);
    }

    private void displayProgress(){
        float progressSum = 0;
        for(Float value : progress.values()) {
            progressSum += value;
        }
        float progressGoalSum = 0;
        for(Float value : progressGoal.values()) {
            progressGoalSum += value;
        }
        progressText.setVisibility(View.VISIBLE);
        progressText.setText(getResources().getString(R.string.ws_progress, (int)(100*(progressSum/progressGoalSum))));
    }
}
