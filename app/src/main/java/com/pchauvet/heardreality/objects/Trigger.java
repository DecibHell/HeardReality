package com.pchauvet.heardreality.objects;

public class Trigger {
    private int delay; // optional
    private boolean onlyOnce;
    private String type; // END_OF_SOUND ,
    private Object trigger;

    public Trigger() {
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public boolean isOnlyOnce() {
        return onlyOnce;
    }

    public void setOnlyOnce(boolean onlyOnce) {
        this.onlyOnce = onlyOnce;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getTrigger() {
        return trigger;
    }

    public void setTrigger(Object trigger) {
        this.trigger = trigger;
    }
}
