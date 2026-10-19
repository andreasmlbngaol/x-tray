# X-Tray release shrinking rules.
# Shrinking and optimisation are on; obfuscation stays off so stack traces stay readable.

-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# Entry point (also written by the Compose plugin).
-keep public class id.andreasmlbngaol.xtray.app.MainKt {
    public static void main(java.lang.String[]);
}

# JNA / X11 bindings: JNA reflects on interfaces and structure layouts.
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { public *; }
-keepclassmembers class * implements com.sun.jna.Library { *; }
-keepclassmembers class * extends com.sun.jna.Structure { *; }
-keepclassmembers class * extends com.sun.jna.PointerType { *; }
-dontwarn java.awt.**

# dbus-java: marshals arguments by reflection over annotated interfaces.
-keep class org.freedesktop.dbus.** { *; }
-keep class com.github.hypfvieh.** { *; }
-keep @org.freedesktop.dbus.annotations.DBusInterfaceName interface * { *; }
-keepclassmembers class * implements org.freedesktop.dbus.interfaces.DBusInterface { *; }
-keepclassmembers class * implements org.freedesktop.dbus.interfaces.DBusSerializable { *; }
-keepclassmembers class * extends org.freedesktop.dbus.messages.DBusSignal { *; }
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
-dontwarn javax.inject.**

# SLF4J providers are discovered through META-INF/services only.
-keep class org.slf4j.nop.** { *; }
-keep class org.slf4j.helpers.** { *; }
-dontwarn org.slf4j.**

# kotlinx.serialization: generated serializers are looked up reflectively.
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class id.andreasmlbngaol.xtray.**$$serializer { *; }
-keepclassmembers class id.andreasmlbngaol.xtray.** { *** Companion; }
-keepclasseswithmembers class id.andreasmlbngaol.xtray.** { kotlinx.serialization.KSerializer serializer(...); }
