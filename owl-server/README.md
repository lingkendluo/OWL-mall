# owl-server

OWL 商城后端。单体应用 + Maven 多模块，一个进程，按微服务边界分包，后期要拆不用重写。

- JDK 21 / Spring Boot 3.2.12 / MyBatis-Plus 3.5.7 / MySQL 8.0
- 金额全程 `long` 分，只在 VO 层转元；对外 id 全 `String`；时间字段全毫秒时间戳
- 不用 Spring Security 全家桶，鉴权是 `HandlerInterceptor` + `UserContext`/`AdminContext`

## 模块

| 模块 | 职责 |
|---|---|
| `owl-common` | 统一响应 `Result`/`PageResult`、错误码、业务异常、上下文、金额与时间工具 |
| `owl-infra` | 配置、JWT、BCrypt、拦截器、全局异常、traceId、Caffeine 缓存、`KVStore`、AI 客户端 |
| `owl-modules/owl-message` | 站内信与未读数 |
| `owl-modules/owl-auth` | 注册登录、游客、地址簿、优惠券、积分 |
| `owl-modules/owl-catalog` | 类目、商品、库存（含 B 端写接口） |
| `owl-modules/owl-payment` | 支付单、退款单、模拟渠道 SPI |
| `owl-modules/owl-trade` | 购物车、结算预览、订单、个人中心汇总 |
| `owl-modules/owl-assistant` | AI 购物助手（DeepSeek） |
| `owl-admin` | B 端接口：管理员登录、商品/订单/统计/类目、审计日志 |
| `owl-boot` | 启动类、配置、SQL、种子数据、静态图片 |

依赖方向只往下：`boot → admin → trade/payment/catalog/auth/message → infra → common`。
`payment` 需要订单数据但不 import `trade`，用 `PayOrderPort` 把依赖倒过来，由 `trade` 实现。

## 启动

```bash
# 1. 建库（库名 owl_mall）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS owl_mall DEFAULT CHARSET utf8mb4;"

# 2. 建表 + 券模板
mysql -uroot -p owl_mall < owl-boot/src/main/resources/db/01_schema.sql
mysql -uroot -p owl_mall < owl-boot/src/main/resources/db/02_init.sql

# 3. 改数据库账号密码：owl-boot/src/main/resources/application-dev.yml

# 4. 打包并启动
mvn clean install -DskipTests
java -jar owl-boot/target/owl-boot-1.0.0.jar --server.port=8080

# 或者开发期直接跑
mvn -pl owl-boot -am spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
```

启动时自动做三件事：`CatalogSeeder` 灌 15 类 / 120 细分 / 90 商品（按 code 幂等 upsert），
`DataSeeder` 建管理员与演示用户，`KVConfig` 探测 Redis。

**端口**：如果环境里存在 `SERVER__PORT` 之类的变量，会覆盖 `application.yml` 的 8080，
启动时用 `--server.port=8080` 显式指定最稳。

**Redis**：本机已装（`tools/redis/`，7.4.11 便携版，`127.0.0.1:6379` / 密码 `owl123456`），
双击 `tools/redis/start-redis.bat` 启动，详见 `tools/redis/README.md`。
即使没启动也能跑：`KVStore` 会在启动时 ping 一次，失败自动降级为进程内实现并打 WARN，
验证码、refresh token、限流、订单号序列都照常工作（单实例、重启后计数归零）。
订单号序列做了兜底：进程内计数归零后先从库里当天最大单号对齐，不会撞 `uk_order_no`。

## 默认账号

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | `admin` | `admin123456` |
| 演示用户 | `13800138000` | `abc12345` |

生产环境把 `owl.seed.enabled` 置 false，并改掉这两个密码。

## 环境变量

| 变量 | 说明 |
|---|---|
| `DEEPSEEK_API_KEY` | AI 助手用它调模型；不配也能启动，接口返回兜底文案 + `degraded=true` |
| `OWL_JWT_SECRET` | JWT 签名密钥，上线必须换 |
| `DEEPSEEK_BASE_URL` | 默认 `https://api.deepseek.com` |

Key 只从环境变量读，yml 里留空，也不会返回给前端。

## 接口约定

- 统一前缀 `/api/v1`，由 `WebConfig.addPathPrefix` 给所有 `@RestController` 加，Controller 里不写
- 统一响应 `{ code, message, data, traceId }`，业务失败 HTTP 仍是 200，只有 401/403/429/500 用 HTTP 状态码
- C 端 `Authorization: Bearer <accessToken>`，B 端同一套头但 `scope=admin`，两边 token 互不通用
- 文档：`http://localhost:8080/doc.html`（Knife4j）

冒烟：

```bash
curl -s localhost:8080/api/v1/health
curl -s localhost:8080/api/v1/c/home
curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"account":"13800138000","password":"abc12345"}'
```

## 与前端联调

前端目前是静态数据，改数据源之前先把后端跑通。前端只需要一个环境变量就能接上 AI 助手：

```
# owl-web/.env.local
VITE_ASSISTANT_API=http://localhost:8080/api/v1/assistant
VITE_ASSISTANT_MODEL=deepseek-chat
```

图片走 `/static/products/{code}.jpg`，由 `owl-boot/src/main/resources/static` 提供，
不受 `/api/v1` 前缀影响。

## 测试

```bash
mvn test -pl owl-common,owl-modules/owl-trade
```

- `TimeTextUtilTest`：站内信时间文案 6 个分支
- `PriceCalculatorTest`：满 99 免运费 / 不满收 5 元 / 券后不满仍收 / 券超商品金额

## 取舍说明

- **支付渠道**：只有 `MockPaymentChannel`，但接口按真实渠道写（`create`/`verifyCallback`/`refund`），
  Service 里没有 `if (channel == "mock")`。`query` 没做——模拟渠道下查自己库里的支付单就是准的。
- **库存**：只认 `c_product.stock`，`UPDATE ... WHERE stock >= qty AND version = ?` 乐观锁，影响 0 行抛 30003。
- **幂等**：下单靠 `clientToken`，回调靠 `UPDATE ... WHERE status = 0` + Redis 锁，退款靠 `p_refund` 唯一约束 + 预查。
- **缓存**：Caffeine，首页 5 分钟 / 类目 10 分钟 / 商品 5 分钟，写操作后在 Service 里主动 evict。
- **审计**：B 端写操作打 `@AdminAction`，AOP 统一写 `s_admin_log`，日志写失败不影响业务。
