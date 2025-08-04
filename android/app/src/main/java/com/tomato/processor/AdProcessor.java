package com.tomato.processor;

import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tomato.nativeaccessibility.AccessibilityEventService;
import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理器，用于处理广告。
 * - 如果同时出现“反馈”和“关闭”按钮，则立即点击“关闭”。
 * - 如果只出现其中一个，则等待35秒后再次检查并尝试关闭。
 */
public class AdProcessor implements ScreenProcessor {
    private static volatile boolean isWaitTaskScheduled = false;

    @Override
    public boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        if (rootNode == null || isWaitTaskScheduled) {
            return false;
        }
        // 只要能找到“反馈”或“关闭”中的任何一个，就认为此处理器可能需要介入
        List<AccessibilityNodeInfo> feedbackNodes = findAdNodes(rootNode, AccessibilityConfig.AD_FEEDBACK_TEXT);
        List<AccessibilityNodeInfo> closeNodes = findAdNodes(rootNode, AccessibilityConfig.AD_CLOSE_TEXT);

        boolean canProcess = !feedbackNodes.isEmpty() || !closeNodes.isEmpty();

        // 回收本次查找的节点
        AccessibilityNodeUtils.recycleNodes(feedbackNodes);
        AccessibilityNodeUtils.recycleNodes(closeNodes);

        return canProcess;
    }

    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> feedbackNodes = findAdNodes(rootNode, AccessibilityConfig.AD_FEEDBACK_TEXT);
        List<AccessibilityNodeInfo> closeNodes = findAdNodes(rootNode, AccessibilityConfig.AD_CLOSE_TEXT);

        // 场景1: 两个按钮都存在，立即点击“关闭”
        if (!feedbackNodes.isEmpty() && !closeNodes.isEmpty()) {
            Log.i(AccessibilityConfig.TAG, "AdProcessor: 发现广告反馈和关闭按钮，立即点击关闭。");
            boolean clicked = AccessibilityActionUtils.performClick(service, closeNodes.get(0));
            AccessibilityNodeUtils.recycleNodes(feedbackNodes);
            AccessibilityNodeUtils.recycleNodes(closeNodes);
            return clicked;
        }

        // 场景2: 只找到其中一个，计划一个延迟任务再次检查
        if (!feedbackNodes.isEmpty() || !closeNodes.isEmpty()) {
            Log.i(AccessibilityConfig.TAG, "AdProcessor: 发现潜在广告，计划在 " + (AccessibilityConfig.AD_CHECK_DELAY_MS / 1000) + " 秒后再次检查。");
            isWaitTaskScheduled = true;

            Runnable checkAdRunnable = () -> {
                Log.i(AccessibilityConfig.TAG, "AdProcessor: 执行延迟的广告检查任务...");
                isWaitTaskScheduled = false; // 重置标志
                AccessibilityNodeInfo currentRoot = service.getRootInActiveWindow();
                if (currentRoot == null) {
                    Log.w(AccessibilityConfig.TAG, "AdProcessor: 延迟检查时无法获取根节点。");
                    return;
                }
                try {
                    List<AccessibilityNodeInfo> finalFeedback = findAdNodes(currentRoot, AccessibilityConfig.AD_FEEDBACK_TEXT);
                    List<AccessibilityNodeInfo> finalClose = findAdNodes(currentRoot, AccessibilityConfig.AD_CLOSE_TEXT);

                    if (!finalFeedback.isEmpty() && !finalClose.isEmpty()) {
                        Log.i(AccessibilityConfig.TAG, "AdProcessor: 延迟检查后，成功找到广告并点击关闭。");
                        AccessibilityActionUtils.performClick(service, finalClose.get(0));
                    } else {
                        Log.i(AccessibilityConfig.TAG, "AdProcessor: 延迟检查后，未发现完整的广告按钮对。");
                    }
                    AccessibilityNodeUtils.recycleNodes(finalFeedback);
                    AccessibilityNodeUtils.recycleNodes(finalClose);
                } finally {
                    currentRoot.recycle();
                }
            };

            service.getHandler().postDelayed(checkAdRunnable, AccessibilityConfig.AD_CHECK_DELAY_MS);
            AccessibilityNodeUtils.recycleNodes(feedbackNodes);
            AccessibilityNodeUtils.recycleNodes(closeNodes);
            return true; // 表示已处理（通过调度任务）
        }

        AccessibilityNodeUtils.recycleNodes(feedbackNodes);
        AccessibilityNodeUtils.recycleNodes(closeNodes);
        return false;
    }

    /**
     * 查找具有特定文本和类名的广告相关节点。
     */
    private List<AccessibilityNodeInfo> findAdNodes(AccessibilityNodeInfo rootNode, String text) {
        List<AccessibilityNodeInfo> matchingNodes = new ArrayList<>();
        if (rootNode == null) return matchingNodes;

        List<AccessibilityNodeInfo> foundNodes = rootNode.findAccessibilityNodeInfosByText(text);
        for (AccessibilityNodeInfo node : foundNodes) {
            if (node != null && AccessibilityConfig.AD_BUTTON_CLASS_NAME.equals(node.getClassName())) {
                matchingNodes.add(node);
            } else if (node != null) {
                node.recycle();
            }
        }
        return matchingNodes;
    }

    public static void resetWaitFlag() {
        isWaitTaskScheduled = false;
    }
}
