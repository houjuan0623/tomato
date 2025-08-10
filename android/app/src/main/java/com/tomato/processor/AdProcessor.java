package com.tomato.processor;

import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tomato.nativeaccessibility.AccessibilityEventService;
import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;

import java.util.List;

/**
 * 首先判断是否是广告界面
 * 如果是广告界面
 * 启用定时器循环检查界面是否出现领取成功的标志，如果出现则点击领取成功按钮
 */
public class AdProcessor implements ScreenProcessor {

    private static final String TAG = AccessibilityConfig.TAG; // 使用统一的TAG方便日志查看

    // --- 广告页面的关键文本 ---
    private static final String AD_MARKER_TEXT = "广告";
    private static final String AD_SUCCESS_TEXT = "领取成功";

    private static volatile boolean isAdTaskRunning = false;

    // 为了防止无限循环，设置一个最大检查次数。例如，每10秒查一次，总共查6次（1分钟）
    private static final int MAX_CHECK_COUNT = 8;
    // 定时器检查间隔，单位：毫秒
    private static final long CHECK_INTERVAL_MS = 10000; // 10秒

    // 用于执行定时任务
    private Handler adHandler;
    private Runnable adCheckRunnable;
    private int checkCounter = 0;

    /**
     * 判断是否进入了广告页面。
     * 逻辑：根据用户提供的信息，只要页面中存在 text="广告" 的节点，就认为可以处理。
     */
    @Override
    public boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        if (rootNode == null) {
            return false;
        }

