package memyselfandi.mypersonaldriver;

import android.location.Address;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

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
import memyselfandi.mypersonaldriver.data.LatLngLight;
import memyselfandi.mypersonaldriver.data.Place;
import memyselfandi.mypersonaldriver.designpattern.MapBoxContainer;
import memyselfandi.mypersonaldriver.utils.MapListener;
import memyselfandi.mypersonaldriver.utils.OnRecyclerItemClickListener;
import memyselfandi.mypersonaldriver.utils.PlaceHelper;
import timber.log.Timber;

//BROKEN : https://github.com/mapbox/mapbox-android-demo/blob/master/MapboxAndroidDemo/src/main/java/com/mapbox/mapboxandroiddemo/examples/location/BasicUserLocation.java
//WORKING : https://github.com/mapbox/mapbox-android-demo/blob/master/MapboxAndroidDemo/src/main/java/com/mapbox/mapboxandroiddemo/examples/plugins/LocationPluginActivity.java
public class MainActivity extends AppCompatActivity implements OnRecyclerItemClickListener, MapListener {

    private MapBoxContainer mapBoxContainer;

    @BindView(R.id.recycler_view_left_drawer)
    RecyclerView recyclerview;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    private Disposable currentDisposable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        View mainView = findViewById(android.R.id.content);
        mapBoxContainer = new MapBoxContainer(this, this, mainView);
        mapBoxContainer.initMap(mainView, savedInstanceState);
        initActionBar();
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


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mapBoxContainer.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
    public void onStart() {
        super.onStart();
        mapBoxContainer.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapBoxContainer.onResume();
    }

    @Override
    public void onPause() {
        mapBoxContainer.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        mapBoxContainer.onStop();
        super.onStop();

    }

    @Override
    public void onLowMemory() {
        mapBoxContainer.onLowMemory();
        super.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        mapBoxContainer.onDestroy();

        PlaceHelper.getInstance().savePlaces(this);
        this.currentDisposable = null;
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mapBoxContainer.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
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
            mapBoxContainer.addPlace(place);
        }
    }

    private void getAddressFromGeoPoint(@NonNull LatLngLight point) {
        if (this.currentDisposable != null) return;

        Observable.just(point)
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<LatLngLight, ObservableSource<Address>>() {
                    @Override
                    public ObservableSource<Address> apply(LatLngLight latLng) {
                        Address address = mapBoxContainer.getAddressFromLatNLng(latLng.getLatitude(), latLng.getLongitude());
                        if (address == null)
                            return Observable.error(new Throwable("address null"));

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
                mapBoxContainer.onLocationSelected(address.getAddressLine(0), address.getLatitude(), address.getLongitude());
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
    public void onMapClick(LatLngLight point) {
        getAddressFromGeoPoint(point);
    }

    @Override
    public void updatePlaces() {
        ((PlaceAdapter) recyclerview.getAdapter()).updatePlaces();
    }

    @Override
    public void onMapReady() {
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

    @Override
    public void dismissKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception exception) {
            Timber.e(exception);
        }

    }


    @Override
    public void onItemClickListener(View view, int position) {
        if (view.getId() == R.id.cell_place) {
            Place place = ((PlaceAdapter) recyclerview.getAdapter()).getItem(position);
            if (place == null) return;

            mapBoxContainer.zoomOnPosition(place.getLat(), place.getLng(), 16);
            drawerLayout.closeDrawer(Gravity.LEFT);
        }
    }
}
