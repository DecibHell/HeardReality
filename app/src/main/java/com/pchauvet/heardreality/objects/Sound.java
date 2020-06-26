package com.pchauvet.heardreality.objects;

import com.google.firebase.firestore.DocumentId;

public class Sound {
    @DocumentId
    private String id;

    private String name;
    private String sourceFile; // name of the file in the FirebaseCloudStorage
    private Trigger onTrigger;
    private Trigger offTrigger;  // optional
    private boolean loop;
    private String source; // reference to sources
    private int volume;  // optional

    public Sound() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Trigger getOnTrigger() {
        return onTrigger;
    }

    public void setOnTrigger(Trigger onTrigger) {
        this.onTrigger = onTrigger;
    }

    public Trigger getOffTrigger() {
        return offTrigger;
    }

    public void setOffTrigger(Trigger offTrigger) {
        this.offTrigger = offTrigger;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }
}
