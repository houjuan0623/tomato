package com.tomato.processor;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tomato.nativeaccessibility.AccessibilityEventService;
import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;

import java.util.List;

public class Middle1InAdProcessor implements ScreenProcessor {
    private static final String TAG = AccessibilityConfig.TAG;

    // --- 关键节点的 content-desc ---
    private static final String REWARD_CONTENT_DESC = "领取奖励";
    private static final String EXIT_CONTENT_DESC = "坚持退出";

    /**
     * 判断当前界面是否是需要处理的挽留弹窗。
     * 条件：界面中必须同时存在包含 "领取奖励" 和 "坚持退出" 文本的节点。
     *
     * @param service  无障碍服务实例
     * @param rootNode 根节点
     * @return 如果满足条件则返回 true
     */
    @Override
    public boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        if (rootNode == null) {
            return false;
        }

        List<AccessibilityNodeInfo> rewardNodes = null;
        List<AccessibilityNodeInfo> exitNodes = null;

        try {
            // 使用 content-desc 查找“领取奖励”节点
            rewardNodes = AccessibilityNodeUtils.findNodesByContentDescriptionContains(rootNode, REWARD_CONTENT_DESC);
            // 使用 content-desc 查找“坚持退出”节点
            exitNodes = AccessibilityNodeUtils.findNodesByContentDescriptionContains(rootNode, EXIT_CONTENT_DESC);

            // 当两个列表都不为空时，说明两个按钮都存在，满足处理条件
            boolean canProcess = !rewardNodes.isEmpty() && !exitNodes.isEmpty();
            if (canProcess) {
                Log.d(TAG, "Middle1InAdProcessor: 发现(基于content-desc)挽留弹窗，准备处理。");
            }
            return canProcess;

        } finally {
            // 【重要】无论如何都要回收查找过程中创建的节点列表
            AccessibilityNodeUtils.recycleNodes(rewardNodes);
            AccessibilityNodeUtils.recycleNodes(exitNodes);
        }
    }

    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        Log.i(TAG, "Middle1InAdProcessor: 正在执行点击 '坚持退出' (基于content-desc) 操作...");

        List<AccessibilityNodeInfo> exitNodes = null;

        try {
            // 再次使用 content-desc 查找“坚持退出”节点以确保节点仍然有效
            exitNodes = AccessibilityNodeUtils.findNodesByContentDescriptionContains(rootNode, EXIT_CONTENT_DESC);

            if (!exitNodes.isEmpty()) {
                AccessibilityNodeInfo exitNode = exitNodes.get(0);
                boolean success = AccessibilityActionUtils.performClick(service, exitNode);
                if (success) {
                    Log.i(TAG, "Middle1InAdProcessor: 成功点击 '坚持退出'。");
                } else {
                    Log.w(TAG, "Middle1InAdProcessor: 点击 '坚持退出' 失败。");
                }
                return success;
            } else {
                Log.w(TAG, "Middle1InAdProcessor: 再次查找时未能找到 content-desc 为 '坚持退出' 的节点。");
                return false;
            }
        } finally {
            // 回收节点列表
            AccessibilityNodeUtils.recycleNodes(exitNodes);
        }
    }
}
