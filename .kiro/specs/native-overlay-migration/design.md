# 设计文档

## 概述

本设计文档描述了将 A8Wisper 悬浮窗系统从 Tauri webview 渲染迁移到纯原生 Python 渲染的技术方案。通过采用轻量级的原生渲染技术，我们将显著减少应用程序的打包体积，提高性能，并简化架构，同时保持所有现有的视觉效果和用户体验。

### 设计目标

1. **体积优化**: 减少至少 20MB 的打包体积
2. **性能提升**: 实现更流畅的动画效果和更低的资源占用
3. **架构简化**: 移除悬浮窗的 Tauri 依赖，保持主界面不变
4. **兼容性保持**: 确保与现有系统的完全兼容

## 架构

### 当前架构 vs 目标架构

**当前架构:**
```
主应用 (Python) 
├── WebSocket 服务器
├── 音频处理引擎
├── ASR/LLM 引擎
└── Tauri 界面
    ├── 主配置窗口 (React/TypeScript)
    └── 悬浮窗 (React/TypeScript + webview)
```

**目标架构:**
```
主应用 (Python)
├── WebSocket 服务器
├── 音频处理引擎
├── ASR/LLM 引擎
├── 原生悬浮窗 (Tkinter/ctypes)
└── Tauri 主界面 (React/TypeScript) [保持不变]
```

### 技术选型分析

基于研究和需求分析，我们有以下几个技术选项：

#### 选项 1: Tkinter (推荐)
- **优势**: Python 内置，零额外依赖，体积最小
- **劣势**: 动画性能相对较低，样式定制有限
- **适用性**: 对于简单的悬浮窗动画完全足够

#### 选项 2: ctypes + Win32 API
- **优势**: 最高性能，完全原生，体积极小
- **劣势**: 开发复杂度高，平台特定
- **适用性**: 性能要求极高的场景

#### 选项 3: pygame (不推荐)
- **优势**: 动画性能好，游戏级渲染
- **劣势**: 额外依赖，体积较大，过度设计

**最终选择**: Tkinter + 必要时的 ctypes 增强

## 组件和接口

### 核心组件

#### 1. NativeOverlayWindow
```python
class NativeOverlayWindow:
    """原生悬浮窗主类"""
    
    def __init__(self):
        self.root = None
        self.canvas = None
        self.state = "IDLE"
        self.animation_timer = None
        self.audio_level = 0.0
        
    def create_window(self) -> None:
        """创建悬浮窗"""
        
    def set_state(self, state: str) -> None:
        """设置悬浮窗状态"""
        
    def update_audio_level(self, level: float) -> None:
        """更新音频电平"""
        
    def show(self) -> None:
        """显示悬浮窗"""
        
    def hide(self) -> None:
        """隐藏悬浮窗"""
```

#### 2. AnimationEngine
```python
class AnimationEngine:
    """动画引擎"""
    
    def __init__(self, canvas):
        self.canvas = canvas
        self.frame_count = 0
        self.animations = {}
        
    def start_animation(self, animation_type: str) -> None:
        """启动指定类型的动画"""
        
    def stop_animation(self) -> None:
        """停止所有动画"""
        
    def update_frame(self) -> None:
        """更新动画帧"""
        
    def draw_ripple_effect(self) -> None:
        """绘制涟漪效果"""
        
    def draw_spinner_effect(self) -> None:
        """绘制旋转器效果"""
        
    def draw_pulse_effect(self) -> None:
        """绘制脉冲效果"""
```

#### 3. WindowManager
```python
class WindowManager:
    """窗口管理器"""
    
    def __init__(self):
        self.screen_info = None
        self.dpi_scale = 1.0
        
    def get_screen_info(self) -> dict:
        """获取屏幕信息"""
        
    def calculate_position(self) -> tuple:
        """计算悬浮窗位置"""
        
    def handle_dpi_change(self) -> None:
        """处理 DPI 变化"""
        
    def set_window_properties(self, window) -> None:
        """设置窗口属性（置顶、透明等）"""
```

#### 4. StateManager
```python
class StateManager:
    """状态管理器"""
    
    def __init__(self, overlay_window):
        self.overlay_window = overlay_window
        self.websocket_client = None
        
    def connect_websocket(self) -> None:
        """连接 WebSocket 服务器"""
        
    def handle_state_message(self, message: dict) -> None:
        """处理状态消息"""
        
    def handle_audio_level_message(self, message: dict) -> None:
        """处理音频电平消息"""
```

### 接口设计

#### WebSocket 消息接口
```python
# 状态更新消息
{
    "type": "app_state",
    "data": "RECORDING" | "RECOGNIZING" | "POLISHING" | "PROCESSING" | "IDLE"
}

# 音频电平消息
{
    "type": "audio_level", 
    "data": 0.0  # 0.0 - 1.0
}
```

