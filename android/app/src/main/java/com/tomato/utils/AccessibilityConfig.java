package com.tomato.utils;

/**
 * 用于存放无障碍服务相关配置的常量。
 * 将所有可配置的字符串和数值集中管理，便于维护。
 */
public final class AccessibilityConfig {

    // 私有构造函数，防止该类被实例化
    private AccessibilityConfig() {}

    // --- App Targeting ---
    /**
     * 目标应用的包名。番茄小说
     */
    public static final String TARGET_PACKAGE_NAME_1 = "com.dragon.read";

    /**
     * 目标应用的包名。安卓弹出来的悬浮窗，提示用户是否要将某个界面添加到主屏。
     */
    public static final String TARGET_PACKAGE_NAME_2 = "com.sec.android.app.launcher";

    // --- View IDs ---
    /**
     * 小说首页顶部输入框左侧搜索按钮的资源ID。
     */
    public static final String TARGET_FOR_INPUT_BUTTON_1 = "com.dragon.read:id/c8";

    /**
     * 提示用户是否要将某个界面添加到主屏的顶部title的资源ID。
     */
    public static final String TARGET_FOR_INPUT_BUTTON_2 = "com.sec.android.app.launcher:id/add_item_title";

    /**
     * 提示用户是否要将某个界面添加到主屏的取消按钮的资源ID。
     */
    public static final String TARGET_FOR_INPUT_BUTTON_3 = "com.sec.android.app.launcher:id/cancel_button";

    /**
     * 番茄小说搜索界面的输入框。
     */
    public static final String TARGET_FOR_INPUT_BUTTON_4 = "com.dragon.read:id/gfy";

    /**
     * 番茄小说首页界面的分类按钮。用以判断当前界面是否是首页。
     */
    public static final String TARGET_FOR_INPUT_BUTTON_5 = "com.dragon.read:id/hia";

    /**
     * 搜索页面的搜索按钮对应的resource id。
     */
    public static final String TARGET_FOR_INPUT_BUTTON_6 = "com.dragon.read:id/gh2";

    // --- Logging ---
    /**
     * 日志标签。
     */
    public static final String TAG = "MyAccessibilityService";

    // --- Retry Logic ---
    /**
     * 查找节点的最大重试次数。
     */
    public static final int MAX_RETRY_ATTEMPTS = 5;

    /**
     * 每次重试的间隔时间（毫秒）。
     */
    public static final long RETRY_DELAY_MS = 1000;

    // --- Gesture Simulation ---
    /**
     * 模拟手势点击的持续时间（毫秒）。
     */
    public static final long GESTURE_CLICK_DURATION_MS = 20;


    // --- Action Identifiers for State Management ---
    public static final String ACTION_ID_CLICK_MAIN_PAGE_SEARCH = "action_click_main_page_search";
    public static final String ACTION_ID_INPUT_NOVEL_NAME = "action_input_novel_name";
    public static final String ACTION_ID_CLICK_SEARCH_BUTTON = "action_click_search_button";
    public static final String ACTION_ID_DISMISS_ADD_TO_HOME_DIALOG = "action_dismiss_add_to_home_dialog";
}

