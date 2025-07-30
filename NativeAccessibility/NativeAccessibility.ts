import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

// 1. 定义接口，明确模块提供给JS的方法、参数和返回值类型
export interface Spec extends TurboModule {
  // 你也可以在这里添加一个简单的同步方法作为示例
  getModuleName(): string;
}

// 2. 向React Native注册我们的模块
// 'NativeAccessibility' 是在原生代码中使用的模块名称，必须保持一致
export default TurboModuleRegistry.getEnforcing<Spec>('NativeAccessibility');
