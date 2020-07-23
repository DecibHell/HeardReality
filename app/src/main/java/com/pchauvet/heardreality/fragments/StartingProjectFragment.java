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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.pchauvet.heardreality.AudioProcess;
import com.pchauvet.heardreality.MainActivity;
import com.pchauvet.heardreality.R;
import com.pchauvet.heardreality.Utils;
import com.pchauvet.heardreality.dialogs.PermissionRationaleDialogFragment;
import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import static com.pchauvet.heardreality.Utils.REQUEST_ACCESS_FINE_LOCATION;
import static com.pchauvet.heardreality.Utils.getLatLngFromGeoPoint;
import static com.pchauvet.heardreality.Utils.getProjectStartingRange;

public class StartingProjectFragment extends Fragment implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        MainActivity.MainActivityListener {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    private TextView mTitle;

    private ImageView mHtTick;
    private TextView mHtText;
    private ImageView mStartTick;
    private TextView mStartText;
    private ImageView mSoundsTick;
    private  TextView mSoundsText;

    private boolean htOk;
    private boolean startOk;
    private boolean soundsOk;

    private Button mCancelButton;
    private Button mGoButton;

    private HeardProject project;

    private FusedLocationProviderClient locationProvider;
    private LocationCallback locationCallback;

    private Range startingRange;

    private Location lastLocation;

    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 3000;

    // The fastest rate for active location updates. Updates will never be more frequent than this value.
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    public StartingProjectFragment (HeardProject project){
        this.project = project;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_starting_project, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTitle = view.findViewById(R.id.spf_title);
        mTitle.setText(getString(R.string.spf_title, project.getName()));

        mHtTick = view.findViewById(R.id.spf_ht_tick);
        mHtText = view.findViewById(R.id.spf_ht_text);
        mStartTick = view.findViewById(R.id.spf_start_tick);
        mStartText = view.findViewById(R.id.spf_start_text);
        mSoundsTick = view.findViewById(R.id.spf_sounds_tick);
        mSoundsText = view.findViewById(R.id.spf_sounds_text);

        // Check if there is a connected head-tracker at start
        String deviceName = ((MainActivity)requireActivity()).getDeviceName();
        if(deviceName != null){
            mHtText.setText(getString(R.string.spf_yes_ht, deviceName));
            mHtTick.setImageResource(android.R.drawable.checkbox_on_background);
            htOk = true;
        } else {
            mHtText.setText(R.string.spf_no_ht);
            mHtTick.setImageResource(android.R.drawable.checkbox_off_background);
            htOk = false;
        }

        mCancelButton = view.findViewById(R.id.spf_cancel_button);
        mCancelButton.setOnClickListener(v -> {
            AudioProcess.unloadAllSounds();
            ((MainActivity) requireActivity()).openWorldMapFragment();
        });

        mGoButton = view.findViewById(R.id.spf_go_button);
        mGoButton.setOnClickListener(v -> ((MainActivity) requireActivity()).openPlayingProjectFragment(project, lastLocation));

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.spf_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Register to listen to the device changes
        ((MainActivity)requireActivity()).listeners.add(this);

        // Preload the sound files
        AudioProcess.preloadProject(project, () -> {
            if(getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    mSoundsText.setText(R.string.spf_yes_sounds);
                    mSoundsTick.setImageResource(android.R.drawable.checkbox_on_background);
                    soundsOk = true;
                    updateGoButtonStatus();
                });
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        locationProvider.removeLocationUpdates(locationCallback);
        // Unregister to the device changes
        ((MainActivity)requireActivity()).listeners.remove(this);
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
        startingRange = getProjectStartingRange(project);
        if (startingRange.getType().equals("CIRCULAR")) {
            LatLng center = getLatLngFromGeoPoint(startingRange.getCenter());
            // Include the range's center in the camera focus
            focusBuilder.include(center);

            Double radius = startingRange.getRadius();
            CircleOptions circleOptions = new CircleOptions()
                    .center(center)
                    .radius(radius);
            circleOptions.fillColor(Color.argb(100, 20, 100, 240));
            mMap.addCircle(circleOptions);
        } else {
            PolygonOptions rectOptions = new PolygonOptions();
            List<LatLng> points = new ArrayList<>();
            for(GeoPoint geoPoint : startingRange.getPoints()){
                LatLng point = getLatLngFromGeoPoint(geoPoint);
                points.add(point);
                // Include all the polygon's points in the camera focus
                focusBuilder.include(point);
            }
            rectOptions.addAll(points);
            rectOptions.fillColor(Color.argb(100, 20, 100, 240));
            mMap.addPolygon(rectOptions);
        }

        // Check if the user is in the starting range at start
        locationProvider.getLastLocation()
                .addOnSuccessListener(location -> {
                    lastLocation = location;
                    LatLng latLng = Utils.getLatLngFromLocation(location);
                    // Set the camera zoom around the user and the starting range
                    focusBuilder.include(latLng);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(focusBuilder.build(), 200));

                    checkLatLngInRange(latLng);
                });

        // Send requests to check if the user is in range when its location changes
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    lastLocation = location;
                    LatLng latLng = Utils.getLatLngFromLocation(location);
                    checkLatLngInRange(latLng);
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

    // Updates the UI to tell if the user is in range
    private void checkLatLngInRange(LatLng location){
        if(startingRange.isLatLngInRange(location)){
            // If the user is in the starting range
            mStartText.setText(R.string.spf_in_start);
            mStartTick.setImageResource(android.R.drawable.checkbox_on_background);
            startOk = true;
        } else {
            mStartText.setText(R.string.spf_out_start);
            mStartTick.setImageResource(android.R.drawable.checkbox_off_background);
            startOk = false;
        }
        updateGoButtonStatus();
    }

    private void updateGoButtonStatus(){
        boolean enable = htOk && startOk && soundsOk;
        mGoButton.setClickable(enable);
        if(enable){
            mGoButton.setTextColor(getResources().getColor(R.color.colorPrimary, null));
        } else {
            mGoButton.setTextColor(getResources().getColor(R.color.grey, null));
        }
    }

    @Override
    public void onDeviceConnected(String deviceName) {
        if(mHtText != null && mHtTick != null){
            mHtText.setText(getString(R.string.spf_yes_ht, deviceName));
            mHtTick.setImageResource(android.R.drawable.checkbox_on_background);
            htOk = true;
            updateGoButtonStatus();
        }
    }

    @Override
    public void onDeviceDisconnected() {
        if(mHtText != null && mHtTick != null) {
            mHtText.setText(R.string.spf_no_ht);
            mHtTick.setImageResource(android.R.drawable.checkbox_off_background);
            htOk = false;
            updateGoButtonStatus();
        }
    }
}

