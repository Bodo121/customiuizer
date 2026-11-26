-verbose

# libxposed API 101 module entry and lifecycle callbacks
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keepattributes RuntimeVisibleAnnotations
-keep,allowoptimization,allowobfuscation public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
    public void onModuleLoaded(...);
    public void onPackageReady(...);
    public void onSystemServerStarting(...);
}

-keep,allowoptimization class name.monwf.customiuizer.mods.utils.HookerClassHelper$MethodHook {
    <methods>;
}

-keepnames class name.monwf.customiuizer.GateWayLauncher

# Obfuscation
-repackageclasses
-allowaccessmodification

-dontwarn kotlin.jvm.internal.SourceDebugExtension
-dontwarn android.**
-dontwarn android.view.**
-dontwarn miui.**
