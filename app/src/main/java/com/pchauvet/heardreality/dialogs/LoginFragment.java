package com.pchauvet.heardreality.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.pchauvet.heardreality.AuthManager;
import com.pchauvet.heardreality.FirestoreManager;
import com.pchauvet.heardreality.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class LoginFragment  extends DialogFragment {

    private SignupFragment mSignupFragment;

    private TextView mErrorText;

    private EditText mEmailEdit;
    private EditText mPasswordEdit;

    private Button mValidateButton;
    private Button mSignupButton;
    private Button mGuestButton;

    private WaitingScreen mWaitingScreen;

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireContext());
        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_login, null);

        mSignupFragment = new SignupFragment();

        mErrorText = dialogView.findViewById(R.id.login_error);

        mEmailEdit = dialogView.findViewById(R.id.login_email);
        mPasswordEdit = dialogView.findViewById(R.id.login_password);

        mValidateButton = dialogView.findViewById(R.id.login_validate_button);
        mValidateButton.setOnClickListener(v -> {
            String email = mEmailEdit.getText().toString();
            String password = mPasswordEdit.getText().toString();
            if(email.isEmpty()){
                // MISSING EMAIL
                mErrorText.setText(R.string.missing_email);
                mEmailEdit.setHintTextColor(getResources().getColor(R.color.colorError, null));
                return;
            }
            if (password.isEmpty()) {
                //MISSING PASSWORD
                mErrorText.setText(R.string.missing_password);
                mPasswordEdit.setHintTextColor(getResources().getColor(R.color.colorError, null));
                return;
            }

            mWaitingScreen = new WaitingScreen();
            mWaitingScreen.show(getParentFragmentManager(), null);
            AuthManager.signIn(email, password, () -> {
                FirestoreManager.gatherData(onGatherDataCompleted());
            },() -> {
                mWaitingScreen.dismiss();
                mErrorText.setText(R.string.login_error);
            });
        });

        mSignupButton = dialogView.findViewById(R.id.login_signup_button);
        mSignupButton.setOnClickListener(v -> mSignupFragment.show(getParentFragmentManager(),null));

        mGuestButton = dialogView.findViewById(R.id.login_guest_button);
        mGuestButton.setOnClickListener(v -> {
            mWaitingScreen = new WaitingScreen();
            mWaitingScreen.show(getParentFragmentManager(), null);
            FirestoreManager.gatherData(onGatherDataCompleted());
        });

        this.setCancelable(false);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        int width = getResources().getDimensionPixelSize(R.dimen.dialog_small_width);
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        return dialog;
    }

    private Runnable onGatherDataCompleted(){
        return () -> {
            mWaitingScreen.dismiss();
            dismiss();
        };
    }

}
