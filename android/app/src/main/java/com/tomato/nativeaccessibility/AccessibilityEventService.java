package com.tomato.nativeaccessibility;

import com.tomato.utils.AccessibilityConfig;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.tomato.processor.MainPageProcessor;
import com.tomato.processor.AddToHomePageProcessor;
import com.tomato.processor.SearchNovelProcessor;
import com.tomato.processor.InputNovelNameProcessor;
import com.tomato.processor.FindAndClickNovelProcessor;
import com.tomato.processor.ReadingPageProcessor;

import com.tomato.utils.ScreenProcessor;

import com.tomato.utils.ActionStateManager;
import com.tomato.utils.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 核心的无障碍服务类。
 * 当用户授权后，此服务会在后台运行，监听指定应用的界面变化。
 *
 * [已修改]
 */
public class AccessibilityEventService extends AccessibilityService {

    // 使用 Handler 来处理延迟操作，避免阻塞主线程
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // 创建一个处理器列表
    private final List<ScreenProcessor> screenProcessors = new ArrayList<>();

    // 用于生成随机延迟
    private final Random random = new Random();

    // 随机延迟的选项 (毫秒)
    private final int[] processingDelays = {3000, 5000};


    /**
     * 当服务成功连接时被调用。
     */
    @Override
    protected void onServiceConnected() {

        Log.i(AccessibilityConfig.TAG, "无障碍服务已连接。");
        // 在这里初始化并注册所有的处理器
        initializeProcessors();
    }

    private void initializeProcessors() {
        // 优先处理可能出现的广告和弹窗
        screenProcessors.add(new AdProcessor());
        screenProcessors.add(new MainPageProcessor());
        screenProcessors.add(new AddToHomePageProcessor());
        screenProcessors.add(new InputNovelNameProcessor());
        screenProcessors.add(new SearchNovelProcessor());
        screenProcessors.add(new FindAndClickNovelProcessor());
        screenProcessors.add(new ReadingPageProcessor());
        // ... 如果有更多界面，继续添加 ...
    }

    /**
     * 当系统检测到符合我们配置的无障碍事件时，此方法会被调用。
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(AccessibilityConfig.TAG, "----------开始一轮事件监听----------");
        if (event == null || event.getPackageName() == null) {
            return;
        }

        // 如果事件不是来自目标应用
        if (!AccessibilityConfig.TARGET_PACKAGE_NAME_1.equals(event.getPackageName().toString()) &&
                !AccessibilityConfig.TARGET_PACKAGE_NAME_2.equals(event.getPackageName().toString())) {
            return;
        }

        int eventType = event.getEventType();

        // 主要监听窗口变化事件，这是进入新界面的最可靠信号
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            Log.d(AccessibilityConfig.TAG, "接收到事件: " + AccessibilityEvent.eventTypeToString(eventType) +
                    " 来自包: " + event.getPackageName() +
                    " 类名: " + event.getClassName());

            // 当窗口状态改变时，通常表示进入新屏幕，此时重置点击状态
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                Log.i(AccessibilityConfig.TAG, "窗口状态改变，重置点击标记。");
                resetServiceState(); // 重置点击标记和取消挂起的重试
            }

            // --- 新增：引入随机延迟 ---
            long randomDelay = getRandomProcessingDelay();

            Log.d(AccessibilityConfig.TAG, "计划在 " + randomDelay + "ms 后尝试处理屏幕内容 (事件: " + AccessibilityEvent.eventTypeToString(eventType) + ")");

            String packageName = event.getPackageName().toString();

            mHandler.postDelayed(() -> {
                if (!AccessibilityConfig.TARGET_PACKAGE_NAME_1.equals(packageName) &&
                        !AccessibilityConfig.TARGET_PACKAGE_NAME_2.equals(packageName)) {
                    Log.d(AccessibilityConfig.TAG, "延迟任务执行时发现已离开目标应用，取消处理。");
                    resetServiceState(); // 确保状态也重置
                    return;
                }

                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
                if (rootNode != null) {
                    Log.d(AccessibilityConfig.TAG, "获取 rootNode 成功，准备处理节点查找与点击。");
                    tryProcessingScreen(0);
                    rootNode.recycle();
                } else {
                    Log.w(AccessibilityConfig.TAG, "未能获取 rootNode。");
                }

            }, randomDelay);

        }
    }

    @Override
    public void onInterrupt() {
        Log.w(AccessibilityConfig.TAG, "无障碍服务被中断。");
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(AccessibilityConfig.TAG, "无障碍服务已解绑。");
        mHandler.removeCallbacksAndMessages(null);
        return super.onUnbind(intent);
    }

    /**
     * 尝试处理当前屏幕，并包含重试逻辑。
     * 
     * @param attempt 当前的尝试次数。
     */
    private void tryProcessingScreen(int attempt) {
        if (attempt >= AccessibilityConfig.MAX_RETRY_ATTEMPTS) {
            Log.w(AccessibilityConfig.TAG, "已达到最大重试次数，放弃。");
            return;
        }

        Log.d(AccessibilityConfig.TAG, "开始第 " + (attempt + 1) + " 次屏幕处理尝试。");

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.w(AccessibilityConfig.TAG, "尝试时 rootNode 为 null，计划重试。");
            scheduleNextAttempt(attempt + 1); // 根节点为空，直接安排重试
            return;
        }

