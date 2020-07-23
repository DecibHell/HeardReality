package com.pchauvet.heardreality;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.Range;
import com.pchauvet.heardreality.objects.Sound;
import com.pchauvet.heardreality.objects.Source;
import com.pchauvet.heardreality.objects.Trigger;
import com.pchauvet.heardreality.objects.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static android.content.ContentValues.TAG;

public class FirestoreManager {

    public static List<HeardProject> projects = new ArrayList<>();
    public static List<User> users = new ArrayList<>();

    public static String lastError;

    public static FirebaseFirestore db(){
        return FirebaseFirestore.getInstance();
    }

    public static void gatherData(final Runnable onCompleted){
        final AtomicBoolean oneTaskCompleted = new AtomicBoolean(false);

        db().collection("HeardProjects")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        projects = new ArrayList<>();
                        if (task.getResult() != null) {
                            final AtomicInteger projectCompletionCounter = new AtomicInteger(0);
                            final AtomicInteger totalProjects = new AtomicInteger(0);
                            // PROCESS ALL PROJECTS
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                HeardProject project = document.toObject(HeardProject.class);
                                // We only fetch projects if they are public or if the authentified user is the owner
                                if(project.isPublished() || (AuthManager.currentUser!=null && AuthManager.currentUser.getUid().equals(project.getOwner()))){
                                    projects.add(project);
                                    // Gather the data of each project (Ranges, Sounds, Sources)
                                    totalProjects.incrementAndGet();
                                    gatherProjectData(project.getId(), new Thread(){
                                        @Override
                                        public void run(){
                                            // Check if all the projects have been gathered properly
                                            if(projectCompletionCounter.incrementAndGet() == totalProjects.get()){
                                                notifyProjectsChanged();
                                                // Check if the users have been gathered too, to send onCompleted
                                                if(oneTaskCompleted.get()){
                                                    onCompleted.run();
                                                }else{
                                                    oneTaskCompleted.set(true);
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Error getting projects.", task.getException());
                        if(oneTaskCompleted.get()){
                            onCompleted.run();
                        }else{
                            oneTaskCompleted.set(true);
                        }
                    }
                });

        db().collection("Users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        users = new ArrayList<>();
                        if (task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                users.add(document.toObject(User.class));
                            }
                        }
                    } else {
                        Log.e(TAG, "Error getting users.", task.getException());
                    }
                    // We send onCompleted only if the other task completed
                    if(oneTaskCompleted.get()){
                        onCompleted.run();
                    }else{
                        oneTaskCompleted.set(true);
                    }
                });

    }

    public static void gatherProjectData(final String projectId, final Runnable onCompleted){
        final AtomicInteger completionCount = new AtomicInteger(0);

        db().collection("HeardProjects")
                .document(projectId)
                .collection("Ranges")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Range> ranges = new ArrayList<>();
                        if (task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ranges.add(document.toObject(Range.class));
                            }
                        }
                        getProject(projectId).setRanges(ranges);
                    } else {
                        Log.e(TAG, "Error getting Ranges from project "+projectId, task.getException());
                    }
                    if(completionCount.incrementAndGet() == 3){
                        onCompleted.run();
                    }
                });

        db().collection("HeardProjects")
                .document(projectId)
                .collection("Sounds")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Sound> sounds = new ArrayList<>();
                        if (task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                sounds.add(document.toObject(Sound.class));
                            }
                        }
                        getProject(projectId).setSounds(sounds);
                    } else {
                        Log.e(TAG, "Error getting Sounds from project "+projectId, task.getException());
                    }
                    if(completionCount.incrementAndGet() == 3){
                        onCompleted.run();
                    }
                });

        db().collection("HeardProjects")
                .document(projectId)
                .collection("Sources")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Source> sources = new ArrayList<>();
                        if (task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                sources.add(document.toObject(Source.class));
                            }
                        }
                        getProject(projectId).setSources(sources);
                    } else {
                        Log.e(TAG, "Error getting Sources from project "+projectId, task.getException());
                    }
                    if(completionCount.incrementAndGet() == 3){
                        onCompleted.run();
                    }
                });
    }

    public static void isUsernameDuplicate(String name, final Runnable ifTrue, final Runnable ifFalse){
        db().collection("Users")
                .whereEqualTo("name", name)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if(task.getResult() != null && !task.getResult().isEmpty()){
                            // WE FOUND A DUPLICATE
                            ifTrue.run();
                        }else{
                            ifFalse.run();
                        }
                    } else {
                        Log.e(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    public static void createUser(String name, final Runnable onSuccess, final Runnable onFailure){
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        // CREATE A NEW USER IN THE USERS COLLECTION WITH THE CURRENT CONNECTED USER'S ID (FRESHLY CREATED)
        db().collection("Users")
                .document(AuthManager.currentUser.getUid())
                .set(user)
                .addOnSuccessListener(o -> {
                    Log.d(TAG, "DocumentSnapshot added for User");
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding document in Users collection", e);
                    onFailure.run();
                });
    }

    public static User getUser(String userId){
        for (User user : users){
            if(user.getId().equals(userId)){
                return user;
            }
        }
        return null;
    }

    public static HeardProject getProject(String projectId){
        for (HeardProject project : projects){
            if(project.getId().equals(projectId)){
                return project;
            }
        }
        return null;
    }


    public static void updateUser(String userId, String field, Object value){
        db().collection("Users")
                .document(userId)
                .update(field, value)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("","User correctly updated");
                    } else {
                        Log.e(TAG, "Error updating user", task.getException());
                    }
                });
    }

    public static List<DBChangeListener> dbChangeListeners = new ArrayList<>();
    public interface DBChangeListener {
        void onProjectsChanged();
    }

    public static void notifyProjectsChanged(){
        StorageManager.checkDownloaded();
        for(DBChangeListener listener : dbChangeListeners){
            listener.onProjectsChanged();
        }
    }
}
