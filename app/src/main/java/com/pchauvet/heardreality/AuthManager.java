package com.pchauvet.heardreality;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static android.content.ContentValues.TAG;

public class AuthManager {
    public static FirebaseUser currentUser;

    public static String lastError;

    public static FirebaseAuth mAuth(){
        return FirebaseAuth.getInstance();
    };

    public static void createUser(String email, String password, final Runnable onSuccess, final Runnable onFailure){
        mAuth().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        currentUser = mAuth().getCurrentUser();
                        onSuccess.run();
                    } else {
                        Log.e(TAG, "createUserWithEmail:failure", task.getException());
                        lastError = task.getException().getMessage();
                        currentUser = null;
                        onFailure.run();
                    }
                });
    }

    public static void signIn(String email, String password, final Runnable onSuccess, final Runnable onFailure){
        mAuth().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        currentUser = mAuth().getCurrentUser();
                        onSuccess.run();
                    } else{
                        Log.e(TAG, "signInWithEmail:failure", task.getException());
                        lastError = task.getException().getMessage();
                        currentUser = null;
                        onFailure.run();
                    }
                });
    }

    public static void disconnect(){
        AuthManager.mAuth().signOut();
        currentUser = null;
    }

}
