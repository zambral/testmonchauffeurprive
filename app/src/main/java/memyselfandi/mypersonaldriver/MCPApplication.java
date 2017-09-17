package memyselfandi.mypersonaldriver;

import android.app.Application;
import android.content.Context;
import android.support.annotation.Nullable;

import com.mapbox.mapboxsdk.Mapbox;

import java.lang.ref.WeakReference;

/**
 * Created by llefoulon on 17/09/2017.
 */

public class MCPApplication extends Application {

    private static WeakReference<MCPApplication> wApp = new WeakReference<MCPApplication>(null);

    @Override
    public void onCreate() {
        super.onCreate();
        wApp = new WeakReference<MCPApplication>(this);

        Mapbox.getInstance(this, BuildConfig.MAP_API_KEY);
    }

    @Nullable
    public static Context getContext(){
        return wApp.get();
    }
}
