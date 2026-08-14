# JNA accesses these classes and members from native code.
# They must keep their original names and members when R8 is enabled.
-dontwarn java.awt.*

-keep class com.sun.jna.* {  *;}
-keep class * extends com.sun.jna.* {  *;}
-keepclassmembers class * extends com.sun.jna.* { public *;}
