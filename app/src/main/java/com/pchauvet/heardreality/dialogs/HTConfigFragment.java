package com.pchauvet.heardreality.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.pchauvet.heardreality.MathUtils.EulerAngles;
import com.pchauvet.heardreality.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class HTConfigFragment extends DialogFragment{

    private String deviceName;

    private ImageButton mExitButton;
    private ImageButton mRenameButton;
    private ImageButton mConfirmName;
    private ImageButton mCancelName;

    private TextView mNameTextview;

    private Button mCalibrateButton;
    private Button mDisconnectButton;

    private ImageView mOrientationImage;
    private ImageView mElevationImage;

    private HTCalibrationFragment mCalibrationFragment;

    private float pitch = 0;
    private float yaw = 0;

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireContext());
        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_ht_config, null);

        mExitButton = dialogView.findViewById(R.id.ht_config_close);
        mExitButton.setOnClickListener(v -> dismiss());

        mRenameButton = dialogView.findViewById(R.id.ht_config_edit_name);
        mConfirmName = dialogView.findViewById(R.id.ht_config_confirm_name);
        mCancelName = dialogView.findViewById(R.id.ht_config_cancel_name);
        mNameTextview = dialogView.findViewById(R.id.ht_config_name);
        mNameTextview.setText(deviceName);

        mRenameButton.setOnClickListener(v -> {
            mNameTextview.setEnabled(true);
            mRenameButton.setVisibility(View.GONE);
            mConfirmName.setVisibility(View.VISIBLE);
            mCancelName.setVisibility(View.VISIBLE);

            mNameTextview.setText(null);
        });

        mConfirmName.setOnClickListener(v -> {
            mNameTextview.setEnabled(false);
            mRenameButton.setVisibility(View.VISIBLE);
            mConfirmName.setVisibility(View.GONE);
            mCancelName.setVisibility(View.GONE);

            deviceName = mNameTextview.getText().toString();

            final HTConfigFragmentListener listener = (HTConfigFragmentListener) requireActivity();
            listener.onNameChanged(deviceName);
        });

        mCancelName.setOnClickListener(v -> {
            mNameTextview.setEnabled(false);
            mRenameButton.setVisibility(View.VISIBLE);
            mConfirmName.setVisibility(View.GONE);
            mCancelName.setVisibility(View.GONE);

            mNameTextview.setText(deviceName);
        });

        mCalibrateButton = dialogView.findViewById(R.id.ht_config_calibrate);
        mCalibrateButton.setOnClickListener(v -> mCalibrationFragment.show(requireActivity().getSupportFragmentManager(),null));

        mDisconnectButton = dialogView.findViewById(R.id.ht_config_disconnect);
        mDisconnectButton.setOnClickListener(v -> {
            final HTConfigFragmentListener listener = (HTConfigFragmentListener) requireActivity();
            listener.onDisconnectOrder();

            dismiss();
        });

        mOrientationImage = dialogView.findViewById(R.id.ht_orientation_image);
        mOrientationImage.setImageResource(R.drawable.orientation_static);
        mElevationImage = dialogView.findViewById(R.id.ht_elevation_image);
        mElevationImage.setImageResource(R.drawable.elevation_static);

        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        int width = getResources().getDimensionPixelSize(R.dimen.dialog_large_width);
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        mCalibrationFragment = new HTCalibrationFragment();

        return dialog;
    }

    public void setDeviceName(String name){
        deviceName = name;
    }

    public void onOrientationChanged(EulerAngles orientation){
        // PITCH
        Drawable[] layers = new Drawable[2];
        // Get the static drawable
        layers[0] = requireContext().getDrawable(R.drawable.elevation_static);
        RotateDrawable elevation_dynamic = new RotateDrawable();
        // Rotate the dynamic drawable
        elevation_dynamic.setFromDegrees(this.pitch);
        elevation_dynamic.setToDegrees(orientation.pitch);
        elevation_dynamic.setPivotXRelative(true);
        elevation_dynamic.setPivotYRelative(true);
        elevation_dynamic.setPivotX(0.5f);
        elevation_dynamic.setPivotY(0.5f);
        elevation_dynamic.setDrawable(requireContext().getDrawable(R.drawable.elevation_dynamic));
        elevation_dynamic.setLevel(1);
        // Add the dynamic drawable to the layers
        layers[1] = elevation_dynamic;
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        // Set the drawables in the image
        mElevationImage.setImageDrawable(layerDrawable);

        // YAW
        layers = new Drawable[2];
        // Get the static drawable
        layers[0] = requireContext().getDrawable(R.drawable.orientation_static);
        RotateDrawable orientation_dynamic = new RotateDrawable();
        // Rotate the dynamic drawable
        orientation_dynamic.setFromDegrees(this.yaw);
        orientation_dynamic.setToDegrees(orientation.yaw);
        orientation_dynamic.setPivotXRelative(true);
        orientation_dynamic.setPivotYRelative(true);
        orientation_dynamic.setPivotX(0.5f);
        orientation_dynamic.setPivotY(0.5f);
        orientation_dynamic.setDrawable(requireContext().getDrawable(R.drawable.orientation_dynamic));
        orientation_dynamic.setLevel(1);
        // Add the dynamic drawable to the layers
        layers[1] = orientation_dynamic;
        layerDrawable = new LayerDrawable(layers);
        // Set the drawables in the image
        mOrientationImage.setImageDrawable(layerDrawable);

        this.yaw = orientation.yaw;
        this.pitch = orientation.pitch;
    }

    public interface HTConfigFragmentListener {
        void onNameChanged(String name);

        void onDisconnectOrder();
    }
}
