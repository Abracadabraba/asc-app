# ASC · 辅助性自律 (Assistive Self-Control)

一个安卓自律锁机 App：为指定的 App 设置每天的"禁用时间段"，在该时间段内打开目标 App 会被强制拦截，
必须输入密码才能继续；密码只以哈希形式保存，App 本身无法显示或找回明文。

## 安全设计说明

- 设置密码后，只有你自己知道明文（写在纸上、告诉信任的人，或存在别处），App 数据库里只存哈希值。
- 连续输错 5 次密码后，会进入 **24 小时冷静期**，此期间无法再尝试。
- 如果彻底忘记密码，可以在锁屏页点"忘记密码"发起申请，等待 24 小时后回到 App 主界面重置密码。
  这个设计是为了避免"永久锁死手机"的极端情况，同时依然能起到强约束作用。
- 无障碍服务权限用于检测前台应用切换，App 不会读取你的屏幕内容或输入的其它文字。

## 功能

1. 从已安装的可启动应用中选择要限制的 App
2. 为其设置每日的禁用时间段（支持跨午夜，如 22:00–07:00）
3. 设置一次性密码（无法被 App 自身找回明文）
4. 在限制时段内打开目标 App 会自动跳转锁屏页，要求输入密码
5. 密码正确后有 10 分钟宽限期，方便你手动重新打开该 App

## 本地项目结构

```
ASC/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/asc/selfcontrol/
│       │   ├── MainActivity.kt              主界面：设置密码、规则
│       │   ├── LockOverlayActivity.kt        锁屏页
│       │   ├── AppLockAccessibilityService.kt 后台监控前台 App
│       │   ├── data/Rule.kt                  规则数据结构
│       │   ├── data/Prefs.kt                 本地存储封装
│       │   └── util/PasswordUtil.kt          密码哈希工具
│       └── res/...
├── .github/workflows/build.yml               GitHub Actions 自动打包
└── build.gradle / settings.gradle / gradle.properties
```

## 用 GitHub Actions 打包 APK（不需要本地装 Android Studio）

### 第 1 步：创建 GitHub 仓库

1. 登录 [github.com](https://github.com)，点右上角 **+ → New repository**
2. 仓库名随意，比如 `asc-app`，选择 Public 或 Private 都可以，点 **Create repository**

### 第 2 步：上传本项目代码

在你电脑上解压这个 zip 后，进入项目文件夹，执行：

```bash
cd ASC
git init
git add .
git commit -m "init ASC project"
git branch -M main
git remote add origin https://github.com/你的用户名/asc-app.git
git push -u origin main
```

（如果你不熟悉命令行，也可以直接在 GitHub 网页上点 **Add file → Upload files**，
把解压后的所有文件和文件夹拖进去上传，注意要保留原有的文件夹结构。）

### 第 3 步：等待自动构建

代码推送后，GitHub 会自动触发 `.github/workflows/build.yml` 里定义的构建流程：

1. 打开你的仓库页面，点顶部 **Actions** 标签
2. 你会看到一个名为 "Build APK" 的工作流正在运行（黄色圆点 = 进行中，绿色勾 = 成功）
3. 大约 3–6 分钟后构建完成，点进这次运行记录
4. 页面底部 **Artifacts** 区域会出现 `ASC-debug-apk`，点击下载，会得到一个 zip，
   解压后就是 `app-debug.apk`

### 第 4 步：安装到手机

1. 把 `app-debug.apk` 传到手机（微信传输助手、USB、网盘等方式都可以）
2. 在手机上点击这个 apk 文件安装
3. 如果提示"未知来源应用"被拦截，去系统设置里允许该来源安装，然后重新点击安装

### 第 5 步：授权与设置

1. 打开 ASC，先设置密码（设置后立刻抄写下来保管好，App 不会再显示）
2. 点击"开启无障碍服务权限"，在系统设置里找到 ASC，打开开关
3. 回到 ASC，添加你要限制的 App 和时间段即可生效

## 小米澎湃OS (HyperOS) 专属设置 —— 非常重要

HyperOS（以及之前的 MIUI）对无障碍服务和后台常驻管得比原生安卓严格很多，
如果跳过下面的设置，很可能出现"刚设置完能用，过一会儿或重启后就失效"的情况。
App 主界面新增了三个跳转按钮，对应下面三步，请务必依次点开确认：

1. **无障碍权限**：点"开启无障碍服务权限" → 在列表里找到 ASC → 打开开关。
   HyperOS 通常会弹出二次安全提示（"该服务可能会读取你的屏幕内容"之类），
   这是系统通用文案，我们的服务只读取当前在前台运行的 App 包名，不会读取你输入的密码或其它内容，放心继续点"允许"。
2. **自启动权限**：点"开启自启动权限" → 找到 ASC → 打开自启动开关。
   不开的话，手机重启或后台清理后 ASC 的服务可能不会自动恢复。
3. **电池无限制**：点"关闭省电限制（电池无限制）" → 找到 ASC → 选择"无限制"，
   而不是"智能省电"或"超级省电"。否则 HyperOS 可能会在后台把 ASC 的进程杀掉，
   导致限制时段到了却没有拦截。

如果以上三个按钮在你的机型上跳转失败（不同 HyperOS 版本菜单位置略有差异），
点"打开应用详情设置"进入 ASC 的应用详情页，从那里手动找入口，大致路径是：

- 自启动：设置 → 应用设置 → 应用管理 → ASC → 自启动
- 电池策略：设置 → 电池与性能 → 应用配置 → ASC → 无限制
- 无障碍：设置 → 无障碍 → 已下载的服务 → ASC

## 关于图标

项目里已经内置了一枚原创图标（靛蓝渐变圆底 + 白色锁形 + 内嵌表盘，呼应"限时锁定"），
包含标准分辨率图标和 Android 8.0+ 自适应图标（`mipmap-anydpi-v26`），
打包出来的 APK 会正常显示图标，不需要额外处理。如果想换成你自己的设计，
替换 `app/src/main/res/mipmap-*/ic_launcher.png`、`ic_launcher_round.png`
以及 `app/src/main/res/drawable/ic_launcher_foreground.png` 这几个文件即可（保持文件名不变）。

## 已知限制（原型版本，供你后续迭代）

- 锁屏页是普通 Activity 而非系统级悬浮窗，理论上按 Home 键可以退出到桌面，
  但目标 App 已经被切走，重新打开仍会再次触发拦截。如果想要更强的防护，
  后续可以加上 Device Admin / 防卸载、开机自启动、以及监听无障碍服务被关闭时的提醒。
- 部分国产手机（小米/华为/OPPO/vivo 等）对无障碍服务和后台保活有额外限制，
  小米澎湃OS/MIUI 的具体设置步骤见上方专属章节；其它品牌思路类似，
  核心是找到"自启动"和"电池优化白名单（无限制）"两个开关。
- 目前重置密码走 24 小时冷静期，没有做云端找回，纯本地实现。
