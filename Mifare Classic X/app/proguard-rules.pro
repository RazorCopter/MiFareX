# ===== kotlinx-serialization =====
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class de.syss.MifareClassicTool.data.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class de.syss.MifareClassicTool.data.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ===== Room =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# ===== Legacy Java (Activities + Core) — never obfuscate =====
-keep class de.syss.MifareClassicTool.Activities.** { *; }
-keep class de.syss.MifareClassicTool.Common { *; }
-keep class de.syss.MifareClassicTool.MCReader { *; }
-keep class de.syss.MifareClassicTool.MCDiffUtils { *; }

# ===== NFC / Android framework =====
-keep class android.nfc.** { *; }
-keep class android.nfc.tech.** { *; }

# ===== Suppress known-safe warnings =====
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
