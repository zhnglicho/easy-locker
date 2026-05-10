# Android 儿童定时锁屏 easy-locker（超简单锁屏） - MVP 开发文档

## 1. 项目目标

开发一个 Android 本地工具 App，用于控制孩子使用手机的时长。

核心理念：

* 家长在把手机给孩子前设置一个倒计时
* App 后台运行
* 倒计时结束后自动锁屏
* 不做远程控制
* 不做复杂家长监控
* 不做 App 级限制
* 只解决“约定时间结束后自动锁屏”的问题

项目定位：

* MVP
* 自用
* 开源
* 极简
* 稳定优先

---

# 2. 技术栈

## 开发环境

* macOS
* Android Studio
* Android SDK 最新稳定版

---

## 编程语言

* Kotlin

---

## UI 框架

* Jetpack Compose

---

## 架构

推荐：

* MVVM

目录结构：

```text
app/
├── ui/
├── viewmodel/
├── service/
├── receiver/
├── data/
├── utils/
└── model/
```

---

# 3. 核心功能

---

# 页面 1：定时锁屏设置页（首页）

## 功能

家长设置：

* 使用时长
* 提醒时间
* 开始计时

---

## UI 要求
ui 设计图查看 assets 目录下的 lock.png, timedown.png, usage.png

### 使用时长设置

使用：

* 圆形表盘（Dial / Circular Slider）

规则：

* 每 5 分钟一档
* 最大 60 分钟
* 最小 5 分钟

可选值：

```text
5
10
15
20
25
30
35
40
45
50
55
60
```

默认：

```text
30 分钟
```

---

## 提醒时间设置

单选：

```text
关闭
1 分钟
2 分钟
3 分钟
```

默认：

```text
1 分钟
```

---

## 开始按钮

点击后：

* 启动倒计时
* 启动 Foreground Service
* App 可进入后台
* 记录开始时间

---

## 页面文案

标题：

```text
定时锁屏
```

副标题：

```text
设定使用时长，帮助孩子合理使用手机
```

按钮：

```text
开始计时
```

---

# 页面 2：倒计时提醒页

## 触发时机

当剩余时间：

* 1 分钟
* 2 分钟
* 3 分钟

时触发（根据用户设置）。

---

## UI 内容

大标题：

```text
还有 1 分钟
```

副标题：

```text
即将锁屏
```

中间：

* 圆形倒计时

底部提示：

```text
时间结束后将自动锁屏
请提前保存重要内容
```

按钮：

```text
知道了
```

---

## 行为

显示方式：

* Activity
* 全屏弹出

显示 3~5 秒后自动关闭也可以。

---

# 页面 3：使用记录页

## 功能

显示：

* 历史使用记录
* 每次定时多久
* 是否正常结束

---

## 数据字段

每条记录：

```text
开始时间
结束时间
使用时长
状态
日期
```

---

## 状态

仅两种：

```text
正常锁屏
提前结束
```

---

## 页面布局

顶部统计：

```text
今日使用次数
总使用时长
```

下面：

按时间倒序显示历史记录。

---

# 4. 核心技术实现

---

# 倒计时

使用：

```kotlin
CountDownTimer
```

或者：

```kotlin
Coroutine + Flow
```

推荐：

```text
Coroutine + StateFlow
```

更现代。

---

# 后台运行

必须使用：

```text
Foreground Service
```

否则：

* App 在后台容易被系统杀死
* 倒计时会暂停

---

## Foreground Notification

通知内容：

```text
儿童定时锁屏运行中
剩余 xx 分钟
```

点击通知：

回到首页。

---

# 自动锁屏

使用：

```kotlin
DevicePolicyManager.lockNow()
```

---

## 必须实现

### DeviceAdminReceiver

用于获取设备管理员权限。

---

## 首次使用流程

第一次启动时：

引导用户开启：

```text
设备管理员权限
```

否则无法锁屏。

---

# 5. 倒计时结束后的行为

## 流程

```text
倒计时结束
↓
调用 lockNow()
↓
锁屏
↓
停止 Foreground Service
↓
记录本次使用记录
↓
关闭 App
```

---

# 6. 数据存储

使用：

```text
Room Database
```

---

## 表结构

### usage_records

字段：

```text
id
start_time
end_time
duration_minutes
status
created_at
```

---

## 配置存储

使用：

```text
DataStore
```

保存：

```text
默认时长
提醒时间
```

---

# 7. UI 风格要求

整体风格：

* 极简
* 儿童友好
* 柔和配色
* 大按钮
* 圆角卡片

---

# 配色（参考 Google Health 风格）

主色：

```text
浅蓝色
#6C8EF5
```

辅助色：

```text
浅绿色
#7CCBA2
```

提醒色：

```text
浅紫色
#C6B5F7
```

背景：

```text
#F7F8FC
```

卡片：

```text
白色
```

---

# 8. MVP 不做的功能

当前阶段不要实现：

* 家长远程控制
* App 白名单
* AccessibilityService
* 防卸载
* 云同步
* 登录注册
* 多设备同步
* PIN 解锁
* 儿童模式
* App 使用统计
* 每日限制

先保证：

```text
定时 → 后台运行 → 自动锁屏
```

闭环稳定。

---

# 9. Android 权限

需要：

## Device Admin

用于锁屏。

---

## Foreground Service

用于后台计时。

---

## Wake Lock（可选）

防止部分系统暂停计时。

---

# 10. 推荐开发顺序

---

## 第一步

完成：

* Compose UI
* 页面切换

---

## 第二步

完成：

* Circular Slider
* 时间选择

---

## 第三步

完成：

* 倒计时逻辑

---

## 第四步

完成：

* Foreground Service

---

## 第五步

完成：

* Device Admin
* 自动锁屏

---

## 第六步

完成：

* Room 数据记录

---

# 11. 开发目标

MVP 标准：

只要做到：

```text
设置时间
↓
后台运行
↓
提醒
↓
自动锁屏
↓
记录历史
```

即算成功。

不要提前增加复杂功能。
