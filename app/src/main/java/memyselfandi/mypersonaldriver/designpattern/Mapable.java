package memyselfandi.mypersonaldriver.designpattern;

import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import memyselfandi.mypersonaldriver.data.Place;

/**
 * Created by llefoulon on 17/09/2017.
 */

public interface Mapable {
    void initMap(@NonNull View view, @Nullable Bundle bundle);

    void onMapReady();

    void onLocationSelected(String desc, double lat, double lng);

    void zoomOnPosition(double lat, double lng, int zoom);

    void enableLocationPlugin();

    void initializeLocationEngine();

    void setCameraPosition(Location location);

    void onStart();

    void onPause();

    void onResume();

    void onSaveInstanceState(Bundle bundle);

    void onStop();

    void onDestroy();

    void onLowMemory();

    void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);

    void addPlace(Place place);

    Address getAddressFromLatNLng(double latitude, double longitude);
}
