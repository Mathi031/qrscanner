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

# Preserve line numbers para des-ofuscar los stacktraces de crashes en Play Console.
-keepattributes SourceFile,LineNumberTable
# Ocultar el nombre original del archivo fuente (sustituido por "SourceFile").
-renamesourcefileattribute SourceFile

# =====================================================================
# Reglas de release para QR Scanner (minify + shrink habilitados)
# =====================================================================

# --- Kotlin metadata ---
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# --- kotlinx.serialization ---
# Conserva los serializers generados de las @Serializable (ScanResult, ContentType).
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class dev.mathi031.qrscanner.**$$serializer { *; }
-keepclassmembers class dev.mathi031.qrscanner.** {
    *** Companion;
}
-keepclasseswithmembers class dev.mathi031.qrscanner.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# --- CameraX ---
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# --- ML Kit Barcode Scanning (modelo bundled) ---
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-dontwarn com.google.mlkit.**

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- Jetpack Compose (R8 ya trae reglas; esto es defensivo) ---
-keep class androidx.compose.runtime.** { *; }

# --- Quick Settings Tile service (instanciado por el sistema vía Manifest) ---
-keep class dev.mathi031.qrscanner.tile.QrScannerTileService { *; }

# --- Application class (referenciada en Manifest) ---
-keep class dev.mathi031.qrscanner.QrScannerApplication { *; }