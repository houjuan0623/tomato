package com.tomato.processor;

import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;
import com.tomato.nativeaccessibility.AccessibilityEventService;

import java.util.List;

public class MainPageProcessor implements ScreenProcessor {
    @Override
    public boolean canProcess(AccessibilityNodeInfo rootNode) {
        // TARGET_FOR_INPUT_BUTTON_1 在其他页面也有出现，所以这里要判断只有在首页才会返回为true
        List<AccessibilityNodeInfo> targetNodes1 = null;
        List<AccessibilityNodeInfo> targetNodes2 = null;

        try {
            // TARGET_FOR_INPUT_BUTTON_1 在其他页面也有出现，所以这里要判断只有在首页才会返回为true
            targetNodes1 = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.TARGET_FOR_INPUT_BUTTON_5);
            if (!(targetNodes1.isEmpty())) {
                targetNodes2 = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.TARGET_FOR_INPUT_BUTTON_1);
                return !(targetNodes2.isEmpty());
            } else {
                return false;
            }
        } finally {
            // 确保在任何情况下都回收节点
            if (targetNodes1 != null) {
                for (AccessibilityNodeInfo node : targetNodes1) {
                    if (node != null) {
                        node.recycle();
                    }
                }
            }
            if (targetNodes2 != null) {
                for (AccessibilityNodeInfo node : targetNodes2) {
                    if (node != null) {
                        node.recycle();
                    }
                }
            }
        }
    }

    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        Log.i(AccessibilityConfig.TAG, "识别到首页搜索按钮，执行点击操作...");
        // 首先获取节点列表
        List<AccessibilityNodeInfo> targetNodes = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.TARGET_FOR_INPUT_BUTTON_1);

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
                Log.i(AccessibilityConfig.TAG, "点击操作已成功发起。设置 hasClickedOnThisScreen = true。");
                service.markActionAsCompleted();
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
