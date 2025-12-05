package com.parental.callrecorder;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadUtils {
    
    private static final String TAG = "UploadUtils";
    private static final String SERVER_URL = "https://dottie-flinty-fernanda.ngrok-free.dev/parental/call-recording-test/backend/upload.php";
    
    public static void uploadRecording(Context context, File audioFile, String phoneNumber, String callType, int duration) {
        new UploadTask().execute(new UploadData(audioFile, phoneNumber, callType, duration));
    }
    
    private static class UploadData {
        File audioFile;
        String phoneNumber;
        String callType;
        int duration;
        
        UploadData(File audioFile, String phoneNumber, String callType, int duration) {
            this.audioFile = audioFile;
            this.phoneNumber = phoneNumber;
            this.callType = callType;
            this.duration = duration;
        }
    }
    
    private static class UploadTask extends AsyncTask<UploadData, Void, Boolean> {
        
        @Override
        protected Boolean doInBackground(UploadData... params) {
            if (params.length == 0) return false;
            
            UploadData data = params[0];
            
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
                
                RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("audio_file", data.audioFile.getName(),
                        RequestBody.create(MediaType.parse("audio/*"), data.audioFile))
                    .addFormDataPart("phone_number", data.phoneNumber)
                    .addFormDataPart("call_type", data.callType)
                    .addFormDataPart("duration", String.valueOf(data.duration))
                    .addFormDataPart("device_id", android.os.Build.MODEL)
                    .addFormDataPart("timestamp", String.valueOf(System.currentTimeMillis() / 1000))
                    .build();
                
                Request request = new Request.Builder()
                    .url(SERVER_URL)
                    .post(requestBody)
                    .build();
                
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Upload successful: " + responseBody);
                    
                    // Διαγραφή του αρχείου μετά το successful upload
                    if (data.audioFile.exists()) {
                        boolean deleted = data.audioFile.delete();
                        Log.d(TAG, "Local file deleted: " + deleted);
                    }
                    
                    return true;
                } else {
                    Log.e(TAG, "Upload failed: " + response.code() + " - " + response.message());
                    return false;
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Upload error: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.d(TAG, "Upload completed successfully");
            } else {
                Log.w(TAG, "Upload failed");
            }
        }
    }
}
