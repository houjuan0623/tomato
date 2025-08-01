package com.tomato.utils;

import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

import com.tomato.nativeaccessibility.AccessibilityConfig;

import java.util.ArrayList;
import java.util.List;

public class AccessibilityNodeUtils {

    private static final String TAG = AccessibilityConfig.TAG + ".NodeUtils";

    /**
     * 递归查找满足指定条件的第一个节点。
     * NodeCondition 的实现者负责检查节点的具体属性，包括可见性和可用性（如果需要）。
     *
     * @param rootNode  搜索的起始节点。
     * @param condition 自定义的节点匹配条件。
     * @return 如果找到符合条件的节点则返回该节点的一个新引用 (obtain)，否则返回 null。
     *         调用者有责任回收返回的节点。
     */
    public static AccessibilityNodeInfo findNode(AccessibilityNodeInfo rootNode, NodeCondition condition){
        if (rootNode == null) {
            return null;
        }

        if (rootNode != null && condition.test(rootNode)){
            return rootNode; // 返回原始的 rootNode 引用
        }

        for (int i = 0; i < rootNode.getChildCount(); i++) {
            AccessibilityNodeInfo child = rootNode.getChild(i);
            if (child != null) {
                // 将 child 传递给 findNode。如果它被 findNode 返回，则由调用者回收。
                // 如果它没有被 findNode 返回，它将在 findNode 内部被回收，或者在这里被回收。
                AccessibilityNodeInfo foundNode = findNode(child, condition); // 递归调用

                if (foundNode != null) {
                    // 如果从递归调用中找到了节点，那么 'child' 节点（作为当前循环的父节点）
                    // 就不应该在这里被回收，因为它可能是 'foundNode' 本身，或者是它的祖先。
                    // 'child' 的回收责任现在转移给了上一层调用，或者如果'child'就是'foundNode'，
                    // 则转移给了最初调用findNode的调用者。
                    // 只有当 foundNode 为 null 时，我们才明确回收这个 child。
                    return foundNode;
                }
                // 如果 foundNode 为 null，意味着在 child 的子树中没有找到，
                // 并且 child 本身也不满足条件（因为它在上一层调用中已经被检查过了）。
                // 所以，在这里回收 child 是安全的。
                child.recycle();
            }
        }
        return null; // 未找到
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
}
