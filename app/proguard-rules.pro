# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===== Tunable Annotation System Rules =====
# Keep the @Tunable annotation
-keep @interface com.particlesdevs.photoncamera.settings.annotations.Tunable
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# Keep all classes with @Tunable annotated fields
-keep class * {
    @com.particlesdevs.photoncamera.settings.annotations.Tunable <fields>;
}

# Keep field names for @Tunable annotated fields (needed for reflection)
-keepclassmembers class * {
    @com.particlesdevs.photoncamera.settings.annotations.Tunable <fields>;
}

# Keep the Tunable system classes
-keep class com.particlesdevs.photoncamera.settings.annotations.** { *; }
-keep class com.particlesdevs.photoncamera.settings.TunableInjector { *; }
-keep class com.particlesdevs.photoncamera.settings.TunablePreferenceGenerator { *; }
-keep class com.particlesdevs.photoncamera.settings.TunablePreferenceGenerator$* { *; }
-keep class com.particlesdevs.photoncamera.settings.TunableSettingsManager { *; }
-keep class com.particlesdevs.photoncamera.settings.SettingsManagerExtensions { *; }
-keep class com.particlesdevs.photoncamera.ui.settings.custompreferences.TunableSeekBarPreference { *; }

# Keep all postpipeline nodes with @Tunable fields for reflection
-keepclassmembers class com.particlesdevs.photoncamera.processing.opengl.postpipeline.** {
    @com.particlesdevs.photoncamera.settings.annotations.Tunable <fields>;
}
-keep class org.chickenhook.restrictionbypass.*{
    public *;
}

# Missing class rules
-dontwarn org.apache.commons.lang.functor.Predicate
