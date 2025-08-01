package com.tomato.utils;

import com.tomato.nativeaccessibility.AccessibilityEventService;
import android.view.accessibility.AccessibilityNodeInfo;

public interface ScreenProcessor {

    /**
     * 判断当前界面是否可以由本处理器处理。
     * @param service AccessibilityEventService 的实例，用于访问状态管理器等。
     * @param rootNode 窗口根节点，用于查找特征元素。
     * @return 如果是本处理器负责的界面，返回 true。
     */
    boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode);

    /**
     * 对当前界面执行具体的操作。
     * @param service AccessibilityEventService 的实例，用于执行点击、访问 Handler 等。
     * @param rootNode 窗口根节点。
     */
    boolean  process(AccessibilityEventService service, AccessibilityNodeInfo rootNode);
}
