# Add project specific ProGuard rules here.
# For more details, see http://developer.android.com/guide/developing/tools/proguard.html

# The following rules are used to inline the logging methods if needed.
# These match the classes in this module.

-alwaysinline class cats.AndroidLoggerKt {
    public static void log(...);
}

-alwaysinline class * implements cats.Logger {
    <methods>;
}