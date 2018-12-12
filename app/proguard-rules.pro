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

# Apollo
-dontwarn com.esotericsoftware.kryo.**
-dontwarn org.objenesis.instantiator.**
-dontwarn org.codehaus.**
-dontwarn java.nio.**
-dontwarn java.lang.invoke.**
-keep class com.lsxiao.apollo.generate.** { *; }

# rxjava
-dontwarn io.reactivex.**
-keep class io.reactivex.** { *;}
-dontwarn org.reactivestreams.**
-keep class org.reactivestreams.** { *;}
-dontwarn org.apache.http.**
-keep class org.apache.http.** { *;}

# rxlifecycle
-keep class com.trello.rxlifecycle2.** { *; }
-dontwarn javax.annotation.**

# EventBus
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

# Other
-libraryjars ../app/libs

# Java
-keep class java.** { *; }
-dontnote java.**
-dontwarn java.**

-keep class javax.** { *; }
-dontnote javax.**
-dontwarn javax.**

-keep class sun.misc.Unsafe { *; }
-dontnote sun.misc.Unsafe

-keep class javax.xml.stream.XMLOutputFactory { *; }

# Chart Engine
-keep class org.achartengine.** { *; }
-dontnote org.achartengine.**

# Simple XML
-keep public class org.simpleframework.** { *; }
-keep class org.simpleframework.xml.** { *; }
-keep class org.simpleframework.xml.core.** { *; }
-keep class org.simpleframework.xml.util.** { *; }

-keepattributes ElementList, Root, InnerClasses, LineNumberTable

-keepclasseswithmembers class * {
    @org.simpleframework.xml.* <fields>;
}

# (the rt.jar has them)
-dontwarn com.bea.xml.stream.**
-dontwarn javax.xml.stream.events.**
