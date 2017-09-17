package memyselfandi.mypersonaldriver.designpattern;

import android.app.Activity;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.geocoder.AndroidGeocoder;
import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;
import com.mapbox.services.android.ui.geocoder.GeocoderAutoCompleteView;
import com.mapbox.services.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.services.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.services.commons.models.Position;

import java.util.List;
import java.util.Locale;

import memyselfandi.mypersonaldriver.R;
import memyselfandi.mypersonaldriver.data.LatLngLight;
import memyselfandi.mypersonaldriver.data.Place;
import memyselfandi.mypersonaldriver.utils.MapListener;
import memyselfandi.mypersonaldriver.utils.PlaceHelper;
import timber.log.Timber;

/**
 * Created by llefoulon on 17/09/2017.
 */

public class MapBoxContainer implements Mappable, OnMapReadyCallback,
        GeocoderAutoCompleteView.OnFeatureListener, MapboxMap.OnMapClickListener, PermissionsListener,
        LocationEngineListener {

    private MapView mapView;
    private MapboxMap mapboxMap = null;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    private Activity activity;
    private MapListener listener;

    public MapBoxContainer(MapListener mapListener, Activity act, @NonNull View view) {
        mapView = (MapView) view.findViewById(R.id.mapView);
        activity = act;
        listener = mapListener;
    }


    @Override
    public void initMap(@NonNull View view, @Nullable Bundle bundle) {
        mapView.onCreate(bundle);
        mapView.getMapAsync(this);

        // Set up autocomplete widget
        GeocoderAutoCompleteView autocomplete = (GeocoderAutoCompleteView) view.findViewById(R.id.query);
        autocomplete.setAccessToken(Mapbox.getAccessToken());
        autocomplete.setType(GeocodingCriteria.TYPE_POI);
        autocomplete.setOnFeatureListener(this);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        onMapReady();
    }

    @Override
    public void onMapReady() {
        enableLocationPlugin();
        mapboxMap.setOnMapClickListener(this);
        if (listener == null) return;

        listener.onMapReady();
    }

    @Override
    public void onFeatureClick(CarmenFeature feature) {
        if(listener != null) {
            listener.dismissKeyboard();
        }

        Position position = feature.asPosition();
        onLocationSelected(feature.getPlaceName(), position.getLatitude(), position.getLongitude());
    }

    @Override
    public void onLocationSelected(String address, double lat, double lng) {

        if (TextUtils.isEmpty(address)) {
            Timber.e("no title for marker");
            return;
        }

        Place removed = PlaceHelper.getInstance().addPlace(new Place(address, lat, lng));
        if (removed != null) {
            //in order not to keep any references to MarkerView
            mapboxMap.removeAnnotations();
        }

        // Build marker
        mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(address));

        zoomOnPosition(lat, lng, 15);
        if (listener == null) return;

        listener.updatePlaces();
    }

    @Override
    public void zoomOnPosition(double lat, double lng, int zoom) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(lat, lng))
                .zoom(zoom)
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 3000, null);
    }

    @SuppressWarnings({"MissingPermission"})
    public void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(mapView.getContext())) {
            // Create an instance of LOST location engine
            initializeLocationEngine();

            locationPlugin = new LocationLayerPlugin(mapView, mapboxMap, locationEngine);
            locationPlugin.setLocationLayerEnabled(LocationLayerMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(activity);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    public void initializeLocationEngine() {
        locationEngine = new LostLocationEngine(mapView.getContext());
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    public void setCameraPosition(Location location) {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16));
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onStart() {
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    public void onResume() {
        mapView.onResume();
    }

    @Override
    public void onPause() {
        mapView.onPause();
    }

    @Override
    public void onStop() {
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
        mapView.onStop();

    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        PlaceHelper.getInstance().savePlaces(mapView.getContext());
        mapView.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
        if (listener == null) return;

        LatLngLight latLng = new LatLngLight(point.getLatitude(), point.getLongitude());

        listener.onMapClick(latLng);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            Toast.makeText(mapView.getContext(), R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    @SuppressWarnings({"MissingPermission"})
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            setCameraPosition(location);
            locationEngine.removeLocationEngineListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void addPlace(Place place) {
        mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(place.getLat(), place.getLng()))
                .title(place.getDescription()));
    }

    @Override
    public Address getAddressFromLatNLng(double latitude, double longitude) {
        AndroidGeocoder geocoder = new AndroidGeocoder(mapView.getContext(), Locale.getDefault());
        geocoder.setAccessToken(Mapbox.getAccessToken());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            return addresses.get(0);
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }

    }
}
