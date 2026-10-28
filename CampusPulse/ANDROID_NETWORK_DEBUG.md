# Android 网络联调记录（2026-10-26）

## 本次环境与目标

- Android：Pixel_7 模拟器，应用包名 `com.campuspulse.app`。
- 后端：本机 Spring Boot，端口 `8081`；MySQL 由后端连接。
- 目标：Android 调用已有注册、登录、活动、报名和兴趣接口，不新增后端接口。

## 报错与排查

1. 起初后端进程未运行。启动 MySQL 和 Spring Boot 后，在电脑上请求 `http://127.0.0.1:8081/api/v1/health`，得到 `status=UP`、`db=UP`。这只证明电脑能访问后端，不能证明模拟器里的应用能访问。
2. Android 清单加入了 `INTERNET` 权限；由于本地后端使用 HTTP，开发阶段设置了 `android:usesCleartextTraffic="true"`。仍出现“无法连接服务”。
3. 请求放在后台线程，异常写入 Logcat 的 `CampusPulseApi` 标签。日志显示应用连接 `10.0.2.2:8081` 时发生 `SocketTimeoutException`（6 秒超时）。模拟器内的 Chrome 和命令行请求同一地址可以得到 HTTP 200，因此能确定的是**应用这一路径上的 TCP 连接超时**；现有证据不足以把原因断言为防火墙或后端故障。
4. 改用 ADB 反向端口转发：`adb reverse tcp:8081 tcp:8081`。应用的 `ApiClient.BASE_URL` 随之设为 `http://127.0.0.1:8081`。这里的 `127.0.0.1` 是模拟器自身，ADB 将其 8081 端口转发到电脑的 8081 端口。重新安装 APK 后，活动列表、登录和写接口均正常。
5. 后续发现模拟器时区为 GMT，电脑/后端按 `Asia/Shanghai` 生成无时区的活动时间文本。客户端曾把已截止的活动误判为可报名；现已按 `Asia/Shanghai` 比较活动开始与报名截止时间。**最终能否报名仍以服务端校验为准。**

## 现在的配置与每次启动步骤

1. 启动 MySQL 与 Spring Boot。先在电脑上检查：`Invoke-RestMethod http://127.0.0.1:8081/api/v1/health`。
2. 启动 Android 模拟器，在 PowerShell 执行：

   ```powershell
   & 'C:\Users\Mony\AppData\Local\Android\Sdk\platform-tools\adb.exe' reverse tcp:8081 tcp:8081
   & 'C:\Users\Mony\AppData\Local\Android\Sdk\platform-tools\adb.exe' reverse --list
   ```

3. 在 Android Studio 运行应用。若仍显示无法连接，依次检查后端健康接口、`adb devices`、`adb reverse --list`，再看 Logcat 中的 `CampusPulseApi` 异常。模拟器重启或 ADB 连接重建后，应重新执行 `adb reverse`。

Android 侧关键配置位于 `app/src/main/AndroidManifest.xml`（网络权限、本地 HTTP）和 `app/src/main/java/com/campuspulse/app/ApiClient.java`（根地址、请求、统一响应与错误处理）。`10.0.2.2` 是模拟器访问电脑的常见地址，但不是本项目当前采用的工作路径。真机若通过 ADB 连接，也可使用反向转发；脱离 ADB 独立运行时必须改为设备可访问的服务地址。课程设计之外的正式部署应使用 HTTPS，而不是保留全局明文 HTTP。

## 验证结果

- `:app:assembleDebug` 构建成功；Android 首页实际读取到后端活动。
- 已有测试用户 `student01` 登录成功，`/api/v1/users/me` 返回账号信息。
- Android 端注册演示用户 `androiddemo1026a` 成功，注册后自动登录；兴趣标签从后端读取并保存。该演示账号留在数据库中，便于答辩时复测。
- 对活动 5“图书馆志愿整理活动”完成报名后，详情页剩余名额由 18 变为 17；随后取消，恢复为 18。
- 推荐接口尚未开发，首页明确显示占位说明，不伪造后端推荐结果。
