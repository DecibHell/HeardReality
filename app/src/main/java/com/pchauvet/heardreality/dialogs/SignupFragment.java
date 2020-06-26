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
import android.widget.Toast;

import com.pchauvet.heardreality.AuthManager;
import com.pchauvet.heardreality.FirestoreManager;
import com.pchauvet.heardreality.MathUtils.EulerAngles;
import com.pchauvet.heardreality.R;

import java.util.concurrent.Callable;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import static com.google.vr.cardboard.ThreadUtils.runOnUiThread;

public class SignupFragment extends DialogFragment {

    private TextView mErrorText;

    private EditText mEmailEdit;
    private EditText mPasswordEdit;
    private EditText mConfirmEdit;
    private EditText mUsernameEdit;

    private Button mValidateButton;
    private Button mCancelButton;

    private WaitingScreen mWaitingScreen;

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireContext());
        @SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_signup, null);

        mErrorText = dialogView.findViewById(R.id.signup_error);

        mEmailEdit = dialogView.findViewById(R.id.signup_email);
        mPasswordEdit = dialogView.findViewById(R.id.signup_password);
        mConfirmEdit = dialogView.findViewById(R.id.signup_confirm);
        mUsernameEdit = dialogView.findViewById(R.id.signup_username);

        mValidateButton = dialogView.findViewById(R.id.signup_validate_button);
        mValidateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmailEdit.getText().toString();
                final String password = mPasswordEdit.getText().toString();
                final String confirmation = mConfirmEdit.getText().toString();
                final String username = mUsernameEdit.getText().toString();

                if(email.isEmpty()){
                    // MISSING EMAIL
                    mErrorText.setText(R.string.missing_email);
                    mEmailEdit.setHintTextColor(getResources().getColor(R.color.colorError));
                    return;
                }
                if (password.isEmpty()) {
                    // MISSING PASSWORD
                    mErrorText.setText(R.string.missing_password);
                    mPasswordEdit.setHintTextColor(getResources().getColor(R.color.colorError));
                    return;
                }
                if (confirmation.isEmpty()) {
                    // MISSING CONFIRMATION
                    mErrorText.setText(R.string.missing_confirm);
                    mPasswordEdit.setHintTextColor(getResources().getColor(R.color.colorError));
                    return;
                }
                if (username.isEmpty()) {
                    // MISSING USERNAME
                    mErrorText.setText(R.string.missing_username);
                    mPasswordEdit.setHintTextColor(getResources().getColor(R.color.colorError));
                    return;
                }
                if (!password.equals(confirmation)) {
                    // PASSWORDS DON'T MATCH
                    mErrorText.setText(R.string.signup_wrong_match);
                    return;
                }

                mWaitingScreen = new WaitingScreen();
                mWaitingScreen.show(getParentFragmentManager(), null);
                FirestoreManager.isUsernameDuplicate(username, new Thread(){
                    @Override
                    public void run(){
                        // DUPLICATE USERNAME
                        mWaitingScreen.dismiss();
                        mErrorText.setText(R.string.duplicate_username);
                    }
                }, new Thread(){
                    @Override
                    public void run(){
                        // CREATE A NEW USER
                        AuthManager.createUser(email, password, onCreateUserSuccess(username),onAuthManagerFailure());
                    }
                });
            }
        });

        mCancelButton = dialogView.findViewById(R.id.signup_cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        int width = getResources().getDimensionPixelSize(R.dimen.dialog_small_width);
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        return dialog;
    }

    private Thread onCreateUserSuccess(final String username) {
        return new Thread() {
            @Override
            // ON A SUCCESSFUL CREATION OF THE USER
            public void run() {
                FirestoreManager.createUser(username, onPersistUserSuccess(), onFirestoreManagerFailure());
            }
        };
    }

    private Thread onPersistUserSuccess() {
        return new Thread() {
            @Override
            // ON A SUCCESSFUL CREATION OF THE USER DOCUMENT IN DATABASE
            public void run() {
                FirestoreManager.gatherData(onGatherDataCompleted());
            }
        };
    }

    private Thread onGatherDataCompleted() {
        return new Thread() {
            @Override
            public void run() {
                // DISMISS ALL DIALOGS
                for (Fragment frag : requireActivity().getSupportFragmentManager().getFragments()) {
                    if (frag instanceof DialogFragment) {
                        ((DialogFragment) frag).dismiss();
                    }
                }
            }
        };
    }

    private Thread onFirestoreManagerFailure(){
        return new Thread(){
            @Override
            // ON A FAILURE
            public void run(){
                mWaitingScreen.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mErrorText.setText(FirestoreManager.lastError);
                    }
                });
            }
        };
    }

    private Thread onAuthManagerFailure() {
        return new Thread() {
            @Override
            // ON A FAILURE
            public void run() {
                mWaitingScreen.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mErrorText.setText(AuthManager.lastError);
                    }
                });
            }
        };
    }
}
