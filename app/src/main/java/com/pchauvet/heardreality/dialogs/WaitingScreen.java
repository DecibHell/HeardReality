package com.pchauvet.heardreality.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.pchauvet.heardreality.AuthManager;
import com.pchauvet.heardreality.FirestoreManager;
import com.pchauvet.heardreality.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class WaitingScreen extends DialogFragment {

    private ImageView mRotatingIcon;

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireContext());
        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.waiting_screen, null);

        mRotatingIcon = dialogView.findViewById(R.id.ws_rotating_icon);
        Animation rotation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_refresh);
        rotation.setRepeatCount(Animation.INFINITE);
        mRotatingIcon.startAnimation(rotation);

        this.setCancelable(false);
        dialog.setContentView(dialogView);

        return dialog;
    }
}