#### 配置接口
```python
# 悬浮窗配置
overlay_config = {
    "enabled": True,
    "position": "bottom_center",  # 位置
    "size": {"width": 180, "height": 44},  # 尺寸
    "opacity": 0.95,  # 不透明度
    "animation_fps": 30,  # 动画帧率
    "colors": {
        "recording": "#EF4444",    # 红色
        "recognizing": "#3B82F6",  # 蓝色  
        "polishing": "#8B5CF6",    # 紫色
        "processing": "#FFFFFF"    # 白色
    }
}
```

## 数据模型

### 动画状态模型
```python
@dataclass
class AnimationState:
    """动画状态数据模型"""
    type: str  # "ripple", "spinner", "pulse", "idle"
    frame_count: int
    audio_level: float
    color: str
    active: bool
    
@dataclass  
class RippleAnimation:
    """涟漪动画数据模型"""
    ripples: List[dict]  # [{"radius": float, "alpha": float}]
    center_x: int
    center_y: int
    max_radius: int
    
@dataclass
class SpinnerAnimation:
    """旋转器动画数据模型"""
    angle: float
    speed: float
    orbit_radius: int
    
@dataclass
class PulseAnimation:
    """脉冲动画数据模型"""
    scale: float
    direction: int  # 1 or -1
    min_scale: float
    max_scale: float
```

### 窗口状态模型
```python
@dataclass
class WindowState:
    """窗口状态数据模型"""
    visible: bool
    x: int
    y: int
    width: int
    height: int
    dpi_scale: float
    always_on_top: bool
    transparent: bool
```

## 功能验证要点

基于需求分析，以下是需要在最终编译运行测试中验证的关键功能点：

### 窗口基础功能
- 悬浮窗能够正确创建并具有无边框、始终置顶和透明背景属性
- 悬浮窗能够正确定位在主显示器的底部中央位置
- 悬浮窗保持 180x44 像素的固定尺寸

### 状态管理功能
- 状态管理器能够正确处理所有有效状态值的转换
- 当状态为 IDLE 时悬浮窗隐藏，非 IDLE 时可见
- WebSocket 消息能够正确解析并应用到悬浮窗状态

### 动画效果功能
- 各种状态下的动画效果能够正确显示
- 动画帧率保持在合理范围内
- 音频电平变化能够及时反映在视觉效果中

### 性能和兼容性
- 悬浮窗启动时间在可接受范围内
- 资源使用保持在合理水平
- 多显示器和不同 DPI 设置下正常工作

### Tauri 依赖移除
- 构建产物中不包含悬浮窗相关的 Tauri 文件
- 应用启动时不初始化悬浮窗 webview
- 打包体积有明显减少

## 错误处理

### 错误类型和处理策略

#### 1. 初始化错误
- **Tkinter 初始化失败**: 降级到基本文本显示模式
- **屏幕信息获取失败**: 使用默认位置和尺寸
- **WebSocket 连接失败**: 启用重连机制，最多重试 3 次

#### 2. 运行时错误  
- **动画渲染错误**: 暂停动画，记录错误日志
- **状态更新错误**: 重置为 IDLE 状态
- **内存不足**: 清理动画缓存，降低帧率

#### 3. 系统环境错误
- **DPI 变化检测失败**: 使用上次已知的 DPI 设置
- **多显示器配置变化**: 重新检测主显示器
- **权限不足**: 提示用户以管理员身份运行

### 错误恢复机制
```python
class ErrorHandler:
    def __init__(self):
        self.retry_count = 0
        self.max_retries = 3
        
    def handle_initialization_error(self, error):
        """处理初始化错误"""
        if self.retry_count < self.max_retries:
            self.retry_count += 1
            return self.retry_initialization()
        else:
            return self.fallback_mode()
            
    def handle_runtime_error(self, error):
        """处理运行时错误"""
        self.log_error(error)
        return self.safe_reset()
```

## 测试策略

### 简化测试方法

基于项目需求，我们将采用简化的测试策略，专注于业务逻辑实现：

#### 编译运行测试
- **语法验证**: 确保所有 Python 模块能正常导入和编译
- **基本功能测试**: 验证悬浮窗能正常启动、显示和响应状态变化
- **集成测试**: 确保与主应用程序的正确集成
- **依赖移除验证**: 确认 Tauri 悬浮窗依赖已完全移除

#### 测试时机
- **开发阶段**: 专注于业务逻辑实现，不编写测试代码
- **完成阶段**: 在所有业务逻辑实现完毕后，进行编译运行测试
- **验证重点**: 确保代码能正常运行，功能符合需求

#### 测试环境要求
- Windows 10/11 测试环境
- 基本的分辨率和 DPI 设置测试
- 与现有 WebSocket 服务器的连接测试