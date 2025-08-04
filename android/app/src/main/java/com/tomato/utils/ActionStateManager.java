package com.tomato.utils;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * 管理自动化流程中已完成操作的状态，以防止重复执行。
 * 这个管理器应该在 AccessibilityEventService 的生命周期内作为单例存在。
 */
public class ActionStateManager {

    private static final String TAG = AccessibilityConfig.TAG + ".StateManager";

    private final Set<String> completedActions = new HashSet<>();

    private static final ActionStateManager instance = new ActionStateManager();

    /**
     * 将一个操作标记为已完成。
     *
     * @param actionIdentifier 唯一标识该操作的字符串。
     */
    public void markActionAsCompleted(String actionIdentifier) {
        if (actionIdentifier != null && !actionIdentifier.isEmpty()) {
            completedActions.add(actionIdentifier);
            Log.i(TAG, "Action marked as completed: " + actionIdentifier);
        }
    }

    /**
     * 检查一个操作是否已经被标记为完成。
     *
     * @param actionIdentifier 唯一标识该操作的字符串。
     * @return 如果操作已完成，则返回 true；否则返回 false。
     */
    public boolean isActionCompleted(String actionIdentifier) {
        boolean isCompleted = actionIdentifier != null && completedActions.contains(actionIdentifier);
        if (isCompleted) {
            Log.d(TAG, "Check: Action '" + actionIdentifier + "' is already completed.");
        }
        return isCompleted;
    }

    public static ActionStateManager getInstance() {
        return instance;
    }

    /**
     * 重置所有状态，清除所有已完成的操作记录。
     * 当开始一个全新的自动化任务流时，可以调用此方法。
     */
    public void resetState() {
        Log.i(TAG, "Resetting all action states.");
        completedActions.clear();
    }
}
