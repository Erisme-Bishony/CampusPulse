# CampusPulse 服务端联调说明（给 A）

更新时间：2026-10-27 17:31
服务端分支：`2026.10.21.B`
服务端代码：`CampusPulseServer/`
数据库脚本：`CampusPulseDatabase/`
机器可读契约：`docs/openapi.json`
Apifox 导入：`docs/Apifox使用说明.md`

---

## 1. 当前可用状态

- 服务端：Spring Boot 4.1.1 + Java 17 + MySQL 8，Maven 项目。
- 本机端口：**8081**（8080 暂时不用，避免和旧进程冲突）。
- 健康检查已通：

```http
GET http://localhost:8081/api/v1/health
```

```json
{"status":"UP","db":"UP","service":"CampusPulseServer","timestamp":"2026-10-21 18:21:52"}
```

- 已完成：注册、登录、退出、当前用户、修改资料、兴趣标签读写；标签、活动列表/详情/搜索筛选；报名/取消、报名状态、我的报名；推荐、行为采集。
- 管理后台已完成：`http://127.0.0.1:8081/admin/login`，账号 `admin / admin123`；后台发布活动后 App 列表可见。A 不需要写后台页面，详见 `docs/管理员后台说明.md`。
- 下一步：统计接口、最后联调与报告。

### 2026-10-27 对 A 两次 Android 更新的确认

- 已拉到 `origin/Erisme/android-dev` 的两次提交：
  - `1b89171 feat(android): 完成首页静态展示与活动列表`
  - `bccb9b2 feat(android): 完成认证与活动联调功能`
- A 的记录里用 **ADB 反向端口转发** 解决模拟器连不上 `10.0.2.2` 的问题：
  ```powershell
  adb reverse tcp:8081 tcp:8081
  ```
  此时 Android 的 `BASE_URL` 用 `http://127.0.0.1:8081` 是合理的，B 不要求改回 `10.0.2.2`。
- A 已实测：首页活动列表、`student01` 登录、`/users/me`、注册、兴趣标签、活动 5 报名/取消。服务端字段与现有实现一致，暂不需要 A 改接口。
- 推荐接口已实现，见下一节；A 首页可以把占位换成真实推荐。管理员后台也不属于 A 的范围。

### 2026-10-27 B-09 推荐与行为采集已完成

新增两个接口，都需要 `Authorization: Bearer <token>`：

| 方法 | 路径 | 用途 |
|---|---|---|
| `GET` | `/api/v1/recommendations?limit=10` | 个性化推荐，`limit` 1–20，默认 10 |
| `POST` | `/api/v1/behaviors` | 记录浏览、分享、停留时长 |

推荐返回示例：

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "activity": {
        "id": 5,
        "title": "校园歌手大赛决赛",
        "type": "文艺",
        "location": "大学生活动中心",
        "startTime": "2026-11-06 19:00:00",
        "endTime": "2026-11-06 21:30:00",
        "signupDeadline": "2026-11-05 19:00:00",
        "capacity": 200,
        "signupCount": 18,
        "remaining": 182,
        "status": "PUBLISHED",
        "tags": []
      },
      "score": 0.39,
      "reason": "近期热门"
    }
  ],
  "timestamp": "2026-10-27 17:31:00"
}
```

推荐规则（A 不需要实现，只看结果）：

- 有标签：0.7 × 标签内容相似度 + 0.3 × 热度；无标签冷启动只用热度。
- 过滤已报名、已结束、非 `PUBLISHED` 的活动；每次请求重算，最多返回 20 条。
- 每条带 `reason`，首页直接展示，不要自己编推荐理由。

行为请求示例：

```json
{"activityId": 5, "type": "VIEW"}
{"activityId": 5, "type": "SHARE"}
{"activityId": 5, "type": "DURATION", "durationSeconds": 32}
```

- `VIEW` / `SHARE` 会原子累加活动表的浏览/分享计数。
- `DURATION` 必须带大于 0 的 `durationSeconds`，只记录停留时长。
- 建议：进入详情页发 `VIEW`，点分享按钮发 `SHARE`，离开详情页时发 `DURATION`；失败不要阻塞页面。

测试与数据：服务端全量 36 个用例通过；真实 MySQL 用 `student01` 验证过推荐 5 条、浏览/分享计数累加，测试行为已清理，种子数据仍是 12 场活动、22 条报名。

### Android 模拟器怎么连

```text
http://10.0.2.2:8081/
```

Retrofit 的 `baseUrl` 要以 `/` 结尾。

真机联调时换成电脑局域网 IP：

```text
http://<电脑局域网IP>:8081/
```

> 如果 B 后面把端口改回 8080，会提前在群里说；A 的 `baseUrl` 最好放在 `BuildConfig` 或常量里，方便改。

---

## 2. 通用约定

### 2.1 统一响应

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "timestamp": "2026-10-21 18:21:52"
}
```

- `code = 0` 表示成功。
- 失败时 `data` 为 `null`，`message` 是可直接展示给用户的中文。
- HTTP 状态码也会同步设置，Android 端可以先看 `code`，再看 `message`。

