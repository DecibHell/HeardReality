package com.pchauvet.heardreality.objects;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import com.google.type.LatLng;

import java.util.List;

public class Range {
    @DocumentId
    private String id;

    private String name;
    private String type; // "POLYGONAL" or "CIRCULAR"

    // CIRCULAR RANGE
    private GeoPoint center;
    private Double radius;

    // POLYGONAL RANGE
    private List<GeoPoint> points;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getRadius() {
        return radius;
    }

    public void setRadius(Double radius) {
        this.radius = radius;
    }

    public GeoPoint getCenter() {
        return center;
    }

    public void setCenter(GeoPoint center) {
        this.center = center;
    }

    public List<GeoPoint> getPoints() {
        return points;
    }

    public void setPoints(List<GeoPoint> points) {
        this.points = points;
    }
}
