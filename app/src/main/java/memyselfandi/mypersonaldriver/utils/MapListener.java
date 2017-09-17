package memyselfandi.mypersonaldriver.utils;

import memyselfandi.mypersonaldriver.data.LatLngLight;

/**
 * Created by llefoulon on 18/09/2017.
 */

public interface MapListener {
    void onMapClick(LatLngLight point);

    void updatePlaces();

    void onMapReady();

    void dismissKeyboard();
}
