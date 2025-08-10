package com.tomato.processor;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tomato.nativeaccessibility.AccessibilityEventService;
import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;

import java.util.List;

public class RankProcessor implements ScreenProcessor {

    private static final String TAG = AccessibilityConfig.TAG;

    @Override
    public boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        if (rootNode == null) {
            return false;
        }
        List<AccessibilityNodeInfo> rankButtons = null;
        try {
            rankButtons = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.RANK_BUTTON_ID);
            return !rankButtons.isEmpty();
        } finally {
            AccessibilityNodeUtils.recycleNodes(rankButtons);
        }
    }

    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        Log.i(TAG, "检测到排行榜按钮 (ID: " + AccessibilityConfig.RANK_BUTTON_ID + ")，准备点击...");
        List<AccessibilityNodeInfo> rankButtons = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.RANK_BUTTON_ID);

        if (rankButtons.isEmpty()) {
            Log.w(TAG, "RankProcessor: 在 process 中未能找到排行榜按钮，可能界面已变化。");
            return false;
        }

        // 点击找到的第一个按钮
        AccessibilityNodeInfo rankButton = rankButtons.get(0);
        boolean clicked = AccessibilityActionUtils.performClick(service, rankButton);

        // 回收所有找到的节点
        AccessibilityNodeUtils.recycleNodes(rankButtons);

        return clicked;
    }
}
