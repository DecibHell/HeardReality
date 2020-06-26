package com.pchauvet.heardreality;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Callable;

import androidx.annotation.NonNull;

import static android.content.ContentValues.TAG;

public class AuthManager {
    public static FirebaseUser currentUser;

    public static String lastError;

    public static FirebaseAuth mAuth(){
        return FirebaseAuth.getInstance();
    };

    public static void createUser(String email, String password, final Thread onSuccess, final Thread onFailure){
        mAuth().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            currentUser = mAuth().getCurrentUser();
                            onSuccess.start();
                        } else {
                            Log.e(TAG, "createUserWithEmail:failure", task.getException());
                            lastError = task.getException().getMessage();
                            currentUser = null;
                            onFailure.start();
                        }
                    }
                });
    }

    public static void signIn(String email, String password, final Thread onSuccess, final Thread onFailure){
        mAuth().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            currentUser = mAuth().getCurrentUser();
                            onSuccess.start();
                        } else{
                            Log.e(TAG, "signInWithEmail:failure", task.getException());
                            lastError = task.getException().getMessage();
                            currentUser = null;
                            onFailure.start();
                        }
                    }
                });
    }

    public static void disconnect(){
        AuthManager.mAuth().signOut();
        currentUser = null;
    }

}
