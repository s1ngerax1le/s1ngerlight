# s1ngerlight (DecoLedController)

专为 **Redmi Turbo 4 Pro (`onyx`)** 打造的摄像头 Deco 环形氛围灯硬件级控制应用。基于 **Jetpack Compose + Material 3 Expressive** 设计语言重构，通过 Root 权限（KernelSU / Magisk / APatch）直接驱动底层 `/sys/class/leds/aw21024_led` 硬件节点，提供实时光环预览、无极色彩与亮度调节、多种动态灯效、纯白相机补光及防系统抢占保护。

---

## ✨ 核心功能

1. **底层硬件直驱 (`LedController` + `InteractiveRootSession`)**
   - 通过常驻交互式 Root Shell 会话毫秒级读写 `/sys/class/leds/aw21024_led` 节点，零卡顿实时响应滑块与调色盘操作。
2. **四种灯效模式与速度控制**
   - **常亮 (`EFFECT_STATIC`)**：稳定输出设定色彩与亮度。
   - **呼吸 (`EFFECT_BREATH`)**：平滑余弦波呼吸渐变，支持自定义呼吸周期（`300ms ~ 3000ms`）。
   - **闪烁 (`EFFECT_BLINK`)**：方波周期闪烁，支持自定义闪烁速度。
   - **律动 (`EFFECT_STROBE`)**：`60ms` 高频爆闪律动效果。
3. **纯白补光模式 (White Boost)**
   - 一键锁定 `100%` 极纯白光（`#FFFFFF` 常亮满亮度），作为暗光环境下的后置相机辅助柔光环；关闭后自动恢复先前的自定义色彩与灯效配置。
4. **防系统抢占锁 (Shield 保护) & 一键全熄锁定**
   - **Shield 模式**：应用灯效后自动锁定驱动节点权限（`chmod 444`），防止系统充电提示或通知强行覆盖用户设定的氛围灯状态。
   - **一键全熄并锁定**：切断 LED 使能引脚并施加只读保护锁，彻底杜绝夜间漏光或系统误触发。
5. **开机自启与控制中心快捷磁贴**
   - **开机自启保持 (`BootReceiver` & `service.d`)**：支持写入 `/data/adb/service.d/disable_deco_led.sh` 及监听 `BOOT_COMPLETED`，重启手机后自动恢复灯光配置。
   - **Quick Settings 快捷磁贴 (`DecoTileService`)**：支持添加至系统下拉控制中心，随时一键开关 Deco 氛围灯。
6. **Material 3 Expressive 视觉体验**
   - 采用 Android 12+ Monet 动态取色、位置感知圆角分段列表 (`SegmentedGroup`)、`Cookie9Sided` 旋转徽章、连体分段按钮组 (`ButtonGroup`) 与深色相机 Deco 模组实时 Canvas 动态光环预览 (`HaloPreview`)。

---

## 🛠️ 技术栈与工程结构

- **UI 框架**：Jetpack Compose + Material 3 Expressive (`androidx.compose.material3:material3:1.5.0-alpha27`)
- **构建工具**：Gradle `9.3.1` + Android Gradle Plugin `9.1.1` + Kotlin `2.4.10` (`compileSdk 37`, `minSdk 26`)

```text
app/src/main/java/com/onyx/decoled/
├── MainActivity.kt                  # Compose 沉浸式主入口
├── MainViewModel.kt                 # 灯光状态管理与硬件同步 ViewModel
├── LedController.java               # /sys/class/leds/aw21024_led 底层硬件驱动与动画线程
├── InteractiveRootSession.java      # 常驻低延迟交互式 Root Shell 会话
├── RootShell.java                   # Root 权限探测与单次命令执行工具
├── DecoTileService.java             # 系统下拉控制中心快捷开关磁贴服务
├── BootReceiver.java                # 开机广播接收器（自动恢复灯光状态）
└── ui/
    ├── components/
    │   ├── HaloPreview.kt           # 深色相机 Deco 底座 + 动态光环 Canvas 实时预览
    │   ├── HeroStatusCard.kt        # 顶部主控卡片（带 Cookie9Sided 自转徽章）
    │   ├── ColorPad.kt              # Compose 2D HSV 无极调色盘
    │   ├── ColorSwatches.kt         # 预设色块选择器
    │   ├── SegmentedGroup.kt        # 位置感知圆角与按压弹性形变分段卡片容器
    │   ├── ListRows.kt              # 标准列表行组件
    │   ├── IconBadge.kt             # 图标与带对比底托的颜色徽章
    │   ├── SectionHeader.kt         # 分组标题组件
    │   └── Haptics.kt               # 全局触觉反馈封装
    ├── screens/
    │   ├── AppScaffold.kt           # 紧凑单层 TopAppBar 与底部导航框架
    │   ├── HomeScreen.kt            # 灯光控制主页
    │   └── SettingsScreen.kt        # Root 探针、Shield 保护、快捷磁贴与全熄锁定设置页
    └── theme/
        ├── Theme.kt                 # Monet 动态色彩与 M3 Expressive 动效主题
        └── Tokens.kt                # Tonal 表面层级与尺寸设计 Token
```

---

## 🚀 编译与安装

1. **构建 Debug APK**：
   ```bash
   ./gradlew assembleDebug
   ```
   编译产物位于：`app/build/outputs/apk/debug/app-debug.apk`。
2. **运行要求**：
   - 设备需具备 Root 权限（推荐 KernelSU / Magisk），并存在 `/sys/class/leds/aw21024_led` 驱动节点（如 Redmi Turbo 4 Pro / POCO F7 Pro `onyx`）。
