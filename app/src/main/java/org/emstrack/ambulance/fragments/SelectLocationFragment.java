package org.emstrack.ambulance.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.AddressComponents;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.LocationRecyclerAdapter;
import org.emstrack.ambulance.adapters.PlacesRecyclerAdapter;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.models.SelectLocationType;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Address;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulanceCall;
import org.emstrack.models.Call;
import org.emstrack.models.GPSLocation;
import org.emstrack.models.Hospital;
import org.emstrack.models.Location;
import org.emstrack.models.NamedAddress;
import org.emstrack.models.Waypoint;
import org.emstrack.models.gson.ExcludeAnnotationExclusionStrategy;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.CalendarDateTypeAdapter;
import org.emstrack.models.util.OnServiceComplete;

import java.nio.file.Watchable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class SelectLocationFragment
        extends Fragment
        implements
        LocationRecyclerAdapter.SelectLocation,
        PlacesRecyclerAdapter.SelectPrediction,
        OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerDragListener {

    private static final String TAG = SelectLocationFragment.class.getSimpleName();
    private static final float minZoomLevel = 15;

    private SelectLocationType type;
    private NamedAddress selectedLocation;

    private RecyclerView recyclerView;

    private View selectHospitalButtonLayout;
    private View selectBaseButtonLayout;
    private View selectOtherButtonLayout;
    private View selectSelectButtonLayout;
    private View searchLayout;
    private EditText searchEditText;
    private View mapFragmentLayout;
    private View selectButtonOkLayout;
    private View selectButtonOk;

    private List<? extends NamedAddress> hospitals;
    private List<? extends NamedAddress> bases;
    private List<? extends NamedAddress> otherLocations;

    private GoogleMap googleMap;
    private Marker marker;
    private float zoomLevel = 16;
    private String lastQueryText;
    private AutocompleteSessionToken googlePlacesToken;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_select_location, container, false);

        recyclerView = rootView.findViewById(R.id.selectRecyclerView);

        // buttons
        View selectToolbar = rootView.findViewById(R.id.selectToolbar);
        selectHospitalButtonLayout = selectToolbar.findViewById(R.id.selectHospitalButtonLayout);
        selectHospitalButtonLayout.setOnClickListener(v -> {
            selectInterface(SelectLocationType.HOSPITAL);
        });
        selectBaseButtonLayout = selectToolbar.findViewById(R.id.selectBaseButtonLayout);
        selectBaseButtonLayout.setOnClickListener(v -> {
            selectInterface(SelectLocationType.BASE);
        });
        selectOtherButtonLayout = selectToolbar.findViewById(R.id.selectOtherButtonLayout);
        selectOtherButtonLayout.setOnClickListener(v -> {
            selectInterface(SelectLocationType.OTHER);
        });
        selectSelectButtonLayout = selectToolbar.findViewById(R.id.selectSelectButtonLayout);
        selectSelectButtonLayout.setOnClickListener(v -> {
            selectInterface(SelectLocationType.SELECT);
        });

        // ok button
        selectButtonOkLayout = rootView.findViewById(R.id.selectButtonOkLayout);
        selectButtonOk = rootView.findViewById(R.id.selectButtonOk);
        selectButtonOk.setOnClickListener(v -> addWaypoint());
        selectButtonOk.setEnabled(false);

        // search layout and button
        searchLayout = rootView.findViewById(R.id.searchLayout);
        searchEditText = rootView.findViewById(R.id.searchText);
        searchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String text = charSequence.toString().trim();
                if (text.length() > 4) {
                    searchPlaces(text);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

        });

        // get bases and other locations
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        bases = appData.getBases();
        otherLocations = appData.getOtherLocations();

        // get hospitals
        List<Hospital> _hospitals = new ArrayList<>();
        SparseArray<Hospital> hospitalsSparseArray = appData.getHospitals();
        for (int i = 0; i < hospitalsSparseArray.size(); i++) {
            _hospitals.add(hospitalsSparseArray.valueAt(i));
        }
        hospitals = _hospitals;

        // Initialize map
        mapFragmentLayout = rootView.findViewById(R.id.mapFragment);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapFragment);
        Objects.requireNonNull(mapFragment).getMapAsync(this);

        // get arguments
        Bundle arguments = getArguments();
        if (arguments != null) {
            type = (SelectLocationType) arguments.getSerializable("type");
        } else {
            type = SelectLocationType.HOSPITAL;
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity activity = (MainActivity) requireActivity();

        // setup navigation
        activity.setupNavigationBar(this);

        // Refresh data
        selectInterface(this.type, true);

    }

    @Override
    public void onCameraIdle() {

        if (googleMap != null) {
            zoomLevel = Math.max(googleMap.getCameraPosition().zoom, minZoomLevel);
        }

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        Log.d(TAG, "onMapReady");

        // save map
        this.googleMap = googleMap;

        // enable zoom buttons
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Add listener to track zoom
        googleMap.setOnCameraIdleListener(this);

        // Add drag listener
        googleMap.setOnMarkerDragListener(this);

    }

    @NonNull
    private View getButtonLayout(SelectLocationType type) {
        switch (type) {
            case HOSPITAL:
                return selectHospitalButtonLayout;
            case BASE:
                return selectBaseButtonLayout;
            case OTHER:
                return selectOtherButtonLayout;
            default:
            case SELECT:
                return selectSelectButtonLayout;
        }
    }

    private void selectInterface(SelectLocationType type) {
        selectInterface(type, false);
    }

    private void selectInterface(SelectLocationType type, boolean force) {

        if (force || type != this.type) {

            int darkColor = getResources().getColor(R.color.bootstrapDark);
            int primaryColor = getResources().getColor(R.color.bootstrapPrimary);

            // deemphasise current button
            View view = getButtonLayout(this.type);
            ((TextView) view.findViewById(android.R.id.title))
                    .setTextColor(darkColor);
            ((ImageView) view.findViewById(android.R.id.icon))
                    .getDrawable()
                    .setTint(darkColor);

            // highlight new button
            view = getButtonLayout(type);
            ((TextView) view.findViewById(android.R.id.title))
                    .setTextColor(primaryColor);
            ((ImageView) view.findViewById(android.R.id.icon))
                    .getDrawable()
                    .setTint(primaryColor);

            // update type
            this.type = type;

            // refresh data
            refreshData();

        }
    }

    /**
     * refreshData
     */
    public void refreshData() {

        // clear selection
        selectedLocation = null;
        selectButtonOk.setEnabled(false);
        mapFragmentLayout.setVisibility(View.GONE);
        selectButtonOkLayout.setVisibility(View.GONE);

        if (type == SelectLocationType.SELECT) {
            lastQueryText = null;
            searchLayout.setVisibility(View.VISIBLE);

            // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
            // and once again when the user makes a selection (for example when calling fetchPlace()).
            googlePlacesToken = AutocompleteSessionToken.newInstance();

            // has text?
            String currentText = searchEditText.getText().toString().trim();
            if (currentText.length() > 4) {
                searchPlaces(currentText);
            }

        } else {
            searchLayout.setVisibility(View.GONE);
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        LocationRecyclerAdapter adapter;
        switch (type) {

            case HOSPITAL:
                adapter = new LocationRecyclerAdapter(getActivity(), SelectLocationType.HOSPITAL, hospitals, this);
                break;

            case BASE:
                adapter = new LocationRecyclerAdapter(getActivity(), SelectLocationType.BASE, bases, this);
                break;

            case OTHER:
                adapter = new LocationRecyclerAdapter(getActivity(), SelectLocationType.OTHER, otherLocations, this);
                break;

            default:
            case SELECT:
                adapter = new LocationRecyclerAdapter(getActivity(), SelectLocationType.SELECT, new ArrayList<>(), this);
                break;

        }
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

        // clear marker
        setLocationMarker();
    }

    public void addWaypoint() {

        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Ambulance ambulance = appData.getAmbulance();
        Call call = appData.getCalls().getCurrentCall();
        if (ambulance!= null && call != null) {

            // Get waypoints
            AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
            final int maximumOrder = ambulanceCall.getNextNewWaypointOrder();

            // build waypoint
            Context context = requireContext();
            String waypointJson;
            String message;
            switch(type) {
                case HOSPITAL:
                    Hospital hospital = (Hospital) selectedLocation;
                    waypointJson = "{\"order\":" + maximumOrder + ",\"location_id\":" + hospital.getId() + "}";
                    message = getString(R.string.confirmNewWaypoint, hospital.toAddress(context));
                    break;

                case OTHER:
                case BASE:
                    Location location = (Location) selectedLocation;
                    waypointJson = "{\"order\":" + maximumOrder + ",\"location_id\":" + location.getId() + "}";
                    message = getString(R.string.confirmNewWaypoint, location.toAddress(context));
                    break;

                default:
                case SELECT:
                    Location newLocation = (Location) selectedLocation;
                    Gson gson = new GsonBuilder()
                            .registerTypeHierarchyAdapter(Calendar.class, new CalendarDateTypeAdapter())
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .addSerializationExclusionStrategy(new ExcludeAnnotationExclusionStrategy())
                            .addSerializationExclusionStrategy(new Waypoint.WaypointCreationExclusionStrategy())
                            .create();
                    Waypoint waypoint = new Waypoint(maximumOrder, Waypoint.STATUS_CREATED, newLocation);
                    waypointJson = gson.toJson(waypoint);
                    message = getString(R.string.confirmNewWaypoint, newLocation.toAddress(context));
                    break;
            }

            // Publish waypoint
            if (waypointJson != null) {

                int ambulanceId = ambulance.getId();
                int callId = call.getId();

                new AlertDialog.Builder(context)
                        .setTitle(R.string.confirm)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok,
                                (dialog, id) -> {

                                    Log.i(TAG, "Waypoint selected");
                                    Log.d(TAG, String.format("waypointJson = %s", waypointJson));

                                    Intent serviceIntent = new Intent(requireActivity(), AmbulanceForegroundService.class);
                                    serviceIntent.setAction(AmbulanceForegroundService.Actions.WAYPOINT_ADD);
                                    serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.WAYPOINT_UPDATE, waypointJson);
                                    serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.WAYPOINT_ID, -1); // -1 means create waypoint
                                    serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.AMBULANCE_ID, ambulanceId);
                                    serviceIntent.putExtra(AmbulanceForegroundService.BroadcastExtras.CALL_ID, callId);

                                    new OnServiceComplete(requireActivity(),
                                            BroadcastActions.SUCCESS,
                                            BroadcastActions.FAILURE,
                                            serviceIntent) {

                                        @Override
                                        public void onSuccess(Bundle extras) {

                                            // navigate up
                                            MainActivity mainActivity = (MainActivity) requireActivity();
                                            mainActivity.navigateUp();

                                        }

                                    }
                                            .start();


                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();

            }
        }

    }

    @Override
    public void selectLocation(SelectLocationType type, NamedAddress location) {

        if (type != this.type) {
            Log.d(TAG, "Wrong type, ignore");
            return;
        }

        // set selected
        selectedLocation = location;

        // set marker
        setLocationMarker();

        // enable button
        selectButtonOk.setEnabled(true);

        // set map visible
        mapFragmentLayout.setVisibility(View.VISIBLE);
        selectButtonOkLayout.setVisibility(View.VISIBLE);

    }

    @Override
    public void selectLocation(AutocompletePrediction prediction) {
        Log.d(TAG, "Select prediction");

        // fetch place
        List<Place.Field> fields = new ArrayList<>();
        fields.add(Place.Field.LAT_LNG);
        fields.add(Place.Field.ADDRESS_COMPONENTS);
        FetchPlaceRequest request = FetchPlaceRequest.builder(prediction.getPlaceId(), fields)
                .setSessionToken(googlePlacesToken)
                .build();

        // get places client
        MainActivity mainActivity = (MainActivity) requireActivity();
        PlacesClient placesClient = mainActivity.getPlacesClient();

        // make the request
        placesClient
                .fetchPlace(request)
                .addOnSuccessListener(response -> {

                    // create location
                    Place place = response.getPlace();
                    LatLng latLng = place.getLatLng();
                    AddressComponents addressComponents = place.getAddressComponents();
                    if (latLng != null) {

                        // parse address and set selected
                        if (addressComponents != null) {
                            Address address = Address.parseAddressComponents(latLng, addressComponents);
                            Log.d(TAG, String.format("parsed address = %s", address));
                            selectedLocation = new Location("", Location.TYPE_WAYPOINT, address);
                        } else {
                            selectedLocation = new Location("", Location.TYPE_WAYPOINT, new GPSLocation(latLng));
                        }

                        // set marker (draggable)
                        setLocationMarker(true);

                        // enable button
                        selectButtonOk.setEnabled(true);

                        // set map visible
                        mapFragmentLayout.setVisibility(View.VISIBLE);
                        selectButtonOkLayout.setVisibility(View.VISIBLE);

                        // hide keyboard
                        // Check if no view has focus:
                        FragmentActivity activity = requireActivity();
                        View view = activity.getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    } else {
                        Log.d(TAG, "Did not get LatLng");
                    }

                })
                .addOnFailureListener(exception -> {
                    Log.d(TAG, "Failed to fetch place");
                });

    }

    private void setLocationMarker() {
        setLocationMarker(false);
    }

    private void setLocationMarker(boolean draggable) {

        if (googleMap != null) {

            // remove current marker
            if (marker != null) {
                marker.remove();
            }

            if (selectedLocation != null) {
                // add marker
                GPSLocation gpsLocation = selectedLocation.getLocation();
                LatLng latLng = new LatLng(gpsLocation.getLatitude(), gpsLocation.getLongitude());
                marker = googleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .draggable(draggable));

                // center map
                googleMap.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                                new CameraPosition(latLng, zoomLevel, 0, 0)));
            }
        }

    }

    private void searchPlaces(String queryText) {

        if (lastQueryText != null && lastQueryText.startsWith(queryText)) {
            Log.d(TAG, "Same text, skipping");
        }

        if (mapFragmentLayout.getVisibility() == View.VISIBLE) {
            // hide map and button
            mapFragmentLayout.setVisibility(View.GONE);
            selectButtonOkLayout.setVisibility(View.GONE);
        }

        // override
        lastQueryText = queryText;

        // Create a RectangularBounds object.
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(-33.880490, 151.184363), //dummy lat/lng
                new LatLng(-33.858754, 151.229596));

        // Use the builder to create a FindAutocompletePredictionsRequest.
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                // Call either setLocationBias() OR setLocationRestriction().
                .setLocationBias(bounds)
                //.setLocationRestriction(bounds)
                //.setCountry("ng")//Nigeria
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(googlePlacesToken)
                .setQuery(queryText)
                .build();

        // get places client
        MainActivity mainActivity = (MainActivity) requireActivity();
        PlacesClient placesClient = mainActivity.getPlacesClient();

        // make the request
        placesClient
                .findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    PlacesRecyclerAdapter adapter = new PlacesRecyclerAdapter(getActivity(), response.getAutocompletePredictions(), this);
                    recyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(exception -> {
                    if (exception instanceof ApiException) {
                        // place not found
                        ApiException apiException = (ApiException) exception;
                        Log.e(TAG, "Place not found: " + apiException.getStatusCode());
                    } else {
                        Log.d(TAG, "Failed to retrieve place");
                    }
                });
    }

    @Override
    public void onMarkerDrag(@NonNull Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(@NonNull Marker marker) {
        // set new position
        LatLng position = marker.getPosition();
        selectedLocation.setLocation(new GPSLocation(position.latitude, position.longitude));
    }

    @Override
    public void onMarkerDragStart(@NonNull Marker marker) {

    }
}