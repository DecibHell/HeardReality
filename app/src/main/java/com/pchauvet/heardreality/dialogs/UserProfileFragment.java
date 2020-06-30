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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.pchauvet.heardreality.AuthManager;
import com.pchauvet.heardreality.FirestoreManager;
import com.pchauvet.heardreality.MathUtils.EulerAngles;
import com.pchauvet.heardreality.R;
import com.pchauvet.heardreality.objects.User;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class UserProfileFragment extends DialogFragment {

    private ImageButton mExitButton;

    private EditText mUsername;
    private ImageButton mUsernameEdit;
    private ImageButton mUsernameConfirm;
    private ImageButton mUsernameCancel;

    private EditText mDescription;
    private ImageButton mDescriptionEdit;
    private ImageButton mDescriptionConfirm;
    private ImageButton mDescriptionCancel;

    private EditText mPassword;
    private EditText mConfirm;
    private Button mPasswordValidate;

    private Button mDisconnectButton;


    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireContext());
        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_user_profile, null);

        final String userId = AuthManager.currentUser.getUid();
        final User user = FirestoreManager.getUser(userId);

        mExitButton = dialogView.findViewById(R.id.upf_close);
        mExitButton.setOnClickListener(v -> dismiss());

        mUsername = dialogView.findViewById(R.id.upf_username);
        mUsernameEdit = dialogView.findViewById(R.id.upf_username_edit);
        mUsernameConfirm = dialogView.findViewById(R.id.upf_username_confirm);
        mUsernameCancel = dialogView.findViewById(R.id.upf_username_cancel);

        mUsername.setText(user.getName());

        mUsernameEdit.setOnClickListener(v -> {
            mUsername.setEnabled(true);
            mUsernameEdit.setVisibility(View.GONE);
            mUsernameConfirm.setVisibility(View.VISIBLE);
            mUsernameCancel.setVisibility(View.VISIBLE);
        });

        mUsernameConfirm.setOnClickListener(v -> {
            mUsername.setEnabled(false);
            mUsernameEdit.setVisibility(View.VISIBLE);
            mUsernameConfirm.setVisibility(View.GONE);
            mUsernameCancel.setVisibility(View.GONE);

            String username = mUsername.getText().toString();
            FirestoreManager.updateUser(userId, "name", username);
            user.setName(username);
        });

        mUsernameCancel.setOnClickListener(v -> {
            mUsername.setEnabled(false);
            mUsernameEdit.setVisibility(View.VISIBLE);
            mUsernameConfirm.setVisibility(View.GONE);
            mUsernameCancel.setVisibility(View.GONE);

            mUsername.setText(user.getName());
        });

        mDescription = dialogView.findViewById(R.id.upf_description);
        mDescriptionEdit = dialogView.findViewById(R.id.upf_description_edit);
        mDescriptionConfirm = dialogView.findViewById(R.id.upf_description_confirm);
        mDescriptionCancel = dialogView.findViewById(R.id.upf_description_cancel);

        mDescription.setText(user.getDescription());

        mDescriptionEdit.setOnClickListener(v -> {
            mDescription.setEnabled(true);
            mDescriptionEdit.setVisibility(View.GONE);
            mDescriptionConfirm.setVisibility(View.VISIBLE);
            mDescriptionCancel.setVisibility(View.VISIBLE);
        });

        mDescriptionConfirm.setOnClickListener(v -> {
            mDescription.setEnabled(false);
            mDescriptionEdit.setVisibility(View.VISIBLE);
            mDescriptionConfirm.setVisibility(View.GONE);
            mDescriptionCancel.setVisibility(View.GONE);

            String description = mDescription.getText().toString();
            FirestoreManager.updateUser(userId, "description", description);
            user.setDescription(description);
        });

        mDescriptionCancel.setOnClickListener(v -> {
            mDescription.setEnabled(false);
            mDescriptionEdit.setVisibility(View.VISIBLE);
            mDescriptionConfirm.setVisibility(View.GONE);
            mDescriptionCancel.setVisibility(View.GONE);

            mDescription.setText(user.getDescription());
        });

        mPassword = dialogView.findViewById(R.id.upf_confirm);
        mConfirm = dialogView.findViewById(R.id.upf_password);
        mPasswordValidate = dialogView.findViewById(R.id.upf_password_validate);

        mPasswordValidate.setOnClickListener(v -> {
            String password = mPassword.getText().toString();
            String confirm = mConfirm.getText().toString();
            if(!password.isEmpty() && password.equals(confirm)){
                AuthManager.currentUser.updatePassword(password);
                mPassword.setText(null);
                mConfirm.setText(null);
            }
        });

        mDisconnectButton = dialogView.findViewById(R.id.upf_disconnect);
        mDisconnectButton.setOnClickListener(v -> {
            AuthManager.disconnect();
            dismiss();
            new LoginFragment().show(requireActivity().getSupportFragmentManager(), null);
        });

        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(dialogView);
        Window window = dialog.getWindow();

        int width = getResources().getDimensionPixelSize(R.dimen.dialog_large_width);
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        return dialog;
    }

}
