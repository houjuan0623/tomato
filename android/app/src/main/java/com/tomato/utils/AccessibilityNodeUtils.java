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
     * 使用广度优先搜索查找所有满足指定条件的节点。
     * NodeCondition 的实现者负责检查节点的具体属性，包括可见性和可用性（如果需要）。
     *
     * @param rootNode  搜索的起始节点。
     * @param condition 自定义的节点匹配条件。
     * @return 返回所有符合条件的节点列表 (节点为副本)，如果未找到则返回空列表。
     *         调用者有责任回收列表中的所有节点。
     */
    public static List<AccessibilityNodeInfo> findAllNodes(AccessibilityNodeInfo rootNode, NodeCondition condition) {
        List<AccessibilityNodeInfo> foundNodes = new ArrayList<>();
        if (rootNode == null) {
            return foundNodes;
        }

        List<AccessibilityNodeInfo> toVisit = new ArrayList<>();

        AccessibilityNodeInfo tempRoot = AccessibilityNodeInfo.obtain(rootNode);
        if (tempRoot == null) return foundNodes;

        List<AccessibilityNodeInfo> queue = new ArrayList<>();
        queue.add(tempRoot);

        while(!queue.isEmpty()) {
            AccessibilityNodeInfo currentNode = queue.remove(0);

            if(currentNode == null) continue;
            if (condition.test(currentNode)) {
                Log.d(TAG, "findAllNodes: Node found matching condition. ID: " + currentNode.getViewIdResourceName());
                foundNodes.add(AccessibilityNodeInfo.obtain(currentNode)); // Add a new copy to results
            }

            // 将所有的子节点的副本加入队列以供后续处理

            // 将所有子节点的副本加入队列以供后续处理
            for (int i = 0; i < currentNode.getChildCount(); i++) {
                AccessibilityNodeInfo child = currentNode.getChild(i);
                if (child != null) {
                    queue.add(AccessibilityNodeInfo.obtain(child)); // 子节点副本入队
                }
            }
            currentNode.recycle(); // 回收从队列中取出的、已处理完毕的 currentNode 副本
        }
        return foundNodes;
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
