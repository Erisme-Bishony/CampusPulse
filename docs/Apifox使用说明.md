# 用 Apifox 调试 CampusPulse 服务端

没有 Postman 不影响联调。项目已提供 OpenAPI 3.0 契约：

- `docs/openapi.json`：机器可读的接口定义，直接导入 Apifox。
- `docs/给A的联调同步.md`：字段说明、错误码和联调顺序。

## 1. 导入

1. 打开 Apifox，进入 CampusPulse 项目（没有就新建一个）。
2. 左侧「项目设置」→「导入数据」→ 选择「OpenAPI / Swagger」。
3. 选择文件 `docs/openapi.json`，格式选 OpenAPI 3.0，导入。
4. 导入后把环境里的 `baseUrl` 设为 `http://127.0.0.1:8081`。
   - Apifox 和 IDEA 在同一台电脑上用这个地址。
   - `http://10.0.2.2:8081` 只给 Android 模拟器里的 App 用，Apifox 不要用。
5. 如果服务不是 8081，以 IDEA 控制台实际端口为准，只改 `baseUrl`。

## 2. 环境变量

在 Apifox 的「环境管理」里建两个变量：

| 变量 | 值 |
| --- | --- |
| `baseUrl` | `http://127.0.0.1:8081` |
| `token` | 先留空，登录后自动写入 |

需要登录的接口统一在目录或项目级别设置鉴权：类型 `Bearer Token`，值 `{{token}}`。这样不用每个请求手写请求头。

## 3. 先跑通登录

1. 打开 `POST /api/v1/auth/login`，body 用：

```json
{"username": "student01", "password": "123456"}
```

2. 发送后应返回 `code: 0`，`data.token` 是一串 JWT。
3. 在 Apifox 该请求的「后置操作」里加一个「提取变量」：
   - 来源：响应 JSON
   - 表达式：`$.data.token`
   - 写入环境变量：`token`
4. 之后再点受保护接口，Apifox 会自动带上 `Authorization: Bearer {{token}}`。

登录失败时先看 `code`：`40102` 是密码错，`40101` 是 token 缺失或失效，不是接口 404。

## 4. 建议的联调顺序

| 顺序 | 请求 | 说明 |
| --- | --- | --- |
| 1 | `GET /api/v1/health` | 确认 `status=UP`、`db=UP` |
| 2 | `POST /api/v1/auth/login` | 拿 token，写入环境变量 |
| 3 | `GET /api/v1/users/me` | 确认鉴权头生效 |
| 4 | `GET /api/v1/tags` | 拿标签 ID，当前 8 个 |
| 5 | `GET /api/v1/activities?page=1&size=5` | 当前 12 场活动 |
| 6 | `GET /api/v1/activities?keyword=讲座` | 中文关键词检索 |
| 7 | `GET /api/v1/activities/1` | 活动详情 |
| 8 | `GET /api/v1/activities/1/registration-status` | 未报名时返回 `status=NONE` |
| 9 | `POST /api/v1/activities/1/registrations` | 报名 |
| 10 | `GET /api/v1/users/me/registrations` | 我的报名 |
| 11 | `DELETE /api/v1/activities/1/registrations` | 取消；再查状态应回到 `NONE` |

活动 3 是满额样例（`signup_count=5`、`capacity=5`），适合验证 `40903`。不要拿它反复报名。

## 5. 测试账号

| 账号 | 密码 | 用途 |
| --- | --- | --- |
| `student01` | `123456` | 普通学生，已有报名数据 |
| `student02` | `123456` | 普通学生 |
| `student06` | `123456` | 可用于报名/取消联调 |
| `admin` | `admin123` | 管理后台账号，普通 App 接口不使用 |

注册时 `confirmPassword` 必填，且必须和 `password` 一致；用户名重复返回 `40901`。

## 6. 和 Postman 的差别

- Apifox 能直接导入 OpenAPI，Postman 也能导入同一份 `openapi.json`；以后如果需要，可以从 Apifox 导出 Postman 集合，不用单独维护两套。
- 当前没有把 OpenAPI 挂在服务端 `/v3/api-docs`，所以用「导入文件」而不是「URL 导入」。以后接入 springdoc 再改成 URL 同步。
- Apifox 的「目录鉴权」只对新建请求生效；导入后请检查受保护接口是否都带上了 Bearer。

接口实现和 `docs/openapi.json` 不一致时，以代码实际返回为准，并同步修契约；不要为了让 Apifox 通过而改错服务端。
