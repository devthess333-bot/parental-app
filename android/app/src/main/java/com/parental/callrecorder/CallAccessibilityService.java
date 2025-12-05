cd /mnt/c/xampp/htdocs/parental/call-recording-test/android/app/src/main/java/com/parental/callrecorder
cat > CallAccessibilityService.java << 'EOF'
package com.parental.callrecorder;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class CallAccessibilityService extends AccessibilityService {

    private static final String TAG = "CallAccessibilityService";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        int eventType = event.getEventType();
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        String className = event.getClassName() != null ? event.getClassName().toString() : "";

        Log.d(TAG, "Accessibility Event - Type: " + eventType +
                   ", Package: " + packageName +
                   ", Class: " + className);

        // Έλεγχος για κλήσεις - περνάμε το eventType ως παράμετρο
        detectCallEvents(event, eventType, packageName, className);
    }

    private void detectCallEvents(AccessibilityEvent event, int eventType, String packageName, String className) {
        // Επιλογή 1: Παρακολούθηση διαλόγου κλήσης
        if (className.contains("InCall") ||
            className.contains("Dialer") ||
            className.contains("Call") ||
            className.contains("Phone") ||
            packageName.contains("com.android.incallui") ||
            packageName.contains("com.android.dialer")) {

            Log.d(TAG, "Call-related screen detected: " + className);

            // Ανάλυση του κειμένου για να καταλάβουμε την κατάσταση της κλήσης
            if (event.getText() != null && !event.getText().isEmpty()) {
                for (CharSequence text : event.getText()) {
                    String textStr = text.toString();
                    Log.d(TAG, "Screen text: " + textStr);

                    if (textStr.contains("Dialing") || textStr.contains("Calling")) {
                        startCallRecording("outgoing");
                    } else if (textStr.contains("Answer") || textStr.contains("Decline")) {
                        startCallRecording("incoming");
                    } else if (textStr.contains("End") || textStr.contains("Hang up")) {
                        stopCallRecording();
                    }
                }
            }
        }

        // Επιλογή 2: Παρακολούθηση notifications
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Log.d(TAG, "Notification detected");
            if (event.getText() != null && !event.getText().isEmpty()) {
                for (CharSequence text : event.getText()) {
                    String textStr = text.toString();
                    if (textStr.contains("Incoming call") ||
                        textStr.contains("Call from") ||
                        textStr.contains("Missed call")) {
                        Log.d(TAG, "Call notification: " + textStr);
                    }
                }
            }
        }
    }

    private void startCallRecording(String callType) {
        Log.d(TAG, "Starting call recording: " + callType);

        // Έναρξη του CallRecordingService
        Intent serviceIntent = new Intent(this, CallRecordingService.class);
        serviceIntent.putExtra("call_type", callType);
        serviceIntent.putExtra("action", "start_recording");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopCallRecording() {
        Log.d(TAG, "Stopping call recording");

        Intent serviceIntent = new Intent(this, CallRecordingService.class);
        serviceIntent.putExtra("action", "stop_recording");
        stopService(serviceIntent);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility service connected");

        // Ρύθμιση του service
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        // Ενεργοποίηση παρακολούθησης όλων των events
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_CLICKED |
                         AccessibilityEvent.TYPE_VIEW_FOCUSED |
                         AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;

        // Ρύθμιση feedback type
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        // Ρύθμιση flags
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;

        // Ρύθμιση timeout
        info.notificationTimeout = 100;

        this.setServiceInfo(info);
    }
}
EOF
