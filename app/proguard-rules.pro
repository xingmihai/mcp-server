# R8 代码压缩/混淆已启用；以下规则保护反射与 JNI 调用不被破坏。
# 通用属性
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keepclassmembers class * {
    native <methods>;
}

# Shizuku：ScriptTools 通过 Class.forName("rikka.shizuku.Shizuku")
# + getDeclaredMethod("newProcess", ...) 反射调用，类名与方法名必须保留
-keep class rikka.shizuku.Shizuku { *; }
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }

# QuickJS：JNI 绑定与 Java <-> JS 回调接口，类名/方法名需保持
-keep class com.quickjs.** { *; }
# ScriptTools.JsConsole 通过 addJavascriptInterface 暴露给 JS，
# log/info/warn/error 仅被 JS 反射调用，必须保留类名与方法名
-keep class com.mcp.server.tools.ScriptTools$JsConsole { *; }

# Gson（依赖已引入）：保护反射序列化所需的注解与模型字段
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# kotlinx-coroutines：避免内部 volatile 字段被优化、ServiceLoader 工厂被移除
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 隐藏系统 API 反射（android.os.SystemProperties）不在 APK 内，无需 keep
