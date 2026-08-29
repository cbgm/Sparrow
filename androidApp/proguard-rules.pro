# JNA accesses these classes and members from native code.
# They must keep their original names and members when R8 is enabled.
-dontwarn java.awt.*

-keep class com.sun.jna.* {  *;}
-keep class * extends com.sun.jna.* {  *;}
-keepclassmembers class * extends com.sun.jna.* { public *;}

# MediaPipe Tasks Text is used by local semantic search and message-safety analysis.
# MediaPipe uses generated protobuf metadata, native/JNI entry points, and runtime
# class/stack inspection that can break when R8 renames or strips these classes.
-keep class com.google.mediapipe.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keep class com.google.common.flogger.** { *; }

