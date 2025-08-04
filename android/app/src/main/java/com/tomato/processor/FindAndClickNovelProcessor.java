package com.tomato.processor;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tomato.nativeaccessibility.AccessibilityEventService;
import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;
import com.tomato.utils.State;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FindAndClickNovelProcessor implements ScreenProcessor {

    private static final int MAX_SCROLL_ATTEMPTS = 10; // To prevent infinite loops

    @Override
    public boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        if (rootNode == null) {
            return false;
        }
        // 1. Check if this action is already completed
        if (service.getStateManager().isActionCompleted(AccessibilityConfig.ACTION_ID_FIND_AND_CLICK_NOVEL)) {
            return false;
        }
        // 2. Check if there is a novel name to search for
        String novelNameToSearch = State.getInstance().getNovelNameToSearch();
        if (novelNameToSearch == null || novelNameToSearch.isEmpty()) {
            return false;
        }
        // 3. Check for a characteristic of the search results page, e.g., the scrollable container
        List<AccessibilityNodeInfo> scrollableNodes = null;
        try {
            scrollableNodes = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.SCROLLABLE_CONTAINER_ID);
            return !scrollableNodes.isEmpty();
        } finally {
            if (scrollableNodes != null) {
                for (AccessibilityNodeInfo node : scrollableNodes) {
                    if (node != null) {
                        node.recycle();
                    }
                }
            }
        }
    }

    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        Log.i(AccessibilityConfig.TAG, "识别到搜索结果页，开始查找并点击目标小说...");
        String novelNameToSearch = State.getInstance().getNovelNameToSearch();
        if (novelNameToSearch == null || novelNameToSearch.isEmpty()) {
            Log.w(AccessibilityConfig.TAG, "要查找的小说名为空，无法执行操作。");
            return false;
        }

        // Use a set to keep track of visited nodes' text to detect end of scroll
        Set<String> seenNodeTexts = new HashSet<>();
        boolean contentChangedAfterScroll = true;

        for (int i = 0; i < MAX_SCROLL_ATTEMPTS && contentChangedAfterScroll; i++) {
            List<AccessibilityNodeInfo> resultItems = null;
            try {
                resultItems = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.TARGET_FOR_SEARCH_RESULT_ITEM);
                int newNodesCount = 0;

                for (AccessibilityNodeInfo itemNode : resultItems) {
                    CharSequence text = itemNode.getText();
                    if (text != null) {
                        String textStr = text.toString();
                        if (seenNodeTexts.add(textStr)) {
                            newNodesCount++;
                        }

                        if (novelNameToSearch.equals(textStr)) {
                            Log.i(AccessibilityConfig.TAG, "找到目标小说: " + novelNameToSearch);
                            // The item itself might not be clickable, but its parent is.
                            // We need to find the clickable parent.
                            AccessibilityNodeInfo clickableParent = findClickableParent(itemNode);
                            if (clickableParent != null) {
                                boolean clicked = AccessibilityActionUtils.performClick(service, clickableParent);
                                if (clicked) {
                                    Log.i(AccessibilityConfig.TAG, "成功点击小说: " + novelNameToSearch);
                                    service.getStateManager().markActionAsCompleted(AccessibilityConfig.ACTION_ID_FIND_AND_CLICK_NOVEL);
                                    clickableParent.recycle(); // Recycle the found clickable parent
                                    return true;
                                } else {
                                    Log.w(AccessibilityConfig.TAG, "点击小说失败: " + novelNameToSearch);
                                    clickableParent.recycle();
                                    return false;
                                }
                            } else {
                                Log.w(AccessibilityConfig.TAG, "找到了小说名，但其父节点不可点击。");
                                return false; // Stop trying if we found it but can't click
                            }
                        }
                    }
                }
                contentChangedAfterScroll = newNodesCount > 0;

            } finally {
                if (resultItems != null) {
                    for (AccessibilityNodeInfo node : resultItems) {
                        if (node != null) {
                            node.recycle();
                        }
                    }
                }
            }

            // If not found, try to scroll
            if (!scrollDown(service, rootNode)) {
                Log.w(AccessibilityConfig.TAG, "无法继续向下滑动，停止查找。");
                break; // Stop if we can't scroll anymore
            }

            // Wait a bit for the UI to settle after scroll
            try {
                Thread.sleep(1000); // 1-second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(AccessibilityConfig.TAG, "Scroll delay interrupted", e);
                return false;
            }
            // Refresh rootNode after scroll
            rootNode = service.getRootInActiveWindow();
            if (rootNode == null) {
                Log.w(AccessibilityConfig.TAG, "滚动后无法获取 rootNode。");
                return false;
            }
        }

        Log.w(AccessibilityConfig.TAG, "滑动了 " + MAX_SCROLL_ATTEMPTS + " 次后仍未找到小说: " + novelNameToSearch);
        return false;
    }

    /**
     * 向下滚动列表以显示更多项目。
     * 这个效果是通过一次“向上滑动”手势（手指从屏幕底部移动到顶部）来实现的。
     * 该方法会优先尝试标准的无障碍滚动操作，如果失败，则回退到通用的手势模拟。
     * @param service AccessibilityService 实例。
     * @param rootNode 当前窗口的根节点。
     * @return 如果滚动操作成功派发，则返回 true。
     */
    private boolean scrollDown(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> scrollableNodes = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.SCROLLABLE_CONTAINER_ID);
        try {
            if (!scrollableNodes.isEmpty()) {
                AccessibilityNodeInfo scrollableNode = scrollableNodes.get(0);
                if (scrollableNode.isScrollable()) {
                    Log.i(AccessibilityConfig.TAG, "找到可滚动节点，尝试 ACTION_SCROLL_FORWARD...");
                    if (scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                        Log.i(AccessibilityConfig.TAG, "ACTION_SCROLL_FORWARD 成功。");
                        return true;
                    }
                    Log.w(AccessibilityConfig.TAG, "ACTION_SCROLL_FORWARD 失败，将回退到手势滑动。");
                }
            } else {
                Log.w(AccessibilityConfig.TAG, "未找到可滚动的容器 (ID: " + AccessibilityConfig.SCROLLABLE_CONTAINER_ID + ")，将直接尝试通用手势滑动。");
            }

            // 回退方案：通用手势滑动
            Log.i(AccessibilityConfig.TAG, "执行备用方案：通用手势向上滑动。");
            return AccessibilityActionUtils.performGenericSwipeUp(service);
        } finally {
            AccessibilityNodeUtils.recycleNodes(scrollableNodes);
        }
    }

    private AccessibilityNodeInfo findClickableParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo parent = node;
        while (parent != null) {
            if (parent.isClickable()) {
                return parent;
            }
            AccessibilityNodeInfo temp = parent;
            parent = parent.getParent();
            if (temp != node) {
                temp.recycle();
            }
        }
        return null;
    }
}