### 2.2 认证

登录或注册成功后返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 7,
      "username": "student07",
      "email": "s7@example.com",
      "phone": "13800000007",
      "role": "STUDENT",
      "tagIds": [1, 2]
    }
  },
  "timestamp": "2026-10-21 18:21:52"
}
```

后续需要登录的接口加请求头：

```http
Authorization: Bearer <token>
```

Token 有效期 7 天。退出接口只做形式上的成功返回，客户端删除本地 token 即可。

### 2.3 时间与分页

- 时间格式：`yyyy-MM-dd HH:mm:ss`
- 时区：`Asia/Shanghai`
- 分页参数从 `1` 开始；默认 `page=1`、`size=10`，`size` 最大 50。

---

## 3. 已实现接口

### 3.1 注册

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "username": "student07",
  "password": "123456",
  "confirmPassword": "123456",
  "email": "s7@example.com",
  "phone": "13800000007",
  "tagIds": [1, 2]
}
```

字段约束：

- `username`：3–50 字符，唯一。
- `password`：6–64 字符。
- `confirmPassword`：必须与 `password` 一致。
- `email`：可空，格式校验。
- `phone`：可空，11 位手机号。
- `tagIds`：可空数组，元素是标签 ID。

成功返回同登录响应（含 token 和 user）。

常见错误：

- `40901`：用户名已存在。
- `40001`：参数校验失败，`message` 会指出具体字段。

