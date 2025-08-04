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

    // 私有构造函数，确保单例模式，并根据请求在初始化时将所有操作标记为已完成
    private ActionStateManager() {
        // 初始化时，将所有已知的操作标记为已完成
        markAllActionsAsCompleted();
    }

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

    /**
     * 将一个操作标记为待处理（即，未完成），从而可以被执行。
     * 这对于在默认所有操作都完成的情况下，选择性地启用某个步骤很有用。
     *
     * @param actionIdentifier 唯一标识该操作的字符串。
     */
    public void markActionAsPending(String actionIdentifier) {
        if (actionIdentifier != null && completedActions.remove(actionIdentifier)) {
            Log.i(TAG, "Action marked as PENDING (re-enabled): " + actionIdentifier);
        }
    }

    /**
     * 将所有已知的操作标记为已完成。
     * 这可以用于在开始一个新流程前，禁用所有自动化步骤。
     */
    public void markAllActionsAsCompleted() {
        Log.i(TAG, "Initializing: Marking all known actions as COMPLETED by default.");
        completedActions.add(AccessibilityConfig.ACTION_ID_CLICK_MAIN_PAGE_SEARCH);
        completedActions.add(AccessibilityConfig.ACTION_ID_INPUT_NOVEL_NAME);
        completedActions.add(AccessibilityConfig.ACTION_ID_CLICK_SEARCH_BUTTON);
        completedActions.add(AccessibilityConfig.ACTION_ID_DISMISS_ADD_TO_HOME_DIALOG);
        completedActions.add(AccessibilityConfig.ACTION_ID_FIND_AND_CLICK_NOVEL);
        completedActions.add(AccessibilityConfig.ACTION_ID_CLICK_START_READING);
    }

    public static ActionStateManager getInstance() {
        return instance;
    }

    /**
     * 重置所有状态，清除所有已完成的操作记录。
     * 当开始一个全新的自动化任务流时，可以调用此方法。调用后，所有操作都将变为“待处理”状态。
     */
    public void resetState() {
        Log.i(TAG, "Resetting all action states to PENDING.");
        completedActions.clear();
    }
}
