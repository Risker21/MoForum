# MoForum — 贴吧风格社区论坛

![Java](https://img.shields.io/badge/Java-17-blue?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.9-green?logo=spring)
![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vue.js)
![Element Plus](https://img.shields.io/badge/Element_Plus-2.9-409EFF?logo=element)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-25A3F0)
![ECharts](https://img.shields.io/badge/ECharts-5.6-FB7299)
![License](https://img.shields.io/badge/License-GPLv3-blue)

**MoForum** 是一个基于 Spring Boot + Vue 3 的全栈论坛社区项目，采用"贴吧"模式。支持按主题分区浏览、发帖盖楼回复、Mo 号身份系统、关注好友、实时私聊等核心功能。前端采用 **"Ink & Paper"** 暖色主题设计（`#b52b2b` 朱红主色），追求精致阅读体验。

---

## 技术栈

### 后端 (`moforum/`)

| 技术 | 版本 |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.9 |
| Spring Security | 7.x |
| Spring WebSocket | — |
| MyBatis | 3.x |
| MySQL | 8.0+ |
| Redis | 7.0+ |
| JWT (jjwt) | 0.12.6 |
| Maven | 3.9+ |
| 阿里云 OSS | — |

### 前端 (`moforum-web/`)

| 技术 | 版本 |
|---|---|
| Vue 3 (Composition API) | 3.5 |
| Element Plus | 2.9 |
| Pinia | 2.3 |
| Vue Router | 4.5 |
| Axios | 1.7 |
| Vite | 6.4 |
| TypeScript | 5.7 |
| STOMP.js + SockJS | — |
| ECharts | 5.6 |

---

## 前置要求

- JDK 17+（确保 `JAVA_HOME` 指向 JDK 17）
- MySQL 8+
- Redis 7+（默认 `localhost:6379`）
- Node.js 18+

## 快速启动

### 1. 数据库初始化

```sql
mysql -u root -p < moforum/sql/init.sql
```

脚本会自动建库、建表并插入 4 个板块。测试用户、帖子、回复等数据由后端首次启动时自动生成。

### 2. 启动 Redis

```bash
redis-server
```

### 3. 启动后端

```bash
cd moforum
mvn spring-boot:run
# 运行在 http://localhost:8080
```

首次启动前复制环境变量模板并填入实际值：

```bash
cp moforum/.env.example moforum/.env
```

编辑 `moforum/.env`，填入你的数据库密码和 OSS 密钥（参考下方示例）。

`.env` 文件内容示例：
```bash
DB_PASSWORD=your_mysql_password
JWT_SECRET=your_random_jwt_secret_string
OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
OSS_ACCESS_KEY=your_oss_access_key
OSS_SECRET_KEY=your_oss_secret_key
OSS_BUCKET=moforum
```

> `.env` 文件已加入 `.gitignore`，不会提交到 GitHub。也支持通过同名系统环境变量覆盖（优先级更高）。

### 4. 启动前端

```bash
cd moforum-web
npm install
npm run dev
# 运行在 http://localhost:5173
```

---

## 示例数据

后端首次启动时，`DataInitializer` 自动检测并生成示例数据：

| 类型 | 内容 |
|---|---|
| **板块** | 综合吧 🗨️、游戏吧 🎮、学习吧 📚、生活吧 ☕ |
| **测试用户** | `alice` / `bob` / `admin`（密码均为 `123456`） |
| **帖子** | 8 篇分布于各板块 |
| **回复** | 26 条楼层回复 |
| **关注** | alice → bob, alice → admin |
| **好友** | alice ↔ bob（已互为好友） |
| **私信** | 3 条示例消息 |

---

## 项目结构

```
MoForum/
├── moforum/                          # 后端 (Spring Boot + MyBatis)
│   ├── sql/                          # 数据库初始化脚本
│   ├── src/main/java/com/moforum/
│   │   ├── config/                   # 配置：Security / JWT / WebSocket / Redis / OSS
│   │   ├── controller/               # 控制器层 (7 个)
│   │   ├── service/                  # 服务层
│   │   ├── mapper/                   # MyBatis Mapper 接口
│   │   └── entity/                   # 实体类
│   └── src/main/resources/
│       ├── application.yaml          # 主配置（含 OSS、MySQL、Redis 默认值）
│       └── mapper/                   # MyBatis XML 映射
│
└── moforum-web/                      # 前端 (Vue 3 + Element Plus)
    ├── public/
    │   └── favicon.svg               # 主题色网站图标
    ├── src/
    │   ├── api/                      # API 请求层（8 个模块）
    │   ├── composables/              # WebSocket 组合式函数
    │   ├── layouts/                  # 布局组件（MainLayout / AuthLayout）
    │   ├── router/                   # 路由配置（含登录守卫）
    │   ├── stores/                   # Pinia 状态管理（user / notification）
    │   ├── styles/                   # 全局 CSS 变量与主题
    │   └── views/                    # 页面视图（15 个）
    └── vite.config.ts                # Vite 配置（含 API 代理 + SPA fallback）
```

## 认证流程

```
┌─────────┐          ┌──────────┐          ┌─────────┐
│  前端    │  POST     │  后端     │  Redis   │  MySQL  │
│          │ ────────→ │          │ ───────→ │         │
│ 登录/注册│  ←──────── │ 生成 JWT │ ←─────── │         │
│          │  token    │          │ 缓存板块 │         │
└────┬────┘           └──────────┘         └─────────┘
     │ 每次请求携带
     │ Authorization: Bearer <token>
     ▼
┌─────────┐
│JwtAuth  │ ──→ 校验签名 → 黑名单检查 → SecurityContext
│ Filter   │
└─────────┘
```

---

## 数据库表结构

| 表名 | 说明 | 核心字段 |
|---|---|---|
| `t_user` | 用户 | id, username, password (BCrypt), user_no (Mo号), avatar_url |
| `t_board` | 贴吧板块 | id, name, description, avatar, post_count |
| `t_post` | 帖子 | id, user_id, board_id, title, content, view_count, reply_count |
| `t_reply` | 回复 | id, post_id, user_id, content, floor |
| `t_follow` | 关注 | follower_id, followed_id (唯一约束) |
| `t_friend_request` | 好友申请 | from_id, to_id, status |
| `t_friend` | 好友关系 | user_id_1, user_id_2 (id1 < id2) |
| `t_message` | 私信 | from_id, to_id, content, read, create_time |

---

## 接口概览

### 用户认证 & 资料

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| POST | `/user/register` | ❌ | 注册 + 返回 JWT |
| POST | `/user/login` | ❌ | 登录（用户名或 Mo 号） |
| POST | `/user/logout` | ✅ | 登出（Token 加入 Redis 黑名单） |
| GET | `/user/getById` | ❌ | 根据 ID 查询用户 |
| GET | `/user/getByUserNo` | ❌ | 根据 Mo 号查询用户 |
| POST | `/user/updateProfile` | ✅ | 更新个人资料（昵称、头像、简介） |
| POST | `/user/uploadAvatar` | ✅ | 上传头像（OSS） |
| GET | `/user/profile` | ✅ | 获取个人完整资料 |

### 板块 & 帖子 & 回复

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| GET | `/board/list` | ❌ | 板块列表（Redis 缓存 1h） |
| GET | `/board/detail` | ❌ | 板块详情 |
| POST | `/post/create` | ✅ | 发帖 |
| GET | `/post/list` | ❌ | 帖子列表（按板块或用户分页） |
| GET | `/post/latest` | ❌ | 全站最新帖子 |
| GET | `/post/detail` | ❌ | 帖子详情（自动增加阅读数） |
| POST | `/post/delete` | ✅ | 删除帖子（仅作者） |
| POST | `/reply/create` | ✅ | 楼层回复 |
| GET | `/reply/list` | ❌ | 回复列表（按帖子分页） |

### 搜索

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| GET | `/search` | ❌ | 全文搜索 `?q=&type=post/board/user/all` |

### 关注 & 好友

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| POST | `/follow/toggle` | ✅ | 切换关注/取关 |
| GET | `/follow/followers` | ❌ | 粉丝列表 |
| GET | `/follow/following` | ❌ | 关注列表 |
| POST | `/friend/request` | ✅ | 发送好友申请 |
| POST | `/friend/respond` | ✅ | 同意/拒绝好友申请 |
| GET | `/friend/list` | ✅ | 好友列表 |
| GET | `/friend/pending-count` | ✅ | 待处理申请数 |

### 私信 (WebSocket 实时推送)

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| POST | `/message/send` | ✅ | 发送消息 + WebSocket 推送 |
| GET | `/message/conversations` | ✅ | 会话列表（含未读计数 + 头像） |
| GET | `/message/list` | ✅ | 聊天记录（分页） |
| POST | `/message/read` | ✅ | 标记已读 |

### 管理后台

| 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|
| GET | `/admin/stats` | ✅ (admin) | 系统统计（含图表数据） |
| POST | `/post/delete` | ✅ (admin) | 管理员删帖 |

---

## 核心特性

- **Ink & Paper 主题** — 暖色朱红 + 米白配色，圆角 16px 卡片，毛玻璃导航，Playfair Display + Noto Serif SC 字体搭配
- **Mo 号系统** — 注册自动分配 10 位数字 Mo 号（类似 QQ 号），支持 Mo 号登录
- **JWT 认证** — 签名 Token + Redis 黑名单，Axios 拦截器自动携带
- **Redis 缓存** — 板块列表缓存（1h TTL），Token 黑名单自动过期
- **BCrypt 加密** — 密码不存明文，Spring Security `matches()` 比对
- **贴吧模式** — 按主题分区，每区独立计数，支持发帖盖楼
- **实时私信** — STOMP over SockJS WebSocket，聊天页面支持 Enter 发送 / Shift+Enter 换行 / 图片上传 / 表情选择
- **OSS 图片上传** — 阿里云 OSS 存储，支持 3MB 以内图片，前端 Markdown 自动插入
- **自定义头像** — 用户在个人主页上传头像，导航栏即时同步更新
- **搜索系统** — 帖子/板块/用户多类型模糊搜索
- **关注 & 好友系统** — 单向关注 + 双向好友（申请/同意/拒绝全流程）
- **实时通知** — 导航栏红点提示未读消息和好友申请
- **管理后台** — 7 日发帖趋势折线图 / 板块分布饼图 / 发帖排行柱状图（ECharts）

---

## 前端 UI 说明

- 全局 CSS 变量基于 `--mf-primary: #b52b2b`，`mf-card`/`mf-fade-in` 等原子类复用
- 所有卡片组件统一 16px 圆角 + 微阴影，hover 时升起
- 头像渐变统一 `#b52b2b → #c0392b`
- 入场动画：`mf-fade-in` 系列（6 级延迟），staggered 自动播放
- 响应式布局：侧边栏在窄屏自适应折叠
- 前端 Vite 代理自动转发 API 请求到后端，SPA 路由通过 `bypass` 正确处理

## 开发说明

- 前端开发服务器 (`localhost:5173`) 通过 Vite 代理将 API 请求转发到后端 (`localhost:8080`)
- 敏感配置通过 `moforum/.env` 文件管理（复制 `.env.example` 后填入实际值），或通过同名系统环境变量设置
- WebSocket 连接 STOMP 端点 `/ws`（SockJS fallback），通过 `token` header 认证
- 图片上传限制 3MB（前后端一致校验）
- 测试账号：`alice` / `bob` / `admin`，密码均为 `123456`
