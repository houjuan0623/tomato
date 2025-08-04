package com.tomato.nativeaccessibility;

import androidx.annotation.NonNull;
import android.util.Log;


import com.facebook.react.bridge.ReactApplicationContext;

import com.tomato.NativeAccessibilitySpec;
import com.tomato.utils.ActionStateManager;
import com.tomato.utils.State;

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
        // 重置状态管理器
        ActionStateManager.getInstance().resetState();
        // 这里可以调用无障碍服务来执行搜索操作
        // 例如，发送一个 Intent 或者直接调用服务的方法
        Log.d(NAME, "从 RN 接收到搜索查询: " + query);
        // 将从 RN 接收到的字符串存储到我们的单例状态管理器中
        State.getInstance().setNovelNameToSearch(query);

        // query会接收一个字符串，这个字符串可能是用/分开的，如果字符串使用/分开，将其拆分为数组
//        String[] queryParts;
//        if (query != null && query.contains("/")) {
//            queryParts = query.split("/");
//        } else {
//            // 如果 query 为 null 或者不包含 /，则将其视为单个搜索词
//            queryParts = new String[]{query};
//        }
    }
}
