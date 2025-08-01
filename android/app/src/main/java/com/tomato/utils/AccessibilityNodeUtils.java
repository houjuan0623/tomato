package com.tomato.utils;

import android.view.accessibility.AccessibilityNodeInfo;

public class AccessibilityNodeUtils {
    // 根据condition查找node
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
}
