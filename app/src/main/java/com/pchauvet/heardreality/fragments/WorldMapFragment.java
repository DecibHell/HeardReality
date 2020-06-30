package com.pchauvet.heardreality.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.pchauvet.heardreality.AudioProcess;
import com.pchauvet.heardreality.FirestoreManager;
import com.pchauvet.heardreality.ProjectElementAdapter;
import com.pchauvet.heardreality.R;
import com.pchauvet.heardreality.StorageManager;
import com.pchauvet.heardreality.dialogs.PermissionRationaleDialogFragment;
import com.pchauvet.heardreality.dialogs.WaitingScreen;
import com.pchauvet.heardreality.objects.HeardProject;

import androidx.appcompat.view.menu.ListMenuPresenter;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import static com.pchauvet.heardreality.Utils.REQUEST_ACCESS_FINE_LOCATION;
import static com.pchauvet.heardreality.Utils.getProjectStartingPoint;

public class WorldMapFragment extends Fragment implements OnMapReadyCallback,
        FirestoreManager.DBChangeListener{

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient mLocationClient;

    private ImageButton mCenterUserButton;
    private SeekBar mZoomSlider;

    private LatLng mUserLatLng;

    private int mLastMovementReason;

    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private Marker mark;

    private ConstraintLayout mListLayout;
    private RadioGroup mListTabs;

    private ImageButton mProjectsToggle;
    private EditText mProjectsNameText;
    private ImageButton mProjectsNameSearch;
    private ImageButton mProjectsNameCancel;

    private ListView mListView;

    private String[] mListFilters = {"", "ALL"};

    private ProjectElementAdapter projectsAdapter;

    private final int DEFAULT_ZOOM = 15;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create a location provider
        mLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        return inflater.inflate(R.layout.fragment_world_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.wmf_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        mCenterUserButton = view.findViewById(R.id.wmf_center_user);
        mCenterUserButton.setOnClickListener(v -> mMap.animateCamera(CameraUpdateFactory.newLatLng(mUserLatLng)));

        mZoomSlider = view.findViewById(R.id.wmf_zoom);
        mZoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int zoom = 1 + progress*5;
                mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mListLayout = view.findViewById(R.id.wmf_list);

        mListTabs = view.findViewById(R.id.wmf_list_tabs);
        mListTabs.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId){
                case R.id.wmf_list_downloaded :
                    mListFilters[1] = "DOWNLOADED";
                    break;
                case R.id.wmf_list_my_projects :
                    mListFilters[1] = "MYPROJECTS";
                    break;
                default :
                    mListFilters[1] = "ALL";
            }
            projectsAdapter.getFilter().filter(formatFilter());
        });

        mProjectsToggle = view.findViewById(R.id.wmf_projects_toggle);
        mProjectsToggle.setOnClickListener(v -> {
            if(mListLayout.getVisibility() == View.GONE){
                mListLayout.setVisibility(View.VISIBLE);
                mProjectsToggle.setBackgroundColor(getResources().getColor(R.color.colorAccent2, null));
            }else{
                mListLayout.setVisibility(View.GONE);
                mProjectsToggle.setBackgroundColor(getResources().getColor(R.color.colorAccent, null));
            }
        });

        mProjectsNameText = view.findViewById(R.id.wmf_projects_name_text);

        mProjectsNameSearch = view.findViewById(R.id.wmf_projects_name_search);
        mProjectsNameCancel = view.findViewById(R.id.wmf_projects_name_cancel);

        mProjectsNameSearch.setOnClickListener(v -> {
            String nameFilter = mProjectsNameText.getText().toString();
            if(!nameFilter.isEmpty()){
                mProjectsNameText.setEnabled(false);
                mProjectsNameSearch.setVisibility(View.GONE);
                mProjectsNameCancel.setVisibility(View.VISIBLE);
                mProjectsToggle.setBackgroundColor(getResources().getColor(R.color.colorAccent2, null));

                mListLayout.setVisibility(View.VISIBLE);

                mListFilters[0] = mProjectsNameText.getText().toString();
                projectsAdapter.getFilter().filter(formatFilter());
            }
        });

        mProjectsNameCancel.setOnClickListener(v -> {
            mProjectsNameText.setEnabled(true);
            mProjectsNameText.setText(null);
            mProjectsNameSearch.setVisibility(View.VISIBLE);
            mProjectsNameCancel.setVisibility(View.GONE);

            mListFilters[0] = "";
            projectsAdapter.getFilter().filter(formatFilter());
        });

        mListView = view.findViewById(R.id.wmf_list_view);
        projectsAdapter = new ProjectElementAdapter(requireContext(), view);
        mListView.setAdapter(projectsAdapter);

        // When clicking on a project in the list, move the camera on its starting point
        mListView.setOnItemClickListener((adapterView, view1, position, id) -> {
            HeardProject project = (HeardProject)adapterView.getItemAtPosition(position);
            LatLng startingPoint = getProjectStartingPoint(project);
            if (startingPoint != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startingPoint, DEFAULT_ZOOM));
            }
        });

        FirestoreManager.dbChangeListeners.add(this);
    }

    /**
     * Manipulates the map once available.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setMapToolbarEnabled(false);

       mMap.setOnCameraMoveStartedListener(reason -> mLastMovementReason = reason);

        mMap.setOnCameraMoveListener(() -> {
            // Update the zoom slider if the movement was made by a user gesture
            if(mLastMovementReason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE){
                CameraPosition cameraPosition = mMap.getCameraPosition();
                float zoom = cameraPosition.zoom;
                mZoomSlider.setProgress((int)(zoom/5));
            }
        });

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            mUserLatLng = new LatLng(location.getLatitude(),location.getLongitude());
                            mark = mMap.addMarker(new MarkerOptions().position(mUserLatLng).title("Oh hi Mark"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mUserLatLng, DEFAULT_ZOOM));
                        }
                    });
            // We create location requests to update the user's position
            mLocationRequest = LocationRequest.create();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setInterval(1000);

            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            mUserLatLng = new LatLng(location.getLatitude(),location.getLongitude());
                            mark.setPosition(mUserLatLng);
                        }
                    }
                }
            };

            mLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        } else {
            final PermissionRationaleDialogFragment dialog = PermissionRationaleDialogFragment.getInstance(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION, getString(R.string.allow_location_services));
            dialog.show(requireActivity().getSupportFragmentManager(), null);
        }
    }

    private String formatFilter(){
        return mListFilters[0]+"\\/"+mListFilters[1];
    }

    public void updateProjectList(){
        requireActivity().runOnUiThread(() -> {
            projectsAdapter.updateItems();
            projectsAdapter.getFilter().filter(formatFilter());
        });
    }

    @Override
    public void onProjectsChanged() {
        updateProjectList();
    }
}