package memyselfandi.mypersonaldriver;

import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import memyselfandi.mypersonaldriver.adapter.PlaceAdapter;
import memyselfandi.mypersonaldriver.data.Place;
import memyselfandi.mypersonaldriver.utils.OnRecyclerItemClickListener;
import memyselfandi.mypersonaldriver.utils.PlaceHelper;
import timber.log.Timber;

//BROKEN : https://github.com/mapbox/mapbox-android-demo/blob/master/MapboxAndroidDemo/src/main/java/com/mapbox/mapboxandroiddemo/examples/location/BasicUserLocation.java
//WORKING : https://github.com/mapbox/mapbox-android-demo/blob/master/MapboxAndroidDemo/src/main/java/com/mapbox/mapboxandroiddemo/examples/plugins/LocationPluginActivity.java
public class MainActivity extends AppCompatActivity implements LocationEngineListener, PermissionsListener,
        GeocoderAutoCompleteView.OnFeatureListener, OnMapReadyCallback, MapboxMap.OnMapClickListener, OnRecyclerItemClickListener {

    @BindView(R.id.mapView)
    MapView mapView;

    @BindView(R.id.recycler_view_left_drawer)
    RecyclerView recyclerview;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    private MapboxMap mapboxMap = null;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;

    private Disposable currentDisposable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initActionBar();
        initMap(savedInstanceState);
        initRecyclerView();
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setHomeAsUpIndicator(R.mipmap.ic_launcher_round);
    }

    private void initRecyclerView() {
        recyclerview.setLayoutManager(new LinearLayoutManager(this));
        recyclerview.setAdapter(new PlaceAdapter(this, this));
    }

    private void initMap(Bundle savedInstanceState) {
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Set up autocomplete widget
        GeocoderAutoCompleteView autocomplete = (GeocoderAutoCompleteView) findViewById(R.id.query);
        autocomplete.setAccessToken(Mapbox.getAccessToken());
        autocomplete.setType(GeocodingCriteria.TYPE_POI);
        autocomplete.setOnFeatureListener(this);
    }

    private void updateMapWithPin(String address, double latitude, double longitude) {
        if (TextUtils.isEmpty(address)) {
            Timber.e("no title for marker");
            return;
        }

        Place removed = PlaceHelper.getInstance().addPlace(new Place(address, latitude, longitude));
        if (removed != null) {
            //in order not to keep any references to MarkerView
            mapboxMap.removeAnnotations();
        }

        // Build marker
        mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title(address));

        zoomOnPosition(latitude, longitude, 15);
        ((PlaceAdapter) recyclerview.getAdapter()).updatePlaces();
    }

    private void zoomOnPosition(double latitude, double longitude, int zoom) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(zoom)
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 3000, null);
    }

    private void hideOnScreenKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception exception) {
            Timber.e(exception);
        }

    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create an instance of LOST location engine
            initializeLocationEngine();

            locationPlugin = new LocationLayerPlugin(mapView, mapboxMap, locationEngine);
            locationPlugin.setLocationLayerEnabled(LocationLayerMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void initializeLocationEngine() {
        locationEngine = new LostLocationEngine(MainActivity.this);
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void setCameraPosition(Location location) {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {}

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                drawerLayout.closeDrawer(Gravity.LEFT);
            } else {
                drawerLayout.openDrawer(Gravity.LEFT);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
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
    @SuppressWarnings({"MissingPermission"})
    public void onStart() {
        super.onStart();
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
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
        super.onStop();

    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }

        if (this.currentDisposable != null) {
            this.currentDisposable.dispose();
        }

        PlaceHelper.getInstance().savePlaces(this);
        this.currentDisposable = null;
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mapView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onFeatureClick(CarmenFeature feature) {
        hideOnScreenKeyboard();
        Position position = feature.asPosition();
        updateMapWithPin(feature.getPlaceName(), position.getLatitude(), position.getLongitude());
    }

    private Observer<Place[]> restorePlacesObserver() {
        return new Observer<Place[]>() {
            @Override
            public void onSubscribe(Disposable d) {
                currentDisposable = d;
            }

            @Override
            public void onNext(Place[] places) {
                addPlacesToMap(places);
            }

            @Override
            public void onError(Throwable e) {
                currentDisposable = null;
            }

            @Override
            public void onComplete() {
                currentDisposable = null;
            }
        };
    }

    private void addPlacesToMap(Place[] places) {
        if (places == null || places.length == 0) return;

        for (Place place : places) {
            mapboxMap.addMarker(new MarkerOptions()
                    .position(new LatLng(place.getLat(), place.getLng()))
                    .title(place.getDescription()));
        }
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        enableLocationPlugin();
        mapboxMap.setOnMapClickListener(this);
        Observable.just("")
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<Place[]>>() {
                    @Override
                    public ObservableSource<Place[]> apply(String s) throws Exception {
                        Place[] places = PlaceHelper.getInstance().getPlaces();
                        return Observable.just(places);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(restorePlacesObserver());
    }

    private void getAddressFromGeoPoint(@NonNull LatLng point) {
        if (this.currentDisposable != null) return;

        Observable.just(point)
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<LatLng, ObservableSource<Address>>() {
                    @Override
                    public ObservableSource<Address> apply(LatLng latLng) throws Exception {
                        AndroidGeocoder geocoder = new AndroidGeocoder(MainActivity.this, Locale.getDefault());
                        geocoder.setAccessToken(Mapbox.getAccessToken());
                        List<Address> addresses = geocoder.getFromLocation(latLng.getLatitude(), latLng.getLongitude(), 1);
                        Address address = addresses.get(0);
                        return Observable.just(address);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(addressFromGeoPointObserver());
    }

    private Observer<Address> addressFromGeoPointObserver() {
        return new Observer<Address>() {
            @Override
            public void onSubscribe(Disposable d) {
                currentDisposable = d;
            }

            @Override
            public void onNext(Address address) {
                updateMapWithPin(address.getAddressLine(0), address.getLatitude(), address.getLongitude());
            }

            @Override
            public void onError(Throwable e) {
                currentDisposable = null;
                Toast.makeText(MainActivity.this, "No addresses found", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {
                currentDisposable = null;
            }
        };
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
        getAddressFromGeoPoint(point);
    }

    @Override
    public void onItemClickListener(View view, int position) {
        if (view.getId() == R.id.cell_place) {
            Place place = ((PlaceAdapter) recyclerview.getAdapter()).getItem(position);
            if (place == null) return;

            zoomOnPosition(place.getLat(), place.getLng(), 16);
            drawerLayout.closeDrawer(Gravity.LEFT);
        }
    }
}
