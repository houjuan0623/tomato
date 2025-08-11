package com.tomato.processor;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tomato.nativeaccessibility.AccessibilityEventService;
import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;
import com.tomato.utils.State;

import java.util.List;
import java.util.Random;

/**
 * 处理器，用于处理小说阅读页面，主要负责自动向左滑动翻页。
 */
public class ReadingPageProcessor implements ScreenProcessor {
    // 使用 volatile 保证多线程间的可见性
    private static volatile boolean isLoopRunning = false;

    // 用于生成随机延迟
    private final Random random = new Random();
    // 随机延迟的选项 (毫秒)
    private final int[] swipeDelays = { 4000, 5000, 6000, 7000 };

    @Override
    public boolean canProcess(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        if (isLoopRunning) {
            return false;
        }
        // 检查全局状态是否开启了自动阅读，并且当前确实在阅读页
        return State.getInstance().isAutoReading() && isReadingPage(rootNode);
    }

    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        // canProcess 已经确认可以启动，这里直接启动循环
        Log.i(AccessibilityConfig.TAG, "识别到阅读页面和自动阅读指令，启动翻页循环...");

        isLoopRunning = true;

        // --- 新增的常量，用于重试逻辑 ---
        final int MAX_RETRIES = 3; // 最大连续重试次数
        final long RETRY_INTERVAL_MS = 2000; // 每次重试的间隔时间（1.5秒）

        // 定义一个健壮的翻页任务
        Runnable swipeRunnable = new Runnable() {
            private int failureCount = 0; // 失败计数器

            @Override
            public void run() {
                // 1. 检查全局开关，这是最优先的停止条件
                if (!State.getInstance().isAutoReading()) {
                    Log.i(AccessibilityConfig.TAG, "自动阅读状态已关闭，永久停止翻页循环。");
                    isLoopRunning = false;
                    return; // 彻底退出循环
                }

                // 2. 检查当前是否仍在阅读页
                AccessibilityNodeInfo currentRoot = service.getRootInActiveWindow();
                if (currentRoot != null && isReadingPage(currentRoot)) {
                    // 下面是成功识别到阅读页的逻辑

                    // 成功了，必须重置失败计数器！
                    if (failureCount > 0) {
                        Log.i(AccessibilityConfig.TAG, "重试成功，已返回阅读页面。");
                    }

                    failureCount = 0; // 重置失败计数
                    Log.d(AccessibilityConfig.TAG, "翻页循环: 在阅读页，执行一次向左滑动。");
                    AccessibilityActionUtils.performGenericSwipeLeft(service);
                    currentRoot.recycle();

                    // 3. 成功滑动后，计划下一次检查
                    long randomDelay = swipeDelays[random.nextInt(swipeDelays.length)];
                    Log.d(AccessibilityConfig.TAG,
                            "计划在 " + randomDelay + "ms 后进行下一次翻页。");
                    service.getHandler().postDelayed(this, randomDelay);
                } else {
                    // 不在阅读页（可能临时切换、弹窗等）
                    failureCount++;
                    Log.w(AccessibilityConfig.TAG, "翻页循环: 未在阅读页，尝试次数: " + failureCount);
                    if (currentRoot != null) {
                        currentRoot.recycle();
                    }
                    if (failureCount < MAX_RETRIES) { // 最多重试3次
                        // 等待一个很短的时间（比如2秒）再试几次，给弹窗消失的时间
                        service.getHandler().postDelayed(this, RETRY_INTERVAL_MS);
                    } else {
                        // 已达到最大重试次数，确认已离开阅读页，彻底停止循环
                        Log.e(AccessibilityConfig.TAG, "已连续重试 " + MAX_RETRIES + " 次仍未返回阅读页，停止翻页循环。");
                        isLoopRunning = false;
                    }
                }
            }
        };

        // 立即启动循环的第一次执行
        service.getHandler().post(swipeRunnable);

        return true; // 返回 true 表示“启动循环”这个动作已成功处理
    }

    /**
     * 辅助方法，检查当前是否在阅读页面。
     * 
     * @param rootNode 根节点
     * @return 如果是阅读页面则返回 true
     */
    private boolean isReadingPage(AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> featureNodes1 = null;
        List<AccessibilityNodeInfo> featureNodes2 = null;
        try {
            featureNodes1 = AccessibilityNodeUtils.findNodesByResourceID(rootNode,
                    AccessibilityConfig.TARGET_FOR_READING_PAGE_FEATURE_1);
            if (!featureNodes1.isEmpty()) {
                return true; // 找到特征1，确认是阅读页
            }
            featureNodes2 = AccessibilityNodeUtils.findNodesByResourceID(rootNode,
                    AccessibilityConfig.TARGET_FOR_READING_PAGE_FEATURE_2);
            return !featureNodes2.isEmpty(); // 找到特征2，确认是阅读页
        } finally {
            AccessibilityNodeUtils.recycleNodes(featureNodes1);
            AccessibilityNodeUtils.recycleNodes(featureNodes2);
        }
    }

    /**
     * [新增] 从外部重置循环标志。
     * 当服务状态重置并取消所有 Handler 任务时，需要调用此方法，以允许循环在下次检查时可以重启。
     */
    public static void resetLoopFlag() {
        isLoopRunning = false;
    }
}