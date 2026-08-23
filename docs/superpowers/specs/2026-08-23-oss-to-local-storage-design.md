# 设计：阿里云 OSS 图片存储替换为服务器本地存储

- 日期：2026-08-23
- 状态：已确认
- 范围：`moforum` 后端 + Docker 部署配置

## 背景与目标

当前图片上传依赖阿里云 OSS 对象存储（`OssService` + `OssConfig`，`aliyun-sdk-oss` 依赖）。用户服务器有 40G 磁盘空间，希望改为把图片直接存到服务器本地磁盘，不再依赖 OSS。

目标：

1. 完全移除 OSS 相关代码与依赖。
2. 图片写到宿主机本地目录，由 nginx 直接对外提供静态文件（不经过 Spring）。
3. 保留现有上传 → 确认 → 孤儿清理的 Redis 流程，行为不变。
4. 图片在宿主机目录上直接可见、可备份。

## 现状（OSS 流程）

- 上传：前端 `POST /api/upload/image`（multipart）→ nginx `/api/` 反代 → `UploadController` → `OssService.upload()` 生成 UUID key，上传到 OSS，返回绝对 URL `https://{bucket}.{endpoint}/uploads/{uuid}{ext}`。
- 确认：`POST /api/upload/confirm` 把 key 从 Redis `img:pending` 移到 `img:confirmed`。
- 清理：`ImageCleanupService` 每 10 分钟删除「上传后 30 分钟仍未确认」的图片。
- 展示：返回的 URL 存进数据库（头像、帖子内容等），前端用 `<img src="URL">` 渲染。

## 目标设计（本地存储）

### 数据流

```
上传：前端 → nginx /api/ 反代 → UploadController
        → LocalStorageService 写文件到 /app/uploads/{uuid}{ext}
        → 返回相对 URL /uploads/{uuid}{ext}

展示：浏览器请求 /uploads/{uuid}{ext}
        → nginx 从 /usr/share/nginx/html/uploads/ 读盘直出（不经过 Spring）

确认/清理：沿用 Redis img:pending / img:confirmed，key = 文件名 {uuid}{ext}
```

### key / URL / 路径映射

| 概念 | 值 |
|---|---|
| key（文件名） | `{uuid}{ext}`，如 `a1b2c3....jpg` |
| 磁盘路径 | `{rootPath}/{key}` = `/app/uploads/a1b2c3....jpg` |
| 返回 URL | `/uploads/{key}` = `/uploads/a1b2c3....jpg` |

`/uploads/` 只是公开 URL 前缀，不再是 key 的一部分（OSS 的 key 是 `uploads/{uuid}{ext}`，本地改为扁平文件名）。

### 组件设计

新建 `LocalStorageService`（`src/main/java/com/moforum/service/LocalStorageService.java`），方法签名与旧 `OssService` 对齐，便于两个调用方改动最小：

```java
@Service
public class LocalStorageService {
    private static final String PUBLIC_PREFIX = "/uploads/";

    @Value("${storage.local.path:/app/uploads}")
    private String rootPath;

    public String upload(MultipartFile file) {
        // 扩展名取自原始文件名；key = UUID(去横线) + ext
        // Files.createDirectories(rootPath) 后写入文件
        // 返回 PUBLIC_PREFIX + key
    }

    public void delete(String url) { deleteByKey(extractKey(url)); }

    public void deleteByKey(String key) {
        // Files.deleteIfExists(rootPath/key)，key 为空直接返回
    }

    public String extractKey(String url) {
        // url 必须以 PUBLIC_PREFIX 开头，否则返回 null
        // 截取后校验 key 是纯文件名（不含 /、\、..），防路径穿越，否则返回 null
    }
}
```

去掉了 `isEnabled()`：本地存储永远可用，`UploadController` 和 `ImageCleanupService` 中的 `isEnabled()` 检查一并删除。

### 文件改动清单

删除（2）：

- `src/main/java/com/moforum/service/OssService.java`
- `src/main/java/com/moforum/config/OssConfig.java`

新建（1）：

- `src/main/java/com/moforum/service/LocalStorageService.java`

修改（9）：

- `src/main/java/com/moforum/controller/UploadController.java` — `OssService` → `LocalStorageService`，删除 `isEnabled()` 检查
- `src/main/java/com/moforum/service/ImageCleanupService.java` — 同上，删除 `isEnabled()` 早退
- `pom.xml` — 删除 `com.aliyun.oss:aliyun-sdk-oss:3.18.1` 依赖
- `src/main/resources/application.yaml` — 删除 `oss:` 块；新增 `storage.local.path: uploads`（本地开发）
- `docker/application.properties` — 删除 `oss.*`；新增 `storage.local.path=/app/uploads`
- `docker/properties.example` — 同步上述改动
- `.env.example` — 删除 `OSS_*` 几行
- `docker-compose.yml` — app 增加 `./uploads:/app/uploads`；nginx 增加 `./uploads:/usr/share/nginx/html/uploads:ro`
- `docker/nginx.conf` — 新增 `location /uploads/ { alias /usr/share/nginx/html/uploads/; expires 30d; }`

### 权限处理

- app 容器以非 root 用户 `appuser`（uid 100，alpine `adduser -S` 的首个系统用户）运行；nginx 以 `nginx` 用户（uid 101）运行。
- app 写入的文件默认 644、目录 755，nginx 只需读权限，天然可读。
- 唯一需要处理的是宿主机 `uploads` 目录的初始属主：部署时执行一次 `mkdir -p uploads && chown 100:100 uploads`，使 appuser（uid 100）能写入，nginx（uid 101）以「其他用户」读权限读取。

### 配置清理

删除所有 OSS 相关配置：

- `application.yaml` 的 `oss:` 块
- `docker/application.properties` 的 `oss.*`
- `docker/properties.example` 的 `oss.*`
- `.env.example` 的 `OSS_*`

新增本地存储路径配置：

- Docker 环境：`storage.local.path=/app/uploads`（与挂载点一致）
- 本地开发：`storage.local.path=uploads`（相对路径，落到项目目录）

### 部署步骤（服务器）

1. 拉取新代码。
2. 在项目目录执行 `mkdir -p uploads && chown 100:100 uploads`。
3. 编辑 `docker/application.properties`：删除 `oss.*`，新增 `storage.local.path=/app/uploads`。
4. `docker compose up -d --build`（因 pom 变更需重新构建镜像）。
5. 浏览器上传一张图验证。

### 测试要点

- 上传图片 → 返回 `/uploads/...` → 页面正常显示（nginx 直出）。
- 上传后不确认 → 30 分钟后被清理（可手动验证 `img:pending` 流程）。
- 删除 OSS 依赖后应用正常启动（无 `ClassNotFoundException`）。
- 路径穿越：构造非法 URL（含 `..` 或 `/`）调用 confirm，不应影响文件系统。

### 范围外 / 备注

- 存量 OSS 数据：不迁移，库中已存在的 OSS 绝对地址头像将 404（用户已确认「可丢弃」）。
- 上传大小限制保持不变（后端 3MB、nginx 10MB）。
