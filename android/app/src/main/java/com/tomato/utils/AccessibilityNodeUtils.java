package com.tomato.utils;

import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityNodeUtils {

    private static final String TAG = AccessibilityConfig.TAG + ".NodeUtils";

    /**
     * 查找所有符合条件的节点
     * @param rootNode 搜索的起始节点。
     * @return 返回所有符合条件的节点列表 (节点为副本)，如果未找到则返回空列表。
     */
    public static List<AccessibilityNodeInfo> findNodesByResourceID(AccessibilityNodeInfo rootNode, String resourceId) {
        Log.d(TAG, "findNodesByResourceID: 正在根据resourceId查找节点: " + resourceId);
        //  findAccessibilityNodeInfosByViewId 会从 rootNode 节点开始，递归地遍历该节点下的整个视图子树（包括它自己、它的所有子节点、孙子节点，以此类推），并找出所有 resource-id 与您提供的ID字符串相匹配的节点，然后将它们全部收集到一个 List 列表中返回。
        return rootNode.findAccessibilityNodeInfosByViewId(resourceId);
    }

    /**
     * 【推荐】通过文本内容查找节点（包含匹配）。
     * 这是对系统API的直接封装，效率和安全性最高。
     *
     * @param rootNode 起始节点
     * @param text     要查找的文本
     * @return 包含该文本的节点列表
     */
    public static List<AccessibilityNodeInfo> findNodesByTextContains(AccessibilityNodeInfo rootNode, String text) {
        if (rootNode == null || text == null) {
            return new ArrayList<>(); // 返回空列表，避免 NullPointerException
        }
        return rootNode.findAccessibilityNodeInfosByText(text);
    }

    /**
     * 【推荐】通过文本内容查找节点（精确匹配）。
     * 基于系统API进行过滤，兼具安全性和准确性。
     *
     * @param rootNode  起始节点
     * @param text      要查找的文本
     * @param exactMatch 是否要求完全一样
     * @return 符合条件的节点列表
     */
    public static List<AccessibilityNodeInfo> findNodesByText(AccessibilityNodeInfo rootNode, String text, boolean exactMatch) {
        if (rootNode == null || text == null) {
            return new ArrayList<>();
        }

        // 如果不是精确匹配，直接调用上面的 contains 方法
        if (!exactMatch) {
            return findNodesByTextContains(rootNode, text);
        }

        // 如果是精确匹配，先用包含匹配找出候选节点，再进行过滤
        List<AccessibilityNodeInfo> candidates = rootNode.findAccessibilityNodeInfosByText(text);
        List<AccessibilityNodeInfo> exactMatches = new ArrayList<>();

        for (AccessibilityNodeInfo node : candidates) {
            boolean isMatch = false;
            // 目前的观察text和contentDescription储存的内容都是一样的
            // text：在屏幕上直接显示给用户看的文字。
            // contentDescription：为没有可见文字的控件提供一个无障碍描述，或者覆盖掉已有的 text 内容。
            // 检查节点的 text 属性
            CharSequence nodeText = node.getText();
            if (nodeText != null && text.equals(nodeText.toString())) {
                isMatch = true;
            }

            // 只有当 getText() 不匹配时，才检查 getContentDescription()
            if (!isMatch) {
                CharSequence nodeDesc = node.getContentDescription();
                if (nodeDesc != null && text.equals(nodeDesc.toString())) {
                    isMatch = true;
                }
            }

            // 决定节点的“命运”
            if (isMatch) {
                // 是精确匹配！
                // 将它加入返回列表，把“所有权”转交给上级调用者。
                // **绝对不能在这里回收它！**
                exactMatches.add(node);
            } else {
                // 不是精确匹配。
                // 我们是它最后的所有者，并且不再需要它了。
                // **必须立即回收它，防止内存泄漏。**
                node.recycle();
            }
        }

        // 循环结束后，所有 candidates 里的节点，要么进入了 exactMatches 列表，要么被回收了。
        // candidates 列表本身会被垃圾回收，我们已经处理完了所有它包含的节点。

        return exactMatches;
    }

    /**
     * 辅助方法，用于统一回收 AccessibilityNodeInfo 节点列表。
     * @param nodes 要回收的节点列表。
     */
    public static void recycleNodes(List<AccessibilityNodeInfo> nodes) {
        if (nodes != null) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null) {
                    node.recycle();
                }
            }
        }
    }
}
