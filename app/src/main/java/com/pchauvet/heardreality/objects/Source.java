package com.pchauvet.heardreality.objects;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;

public class Source {
    @DocumentId
    private String id;

    private String name;
    private GeoPoint position;
    private float altitude;

    private Integer distanceModel; // optional , 0=logarithmic, 1=linear, 2=none    // if specified, specify also distanceMin and distanceMax
    private Float distanceMin;
    private Float distanceMax;

    public Source() {
    }

    public float getAltitude() {
        return altitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public void setDistanceModel(Integer distanceModel) {
        this.distanceModel = distanceModel;
    }

    public void setDistanceMin(Float distanceMin) {
        this.distanceMin = distanceMin;
    }

    public void setDistanceMax(Float distanceMax) {
        this.distanceMax = distanceMax;
    }

    public Float getDistanceMax() {
        return distanceMax;
    }

    public Integer getDistanceModel() {
        return distanceModel;
    }

    public Float getDistanceMin() {
        return distanceMin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GeoPoint getPosition() {
        return position;
    }

    public void setPosition(GeoPoint position) {
        this.position = position;
    }
}
