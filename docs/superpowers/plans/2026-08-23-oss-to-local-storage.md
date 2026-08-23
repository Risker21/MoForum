# 本地图片存储（替换阿里云 OSS）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把图片上传从阿里云 OSS 改为服务器本地磁盘存储，由 nginx 直接对外提供图片，完全移除 OSS 代码与依赖。

**Architecture:** 新增 `LocalStorageService` 把上传文件写到宿主机挂载目录 `/app/uploads`，返回相对 URL `/uploads/{key}`；nginx 把 `/uploads/` 映射到该目录静态直出（不经过 Spring）。上传→确认→孤儿清理的 Redis 流程保持不变。

**Tech Stack:** Java 17、Spring Boot 3.5.9、MyBatis、Redis、Docker Compose、nginx。

## Global Constraints

- Java 17，Spring Boot 3.5.9（不变）。
- 上传大小限制保持 3MB（后端 `UploadController.MAX_SIZE` + `application.yaml` multipart 配置不变）。
- 文件命名规则：`key = UUID(去横线) + 原文件扩展名`，例如 `a1b2c3....jpg`。
- URL 规则：返回相对路径 `/uploads/{key}`。
- 磁盘规则：文件写到 `{rootPath}/{key}`。
- `rootPath` 默认 `/app/uploads`；Docker 环境用 `storage.local.path=/app/uploads`，本地开发用 `storage.local.path=uploads`。
- `PUBLIC_PREFIX = "/uploads/"`。
- 全部任务完成后，代码库中不得残留任何 `aliyun` / `oss.` / `OSS`（大小写不敏感，不含 README 中 `crossOrigin` 等无关子串）引用。
- 每次提交的 commit message 末尾追加 `Co-Authored-By: Claude <noreply@anthropic.com>`。

---

### Task 1: 新增 LocalStorageService（含单元测试）

**Files:**
- Create: `moforum/src/main/java/com/moforum/service/LocalStorageService.java`
- Test: `moforum/src/test/java/com/moforum/service/LocalStorageServiceTest.java`

**Interfaces:**
- Produces:
  - `String upload(MultipartFile file)` — 写文件并返回 `/uploads/{key}`
  - `void delete(String url)` — 按 URL 删除
  - `void deleteByKey(String key)` — 按 key 删除（key 不合法时静默忽略）
  - `String extractKey(String url)` — 从 URL 提取 key，非法返回 null

- [ ] **Step 1: 写失败测试**

新建 `moforum/src/test/java/com/moforum/service/LocalStorageServiceTest.java`：

```java
package com.moforum.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService service() {
        return new LocalStorageService(tempDir.toString());
    }

    @Test
    void uploadWritesFileAndReturnsPublicUrl() throws Exception {
        LocalStorageService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "fake-image-bytes".getBytes());

        String url = service.upload(file);

        assertTrue(url.startsWith("/uploads/"));
        String key = url.substring("/uploads/".length());
        Path saved = tempDir.resolve(key);
        assertTrue(Files.exists(saved));
        assertEquals("fake-image-bytes", new String(Files.readAllBytes(saved)));
    }

    @Test
    void extractKeyReturnsFilenameForValidUrl() {
        LocalStorageService service = service();
        assertEquals("abc123.jpg", service.extractKey("/uploads/abc123.jpg"));
    }

    @Test
    void extractKeyRejectsInvalidUrl() {
        LocalStorageService service = service();
        assertNull(service.extractKey("/uploads/../../etc/passwd"));
        assertNull(service.extractKey("/uploads/../secret.txt"));
        assertNull(service.extractKey("/uploads/a/b.txt"));
        assertNull(service.extractKey("https://bucket.oss.com/uploads/a.jpg"));
        assertNull(service.extractKey(null));
        assertNull(service.extractKey(""));
    }

    @Test
    void deleteByUrlRemovesFile() throws Exception {
        LocalStorageService service = service();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", "x".getBytes());
        String url = service.upload(file);
        String key = service.extractKey(url);

        service.delete(url);

        assertFalse(Files.exists(tempDir.resolve(key)));
    }

    @Test
    void deleteByKeyCannotEscapeRoot() throws Exception {
        Path root = tempDir.resolve("uploads");
        Files.createDirectories(root);
        LocalStorageService service = new LocalStorageService(root.toString());

        Path sensitive = tempDir.resolve("secret.txt"); // root 之外的兄弟文件
        Files.write(sensitive, "keep".getBytes());

        service.deleteByKey("../secret.txt"); // 若未防护会越界删除 sensitive

        assertTrue(Files.exists(sensitive));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `cd moforum && mvn -q -Dtest=LocalStorageServiceTest test`
Expected: 编译失败（`LocalStorageService` 类不存在）。

- [ ] **Step 3: 实现最小代码**

新建 `moforum/src/main/java/com/moforum/service/LocalStorageService.java`：

```java
package com.moforum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalStorageService {

    private static final String PUBLIC_PREFIX = "/uploads/";

    private final String rootPath;

    public LocalStorageService(@Value("${storage.local.path:/app/uploads}") String rootPath) {
        this.rootPath = rootPath;
    }

    public String upload(MultipartFile file) {
        String ext = "";
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            ext = name.substring(name.lastIndexOf("."));
        }
        String key = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(rootPath);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(key));
            return PUBLIC_PREFIX + key;
        } catch (IOException e) {
            throw new RuntimeException("上传失败: " + e.getMessage());
        }
    }

    public void delete(String url) {
        deleteByKey(extractKey(url));
    }

    public void deleteByKey(String key) {
        if (!isValidKey(key)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(rootPath, key));
        } catch (IOException e) {
            // 删除失败不影响主流程
        }
    }

    public String extractKey(String url) {
        if (url == null || !url.startsWith(PUBLIC_PREFIX)) {
            return null;
        }
        String key = url.substring(PUBLIC_PREFIX.length());
        return isValidKey(key) ? key : null;
    }

    private static boolean isValidKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        return !key.contains("/") && !key.contains("\\") && !key.contains("..");
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `cd moforum && mvn -q -Dtest=LocalStorageServiceTest test`
Expected: `Tests run: 5, Failures: 0, Errors: 0`（BUILD SUCCESS）。

