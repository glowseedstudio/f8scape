# ==============================================================================
# F8scape — R8 / ProGuard Configuration
# Target: Samsung Galaxy Z Fold 8 (SM-F971B / h8q)
# ==============================================================================

# Preserve line numbers for clean crash diagnostics without leaking local paths
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------------------------
# 1. Preserve all F8scape Enums (persisted by .name in DataStore Preferences)
# ------------------------------------------------------------------------------
-keep enum com.f8scape.** {
    *;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static kotlin.enums.EnumEntries getEntries();
}

# ------------------------------------------------------------------------------
# 2. Preserve Android Manifest Entry Points & System Services
# ------------------------------------------------------------------------------
-keep public class com.f8scape.F8scapeApplication { *; }
-keep public class com.f8scape.app.MainActivity { *; }
-keep public class com.f8scape.dashboard.DashboardActivity { *; }
-keep public class com.f8scape.service.CoverOverlayService { *; }
-keep public class com.f8scape.media.F8MediaNotificationListener { *; }
-keep public class com.f8scape.service.F8scapeTileService { *; }
-keep public class com.f8scape.service.BootRecoveryReceiver { *; }

# ------------------------------------------------------------------------------
# 3. Preserve Data Models, Preferences & Diagnostic Snapshots
# ------------------------------------------------------------------------------
-keep class com.f8scape.data.** { *; }
-keep class com.f8scape.cards.** { *; }
-keep class com.f8scape.diagnostics.** { *; }
-keep class com.f8scape.fold.** { *; }
-keep class com.f8scape.display.** { *; }
-keep class com.f8scape.automation.** { *; }
-keep class com.f8scape.power.** { *; }
-keep class com.f8scape.media.MediaSnapshot { *; }

# ------------------------------------------------------------------------------
# 4. Foldable Window Extensions & Hinge Posture (AndroidX Window / OEM Sidecar)
# ------------------------------------------------------------------------------
-keep class androidx.window.** { *; }
-keep interface androidx.window.** { *; }
-keep class androidx.window.extensions.** { *; }
-keep interface androidx.window.extensions.** { *; }
-keep class androidx.window.sidecar.** { *; }
-keep interface androidx.window.sidecar.** { *; }
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**

# ------------------------------------------------------------------------------
# 5. Kotlin Coroutines & Compose Runtime Safety
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.FlowKt__MigrationKt
