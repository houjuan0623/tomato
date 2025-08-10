package com.tomato.processor;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tomato.nativeaccessibility.AccessibilityEventService;
import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;

import java.util.List;

/**
 * 处理跳过广告后出现的 "恭喜获得免广告权益30分钟" 弹窗。
 * 这个处理器会点击“知道了”按钮，以返回到阅读界面。
 */
public class AfterSkipAdProcessor implements ScreenProcessor {
    @Override
    public boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        if (rootNode == null) {
            return false;
        }
        // 查找是否存在关闭按钮
        List<AccessibilityNodeInfo> closeButtons = null;
        try {
            closeButtons = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.AFTER_AD_CLOSE_BUTTON_ID);
            // 如果找到了按钮，就说明可以处理
            return !closeButtons.isEmpty();
        } finally {
            // 无论找没找到，都要回收节点列表
            AccessibilityNodeUtils.recycleNodes(closeButtons);
        }
    }

    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        Log.i(AccessibilityConfig.TAG, "识别到广告后的关闭按钮，执行点击操作...");
        List<AccessibilityNodeInfo> closeButtons = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.AFTER_AD_CLOSE_BUTTON_ID);

        if (closeButtons.isEmpty()) {
            Log.w(AccessibilityConfig.TAG, "AfterSkipAdProcessor: 未能找到关闭按钮，可能在 canProcess 之后界面已变化。");
            return false;
        }

        // 通常只有一个关闭按钮，我们点击第一个找到的
        AccessibilityNodeInfo closeButton = closeButtons.get(0);
        boolean clicked = AccessibilityActionUtils.performClick(service, closeButton);
        // 回收所有找到的节点
        AccessibilityNodeUtils.recycleNodes(closeButtons);
        return clicked;
    }
}
