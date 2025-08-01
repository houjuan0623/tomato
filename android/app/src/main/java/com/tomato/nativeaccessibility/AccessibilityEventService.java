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
import com.tomato.utils.ScreenProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * 核心的无障碍服务类。
 * 当用户授权后，此服务会在后台运行，监听指定应用的界面变化。
 *
 * [已修改]
 */
public class AccessibilityEventService extends AccessibilityService {

    // 状态标志位：防止在同一个界面上重复点击
    private boolean hasClickedOnThisScreen = false;

    // 使用 Handler 来处理延迟操作，避免阻塞主线程
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    // 创建一个处理器列表
    private final List<ScreenProcessor> screenProcessors = new ArrayList<>();

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
        screenProcessors.add(new MainPageProcessor());
        screenProcessors.add(new AddToHomePageProcessor());
        screenProcessors.add(new InputNovelNameProcessor());
        screenProcessors.add(new SearchNovelProcessor());
        // ... 如果有更多界面，继续添加 ...
    }

    /**
     * 新增一个公共方法，供处理器调用来更新状态
     */
    public void markActionAsCompleted() {
        Log.i(AccessibilityConfig.TAG, "处理器报告操作完成，设置 hasClickedOnThisScreen = true。");
        this.hasClickedOnThisScreen = true;
        mHandler.removeCallbacksAndMessages(null); // 取消所有重试
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
            // 如果用户之前在目标应用内，现在离开了，就重置状态
            if (hasClickedOnThisScreen) {
                Log.d(AccessibilityConfig.TAG, "用户离开目标应用，重置所有状态。");
                hasClickedOnThisScreen = false;
                // 当离开目标应用时，取消所有待处理的重试任务
                mHandler.removeCallbacksAndMessages(null);
            }
            return;
        }

        int eventType = event.getEventType();

        // 主要监听窗口变化事件，这是进入新界面的最可靠信号
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.d(AccessibilityConfig.TAG, "接收到事件: " + AccessibilityEvent.eventTypeToString(eventType) +
                    " 来自包: " + event.getPackageName() +
                    " 类名: " + event.getClassName());

            // 当窗口状态改变时，通常表示进入新屏幕，此时重置点击状态
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                Log.i(AccessibilityConfig.TAG, "窗口状态改变，重置点击标记。");
                resetServiceState(); // 重置点击标记和取消挂起的重试
            }

            // 如果本屏幕已点击过，并且不是因为窗口状态改变而重置的，则不再处理
            // (允许在窗口状态改变后立即重新尝试点击)
            if (hasClickedOnThisScreen && eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                Log.d(AccessibilityConfig.TAG, "已在本屏幕点击过，忽略事件: " + AccessibilityEvent.eventTypeToString(eventType));
                return;
            }

            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // 只有当尚未在本屏幕点击过时，才尝试查找和点击
                if (!hasClickedOnThisScreen) {
                    Log.d(AccessibilityConfig.TAG, "尝试处理节点查找与点击...");
                    tryProcessingScreen(0);
                } else {
                    Log.d(AccessibilityConfig.TAG, "hasClickedOnThisScreen 为 true，跳过 tryProcessingScreen。");
                }
                rootNode.recycle();
            } else {
                // 在这里使用延迟处理的策略解决窗口滑动的瞬间出现的问题。
                // RootNode 为 null，特别是对于内容变化或滑动事件，尝试延迟获取
                Log.w(AccessibilityConfig.TAG, "事件 " + AccessibilityEvent.eventTypeToString(eventType) + " 发生时，rootNode 为 null。尝试延迟获取...");
                mHandler.postDelayed(() -> {
                    Log.d(AccessibilityConfig.TAG, "延迟后尝试重新获取 rootNode。");
                    AccessibilityNodeInfo delayedRootNode = getRootInActiveWindow();
                    if (delayedRootNode != null) {
                        if (!hasClickedOnThisScreen) { // 再次检查点击状态
                            Log.d(AccessibilityConfig.TAG, "延迟获取 rootNode 成功，准备处理节点查找与点击。");
                            tryProcessingScreen(0);
                        }
                        delayedRootNode.recycle();
                    } else {
                        Log.w(AccessibilityConfig.TAG, "延迟后仍然未能获取 rootNode。");
                    }
                }, 2000); // 延迟 2 秒，这个值需要测试和调整
            }
        }
    }

    /**
     * 尝试处理当前屏幕，并包含重试逻辑。
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
            if (processor.canProcess(rootNode)) {
                Log.d(AccessibilityConfig.TAG, "找到处理器: " + processor.getClass().getSimpleName());
                // 把处理任务交给它，并获取结果
                processedSuccessfully = processor.process(this, rootNode);
                // 找到并处理后，就跳出循环
                break;
            }
        }

        rootNode.recycle();

        // 根据处理结果决定下一步
        if (processedSuccessfully) {
            // 如果成功，markActionAsCompleted 已经在处理器内部调用了
            // 所以这里不需要做任何事
            Log.i(AccessibilityConfig.TAG, "屏幕处理成功。");
        } else {
            // 如果没有找到处理器，或者处理器执行失败
            Log.w(AccessibilityConfig.TAG, "当前屏幕无处理器或处理失败，计划重试。");
            scheduleNextAttempt(attempt + 1);
        }
    }

    /**
     * 重置服务的状态，例如点击标志和待处理任务。
     */
    private void resetServiceState() {
        Log.d(AccessibilityConfig.TAG, "重置服务状态: hasClickedOnThisScreen = false, 清除 Handler 消息。");
        hasClickedOnThisScreen = false;
        mHandler.removeCallbacksAndMessages(null); // 取消所有挂起的重试任务
    }

    /**
     * 安排下一次查找和点击的尝试。
     *
     * @param nextAttempt 下一次尝试的计数。
     */
    private void scheduleNextAttempt(int nextAttempt) {
        // 如果已经点击，或已达到最大尝试次数，则不调度
        if (hasClickedOnThisScreen) {
            Log.d(AccessibilityConfig.TAG, "scheduleNextAttempt: 已点击，不调度重试。");
            return;
        }
        if (nextAttempt >= AccessibilityConfig.MAX_RETRY_ATTEMPTS) {
            Log.w(AccessibilityConfig.TAG, "scheduleNextAttempt: 已达最大重试次数，不再调度。");
            return;
        }

        Log.d(AccessibilityConfig.TAG, "计划在 " + AccessibilityConfig.RETRY_DELAY_MS + "ms 后进行第 " + (nextAttempt + 1) + " 次尝试。");

        mHandler.postDelayed(() -> {
            // 在执行延迟任务前，再次检查是否已经点击过了
            if (hasClickedOnThisScreen) {
                Log.d(AccessibilityConfig.TAG, "延迟任务执行时发现已点击，取消重试。");
                return;
            }
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
}
