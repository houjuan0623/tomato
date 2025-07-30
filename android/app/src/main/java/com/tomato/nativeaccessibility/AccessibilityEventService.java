package com.tomato.nativeaccessibility;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * 核心的无障碍服务类。
 * 当用户授权后，此服务会在后台运行，监听指定应用的界面变化。
 */
public class AccessibilityEventService extends AccessibilityService {
    // 定义一个日志标签，方便我们过滤日志
    private static final String TAG = "MyAccessibilityService";
    // 点击小说首页顶部输入框左侧的搜索按钮进入搜索界面
    private static final String TARGET_FOR_INPUT_BUTTON = "com.dragon.read:id/c8";
    // 定义你想要监听的目标 App 的包名
    private static final String TARGET_PACKAGE_NAME = "com.dragon.read";

    // 状态标志位：防止在同一个界面上重复点击
    private boolean hasClickedOnThisScreen = false;
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
        if (packageName == null || !(TARGET_PACKAGE_NAME.equals(packageName.toString()))) {
            // 如果用户切换到了其他应用，重置点击标志位
            if (hasClickedOnThisScreen) {
                Log.d(TAG, "用户离开目标应用，重置点击状态。");
                hasClickedOnThisScreen = false;
            }
            return;
        }

        int eventType = event.getEventType();

        Log.d(TAG, "检测到事件");
        // 主要触发器：窗口状态改变（进入新页面）
        // 备用触发器：窗口内容改变（页面内UI刷新）
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            // 如果已经在这个页面点击过了，就直接返回，不再处理
            if (hasClickedOnThisScreen) {
                return;
            }
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                return;
            }

            // 尝试查找并点击按钮
            findAndClickReadyButton(rootNode);

            // AccessibilityNodeInfo 并非一个普通的Java对象。它只是一个“外壳”，内部关联着系统底层（通常是C++实现）的大量数据和资源。
            // 用完之后需要回收
            rootNode.recycle();
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

    /** ----------------------------------自定义函数--------------------------------*/
    /**
     * 查找并点击处于“就绪”状态的按钮
     * @param rootNode 根节点
     */
    private void findAndClickReadyButton(AccessibilityNodeInfo rootNode) {
        try {
            Thread.sleep(10000); // 等待1秒
        } catch (InterruptedException e) {
            Log.w(TAG, "等待10s被打断", e);
            Thread.currentThread().interrupt(); // 重新设置中断状态
            return; // 线程中断，直接退出方法
        }
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(TARGET_FOR_INPUT_BUTTON);

        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                Log.d(TAG, "找到目标节点：" + node.toString());
                if (node == null) {
                    continue; // 跳过 null 节点
                }
                boolean clicked = false;

                for (int attempt = 0; attempt < 20; attempt++) { // 最多尝试20次，即等待20秒
                    // 核心检查：确保按钮可见、可用且可点击
                    if (node.isVisibleToUser() && node.isEnabled()) {
                        Log.d(TAG, "按钮已就绪 (尝试 " + (attempt + 1) + " 次)，执行点击！");
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.d(TAG, "已经执行到这里");
                        // 关键一步：设置标志位，表示我们已经成功点击
                        hasClickedOnThisScreen = true;
                        clicked = true;
                        break; // 点击成功，跳出重试循环
                    } else {
                        Log.d(TAG, "按钮未就绪 (尝试 " + (attempt + 1) + " 次)。可见: " + node.isVisibleToUser() + ", 可用: " + node.isEnabled());
                        if (attempt < 19) { // 如果不是最后一次尝试，则等待
                            try {
                                Thread.sleep(1000); // 等待1秒
                            } catch (InterruptedException e) {
                                Log.w(TAG, "等待被打断", e);
                                Thread.currentThread().interrupt(); // 重新设置中断状态
                                node.recycle(); // 发生异常时也回收节点
                                return; // 线程中断，直接退出方法
                            }
                        }
                    }
                }

                if (node != null) {
                    node.recycle();
                }

                if (clicked) {
                    Log.d(TAG, "成功点击按钮并退出查找。");
                    return; // 成功点击后就可以退出了，避免操作其他同ID的节点
                } else {
                    Log.d(TAG, "等待20秒后，按钮仍然未就绪或未找到。");
                    return; // 结束当前按钮的处理
                }
            }
        } else {
            Log.d(TAG, "根据提供的resource id未找到节点");
        }
    }
}
