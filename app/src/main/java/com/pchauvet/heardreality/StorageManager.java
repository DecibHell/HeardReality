package com.pchauvet.heardreality;

import android.content.Context;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pchauvet.heardreality.dialogs.WaitingScreen;
import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.Sound;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageManager {

    public static List<String> downloadedProjects = new ArrayList<>();

    public static FirebaseStorage storage() {return FirebaseStorage.getInstance();}

    public static StorageReference storageRoot() {return storage().getReference().child("HeardProjects");}

    public static void downloadProject(Context context, final String projectId, final Runnable onSuccess, final Runnable onFailure, WaitingScreen ws){
        final HeardProject project = FirestoreManager.getProject(projectId);
        if(project.getSounds() != null && !project.getSounds().isEmpty()) {
            String ownerId = project.getOwner();
            String project_path = project.isPublished() ? ownerId + "/" + projectId : ownerId + "/private_" + projectId;

            // Set the variables to keep track of the asynchronous download of the sounds
            final AtomicInteger processedSounds = new AtomicInteger(0);
            final AtomicBoolean gotNoFailure = new AtomicBoolean(true);
            final int totalSounds = project.getSounds().size();

            for (Sound sound : project.getSounds()) {
                String sourceFile = sound.getSourceFile();
                // Create a new file in the app's folder
                File newFile = getSoundFile(context, ownerId, projectId, sourceFile);
                newFile.getParentFile().mkdirs();
                // Download the file from the Cloud Storage
                storageRoot().child(project_path).child(sourceFile)
                        .getFile(newFile)
                        .addOnSuccessListener(taskSnapshot -> {
                            if (processedSounds.incrementAndGet() == totalSounds) {
                                if (gotNoFailure.get()) {
                                    Log.v("", "Successfully downloaded all the sounds");
                                    downloadedProjects.add(projectId);
                                    ws.dismiss();
                                    onSuccess.run();
                                } else {
                                    Log.e("", "Something went wrong while downloading the sounds");
                                    ws.dismiss();
                                    onFailure.run();
                                }
                            }
                        }).addOnFailureListener(e -> {
                            Log.v("", "Error while downloading a sound : " + e.getMessage());
                            gotNoFailure.set(false);
                        }).addOnProgressListener(taskSnapshot -> {
                            ws.setProgressGoal(taskSnapshot.getTask().toString(), taskSnapshot.getTotalByteCount());
                            ws.setProgress(taskSnapshot.getTask().toString(), taskSnapshot.getBytesTransferred());
                        });
            }
        } else {
            Log.v("", "No sounds to download");
            onSuccess.run();
        }
    }

    // Check which projects are downloaded by checking if all their sounds' source files are present on disk
    public static void checkDownloaded(Context context){
        downloadedProjects.clear();
        for (HeardProject project : FirestoreManager.projects){
            if(project.getSounds() != null && !project.getSounds().isEmpty()){
                boolean isDownloaded = true;
                for (Sound sound : project.getSounds()){
                    File sourceFile = getSoundFile(context, project.getOwner(), project.getId(), sound.getSourceFile());
                    isDownloaded = isDownloaded && sourceFile.exists();
                }
                if(isDownloaded){
                    downloadedProjects.add(project.getId());
                }
            } else {
                downloadedProjects.add(project.getId());
            }
        }
    }

    public static File getSoundFile (Context context, String ownerId, String projectId, String sourceFile){
        return new File(context.getFilesDir(), ownerId+"/"+projectId+"/"+sourceFile);
    }

    public static boolean deleteProject (Context context, final String projectId){
        final HeardProject project = FirestoreManager.getProject(projectId);
        if(project.getSounds() != null && !project.getSounds().isEmpty()) {
            String ownerId = project.getOwner();
            for (Sound sound : project.getSounds()) {
                String sourceFile = sound.getSourceFile();
                File soundFile = getSoundFile(context, ownerId, projectId, sourceFile);
                soundFile.delete();
            }
            new File(context.getFilesDir(), ownerId+"/"+projectId).delete();
            downloadedProjects.remove(projectId);
            return true;
        } else {
            Log.v("", "No sounds to delete");
            return false;
        }
    }
}
