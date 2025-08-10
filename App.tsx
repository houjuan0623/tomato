/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */
import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  Alert,
  TouchableOpacity,
} from 'react-native';

import NativeAccessibility from './NativeAccessibility/NativeAccessibility';

function App() {
  // const [moduleName, setModuleName] = useState<string>('');
  const [inputText, setInputText] = useState<string>('');

  // useEffect(() => {
  //   try {
  //     const name = NativeAccessibility.getModuleName();
  //     setModuleName(name);
  //   } catch (e) {
  //     console.error('调用 getModuleName 失败', e);
  //     setModuleName('获取模块名失败');
  //   }
  // }, []);

  const handleSendTextToNative = () => {
    if (!inputText.trim()) {
      Alert.alert('提示', '请输入内容后再发送');
      return;
    }
    try {
      // 调用原生模块的新方法，并传递输入框的文本
      NativeAccessibility.performSearch(inputText);
    } catch (e) {
      console.error('调用 performSearch 失败', e);
    }
  };

  // 检查模块是否成功加载
  if (!NativeAccessibility) {
    return (
      <View style={styles.container}>
        <Text style={styles.errorText}>
          错误：原生模块 'NativeAccessibility' 加载失败。
          请检查以上所有配置步骤是否正确，特别是 `package.json`
          和原生代码的注册。 然后重新编译您的应用 (npx react-native
          run-android)。
        </Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>工具：</Text>
      {/* <Text style={styles.moduleName}>模块名: {moduleName}</Text> */}
      <TextInput
        style={styles.input}
        placeholder="请输入要搜索的内容..."
        onChangeText={setInputText}
        value={inputText}
      />
      <TouchableOpacity
        style={styles.button}
        onPress={handleSendTextToNative}
        testID="start_execution_button" //  <-- 在这里设置 nativeID
      >
        <Text style={styles.buttonText}>开始执行</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
  },
  errorText: {
    padding: 20,
    fontSize: 16,
    color: 'red',
    textAlign: 'center',
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  moduleName: {
    fontSize: 14,
    fontStyle: 'italic',
    color: '#555',
    marginBottom: 20,
  },
  input: {
    height: 40,
    borderColor: 'gray',
    borderWidth: 1,
    marginBottom: 10,
    paddingHorizontal: 10,
  },
  button: {
    backgroundColor: '#007AFF',
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default App;