        List<AccessibilityNodeInfo> adMarkers = null;
        try {
            // 查找是否存在 "广告" 标志节点
            adMarkers = AccessibilityNodeUtils.findNodesByText(rootNode, AD_MARKER_TEXT, true); // 使用完全匹配
            return !adMarkers.isEmpty();
        } finally {
            AccessibilityNodeUtils.recycleNodes(adMarkers);
        }
    }

    /**
     * 处理广告的完整流程。
     * 新逻辑：
     * 1. (canProcess触发) 确认进入广告页。
     * 2. (本方法启动) 启动定时器，周期性检查。
     * 3. 检查页面是否存在含 "领取成功" 的节点。
     * 4. 如果存在，则点击该节点关闭广告，任务结束。
     * 5. 如果不存在，但存在含 "秒后可领奖励" 的节点，则继续等待。
     *
     * @param service  AccessibilityEventService 的实例。
     * @param rootNode 窗口根节点 (注意：此节点可能很快失效，定时任务中需要重新获取)。
     * @return boolean 返回 true 表示已成功启动广告处理流程。
     */
    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        if (isAdTaskRunning) {
            Log.d(TAG, "AdProcessor: 任务已在运行中，跳过本次 process 调用。");
            return false;
        }

        Log.i(TAG, "AdProcessor: 启动广告处理流程 (新逻辑)...");
        isAdTaskRunning = true;
        checkCounter = 0;

        adHandler = service.getHandler();
        adCheckRunnable = new Runnable() {
            @Override
            public void run() {
                // 每次检测时，都获取最新的窗口信息
                AccessibilityNodeInfo currentRootNode = service.getRootInActiveWindow();
                if (currentRootNode == null) {
                    Log.w(TAG, "AdProcessor: 无法获取当前窗口的根节点。");
                    resetTaskState();
                    return;
                }

                // 检查是否超时
                if (checkCounter++ >= MAX_CHECK_COUNT) {
                    Log.e(TAG, "AdProcessor: 检测超时，未能关闭广告。");
                    resetTaskState();
                    currentRootNode.recycle();
                    return;
                }

                Log.d(TAG, "AdProcessor: 正在进行第 " + checkCounter + " 次检测...");

                // 核心处理逻辑
                processAdState(service, currentRootNode);

                currentRootNode.recycle();
            }
        };
        // 立即开始第一次检测
        Log.i(TAG, "AdProcessor: 定时器已初始化，立即开始首次检测。");
        adHandler.post(adCheckRunnable);

        return true;
    }

    /**
     * 根据当前广告状态进行处理
     * @param service AccessibilityEventService 实例
     * @param rootNode 当前窗口的根节点
     */
    private void processAdState(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> successNodes = null;

        try {
            // 步骤 1: 查找 "领取成功" 的锚点节点
            successNodes = AccessibilityNodeUtils.findNodesByTextContains(rootNode, AD_SUCCESS_TEXT);
            if (successNodes != null && successNodes.size() > 1) {
                AccessibilityNodeInfo triggerNode = successNodes.get(0); // 获取触发状态的节点
                Log.i(TAG, "AdProcessor: 检测到 '" + AD_SUCCESS_TEXT + "'! 准备基于索引查找关闭按钮。");
                Log.i(TAG, "successNodes.get(0) 是：" + successNodes.get(0));
                Log.i(TAG, "1111111111111111111111111111111111111111111successNodes.get(0).getClassName() 是：" + successNodes.get(0).getClassName());

                Log.i(TAG, "successNodes.get(1) 是：" + successNodes.get(1));
                Log.i(TAG, "1111111111111111111111111111111111111111111122222222222222222222222222222successNodes.get(1).getClassName() 是：" + successNodes.get(1).getClassName());


                // 步骤 2: 基于锚点节点，查找下一个符合条件的兄弟节点
                AccessibilityNodeInfo targetButton = findNextSiblingImageByClass(triggerNode, AccessibilityConfig.TARGET_IMAGE_CLASS);
                if (targetButton != null) {
                    Log.i(TAG, "AdProcessor: 找到目标关闭按钮 (基于索引)，准备点击。");
                    if (AccessibilityActionUtils.performClick(service, targetButton)) {
                        Log.i(TAG, "AdProcessor: 成功点击关闭按钮，广告流程结束。");
                        resetTaskState();
                    } else {
                        Log.w(TAG, "AdProcessor: 点击关闭按钮失败，将在下个周期重试。");
                    }
                    // 回收找到的目标按钮
                    targetButton.recycle();
                } else {
                    Log.w(TAG, "AdProcessor: 未能找到符合条件的下一个 " + AccessibilityConfig.TARGET_IMAGE_CLASS + " 兄弟节点。");
                }
            }
        } finally {
            AccessibilityNodeUtils.recycleNodes(successNodes);
        }

        // 只要任务没被重置，就安排下一次检测
        if (isAdTaskRunning) {
            adHandler.postDelayed(adCheckRunnable, CHECK_INTERVAL_MS);
        }
    }

    /**
     * 【新】查找锚点节点的下一个指定类名的兄弟节点。
     * @param anchorNode 锚点节点 (例如，包含“领取成功”文本的节点)
     * @param className  目标类名
     * @return 找到的第一个符合条件的兄弟节点，如果没有则返回 null
     */
    private AccessibilityNodeInfo findNextSiblingImageByClass(AccessibilityNodeInfo anchorNode, String className) {
        if (anchorNode == null || className == null) {
            return null;
        }

        AccessibilityNodeInfo parent = anchorNode.getParent();

        if (parent == null) {
            return null;
        }

        try {
            int anchorIndex = -1;
            Rect anchorRect = new Rect();
            Rect childRect = new Rect(); // 优化：在循环外创建，避免重复分配内存
            anchorNode.getBoundsInScreen(anchorRect);

            // 步骤 1: 找到锚点节点在父节点中的索引
            for (int i = 0; i < parent.getChildCount(); i++) {
                AccessibilityNodeInfo child = parent.getChild(i);
                if (child != null) {
                    child.getBoundsInScreen(childRect);
//                    if (anchorRect.equals(childRect)) {
//                        Log.d(TAG, "=====================================找到锚点节点在父节点中的索引: " + i);
//                        Log.d(TAG, String.valueOf(anchorNode.getClassName()));
//                        Log.d(TAG, String.valueOf(child.getClassName()));
//                    } else {
//                        Log.d(TAG, "。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。未能找到锚点节点在父节点中的索引: " + i);
//                    }
                     if (anchorRect.equals(childRect) &&
                             String.valueOf(anchorNode.getClassName()).equals(String.valueOf(child.getClassName()))) {
                         Log.d(TAG, "=====================================找到锚点节点在父节点中的索引: " + i);
                         Log.d(TAG, "找到锚点节点在父节点中的索引: " + i);
                         anchorIndex = i;
                         child.recycle(); // 回收用于比较的节点
                         break; // 找到索引，跳出循环
                     }
                     Log.d(TAG, "。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。未能找到锚点节点在父节点中的索引: " + i);
                    child.recycle(); // 如果不是，也要回收
                }
            }

            if (anchorIndex == -1) {
                Log.w(TAG, "无法在父节点中定位到锚点节点。锚点: " + anchorNode);
                return null;
            }

            // 步骤 2: 从锚点索引之后开始，查找第一个符合条件的兄弟节点
            for (int i = anchorIndex + 1; i < parent.getChildCount(); i++) {
                AccessibilityNodeInfo potentialTarget = parent.getChild(i);
                if (potentialTarget != null) {
                    if (className.equals(potentialTarget.getClassName().toString())) {
                        // 找到了！所有权将转移给调用者。
                        return potentialTarget;
                    }
                    // 如果不是目标，回收它并继续查找
                    potentialTarget.recycle();
                }
            }
        } finally {
            // 操作完成后，回收父节点
            parent.recycle();
        }

        // 未找到符合条件的兄弟节点
        return null;
    }

    /**
     * 重置任务状态，并移除所有待处理的回调
     */
    private void resetTaskState() {
        if (adHandler != null && adCheckRunnable != null) {
            adHandler.removeCallbacks(adCheckRunnable);
        }
        isAdTaskRunning = false;
        checkCounter = 0;
        adHandler = null;
        adCheckRunnable = null;
        Log.i(TAG, "AdProcessor: 任务状态已重置。");
    }

    /**
     * [新增] 从外部重置任务标志。
     * 当服务状态重置时，需要调用此方法，以允许处理器可以处理下一个广告。
     */
    public static void resetTaskFlag() {
        isAdTaskRunning = false;
    }
}
