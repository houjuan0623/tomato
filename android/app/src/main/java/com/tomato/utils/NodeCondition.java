// 在您的 utils 包下，例如 com.tomato.utils (或者一个更通用的地方)
package com.tomato.utils;

import android.view.accessibility.AccessibilityNodeInfo;

// 自定义函数式接口
public interface NodeCondition {
    boolean test(AccessibilityNodeInfo node);
}