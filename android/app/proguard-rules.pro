# Keep our application classes
-keep public class com.parental.callrecorder.** {
    public protected *;
}

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep Android support library classes
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**
