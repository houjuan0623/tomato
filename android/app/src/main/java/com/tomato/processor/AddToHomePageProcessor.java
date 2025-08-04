package com.tomato.processor;

import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;
import com.tomato.nativeaccessibility.AccessibilityEventService;

import java.util.List;

/**
 * 处理“Add to Home Screen”提示框的处理器。
 * 这个处理器会在识别到提示框时，点击“cancel”按钮。
 */
public class AddToHomePageProcessor implements ScreenProcessor {
    /**
     * 判断当前屏幕是否可以处理。如果有需要处理的特征元素，则返回 true。这里的特征元素是标题Add to Home Screen。
     * @param rootNode 窗口根节点，用于查找特征元素。
     * @return 是否可以处理当前屏幕。
     */
    @Override
    public boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        // 1. 检查此操作是否已完成
        if (service.getStateManager().isActionCompleted(AccessibilityConfig.ACTION_ID_DISMISS_ADD_TO_HOME_DIALOG)) {
            return false;
        }

        List<AccessibilityNodeInfo> targetNodes = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.TARGET_FOR_INPUT_BUTTON_2);
        // 如果没有找到输入框，直接返回false
        if (targetNodes.isEmpty()) {
            return false;
        }
        // 回收节点
        for (AccessibilityNodeInfo node : targetNodes) {
            if (node != null) {
                node.recycle();
            }
        }
        return true;
    }

    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        Log.i(AccessibilityConfig.TAG, "识别到提示框标题：Add to Home Screen，执行搜索操作...");
        // 首先获取节点列表
        List<AccessibilityNodeInfo> targetNodes = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.TARGET_FOR_INPUT_BUTTON_3);

        // 安全检查：如果列表为空，直接返回 false
        if (targetNodes.isEmpty()) {
            Log.w(AccessibilityConfig.TAG, "未能找到目标点击节点。");
            return false;
        }

        // 获取我们需要的节点
        AccessibilityNodeInfo targetNode = targetNodes.get(0);
        // 注意：如果列表可能包含多个节点，且您只处理第一个，
        // 理论上也应该回收列表中的其他节点，但在这里我们简化处理。
        try {
            Log.i(AccessibilityConfig.TAG, "节点找到! (ID: " + targetNode.getViewIdResourceName());
            // 使用 AccessibilityActionUtils 执行点击
            // performClick 内部会再次校验可见性和可用性作为安全措施
            boolean clickInitiated = AccessibilityActionUtils.performClick(service, targetNode);
            if (clickInitiated) {
                Log.i(AccessibilityConfig.TAG, "点击操作已成功发起。");
                // 使用状态管理器标记操作完成
                service.getStateManager().markActionAsCompleted(AccessibilityConfig.ACTION_ID_DISMISS_ADD_TO_HOME_DIALOG);
                return true;
            } else {
                Log.w(AccessibilityConfig.TAG, "点击操作发起失败 (可能节点在点击前变为不可见/不可用)。");
                return false;
            }
        } finally {
            Log.d(AccessibilityConfig.TAG, "回收 " + targetNodes.size() + " 个找到的节点。");
            // 遍历列表，回收每一个节点
            for (AccessibilityNodeInfo node : targetNodes) {
                if (node != null) {
                    node.recycle();
                }
            }
        }
    }
}
