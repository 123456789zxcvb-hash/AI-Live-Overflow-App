# 保用 WebView JS 桡
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
# 保用 Gson 帺宗化
keepAttributes Signature
-keepAttributes *Annotation*
-keep class com.example.deskpet.service.** { * ;}