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
 * 按钮 “看视频免30分钟广告” 对应的 processor
 */
public class EnterAdProcessor implements ScreenProcessor {
    @Override
    public boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        if (rootNode == null) {
            return false;
        }
        // 查找是否存在广告关闭按钮
        List<AccessibilityNodeInfo> adButtons = null;

        try {
            adButtons = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.AD_BUTTON_ID);
            // 如果找到了按钮，就说明可以处理
            return !adButtons.isEmpty();
        } finally {
            // 无论找没找到，都要回收节点列表
            AccessibilityNodeUtils.recycleNodes(adButtons);
        }
    }

    /**
     * 本processor实现点击进入广告。
     * 剩下的步骤分别在AdProcessor和AfterSkipAdProcessor中实现
     * @param service  AccessibilityEventService 的实例，用于执行点击、访问 Handler 等。
     * @param rootNode 窗口根节点。
     * @return
     */
    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> adButtons = AccessibilityNodeUtils.findNodesByResourceID(rootNode,
                AccessibilityConfig.AD_BUTTON_ID);
        if (adButtons.isEmpty()) {
            Log.w(AccessibilityConfig.TAG, "process: 未能找到广告关闭按钮，可能在 canProcess 之后界面已变化。");
            return false;
        }
        // 通常广告关闭按钮只有一个，我们点击第一个找到的
        AccessibilityNodeInfo adButton = adButtons.get(0);
        try {
            boolean clicked = AccessibilityActionUtils.performClick(service, adButton);
            if (clicked) {
                Log.i(AccessibilityConfig.TAG, "成功发起对广告关闭按钮的点击。");
                return true;
            } else {
                Log.w(AccessibilityConfig.TAG, "发起对广告关闭按钮的点击失败。");
                return false;
            }
        } finally {
            // 回收所有找到的节点
            AccessibilityNodeUtils.recycleNodes(adButtons);
        }
    }
}