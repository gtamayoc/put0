package gtc.dcc.put0.core.utils;

import androidx.annotation.NonNull;
import leakcanary.EventListener;
import leakcanary.LeakCanary;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom LeakCanary event listener to ensure all events (even when no leaks are
 * found)
 * are logged to our centralized CoreLogger.
 */
public class LeakEventListener implements EventListener {

    public static void setup() {
        LeakCanary.Config config = LeakCanary.getConfig();
        List<EventListener> listeners = new ArrayList<>(config.getEventListeners());
        listeners.add(new LeakEventListener());

        LeakCanary.setConfig(config.newBuilder()
                .eventListeners(listeners)
                .build());
    }

    @Override
    public void onEvent(@NonNull Event event) {
        CoreLogger.d("LeakCanary Event: " + event.getClass().getSimpleName());

        // Specifically catch the end of analysis even if no leaks were found
        if (event instanceof Event.HeapAnalysisDone) {
            CoreLogger.d("LeakCanary: Heap analysis completed.");
            Event.HeapAnalysisDone doneEvent = (Event.HeapAnalysisDone) event;
            CoreLogger.d("Result: " + doneEvent.getHeapAnalysis().getClass().getSimpleName());
        }
    }
}
