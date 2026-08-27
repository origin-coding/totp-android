# Android TOTP 身份验证器

[English](README.md) | [简体中文](README.zh-CN.md)

一款小型原生 Android 身份验证器，用于生成符合标准的基于时间的一次性密码（TOTP）。

本仓库是一个专注于现代 Android 开发、Android 安全 API、自动化测试和 APK 分发的学习项目。签名构建通过 GitHub Releases 发布。

## 项目目标

本项目旨在提供一款小巧、易于理解的 TOTP 身份验证器，并实现以下目标：

- 支持标准身份验证器应用常用的 TOTP 格式；
- 将账户数据和密钥保存在设备本地；
- 在可行的情况下使用 Android 平台和 Jetpack API；
- 展示一种简单、易维护且没有不必要框架层次的架构；
- 可以构建、测试、签名，并通过 GitHub Releases 以 APK 形式分发。

本项目有意保持较小的范围。它不是身份平台、密码管理器、云同步服务、Passkey 提供程序或专有身份验证客户端。

## AI Agent 辅助开发

本项目在需求分析和实现过程中接受了 AI Agent 的指导。AI Agent 作为协作工具，用于拆解需求、评估架构和 API 选择、指导增量实现，以及审查代码和测试。

项目始终由人主导：项目范围、产品决策、安全权衡和仓库变更均由维护者审核。AI 提出的建议会结合相关标准、官方 Android API、源代码审查和自动化测试进行核验。

## 功能

- 存储和管理多个 TOTP 账户。
- 通过设置密钥、`otpauth://` URI 或二维码添加账户。
- 编辑账户信息和 TOTP 参数、更换密钥以及删除账户。
- 使用 SHA-1、SHA-256 或 SHA-512 生成 6 位或 8 位验证码。
- 支持自定义 TOTP 周期。
- 显示验证码的剩余有效时间，并将验证码复制到剪贴板。
- 在 TOTP 密钥存入本地 Room 数据库前对其加密。
- 使用强生物识别或设备屏幕锁保护应用访问。
- 完全在本地运行，不申请网络权限，也不依赖后端服务。

TOTP 实现基于 RFC 2104、RFC 4226、RFC 6238、RFC 4648、RFC 3986，以及常用的密钥 URI 格式（`otpauth://`）。密码学操作使用 JCA API，不自行实现密码学原语，也不依赖第三方 TOTP 运行时库。

## 架构

应用采用小型三模块架构，并使用显式依赖关系：

```text
Compose UI / ViewModel
          |
          v
      Repository
          |
          v
 Room / Android Keystore / DataStore
```

### `:core`

一个独立于平台的 Kotlin/JVM 模块，包含：

- Base32 编码和解码；
- HOTP 和 TOTP 生成；
- OTP 领域模型和校验；
- `otpauth://` URI 解析和格式化。

该模块不依赖 Android，并完全在 JVM 上进行测试，其中包括基于 RFC 测试向量的测试。

### `:data`

一个 Android Library 模块，包含：

- Room 3 数据库、实体和 DAO；
- TOTP 账户仓库；
- 基于 Android Keystore 的密钥保护；
- 基于 DataStore 的应用设置。

TOTP 密钥会先使用由 Android Keystore 生成并持有的 AES-GCM 密钥进行加密，再由 Room 持久化加密后的数据。

### `:app`

Android 应用模块，包含：

- Jetpack Compose 和 Material 3 UI；
- Navigation 3 路由；
- ViewModel 和生命周期集成；
- 使用 CameraX 和 ML Kit 扫描二维码；
- 使用 AndroidX Biometric 实现应用锁。

依赖项通过构造函数注入和一个小型应用级容器进行组装。本项目有意不使用依赖注入框架。

## 技术栈

- Kotlin 和 Gradle Kotlin DSL
- Jetpack Compose 和 Material 3
- Navigation 3
- Coroutines、Flow、StateFlow 和 ViewModel
- Room 3 和 DataStore
- Android Keystore 和 AndroidX Biometric
- CameraX 和 ML Kit 条码扫描
- JUnit、AndroidX Test 和 Compose 测试 API

最低支持 Android 12（`minSdk 31`）。当前项目配置使用 Android API 37 进行编译。

## 开始使用

### 环境要求

- 与项目所用 Android Gradle Plugin 兼容的当前稳定版 Android Studio
- Android Studio 内置的 Gradle JDK
- Android SDK Platform 37
- Android 12 或更高版本的设备或模拟器

项目已包含 Gradle Wrapper，无需单独安装 Gradle。

### 打开并运行项目

1. 克隆仓库，并在 Android Studio 中打开仓库根目录。
2. 让 Android Studio 安装缺少的 SDK 组件并完成 Gradle 同步。
3. 选择 `app` 运行配置。
4. 选择 Android 12 或更高版本的设备或模拟器，然后运行应用。

在 macOS 或 Linux 的终端中构建 Debug APK：

```bash
./gradlew assembleDebug
```

在 Windows PowerShell 中：

```powershell
.\gradlew.bat assembleDebug
```

APK 将输出到：

```text
app/build/outputs/apk/debug/app-debug.apk
```

也可以使用以下命令将 Debug 构建安装到已连接的设备：

```bash
./gradlew installDebug
```

从 Windows PowerShell 运行 Gradle 命令时，请使用 `.\gradlew.bat` 代替 `./gradlew`。

## 开发和测试

运行 JVM 单元测试：

```bash
./gradlew test
```

运行 Android Lint：

```bash
./gradlew lint
```

构建 Debug APK：

```bash
./gradlew assembleDebug
```

连接模拟器或设备后，运行 Instrumented Test 和 Compose 测试：

```bash
./gradlew connectedDebugAndroidTest
```

常规的本地和 CI 验证顺序为：

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

在重要版本发布前，还应在 Android 设备或模拟器上运行 Instrumented Test，并验证关键的账户管理、二维码扫描和应用锁流程。

## 安全说明

- TOTP 密钥在数据库持久化前，会使用 AES-GCM 和不可导出的 Android Keystore 密钥进行加密。
- Android 应用备份已禁用，避免加密数据与其设备绑定密钥分离。
- 应用仅为扫描二维码申请相机权限，不申请网络权限。
- 可选的应用锁支持强生物识别或已配置的设备凭据。
- 签名密钥和本地签名配置不得提交到仓库。

这是一个学习项目，尚未接受独立安全审计。在使用本应用保护重要账户前，请审查其实现并了解相关风险。

## 分发

请从 [GitHub Releases](https://github.com/origin-coding/totp-android/releases) 下载签名 APK。每个 Release 都包含 APK 及对应的 `.sha256` 校验文件，请在安装前验证 APK 的校验值。

持续集成会在推送和拉取请求时运行单元测试、Android Lint 和 Debug APK 构建。版本标签会触发单独的工作流，构建并验证签名 APK、生成 SHA-256 校验值，并通过 GitHub Releases 发布文件。

Release 签名材料保存在仓库之外。受信任的 Release 证书指纹和签名流程记录在 [Release signing](docs/release-signing.md) 中。

Google Play 分发和其他平台不在本项目范围内。

## 许可证

本项目基于 [MIT License](LICENSE) 发布。
