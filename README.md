# OWL-System · OWL 商城

一个前后端分离的商城项目：Spring Boot 单体多模块后端 + 两个独立的 Vue 3 前端应用。

## 目录

| 目录 | 是什么 | 用什么打开 | 跑在哪 |
|---|---|---|---|
| `owl-server/` | 后端：用户端接口 + 管理端接口 + 启动模块 | IntelliJ IDEA（打开根 `pom.xml`） | http://localhost:8080 |
| `owl-web/` | 前端：用户端 + 管理端两个应用 | VS Code（打开 `owl-web` 文件夹） | 5173 / 5174 |
| `docs/` | 开发文档、API 契约、OpenAPI、生成脚本 | 浏览器 / 编辑器 | — |
| `tools/redis/` | 本地 Redis 7.4.11 便携版（免安装、免管理员） | 双击 `start-redis.bat` | 127.0.0.1:6379 |
| `.backup/` | 重组前的源码快照（不进 git，确认无误后可删） | — | — |

## 后端 · owl-server

一个 Spring Boot 进程，按模块分包，不是两个服务：

- 业务模块在 `owl-modules/*`：`auth` 登录注册、`catalog` 商品库存、`trade` 购物车订单、
  `payment` 支付、`message` 站内信、`assistant` AI 助手
- `owl-admin` 是**管理端接口**：管理员登录、商品/订单/统计/类目管理、审计日志
- `owl-boot` 是**启动模块**：`OwlApplication`、`application*.yml`、建表 SQL、种子数据、静态商品图
- `owl-common` / `owl-infra`：统一响应、异常、JWT、拦截器、缓存等底座

> 管理端接口和用户端接口共用同一个 8080 进程（`/api/v1/admin/**` 与 `/api/v1/c/**`），
> 拆成两个工程会复制 common/infra，还要维护两套启动和事务边界，所以保持单体。
> 模块职责详见 `owl-server/README.md`。

启动：IDEA 打开 `owl-server` 根目录 → 等 Maven 导入 → 运行 `owl-boot` 的 `OwlApplication`。
需要本机 MySQL（库名 `owl_mall`，账号见 `owl-boot/src/main/resources/application-dev.yml`）。

## 前端 · owl-web

```
owl-web/
├── apps/client   # 用户端 → http://localhost:5173
├── apps/admin    # 管理端 → http://localhost:5174
└── shared        # http 封装、公开公告接口、设计变量
```

```bash
cd owl-web
npm install          # 第一次
npm run dev          # 用户端 5173
npm run dev:admin    # 管理端 5174
npm run dev:all      # 两个一起
```

两个应用都通过 Vite 代理访问后端（`/api/v1`、`/static` → 8080），无需后端配 CORS。
详见 `owl-web/README.md`。

## 启动顺序

1. 起 MySQL，建库 `owl_mall`，执行 `owl-boot/src/main/resources/db/` 下的 `01_schema.sql`、`02_init.sql`
2. 起 Redis：双击 `tools/redis/start-redis.bat`（`127.0.0.1:6379`，密码 `owl123456`）
3. 启动后端 `OwlApplication`（8080）
4. 起前端：用户端 5173 / 管理端 5174
5. 管理端用 `owl-server/README.md` 里的默认管理员账号登录；用户端登录走会员账号

> Redis 不是启动硬依赖：没起 Redis 后端也能跑，只是会打一行 WARN 并降级成进程内缓存
> （重启丢数据、不能多实例）。连上后日志会变成 `[owl] Redis 已连接，使用 RedisKVStore`。
> 详见 `tools/redis/README.md`。