        boolean processedSuccessfully = false;
        // 遍历所有注册的处理器
        for (ScreenProcessor processor : screenProcessors) {
            if (processor.canProcess(this, rootNode)) {
                Log.d(AccessibilityConfig.TAG, "找到处理器: " + processor.getClass().getSimpleName());
                // 把处理任务交给它，并获取结果
                processedSuccessfully = processor.process(this, rootNode);
                // 找到并处理后，就跳出循环
                break;
            }
        }

        rootNode.recycle();

        // 根据处理结果决定下一步
        if (!processedSuccessfully) {
            // 如果没有找到处理器，或者处理器执行失败
            Log.w(AccessibilityConfig.TAG, "当前屏幕无处理器或处理失败，计划重试。");
            scheduleNextAttempt(attempt + 1);
        }
    }

    /**
     * 重置服务的状态，例如点击标志和待处理任务。
     */
    private void resetServiceState() {
        // 注意：此方法会取消所有挂起的 Handler 任务，包括重试和自动翻页循环。
        Log.d(AccessibilityConfig.TAG, "重置服务状态: 清除所有挂起的 Handler 消息。");
        mHandler.removeCallbacksAndMessages(null); // 取消所有挂起的重试任务
        // [重要] 同时重置翻页处理器的循环标志，以允许它在下次事件检查时可以被重新启动。
        ReadingPageProcessor.resetLoopFlag();
        AdProcessor.resetWaitFlag(); // [新增] 重置广告处理器的等待标志
    }

    /**
     * 安排下一次查找和点击的尝试。
     *
     * @param nextAttempt 下一次尝试的计数。
     */
    private void scheduleNextAttempt(int nextAttempt) {
        if (nextAttempt >= AccessibilityConfig.MAX_RETRY_ATTEMPTS) {
            Log.w(AccessibilityConfig.TAG, "scheduleNextAttempt: 已达最大重试次数，不再调度。");
            return;
        }

        Log.d(AccessibilityConfig.TAG,
                "计划在 " + AccessibilityConfig.RETRY_DELAY_MS + "ms 后进行第 " + (nextAttempt + 1) + " 次尝试。");

        mHandler.postDelayed(() -> {
            Log.d(AccessibilityConfig.TAG, "执行计划中的重试 (第 " + (nextAttempt + 1) + " 次尝试)。");
            AccessibilityNodeInfo newRootNode = getRootInActiveWindow();
            if (newRootNode != null) {
                tryProcessingScreen(nextAttempt); // 注意这里传递的是 nextAttempt
                newRootNode.recycle();
            } else {
                Log.w(AccessibilityConfig.TAG, "计划重试时，newRootNode 为 null。可能再次安排或放弃。");
                // 可以在这里决定是否再次调度，如果根节点仍然为 null
                // scheduleNextAttempt(nextAttempt + 1); // 谨慎，避免死循环
            }
        }, AccessibilityConfig.RETRY_DELAY_MS);
    }

    /**
     * 获取状态管理器
     * 
     * @return 状态管理器
     */
    public ActionStateManager getStateManager() {
        return ActionStateManager.getInstance();
    }

    public Handler getHandler() {
        return mHandler;
    }

    /**
     * 获取一个随机的延迟时间。
     * 
     * @return 2000, 3000, or 5000 (ms)
     */
    private long getRandomProcessingDelay() {
        return processingDelays[random.nextInt(processingDelays.length)];
    }
}
