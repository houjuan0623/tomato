package com.tomato.nativeaccessibility;

import androidx.annotation.NonNull;

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
}
