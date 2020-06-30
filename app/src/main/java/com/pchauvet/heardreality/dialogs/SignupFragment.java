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
        mValidateButton.setOnClickListener(v -> {
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
            FirestoreManager.isUsernameDuplicate(username, () -> {
                // DUPLICATE USERNAME
                mWaitingScreen.dismiss();
                mErrorText.setText(R.string.duplicate_username);
            }, () -> {
                // CREATE A NEW USER
                AuthManager.createUser(email, password, onCreateUserSuccess(username),onAuthManagerFailure());
            });
        });

        mCancelButton = dialogView.findViewById(R.id.signup_cancel_button);
        mCancelButton.setOnClickListener(v -> dismiss());

        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        int width = getResources().getDimensionPixelSize(R.dimen.dialog_small_width);
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        return dialog;
    }

    private Runnable onCreateUserSuccess(final String username) {
        return () -> FirestoreManager.createUser(username, onPersistUserSuccess(), onFirestoreManagerFailure());
    }

    private Runnable onPersistUserSuccess() {
        // ON A SUCCESSFUL CREATION OF THE USER DOCUMENT IN DATABASE
        return () -> FirestoreManager.gatherData(requireContext(), onGatherDataCompleted());
    }

    private Runnable onGatherDataCompleted() {
        return () -> {
            // DISMISS ALL DIALOGS
            for (Fragment frag : requireActivity().getSupportFragmentManager().getFragments()) {
                if (frag instanceof DialogFragment) {
                    ((DialogFragment) frag).dismiss();
                }
            }
        };
    }

    private Runnable onFirestoreManagerFailure(){
        return () -> {
            mWaitingScreen.dismiss();
            runOnUiThread(() -> mErrorText.setText(FirestoreManager.lastError));
        };
    }

    private Runnable onAuthManagerFailure() {
        return () -> {
            mWaitingScreen.dismiss();
            runOnUiThread(() -> mErrorText.setText(AuthManager.lastError));
        };
    }
}