- [ ] **Step 5: 提交**

```bash
cd /c/Users/32252/Desktop/MoForum
git add moforum/src/main/java/com/moforum/service/LocalStorageService.java moforum/src/test/java/com/moforum/service/LocalStorageServiceTest.java
git commit -m "feat: 新增本地图片存储服务 LocalStorageService

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: UploadController 改用 LocalStorageService

**Files:**
- Modify: `moforum/src/main/java/com/moforum/controller/UploadController.java`

**Interfaces:**
- Consumes: `LocalStorageService.upload(MultipartFile)`、`LocalStorageService.extractKey(String)`
- Produces: `POST /api/upload/image`、`POST /api/upload/confirm`（行为与原来一致，仅去掉 `isEnabled()` 检查）

- [ ] **Step 1: 替换整个文件内容**

用 Write 覆盖 `moforum/src/main/java/com/moforum/controller/UploadController.java` 为：

```java
package com.moforum.controller;

import com.moforum.config.UserPrincipal;
import com.moforum.service.LocalStorageService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private static final long MAX_SIZE = 3 * 1024 * 1024;

    private final LocalStorageService storageService;
    private final StringRedisTemplate redis;

    public UploadController(LocalStorageService storageService, StringRedisTemplate redis) {
        this.storageService = storageService;
        this.redis = redis;
    }

    @PostMapping("/image")
    public Map<String, Object> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return Map.of("success", false, "message", "请先登录");
        }
        if (file.isEmpty()) {
            return Map.of("success", false, "message", "文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            return Map.of("success", false, "message", "图片大小不能超过 3MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Map.of("success", false, "message", "仅支持图片格式");
        }
        try {
            String url = storageService.upload(file);
            String key = storageService.extractKey(url);
            if (key != null) {
                redis.opsForSet().add("img:pending", key);
                redis.opsForHash().put("img:pending:meta", key, String.valueOf(System.currentTimeMillis()));
            }
            return Map.of("success", true, "message", "上传成功", "url", url);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirmImage(@RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return Map.of("success", false, "message", "请先登录");
        }
        String url = body.get("url");
        if (url == null || url.isEmpty()) {
            return Map.of("success", false, "message", "参数缺失");
        }
        String key = storageService.extractKey(url);
        if (key != null) {
            redis.opsForSet().add("img:confirmed", key);
            redis.opsForSet().remove("img:pending", key);
            redis.opsForHash().delete("img:pending:meta", key);
        }
        return Map.of("success", true);
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd moforum && mvn -q -DskipTests compile`
Expected: BUILD SUCCESS（此时 `OssService` 仍存在，只是不再被 UploadController 引用）。

- [ ] **Step 3: 提交**

```bash
cd /c/Users/32252/Desktop/MoForum
git add moforum/src/main/java/com/moforum/controller/UploadController.java
git commit -m "refactor: 上传接口改用本地存储

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: ImageCleanupService 改用 LocalStorageService

**Files:**
- Modify: `moforum/src/main/java/com/moforum/service/ImageCleanupService.java`

**Interfaces:**
- Consumes: `LocalStorageService.deleteByKey(String)`
- Produces: 每 10 分钟清理「上传后 30 分钟未确认」的图片（行为不变）

- [ ] **Step 1: 替换整个文件内容**

用 Write 覆盖 `moforum/src/main/java/com/moforum/service/ImageCleanupService.java` 为：

```java
package com.moforum.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@EnableScheduling
public class ImageCleanupService {

    private final LocalStorageService storageService;
    private final StringRedisTemplate redis;

    public ImageCleanupService(LocalStorageService storageService, StringRedisTemplate redis) {
        this.storageService = storageService;
        this.redis = redis;
    }

    @Scheduled(fixedRate = 600_000)
    public void cleanupOrphanedImages() {
        Set<String> pending = redis.opsForSet().members("img:pending");
        if (pending == null || pending.isEmpty()) {
            return;
        }
        Set<String> confirmed = redis.opsForSet().members("img:confirmed");
        long cutoff = System.currentTimeMillis() - 30 * 60 * 1000L;
        for (String key : pending) {
            String ts = (String) redis.opsForHash().get("img:pending:meta", key);
            if (ts == null) {
                continue;
            }
            try {
                if (Long.parseLong(ts) < cutoff && (confirmed == null || !confirmed.contains(key))) {
                    storageService.deleteByKey(key);
                    redis.opsForSet().remove("img:pending", key);
                    redis.opsForHash().delete("img:pending:meta", key);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd moforum && mvn -q -DskipTests compile`
Expected: BUILD SUCCESS。

- [ ] **Step 3: 提交**

```bash
cd /c/Users/32252/Desktop/MoForum
git add moforum/src/main/java/com/moforum/service/ImageCleanupService.java
git commit -m "refactor: 图片清理改用本地存储

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: 删除 OSS（OssService / OssConfig / pom 依赖）

**Files:**
- Delete: `moforum/src/main/java/com/moforum/service/OssService.java`
- Delete: `moforum/src/main/java/com/moforum/config/OssConfig.java`
- Modify: `moforum/pom.xml`（删除 `aliyun-sdk-oss` 依赖，第 104-107 行）

**Interfaces:**
- 前置条件：Task 2、Task 3 已把唯一两个调用方切走，删除后不再有任何 `OssService` / `OSS` 引用。

- [ ] **Step 1: 删除两个文件**

Run: `cd /c/Users/32252/Desktop/MoForum && git rm moforum/src/main/java/com/moforum/service/OssService.java moforum/src/main/java/com/moforum/config/OssConfig.java`

- [ ] **Step 2: 删除 pom.xml 依赖**

用 Edit 将 `moforum/pom.xml` 中以下内容整段删除（含前后空行之一）：

```xml
        <dependency>
            <groupId>com.aliyun.oss</groupId>
            <artifactId>aliyun-sdk-oss</artifactId>
            <version>3.18.1</version>
        </dependency>
```

- [ ] **Step 3: 编译 + 单元测试验证**

Run: `cd moforum && mvn -q -DskipTests compile && mvn -q -Dtest=LocalStorageServiceTest test`
Expected: 两步均 BUILD SUCCESS；`Tests run: 5, Failures: 0`。

- [ ] **Step 4: 提交**

```bash
cd /c/Users/32252/Desktop/MoForum
git add -A moforum/pom.xml
git commit -m "refactor: 移除阿里云 OSS 依赖

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 5: 清理 OSS 相关配置

**Files:**
- Modify: `moforum/src/main/resources/application.yaml`
- Modify: `moforum/docker/properties.example`
- Modify: `moforum/.env.example`

**Interfaces:**
- Produces: `storage.local.path` 配置（Docker=`/app/uploads`，本地=`uploads`）

- [ ] **Step 1: 修改 application.yaml**

用 Edit 删除 `moforum/src/main/resources/application.yaml` 末尾的 `oss:` 块：

```yaml
oss:
  endpoint: ${OSS_ENDPOINT:}
  access-key: ${OSS_ACCESS_KEY:}
  secret-key: ${OSS_SECRET_KEY:}
  bucket: ${OSS_BUCKET:}
  max-size: 3145728
```

在文件末尾新增：

```yaml

storage:
  local:
    path: uploads
```

（最终 `jwt:` 块之后是 `storage:` 块，`oss:` 块已不存在。）

- [ ] **Step 2: 修改 docker/properties.example**

用 Edit 删除 `moforum/docker/properties.example` 末尾的 OSS 块：

```properties
# 阿里云 OSS 配置
oss.endpoint=oss-cn-beijing.aliyuncs.com
oss.access-key=your_oss_access_key
oss.secret-key=your_oss_secret_key
oss.bucket=your_bucket_name
oss.max-size=3145728
```

替换为：

```properties
# 本地图片存储路径（容器内，对应 docker-compose 挂载的 ./uploads）
storage.local.path=/app/uploads
```

- [ ] **Step 3: 修改 .env.example**

用 Edit 删除 `moforum/.env.example` 中的 OSS 行：

```bash
# 阿里云 OSS
# OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
# OSS_ACCESS_KEY=your_oss_access_key
# OSS_SECRET_KEY=your_oss_secret_key
# OSS_BUCKET=your_bucket_name
```

（删除后「应用配置」段落只保留 JWT 相关注释。）

- [ ] **Step 4: 编译 + 打包验证**

Run: `cd moforum && mvn -q -DskipTests package`
Expected: BUILD SUCCESS（确认配置改动不影响打包）。

- [ ] **Step 5: 提交**

```bash
cd /c/Users/32252/Desktop/MoForum
git add moforum/src/main/resources/application.yaml moforum/docker/properties.example moforum/.env.example
git commit -m "refactor: 清理 OSS 相关配置，新增本地存储路径

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 6: Docker 挂载 + nginx 直出

**Files:**
- Modify: `moforum/docker-compose.yml`
- Modify: `moforum/docker/nginx.conf`

**Interfaces:**
- Produces: app 容器写 `/app/uploads`，nginx 容器把 `/uploads/` 映射到 `/usr/share/nginx/html/uploads/`（宿主机同目录 `./uploads` 分别挂载到两容器）。

- [ ] **Step 1: 修改 docker-compose.yml（app 服务）**

用 Edit 将 app 服务的 volumes：

```yaml
    volumes:
      - ./docker/application.properties:/app/config/application.properties:ro
```

替换为：

```yaml
    volumes:
      - ./docker/application.properties:/app/config/application.properties:ro
      - ./uploads:/app/uploads
```

- [ ] **Step 2: 修改 docker-compose.yml（nginx 服务）**

用 Edit 将 nginx 服务的 volumes：

```yaml
    volumes:
      - ./docker/nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./docker/frontend:/usr/share/nginx/html:ro  # 前端静态文件
```

替换为：

```yaml
    volumes:
      - ./docker/nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./docker/frontend:/usr/share/nginx/html:ro  # 前端静态文件
      - ./uploads:/usr/share/nginx/html/uploads:ro  # 用户上传图片
```

- [ ] **Step 3: 修改 docker/nginx.conf**

用 Edit 在 `location /api/ { ... }` 块之后、`location / { ... }` 之前，插入：

```nginx
    # ============ 用户上传图片（本地存储直出） ============
    location /uploads/ {
        alias /usr/share/nginx/html/uploads/;
        expires 30d;
    }

```

- [ ] **Step 4: 校验 compose 配置**

Run: `cd moforum && docker compose config -q`
Expected: 无输出（无错误），退出码 0。

- [ ] **Step 5: 提交**

```bash
cd /c/Users/32252/Desktop/MoForum
git add moforum/docker-compose.yml moforum/docker/nginx.conf
git commit -m "feat: docker 挂载本地图片目录并由 nginx 直出

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 7: 更新 README 移除 OSS 说明

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 技术栈表格删除 OSS 行**

用 Edit 删除：

```markdown
| 阿里云 OSS | — |
```

- [ ] **Step 2: 修改 .env 说明**

用 Edit 将：

```markdown
编辑 `moforum/.env`，填入你的数据库密码和 OSS 密钥（参考下方示例）。
```

改为：

```markdown
编辑 `moforum/.env`，填入你的数据库密码（参考下方示例）。
```

- [ ] **Step 3: 修改 .env 示例内容**

用 Edit 将：

```bash
DB_PASSWORD=your_mysql_password
JWT_SECRET=your_random_jwt_secret_string
OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
OSS_ACCESS_KEY=your_oss_access_key
OSS_SECRET_KEY=your_oss_secret_key
OSS_BUCKET=moforum
```

改为：

```bash
DB_PASSWORD=your_mysql_password
JWT_SECRET=your_random_jwt_secret_string
```

- [ ] **Step 4: 修改项目结构注释**

用 Edit 将：

```markdown
│   │   ├── config/                   # 配置：Security / JWT / WebSocket / Redis / OSS
```

改为：

```markdown
│   │   ├── config/                   # 配置：Security / JWT / WebSocket / Redis
```

用 Edit 将：

```markdown
│       ├── application.yaml          # 主配置（含 OSS、MySQL、Redis 默认值）
```

改为：

```markdown
│       ├── application.yaml          # 主配置（含 MySQL、Redis 默认值）
```

- [ ] **Step 5: 修改接口概览**

用 Edit 将：

```markdown
| POST | `/user/uploadAvatar` | ✅ | 上传头像（OSS） |
```

改为：

```markdown
| POST | `/user/uploadAvatar` | ✅ | 上传头像（本地存储） |
```

- [ ] **Step 6: 修改核心特性**

用 Edit 将：

```markdown
- **OSS 图片上传** — 阿里云 OSS 存储，支持 3MB 以内图片，前端 Markdown 自动插入
```

改为：

```markdown
- **本地图片存储** — 服务器本地磁盘存储（nginx 直出），支持 3MB 以内图片，前端 Markdown 自动插入
```

- [ ] **Step 7: 提交**

```bash
cd /c/Users/32252/Desktop/MoForum
git add README.md
git commit -m "docs: 更新 README 移除 OSS 说明

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 8: gitignored 配置 + 服务器部署验证

> 本任务的 `docker/application.properties` 已被 `.gitignore` 忽略（含真实密钥），改动不提交。其余步骤在服务器上执行。

**Files:**
- Modify（本地 + 服务器，不提交）: `moforum/docker/application.properties`

- [ ] **Step 1: 修改本机 docker/application.properties**

用 Edit 删除 `docker/application.properties` 末尾整个 OSS 块（从 `# 阿里云 OSS 配置` 注释行到 `oss.max-size=3145728`，共 6 行，含你的真实 AccessKey/SecretKey），替换为：

```properties
# 本地图片存储路径（容器内，对应 docker-compose 挂载的 ./uploads）
storage.local.path=/app/uploads
```

> 注意：该文件含真实密钥且已被 `.gitignore` 忽略，改动不要提交。上面只给出要「替换成」的内容，要「删除」的原内容因含密钥不再在本文档中贴出。

- [ ] **Step 2: 服务器上同步修改 docker/application.properties**

在服务器项目目录（如 `/www/wwwroot/MoForum/moforum`）执行同样的删除 OSS 块 + 新增 `storage.local.path=/app/uploads` 的编辑（或直接 `git pull` 后用同样方式编辑）。

- [ ] **Step 3: 服务器上创建上传目录并赋权**

Run（在服务器项目目录 `/www/wwwroot/MoForum/moforum`）：

```bash
mkdir -p uploads && chown 100:100 uploads
```

说明：`100:100` 对应 app 容器的 `appuser`；nginx 容器以 uid 101 的 `nginx` 用户运行，通过「其他用户」读权限即可读取 app 写出的 644 文件。

- [ ] **Step 4: 重新构建并启动**

Run（在服务器项目目录）：

```bash
docker compose up -d --build
```

Expected: 4 个容器（mysql/redis/app/nginx）健康，`docker compose ps` 无 unhealthy。

- [ ] **Step 5: 端到端验证**

1. 浏览器登录后发一个带图片的帖子，或上传头像。
2. 确认返回的 URL 是 `/uploads/xxx.jpg`，图片能正常显示（请求走 nginx，不经过 `/api`）。
3. 检查宿主机文件确实写入：`ls -l uploads/` 能看到对应文件。

---

## 验收清单

- [ ] `grep -ri "aliyun\|oss\." moforum/src moforum/pom.xml` 无结果（README 中 `crossOrigin` 等无关子串除外）。
- [ ] `mvn -q -Dtest=LocalStorageServiceTest test` 通过。
- [ ] 前端上传图片返回 `/uploads/...`，nginx 直出可显示。
- [ ] 上传后不确认，30 分钟后被 `ImageCleanupService` 清理。