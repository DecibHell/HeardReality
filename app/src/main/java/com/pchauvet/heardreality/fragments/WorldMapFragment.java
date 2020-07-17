package com.pchauvet.heardreality.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pchauvet.heardreality.FirestoreManager;
import com.pchauvet.heardreality.ProjectElementAdapter;
import com.pchauvet.heardreality.R;
import com.pchauvet.heardreality.StorageManager;
import com.pchauvet.heardreality.Utils;
import com.pchauvet.heardreality.dialogs.PermissionRationaleDialogFragment;
import com.pchauvet.heardreality.objects.HeardProject;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import static com.pchauvet.heardreality.Utils.REQUEST_ACCESS_FINE_LOCATION;

public class WorldMapFragment extends Fragment implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        FirestoreManager.DBChangeListener,
        StorageManager.StorageChangeListener {

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;

    private ArrayList<Marker> projectMarkers = new ArrayList<>();

    private ConstraintLayout mListLayout;
    private RadioGroup mListTabs;

    private ImageButton mProjectsToggle;
    private EditText mProjectsNameText;
    private ImageButton mProjectsNameSearch;
    private ImageButton mProjectsNameCancel;

    private ListView mListView;

    private String[] mListFilters = {"", "ALL"};

    private ProjectElementAdapter projectsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_world_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.wmf_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

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
        projectsAdapter = new ProjectElementAdapter(requireContext(), this);
        mListView.setAdapter(projectsAdapter);

        // When clicking on a project in the list, move the camera to its marker
        mListView.setOnItemClickListener((adapterView, view1, position, id) -> {
            HeardProject project = (HeardProject)adapterView.getItemAtPosition(position);
            focusOnProject(project);
        });

        // Subscribe to database and storage changes, to update the projects list accordingly
        FirestoreManager.dbChangeListeners.add(this);
        StorageManager.storageChangeListeners.add(this);
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

        // Set a listener for marker click.
        mMap.setOnMarkerClickListener(this);

        // Create the projects' markers
        displayProjectsMarkers();

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        // Get Location Data if it has been enabled
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            // Center the map on the user
            FusedLocationProviderClient locationProvider = new FusedLocationProviderClient(requireContext());
            locationProvider.getLastLocation()
                    .addOnSuccessListener(location -> {
                        LatLng latLng = Utils.getLatLngFromLocation(location);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    });
        } else {
            final PermissionRationaleDialogFragment dialog = PermissionRationaleDialogFragment.getInstance(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ACCESS_FINE_LOCATION, getString(R.string.allow_location_services));
            dialog.show(requireActivity().getSupportFragmentManager(), null);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        // Unsubscribe the change listeners when the fragment is destroyed
        FirestoreManager.dbChangeListeners.remove(this);
        StorageManager.storageChangeListeners.remove(this);
    }

    @Override
    public boolean onMyLocationButtonClick() { return false; }

    @Override
    public void onMyLocationClick(@NonNull Location location) {}

    private String formatFilter(){ return mListFilters[0]+"\\/"+mListFilters[1]; }

    private void updateProjectList(){
        projectsAdapter.updateItems();
        projectsAdapter.getFilter().filter(formatFilter());
    }

    private void displayProjectsMarkers(){
        projectMarkers.clear();
        for (HeardProject project : FirestoreManager.projects){
            LatLng startPos = Utils.getProjectStartingPoint(project);
            if(startPos != null){
                Marker projectMarker = mMap.addMarker(new MarkerOptions().position(startPos).title(project.getName()));
                projectMarker.setTag(project.getId());
                projectMarkers.add(projectMarker);
            }
        }
    }

    public void displayProjectDetails(HeardProject project){
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        ProjectDetailsFragment projectDetailsFragment = new ProjectDetailsFragment(project);
        fragmentTransaction.replace(R.id.wmf_project_details_placeholder, projectDetailsFragment);
        fragmentTransaction.commit();
    }

    public void focusOnProject(HeardProject project){
        for (Marker projectMarker : projectMarkers){
            if(projectMarker.getTag() == project.getId()){
                projectMarker.showInfoWindow();
                mMap.animateCamera(CameraUpdateFactory.newLatLng(projectMarker.getPosition()), 250, null);
            }
        }
    }

    @Override
    public void onProjectsChanged() {
        requireActivity().runOnUiThread(() -> {
            updateProjectList();
            displayProjectsMarkers();
        });
    }

    @Override
    public void onDownloadsChanged() {
        requireActivity().runOnUiThread(() -> {
            updateProjectList();
            displayProjectsMarkers();
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTag() != null){
            HeardProject project = FirestoreManager.getProject(marker.getTag().toString());
            if (project != null){
                displayProjectDetails(project);
            }
        }
        return false;
    }

}