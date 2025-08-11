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
     * 【已修改】通过内容描述（content-desc）查找节点（包含匹配）。
     * 由于系统没有只查找 content-desc 的原生 API，此方法通过递归手动实现。
     *
     * @param rootNode    起始节点
     * @param contentDesc 要查找的内容描述文本
     * @return 包含该内容描述的节点列表
     */
    public static List<AccessibilityNodeInfo> findNodesByContentDescriptionContains(AccessibilityNodeInfo rootNode, String contentDesc) {
        List<AccessibilityNodeInfo> foundNodes = new ArrayList<>();
        if (rootNode == null || contentDesc == null) {
            return foundNodes; // 返回空列表，避免 NullPointerException
        }
        // 调用递归辅助函数开始查找
        findNodesByContentDescriptionRecursive(rootNode, contentDesc, foundNodes);
        return foundNodes;
    }

    /**
     * 用于递归查找的私有辅助方法。
     *
     * @param currentNode 当前正在检查的节点
     * @param contentDesc 要查找的内容描述
     * @param foundNodes  用于存放结果的列表
     */
    private static void findNodesByContentDescriptionRecursive(AccessibilityNodeInfo currentNode, String contentDesc, List<AccessibilityNodeInfo> foundNodes) {
        if (currentNode == null) {
            return;
        }

        // 1. 检查当前节点
        CharSequence currentContentDesc = currentNode.getContentDescription();
        if (currentContentDesc != null && currentContentDesc.toString().contains(contentDesc)) {
            // 找到了！添加一个节点的副本到列表中
            foundNodes.add(AccessibilityNodeInfo.obtain(currentNode));
        }

        // 2. 递归遍历所有子节点
        for (int i = 0; i < currentNode.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = currentNode.getChild(i);
            if (childNode != null) {
                findNodesByContentDescriptionRecursive(childNode, contentDesc, foundNodes);
                // 注意：这里不需要 recycle() childNode，因为它的所有权仍在父节点中
            }
        }
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
