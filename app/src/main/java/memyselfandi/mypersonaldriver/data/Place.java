package memyselfandi.mypersonaldriver.data;

/**
 * Created by llefoulon on 17/09/2017.
 */

public class Place {
    private String description;
    private double lat;
    private double lng;

    public Place(String desc,double lat,double lng) {
        this.description = desc;
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng(){
        return lng;
    }

    public String getDescription(){
        return description;
    }
}
