package com.pchauvet.heardreality.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.os.Bundle;
import android.os.Looper;
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
import android.widget.Toast;

import com.pchauvet.heardreality.AuthManager;
import com.pchauvet.heardreality.FirestoreManager;
import com.pchauvet.heardreality.MathUtils.EulerAngles;
import com.pchauvet.heardreality.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

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
        mValidateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                AuthManager.signIn(email, password,new Thread(){
                    @Override
                    // ON A SUCCESSFUL CREATION
                    public void run(){
                        FirestoreManager.gatherData(onGatherDataCompleted());
                    }
                }, new Thread(){
                    @Override
                    // ON A FAILURE
                    public void run(){
                        mWaitingScreen.dismiss();
                        mErrorText.setText(R.string.login_error);
                    }
                });
            }
        });

        mSignupButton = dialogView.findViewById(R.id.login_signup_button);
        mSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignupFragment.show(getParentFragmentManager(),null);
            }
        });

        mGuestButton = dialogView.findViewById(R.id.login_guest_button);
        mGuestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWaitingScreen = new WaitingScreen();
                mWaitingScreen.show(getParentFragmentManager(), null);
                FirestoreManager.gatherData(onGatherDataCompleted());
            }
        });

        this.setCancelable(false);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        int width = getResources().getDimensionPixelSize(R.dimen.dialog_small_width);
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        return dialog;
    }

    private Thread onGatherDataCompleted(){
        return new Thread(){
            @Override
            // ON A SUCCESSFUL GATHERING OF DATABASE ELEMENTS
            public void run(){
            mWaitingScreen.dismiss();
            dismiss();
            }
        };
    }

}
