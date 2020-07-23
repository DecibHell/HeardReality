package com.pchauvet.heardreality.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.firestore.GeoPoint;
import com.pchauvet.heardreality.AudioProcess;
import com.pchauvet.heardreality.R;
import com.pchauvet.heardreality.Utils;
import com.pchauvet.heardreality.dialogs.PermissionRationaleDialogFragment;
import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.Range;
import com.pchauvet.heardreality.objects.Source;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import static com.pchauvet.heardreality.Utils.REQUEST_ACCESS_FINE_LOCATION;
import static com.pchauvet.heardreality.Utils.getLatLngFromGeoPoint;

public class PlayingProjectMapFragment extends Fragment implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener{

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    private HeardProject project;

    private FusedLocationProviderClient locationProvider;
    private LocationCallback locationCallback;


    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 3000;

    // The fastest rate for active location updates. Updates will never be more frequent than this value.
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    public PlayingProjectMapFragment (HeardProject project){
        this.project = project;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playing_project_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.ppmf_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(locationProvider != null){
            locationProvider.removeLocationUpdates(locationCallback);
        }
    }

    /**
     * Manipulates the map once available.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        // Get Location Data if it has been enabled
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            locationProvider = LocationServices.getFusedLocationProviderClient(requireContext());
        } else {
            final PermissionRationaleDialogFragment dialog = PermissionRationaleDialogFragment.getInstance(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION, getString(R.string.allow_location_services));
            dialog.show(requireActivity().getSupportFragmentManager(), null);
        }

        final LatLngBounds.Builder focusBuilder = LatLngBounds.builder();
        // Print the Starting range
        for (Range range : project.getRanges()){
            if (range.getType().equals("CIRCULAR")) {
                LatLng center = getLatLngFromGeoPoint(range.getCenter());
                // Include the range's center in the camera focus
                focusBuilder.include(center);

                Double radius = range.getRadius();
                CircleOptions circleOptions = new CircleOptions()
                    .center(center)
                    .radius(radius)
                    .fillColor(Color.argb(100, 10, 50, 200))
                    .strokeColor(0);
                mMap.addCircle(circleOptions);
            } else {
                PolygonOptions rectOptions = new PolygonOptions();
                List<LatLng> points = new ArrayList<>();
                for(GeoPoint geoPoint : range.getPoints()){
                    LatLng point = getLatLngFromGeoPoint(geoPoint);
                    points.add(point);
                    // Include all the polygon's points in the camera focus
                    focusBuilder.include(point);
                }
                rectOptions.addAll(points);
                rectOptions.fillColor(Color.argb(100, 10, 50, 200));
                rectOptions.strokeColor(0);
                mMap.addPolygon(rectOptions);
            }
        }

        for (Source source : project.getSources()) {
            LatLng center = Utils.getLatLngFromGeoPoint(source.getPosition());
            // Draw the source's icon
            mMap.addGroundOverlay(new GroundOverlayOptions()
                    .position(center, 10)
                    .image(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_lock_silent_mode_off)));

            // Draw the source's distance rolloff zone if any
            if (source.getDistanceModel() != null) {
                double radius = source.getDistanceMin();
                CircleOptions circleOptions = new CircleOptions()
                        .center(center)
                        .radius(radius)
                        .fillColor(Color.argb(100, 200, 10, 10))
                        .strokeColor(0);
                mMap.addCircle(circleOptions);

                radius = source.getDistanceMax();
                circleOptions.radius(radius);
                mMap.addCircle(circleOptions);
            }
        }

        // Move the camera to show the whole project
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(focusBuilder.build(), 200));

        // Send requests to check if the user is in range when its location changes
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    ((PlayingProjectFragment)getParentFragment()).onUserMoved(location);
                }
            }
        };

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationProvider.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
    }
}

