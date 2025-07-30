/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */
import React, { useState, useEffect } from 'react';
import { NewAppScreen } from '@react-native/new-app-screen';
import {
  StatusBar,
  StyleSheet,
  useColorScheme,
  View,
  SafeAreaView,
  Text,
} from 'react-native';

import NativeAccessibility from './NativeAccessibility/NativeAccessibility';

function App() {
  const isDarkMode = useColorScheme() === 'dark';
  const [moduleName, setModuleName] = useState<string>('');

  useEffect(() => {
    try {
      const name = NativeAccessibility.getModuleName();
      setModuleName(name);
    } catch (e) {
      console.error('调用 getModuleName 失败', e);
      setModuleName('获取模块名失败');
    }
  }, []);

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
      <Text style={styles.moduleName}>{moduleName}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  errorText: {
    padding: 20,
    fontSize: 16,
    color: 'red',
    textAlign: 'center',
  },
  moduleName: {
    fontSize: 14,
    fontStyle: 'italic',
    color: '#555',
  },
});

export default App;
