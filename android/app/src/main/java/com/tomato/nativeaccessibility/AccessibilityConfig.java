package com.tomato.nativeaccessibility;

/**
 * 用于存放无障碍服务相关配置的常量。
 * 将所有可配置的字符串和数值集中管理，便于维护。
 */
public final class AccessibilityConfig {

    // 私有构造函数，防止该类被实例化
    private AccessibilityConfig() {}

    // --- App Targeting ---
    /**
     * 目标应用的包名。
     */
    public static final String TARGET_PACKAGE_NAME = "com.dragon.read";

    // --- View IDs ---
    /**
     * 小说首页顶部输入框左侧搜索按钮的资源ID。
     */
    public static final String TARGET_FOR_INPUT_BUTTON = "com.dragon.read:id/c8";

    // --- Logging ---
    /**
     * 日志标签。
     */
    public static final String TAG = "MyAccessibilityService";

    // --- Retry Logic ---
    /**
     * 查找节点的最大重试次数。
     */
    public static final int MAX_RETRY_ATTEMPTS = 20;

    /**
     * 每次重试的间隔时间（毫秒）。
     */
    public static final long RETRY_DELAY_MS = 1000;

    // --- Gesture Simulation ---
    /**
     * 模拟手势点击的持续时间（毫秒）。
     */
    public static final long GESTURE_CLICK_DURATION_MS = 20;
}

