package memyselfandi.mypersonaldriver.data;

/**
 * Created by llefoulon on 18/09/2017.
 */

public class LatLngLight {
    private double lat;
    private double lng;

    public LatLngLight(double lat,double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLatitude() {
        return lat;
    }

    public double getLongitude() {
        return lng;
    }
}
