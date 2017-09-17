package memyselfandi.mypersonaldriver;

import android.app.Application;

import com.mapbox.mapboxsdk.Mapbox;

/**
 * Created by llefoulon on 17/09/2017.
 */

public class MCPApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Mapbox.getInstance(this, BuildConfig.MAP_API_KEY);
    }
}
