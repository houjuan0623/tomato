package com.tomato.processor;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tomato.nativeaccessibility.AccessibilityEventService;
import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;

import java.util.List;

public class ProductProcessor implements ScreenProcessor {

    private static final String TAG = AccessibilityConfig.TAG;
    

    @Override
    public boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        if (rootNode == null) {
            return false;
        }
        List<AccessibilityNodeInfo> featureNodes = null;
        List<AccessibilityNodeInfo> clickNodes = null;
        try {
            featureNodes = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.PRODUCT_FEATURE_ID);
            clickNodes = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.PRODUCT_CLICK_ID);
            // 当两个特征节点都存在时，才认为可以处理
            return !featureNodes.isEmpty() && !clickNodes.isEmpty();
        } finally {
            AccessibilityNodeUtils.recycleNodes(featureNodes);
            AccessibilityNodeUtils.recycleNodes(clickNodes);
        }
    }

    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        Log.i(TAG, "检测到产品推荐弹窗，准备点击关闭按钮 (ID: " + AccessibilityConfig.PRODUCT_CLICK_ID + ")");
        List<AccessibilityNodeInfo> clickNodes = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.PRODUCT_CLICK_ID);

        if (clickNodes.isEmpty()) {
            Log.w(TAG, "ProductProcessor: 在 process 中未能找到目标点击按钮，可能界面已变化。");
            return false;
        }

        // 点击找到的第一个按钮
        AccessibilityNodeInfo clickButton = clickNodes.get(0);
        boolean clicked = AccessibilityActionUtils.performClick(service, clickButton);

        // 回收所有找到的节点
        AccessibilityNodeUtils.recycleNodes(clickNodes);

        return clicked;
    }
}
