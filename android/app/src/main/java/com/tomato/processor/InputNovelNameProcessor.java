package com.tomato.processor;

import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

import com.tomato.utils.AccessibilityActionUtils;
import com.tomato.utils.AccessibilityConfig;
import com.tomato.utils.AccessibilityNodeUtils;
import com.tomato.utils.ScreenProcessor;
import com.tomato.nativeaccessibility.AccessibilityEventService;

import java.util.List;

public class InputNovelNameProcessor implements ScreenProcessor {
    @Override
    public boolean canProcess(AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> targetNodes = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.TARGET_FOR_INPUT_BUTTON_4);

        // 如果没有找到输入框，直接返回false
        if (targetNodes.isEmpty()) {
            return false;
        }
        // 获取第一个节点，并检查其文本内容
        AccessibilityNodeInfo targetNode = targetNodes.get(0);
        CharSequence currentText = targetNode.getText();

        // 回收节点
        for (AccessibilityNodeInfo node : targetNodes) {
            if (node != null) {
                node.recycle();
            }
        }

        // 如果文本为"测试输入小说名字"，则表示已经处理过，返回false
        // TODO: 这里是硬编码，字符串稍后要改为动态的
        if (currentText != null && "测试输入小说名字".equals(currentText.toString())) {
            Log.i(AccessibilityConfig.TAG, "输入框中已存在目标文字，无需重复填充。");
            return false;
        }

        // 否则，表示可以进行处理
        return true;
    }

    @Override
    public boolean process(AccessibilityEventService service, AccessibilityNodeInfo rootNode) {
        Log.i(AccessibilityConfig.TAG, "识别到搜索小说对应的输入框，执行文字填充操作...");
        // 找到输入框并填充文字
        List<AccessibilityNodeInfo> targetNodes = AccessibilityNodeUtils.findNodesByResourceID(rootNode, AccessibilityConfig.TARGET_FOR_INPUT_BUTTON_4);

        // 安全检查：如果列表为空，直接返回 false
        if (targetNodes.isEmpty()) {
            Log.w(AccessibilityConfig.TAG, "未能找到目标点击节点。");
            return false;
        }

        // 获取我们需要的节点
        AccessibilityNodeInfo targetNode = targetNodes.get(0);

        try {
            Log.i(AccessibilityConfig.TAG, "数据框对应的节点找到! " );

            // 检查类名是否为 EditText，并且它当前是可编辑的
            boolean isInputField = "android.widget.EditText".equals(targetNode.getClassName().toString())
                    && targetNode.isEditable();
            if (!isInputField) {
                Log.w(AccessibilityConfig.TAG, "找到的节点不是一个可编辑的输入框。");
                return false;
            }

            Log.d(AccessibilityConfig.TAG, "已找到可编辑的输入框。");
            // 使用 AccessibilityActionUtils 执行点击
            // performInput 内部会再次校验可见性和可用性作为安全措施
            // TODO: 这里的字符串稍后要改为动态的
            boolean clickInitiated = AccessibilityActionUtils.performInput(service, targetNode, "测试输入小说名字");
            if (clickInitiated) {
                Log.i(AccessibilityConfig.TAG, "点击操作已成功发起。设置 hasClickedOnThisScreen = true。");
                service.markActionAsCompleted();
                return true;
            } else {
                Log.w(AccessibilityConfig.TAG, "点击操作发起失败 (可能节点在点击前变为不可见/不可用)。");
                return false;
            }
        } finally {
            Log.d(AccessibilityConfig.TAG, "回收 " + targetNodes.size() + " 个找到的节点。");
            // 遍历列表，回收每一个节点
            for (AccessibilityNodeInfo node : targetNodes) {
                if (node != null) {
                    node.recycle();
                }
            }
        }
    }
}
