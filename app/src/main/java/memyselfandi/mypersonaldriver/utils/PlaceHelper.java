package memyselfandi.mypersonaldriver.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Iterator;

import memyselfandi.mypersonaldriver.MCPApplication;
import memyselfandi.mypersonaldriver.data.Place;

/**
 * Created by llefoulon on 17/09/2017.
 */

public class PlaceHelper {

    private static final String PLACE_NAMESPACE = "places";
    private static final String PLACES_KEY = "places";
    private static final int MAX_AMOUNT = 15;

    private static final class PlaceHelperHolder {
        private static final PlaceHelper INSTANCE = new PlaceHelper(MCPApplication.getContext());
        private PlaceHelperHolder(){}
    }

    public static PlaceHelper getInstance(){
        return PlaceHelperHolder.INSTANCE;
    }

    private ArrayDeque<Place> places = null;

    private PlaceHelper(@Nullable Context ctx){
        if(ctx == null) return;

        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PLACE_NAMESPACE,Context.MODE_PRIVATE);
        String placesString = sharedPreferences.getString(PLACES_KEY,null);
        if(placesString != null) {
            Type listType = new TypeToken<ArrayDeque<Place>>(){}.getType();
            places = new Gson().fromJson(placesString, listType);
        } else {
            places = new ArrayDeque<>(MAX_AMOUNT);
        }

    }

    public Place[] getPlaces() {
        if(places == null || places.isEmpty()) return null;

        Place[] array = new Place[places.size()];
        int count = 0;
        for (Iterator<Place> it = places.iterator(); it.hasNext();) {
            array[count++] = it.next();
        }
        return array;
    }

    public Place addPlace(@NonNull Place place) {
        Place removed = null;
        if(places.size() >= PlaceHelper.MAX_AMOUNT) {
            removed = places.pollLast();
        }

        places.addFirst(place);
        return removed;
    }

    public void savePlaces(@NonNull Context ctx){
        SharedPreferences sharedPreferences = ctx.getSharedPreferences(PLACE_NAMESPACE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PLACES_KEY,new Gson().toJson(places));
        editor.apply();
    }
}
