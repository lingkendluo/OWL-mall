# OWL 商城 · 后端开发文档（可投喂版）

> 版本：v1.1 · 2026-10-28（JDK 21 修订：版本矩阵、编译参数、IDEA 配置、数据基数 15/120/90）
> 对应前端：`E:\OWL-System\owl-web`（Vue 3.5 + TS + Pinia + Vite 8）
> 目标后端：`E:\OWL-System\owl-server`（JDK 21 + Spring Boot 3.2.12 + Spring MVC + Spring Cloud Alibaba 2023）
> 配套投喂包：`docs/AI投喂包-JDK21.md`（每轮可复制的提示词）
> 文档用途：**直接投喂 AI 编码**。每一节都可独立引用，第 10 章的任务单按顺序喂即可。

---

## 目录

- [0. 怎么用这份文档](#0-怎么用这份文档)
- [1. 现状与范围](#1-现状与范围)
- [2. 关键决策（先读，避免返工）](#2-关键决策先读避免返工)
- [3. 技术栈与版本](#3-技术栈与版本)
- [4. 工程结构](#4-工程结构)
- [5. 数据库设计](#5-数据库设计)
- [6. 接口契约](#6-接口契约)
- [7. 核心业务规则](#7-核心业务规则)
- [8. 安全设计](#8-安全设计)
- [9. 微服务演进路线](#9-微服务演进路线)
- [10. 开发任务单（投喂用）](#10-开发任务单投喂用)
- [11. 前端对接改造清单](#11-前端对接改造清单)
- [12. 验收标准](#12-验收标准)
- [附录 A 错误码表](#附录-a-错误码表)
- [附录 B 状态枚举](#附录-b-状态枚举)
- [附录 C 投喂话术模板](#附录-c-投喂话术模板)

---

## 0. 怎么用这份文档

### 0.1 投喂策略

不要一次性把全文丢给 AI 说"把这个项目写完"，那样生成的代码一定跑不起来。按下面的节奏：

| 轮次 | 投喂内容 | 期望产出 |
|---|---|---|
| 第 1 轮 | 第 3、4 章 + 任务 T1 | 能 `mvn clean install` 通过的空骨架 |
| 第 2 轮 | 第 5 章 DDL + 任务 T2 | 建库脚本 + 实体 + Mapper，能启动 |
| 第 3 轮起 | 每次「第 6 章某一节 + 对应任务卡」 | 一个模块的完整 CRUD + 接口 |
| 最后 | 第 11 章 | 前端切换数据源 |

每一轮开头固定加一句：

```
严格遵守《OWL 商城后端开发文档》v1.0，只实现任务 Tn 范围内的代码，
不要修改其它模块，不要引入文档未列出的依赖。
包名统一 com.owl，类名、字段名、接口路径必须与文档逐字一致。
```

### 0.2 术语约定

| 术语 | 含义 |
|---|---|
| 商品 / product | 单规格商品（无 SKU 维度），对应前端 `Product` |
| 类目 / category | 一级分类，15 个（含"推荐"虚拟类目） |
| 细分 / sub | 二级分类，每类 8 个，对应前端 `Sub` |
| 购物车 / cart | 长期存放的待买清单，服务端持久化 |
| 待购清单 / draft | 前端本地的"这一单"，MVP 不落库，见 D4 |
| 分 / cent | 金额存储单位，1 元 = 100 分 |

### 0.3 文档中的强制标记

- **必须**：不符合就是 Bug。
- **禁止**：违反直接打回。
- **建议**：可以按实际情况调整，但要在代码注释里写理由。

---

## 1. 现状与范围

### 1.1 前端已实现的能力（后端要对齐的全部）

路由（`src/router/index.ts`，全部 `requiresAuth` 除 `/login`）：

| 路径 | 页面 | 依赖的数据 |
|---|---|---|
| `/login` | 登录 / 注册 / 游客 | 无（本地演示） |
| `/` | 首页：推荐流 + 分类墙 + 搜索 | 类目、商品、推荐位 |
| `/c/:catId?sub=` | 分类页 | 类目详情、细分（带件数）、商品列表 |
| `/item/:id` | 商品详情 | 商品、四段详情、同类推荐 |
| `/cart` | 购物车 | 购物车行、合计、勾选 |
| `/checkout` | 待购买（结算） | 待购清单、运费、应付 |
| `/profile` | 个人中心 | 用户资产、订单角标 |
| `/messages` | 消息 | 站内信列表、未读数 |

Pinia Store 与持久化 key：

| Store | 文件 | 本地 key | 后端要不要接管 |
|---|---|---|---|
| auth | `stores/auth.ts` | `owl.auth.session.v1` | **接管**（换 JWT） |
| cart | `stores/cart.ts` | `owl.cart.v1` | **接管**（服务端购物车） |
| checkout | `stores/checkout.ts` | `owl.checkout.v1` | 不接管，见 D4 |
| messages | `stores/messages.ts` | 无（内存 SEED） | **接管** |
| assistant | `stores/assistant.ts` | 无 | 只接管"大脑"，见 6.9 |

### 1.2 前端字段（后端 VO 必须逐字对齐）

**AuthUser**（`stores/auth.ts`）：

```ts
{ id: string, name: string, phone: string, email: string,
  level: 'black' | 'normal' | 'guest', memberNo: string,
  points: number, coupons: number, balance: number,
  joinedAt: number /* 毫秒时间戳 */, isGuest: boolean }
```

**Product**（`views/home/catalog.ts`）：

```ts
{ id: string, name: string, spec: string, price: number /* 元 */, list: number /* 元 */,
  tag: string, emoji: string, image: string, sub: string,
  detail: { slogan: string, intro: string,
            facts: Record<string, string>, tips: string[] } }
```

**Category / Sub**：

```ts
Category { id: string, name: string, hint: string, glyph: string,
           subs: Sub[], items: Product[] }
Sub { id: string, name: string, count: number }   // count=0 表示补货中
```

**CartLine**（`stores/cart.ts`）：

```ts
{ id: string, name: string, spec: string, price: number, list: number,
  emoji: string, image?: string, qty: number, checked: boolean }
```

**Message**（`stores/messages.ts`）：

```ts
{ id: string, kind: 'order' | 'promo' | 'system',
  title: string, body: string, time: string /* 服务端格式化的展示文案 */, read: boolean }
```

**AssistContext**（`services/assistant.ts`）：

```ts
{ cartCount: number, cartAmount: number, cartCheckedAmount: number, cartSaved: number,
  toFreeShipping: number, isFreeShipping: boolean, freeShippingAt: number,
  lines: Array<{ name: string, qty: number, price: number }> }
```

### 1.3 本期范围

**做：**

1. 认证：手机号/邮箱 + 密码登录、手机号验证码注册、游客登录、JWT 续期、退出。
2. 商品：15 个类目、90 件商品、细分筛选、关键词搜索、商品详情、首页聚合。
3. 购物车：增删改查、勾选、全选、登录合并。
4. 交易：服务端计价预览、创建订单、订单列表/详情、取消、确认收货。
5. 支付：模拟渠道的下单/回调/查询/退款，状态机与幂等完整。
6. 个人中心：资料、资产、订单角标、地址簿。
7. 消息：列表、未读数、已读、删除。
8. AI 助手：`POST /assistant/chat` 对接 DeepSeek，带购物车上下文。
9. B 端：管理员登录、商品 CRUD 与上下架、库存调整、订单发货、概览统计。
10. 工程能力：统一响应、全局异常、参数校验、Knife4j、traceId、单元测试骨架。

**不做（明确写出来，避免 AI 自行发挥）：**

- 多租户 / 店铺 / 商家入驻（前端是单店，没有 shop 概念）
- 真实微信、支付宝渠道对接（只做模拟渠道，但要留 SPI 接口）
- 秒杀、拼团、分销、结算分账
- 优惠券核销规则引擎（只做"我的券"查询与下单时简单满减，见 7.5）
- 小程序端、WebSocket、IM
- 分布式事务中间件（微服务阶段用本地消息表，见 9.5）

### 1.4 与前端的联调边界

前端目前是**纯静态数据**，不会主动请求后端。所以顺序是：

```
后端先跑通（Postman / curl 验收） → 再按第 11 章改前端数据源
```

不要在写后端时同步改前端，两边一起动会分不清是接口问题还是页面问题。

---

## 2. 关键决策（先读，避免返工）

| # | 决策 | 结论 | 理由 |
|---|---|---|---|
| D1 | 工程形态 | **单体 + Maven 多模块**，一个进程，7 个业务模块 | 一个人开发，先把业务跑通；模块边界按微服务切，后期拆出去不用重写 |
| D2 | 金额 | **数据库存 `bigint` 分；接口返回元（number，两位小数）** | 前端 `price` 是元，返回分会让每个页面都要除 100；内部计算一律用 `long` 分，只在 VO 层转元 |
| D3 | 商品主键 | 表用 `bigint` 自增，对外 `id` 用 **`code` varchar**，值等于前端的 `snacks-1` 这类 | 前端路由 `/item/:id` 和 `cart.lines[].id` 都当字符串用，换数字 ID 要改一堆逻辑 |
| D4 | 待购清单 | **不落库**。前端本地存，下单时把 `lines` 提交给后端，后端重新查价计价 | 它是"结算草稿"，不是业务实体；落库会引入过期、脏数据、多端冲突三个问题 |
| D5 | 购物车 | 服务端持久化，登录后合并本地未同步的行 | 换设备要能看到；这是电商标配 |
| D6 | 游客 | 真实建 `m_user` 行，`is_guest=1`，24 小时后由定时任务清理 | 游客也要能加购、看消息，用匿名 token 会让所有业务表都要兼容 null userId |
| D7 | 鉴权 | JWT 双 token（access 2h / refresh 14d），`Authorization: Bearer`，**不用 Spring Security 全家桶** | 引 `spring-security-crypto` 只用 BCrypt；鉴权用拦截器 + `UserContext`，代码量少一个数量级 |
| D8 | 支付 | 自建模拟渠道，`pay/mock/**`，状态机与幂等按真实渠道写 | 真实对接有资质门槛；模拟渠道能把回调、重复支付、退款三条链路走完 |
| D9 | AI | 后端持 DeepSeek Key，前端只调自家 `/assistant/chat` | Key 不能进前端 bundle；服务端可做限流、审计、上下文裁剪 |
| D10 | 缓存 | 类目/商品用 **Caffeine 本地缓存**；验证码、refresh token、限流用 Redis | 单体阶段本地缓存最快；微服务阶段换 Redis 共享 |
| D11 | 错误码 | 5 位数字业务码，HTTP 状态码只用 200/401/403/404/429/500 | 前端只判 `code === 0`，HTTP 语义保留给网关和监控 |
| D12 | 时间 | 接口一律返回**毫秒时间戳**（`xxxAt: 1730000000000`）；消息的 `time` 字段例外，由服务端格式化成"12 分钟前" | 对齐前端 `joinedAt: number`；时区问题留给前端 `new Date()` |

---

## 3. 技术栈与版本

### 3.1 版本矩阵（必须锁死，不要用 latest）

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | **21** | 本机已装；Spring Boot 3.2 官方支持 17–21，21 可用 |
| Maven | 3.9.x | IDEA 内置的也行，但建议用独立 Maven |
| Spring Boot | 3.2.12 | 与 SCA 2023.0.3.2 验证过的组合 |
| Spring Cloud | 2023.0.3 | |
| Spring Cloud Alibaba | 2023.0.3.2 | Nacos / Sentinel / Seata |
| MyBatis-Plus | 3.5.7 | 用 `mybatis-plus-spring-boot3-starter` |
| MySQL | 8.0.x | 本机已装，库名 `owl_mall` |
| Redis | 7.x | 本机已装 |
| Nacos | 2.3.x | 单体阶段只做配置中心，T9 才真正用注册中心 |
| JJWT | 0.12.5 | API 与 0.9 不同，见 8.2 |
| Knife4j | 4.5.0 | `knife4j-openapi3-jakarta-spring-boot-starter` |
| spring-security-crypto | 随 Boot | 只用 `BCryptPasswordEncoder` |
| Lombok | 1.18.34 | **必须 ≥ 1.18.30**，否则 JDK 21 下编译报 `NoSuchFieldError` |
| Caffeine | 3.1.8 | 本地缓存 |
| DeepSeek | OpenAI 兼容 API | `https://api.deepseek.com`，模型 `deepseek-chat` |

> 如果 Maven 拉不到某个版本，**降级整组**而不是单换一个：Boot 3.2.x ↔ Cloud 2023.0.x ↔ SCA 2023.0.3.x 是一组，混版本会出现 `NoSuchMethodError`。
>
> 实测拉不到 `com.alibaba.cloud:spring-cloud-alibaba-dependencies:2023.0.3.2` 时，换 `2023.0.3.1`（同组，兼容 Cloud 2023.0.3），**不要**改用 2022.x。

### 3.2 父 POM 关键片段

```xml
<properties>
  <java.version>21</java.version>
  <maven.compiler.release>21</maven.compiler.release>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <spring-boot.version>3.2.12</spring-boot.version>
  <spring-cloud.version>2023.0.3</spring-cloud.version>
  <spring-cloud-alibaba.version>2023.0.3.2</spring-cloud-alibaba.version>
  <mybatis-plus.version>3.5.7</mybatis-plus.version>
  <jjwt.version>0.12.5</jjwt.version>
  <knife4j.version>4.5.0</knife4j.version>
</properties>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
        <release>21</release>
        <!-- 关键：Spring 6 靠它读参数名，不加 @RequestParam 会报 -parameters 缺失 -->
        <parameters>true</parameters>
        <annotationProcessorPaths>
          <path>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.34</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>${spring-boot.version}</version>
      <type>pom</type><scope>import</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>${spring-cloud.version}</version>
      <type>pom</type><scope>import</scope>
    </dependency>
    <dependency>
      <groupId>com.alibaba.cloud</groupId>
      <artifactId>spring-cloud-alibaba-dependencies</artifactId>
      <version>${spring-cloud-alibaba.version}</version>
      <type>pom</type><scope>import</scope>
    </dependency>
    <dependency>
      <groupId>com.baomidou</groupId>
      <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
      <version>${mybatis-plus.version}</version>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 3.3 每个模块的基础依赖

```xml
<!-- 所有业务模块 -->
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>
  <dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
  </dependency>
  <dependency>
    <groupId>com.owl</groupId>
    <artifactId>owl-common</artifactId>
    <version>${project.version}</version>
  </dependency>
  <dependency>
    <groupId>com.owl</groupId>
    <artifactId>owl-infra</artifactId>
    <version>${project.version}</version>
  </dependency>
  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
  </dependency>
</dependencies>
```

**禁止**：在业务模块里写 `<version>`（统一由 BOM 管）。

### 3.4 JDK 21 + IDEA 配置（照着点一遍，省半天）

**一、IDEA 四处都要设成 21，少一处就报 "无效的目标发行版"**

| 位置 | 设置 |
|---|---|
| `File → Project Structure → Project` | SDK = 21，Language level = 21 |
| `File → Project Structure → Modules` | 每个模块的 Language level = 21 |
| `Settings → Build, Execution, Deployment → Build Tools → Maven → Runner` | JRE = 21 |
| `Settings → Build, Execution, Deployment → Compiler → Java Compiler` | Project bytecode version = 21 |

**二、编码**

`Settings → Editor → File Encodings`：Global / Project / Properties 全部 `UTF-8`，勾上 `Transparent native-to-ascii conversion`。

Maven `pom.xml` 里已有 `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`，别漏。

**三、Lombok 注解处理器**

`Settings → Build, Execution, Deployment → Compiler → Annotation Processors` → 勾 `Enable annotation processing`。

IDEA 2023.3+ 自带 Lombok 插件，不用装；如果 `@Data` 标红但能编译，是插件没启用，去 `Plugins` 里确认。

**四、Nacos 在 JDK 17+ 必须加 `--add-opens`**

Nacos 2.3 客户端用反射读 `java.base` 内部类，JDK 21 默认禁止，会报：

```
java.lang.reflect.InaccessibleObjectException:
Unable to make field private volatile java.lang.Thread java.lang.ThreadLocal$ThreadLocalMap...
```

两个地方都要加（IDEA 运行配置的 `VM options`）：

```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/java.math=ALL-UNNAMED
--add-opens java.base/java.net=ALL-UNNAMED
```

打包成 jar 后用 `java --add-opens ... -jar owl-boot.jar`，或者写进 `JAVA_TOOL_OPTIONS`。

> 单体阶段（T1–T9）不引 Nacos，**加了这些参数也没有副作用**，可以一开始就配上，省得到 T10 才踩。

**五、虚拟线程：先别开**

JDK 21 支持 `spring.threads.virtual.enabled=true`，但本项目全是 JDBC 阻塞调用，虚拟线程不会提升吞吐，反而会让 Hikari 连接池（默认 10）瞬间打满，报 `Connection is not available, request timed out`。

建议保持默认（平台线程），等微服务阶段 + Redis 缓存上来再评估。

**六、IDEA 常见报错对照**

| 现象 | 原因 | 处理 |
|---|---|---|
| `无效的目标发行版: 21` | 编译器 bytecode 还是 17 | 上面第一张表四处都改成 21 |
| `java: 找不到符号 @Data` | 注解处理器没开 | 第三条 |
| `Lombok requires enabled annotation processing` | 同上 | 同上 |
| 启动报 `Unsupported class file major version 65` | 某个依赖是 JDK 21 编译的，但运行 JRE 是 17 | 检查 Run Configuration 的 JRE |
| `-parameters` 缺失警告 | 父 POM 没加 `maven-compiler-plugin` 的 `parameters` | 见 3.2 |
| 中文乱码（接口返回 `????`） | 文件编码或连接串 | File Encodings + `characterEncoding=utf8` |
| Maven 下载卡住 | 没用国内镜像 | `settings.xml` 加阿里云 mirror |

---

## 4. 工程结构

### 4.1 目录树（照建）

```
E:\OWL-System\
├─ owl-web\                          # 已有前端
├─ docs\
└─ owl-server\                       # 新建
   ├─ pom.xml                        # packaging=pom，聚合 + 版本管理
   ├─ owl-dependencies\pom.xml        # 可选
   ├─ owl-common\                     # 纯 POJO 与工具，不依赖 Spring Web
   │  └─ src/main/java/com/owl/common/
   │     ├─ api/          Result, PageResult, ErrorCode, BizException
   │     ├─ context/      UserContext, LoginUser
   │     ├─ util/         PriceUtil, OrderNoGenerator, IdUtils
   │     └─ validation/   自定义校验注解（手机号、密码强度）
   ├─ owl-infra\                      # 中间件 + Web 基础设施，被业务模块依赖
   │  └─ src/main/java/com/owl/infra/
   │     ├─ cache/        CacheService（Caffeine 实现）
   │     ├─ redis/        RedisService, RedisKey
   │     ├─ security/     JwtService, PasswordService, JwtProperties
   │     ├─ storage/      FileStorage（本地静态目录）
   │     ├─ ai/           AiClient, DeepSeekClient
   │     └─ web/          GlobalExceptionHandler, TraceFilter, WebConfig, JwtAuthInterceptor
   ├─ owl-modules\
   │  ├─ owl-auth\        # 用户、认证、地址
   │  ├─ owl-catalog\     # 类目、商品、库存
   │  ├─ owl-trade\       # 购物车、结算、订单
   │  ├─ owl-payment\     # 支付、退款（模拟）
   │  ├─ owl-message\     # 站内信
   │  └─ owl-assistant\   # AI 助手
   ├─ owl-admin\                      # B 端接口，依赖上面所有模块
   └─ owl-boot\                       # 启动模块
      └─ src/main/
         ├─ java/com/owl/
         │  ├─ OwlApplication.java
         │  └─ controller/HealthController.java
         └─ resources/
            ├─ application.yml
            ├─ application-dev.yml
            ├─ application-prod.yml
            ├─ seed/catalog.json       # 商品初始化数据
            └─ static/products/*.jpg   # 从 owl-web 复制过来的 105 张图
```

> **`owl-common` 与 `owl-infra` 的边界**：`common` 只有"不依赖 Spring 也能编译"的东西（POJO、枚举、工具类）；`GlobalExceptionHandler` / 拦截器 / `WebMvcConfigurer` 这些必须依赖 `spring-web`，一律放 `infra/web`。业务模块同时依赖这两个模块。

### 4.2 模块内部包结构（每个业务模块一致）

以 `owl-catalog` 为例：

```
com.owl.catalog
├─ controller/        # 只做参数接收与响应封装，禁止写业务
│  ├─ CategoryController.java
│  └─ ProductController.java
├─ service/           # 接口
│  ├─ CategoryService.java
│  └─ impl/           # 实现，事务边界在这一层
├─ mapper/            # MyBatis-Plus Mapper，只写 SQL
├─ entity/            # 与表一一对应，禁止直接返回给前端
├─ dto/               # 入参（XxxRequest），带校验注解
├─ vo/                # 出参（XxxVO），字段名对齐前端
├─ converter/         # entity <-> vo 转换
├─ enums/             # 本模块枚举
└─ config/            # 本模块专属配置
```

> DTO / VO 的完整清单（30 个 DTO、39 个 VO，含字段与用途）见 `docs/API契约-DTO-VO.md`；
> 接口本身以 `docs/owl-mall-openapi.json` 为准，Apifox 导入这个文件即可。

### 4.3 分层规则（ArchUnit 会查，先记住）

1. `controller` **禁止**注入 `Mapper`，只能注入 `Service`。
2. `service` **禁止**返回 `entity`，必须转 `vo`。
3. `mapper` **禁止**被其它模块引用（跨模块只能调 Service 接口）。
4. `entity` **禁止**出现在 `controller` 的方法签名里。
5. 模块之间**禁止**直接查对方的表；需要数据就调对方 Service。
6. 事务注解 `@Transactional` 只加在 `service.impl` 的**public 方法**上，**禁止**加在 Controller。

### 4.4 配置样例

`application.yml`：

```yaml
server:
  port: 8080

spring:
  application:
    name: owl-server
  profiles:
    active: dev
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null
  servlet:
    multipart:
      max-file-size: 5MB
  mvc:
    static-path-pattern: /static/**

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

owl:
  jwt:
    secret: ${OWL_JWT_SECRET:owl-mall-dev-secret-please-change-in-prod-32chars}
    access-ttl: 7200          # 秒
    refresh-ttl: 1209600      # 14 天
  shipping:
    free-threshold-cent: 9900 # 满 99 元免运费
    fee-cent: 500             # 不满收 5 元
  ai:
    base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
    api-key: ${DEEPSEEK_API_KEY:}
    model: deepseek-chat
    timeout-ms: 30000

knife4j:
  enable: true
  setting:
    language: zh_cn

springdoc:
  swagger-ui:
    path: /doc.html
```

`application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/owl_mall?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000ms

logging:
  level:
    com.owl: debug
```

> **为什么不配 `server.servlet.context-path`**：context-path 会连静态资源一起加前缀，商品图会变成 `/api/v1/static/products/x.jpg`，前端和 Vite 代理都要跟着改。这里改用 `configurePathMatch` 只给 `@RestController` 加 `/api/v1` 前缀，静态资源仍然是 `/static/**`。
>
> `SpringBoot 3` 的写法（放在 `owl-infra/web/WebConfig`）：
>
> ```java
> @Configuration
> public class WebConfig implements WebMvcConfigurer {
>     @Override
>     public void configurePathMatch(PathMatchConfigurer configurer) {
>         configurer.addPathPrefix("/api/v1",
>                 HandlerTypePredicate.forAnnotation(RestController.class));
>     }
> }
> ```
>
> 这样 Controller 上**只写** `@RequestMapping("/auth")`，实际路径自动是 `/api/v1/auth`；`@RequestMapping("/health")` 也不会被加两次前缀（`HealthController` 若不在 `com.owl` 扫描范围内要单独处理）。

### 4.5 启动类

```java
@SpringBootApplication(scanBasePackages = "com.owl")
@MapperScan("com.owl.**.mapper")
@EnableCaching
public class OwlApplication {
    public static void main(String[] args) {
        SpringApplication.run(OwlApplication.class, args);
    }
}
```

---

## 5. 数据库设计

### 5.1 设计约定

1. 库名 `owl_mall`，字符集 `utf8mb4`，排序 `utf8mb4_0900_ai_ci`。
2. **金额一律 `bigint`，单位分**。字段名以 `_cent` 结尾。
3. 状态用 `tinyint` + 注释，不用字符串（唯一例外：`n_message.kind`、`m_user.level`，因为前端直接传字符串）。
4. 每张表必备 `created_at` / `updated_at`；业务表加 `deleted`（0 正常 / 1 删除）。
5. **不建物理外键**，关联由应用层保证。
6. 索引命名 `idx_表名_字段`，唯一索引 `uk_表名_字段`。
7. 购物车行删除用**物理删除**（逻辑删除会让 `uk_user_product` 冲突）。

### 5.2 ER 概览

```
m_user 1──n m_user_address
   │ 1
   ├──n t_cart_item ──n c_product ──1 c_category ──1..n c_sub_category
   ├──n t_order 1──n t_order_item
   │        │ 1
   │        └──n p_payment 1──n p_refund
   ├──n mk_user_coupon ──1 mk_coupon
   ├──n n_message
   └──n ai_usage_log
c_product 1──1 c_product_detail
c_product 1──n c_stock_log
```

### 5.3 建表脚本

`owl-boot/src/main/resources/db/01_schema.sql`，**完整可执行**：

```sql
CREATE DATABASE IF NOT EXISTS owl_mall
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE owl_mall;

-- ============ 会员 ============
CREATE TABLE IF NOT EXISTS m_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  phone         VARCHAR(20)  NULL COMMENT '手机号，邮箱注册为 NULL',
  email         VARCHAR(120) NULL COMMENT '邮箱，手机注册为 NULL',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt；游客为随机串',
  nickname      VARCHAR(32)  NOT NULL COMMENT '昵称，对应前端 name',
  avatar        VARCHAR(255) NULL,
  level         VARCHAR(16)  NOT NULL DEFAULT 'normal' COMMENT 'black/normal/guest',
  member_no     VARCHAR(32)  NULL COMMENT 'OWL-2026-1234，游客为 NULL',
  points        INT          NOT NULL DEFAULT 0 COMMENT '积分',
  balance_cent  BIGINT       NOT NULL DEFAULT 0 COMMENT '余额，分',
  is_guest      TINYINT      NOT NULL DEFAULT 0 COMMENT '1=游客',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  last_login_at DATETIME     NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_phone (phone),
  UNIQUE KEY uk_user_email (email),
  UNIQUE KEY uk_user_member_no (member_no),
  KEY idx_user_guest (is_guest, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE IF NOT EXISTS m_user_address (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL,
  receiver   VARCHAR(32) NOT NULL COMMENT '收货人',
  phone      VARCHAR(20) NOT NULL,
  province   VARCHAR(32) NOT NULL,
  city       VARCHAR(32) NOT NULL,
  district   VARCHAR(32) NOT NULL,
  detail     VARCHAR(128) NOT NULL,
  tag        VARCHAR(16) NULL COMMENT '家/公司',
  is_default TINYINT     NOT NULL DEFAULT 0,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_addr_user (user_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址';

CREATE TABLE IF NOT EXISTS m_points_log (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL,
  change_val INT         NOT NULL COMMENT '正加负减',
  type       TINYINT     NOT NULL COMMENT '1下单 2评价 3退款扣回 4后台调整',
  ref_no     VARCHAR(32) NULL COMMENT '关联订单号',
  remark     VARCHAR(128) NULL,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_points_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

-- ============ 商品 ============
CREATE TABLE IF NOT EXISTS c_category (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  code       VARCHAR(32) NOT NULL COMMENT 'snacks/meat/...',
  name       VARCHAR(32) NOT NULL,
  hint       VARCHAR(64) NOT NULL COMMENT '左栏副文案',
  glyph      VARCHAR(1024) NULL COMMENT '内联 SVG path',
  sort       INT         NOT NULL DEFAULT 0,
  status     TINYINT     NOT NULL DEFAULT 1,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一级类目';

CREATE TABLE IF NOT EXISTS c_sub_category (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  category_id BIGINT      NOT NULL,
  code        VARCHAR(32) NOT NULL COMMENT 'nut/seed/...',
  name        VARCHAR(32) NOT NULL,
  sort        INT         NOT NULL DEFAULT 0,
  status      TINYINT     NOT NULL DEFAULT 1,
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sub_cat_code (category_id, code),
  KEY idx_sub_category (category_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二级细分';

CREATE TABLE IF NOT EXISTS c_product (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  code        VARCHAR(64)  NOT NULL COMMENT '对外 id，如 snacks-1',
  category_id BIGINT       NOT NULL,
  sub_id      BIGINT       NOT NULL,
  name        VARCHAR(128) NOT NULL,
  spec        VARCHAR(64)  NOT NULL COMMENT '规格文案',
  price_cent  BIGINT       NOT NULL COMMENT '售价，分',
  list_cent   BIGINT       NOT NULL COMMENT '划线价，分',
  tag         VARCHAR(16)  NULL COMMENT '会员价/人气/新品',
  emoji       VARCHAR(16)  NULL COMMENT '无图时回落',
  image       VARCHAR(255) NULL COMMENT '如 /static/products/snacks-1.jpg',
  stock       INT          NOT NULL DEFAULT 0,
  sales       INT          NOT NULL DEFAULT 0,
  sort        INT          NOT NULL DEFAULT 0,
  is_featured TINYINT      NOT NULL DEFAULT 0 COMMENT '1=首页推荐位',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  version     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_code (code),
  KEY idx_product_cat (category_id, status, sort),
  KEY idx_product_sub (sub_id, status),
  KEY idx_product_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品（单规格）';

CREATE TABLE IF NOT EXISTS c_product_detail (
  product_id BIGINT       NOT NULL,
  slogan     VARCHAR(128) NOT NULL COMMENT '一句话卖点',
  intro      TEXT         NOT NULL,
  facts_json JSON         NULL COMMENT '{"产地":"辽宁"} 有序对象',
  tips_json  JSON         NULL COMMENT '["贴士1","贴士2"]',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品详情';

CREATE TABLE IF NOT EXISTS c_stock_log (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  product_id BIGINT      NOT NULL,
  change_qty INT         NOT NULL COMMENT '负数为扣减',
  type       TINYINT     NOT NULL COMMENT '1下单 2取消回补 3入库 4后台调整',
  ref_no     VARCHAR(64) NULL,
  before_qty INT         NOT NULL,
  after_qty  INT         NOT NULL,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_stock_product (product_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水';

-- ============ 交易 ============
CREATE TABLE IF NOT EXISTS t_cart_item (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  user_id    BIGINT   NOT NULL,
  product_id BIGINT   NOT NULL,
  qty        INT      NOT NULL DEFAULT 1,
  checked    TINYINT  NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cart_user_product (user_id, product_id),
  KEY idx_cart_user (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车行（物理删除）';

CREATE TABLE IF NOT EXISTS t_order (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  order_no          VARCHAR(32)  NOT NULL COMMENT 'OWL + yyyyMMdd + 雪花后6位',
  user_id           BIGINT       NOT NULL,
  status            TINYINT      NOT NULL DEFAULT 10 COMMENT '10待付款 20待发货 30待收货 40已完成 50已取消 60退款中 70已退款',
  pay_status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0未支付 1已支付 2已退款',
  goods_amount_cent BIGINT       NOT NULL COMMENT '商品合计',
  discount_cent     BIGINT       NOT NULL DEFAULT 0 COMMENT '优惠合计',
  ship_fee_cent     BIGINT       NOT NULL DEFAULT 0,
  pay_amount_cent   BIGINT       NOT NULL COMMENT '应付 = 商品 - 优惠 + 运费',
  coupon_id         BIGINT       NULL COMMENT 'mk_user_coupon.id',
  receiver          VARCHAR(32)  NOT NULL,
  phone             VARCHAR(20)  NOT NULL,
  address_snapshot  JSON         NOT NULL COMMENT '下单时地址快照',
  delivery_type     TINYINT      NOT NULL DEFAULT 1 COMMENT '1极速达 2次日达 3自提',
  remark            VARCHAR(255) NULL,
  review_status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0未评价 1已评价',
  pay_time          DATETIME     NULL,
  deliver_time      DATETIME     NULL,
  receive_time      DATETIME     NULL,
  close_time        DATETIME     NULL,
  cancel_reason     VARCHAR(128) NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted           TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_user (user_id, status, created_at),
  KEY idx_order_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

CREATE TABLE IF NOT EXISTS t_order_item (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  order_no        VARCHAR(32)  NOT NULL,
  product_id      BIGINT       NOT NULL,
  product_code    VARCHAR(64)  NOT NULL,
  name_snapshot   VARCHAR(128) NOT NULL,
  spec_snapshot   VARCHAR(64)  NULL,
  image_snapshot  VARCHAR(255) NULL,
  price_cent      BIGINT       NOT NULL,
  list_cent       BIGINT       NOT NULL,
  qty             INT          NOT NULL,
  total_cent      BIGINT       NOT NULL,
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_item_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单行';

CREATE TABLE IF NOT EXISTS t_order_status_log (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  order_no      VARCHAR(32) NOT NULL,
  from_status   TINYINT     NOT NULL,
  to_status     TINYINT     NOT NULL,
  operator_type TINYINT     NOT NULL COMMENT '1用户 2系统 3管理员',
  operator_id   BIGINT      NULL,
  remark        VARCHAR(128) NULL,
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_olog_order (order_no, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态流水';

-- ============ 支付 ============
CREATE TABLE IF NOT EXISTS p_payment (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  pay_no         VARCHAR(32) NOT NULL COMMENT 'P + yyyyMMddHHmmss + 6位',
  order_no       VARCHAR(32) NOT NULL,
  user_id        BIGINT      NOT NULL,
  channel        VARCHAR(16) NOT NULL DEFAULT 'mock' COMMENT 'mock/wechat/alipay',
  amount_cent    BIGINT      NOT NULL,
  status         TINYINT     NOT NULL DEFAULT 0 COMMENT '0待支付 1成功 2失败 3关闭 4已退款',
  out_trade_no   VARCHAR(64) NULL COMMENT '渠道流水号',
  paid_at        DATETIME    NULL,
  expire_at      DATETIME    NULL,
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pay_no (pay_no),
  KEY idx_pay_order (order_no, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单';

CREATE TABLE IF NOT EXISTS p_refund (
  id                BIGINT       NOT NULL AUTO_INCREMENT,
  refund_no         VARCHAR(32)  NOT NULL,
  pay_no            VARCHAR(32)  NOT NULL,
  order_no          VARCHAR(32)  NOT NULL,
  user_id           BIGINT       NOT NULL,
  amount_cent       BIGINT       NOT NULL,
  reason            VARCHAR(128) NULL,
  status            TINYINT      NOT NULL DEFAULT 0 COMMENT '0处理中 1成功 2失败',
  channel_refund_no VARCHAR(64)  NULL,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_refund_no (refund_no),
  KEY idx_refund_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款单';

-- ============ 营销 ============
CREATE TABLE IF NOT EXISTS mk_coupon (
  id              BIGINT      NOT NULL AUTO_INCREMENT,
  code            VARCHAR(32) NOT NULL,
  name            VARCHAR(64) NOT NULL,
  type            TINYINT     NOT NULL DEFAULT 1 COMMENT '1满减 2折扣',
  threshold_cent  BIGINT      NOT NULL DEFAULT 0 COMMENT '门槛，0 无门槛',
  discount_cent   BIGINT      NOT NULL DEFAULT 0 COMMENT '满减金额',
  discount_rate   INT         NULL COMMENT '折扣：85=8.5折',
  start_at        DATETIME    NOT NULL,
  end_at          DATETIME    NOT NULL,
  total           INT         NOT NULL DEFAULT 0,
  received        INT         NOT NULL DEFAULT 0,
  status          TINYINT     NOT NULL DEFAULT 1,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_coupon_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

CREATE TABLE IF NOT EXISTS mk_user_coupon (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL,
  coupon_id   BIGINT      NOT NULL,
  status      TINYINT     NOT NULL DEFAULT 0 COMMENT '0未用 1已用 2过期',
  order_no    VARCHAR(32) NULL,
  received_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used_at     DATETIME    NULL,
  PRIMARY KEY (id),
  KEY idx_uc_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户券';

-- ============ 消息 ============
CREATE TABLE IF NOT EXISTS n_message (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL,
  kind       VARCHAR(16) NOT NULL COMMENT 'order/promo/system，直接对齐前端',
  title      VARCHAR(64) NOT NULL,
  body       VARCHAR(512) NOT NULL,
  biz_no     VARCHAR(32) NULL COMMENT '关联订单号，点进去能跳',
  read_flag  TINYINT     NOT NULL DEFAULT 0,
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted    TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_msg_user (user_id, read_flag, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信';

-- ============ 后台 ============
CREATE TABLE IF NOT EXISTS s_admin (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(32)  NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  nickname      VARCHAR(32)  NOT NULL,
  role          VARCHAR(16)  NOT NULL DEFAULT 'ops' COMMENT 'super/ops',
  status        TINYINT      NOT NULL DEFAULT 1,
  last_login_at DATETIME     NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员';

CREATE TABLE IF NOT EXISTS s_admin_log (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  admin_id   BIGINT       NOT NULL,
  action     VARCHAR(64)  NOT NULL COMMENT 'PRODUCT_UPDATE/ORDER_DELIVER',
  target     VARCHAR(64)  NULL,
  detail     VARCHAR(512) NULL,
  ip         VARCHAR(64)  NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_alog_admin (admin_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作日志';

CREATE TABLE IF NOT EXISTS ai_usage_log (
  id                BIGINT      NOT NULL AUTO_INCREMENT,
  user_id           BIGINT      NULL,
  model             VARCHAR(32) NOT NULL,
  prompt_tokens     INT         NOT NULL DEFAULT 0,
  completion_tokens INT         NOT NULL DEFAULT 0,
  latency_ms        INT         NOT NULL DEFAULT 0,
  success           TINYINT     NOT NULL DEFAULT 1,
  created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ai_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用流水';
```

### 5.4 商品数据初始化（不要手写 90 条 INSERT）

前端 `catalog.ts` 是 TS 字面量，还用了 `import.meta.glob` 索引图片，普通 Node 直接跑会报错。仓库里已经放好脚本并**实测通过**：

```
owl-web/scripts/export-catalog.mjs
```

它借 Vite 的 SSR 加载器（`server.ssrLoadModule`）来执行 TS，天然支持 glob 与路径别名，不需要额外装 `vite-node`。

**第 1 步：导出 JSON**

```bash
cd E:/OWL-System/owl-web
node scripts/export-catalog.mjs
# 实测输出：catalog exported: 15 categories / 120 subs / 90 products
```

产出 `E:/OWL-System/docs/catalog.json`。

脚本核心逻辑（要改字段就改这里）：

```js
const mod = await server.ssrLoadModule('/src/views/home/catalog.ts')
const out = mod.CATEGORIES.map((cat, ci) => ({
  code: cat.id, name: cat.name, hint: cat.hint, glyph: cat.glyph, sort: ci * 10,
  subs: cat.subs.map((s, si) => ({ code: s.id, name: s.name, sort: si * 10 })),
  products: cat.items.map((p, pi) => ({
    code: p.id, name: p.name, spec: p.spec, price: p.price, list: p.list,
    tag: p.tag, emoji: p.emoji, sub: p.sub, sort: pi * 10, detail: p.detail,
  })),
}))
writeFileSync(resolve(root, '../docs/catalog.json'), JSON.stringify(out, null, 2), 'utf8')
```

> **数据基数（不要凭印象写）**：15 个大类 / 120 个细分 / 90 件商品（每类 6 件）。
> 前端首页那句"N 个大类 · M 个细分"是 `CATEGORIES.length` 与 `totalSubs` 动态算的，显示 **15 个大类 · 120 个细分**。
> 唯一写死"14 个大类"的是 `services/assistant.ts:76` 的欢迎语，属于前端遗留文案，不影响后端。

**第 2 步：把 JSON 放到 `owl-boot/src/main/resources/seed/catalog.json`**

```bash
mkdir -p E:/OWL-System/owl-server/owl-boot/src/main/resources/seed
cp E:/OWL-System/docs/catalog.json \
   E:/OWL-System/owl-server/owl-boot/src/main/resources/seed/catalog.json
```

**第 3 步：图片复制到 `owl-boot/src/main/resources/static/products/`**

```bash
mkdir -p E:/OWL-System/owl-server/owl-boot/src/main/resources/static/products
cp E:/OWL-System/owl-web/src/assets/products/*.jpg \
   E:/OWL-System/owl-server/owl-boot/src/main/resources/static/products/
ls E:/OWL-System/owl-server/owl-boot/src/main/resources/static/products | wc -l   # 期望 105
```

**第 4 步：`CatalogSeeder`（`ApplicationRunner`，幂等）**

规则：

1. 启动时读 `classpath:seed/catalog.json`。
2. 按 `code` upsert 类目、细分、商品、详情（已存在就跳过，不覆盖后台改过的数据）。
3. `sort` 用数组下标 × 10。
4. `image` 写 `/static/products/{code}.jpg`。
5. `stock` 初始给 200，`sales` 给 0～50 的伪随机（固定 seed，保证每次重启一致）。
6. 全部完成后打一行日志：`catalog seeded: 15 categories / 120 subs / 90 products`。
7. 首页推荐位：按前端 `FEATURED_IDS` 那 12 件的 `code` 建一个常量集合，命中的置 `is_featured=1`，其余 0。**不要**用"前 12 条"代替，前端是跨类目挑的。
8. 加开关 `owl.seed.enabled=true`，生产置 false。

**第 5 步：细分件数不要存表**

前端 `Sub.count` 是实时算的。后端查询时用聚合：

```sql
SELECT s.id, s.code, s.name,
       COUNT(p.id) AS cnt
FROM c_sub_category s
LEFT JOIN c_product p ON p.sub_id = s.id AND p.status = 1 AND p.deleted = 0
WHERE s.category_id = #{categoryId}
GROUP BY s.id, s.code, s.name
ORDER BY s.sort
```

`cnt = 0` 保留该细分（前端要显示"补货中"），**禁止**在 SQL 里过滤掉 0。

### 5.5 索引与查询注意

1. 搜索用 `LIKE '%kw%'`，90 条数据全表扫也无所谓。**禁止**为此上 Elasticsearch。
2. 类目树查询一次查完再在内存里组装，**禁止**在循环里查数据库（N+1）。
3. `t_order` 按 `(user_id, status, created_at)` 建了联合索引，列表查询必须带 `user_id` 且 `ORDER BY created_at DESC`。
4. 金额排序、比较全部在 SQL 里用 `_cent` 字段，不要在 Java 里转元再比。

### 5.6 初始化数据（`02_init.sql`）

只放**静态配置**，密码一律不写死在 SQL 里（BCrypt 哈希手写不了）。

```sql
USE owl_mall;

-- 新人券模板：满 99 减 10
INSERT INTO mk_coupon (code, name, type, threshold_cent, discount_cent, start_at, end_at, total, received, status)
VALUES ('NEW10', '新人券 满 99 减 10', 1, 9900, 1000, NOW(), DATE_ADD(NOW(), INTERVAL 365 DAY), 100000, 0, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), threshold_cent = VALUES(threshold_cent);

-- 满 199 减 30（演示"我的券"有多张）
INSERT INTO mk_coupon (code, name, type, threshold_cent, discount_cent, start_at, end_at, total, received, status)
VALUES ('FULL30', '满 199 减 30', 1, 19900, 3000, NOW(), DATE_ADD(NOW(), INTERVAL 90 DAY), 50000, 0, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);
```

`DataSeeder`（`ApplicationRunner`，`owl.seed.enabled=true` 时执行）负责：

| 数据 | 规则 |
|---|---|
| 管理员 | `username=admin`，密码 `admin123456`，role `super`；已存在则跳过，**不覆盖已改的密码** |
| 测试用户 | `13800138000 / abc12345`，昵称"罗耀"，level `black`，points 2860，balance 12050 分 |
| 测试用户券 | 各发 1 张 `NEW10`、1 张 `FULL30`（`mk_user_coupon`） |
| 测试地址 | 浙江省杭州市西湖区文三路 88 号 OWL 公寓 3 幢 1201 室，`is_default=1` |

密码统一 `passwordEncoder.encode(raw)` 后写入，日志只打印"admin seeded"，**不打印密码**。

> 这段数据是为了 curl 验收方便。生产环境把 `owl.seed.enabled` 置 false。

---

## 6. 接口契约

### 6.1 通用约定

**Base URL**：`http://localhost:8080/api/v1`（由 `WebConfig.addPathPrefix` 统一加前缀，Controller 上不再写 `/api/v1`）。

**统一响应体**：

```json
{ "code": 0, "message": "ok", "data": { }, "traceId": "b7f3c2d1e4a5" }
```

- `code = 0` 表示成功，其它见附录 A。
- 业务失败 `data` 为 `null`，HTTP 仍返回 200（401/403 除外，交给网关）。
- `traceId` 由 `TraceFilter` 生成，写入 MDC，日志格式带 `[%X{traceId}]`。

**分页响应**：

```json
{ "list": [], "total": 90, "page": 1, "size": 20, "pages": 5 }
```

**鉴权头**：

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9....
```

**金额**：所有 `price` / `amount` / `payable` 字段都是**元**，JSON number，最多两位小数。
**时间**：所有 `xxxAt` 字段是**毫秒时间戳**（number）。
**ID**：用户 `id`、商品 `id`、类目 `id`、细分 `id` 全部是 **string**，禁止返回数字（前端 `normalize()` 会因类型不符丢弃会话）。

**幂等**：写接口凡是可能重复提交的（创建订单、支付回调、退款），必须带幂等键或做状态机幂等，见 7.4。

**C 端 vs B 端**：C 端走 `UserContext`（`m_user`），B 端走 `AdminContext`（`s_admin`），两套拦截器、两套 token，互不通用。

---

### 6.2 认证模块 `/auth`

#### 6.2.1 发送短信验证码

```
POST /auth/sms-code        公开
```

请求：

```json
{ "phone": "13800138000", "scene": "register" }
```

响应：

```json
{ "expireIn": 300, "code": "482913" }
```

规则：

1. `phone` 必须匹配 `^1[3-9]\d{9}$`，否则 10001。
2. `scene` 目前只允许 `register`。
3. 同一手机号 60 秒内重复请求 → 10006，返回"请 60 秒后再试"。
4. 同一 IP 每小时上限 20 条（Redis 计数）。
5. 验证码 6 位数字，存 Redis `owl:sms:code:{scene}:{phone}`，TTL 300 秒。
6. **dev 环境把 `code` 回传**（方便联调），prod 返回 `null`。用 `@Profile` 或配置开关控制，不要留后门。

#### 6.2.2 注册

```
POST /auth/register        公开
```

请求：

```json
{
  "name": "罗耀",
  "phone": "13800138000",
  "code": "482913",
  "password": "abc12345",
  "agree": true
}
```

响应：

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "expiresIn": 7200,
  "user": {
    "id": "1001",
    "name": "罗耀",
    "phone": "13800138000",
    "email": "",
    "level": "normal",
    "memberNo": "OWL-2026-4821",
    "points": 0,
    "coupons": 1,
    "balance": 0,
    "joinedAt": 1730100000000,
    "isGuest": false
  }
}
```

规则：

1. `agree` 必须为 `true`，否则 10001 "请先同意用户协议"。
2. 手机号已存在 → 20003。
3. 密码 8-20 位，必须同时含字母和数字，否则 10001。
4. 验证码错误或过期 → 20002，错误 5 次作废。
5. 注册成功：`level = normal`（消费满 1000 元升 `black`，由定时任务算）、`memberNo = OWL-{年}-{4 位随机}`、发 1 张新人券（`mk_coupon` 里 `code='NEW10'`，满 99 减 10）、写一条 `system` 消息"欢迎加入 OWL"。
6. 密码用 `BCryptPasswordEncoder` 加密，`strength=10`。

#### 6.2.3 登录

```
POST /auth/login        公开
```

请求：

```json
{ "account": "13800138000", "password": "abc12345", "remember": true }
```

响应：同 6.2.2。

规则：

1. `account` 可以是手机号或邮箱，服务端判断格式后分别查 `phone` / `email`。
2. 账号不存在或密码错误，**统一返回 20001 "账号或密码错误"**（不要提示哪个错，防枚举）。
3. 连续错 5 次，锁 15 分钟（Redis `owl:auth:fail:{account}`），返回 20005。
4. 账号被禁用 → 20004。
5. 登录成功清失败计数，更新 `last_login_at`。
6. `remember` 只影响前端存储位置，后端 token TTL 不变。

#### 6.2.4 游客登录

```
POST /auth/guest        公开
```

请求：无 body。

响应：同 6.2.2，`user.isGuest = true`、`level = "guest"`、`name = "访客"`、`memberNo = ""`、`points/coupons/balance = 0`。

规则：

1. 每次调用新建一个游客用户（`phone/email = NULL`，`password_hash` 写随机 UUID 的 BCrypt）。
2. 游客可以加购、下单（模拟支付）、看消息；**不可**修改密码、不可评价。
3. 定时任务每天 03:00 清理 `is_guest=1 AND created_at < NOW() - INTERVAL 24 HOUR` 的用户及其购物车、消息。订单保留（有金额流水），`user_id` 不做处理。

#### 6.2.5 刷新 token

```
POST /auth/refresh        公开（凭 refreshToken）
```

请求：

```json
{ "refreshToken": "eyJhbGciOi..." }
```

响应：

```json
{ "accessToken": "eyJ...", "refreshToken": "eyJ...", "expiresIn": 7200 }
```

规则：

1. refreshToken 必须 `type=refresh`，且 Redis 里存在 `owl:auth:refresh:{userId}:{jti}`。
2. **轮换**：刷新成功后删除旧 jti，签发新的一对。
3. refresh 过期或不存在 → 10002，前端跳登录页。

#### 6.2.6 退出登录

```
POST /auth/logout        需登录
```

响应：`data = null`。

规则：删除该用户全部 refresh token；当前 access token 剩余 TTL 写入黑名单 `owl:auth:black:{jti}`。

#### 6.2.7 当前用户

```
GET /auth/me        需登录
```

响应：`user` 对象（同 6.2.2 的 `user`）。

#### 6.2.8 修改资料 / 改密码

```
PATCH /auth/me           需登录
PUT   /auth/password     需登录
```

`PATCH /auth/me` 请求（字段都可选，只改传入的）：

```json
{ "nickname": "阿枢", "email": "a@b.com", "avatar": "https://..." }
```

`PUT /auth/password` 请求：

```json
{ "oldPassword": "abc12345", "newPassword": "xyz98765" }
```

规则：改密码校验旧密码，成功后**踢掉所有 refresh token**（强制重新登录）。

---

### 6.3 商品模块 `/c`

#### 6.3.1 首页聚合

```
GET /c/home        公开
```

响应：

```json
{
  "brand": { "name": "OWL 商城", "desc": "15 个大类 · 120 个细分 · 最快 1 小时送达" },
  "categories": [
    {
      "id": "snacks", "name": "零食", "hint": "坚果 · 炒货 · 果干",
      "glyph": "<path d=\"...\"/>",
      "productCount": 6,
      "subs": [ { "id": "nut", "name": "坚果炒货", "count": 2 }, { "id": "seed", "name": "瓜子松子", "count": 0 } ]
    }
  ],
  "featured": [ { "id": "snacks-1", "name": "每日混合坚果 750g", "price": 99.00 } ],
  "freeShippingAt": 99,
  "shippingText": "满 ¥99 免运费 · 最快 1 小时送达"
}
```

规则：

1. `categories` 返回 15 个真实类目（不含"推荐"），每个带全部 8 个细分（**含 count=0**，前端要显示"补货中"）。
2. `featured` 返回 `is_featured=1` 且上架的商品，最多 12 件，按 `sort` 升序。**每个元素都是完整 `Product`**（示例里为了篇幅只写了三个字段）。
3. 整个响应缓存 5 分钟（Caffeine，key `home`），后台改商品时主动失效。
4. `glyph` 是 SVG path 字符串，原样从 `c_category.glyph` 返回，**不要转义**（前端用 `v-html`）。

#### 6.3.2 类目列表

```
GET /c/categories        公开
```

响应：`categories` 数组（同 6.3.1，不含 `featured` / `brand`）。

#### 6.3.3 类目详情

```
GET /c/categories/{code}?sub=nut        公开
```

响应：

```json
{
  "id": "snacks",
  "name": "零食",
  "hint": "坚果 · 炒货 · 果干",
  "glyph": "<path .../>",
  "productCount": 6,
  "subs": [ { "id": "nut", "name": "坚果炒货", "count": 2 } ],
  "items": [ { "id": "snacks-1", "name": "...", "price": 99.00, "sub": "nut" } ],
  "currentSub": "nut",
  "fallback": false
}
```

规则：

1. `code` 不存在 → 30001。
2. 传了 `sub` 且该细分有商品 → `items` 只返回该细分；`fallback=false`。
3. 传了 `sub` 但该细分 **0 件** → `items` 返回整类，`fallback=true`（前端会提示"还在补货"）。
4. 不传 `sub` → 返回整类。
5. 只返回 `status=1` 且 `deleted=0` 的商品。

#### 6.3.4 商品列表 / 搜索

```
GET /c/products?keyword=坚果&category=snacks&sub=nut&sort=default&page=1&size=20        公开
```

响应：分页 `Product`。

参数：

| 参数 | 说明 |
|---|---|
| `keyword` | 模糊匹配商品名、规格、类目名、类目 hint、细分名（对齐前端 `searchHits`） |
| `category` | 类目 code |
| `sub` | 细分 code |
| `sort` | `default`（sort,id）/ `price_asc` / `price_desc` / `sales_desc` |
| `page` / `size` | 默认 1 / 20，`size` 上限 100 |

规则：`keyword` 命中类目时，返回该类目下**全部**商品（与前端逻辑一致）。

#### 6.3.5 商品详情

```
GET /c/products/{code}        公开
```

响应：`Product` + `siblings`：

```json
{
  "id": "snacks-1",
  "name": "每日混合坚果 750g",
  "spec": "7 种果仁 · 30 小包",
  "price": 99.00,
  "list": 129.00,
  "tag": "会员价",
  "emoji": "🥜",
  "image": "/static/products/snacks-1.jpg",
  "sub": "nut",
  "detail": {
    "slogan": "一天一小包，把坚果吃成习惯",
    "intro": "腰果、巴旦木……",
    "facts": { "规格": "750g / 30 小包", "果仁含量": "约 88%" },
    "tips": ["每天 25-35g 就够了", "开封后密封冷藏"]
  },
  "siblings": [ { "id": "snacks-2", "name": "..." } ]
}
```

规则：

1. 不存在或下架 → 30001。
2. `siblings` 同细分不足 4 件时用同类补，仍不足就有多少返回多少，**不要重复**。
3. `facts` 必须是有序对象（Jackson 用 `LinkedHashMap` 或直接返回数据库 JSON 原文），前端按插入顺序渲染。

#### 6.3.6 推荐位

```
GET /c/featured?limit=12        公开
```

响应：`Product` 数组。规则：`is_featured=1` 且上架，按 `sort`。

---

### 6.4 购物车 `/cart`

> 全部需登录（含游客）。勾选状态也持久化，换设备一致。

#### 6.4.1 查询购物车

```
GET /cart        需登录
```

响应：

```json
{
  "lines": [
    {
      "id": "snacks-1", "name": "每日混合坚果 750g", "spec": "7 种果仁 · 30 小包",
      "price": 99.00, "list": 129.00, "emoji": "🥜",
      "image": "/static/products/snacks-1.jpg", "qty": 3, "checked": true
    }
  ],
  "summary": {
    "count": 3, "amount": 297.00, "saved": 90.00,
    "checkedCount": 3, "checkedAmount": 297.00, "allChecked": true,
    "isFreeShipping": true, "toFreeShipping": 0, "shippingRate": 100,
    "freeShippingAt": 99, "shipFee": 0
  }
}
```

规则：

1. `lines` 按 `updated_at DESC`。
2. 商品被删除或下架的行，返回时带 `invalid: true`，`price` 用加入时的快照价，前端置灰（本期前端还没做，接口先留字段）。
3. `summary` 全部由服务端算，前端只展示；**禁止**前端用 lines 自己乘（两边算法不一致会对不上账）。

#### 6.4.2 加入购物车

```
POST /cart/items        需登录
```

请求：

```json
{ "productId": "snacks-1", "qty": 1 }
```

响应：完整购物车（同 6.4.1）。

规则：

1. `qty` 默认 1，范围 1-99，越界取边界值（不报错，与前端 `Math.min(MAX_QTY, ...)` 一致）。
2. 已存在则累加，累加后超 99 取 99。
3. 加入时把该行 `checked` 置 1（对齐前端 `add()`）。
4. 商品不存在/下架 → 30001 / 30002。
5. 库存不足**不在这里拦**（购物车允许先加，下单才校验），但 `stock=0` 时返回 30003。

#### 6.4.3 修改数量 / 勾选

```
PATCH /cart/items/{productId}        需登录
```

请求（字段可选，传哪个改哪个）：

```json
{ "qty": 2, "checked": false }
```

响应：完整购物车。规则：`qty` 越界截断；行不存在 → 10004。

#### 6.4.4 删除 / 批量勾选 / 清空

```
DELETE /cart/items/{productId}        需登录      # 删一行
PUT    /cart/checked                  需登录      # 全选/全不选
DELETE /cart/checked                  需登录      # 删除已勾选
DELETE /cart                          需登录      # 清空
```

`PUT /cart/checked` 请求：

```json
{ "checked": true, "ids": ["snacks-1"] }
```

`ids` 不传表示全部行。响应：完整购物车（`DELETE /cart` 返回 null）。

#### 6.4.5 合并本地购物车（登录后调用）

```
POST /cart/merge        需登录
```

请求：

```json
{ "lines": [ { "productId": "snacks-1", "qty": 2 }, { "productId": "fruit-3", "qty": 1 } ] }
```

响应：完整购物车 + `skipped`：

```json
{ "lines": [], "summary": {}, "skipped": ["fruit-3"] }
```

规则：

1. 逐个 upsert，已存在则 `qty = min(99, 现有 + 传入)`。
2. 商品已下架的行**跳过**，写进 `skipped`。
3. 前端登录成功后调用一次，成功后清空本地 `owl.cart.v1`。

---

### 6.5 交易模块 `/trade`

#### 6.5.1 结算预览（服务端计价）

```
POST /trade/preview        需登录
```

请求：

```json
{
  "lines": [ { "productId": "snacks-1", "qty": 2 }, { "productId": "meat-1", "qty": 1 } ],
  "addressId": 1,
  "couponId": 12
}
```

响应：

```json
{
  "items": [
    { "id": "snacks-1", "name": "每日混合坚果 750g", "spec": "7 种果仁 · 30 小包",
      "image": "/static/products/snacks-1.jpg", "price": 99.00, "list": 129.00,
      "qty": 2, "total": 198.00 }
  ],
  "amounts": {
    "goodsAmount": 198.00,
    "discount": 10.00,
    "shipFee": 0,
    "payable": 188.00,
    "saved": 60.00
  },
  "shipping": { "freeShippingAt": 99, "toFreeShipping": 0, "isFreeShipping": true },
  "address": { "id": "1", "receiver": "罗耀", "phone": "138****8000",
               "fullAddress": "浙江省杭州市西湖区文三路 88 号 OWL 公寓 3 幢 1201 室", "tag": "家" },
  "coupons": [ { "id": "12", "name": "新人券 满 99 减 10", "discount": 10.00, "usable": true } ],
  "warnings": [ "「谷饲西冷牛排」库存仅剩 2 件" ]
}
```

规则：

1. **禁止信任前端价格**：所有 `price` 必须按 `productId` 重新查库，前端传的价格一律忽略（请求体里根本没有价格字段）。
2. 商品下架/删除 → 30002；库存不足 → 30003（`warnings` 里提示，不直接失败，下单才失败）。
3. 运费：`goodsAmount - discount >= 9900` 分则 0，否则 500 分。
4. 优惠券校验：属于当前用户、未使用、在有效期内、满足门槛；不满足则 `usable=false` 并给原因，**不报错**。
5. `saved` = Σ(max(0, list - price) × qty)，只用于展示"已比划线价省"。

#### 6.5.2 创建订单

```
POST /trade/orders        需登录
```

请求：

```json
{
  "lines": [ { "productId": "snacks-1", "qty": 2 } ],
  "addressId": 1,
  "couponId": 12,
  "remark": "放门口",
  "deliveryType": 1,
  "fromCart": true,
  "clientToken": "c1f4a9e2-8b3d-4a71-9f22-6d0e5b7a1c33"
}
```

响应：

```json
{
  "orderNo": "OWL20261028000123",
  "payNo": "P20261028231001001",
  "payAmount": 188.00,
  "status": 10,
  "expireAt": 1730110800000
}
```

规则（**必须在一个本地事务里**）：

1. `clientToken` 幂等：Redis `owl:trade:token:{userId}:{clientToken}` 存 10 分钟，重复请求直接返回上次的 `orderNo`（10005 也可，但返回订单更友好）。
2. 重新计价（同 6.5.1），金额以服务端为准。
3. 扣库存用**乐观锁**：

```sql
UPDATE c_product
SET stock = stock - #{qty}, sales = sales + #{qty}, version = version + 1
WHERE id = #{id} AND stock >= #{qty} AND version = #{version} AND status = 1 AND deleted = 0
```

影响行数为 0 → 30003，事务回滚。

4. 写 `c_stock_log`（type=1，ref_no=orderNo）。
5. 写 `t_order` + `t_order_item`（商品名/规格/图/价格全部快照）。
6. 锁券：`UPDATE mk_user_coupon SET status=1, order_no=?, used_at=NOW() WHERE id=? AND user_id=? AND status=0`，影响 0 行说明券被抢用 → 40005 回滚。
7. `fromCart=true` 时删除购物车对应行。**前端约定**：从购物车「去结算」进来的传 `true`；详情页「立即购买」进来的传 `false`（否则会误删购物车里同款）。
8. 提交后发一条 `order` 消息"订单已创建，请在 30 分钟内完成支付"。
9. 订单号规则：`OWL` + `yyyyMMdd` + 6 位序列（Redis `INCR owl:order:seq:{yyyyMMdd}`，当天重置，补零）。
10. 未支付订单 30 分钟后自动关闭：定时任务每分钟扫 `status=10 AND created_at < NOW() - 30 MIN`，回补库存、退券、写状态流水。

#### 6.5.3 订单列表

```
GET /trade/orders?status=all&page=1&size=10        需登录
```

`status` 取值：

| 值 | 含义 | SQL 条件 |
|---|---|---|
| `all` | 全部 | 无 |
| `unpaid` | 待付款 | `status = 10` |
| `shipping` | 待收货 | `status = 30` |
| `review` | 待评价 | `status = 40 AND review_status = 0` |
| `after` | 退换/售后 | `status IN (60, 70)` |

响应：分页，`list` 元素：

```json
{
  "orderNo": "OWL20261028000123",
  "status": 10,
  "statusText": "待付款",
  "payAmount": 188.00,
  "totalQty": 2,
  "firstItemName": "每日混合坚果 750g",
  "coverImage": "/static/products/snacks-1.jpg",
  "createdAt": 1730106600000,
  "expireAt": 1730110800000
}
```

#### 6.5.4 订单详情

```
GET /trade/orders/{orderNo}        需登录
```

响应：

```json
{
  "orderNo": "OWL20261028000123",
  "status": 10,
  "statusText": "待付款",
  "payStatus": 0,
  "amounts": { "goodsAmount": 198.00, "discount": 10.00, "shipFee": 0, "payable": 188.00, "saved": 60.00 },
  "items": [
    { "id": "snacks-1", "name": "每日混合坚果 750g", "spec": "7 种果仁 · 30 小包",
      "image": "/static/products/snacks-1.jpg", "price": 99.00, "list": 129.00, "qty": 2, "total": 198.00 }
  ],
  "address": { "receiver": "罗耀", "phone": "138****8000",
               "fullAddress": "浙江省杭州市西湖区文三路 88 号 OWL 公寓 3 幢 1201 室" },
  "deliveryType": 1,
  "deliveryText": "极速达 · 最快 1 小时送达",
  "remark": "放门口",
  "payNo": "P20261028231001001",
  "createdAt": 1730106600000,
  "payTime": null,
  "deliverTime": null,
  "receiveTime": null,
  "closeTime": null,
  "cancelReason": null,
  "timeline": [ { "status": 10, "statusText": "待付款", "at": 1730106600000, "remark": "订单创建" } ]
}
```

规则：只能查自己的订单（`user_id` 校验），越权返回 10003。

#### 6.5.5 取消 / 确认收货 / 角标

```
POST /trade/orders/{orderNo}/cancel        需登录
POST /trade/orders/{orderNo}/confirm       需登录
GET  /trade/orders/counts                  需登录
```

取消请求：

```json
{ "reason": "不想要了" }
```

规则：

1. 只有 `status=10` 可取消；`status=20` 需联系客服（本期不支持）；其余 40003。
2. 取消时：回补库存（`stock + qty`，写 `c_stock_log` type=2）、退券、`status=50`、写流水、发消息。
3. `confirm` 只有 `status=30` 可调，成功后 `status=40`，`receive_time=NOW()`。
4. `counts` 响应：

```json
{ "unpaid": 1, "shipping": 2, "review": 0, "after": 0 }
```

---

### 6.6 支付模块 `/pay`（模拟渠道）

#### 6.6.1 发起支付

```
POST /pay/mock/{orderNo}        需登录
```

响应：

```json
{
  "payNo": "P20261028231001001",
  "channel": "mock",
  "amount": 188.00,
  "mockToken": "mock_9f2c1a...",
  "expireAt": 1730110800000
}
```

规则：

1. 订单必须是自己的、`status=10`、未超时。
2. 已存在 `status=0` 的支付单则复用，**不要重复创建**。
3. `mockToken` 写 Redis `owl:pay:mock:{payNo}`，TTL 与订单一致。

#### 6.6.2 模拟支付回调

```
POST /pay/mock/callback        公开（用 mockToken 鉴权）
```

请求：

```json
{ "payNo": "P20261028231001001", "mockToken": "mock_9f2c1a...", "success": true }
```

响应：

```json
{ "orderNo": "OWL20261028000123", "payStatus": 1, "orderStatus": 20 }
```

规则（这是幂等设计的重点）：

1. 校验 `mockToken` 与 Redis 一致，否则 50001。
2. `p_payment.status` 已是 1 → **直接返回成功**，不重复改订单（幂等）。
3. `success=false` → 支付单 `status=2`，订单仍是 10，允许重新发起。
4. 成功：`p_payment.status=1`、`paid_at=NOW()`、`out_trade_no=MOCK+时间戳`；`t_order.pay_status=1`、`status=20`、`pay_time=NOW()`；写状态流水；发消息"支付成功，正在拣货"。
5. 整个方法加分布式锁 `lock:pay:{payNo}`（Redis，10 秒），防并发回调。

#### 6.6.3 查询支付单 / 退款

```
GET  /pay/{payNo}              需登录
POST /pay/refund               需登录
GET  /pay/refunds/{refundNo}   需登录
```

退款请求：

```json
{ "orderNo": "OWL20261028000123", "reason": "买错了" }
```

退款响应：

```json
{ "refundNo": "R20261028231500001", "amount": 188.00, "status": 0 }
```

规则：

1. 只有 `pay_status=1` 且订单状态在 `(20, 30, 40)` 可申请退款；`10` 请走取消。
2. 退款金额 = 支付金额（本期全额退，不支持部分退）。
3. 同一订单存在处理中/成功的退款单 → 50003。
4. 退款单创建后 `status=0`；模拟渠道**同步**置为成功：`p_refund.status=1`、`p_payment.status=4`、`t_order.status=70`、`pay_status=2`；回补库存；发消息"退款已到账"。
5. 真实渠道接入点：`PaymentChannel` 接口（`create` / `query` / `refund` / `parseCallback`），`MockPaymentChannel` 是一个实现，后续加 `WechatPaymentChannel` 即可，**不要**在 Service 里写 if-else 判断渠道。

---

### 6.7 个人中心 `/m`

#### 6.7.1 汇总信息

```
GET /m/profile        需登录
```

响应：

```json
{
  "user": { "id": "1001", "name": "罗耀", "level": "black", "memberNo": "OWL-2026-4821",
            "phone": "13800138000", "email": "", "points": 2860, "coupons": 3,
            "balance": 120.50, "joinedAt": 1730100000000, "isGuest": false },
  "assets": { "coupons": 3, "points": 2860, "balance": 120.50 },
  "orderCounts": { "unpaid": 1, "shipping": 2, "review": 0, "after": 0 }
}
```

规则：一次查完，**禁止**前端再发三个请求。

#### 6.7.2 地址簿

```
GET    /m/addresses                需登录
POST   /m/addresses                需登录
PUT    /m/addresses/{id}           需登录
DELETE /m/addresses/{id}           需登录
PUT    /m/addresses/{id}/default   需登录
```

新增/修改请求：

```json
{ "receiver": "罗耀", "phone": "13800138000", "province": "浙江省",
  "city": "杭州市", "district": "西湖区", "detail": "文三路 88 号 OWL 公寓 3 幢 1201 室",
  "tag": "家", "isDefault": true }
```

响应：

```json
{ "id": "1", "receiver": "罗耀", "phone": "13800138000",
  "province": "浙江省", "city": "杭州市", "district": "西湖区",
  "detail": "文三路 88 号 OWL 公寓 3 幢 1201 室", "tag": "家", "isDefault": true }
```

规则：本人查看自己的地址**返回真实手机号**（编辑时要用，打码会导致保存回脏数据）；订单、消息里展示时才打码。设置默认时把该用户其它地址 `is_default` 置 0（同一事务）；最多 20 条。

#### 6.7.3 优惠券 / 积分

```
GET /m/coupons?status=unused        需登录
GET /m/points?page=1&size=20        需登录
```

`status`：`unused` / `used` / `expired`。

优惠券响应：

```json
{ "list": [ { "id": "12", "name": "新人券 满 99 减 10", "type": 1,
              "threshold": 99.00, "discount": 10.00,
              "startAt": 1730100000000, "endAt": 1732700000000,
              "status": 0, "usable": true } ],
  "total": 1, "page": 1, "size": 20, "pages": 1 }
```

规则：查询时顺手把 `end_at < NOW() AND status=0` 的券置为 2（惰性过期），不要只靠定时任务。

---

### 6.8 消息模块 `/m/messages`

#### 6.8.1 列表

```
GET /m/messages?kind=all&page=1&size=20        需登录
```

`kind`：`all` / `order` / `promo` / `system`。

响应：

```json
{
  "list": [
    { "id": "10086", "kind": "order", "title": "订单已发出",
      "body": "你购买的「谷饲西冷牛排 1kg」已由杭州文三路店拣货完成，骑手正在路上，预计 1 小时内送达。",
      "time": "12 分钟前", "read": false, "createdAt": 1730105880000, "bizNo": "OWL20261028000123" }
  ],
  "total": 7, "page": 1, "size": 20, "pages": 1
}
```

规则：`time` 由服务端格式化，前端直接用：

| 时间差 | 格式 |
|---|---|
| < 1 分钟 | `刚刚` |
| < 60 分钟 | `N 分钟前` |
| 今天内 | `N 小时前` |
| 昨天 | `昨天 HH:mm` |
| 今年内 | `M 月 d 日` |
| 更早 | `yyyy 年 M 月 d 日` |

#### 6.8.2 未读数 / 已读 / 删除

```
GET    /m/messages/unread           需登录
POST   /m/messages/{id}/read        需登录
POST   /m/messages/read-all         需登录
DELETE /m/messages/{id}             需登录
```

未读数响应：

```json
{ "total": 3, "order": 2, "promo": 1, "system": 0 }
```

规则：只能操作自己的消息，越权 10003；删除是逻辑删除。

---

### 6.9 AI 助手 `/assistant`

> 前端契约在 `owl-web/src/services/assistant.ts`，**不要改前端请求体结构**。前端配 `VITE_ASSISTANT_API=http://localhost:8080/api/v1/assistant` 后，实际请求 `POST /api/v1/assistant/chat`。

#### 6.9.1 对话

```
POST /assistant/chat        公开（登录用户带 token，游客匿名）
```

请求（前端原样发出）：

```json
{
  "model": "deepseek-chat",
  "messages": [
    { "role": "system", "content": "你是 OWL 商城的购物分析助手……" },
    { "role": "user", "content": "帮我凑到免运费" }
  ],
  "context": {
    "cartCount": 2,
    "cartAmount": 198.00,
    "cartCheckedAmount": 198.00,
    "cartSaved": 60.00,
    "toFreeShipping": 0,
    "isFreeShipping": true,
    "freeShippingAt": 99,
    "lines": [ { "name": "每日混合坚果 750g", "qty": 2, "price": 99.00 } ]
  }
}
```

响应（必须含 `reply`，前端 `pickReply` 认这个字段）：

```json
{
  "reply": "已经免运费了 —— 已选 ¥198.00，过了 ¥99 门槛。\n\n这单符合极速达条件，最快 1 小时内送到。",
  "model": "deepseek-chat",
  "usage": { "promptTokens": 420, "completionTokens": 86, "totalTokens": 506 },
  "degraded": false
}
```

规则：

1. **服务端自己拼 system prompt，忽略前端传来的 `system` 消息**（防提示词注入）。见 6.9.2。2. 只取 `messages` 里最后 10 条 `user`/`assistant`，把 `context` 序列化成一个 `user` 消息附在最后：

```
【当前购物车】{"cartCount":2,...}
```

3. 调 DeepSeek：`POST {base-url}/chat/completions`，`temperature=0.6`，`max_tokens=600`，`timeout=30s`。
4. 限流：登录用户 20 次/小时（Redis `owl:ai:limit:u:{userId}`），匿名 5 次/小时（按 IP）。超限返回 70002。
5. 模型超时/报错 → **不要抛 5xx**，返回 `code=0` + 兜底文案 + `degraded=true`，前端体验不中断：

```
抱歉，我这边算得有点慢。你可以先看购物车页的合计，或者再问我一次。
```

6. 每次调用写 `ai_usage_log`（成功、失败都写），`latency_ms` 必填。
7. API Key 只从环境变量 `DEEPSEEK_API_KEY` 读，**禁止**写进 `application.yml` 明文、禁止返回给前端。

#### 6.9.2 服务端 System Prompt（照抄）

```
你是 OWL 商城的购物分析助手，风格像一位懂行的店员：简洁、有分寸，不推销。

规则：
1. 优先使用【当前购物车】里的真实数据回答，涉及金额必须与给定数字一致，不要自己编。
2. 用户问凑单时，先算差额，再给 1-2 个具体建议，说明加了之后合计多少。
3. 用户问推荐、库存、活动，而上下文里没有数据时，直说"这个我查不到"，不要编造商品或价格。
4. 只聊购物相关（商品、价格、运费、配送、售后、订单）。其它话题礼貌拒绝并拉回购物。
5. 回复用纯文本，可以用「·」列举，不要用 Markdown 标题、表格、代码块。
6. 不超过 200 字。
```

#### 6.9.3 流式（预留，T7 之后再实现）

```
POST /assistant/chat/stream        text/event-stream
```

事件格式：`data: {"delta":"已经"}\n\n`，结束 `data: [DONE]\n\n`。
前端 `requestReply` 已预留 `onChunk` 位置，接的时候改 `remoteReply` 一段即可，组件不用动。

---

### 6.10 B 端 `/admin`

> 独立于 C 端：`s_admin` 表、`scope=admin` 的 JWT、`/admin/**` 拦截器。C 端 token 访问 B 端一律 403。

#### 6.10.1 管理员登录

```
POST /admin/auth/login        公开
```

请求：

```json
{ "username": "admin", "password": "admin123456" }
```

响应：

```json
{
  "accessToken": "eyJ...",
  "expiresIn": 7200,
  "admin": { "id": "1", "username": "admin", "nickname": "超级管理员", "role": "super" }
}
```

规则：初始化脚本插入 `admin / admin123456`（BCrypt），**上线前必须改**；登录失败 5 次锁 15 分钟。

#### 6.10.2 商品管理

```
GET    /admin/products?keyword=&status=&page=1&size=20        需管理员
POST   /admin/products                                        需管理员
PUT    /admin/products/{code}                                 需管理员
PATCH  /admin/products/{code}/status                          需管理员
PATCH  /admin/products/{code}/stock                           需管理员
```

新增/修改请求：

```json
{
  "name": "每日混合坚果 750g",
  "categoryCode": "snacks",
  "subCode": "nut",
  "spec": "7 种果仁 · 30 小包",
  "price": 99.00,
  "list": 129.00,
  "tag": "会员价",
  "emoji": "🥜",
  "image": "/static/products/snacks-1.jpg",
  "stock": 200,
  "isFeatured": true,
  "detail": {
    "slogan": "一天一小包，把坚果吃成习惯",
    "intro": "……",
    "facts": { "规格": "750g / 30 小包" },
    "tips": ["每天 25-35g 就够了"]
  }
}
```

上下架请求：`{ "status": 0 }`（0 下架 / 1 上架）。
库存调整请求：`{ "delta": 50, "reason": "补货" }`（正加负减，写 `c_stock_log` type=4）。

规则：

1. `code` 新增时由后端生成：`{categoryCode}-{序号}`，序号取该 category 下最大 +1。
2. 价格必须 `0 < price <= list`，否则 10001。
3. 修改商品后失效商品详情缓存与首页缓存。
4. 删除商品用 `DELETE /admin/products/{code}`，逻辑删除；已产生订单的商品允许删（订单有快照）。
5. 每次写操作记 `s_admin_log`。

#### 6.10.3 订单管理

```
GET  /admin/orders?status=&keyword=&page=1&size=20        需管理员
GET  /admin/orders/{orderNo}                             需管理员
POST /admin/orders/{orderNo}/deliver                     需管理员
```

发货请求：

```json
{ "company": "顺丰", "trackingNo": "SF1234567890" }
```

规则：只有 `status=20` 可发货，成功后 `status=30`、`deliver_time=NOW()`、写流水、发消息"订单已发出"。

#### 6.10.4 概览统计

```
GET /admin/stats/overview        需管理员
```

响应：

```json
{
  "gmv": 128600.00,
  "orderCount": 326,
  "userCount": 158,
  "productCount": 90,
  "lowStockCount": 3,
  "todayOrderCount": 12,
  "todayGmv": 2680.00
}
```

规则：`gmv` 只统计 `pay_status=1` 且未退款的订单；`lowStock` 指 `stock < 10` 且上架。

#### 6.10.5 类目管理

```
GET  /admin/categories            需管理员
POST /admin/categories            需管理员
PUT  /admin/categories/{code}     需管理员
```

请求：`{ "code": "snacks", "name": "零食", "hint": "坚果 · 炒货 · 果干", "glyph": "<path .../>", "sort": 10, "subs": [{ "code": "nut", "name": "坚果炒货" }] }`

规则：`subs` 全量覆盖（先删后插，注意有商品的细分不允许删除，返回 30004）。

---

## 7. 核心业务规则

### 7.1 计价

**唯一入口**：`com.owl.trade.service.PriceCalculator`，**禁止**在 Controller 或前端算钱。

```java
public PriceResult calculate(List<PriceLine> lines, Long userId, Long userCouponId) {
    long goods = 0, saved = 0;
    for (PriceLine line : lines) {
        goods += line.priceCent() * line.qty();
        saved += Math.max(0, line.listCent() - line.priceCent()) * line.qty();
    }
    long discount = couponService.discountOf(userCouponId, userId, goods); // 不满足门槛返回 0
    long afterDiscount = Math.max(0, goods - discount);
    long shipFee = afterDiscount >= props.getFreeThresholdCent() ? 0 : props.getFeeCent();
    long payable = afterDiscount + shipFee;
    return new PriceResult(goods, discount, shipFee, payable, saved);
}
```

规则：

1. 阈值 9900 分、运费 500 分从 `owl.shipping.*` 读，**禁止硬编码**（前端 `FREE_SHIPPING = 99` / `SHIP_FEE = 5` 要对得上）。
2. `shipFee` 判定用「优惠后金额」，不是商品原价。
3. 所有中间结果都是 `long`，最后才 `PriceUtil.toYuan()`。
4. `PriceUtil`：

```java
public static BigDecimal toYuan(long cent) {
    return BigDecimal.valueOf(cent, 2);   // 9900 -> 99.00
}
public static long toCent(BigDecimal yuan) {
    return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
}
```

### 7.2 库存

1. 只有 `c_product.stock` 一个真相来源，**不做 Redis 预扣**（单体阶段没必要，还会引入一致性问题）。
2. 扣减必须带 `stock >= qty AND version = ?`，影响行数 0 就抛 30003，事务回滚。
3. 每次变动写 `c_stock_log`，`before_qty/after_qty` 都要填，方便对账。
4. 取消订单、退款、超时关单都要回补，回补同样走乐观锁。
5. 微服务阶段（第 9 章）再把库存拆出去，用 Redis + 异步落库；**现在不要提前优化**。

### 7.3 订单状态机

```
10 待付款 ──支付成功──> 20 待发货 ──发货──> 30 待收货 ──确认──> 40 已完成
   │                        │                    │
   │取消/超时                │退款                 │退款
   ▼                        ▼                    ▼
50 已取消              60 退款中 ──退款成功──> 70 已退款
```

允许的流转（**其它一律 40003**）：

| 当前 | 动作 | 目标 | 触发方 |
|---|---|---|---|
| 10 | 支付成功 | 20 | 支付回调 |
| 10 | 取消 | 50 | 用户 |
| 10 | 超时 30 分钟 | 50 | 定时任务 |
| 20 | 发货 | 30 | 管理员 |
| 20 / 30 / 40 | 申请退款 | 60 | 用户 |
| 30 | 确认收货 | 40 | 用户 |
| 60 | 退款成功 | 70 | 支付回调 |

实现方式：`OrderStatus` 枚举 + `canTransitTo(target)`，Service 里统一 `order.transitTo(...)`，**禁止**到处 `setStatus`。每次流转写 `t_order_status_log`。

### 7.4 幂等设计（三处必须做）

| 场景 | 方案 |
|---|---|
| 创建订单 | 前端传 `clientToken`，Redis `SETNX owl:trade:token:{userId}:{token} {orderNo} EX 600`；已存在直接返回旧订单号 |
| 支付回调 | `UPDATE p_payment SET status=1 WHERE pay_no=? AND status=0`，影响 0 行说明已处理，直接返回成功 |
| 退款 | 先查 `p_refund` 是否有 `order_no` 且 `status IN (0,1)`，有则返回 50003；退款单号唯一索引兜底 |

```java
// 创建订单的幂等骨架
String key = RedisKey.tradeToken(userId, req.getClientToken());
String exist = redis.get(key);
if (exist != null) {
    return orderService.detail(exist);   // 直接返回上次结果
}
if (!redis.setIfAbsent(key, "PENDING", Duration.ofMinutes(10))) {
    throw new BizException(ErrorCode.REPEAT_SUBMIT);
}
try {
    OrderVO order = doCreate(userId, req);
    redis.set(key, order.getOrderNo(), Duration.ofMinutes(10));
    return order;
} catch (RuntimeException e) {
    redis.delete(key);   // 失败要放开，否则用户 10 分钟内不能重试
    throw e;
}
```

### 7.5 优惠券

本期只做两种：满减、折扣，规则简单：

1. 可用条件：`status=0`、`start_at <= NOW() <= end_at`、`goodsAmount >= threshold_cent`、属于当前用户。
2. 满减：`discount = min(discount_cent, goodsAmount)`。
3. 折扣：`discount = goodsAmount * (100 - discount_rate) / 100`，向下取整到分。
4. 一单只能用一张，**不叠加**。
5. 下单锁券，取消/超时/退款释放（`status=0`, `order_no=null`）。
6. 新人券 `NEW10`：满 99 减 10，注册自动发。

### 7.6 购物车与「待购清单」怎么对应

前端有两份清单，后端只有一份购物车。映射关系：

| 前端 | 后端 | 说明 |
|---|---|---|
| `cart.lines` | `t_cart_item` | 长期存放，服务端持久化，登录合并 |
| `checkout.lines` | **不落库** | 结算草稿，只在内存/localStorage |
| 卡片上的 `+ / -` | 改 `checkout` | 不请求后端 |
| 「加入购物车」 | `POST /cart/items` | 真实入库 |
| 购物车页「去结算」 | 把勾选行搬进 `checkout`，跳 `/checkout` | 前端行为不变 |
| 结算页「提交订单」 | `POST /trade/orders`，body 带 `lines` | 服务端重新计价 |

**为什么待购清单不落库**：它是"这一次要买什么"的临时意图，落库会带来三个麻烦——多端同时编辑冲突、库存/价格过期、下单后残留。前端已经用 localStorage 处理得很好，后端只需要在**下单那一刻**拿到准确的 `productId + qty`。

**必须在文档里守住的一条**：`POST /trade/orders` 收到的 `lines` 只是"买什么、买几件"，价格、名称、图片一律由服务端查库填充。前端传的任何金额字段都要忽略。

### 7.7 缓存

单体阶段用 Caffeine（`CacheService`），不引 Redis 做业务缓存。

| 缓存 | Key | TTL | 失效时机 |
|---|---|---|---|
| 首页聚合 | `home` | 5 min | 商品/类目写操作后 `evict("home")` |
| 类目列表 | `categories` | 10 min | 类目写操作后 |
| 商品详情 | `product:{code}` | 5 min | 商品更新/上下架后 |
| 推荐位 | `featured` | 5 min | 商品 `is_featured` 变更后 |

Redis 只用于：验证码、refresh token、登录失败计数、限流、订单号序列、幂等键、分布式锁。Key 规范：

```
owl:{模块}:{业务}:{标识}
owl:sms:code:register:13800138000
owl:auth:refresh:1001:{jti}
owl:auth:fail:13800138000
owl:trade:token:1001:{clientToken}
owl:order:seq:20261028
owl:ai:limit:u:1001
lock:pay:{payNo}
```

**禁止**：`keys *`、无 TTL 的 key、把整个对象 JSON 塞进一个 key 当数据库用。

### 7.8 定时任务

| 任务 | 频率 | 逻辑 |
|---|---|---|
| 关闭超时未支付订单 | 每分钟 | `status=10 AND created_at < NOW()-30min` → 取消、回补库存、退券、发消息 |
| 清理游客账号 | 每天 03:00 | `is_guest=1 AND created_at < NOW()-24h`，删购物车/消息，保留订单 |
| 券惰性过期兜底 | 每小时 | `status=0 AND end_at < NOW()` → 2 |
| 会员等级重算 | 每天 04:00 | 近 12 个月实付 ≥ 1000 元 → `black`，否则 `normal` |
| 低库存告警 | 每天 09:00 | `stock < 10` 且上架，打 warn 日志（不接短信） |

实现：`@Scheduled(cron = "...")` + `@EnableScheduling`，**必须**加 Redis 分布式锁 `lock:job:{name}`，否则以后多实例会重复执行。

---

## 8. 安全设计

### 8.1 拦截器与白名单

```
TraceFilter（最高优先级，生成 traceId）
  → JwtAuthInterceptor   拦 /auth/**, /cart/**, /trade/**, /pay/**, /m/**, /assistant/**
  → AdminAuthInterceptor 拦 /admin/**
```

白名单（不校验 token）：

```
/auth/login, /auth/register, /auth/guest, /auth/refresh, /auth/sms-code
/c/**, /assistant/chat, /pay/mock/callback
/doc.html, /v3/api-docs/**, /static/**, /error
/admin/auth/login
```

> `/pay/mock/callback` 是模拟渠道回调，靠 `mockToken` 鉴权（见 6.6.2），不能要求用户 JWT，否则渠道回调永远进不来。真实渠道同理，要单独开白名单。

实现要点：

1. `JwtAuthInterceptor.preHandle` 解析 token → `LoginUser` → `UserContext.set(user)`。
2. `afterCompletion` **必须** `UserContext.clear()`，否则线程池复用会串号（这是最常见的坑）。
3. Controller 里取当前用户统一 `UserContext.requireUserId()`，**禁止**从请求参数里拿 `userId`。
4. 需要登录但没 token → 10002（HTTP 401）；token 过期同样 10002，前端据此跳登录。

### 8.2 JWT

结构（HS256）：

```json
{
  "sub": "1001",
  "typ": "access",
  "jti": "9f2c1a7b",
  "scope": "user",
  "iat": 1730106600,
  "exp": 1730113800
}
```

JJWT 0.12.x 写法（和网上 0.9 的示例不一样，别抄错）：

```java
SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

String token = Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("typ", "access")
        .claim("scope", "user")
        .id(jti)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + ttl * 1000))
        .signWith(key)
        .compact();

Claims claims = Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
```

规则：

1. `secret` 至少 32 字节，从环境变量读，**禁止**硬编码在代码里。
2. access 2 小时，refresh 14 天；refresh 必须存 Redis 才能吊销。
3. 解析失败分三种：过期（10002 + "登录已过期"）、签名错误（10002）、格式错误（10002）。不要给前端暴露细节。

### 8.3 密码与验证码

1. BCrypt strength 10，**禁止** MD5/SHA1/明文。
2. 登录失败计数 `owl:auth:fail:{account}`，5 次锁 15 分钟，成功即删。
3. 验证码 6 位数字，5 分钟有效，一次有效（校验后立即删）。
4. 验证码发送做手机号 60 秒 + IP 每小时 20 条双限流。
5. 日志里**禁止**打印密码、验证码、完整手机号；手机号统一 `138****8000`。

### 8.4 越权与防刷

1. 所有按 ID 查询/修改的接口，SQL 必须带 `user_id = #{currentUserId}`，**不要**先查再在 Java 里比（容易漏）。
2. 订单、地址、消息、券的详情接口都要写越权测试用例：A 用户访问 B 用户的资源必须 10003。
3. 写接口加简单限流：同一用户 1 秒内同一路径超过 5 次 → 10006（Redis 计数，`@RateLimit` 注解 + AOP）。
4. 上传接口（本期只有后台图片）限制 5MB、只允许 jpg/png/webp，文件名用 UUID 重命名，禁止用原始文件名拼路径。

### 8.5 CORS 与前端联调

开发期允许 `http://localhost:5173`：

```java
registry.addMapping("/**")
        .allowedOriginPatterns("http://localhost:*")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
```

生产用 Nginx 同源代理，**不要** `allowedOrigins("*")` 配 `allowCredentials(true)`（浏览器会直接拒绝）。

---

## 9. 微服务演进路线

### 9.1 先别拆：什么时候才值得拆

单体跑通以下全部之后再拆，否则你会在 7 个进程里同时 debug：

- [ ] C 端 15 个接口全部通过验收（第 12 章）
- [ ] 下单 → 支付 → 发货 → 收货 → 退款 全链路跑通
- [ ] 单元测试覆盖计价、库存、状态机
- [ ] 日志有 traceId，能定位一次请求

拆的理由只有两个：**要独立扩容**（商品查询 QPS 高）、**要独立发布**（改商品不影响下单）。学习 Spring Cloud Alibaba 也算一个，那就按 9.8 的顺序拆，不要一次性重写。

### 9.2 拆分映射

| 单体模块 | 微服务 | 端口 | Nacos 服务名 | 职责 |
|---|---|---|---|---|
| `owl-boot` | `owl-gateway` | 8080 | `owl-gateway` | 路由、鉴权、限流、跨域 |
| `owl-auth` | `owl-auth-service` | 8081 | `owl-auth` | 注册登录、用户、地址 |
| `owl-catalog` | `owl-catalog-service` | 8082 | `owl-catalog` | 类目、商品、库存 |
| `owl-trade` | `owl-trade-service` | 8083 | `owl-trade` | 购物车、计价、订单 |
| `owl-payment` | `owl-payment-service` | 8084 | `owl-payment` | 支付、退款 |
| `owl-message` | `owl-message-service` | 8085 | `owl-message` | 站内信 |
| `owl-assistant` | `owl-assistant-service` | 8086 | `owl-assistant` | AI 网关 |
| `owl-admin` | `owl-admin-service` | 8087 | `owl-admin` | 后台接口 |

`owl-common` / `owl-infra` 打成 jar 被所有服务依赖，不单独部署。

### 9.3 网关

`owl-gateway` 的依赖（**不能**引 `spring-boot-starter-web`，会冲突）：

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
  <groupId>com.alibaba.cloud</groupId>
  <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
  <groupId>com.alibaba.cloud</groupId>
  <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<dependency>
  <groupId>com.alibaba.cloud</groupId>
  <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

路由配置：

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: false          # 关闭自动路由，全部显式配置
      routes:
        - id: auth
          uri: lb://owl-auth
          predicates: [ Path=/api/v1/auth/**, /api/v1/m/** ]
        - id: catalog
          uri: lb://owl-catalog
          predicates: [ Path=/api/v1/c/** ]
        - id: trade
          uri: lb://owl-trade
          predicates: [ Path=/api/v1/cart/**, /api/v1/trade/** ]
        - id: payment
          uri: lb://owl-payment
          predicates: [ Path=/api/v1/pay/** ]
        - id: message
          uri: lb://owl-message
          predicates: [ Path=/api/v1/m/messages/** ]
        - id: assistant
          uri: lb://owl-assistant
          predicates: [ Path=/api/v1/assistant/** ]
        - id: admin
          uri: lb://owl-admin
          predicates: [ Path=/api/v1/admin/** ]
```

全局鉴权过滤器 `AuthGlobalFilter`：

1. 白名单直接放行（同 8.1）。
2. 取 `Authorization`，校验 JWT，把 `userId` / `scope` 放进 header：`X-User-Id`、`X-User-Scope`。
3. **剥离**外部传入的 `X-User-Id`（防伪造）。
4. 下游服务只信任网关注入的 header，`JwtAuthInterceptor` 改成读 header 即可（代码改动很小）。

### 9.4 Nacos

本机已装，启动：

```bash
# Windows
startup.cmd -m standalone
# 访问 http://localhost:8848/nacos，默认 nacos/nacos
```

建两个命名空间：`owl-dev`、`owl-prod`。每个服务一个 dataId：`owl-trade-dev.yml`。

`bootstrap.yml` 在 Spring Cloud 2023 已废弃，用 `spring.config.import`：

```yaml
spring:
  application:
    name: owl-trade
  config:
    import:
      - optional:nacos:owl-trade-${spring.profiles.active}.yml
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      username: nacos
      password: nacos
      discovery:
        namespace: owl-dev
        group: DEFAULT_GROUP
      config:
        namespace: owl-dev
        group: DEFAULT_GROUP
        file-extension: yaml
```

**注意**：`optional:` 前缀表示配置中心连不上也能启动，本地开发必备，否则 Nacos 没开会起不来。

### 9.5 OpenFeign 调用

trade 需要商品信息，拆开后不能再查 `c_product` 表，改为调 catalog：

```java
@FeignClient(name = "owl-catalog", path = "/internal/products",
             fallbackFactory = ProductClientFallback.class)
public interface ProductClient {

    @PostMapping("/batch")
    Result<List<ProductSnapshot>> batch(@RequestBody List<String> codes);

    @PostMapping("/deduct")
    Result<Void> deduct(@RequestBody StockDeductRequest req);
}
```

规则：

1. 内部接口统一放 `/internal/**`，网关**不路由** `/internal/**`（外网访问不到）。
2. `fallbackFactory` 必须实现，返回明确错误码，不要返回 null 让上层 NPE。
3. Feign 超时：连接 2s、读取 3s；重试**只对 GET** 开启，POST 扣库存**禁止重试**（会重复扣）。
4. 传对象用 DTO，**禁止**直接传 Entity（表结构一变就崩）。

### 9.6 Sentinel

1. 每个服务引 `spring-cloud-starter-alibaba-sentinel`，配 `spring.cloud.sentinel.transport.dashboard=127.0.0.1:8858`（不要用 8080，会和网关冲突）。
2. 必配三条规则：
   - 商品详情 QPS 限流 200，快速失败；
   - 下单接口 QPS 限流 50，排队 500ms；
   - `ProductClient` 熔断：慢调用比例 > 50%（RT > 1s）持续 10s 则熔断 30s。
3. 规则持久化到 Nacos（`sentinel` dataId），**不要**只配在控制台（重启就没了）。

### 9.7 分布式事务：不要上 Seata

下单要跨 trade + catalog（扣库存）+ marketing（锁券）。三种方案对比：

| 方案 | 一致性 | 复杂度 | 建议 |
|---|---|---|---|
| Seata AT | 强一致 | 高（TC 服务、undo_log、全局锁） | 学习可以，生产小项目不建议 |
| TCC | 强一致 | 最高（每个参与方写 try/confirm/cancel） | 秒杀场景才值得 |
| **本地消息表 + 最终一致** | 最终一致 | 低 | **本期选这个** |

落地方式：

1. `t_order` 建好后，在**同一个本地事务**里写 `t_local_message`（`biz_type=STOCK_DEDUCT`, `payload`, `status=0`）。
2. 定时任务扫 `status=0` 的消息，调 catalog 扣库存；成功置 1，失败重试（指数退避，最多 5 次）。
3. 库存扣失败且重试耗尽 → 自动取消订单并退款。
4. 这样 trade 的事务只在自己的库，跨服务靠消息补偿，代码量比 Seata 少一半。

> 这条决策要写进代码注释，否则以后有人"优化"成 Seata 会把简单问题复杂化。

### 9.8 拆分步骤（按顺序做，每步都能跑）

1. 单体加 `spring-cloud-starter-alibaba-nacos-discovery`，注册上去，网关先不接，确认 Nacos 能看到实例。
2. 拆 `owl-gateway`，把单体当唯一上游，路由全量转发。此时行为不变，只是多了一跳。
3. 拆 `owl-auth`：新建服务，复制 `owl-auth` 模块代码，删掉单体里的同名 Controller，网关指向新服务。
4. 拆 `owl-catalog`（读多，先拆它收益最大），trade 里改成 Feign 调商品。
5. 拆 `owl-trade` + `owl-payment`（这两个一起拆，因为下单和支付耦合紧）。
6. 拆 `owl-message` + `owl-assistant`（最简单，最后做）。
7. 单体只留 `owl-admin`，最后也拆掉，单体退役。

每步做完都要跑一遍第 12 章的验收清单，**不要连着拆两个**。

---

## 10. 开发任务单（投喂用）

> 每条任务都是独立的投喂单元。开头模板见附录 C。
> 顺序不能跳：T3 依赖 T2 的表，T5 依赖 T3 的登录态，T6 依赖 T4 的商品和 T5 的购物车。

### T0 环境自检（10 分钟）

**目标**：确认本机具备全部前置条件，避免写到一半发现版本不对。

```bash
java -version          # 期望 21.x（java version "21.0.x"）
mvn -v                 # 期望 3.9.x，且 Java version: 21
mysql --version        # 期望 8.0.x
mysql -uroot -p123456 -e "SELECT VERSION();"
redis-cli ping         # 期望 PONG
curl http://localhost:8848/nacos/actuator/health   # 期望 {"status":"UP"}
```

**产出**：`docs/env-check.md`，把每条命令的实际输出贴进去。

**常见问题**：

- 系统里有多个 JDK → `JAVA_HOME` 指向 21，IDEA 里四处也都要 21（见 3.4）。
- `mvn -v` 显示 Java 17 → Maven 用的是自己的 JAVA_HOME，改 `Settings → Maven → Runner → JRE`。
- Redis 没设密码但连不上 → 检查是不是 WSL 里的 Redis，Windows 侧连 `127.0.0.1` 不通，改用 `localhost` 或装 Windows 版。
- Nacos 启动报 `db.num is null` → 用 `-m standalone`，别用 cluster。

---

### T1 工程骨架（半天）

**目标**：`mvn clean package` 通过，启动后 `/health` 能返回。

**产出文件**：

```
owl-server/pom.xml
owl-server/owl-common/pom.xml
   └─ api/{Result,PageResult,ErrorCode,BizException}.java
   └─ context/{UserContext,LoginUser}.java
   └─ util/PriceUtil.java
owl-server/owl-infra/pom.xml
   └─ web/{GlobalExceptionHandler,TraceFilter,WebConfig}.java
owl-server/owl-boot/pom.xml + OwlApplication.java + controller/HealthController.java
   + resources/application.yml + application-dev.yml
owl-server/owl-modules/pom.xml（聚合）
```

**必须实现的类**：

| 类 | 所在模块 | 要求 |
|---|---|---|
| `Result<T>` | common | `ok(data)` / `ok()` / `fail(ErrorCode)` / `fail(code, msg)`，字段 `code/message/data/traceId` |
| `ErrorCode` | common | 枚举，含附录 A 全部码，字段 `code/message` |
| `BizException` | common | `RuntimeException` 子类，持 `ErrorCode` |
| `PageResult<T>` | common | `list/total/page/size/pages`，静态方法 `of(List<T> list, long total, int page, int size)` |
| `TraceFilter` | infra | 生成 16 位 traceId 放 MDC + 响应头 `X-Trace-Id` |
| `GlobalExceptionHandler` | infra | 处理 `BizException`、`MethodArgumentNotValidException`、`ConstraintViolationException`、`Exception`，全部转 `Result` |
| `WebConfig` | infra | `addPathPrefix("/api/v1", RestController)` + CORS |
| `HealthController` | boot | `GET /health` 返回 `{"status":"UP","time":...}` |

**验收**：

```bash
cd E:/OWL-System/owl-server
mvn -q clean package -DskipTests
java -jar owl-boot/target/owl-boot-1.0.0.jar
curl http://localhost:8080/api/v1/health
# {"code":0,"message":"ok","data":{"status":"UP"},"traceId":"..."}
```

**禁止**：

- 不要在这一步引 Nacos / Sentinel / Gateway 依赖，单体阶段用不到。
- 不要在 `owl-common` 里引 `spring-boot-starter-web`（会传染给所有模块，且 `owl-infra` 会循环依赖）。
- 不要写业务代码。

---

### T2 建库 + 实体 + 商品初始化（1 天）

**目标**：20 张表建好，90 件商品能被 `SELECT` 出来。

**步骤**：

1. 执行 `01_schema.sql`（第 5.3 节）。
2. 执行 `02_init.sql`（第 5.6 节）：只插券模板，**不插用户**（密码由 `DataSeeder` 加密写入）。
3. 生成 `catalog.json`（第 5.4 节），复制图片，写 `CatalogSeeder` + `DataSeeder`。
4. 用 MyBatis-Plus 代码生成或手写，产出 20 个 Entity + Mapper（只到 `owl-auth` / `owl-catalog` 两个模块，其它等对应任务再写）。
5. 配置 `MybatisPlusConfig`（放 `owl-infra/db`）：分页插件、乐观锁插件、防全表更新插件。

**Entity 规范**：

- 类名 = 表名驼峰（`c_product` → `CProduct`），**必须** `@TableName("c_product")`。
- 主键 `@TableId(type = IdType.AUTO)`。
- 逻辑删除字段 `@TableLogic`。
- 时间字段统一 `LocalDateTime`，**不要**用 `Date`。
- **禁止**在 Entity 上加 `@JsonFormat` / 任何给前端看的注解（Entity 不出 Service）。

**验收**：

```bash
mysql -uroot -p123456 owl_mall -e "
SELECT COUNT(*) FROM c_category;      -- 15
SELECT COUNT(*) FROM c_sub_category;  -- 120
SELECT COUNT(*) FROM c_product;       -- 90
SELECT COUNT(*) FROM c_product_detail;-- 90
SELECT COUNT(*) FROM c_product WHERE is_featured = 1; -- 12
"
# 启动日志里应有：catalog seeded: 15 categories / 120 subs / 90 products
```

**常见坑**：

- `c_product.code` 唯一索引冲突 → Seeder 用 `INSERT ... ON DUPLICATE KEY UPDATE` 或先查后插，保证可重复执行。
- `facts_json` 用 `JSON` 类型，MyBatis 默认映射不了 → 加 `JacksonTypeHandler`，字段上标 `@TableField(typeHandler = JacksonTypeHandler.class)`，并加 `@TableName(autoResultMap = true)`。
- 中文乱码 → 连接串 `characterEncoding=utf8`，库和表 `utf8mb4`。

---

### T3 认证模块（1 天）

**目标**：注册 → 登录 → 带 token 访问 `/auth/me` 全通。

**产出**：

```
owl-auth/
├─ controller/AuthController.java
├─ service/AuthService.java + impl
├─ mapper/MUserMapper.java
├─ entity/MUser.java
├─ dto/{SmsCodeRequest,RegisterRequest,LoginRequest,RefreshRequest,UpdateProfileRequest,ChangePasswordRequest}.java
├─ vo/{LoginVO,UserVO}.java
└─ converter/UserConverter.java
owl-infra/security/{JwtService,PasswordService,JwtProperties}.java
owl-common/context/{UserContext,LoginUser}.java
owl-common/config/WebConfig.java（注册拦截器）
```

**要点**：

1. `UserVO.id` 用 `String.valueOf(id)`，`joinedAt` 用 `createdAt.toInstant(ZoneOffset.ofHours(8)).toEpochMilli()`。
2. `LoginVO` 字段名严格按 6.2.2，**不要**改名成 `token` / `userInfo`。
3. 验证码校验逻辑抽成 `SmsCodeService`，方便以后换阿里云短信。
4. 注册送券、写欢迎消息放在 `@Transactional` 里，任何一步失败整体回滚。
5. 拦截器 `afterCompletion` 里 `UserContext.clear()`。

**验收**：

```bash
# 1. 验证码
curl -s -X POST localhost:8080/api/v1/auth/sms-code \
  -H 'Content-Type: application/json' \
  -d '{"phone":"13900139000","scene":"register"}'
# 记下 data.code

# 2. 注册
curl -s -X POST localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"测试","phone":"13900139000","code":"<上一步的code>","password":"abc12345","agree":true}'

# 3. 登录
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"account":"13900139000","password":"abc12345","remember":true}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")

# 4. 当前用户
curl -s localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"
# data.id 必须是字符串 "1002"
```

**必须写的测试**：密码错误 5 次锁号、验证码复用失败、重复注册 20003、无 token 访问 10002。

---

### T4 商品模块（1 天）

**目标**：前端三个页面（首页/分类/详情）需要的全部数据都能一次拿到。

**产出**：

```
owl-catalog/
├─ controller/{CategoryController,ProductController}.java
├─ service/{CategoryService,ProductService}.java + impl
├─ mapper/{CCategoryMapper,CSubCategoryMapper,CProductMapper,CProductDetailMapper}.java
├─ vo/{CategoryVO,SubVO,ProductVO,ProductDetailVO,HomeVO}.java
└─ converter/ProductConverter.java
```

**要点**：

1. 首页聚合一次查完：类目 + 细分计数（一条聚合 SQL）+ 推荐位（一条 SQL），**禁止**循环查。
2. `ProductVO.price` 用 `PriceUtil.toYuan(cent)`，返回 `BigDecimal`，Jackson 序列化成 `99.00`。
3. `detail.facts` 用 `LinkedHashMap` 保序。
4. `siblings` 查同类其它商品，limit 4，排除自己。
5. 缓存：`@Cacheable(cacheNames="home", key="'home'")`，写操作 `@CacheEvict(allEntries=true)`。

**验收**：

```bash
curl -s localhost:8080/api/v1/c/home | python -m json.tool | head -40
curl -s localhost:8080/api/v1/c/categories/snacks?sub=nut
curl -s "localhost:8080/api/v1/c/products?keyword=坚果"
curl -s localhost:8080/api/v1/c/products/snacks-1
```

判断标准：

- `c/home` 的 `categories` 长度 14，每个 `subs` 长度 8，`count` 有 0 有非 0。
- `featured` 长度 12。
- `products?keyword=坚果` 的 `total` 与前端搜索"坚果"得到的 6 条一致。
- `products/snacks-1` 的 `facts` 顺序与 `catalog.ts` 里一致（规格在前，不是字母序）。

---

### T5 购物车（1 天）

**目标**：加购、改量、勾选、删除、合并，全部服务端持久化。

**产出**：`owl-trade` 的 `CartController` / `CartService` / `TCartItemMapper` / `CartVO` / `CartLineVO` / `CartSummaryVO`。

**要点**：

1. `GET /cart` 一次 join 商品表返回，**禁止**先查行再循环查商品。
2. 每次写操作后返回**完整购物车**，前端直接 `store.lines = resp.lines`，不用再拉一次。
3. `summary` 计算复用 `PriceCalculator`（只算商品部分，不算券和运费），保证与结算页一致。
4. 合并接口 `POST /cart/merge` 用 `INSERT ... ON DUPLICATE KEY UPDATE qty = LEAST(99, qty + VALUES(qty))`。

**验收**：

```bash
TOKEN=...   # T3 拿到的
curl -s -X POST localhost:8080/api/v1/cart/items -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"productId":"snacks-1","qty":2}'
curl -s -X PATCH localhost:8080/api/v1/cart/items/snacks-1 -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"qty":3}'
curl -s localhost:8080/api/v1/cart -H "Authorization: Bearer $TOKEN"
```

判断：`summary.count=3`、`summary.amount=297.00`、`isFreeShipping=true`（297 > 99）。

---

### T6 订单 + 支付 + 退款（2 天，最难）

**目标**：从结算预览到退款到账全链路闭环。

**产出**：

```
owl-trade/
├─ controller/{CheckoutController,OrderController}.java
├─ service/{CheckoutService,OrderService,PriceCalculator,OrderNoGenerator}.java
├─ mapper/{TOrderMapper,TOrderItemMapper,TOrderStatusLogMapper}.java
└─ vo/{PreviewVO,OrderBriefVO,OrderDetailVO,OrderCountVO}.java
owl-payment/
├─ controller/PaymentController.java
├─ service/{PaymentService,RefundService}.java
├─ channel/{PaymentChannel,MockPaymentChannel}.java
└─ vo/{PayVO,RefundVO}.java
```

**实现顺序（不要跳）**：

1. `PriceCalculator` + 单元测试（覆盖：满 99 免运费、不满收 5 元、券后不满 99 仍收运费、券超门槛）。
2. `POST /trade/preview`（只读，最好调）。
3. `POST /trade/orders`（事务 + 乐观锁 + 幂等）。
4. `GET /trade/orders` / `{orderNo}` / `counts`。
5. `POST /pay/mock/{orderNo}` + `/pay/mock/callback`。
6. `cancel` / `confirm` / `refund`。
7. 定时任务关单。

**必须写的测试**：

| 用例 | 期望 |
|---|---|
| 同一 `clientToken` 提交两次 | 返回同一 `orderNo`，只扣一次库存 |
| 库存 1 件，两个用户同时下单 | 一个成功，一个 30003 |
| 支付回调重复 3 次 | 订单只从 10 → 20 一次，`pay_time` 不变 |
| 取消订单后再支付 | 50002 |
| 退款两次 | 第二次 50003 |
| 券已用再下单 | 40005 |

**验收**：

```bash
# 预览
curl -s -X POST localhost:8080/api/v1/trade/preview -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"lines":[{"productId":"snacks-1","qty":2}]}'

# 下单
curl -s -X POST localhost:8080/api/v1/trade/orders -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"lines":[{"productId":"snacks-1","qty":2}],"addressId":1,"clientToken":"test-001","fromCart":true}'

# 支付
curl -s -X POST localhost:8080/api/v1/pay/mock/OWL20261028000001 -H "Authorization: Bearer $TOKEN"

# 回调
curl -s -X POST localhost:8080/api/v1/pay/mock/callback -H 'Content-Type: application/json' \
  -d '{"payNo":"P...","mockToken":"mock_...","success":true}'

# 订单详情应为 status=20
curl -s localhost:8080/api/v1/trade/orders/OWL20261028000001 -H "Authorization: Bearer $TOKEN"
```

---

### T7 个人中心 + 消息（1 天）

**目标**：`/m/profile` 一个接口能给前端个人中心全部数据。

**产出**：`owl-auth` 的 `ProfileController` / `AddressController` / `UserCouponController`；`owl-message` 的 `MessageController` / `MessageService` / `NMessageMapper`。

**要点**：

1. `orderCounts` 用一条 `SELECT status, COUNT(*) ... GROUP BY status` 查完，再在内存里映射，**禁止** 4 条 count。
2. 消息 `time` 格式化抽 `TimeTextUtil`，单测覆盖 6 种分支。
3. 消息模板抽 `MessageTemplate` 枚举（订单创建/支付成功/已发货/退款到账/欢迎），业务代码只传参数。
4. 地址手机号返回时打码（`138****8000`）。

**验收**：

```bash
curl -s localhost:8080/api/v1/m/profile -H "Authorization: Bearer $TOKEN"
curl -s "localhost:8080/api/v1/m/messages?kind=order" -H "Authorization: Bearer $TOKEN"
curl -s localhost:8080/api/v1/m/messages/unread -H "Authorization: Bearer $TOKEN"
```

判断：`profile.orderCounts` 与订单列表各状态数量一致；消息 `time` 是"12 分钟前"这类中文，不是 ISO 字符串。

---

### T8 AI 助手（半天）

**目标**：前端配一个环境变量就能从本地演示切到真模型。

**产出**：`owl-assistant` 的 `AssistantController` / `AssistantService` / `DeepSeekClient` / `AiUsageLogMapper`。

**要点**：

1. 用 `RestClient`（Boot 3.2 自带）或 `WebClient`，**不要**引第三方 SDK。
2. 请求 DeepSeek：

```java
Map<String, Object> body = Map.of(
    "model", props.getModel(),
    "messages", messages,
    "temperature", 0.6,
    "max_tokens", 600,
    "stream", false
);
```

3. 响应取 `choices[0].message.content`，为空则走兜底。
4. 限流用 `RedisTemplate.opsForValue().increment(key)` + `expire`。
5. `context` 不要原样透传（前端可能传超大对象），只取需要的 8 个字段重新组装。

**验收**：

```bash
curl -s -X POST localhost:8080/api/v1/assistant/chat \
  -H 'Content-Type: application/json' \
  -d '{"model":"deepseek-chat","messages":[{"role":"user","content":"帮我凑到免运费"}],"context":{"cartCount":2,"cartAmount":198,"cartCheckedAmount":198,"cartSaved":60,"toFreeShipping":0,"isFreeShipping":true,"freeShippingAt":99,"lines":[{"name":"每日混合坚果 750g","qty":2,"price":99}]}}'
```

判断：`data.reply` 是中文、提到了 ¥198 和免运费、200 字以内。

**前端切真模型**：在 `owl-web/.env.local` 加一行

```
VITE_ASSISTANT_API=http://localhost:8080/api/v1/assistant
```

重启 `npm run dev`，面板角标从"演示"消失（`REMOTE_ENABLED` 变 true）。

---

### T9 B 端（1 天）

**目标**：不写后台页面，接口能用 curl / Apifox 跑通即可。

**产出**：`owl-admin` 的 `AdminAuthController` / `AdminProductController` / `AdminOrderController` / `AdminStatsController` / `AdminAuthInterceptor`。

**要点**：

1. B 端 token `scope=admin`，`AdminAuthInterceptor` 校验 scope，C 端 token 直接 403。
2. 商品新增 `code` 生成要加锁（Redis `lock:product:code:{categoryCode}`），否则并发会出现重复 code。
3. 库存调整走 `PATCH .../stock`，`delta` 正负都允许，但不能减到负数（`stock + delta >= 0`）。
4. 所有写操作记 `s_admin_log`，AOP 实现，不要每个方法手写。

**验收**：

```bash
ADMIN_TOKEN=$(curl -s -X POST localhost:8080/api/v1/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123456"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")

curl -s "localhost:8080/api/v1/admin/products?page=1&size=5" -H "Authorization: Bearer $ADMIN_TOKEN"
curl -s localhost:8080/api/v1/admin/stats/overview -H "Authorization: Bearer $ADMIN_TOKEN"
```

判断：C 端 token 访问 `/admin/products` 返回 403。

---

### T10 微服务拆分（按需，2-3 天）

**前置**：第 12 章验收清单全过。

按 9.8 的 7 步走，每步独立投喂：

```
按《OWL 商城后端开发文档》9.3-9.5，把 owl-gateway 拆出来。
当前单体在 8080，网关也监听 8080 会冲突，单体先改 8088。
只做网关 + 路由 + 鉴权过滤器，不改任何业务代码。
产出：owl-gateway 模块、路由配置、AuthGlobalFilter、验收 curl。
```

---

## 11. 前端对接改造清单

> 后端全绿之后再动前端。改完一个页面就验一个，不要一次全改。

### 11.1 新增请求层

**`src/services/http.ts`**

```ts
const BASE = import.meta.env.VITE_API_BASE ?? '/api/v1'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

/** 从会话里取 token，取不到返回 null。token 与 user 存在同一个 key 下，见 11.5 */
function currentToken(): string | null {
  const raw =
    localStorage.getItem('owl.auth.session.v1') ?? sessionStorage.getItem('owl.auth.session.v1')
  if (!raw) return null
  try {
    return JSON.parse(raw).accessToken ?? null
  } catch {
    return null
  }
}

export async function request<T>(
  path: string,
  init: RequestInit & { auth?: boolean } = {},
): Promise<T> {
  const token = currentToken()
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  if (init.auth !== false && token) headers.set('Authorization', `Bearer ${token}`)

  const res = await fetch(`${BASE}${path}`, { ...init, headers })
  if (res.status === 401) {
    // 走一次 refresh，失败才踢回登录
    if (await refreshToken()) return request<T>(path, init)
    throw new Error('登录已过期')
  }
  const body = (await res.json()) as ApiResult<T>
  if (body.code !== 0) throw new Error(body.message || `请求失败(${body.code})`)
  return body.data
}

/** 伪代码：失败时清会话并跳登录 */
async function refreshToken(): Promise<boolean> { /* POST /auth/refresh */ }
```

要求：

- `VITE_API_BASE=http://localhost:8080/api/v1` 写进 `.env.development`。
- `refreshToken()` 用 `POST /auth/refresh`，成功写回两个 token，失败清空并 `router.push('/login')`。

### 11.2 各 store 改造

| 文件 | 改什么 |
|---|---|
| `stores/auth.ts` | `signIn` → `POST /auth/login`；`signUp` → 先 `/auth/sms-code` 再 `/auth/register`；`signInAsGuest` → `/auth/guest`；新增 `token` / `refreshToken` ref 与 `restore()`；`signOut` 调 `/auth/logout` |
| `stores/cart.ts` | 保留本地缓存作为离线兜底，但 `add/remove/setQty/toggle` 改成调接口后**用返回的 lines 覆盖**；新增 `mergeLocal()` |
| `stores/messages.ts` | 删掉 `SEED`，`load()` 拉接口；`markRead/markAllRead/remove` 调接口后本地同步 |
| `stores/checkout.ts` | **不改**（继续 localStorage），只在 `submit` 前调 `/trade/preview` 校验价格 |
| 新增 `stores/catalog.ts` | 缓存 `/c/home`、类目详情、商品详情，带 loading 与 error |

### 11.3 页面改造

| 页面 | 改动 |
|---|---|
| `views/home/catalog.ts` | 保留作为 **fallback 静态数据**；新增 `services/catalog.ts` 提供 `fetchHome()`；`HomeView` 优先用接口数据，失败时回落静态 |
| `views/home/HomeView.vue` | `searchHits` 改为调 `/c/products?keyword=`（加 250ms 防抖）；`FEATURED` 用接口；`pickCat` 改为 `await catalog.loadCategory(id)` 再切内部视图（`/c/home` 不返回 items） |
| `views/category/CategoryView.vue` | `findCategory` 改为 `catalog.loadCategory(catId, sub)` |
| `views/item/ProductDetailView.vue` | `findProduct` 改为 `catalog.loadProduct(id)` |
| `views/checkout/CheckoutView.vue` | `submit()` 调 `POST /trade/orders`，成功跳订单详情（本期可先回首页 + toast） |
| `views/messages/MessagesView.vue` | 从 store 的 `load()` 取数，删除本地 SEED |
| `views/profile/ProfileView.vue` | `ORDERS` 的 count 改为 `profile.orderCounts` |
| `views/login/LoginView.vue` | `onLogin` / `onRegister` / `onGuest` 改为 `await` 版本；`sendCode` 调接口；`PENDING_MS` 假延时删掉 |

### 11.4 图片与代理

1. 商品图统一走后端 `/static/products/{code}.jpg`，前端 `image` 直接用接口返回值，**不要**再 `import.meta.glob`。
2. `vite.config.ts` 加代理（开发期避免 CORS）：

```ts
server: {
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true },
    '/static': { target: 'http://localhost:8080', changeOrigin: true },
  },
},
```

3. 配了代理后 `VITE_API_BASE=/api/v1` 即可，不用写完整域名。

### 11.5 登录态字段迁移

前端 `localStorage` 里旧格式是 `AuthUser`，新格式是 `{ accessToken, refreshToken, user }`。`auth.ts` 的 `restore()` 要兼容两种：

```ts
const raw = localStorage.getItem('owl.auth.session.v1')
if (!raw) return null
const parsed = JSON.parse(raw)
if ('accessToken' in parsed) return parsed          // 新格式
return { user: parsed, accessToken: null }          // 旧格式，提示重新登录
```

---

## 12. 验收标准

### 12.1 功能验收（curl 全过）

- [ ] 注册 → 登录 → `/auth/me` 返回正确用户
- [ ] 首页 15 类目 × 8 细分（120 个，含 count=0）、12 件推荐
- [ ] 分类页 `?sub=nut` 只返回坚果，`?sub=空细分` 返回整类 + `fallback=true`
- [ ] 搜索"坚果"命中 6 件，与前端静态数据一致
- [ ] 商品详情 facts 顺序正确、siblings 4 件不重复
- [ ] 加购 3 件 → 购物车 `count=3`、`amount` 正确、刷新后仍在
- [ ] 购物车勾选/取消勾选，`checkedAmount` 跟着变
- [ ] 结算预览金额 = 商品 - 券 + 运费
- [ ] 下单成功扣库存，`c_stock_log` 有记录
- [ ] 模拟支付成功，订单 10 → 20
- [ ] 取消订单回补库存、释放券
- [ ] 确认收货 30 → 40
- [ ] 退款 20/30 → 70，库存回补
- [ ] 个人中心订单角标与列表数量一致
- [ ] 消息未读数正确，已读后减少
- [ ] AI 助手回复含真实购物车金额
- [ ] B 端商品上下架、库存调整生效
- [ ] C 端 token 访问 B 端接口 403

### 12.2 边界与异常

- [ ] 库存 0 商品加购 → 30003
- [ ] 库存 1 件两人同时下单 → 一个 30003
- [ ] 重复 `clientToken` 下单 → 同一订单
- [ ] 支付回调重复 3 次 → 只成功一次
- [ ] 越权访问他人订单 → 10003
- [ ] 未登录访问 `/cart` → 10002
- [ ] 密码错 5 次 → 20005
- [ ] 验证码复用 → 20002
- [ ] 券不足门槛 → `usable=false` 不报错
- [ ] 超时 30 分钟订单自动关闭（可把阈值改成 1 分钟测）

### 12.3 工程质量

- [ ] `mvn clean package` 零警告（至少零 error）
- [ ] 核心类单测覆盖：`PriceCalculator`、`OrderStatus`、`TimeTextUtil`、`JwtService`
- [ ] 日志无密码、无完整手机号、无 API Key
- [ ] 所有接口有 Knife4j 注解（`@Tag` / `@Operation`），`/doc.html` 能看到
- [ ] `git log` 每个任务一个 commit，message 说清做了什么

### 12.4 性能（本机基线）

- [ ] `/c/home` P95 < 50ms（缓存命中 < 10ms）
- [ ] `/c/products?keyword=` P95 < 80ms
- [ ] 下单接口 P95 < 200ms（含 3 次 SQL + 1 次 Redis）
- [ ] AI 接口 P95 < 8s（模型本身耗时，不算超时）

---

## 附录 A 错误码表

| 码 | 含义 | HTTP |
|---|---|---|
| 0 | 成功 | 200 |
| 10001 | 参数错误 | 200 |
| 10002 | 未登录或登录已过期 | 401 |
| 10003 | 无权限（越权访问） | 403 |
| 10004 | 资源不存在 | 200 |
| 10005 | 重复提交 | 200 |
| 10006 | 请求过于频繁 | 429 |
| 20001 | 账号或密码错误 | 200 |
| 20002 | 验证码错误或已过期 | 200 |
| 20003 | 手机号已注册 | 200 |
| 20004 | 账号已被禁用 | 200 |
| 20005 | 尝试次数过多，请稍后再试 | 200 |
| 30001 | 商品或类目不存在 | 200 |
| 30002 | 商品已下架 | 200 |
| 30003 | 库存不足 | 200 |
| 30004 | 该类目下仍有商品，不能删除 | 200 |
| 40001 | 购物车为空 | 200 |
| 40002 | 订单不存在 | 200 |
| 40003 | 订单状态不允许此操作 | 200 |
| 40004 | 订单已超时关闭 | 200 |
| 40005 | 优惠券不可用 | 200 |
| 50001 | 支付单不存在或参数错误 | 200 |
| 50002 | 支付状态异常 | 200 |
| 50003 | 退款申请重复或金额超限 | 200 |
| 60001 | 消息不存在 | 200 |
| 70001 | AI 服务暂时不可用 | 200 |
| 70002 | AI 调用次数已达上限 | 200 |
| 90001 | 系统内部错误 | 500 |

## 附录 B 状态枚举

**订单状态 `t_order.status`**

| 值 | 名称 | 前端文案 |
|---|---|---|
| 10 | 待付款 | 待付款 |
| 20 | 待发货 | 待发货 |
| 30 | 待收货 | 待收货 |
| 40 | 已完成 | 已完成 |
| 50 | 已取消 | 已取消 |
| 60 | 退款中 | 退款中 |
| 70 | 已退款 | 已退款 |

**支付状态 `p_payment.status`**：0 待支付 / 1 成功 / 2 失败 / 3 关闭 / 4 已退款
**退款状态 `p_refund.status`**：0 处理中 / 1 成功 / 2 失败
**用户等级 `m_user.level`**：`black` 黑卡 / `normal` 普通 / `guest` 访客
**优惠券类型 `mk_coupon.type`**：1 满减 / 2 折扣
**用户券状态 `mk_user_coupon.status`**：0 未用 / 1 已用 / 2 过期
**库存流水类型 `c_stock_log.type`**：1 下单 / 2 取消回补 / 3 入库 / 4 后台调整

## 附录 C 投喂话术模板

### C.1 单任务投喂

```
你是我的后端开发搭档。严格遵守《OWL 商城后端开发文档》v1.0。

本次只做任务 T3（认证模块），文档对应章节：6.2、8.1、8.2、8.3。

约束：
1. 包名 com.owl，类名/字段名/接口路径与文档逐字一致。
2. 不实现 T3 以外的功能，不改其它模块。
3. 不引入文档未列出的依赖。
4. 每个类给出完整代码，不要用 "..." 省略。
5. 最后给验收用的 curl 命令和预期输出。

先复述你的实现计划（5 行以内），我确认后再出代码。
```

### C.2 报错修复

```
按文档 T3 实现后出现以下错误，请定位并给修复方案。

环境：JDK 21 / Boot 3.2.12 / SCA 2023.0.3.2
错误日志：
<粘贴完整堆栈，不要只贴最后一行>

相关代码：
<粘贴报错类>

要求：
1. 先判断是依赖版本问题还是代码问题。
2. 给最小改动方案，不要重写整个类。
3. 说明为什么会这样，避免下次再犯。
```

### C.3 代码审查

```
对照《OWL 商城后端开发文档》检查以下代码，只列问题不改代码：

1. 是否有跨层调用（Controller 直接注入 Mapper）？
2. 金额是否全程用 long 分，有没有中途转 double？
3. 事务边界是否正确（@Transactional 在 service.impl）？
4. 是否有 N+1 查询？
5. 越权校验是否带 user_id 条件？
6. 幂等是否覆盖（clientToken / 支付回调 / 退款）？

代码：
<粘贴>
```

### C.4 微服务拆分

```
按文档第 9 章，把 owl-catalog 拆成独立服务。

要求：
1. 新建 owl-catalog-service 模块，端口 8082，服务名 owl-catalog。
2. 保留原模块代码，先不删，保证单体还能跑。
3. 加 Nacos 注册 + 配置中心，配置从 owl-catalog-dev.yml 读。
4. 网关加路由 /api/v1/c/** → lb://owl-catalog。
5. 给启动命令和验证步骤（Nacos 服务列表截图要点 + curl）。

不要动 trade / payment 模块。
```

---

**文档结束。**

> 写代码时如果发现文档与实际冲突，**以文档为准改代码**；如果文档本身有错，先改文档再改代码，别让两者分叉。
