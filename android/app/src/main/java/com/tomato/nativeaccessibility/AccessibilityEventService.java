package com.tomato.nativeaccessibility;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * 核心的无障碍服务类。
 * 当用户授权后，此服务会在后台运行，监听指定应用的界面变化。
 */
public class AccessibilityEventService extends AccessibilityService {
    // 定义一个日志标签，方便我们过滤日志
    private static final String TAG = "MyAccessibilityService";

    // 定义你想要监听的目标 App 的包名
    private static final String TARGET_PACKAGE_NAME = "com.dragon.read";

    /**
     * 当服务成功连接时被调用。
     * 可以在这里进行一些初始化操作。
     */
    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "无障碍服务已连接。");
    }

    /**
     * 当系统检测到符合我们配置的无障碍事件时，此方法会被调用。
     * 这是处理所有事件的核心入口。
     *
     * @param event 包含事件信息的对象
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }

        // 获取事件来源的 App 包名
        CharSequence packageName = event.getPackageName();

        int eventType = event.getEventType();

        Log.d(TAG, "  - 终于监听到了。 ");
        Log.d(TAG, "  - 事件类型: " + AccessibilityEvent.eventTypeToString(eventType));

        // 判断事件是否来自我们想要监听的目标 App
        if (packageName != null && TARGET_PACKAGE_NAME.equals(packageName.toString())) {
//            int eventType = event.getEventType();

            // 在 Logcat 中打印事件信息，这是验证服务是否工作的关键
            Log.d(TAG, "捕获到目标应用事件 (" + TARGET_PACKAGE_NAME + ")");
            Log.d(TAG, "  - 事件类型: " + AccessibilityEvent.eventTypeToString(eventType));

            // 尝试获取事件相关的文本
            if (!event.getText().isEmpty()) {
                Log.d(TAG, "  - 事件文本: " + event.getText().get(0));
            }

            // 在这里，你可以将事件信息通过广播发送给 React Native 模块
            // (这是我们下一步要做的)
        }
        // 如果不是来自目标 App 的事件，就直接忽略
    }

    /**
     * 当服务被系统中断时调用（例如，权限被关闭或系统需要回收资源）。
     */
    @Override
    public void onInterrupt() {
        Log.w(TAG, "无障碍服务被中断。");
    }

    /**
     * 当服务被解除绑定时调用。
     * @param intent The Intent that was used to bind to this service.
     * @return true if you would like to have the service's onRebind(Intent) method
     *         later called when new clients bind to it.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "无障碍服务已解绑。");
        return super.onUnbind(intent);
    }
}
