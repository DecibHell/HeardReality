package com.pchauvet.heardreality;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.Sound;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;

public class StorageManager {

    public static List<String> downloadedProjects = new ArrayList<>();

    private static Context context;

    public static FirebaseStorage storage() {return FirebaseStorage.getInstance();}

    public static StorageReference storageRoot() {return storage().getReference().child("HeardProjects");}

    public static void init (Context context){
        StorageManager.context = context;
    }

    public static void downloadProject(final String projectId, final Thread onSuccess, final Thread onFailure){
        final HeardProject project = FirestoreManager.getProject(projectId);
        if(project.getSounds() != null) {
            String ownerId = project.getOwner();
            String project_path = project.isPublished() ? ownerId + "/" + projectId : ownerId + "/private_" + projectId;

            // Set the variables to keep track of the asynchronous download of the sounds
            final AtomicInteger processedSounds = new AtomicInteger(0);
            final AtomicBoolean gotNoFailure = new AtomicBoolean(true);
            final int totalSounds = project.getSounds().size();

            for (Sound sound : project.getSounds()) {
                String sourceFile = sound.  getSourceFile();
                // Create a new file in the app's folder
                File newFile = getSoundFile(ownerId, projectId, sourceFile);
                newFile.getParentFile().mkdirs();
                // Download the file from the Cloud Storage
                storageRoot().child(project_path).child(sourceFile)
                        .getFile(newFile)
                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                if (processedSounds.incrementAndGet() == totalSounds) {
                                    if (gotNoFailure.get()) {
                                        Log.v("", "Successfully downloaded all the sounds");
                                        downloadedProjects.add(projectId);
                                        onSuccess.start();
                                    } else {
                                        Log.e("", "Something went wrong while downloading the sounds");
                                        onFailure.start();
                                    }
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.v("", "Error while downloading a sound : " + e.getMessage());
                        gotNoFailure.set(false);
                    }
                });
            }
        } else {
            Log.v("", "No sounds to download");
            onSuccess.start();
        }
    }

    // Check which projects are downloaded by checking if all their sounds' source files are present on disk
    public static void checkDownloaded(){
        downloadedProjects.clear();
        for (HeardProject project : FirestoreManager.projects){
            if(project.getSounds() != null){
                boolean isDownloaded = true;
                for (Sound sound : project.getSounds()){
                    File sourceFile = getSoundFile(project.getOwner(), project.getId(), sound.getSourceFile());
                    isDownloaded = isDownloaded && sourceFile.exists();
                }
                if(isDownloaded){
                    downloadedProjects.add(project.getId());
                }
            }
        }
    }

    public static File getSoundFile (String ownerId, String projectId, String sourceFile){
        return new File(context.getFilesDir(), ownerId+"/"+projectId+"/"+sourceFile);
    }
}
