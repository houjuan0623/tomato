package com.tomato.processor;

import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;
import com.tomato.nativeaccessibility.AccessibilityEventService;

import java.util.List;

public class SearchNovelProcessor implements ScreenProcessor {
    @Override
    public boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        if (rootNode == null) {
            return false;
        }

        // 1. 检查此操作是否已完成
        if (service.getStateManager().isActionCompleted(AccessibilityConfig.ACTION_ID_CLICK_SEARCH_BUTTON)) {
            return false;
        }

        // 2. 检查界面特征：必须同时存在搜索按钮和有文本的输入框
        List<AccessibilityNodeInfo> searchButtons = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.TARGET_FOR_INPUT_BUTTON_6);
        boolean hasSearchButton = !searchButtons.isEmpty();

        List<AccessibilityNodeInfo> inputFields = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.TARGET_FOR_INPUT_BUTTON_4);
        boolean hasInputFieldWithText = false;

        if (!inputFields.isEmpty()) {
            AccessibilityNodeInfo inputField = inputFields.get(0);
            // 检查文本是否不为 null 且不为空
            if (inputField.getText() != null && inputField.getText().length() > 0) {
                hasInputFieldWithText = true;
            }
        }

        // 回收节点
        recycleNodes(searchButtons);
        recycleNodes(inputFields);

        // 只有当同时找到搜索按钮和有文本的输入框，且操作未完成时，才返回true
        return hasSearchButton && hasInputFieldWithText;
    }

    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        Log.i(AccessibilityConfig.TAG, "识别到搜索页搜索按钮，执行搜索操作...");

        // 首先获取搜索节点列表
        List<AccessibilityNodeInfo> targetNodes = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.TARGET_FOR_INPUT_BUTTON_6);
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
                service.getStateManager().markActionAsCompleted(AccessibilityConfig.ACTION_ID_CLICK_SEARCH_BUTTON);
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

    /**
     * 辅助方法，用于统一回收 AccessibilityNodeInfo 节点列表。
     */
    private void recycleNodes(List<AccessibilityNodeInfo> nodes) {
        if (nodes != null) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null) {
                    node.recycle();
                }
            }
        }
    }
}
