package com.tomato.nativeaccessibility;

import androidx.annotation.NonNull;
import android.util.Log;

import com.tomato.NativeAccessibilitySpec;
import com.facebook.react.bridge.ReactApplicationContext;

public class NativeAccessibilityModule extends NativeAccessibilitySpec {
    public static final String NAME = "NativeAccessibility";

    public NativeAccessibilityModule(ReactApplicationContext context) {
        super(context);
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    // 2. 实现Spec中定义的 `getModuleName` 同步方法
    @Override
    public String getModuleName() {
        return "这是来自 " + NAME + " 原生模块的确认信息！";
    }

    @Override
    public void performSearch(String query) {
        // 这里可以调用无障碍服务来执行搜索操作
        // 例如，发送一个 Intent 或者直接调用服务的方法
        Log.d(NAME, "Performing search with query: " + query);
        
        // 你可以在这里添加调用无障碍服务的逻辑
        // 比如通过发送广播或直接与 AccessibilityService 交互
    }
}
