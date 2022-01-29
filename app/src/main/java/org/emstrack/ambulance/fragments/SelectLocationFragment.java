package org.emstrack.ambulance.fragments;

import static org.emstrack.ambulance.util.GoogleMapsHelper.centerMap;
import static org.emstrack.ambulance.util.GoogleMapsHelper.clearMarkers;
import static org.emstrack.ambulance.util.GoogleMapsHelper.getMarkerBitmapDescriptor;
import static org.emstrack.ambulance.util.GoogleMapsHelper.padAndMatchBounds;

import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.LocationRecyclerAdapter;
import org.emstrack.ambulance.adapters.PlacesRecyclerAdapter;
import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.dialogs.SimpleAlertDialog;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.models.NamedAddressWithDistance;
import org.emstrack.ambulance.models.SelectLocationType;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.ViewHolderWithSelectedPosition;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SelectLocationFragment
        extends Fragment
        implements
        LocationRecyclerAdapter.OnClick,
        ViewHolderWithSelectedPosition.OnClick<AutocompletePrediction>,
        OnMapReadyCallback, GoogleMap.OnCameraIdleListener,
        GoogleMap.OnMarkerDragListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener {

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
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private View selectLocationBottomSheet;

    private List<? extends NamedAddress> hospitals;
    private List<? extends NamedAddress> bases;
    private List<? extends NamedAddress> otherLocations;

    private GoogleMap googleMap;
    private float zoomLevel = 16;
    private String lastQueryText;
    private AutocompleteSessionToken googlePlacesToken;

    private Marker selectedLocationMarker;
    private final Map<Integer, Marker> markerMap = new HashMap<>();
    private int mapHeight;
    private int mapWidth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_select_location, container, false);

        recyclerView = rootView.findViewById(R.id.selectRecyclerView);

        // bottom sheet
        selectLocationBottomSheet = rootView.findViewById(R.id.selectLocationBottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(selectLocationBottomSheet);

        // buttons
        View selectToolbar = rootView.findViewById(R.id.selectToolbar);

        selectHospitalButtonLayout = selectToolbar.findViewById(R.id.selectHospitalButtonLayout);
        selectHospitalButtonLayout.setOnClickListener(v -> selectInterfaceAndRefreshData(SelectLocationType.HOSPITAL));

        selectBaseButtonLayout = selectToolbar.findViewById(R.id.selectBaseButtonLayout);
        selectBaseButtonLayout.setOnClickListener(v -> selectInterfaceAndRefreshData(SelectLocationType.BASE));

        selectOtherButtonLayout = selectToolbar.findViewById(R.id.selectOtherButtonLayout);
        selectOtherButtonLayout.setOnClickListener(v -> selectInterfaceAndRefreshData(SelectLocationType.OTHER));

        selectSelectButtonLayout = selectToolbar.findViewById(R.id.selectSelectButtonLayout);
        selectSelectButtonLayout.setOnClickListener(v -> selectInterfaceAndRefreshData(SelectLocationType.SELECT));

        // ok button
        selectButtonOkLayout = rootView.findViewById(R.id.selectButtonOkLayout);
        selectButtonOk = rootView.findViewById(R.id.selectButtonOk);
        selectButtonOk.setOnClickListener(v -> addWaypoint());

        selectButtonOk.setEnabled(false);
        selectButtonOkLayout.setVisibility(View.GONE);

        // search layout and button
        searchLayout = rootView.findViewById(R.id.searchLayout);
        searchEditText = rootView.findViewById(R.id.searchText);
        searchEditText.addTextChangedListener(new TextWatcher() {

            CountDownTimer timer = null;

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString();
                if (text.length() > 4) {
                    // cancel current timer first
                    if (timer != null) {
                        timer.cancel();
                    }
                    // start new timer
                    timer = new CountDownTimer(1000, 1500) {
                        @Override
                        public void onTick(long l) {
                            // will never be received
                        }

                        @Override
                        public void onFinish() {
                            searchPlaces(text);
                        }
                    }
                            .start();
                }
            }

        });
        ImageView searchCloseButton = rootView.findViewById(R.id.searchCloseButton);
        searchCloseButton.setOnClickListener(v -> {
            searchEditText.setText("");
            initializeSearch();
        });
        ImageView searchLocationButton = rootView.findViewById(R.id.searchLocationButton);
        searchLocationButton.setOnClickListener(v -> {
            Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
            if (ambulance != null) {
                // select marker
                selectDropMarker(ambulance.getLocation().add(.0001f, .0001f).toLatLng());
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
        mapHeight = mapWidth = -1;
        mapFragmentLayout = rootView.findViewById(R.id.mapFragment);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapFragment);
        Objects.requireNonNull(mapFragment).getMapAsync(this);

        // get arguments
        processArguments();

        // set type
        type = SelectLocationType.HOSPITAL;

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity activity = (MainActivity) requireActivity();

        // setup navigation
        activity.setupNavigationBar();

        if (googleMap == null) {

            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                    .findFragmentById(R.id.mapFragment);
            Objects.requireNonNull(mapFragment).getMapAsync(this);

        }

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

        // Add on click listener
        googleMap.setOnMarkerClickListener(this);

        // Add on window click listener
        googleMap.setOnInfoWindowClickListener(this);

        // Add drag listener
        googleMap.setOnMarkerDragListener(this);

        // Set info window adapter
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Nullable
            @Override
            public View getInfoContents(@NonNull Marker marker) {

                // get marker
                NamedAddress namedAddress = (NamedAddress) marker.getTag();
                if (namedAddress != null) {
                    View view = getLayoutInflater().inflate(R.layout.google_maps_location_info_window, null);

                    String title = namedAddress.getName();
                    if (title != null && !title.equals("")) {

                        ((TextView) view.findViewById(R.id.title)).setText(title);

                    } else {

                        ((TextView) view.findViewById(R.id.title)).setVisibility(View.GONE);

                    }

                    ((TextView) view.findViewById(R.id.content))
                            .setText(namedAddress.toAddress(requireContext()));

                    return view;
                } else {
                    return null;
                }
            }

            @Nullable
            @Override
            public View getInfoWindow(@NonNull Marker marker) {
                return null;
            }
        });

        // get map dimensions
        mapHeight = mapFragmentLayout.getMeasuredHeight();
        mapWidth = mapFragmentLayout.getMeasuredWidth();

        // Refresh data
        selectInterfaceAndRefreshData(this.type, true);

    }

    public void processArguments() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            Log.d(TAG, "Has arguments");

            MainActivity activity = (MainActivity) requireActivity();
            String action = arguments.getString(MainActivity.ACTION);
            if (action != null) {
                Log.d(TAG, "Process arguments");

                int ambulanceId = arguments.getInt("ambulanceId", -1);
                int callId = arguments.getInt("callId", -1);

                AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                Ambulance ambulance = appData.getAmbulance();
                Call call = appData.getCalls().getCurrentCall();

                boolean invalidCall = true;
                if (ambulance != null && call != null) {

                    // get ambulance call and waypoint
                    AmbulanceCall ambulanceCall = call.getCurrentAmbulanceCall();
                    Waypoint waypoint = ambulanceCall.getNextWaypoint();
                    if (action.equals(MainActivity.ACTION_ADD_WAYPOINT) &&
                            ambulance.getId() == ambulanceId &&
                            ambulanceCall.getAmbulanceId() == ambulanceId &&
                            call.getId() == callId &&
                            waypoint == null) {
                        Log.d(TAG, "All parameters are consistent");
                        invalidCall = false;
                    }

                }

                if (invalidCall) {
                    Log.d(TAG, String.format("Ambulance '%d' and call '%d' are not current", ambulanceId, callId));
                    new SimpleAlertDialog(activity, getString(R.string.alert_warning_title))
                            .alert(getString(R.string.cannotAddWaypoint), (dialogInterface, i) -> activity.navigateUp());
                }

            }
        } else {
            Log.d(TAG, "Has no arguments");
        }
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

    private void selectInterfaceAndRefreshData(SelectLocationType type) {
        selectInterfaceAndRefreshData(type, false);
    }

    private void selectInterfaceAndRefreshData(SelectLocationType type, boolean force) {

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

    private void initializeSearch() {

        lastQueryText = null;
        searchLayout.setVisibility(View.VISIBLE);

        // hide bottom sheet
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        // reset recyclerview
        PlacesRecyclerAdapter adapter = new PlacesRecyclerAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // hide list
        recyclerView.setVisibility(View.GONE);

    }

    /**
     * refreshData
     */
    public void refreshData() {

        // clear selection
        if (selectedLocation != null && selectedLocationMarker != null) {
            // remove pin from map
            selectedLocationMarker.remove();
        }
        selectedLocation = null;

        selectButtonOk.setEnabled(false);
        selectButtonOkLayout.setVisibility(View.GONE);

        if (type == SelectLocationType.SELECT) {

            // initialize search
            initializeSearch();

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

            // make bottom sheet collapsed if hidden
            if (bottomSheetBehavior.isHideable()) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                bottomSheetBehavior.setHideable(false);
            }

        }

        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
        if (ambulance != null) {

            GPSLocation location = ambulance.getLocation();

            LatLngBounds.Builder builder = LatLngBounds.builder();
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
            recyclerView.setLayoutManager(linearLayoutManager);

            if (type != SelectLocationType.SELECT) {

                // show list
                recyclerView.setVisibility(View.VISIBLE);

                LocationRecyclerAdapter adapter;
                switch (type) {

                    case HOSPITAL:
                        adapter = new LocationRecyclerAdapter(SelectLocationType.HOSPITAL, hospitals, location, this);
                        clearAndAddLocationMarkers(SelectLocationType.HOSPITAL, hospitals, builder);
                        break;

                    case BASE:
                        adapter = new LocationRecyclerAdapter(SelectLocationType.BASE, bases, location, this);
                        clearAndAddLocationMarkers(SelectLocationType.BASE, bases, builder);
                        break;

                    default:
                    case OTHER:
                        adapter = new LocationRecyclerAdapter(SelectLocationType.OTHER, otherLocations, location, this);
                        clearAndAddLocationMarkers(SelectLocationType.OTHER, otherLocations, builder);
                        break;
                }
                recyclerView.setAdapter(adapter);

                // add current ambulance
                addCurrentAmbulanceMarker(ambulance, builder);

                // calculate bounds
                LatLngBounds mapBounds = builder.build();

                // update bounds
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {

                    // center with offset
                    int bottomSheetHeight = selectLocationBottomSheet.getMeasuredHeight();
                    LatLngBounds updatedMapBounds = padAndMatchBounds(mapBounds, .2f, mapHeight - bottomSheetHeight, mapWidth);
                    centerMap(googleMap, updatedMapBounds, 0, 0, -bottomSheetHeight/2);

                } else {

                    // center
                    LatLngBounds updatedMapBounds = padAndMatchBounds(mapBounds, .2f, mapHeight, mapWidth);
                    centerMap(googleMap, updatedMapBounds, 0);

                }

            } else {

                // clear markers
                clearMarkers(markerMap);

                // add current ambulance
                addCurrentAmbulanceMarker(ambulance, builder);

                // center map (bottom sheet is always hidden)
                centerMap(googleMap, ambulance.getLocation().toLatLng(), 0, zoomLevel, false, 0, null);

            }
        } else {
            Log.d(TAG, "ambulance was null!");
        }

    }

    private void centerMapWithOffset(Marker marker, boolean openInfoWindow) {

        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {

            // offset center if bottom sheet is expanded
            int smallMapHeight = selectLocationBottomSheet.getMeasuredHeight();
            centerMap(googleMap, marker.getPosition(), 0, -smallMapHeight / 2);

        } else {

            centerMap(googleMap, marker.getPosition(),  0, 0);

        }

        // open info window
        if (openInfoWindow && !marker.isInfoWindowShown()) {
            marker.showInfoWindow();
        }

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

                // disable select button
                selectButtonOk.setEnabled(false);

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

                                            // enable select button
                                            selectButtonOk.setEnabled(true);

                                        }

                                        @Override
                                        public void onFailure(Bundle extras) {
                                            super.onFailure(extras);

                                            // enable select button
                                            selectButtonOk.setEnabled(true);

                                        }
                                    }
                                            .setFailureMessage(getString(R.string.couldNotPostWaypoint))
                                            .setAlert(new AlertSnackbar(requireActivity()))
                                            .start();


                                })
                        .setNegativeButton(android.R.string.cancel, (d, i) -> {

                            // enable select button
                            selectButtonOk.setEnabled(true);

                        })
                        .setCancelable(false)
                        .create()
                        .show();

            }
        }

    }

    @Override
    public void onClick(@NonNull SelectLocationType type, @NonNull NamedAddress location) {
        if (type != this.type) {
            Log.d(TAG, "Wrong type, ignore");
            return;
        }

        // enable button
        selectButtonOk.setEnabled(true);
        selectButtonOkLayout.setVisibility(View.VISIBLE);

        // open bottom sheet
        openBottomSheet();

        // set selected
        selectedLocation = location;

        // get marker
        int id;
        if (type == SelectLocationType.HOSPITAL) {
            Hospital hospital = (Hospital) location;
            id = hospital.getId();
        } else {
            Location loc = (Location) location;
            id = loc.getId();
        }
        Marker marker = markerMap.get(id);
        if (marker != null) {
            // center map and open info window
            centerMapWithOffset(marker, true);
        }

    }

    @Override
    public void onClick(@NonNull AutocompletePrediction prediction) {

        Log.d(TAG, "Select prediction");

        if (type != SelectLocationType.SELECT) {
            Log.d(TAG, "Not on select, ignoring...");
            return;
        }

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

                    if (type != SelectLocationType.SELECT) {
                        Log.d(TAG, "Not on select, ignoring...");
                        return;
                    }

                    // create location
                    Place place = response.getPlace();
                    LatLng latLng = place.getLatLng();
                    AddressComponents addressComponents = place.getAddressComponents();
                    if (latLng != null) {

                        // parse address and set selected
                        Location location;
                        if (addressComponents != null) {
                            Address address = Address.parseAddressComponents(latLng, addressComponents);
                            Log.d(TAG, String.format("parsed address = %s", address));
                            location = new Location("", Location.TYPE_WAYPOINT, address);
                        } else {
                            location = new Location("", Location.TYPE_WAYPOINT, new GPSLocation(latLng));
                        }

                        // set location
                        setLocation(location);

                    } else {
                        Log.d(TAG, "Did not get LatLng");
                    }

                })
                .addOnFailureListener(exception -> Log.d(TAG, "Failed to fetch place"));

    }

    private void selectDropMarker(LatLng latLng) {

        // find address
        Address address = reverseGeocoding(latLng);
        Log.d(TAG, "address = " + address);

        // set marker
        setLocation(new Location("", Location.TYPE_WAYPOINT, address));

        // open bottom sheet
        openBottomSheet();

    }

    private void setLocation(Location location) {

        // set selected location
        selectedLocation = location;

        // set marker
        setSelectedLocationMarker();

        // enable button
        selectButtonOk.setEnabled(true);
        selectButtonOkLayout.setVisibility(View.VISIBLE);

        // hide keyboard
        // Check if no view has focus:
        FragmentActivity activity = requireActivity();
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // hide list
        recyclerView.setVisibility(View.GONE);

        // show info window
        if (selectedLocationMarker != null) {
            selectedLocationMarker.showInfoWindow();
        }

    }

    private void setSelectedLocationMarker() {

        if (googleMap != null && selectedLocation != null) {

            // remove current marker
            if (selectedLocationMarker != null) {
                selectedLocationMarker.remove();
            }

            // add marker
            LatLng latLng = selectedLocation.getLocation().toLatLng();
            selectedLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("title")
                    .snippet(selectedLocation.toAddress(requireContext()))
                    .draggable(true));

            if (selectedLocationMarker != null) {
                selectedLocationMarker.setTag(selectedLocation);
                // do not add to map!
            }

            // create bounds
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(latLng);

            Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
            if (ambulance != null) {
                builder.include(ambulance.getLocation().toLatLng());
            }

            // center map with offset
            LatLngBounds mapBounds = builder.build();
            LatLngBounds updatedMapBounds;
            int state = bottomSheetBehavior.getState();
            int bottomSheetHeight = selectLocationBottomSheet.getMeasuredHeight();
            int searchLayoutHeight = searchLayout.getMeasuredHeight();
            int yOffset = 0;
            if (state == BottomSheetBehavior.STATE_COLLAPSED || state == BottomSheetBehavior.STATE_HIDDEN) {
                // closed
                updatedMapBounds = padAndMatchBounds(mapBounds, .2f, mapHeight, mapWidth);
            } else {
                // open
                updatedMapBounds = padAndMatchBounds(mapBounds, .2f, mapHeight - bottomSheetHeight - searchLayoutHeight, mapWidth);
                yOffset = -bottomSheetHeight / 2;
            }
            centerMap(googleMap, updatedMapBounds, 0, 0, yOffset);

        }

    }

    private void addCurrentAmbulanceMarker(Ambulance ambulance, LatLngBounds.Builder builder) {

        // include ambulance
        LatLng latLng = ambulance.getLocation().toLatLng();
        builder.include(latLng);

        // Create marker
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(getMarkerBitmapDescriptor("AMBULANCE_CURRENT"))
                .rotation((float) ambulance.getOrientation())
                .anchor(.5f, .5f)
                .flat(true));

        // add to map
        markerMap.put(-1, marker);

    }

    private void clearAndAddLocationMarkers(SelectLocationType type, List<? extends NamedAddress> list, LatLngBounds.Builder builder) {

        if (googleMap != null) {

            // remove markers
            clearMarkers(markerMap);

            // get context
            Context context = requireContext();

            // add markers
            for (NamedAddress entry: list) {

                // get Location
                GPSLocation location = entry.getLocation();

                // add to bounds
                LatLng latLng = location.toLatLng();
                builder.include(latLng);

                // get icon
                String snippet;
                int id;
                if (type == SelectLocationType.HOSPITAL) {
                    Hospital hospital = (Hospital) entry;
                    id = hospital.getId();
                    snippet = hospital.toAddress(context);
                } else {
                    Location loc = (Location) entry;
                    id = loc.getId();
                    snippet = loc.toAddress(context);
                }

                float anchorU = 0.5f;
                float anchorV = 0.5f;
                boolean flat = true;
                String icon, title;
                switch (type) {
                    case HOSPITAL:
                        title = icon = "HOSPITAL";
                        break;
                    case BASE:
                        title = icon = "BASE";
                        break;
                    case OTHER:
                    default:
                    case SELECT:
                        title = "";
                        icon = "WAYPOINT_" + Location.TYPE_OTHER;
                        anchorV = 1.0f;
                        flat = false;
                        break;
                }

                // Create marker
                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .icon(getMarkerBitmapDescriptor(icon))
                        .anchor(anchorU, anchorV)
                        .title(title)
                        .snippet(snippet)
                        .flat(flat));

                // Add entry to marker
                if (marker != null) {
                    marker.setTag(entry);

                    // add to map
                    markerMap.put(id, marker);
                }

            }

        }

    }

    private void searchPlaces(String queryText) {

        if (type != SelectLocationType.SELECT) {
            Log.d(TAG, "Not on select, ignoring...");
            return;
        }

        if (lastQueryText != null && lastQueryText.startsWith(queryText)) {
            Log.d(TAG, "Same text, skipping");
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

                    if (type != SelectLocationType.SELECT) {
                        Log.d(TAG, "Not on select, ignoring...");
                        return;
                    }

                    // get predictions
                    List<AutocompletePrediction> predictions = response.getAutocompletePredictions();

                    if (predictions.size() > 0) {

                        // set up adapter with results
                        PlacesRecyclerAdapter adapter = new PlacesRecyclerAdapter(predictions, this);
                        recyclerView.setAdapter(adapter);

                        // show list
                        recyclerView.setVisibility(View.VISIBLE);

                        // open bottom sheet
                        openBottomSheet();

                    }
                    // TODO: do we do anything if no results?

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

    private void openBottomSheet() {

        // expand sheet visible
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        // disable hideable so user can interact with the results
        if (bottomSheetBehavior.isHideable()) {
            bottomSheetBehavior.setHideable(false);
        }

    }

    private Address reverseGeocoding(LatLng latLng) {

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<android.location.Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses.size() > 0) {
                Log.d(TAG, "addresses[0] = " + addresses.get(0));
                return Address.parseAddress(addresses.get(0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onMarkerDrag(@NonNull Marker marker) {

        if (marker.isInfoWindowShown()) {
            marker.hideInfoWindow();
        }

    }

    @Override
    public void onMarkerDragEnd(@NonNull Marker marker) {

        // set new position
        LatLng position = marker.getPosition();

        // update address
        Address address = reverseGeocoding(position);
        Log.d(TAG, "address = " + address);
        if (address != null) {
            // update full address
            selectedLocation.copy(address);
            // and snippet
            marker.setSnippet(selectedLocation.toAddress(requireContext()));
        } else {
            // update just the location
            selectedLocation.setLocation(new GPSLocation(position));
        }

        // show info window
        if (!marker.isInfoWindowShown()) {
            marker.showInfoWindow();
        }

    }

    @Override
    public void onMarkerDragStart(@NonNull Marker marker) {

    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {
        // get tag
        NamedAddress namedAddress = (NamedAddress) marker.getTag();
        if (namedAddress != null) {
            if (type == SelectLocationType.SELECT) {

                // get dropped marker
                openBottomSheet();

                // hide list
                recyclerView.setVisibility(View.GONE);

            } else {
                // get location
                LocationRecyclerAdapter adapter = (LocationRecyclerAdapter) recyclerView.getAdapter();
                if (adapter != null) {
                    int position = adapter.getPosition(new NamedAddressWithDistance(namedAddress));
                    if (position >= 0) {
                        // update selected item on recycler view
                        adapter.setSelectedPosition(position);
                        recyclerView.smoothScrollToPosition(position);

                        // select location
                        onClick(type, namedAddress);
                    }
                }
            }
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
//        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
//            // center map with offset and stop further processing
//            centerMapWithOffset(marker, true);
//            return true;
//        } else {
//            return false;
//        }
        // center map and stop further processing
        centerMapWithOffset(marker, true);
        return true;
    }

}