### 3.2 登录

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "username": "student01",
  "password": "123456"
}
```

成功返回 token 和 user；密码错误返回 `40102`。

### 3.3 退出

```http
POST /api/v1/auth/logout
Authorization: Bearer <token>
```

返回 `data: null`。客户端删除 token 即可。

### 3.4 当前用户

```http
GET /api/v1/users/me
Authorization: Bearer <token>
```

`data` 为 `UserResponse`：

```json
{
  "id": 7,
  "username": "student07",
  "email": "s7@example.com",
  "phone": "13800000007",
  "role": "STUDENT",
  "tagIds": [1, 2]
}
```

### 3.5 修改资料

```http
PUT /api/v1/users/me
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "email": "new@example.com",
  "phone": "13900000007"
}
```

两个字段都可空；返回更新后的 `UserResponse`。

### 3.6 读取兴趣标签

```http
GET /api/v1/users/me/interests
Authorization: Bearer <token>
```

`data` 为标签 ID 数组：

```json
[1, 2]
```

### 3.7 更新兴趣标签

```http
PUT /api/v1/users/me/interests
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "tagIds": [1, 2, 5]
}
```

会整体替换原有关注标签，返回更新后的 `UserResponse`。

---

## 4. 活动与标签接口（10-21 已实现，字段冻结）

> 以下字段已在真实 MySQL 上验证；如果后续必须调整，B 会先通知 A，不单方面改。

### 4.1 标签列表

```http
GET /api/v1/tags
```

`data`：

```json
[
  {"id": 1, "name": "学术讲座", "color": "#4A90D9"},
  {"id": 2, "name": "文艺", "color": "#E67E22"}
]
```

### 4.2 活动列表 / 搜索 / 筛选

```http
GET /api/v1/activities?page=1&size=10&keyword=讲座&tagId=1&type=讲座&date=2026-10-25
```

参数都可选：

| 参数 | 说明 |
|---|---|
| `page` | 页码，从 1 开始 |
| `size` | 每页数量，最大 50 |
| `keyword` | 标题/描述/地点/类型关键词；中文至少 2 个字 |
| `tagId` | 按标签筛选 |
| `type` | 活动类型中文名，如 `讲座`、`比赛`、`招新`、`演出`；当前数据里还有 `志愿`、`沙龙`、`宣讲`、`工作坊`、`市集` |
| `date` | 活动开始日期，格式 `yyyy-MM-dd` |

`data`：

```json
{
  "items": [
    {
      "id": 1,
      "title": "人工智能前沿讲座",
      "type": "讲座",
      "location": "图书馆报告厅",
      "startTime": "2026-10-25 19:00:00",
      "endTime": "2026-10-25 21:00:00",
      "signupDeadline": "2026-10-24 18:00:00",
      "capacity": 100,
      "signupCount": 36,
      "remaining": 64,
      "coverUrl": "https://example.com/cover.jpg",
      "status": "PUBLISHED",
      "tags": [
        {"id": 1, "name": "学术讲座", "color": "#4A90D9"}
      ]
    }
  ],
  "total": 12,
  "page": 1,
  "size": 10,
  "totalPages": 2
}
```

说明：

- `remaining = capacity - signupCount`，不会小于 0。
- `coverUrl` 可能为 `null`，Android 端要准备占位图。
- 列表只返回 `PUBLISHED` 的活动；管理后台另用管理接口。

### 4.3 活动详情

```http
GET /api/v1/activities/{id}
```

`data` 在列表字段基础上增加：

```json
{
  "description": "活动详细介绍……",
  "organizer": "计算机学院学生会",
  "viewCount": 128,
  "shareCount": 9,
  "createdAt": "2026-10-20 10:00:00",
  "updatedAt": "2026-10-20 10:00:00"
}
```

不存在返回 `40401`。

### 4.4 报名

```http
POST /api/v1/activities/{id}/registrations
Authorization: Bearer <token>
```

无请求体。

成功 `data`：

```json
{
  "id": 23,
  "activityId": 1,
  "status": "REGISTERED",
  "createdAt": "2026-10-21 18:30:00"
}
```

失败：

- `40902`：已报名，不能重复报名。
- `40903`：名额已满。
- `40904`：报名已截止或活动已结束。
- `40401`：活动不存在。
- `40101`：未登录或 token 过期。

### 4.5 取消报名

```http
DELETE /api/v1/activities/{id}/registrations
Authorization: Bearer <token>
```

成功返回 `data: null`。

### 4.6 我的报名状态

```http
GET /api/v1/activities/{id}/registration-status
Authorization: Bearer <token>
```

`data`：

```json
{
  "registered": true,
  "status": "REGISTERED"
}
```

`status` 可能是 `REGISTERED`、`CANCELLED`、`NONE`。

### 4.7 我的报名列表

```http
GET /api/v1/users/me/registrations
Authorization: Bearer <token>
```

`data`：

```json
[
  {
    "id": 23,
    "status": "REGISTERED",
    "createdAt": "2026-10-21 18:30:00",
    "activity": {
      "id": 1,
      "title": "人工智能前沿讲座",
      "location": "图书馆报告厅",
      "startTime": "2026-10-25 19:00:00",
      "endTime": "2026-10-25 21:00:00",
      "status": "PUBLISHED"
    }
  }
]
```

说明：返回该用户全部报名记录，包含 `CANCELLED` 历史；Android 的“我的报名”如果只想显示有效报名，按 `status=REGISTERED` 过滤即可。

---

## 5. 错误码

| code | HTTP | 含义 |
|---|---|---|
| 40001 | 400 | 参数校验失败 |
| 40101 | 401 | 未登录或登录已过期 |
| 40102 | 401 | 用户名或密码错误 |
| 40301 | 403 | 无权限访问 |
| 40401 | 404 | 资源不存在 |
| 40901 | 409 | 用户名已存在 |
| 40902 | 409 | 已报名，不能重复报名 |
| 40903 | 409 | 活动名额已满 |
| 40904 | 409 | 报名已截止或活动已结束 |
| 50000 | 500 | 服务器内部错误 |

Android 端建议统一处理：

- `40101`：清除 token，跳登录页。
- 其他错误：直接展示后端返回的 `message`，不要自己编原因。

---

## 6. 测试账号

学生账号（密码都是 `123456`）：

```text
student01
student02
student03
student04
student05
student06
```

管理员账号（后台用，Android 端不需要）：

```text
admin / admin123
```

活动 3 是“满额”样例（5/5），适合测试 `40903`。

---

## 7. 建议的联调顺序

1. 配好 `baseUrl`，先调 `/api/v1/health`，确认模拟器能访问 `10.0.2.2:8081`。
2. 用 `student01/123456` 调登录，保存 token。
3. 调 `/api/v1/users/me`，确认请求头生效。
4. 标签列表 → 活动列表 → 活动详情。
5. 活动详情页调报名 → 我的报名 → 取消 → 再报名。
6. 分别测：密码错误、重复报名、满额活动、token 过期/不传 token。
7. 把 Retrofit 请求/响应日志或截图发给 B，联调问题当天修。

---

## 8. 需要 A 确认或提供

1. `10.0.2.2:8081` 这个地址是否可用；如果 A 的本机模拟器有特殊网络配置，提前说。
2. 活动列表卡片需要哪些字段？当前准备返回：标题、类型、地点、开始时间、剩余名额、封面、标签。
3. 活动详情页是否需要“已报名/报名状态”直接放在详情响应里？目前设计是单独调 `/registration-status`，如果 A 觉得多一次请求，可以改成登录时详情带状态。
4. 日期筛选是否要按“活动开始日期”精确匹配？当前按 `date` 当天筛选。
5. token 存储方式：建议 `SharedPreferences`，A 确认后按这个写。
6. A 的 Android 分支名、当前进度、预计什么时候可以开始联调。
7. A 的姓名、学号，用于报告封面和提交文件夹命名。

---

## 9. B 接下来 48 小时

- 10-21 晚：标签接口、活动列表/详情、搜索筛选，补 MockMvc 测试。
- 10-22：报名/取消/我的报名，事务和并发测试；接口联调。
- 10-23：修联调问题；补 `docs/openapi.json` 和 Apifox 导入说明（不再依赖 Postman）。
- 10-24 起：推荐、管理后台、行为统计；同时推进报告。

> 10-22 更新：报名接口已完成，MockMvc 覆盖报名、重复报名、满额、截止、取消、重新报名、未登录、我的报名、并发抢名额；真实 MySQL 也已走通。A 可以直接按第 4 节接。
