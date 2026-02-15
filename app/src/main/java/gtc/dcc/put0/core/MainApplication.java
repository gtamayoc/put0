package gtc.dcc.put0.core;

import android.app.Application;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import gtc.dcc.put0.core.utils.CoreLogger;
import gtc.dcc.put0.core.utils.SharedPreferenceManager;

public class MainApplication extends Application {

    private static String tag = "PPUT0";

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferenceManager.initialize(this);
        CoreLogger.d("MainApplication Initializing...");
        configLogger();
        CoreLogger.d("MainApplication Initialized");
    }

    private void configLogger() {
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false) // (Optional) Whether to show thread info or not. Default true
                .methodCount(0) // (Optional) How many method line to show. Default 2
                .methodOffset(7) // (Optional) Hides internal method calls up to offset. Default 5
                .tag(tag) // (Optional) Global tag for every log. Default PRETTY_LOGGER
                .build();

        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));
    }

}
