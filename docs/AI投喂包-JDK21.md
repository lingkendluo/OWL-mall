# OWL 商城后端 · AI 投喂包（JDK 21 / IDEA）

> 配套规格书：`docs/OWL商城-后端开发文档.md`（v1.1，已按 JDK 21 修订）
> 适用：IDEA 2023.3+ / JDK 21 / Spring Boot 3.2.12 / Spring Cloud Alibaba 2023.0.3.2
> 用法：**这份文件是"指挥棒"，规格书是"图纸"，前端代码是"验收标准"**。三者一起给 AI，但每一轮只喂一章图纸。

---

## 0. 三分钟上手

### 0.1 你手里应该有四样东西

| # | 材料 | 来源 | 每轮都给吗 |
|---|---|---|---|
| A | **本文件**（投喂包） | 就是你现在看的这份 | 是，整份 |
| B | **规格书** | `docs/OWL商城-后端开发文档.md` | 只给当前任务对应的章节（见每轮标注） |
| C | **接口契约（DTO/VO）** | `docs/API契约-DTO-VO.md` | 当轮涉及的 DTO/VO 小节 |
| D | **前端代码片段** | `owl-web/src/...` | 第 0 轮给全量清单，后面按需 |

> 接口文档在 Apifox 里：导入 `docs/owl-mall-openapi.json` 即可看到 63 个接口、30 个 DTO、39 个 VO。
> 但 **AI 读不到你的 Apifox**，所以给 AI 的是 `API契约-DTO-VO.md`（同样的内容，Markdown 版）。
>
> 如果用的 AI 不支持上传附件（比如某些网页版），就把 B 的对应章节**整段粘贴**进对话，别只给章节号。

### 0.2 每一轮怎么发

```
┌─────────────────────────────────────────────┐
│ 1. 通用前缀（第 1 节，原样复制）              │
│ 2. 本轮任务块（第 3~13 节，原样复制）          │
│ 3. 规格书对应章节（复制粘贴或附件）            │
│ 4. 契约文档里当轮的 DTO / VO 表               │
│ 5. 本轮需要的前端文件（按附录 A 截取）         │
└─────────────────────────────────────────────┘
```

**AI 回复后先别急着让它写下一轮**：跑一遍它给的验收命令，过了再继续。没过就用第 14 节的修错话术。

### 0.3 两种投喂方式

**方式 A：对话式**（网页版、API、不支持读目录的工具）

按附录 A 手动截取文件行号，粘贴进对话。缺点是麻烦，优点是上下文干净、token 省一半。

**方式 B：仓库式**（Cursor / Claude Code / IDEA AI Assistant / Copilot 等能读项目目录的工具）

不用截取，直接把路径告诉它，但要给"读什么、不读什么"的白名单，否则它会去读 3000 行的 CSS：

```
项目在 E:\OWL-System。
前端：owl-web，后端（待建）：owl-server。
规格书：docs/OWL商城-后端开发文档.md
投喂包：docs/AI投喂包-JDK21.md
接口契约（DTO/VO）：docs/API契约-DTO-VO.md
OpenAPI：docs/owl-mall-openapi.json（只读，不要改）
商品种子：docs/catalog.json

只允许读以下前端文件（其它一律不要读）：
- owl-web/src/stores/auth.ts
- owl-web/src/stores/cart.ts
- owl-web/src/stores/checkout.ts
- owl-web/src/stores/messages.ts
- owl-web/src/services/assistant.ts
- owl-web/src/router/index.ts
- owl-web/src/utils/format.ts
- owl-web/src/views/home/useShopActions.ts
- owl-web/src/views/home/catalog.ts（只看 interface 定义与文件末尾的 export，不要读 2000 行数据）
- owl-web/src/views/*/*.vue（只看 <script> 部分，跳过 <template> 和 import 的 css）

禁止读：任何 .css、assets/ 下的图片、GalaxyBg.vue、doll-scene.js、dist/、node_modules/。
```

### 0.4 上下文预算（很重要）

前端代码全量约 1.5 万行，加上规格书 3200 行，**任何模型都塞不下**。按这个配比：

| 轮次 | 规格书 | 前端代码 | 估算 token |
|---|---|---|---|
| 第 0 轮 | 目录 + 第 1、2 章 | 附录 A 的"必给"清单（约 1200 行） | 25k |
| 第 1 轮 | 3、4 章 | 无 | 12k |
| 第 2 轮 | 5 章 | `catalog.ts` 类型 + 一段样例 | 15k |
| 第 3~10 轮 | 6.x 单节 + 7.x 相关节 | 对应 store / 页面 script | 15~20k |
| 第 11 轮 | 9 章 | 无 | 10k |

**超过 30k 就拆**：先让它写接口层（Controller + DTO/VO），下一轮再写 Service + Mapper。

---

## 1. 通用前缀（每次必带）

复制下面整段，放在每条消息最前面：

```
你是我的后端开发搭档，负责 OWL 商城后端。严格遵守以下约束，违反任何一条都算交付失败。

【项目】
- 前端已完成：Vue 3.5 + TS + Pinia + Vite，位于 E:\OWL-System\owl-web，页面和交互都能跑，只是数据来自本地静态文件。
- 后端要新建：E:\OWL-System\owl-server，Java 21 + Spring Boot 3.2.12 + Spring MVC + MyBatis-Plus 3.5.7。
- 微服务依赖（Spring Cloud Alibaba 2023.0.3.2）前期只引依赖、不启用，第 11 轮才拆。
- 数据库 MySQL 8（库 owl_mall）、缓存 Redis 7、注册配置中心 Nacos 2.3（本机已装）。

【硬约束】
1. 包名根 com.owl，模块名、类名、字段名、接口路径必须与规格书逐字一致，不要"优化"命名。
2. 金额：数据库存 bigint 分（字段 _cent），Service 内部一律 long 运算，只在 VO 层用 PriceUtil.toYuan 转元。禁止 double、禁止中途转 BigDecimal 再转回来。
3. 对外 ID：用户 id、商品 id、类目 id、细分 id 全部返回 String。用户 id 用 String.valueOf，商品用 code（如 snacks-1）。返回数字会导致前端会话丢失。
4. 不用 Spring Security 全家桶，只用 spring-security-crypto 的 BCryptPasswordEncoder；鉴权用 HandlerInterceptor + UserContext(ThreadLocal)。
5. 不用 JPA / Hibernate，持久层只用 MyBatis-Plus。
6. 不用 @Autowired 字段注入，统一用 Lombok @RequiredArgsConstructor 构造器注入。
7. 不引入规格书没列的依赖。需要新依赖先说明理由，等我确认。
8. 不改前端代码。前端只作为契约来源，任何"顺手改一下前端"的建议都不要做。
9. Controller 不写业务、不注入 Mapper；@Transactional 只加在 service.impl 的 public 方法上。
10. 严格三层模型：入参 DTO 放 com.owl.{模块}.dto.XxxRequest，出参 VO 放 com.owl.{模块}.vo.XxxVO，
    字段名与 docs/API契约-DTO-VO.md 逐字一致，一个字段都不许改名或增删。Entity 只在 Service 与 Mapper 之间流转。
11. Controller 方法签名里只允许 DTO / VO / Result，禁止出现 Entity；Service 也禁止返回 Entity。
12. VO 里金额是 BigDecimal（元，两位小数），id 是 String，时间字段是 Long 毫秒时间戳，字段名以 At 结尾。
13. 每个模块必须有 converter 包，Entity → VO 的转换写在里面，禁止在 Controller 里手拼 VO。

【输出格式】
1. 先给本轮要创建/修改的文件树（只列路径，一句话说明作用）。
2. 按 DTO → VO → Converter → Mapper → Service → Controller 的顺序给代码，先模型后逻辑。
3. 再逐个文件给完整代码，用带语言标记的代码块包起来（java / xml / sql / yaml），并在块前标注文件路径。
4. 不要用 "// ..." 省略任何方法体；不要写"其余同上"。
5. 最后给：启动命令、验收 curl（含预期输出）、本轮遗留问题。
6. 代码里的中文注释只写"为什么"，不写"是什么"。

【环境】
- JDK 21，编译参数 release=21、parameters=true，Lombok 1.18.34。
- IDEA 开发，Maven 多模块，父 POM 统一管版本，子模块不写 <version>。
- 时区 Asia/Shanghai，接口时间字段一律毫秒时间戳（long），字段名以 At 结尾。

如果本轮任务与以上约束冲突，先指出来，不要擅自改。
```

---

## 2. 第 0 轮：勘察与计划（不写代码）

**目的**：让 AI 先把前端契约读透，输出一份"我理解到的字段对照表"，你确认无误再开工。这一轮能挡掉 80% 的返工。

**附件**：附录 A 的"必给"清单 + 规格书第 1、2 章。

```
本轮只做勘察，不写任何代码。

附件是 OWL 商城前端的核心文件。请完成三件事：

1. 读完后端要对接的全部数据契约，输出一张表：
   前端文件 → 类型/Store → 字段清单 → 后端对应 VO → 注意事项。
   特别注意：字段类型（string/number/可选）、金额单位、时间格式、可空性。

2. 列出前端已经写死、但后端必须改掉的假设。例如：
   - 登录成功后前端本地生成 id/昵称/积分
   - 购物车存在 localStorage
   - 商品价格来自 catalog.ts
   每条写：现状 → 接后端后怎么变 → 前端要改哪个文件。

3. 指出规格书里与前端代码不一致的地方（如果有）。逐条给：规格书怎么写、前端怎么写、建议以哪个为准。

不要给实现方案，不要给代码。我要先确认你读懂了。
```

**验收标准**：表格里必须出现这几条，缺一条说明它没读进去：

- `AuthUser.id` 是 string，`joinedAt` 是毫秒数
- `Product.price/list` 是元（number），不是分
- `CartLine.id` 是商品 code，`qty` 上限 99，`checked` 持久化
- `Sub.count = 0` 表示补货中，不能过滤掉
- `Message.time` 是展示文案（"12 分钟前"），不是 ISO 时间
- `AssistContext` 的 8 个字段

---

## 3. 第 1 轮：工程骨架

**规格书**：第 3 章（技术栈/版本/JDK 21 配置）、第 4 章（工程结构）、任务 T1
**前端**：不需要

```
本轮做任务 T1：Maven 多模块骨架 + 公共基础设施，不写任何业务。

要求：
1. 目录结构严格按规格书 4.1，包括 owl-common / owl-infra / owl-modules / owl-admin / owl-boot。
2. owl-common 只放不依赖 Spring Web 的类：Result、PageResult、ErrorCode、BizException、UserContext、LoginUser、PriceUtil。
3. owl-infra 放 Web 基础设施：GlobalExceptionHandler、TraceFilter、WebConfig、MybatisPlusConfig。
4. WebConfig 用 configurePathMatch 给 @RestController 加 /api/v1 前缀，不要用 server.servlet.context-path（会把 /static 图片也加前缀）。
5. 父 POM 的 maven-compiler-plugin 必须加 <parameters>true</parameters>，否则 Spring 6 读不到参数名。
6. application.yml 按规格书 4.4，dev profile 连本机 MySQL/Redis。
7. HealthController 放 owl-boot，GET /health 返回 {"status":"UP","time":...}。
8. 错误码枚举按附录 A 全量写，不要只写用到的那几个。

验收：
- mvn -q clean package -DskipTests 通过
- java -jar owl-boot/target/owl-boot-1.0.0.jar 启动无报错
- curl http://localhost:8080/api/v1/health 返回 code=0
```

**注意**：这一轮 AI 容易犯的错是把 `GlobalExceptionHandler` 塞进 `owl-common`（然后 common 被迫依赖 web）。发现就让它按 4.1 的分层改回来。

---

## 4. 第 2 轮：数据库与初始化

**规格书**：第 5 章（全部）、任务 T2
**前端**：`catalog.ts` 类型定义（1-68 行）+ 1 个类目的 RAW 样例（约 70-140 行）

```
本轮做任务 T2：建库 + 实体 + 商品数据初始化。

要求：
1. 输出两个 SQL 文件（放 owl-boot/src/main/resources/db/）：
   - 01_schema.sql：规格书 5.3 的 20 张表，一字不改照抄（含索引、注释、字符集）。
   - 02_init.sql：规格书 5.6 的券模板，不要写用户（密码由 DataSeeder 加密）。
2. 写 Entity（只写 owl-auth 和 owl-catalog 用到的 8 张表）：
   MUser、MUserAddress、CCategory、CSubCategory、CProduct、CProductDetail、CStockLog、MUserCoupon。
   注意：
   - c_product_detail 的 facts_json / tips_json 用 JacksonTypeHandler，类上加 @TableName(autoResultMap = true)。
   - 时间字段用 LocalDateTime。
   - 逻辑删除字段加 @TableLogic。
3. 写 CatalogSeeder（ApplicationRunner）：
   - 读 classpath:seed/catalog.json
   - 按 code upsert 类目、细分、商品、详情，已存在跳过
   - image 写 /static/products/{code}.jpg，stock=200，sales 用固定种子伪随机
   - 前端 FEATURED_IDS 那 12 件置 is_featured=1
   - 开关 owl.seed.enabled，默认 true
4. 写 DataSeeder：admin/admin123456（super）、测试用户 13800138000/abc12345、
   给测试用户发 2 张券、加 1 个默认地址。密码用 BCrypt，日志不打印密码。
5. MybatisPlusConfig 加分页、乐观锁、防全表更新三个插件。

验收：
- 启动日志出现 catalog seeded: 15 categories / 120 subs / 90 products
- SELECT COUNT(*) FROM c_product = 90，c_sub_category = 120，is_featured = 12
- SELECT code,name,price_cent,is_featured FROM c_product WHERE is_featured=1 返回 12 行
```

**前置动作（你自己做，AI 做不了）**：先跑 `node scripts/export-catalog.mjs`（仓库里已有，实测通过）生成 `docs/catalog.json`，细节见规格书 5.4。期望输出 `15 categories / 120 subs / 90 products`。

---

## 5. 第 3 轮：认证模块

**规格书**：6.2、8.1、8.2、8.3、任务 T3
**前端**：`stores/auth.ts`（全 300 行）+ `views/login/LoginView.vue` 的 478-726 行

```
本轮做任务 T3：认证模块，接口 6.2.1~6.2.8 全部实现。

要求：
1. JwtService 用 JJWT 0.12.5 的新 API（Jwts.builder().subject().signWith(key)，不是 0.9 的 setSubject）。
2. UserVO 的 id 是 String，joinedAt 是毫秒 long，level 是 String（black/normal/guest），
   字段名与前端 AuthUser 完全一致，不要加 userNickName 这类"更规范"的命名。
3. LoginVO 返回 accessToken / refreshToken / expiresIn / user 四个字段。
4. refreshToken 存 Redis（owl:auth:refresh:{userId}:{jti}），刷新时轮换，旧的删除。
5. 注册流程：校验验证码 → 查重 → 建用户（level=normal）→ 发新人券 → 写欢迎消息，全部在一个事务里。
6. 登录失败 5 次锁 15 分钟，账号不存在和密码错误都返回 20001，不要区分。
7. 游客登录建真实用户行（is_guest=1，member_no 为 NULL，密码随机）。
8. JwtAuthInterceptor 的 afterCompletion 必须 UserContext.clear()。
9. 白名单按规格书 8.1，用 excludePathPatterns 配，不要在每个 Controller 上写注解。

验收（规格书 T3 的四条 curl 全跑）：
- 注册后 data.user.id 是字符串 "1002"
- 登录拿到 token，GET /auth/me 返回同一用户
- 密码错 5 次后第 6 次返回 20005
- 无 token 访问 /auth/me 返回 10002
```

---

## 6. 第 4 轮：商品模块

**规格书**：6.3、7.7、任务 T4
**前端**：`views/home/catalog.ts` 类型 + 样例、`CategoryView.vue` 1-60 行、`ProductDetailView.vue` 1-80 行、`HomeView.vue` 1-165 行

```
本轮做任务 T4：商品模块，接口 6.3.1~6.3.6。

要求：
1. /c/home 一次返回 brand + categories(15，含 8 个细分及 count) + featured(12)，不要分三次查。
2. 细分 count 用一条 LEFT JOIN 聚合 SQL 算，count=0 的细分必须保留（前端显示"补货中"）。
3. ProductVO 的 price/list 用 PriceUtil.toYuan(cent)，返回 BigDecimal，JSON 里是 99.00。
4. detail.facts 用 LinkedHashMap，保持数据库 JSON 的键顺序。
5. /c/categories/{code}?sub= 的 fallback 规则：
   细分有货 → 只返回该细分；细分 0 件 → 返回整类且 fallback=true；不传 sub → 整类。
6. /c/products 的 keyword 要同时匹配商品名、规格、类目名、类目 hint、细分名（对齐前端 searchHits）。
7. /c/products/{code} 带 siblings（同细分优先，不足用同类补，最多 4 件，排除自己，不重复）。
8. 缓存用 Caffeine：home 5 分钟、categories 10 分钟、product:{code} 5 分钟，写操作 evict。

验收：
- /c/home 的 categories 长度 15，每个 subs 长度 8
- /c/products?keyword=坚果 的 total = 6
- /c/products/snacks-1 的 facts 第一个键是"规格"（不是字母序）
- /c/categories/snacks?sub=seed（假设 0 件）返回整类且 fallback=true
```

---

## 7. 第 5 轮：购物车

**规格书**：6.4、7.6、任务 T5
**前端**：`stores/cart.ts`（全 180 行）+ `views/cart/CartView.vue` 1-64 行

```
本轮做任务 T5：购物车，接口 6.4.1~6.4.5。

要求：
1. 每个写接口都返回完整购物车（lines + summary），前端直接覆盖本地状态，不用二次拉取。
2. summary 由服务端算：count/amount/saved/checkedCount/checkedAmount/allChecked/isFreeShipping/toFreeShipping/shippingRate/freeShippingAt/shipFee。
3. 加购时 qty 越界截断到 1-99，不报错；已存在则累加，上限 99；加入时 checked 置 1。
4. 商品下架或库存 0 时加购返回 30002 / 30003；已存在的行下架后返回 invalid=true，不要直接删。
5. 合并接口用 INSERT ... ON DUPLICATE KEY UPDATE qty = LEAST(99, qty + VALUES(qty))。
6. 购物车行删除用物理删除（表没有 deleted 字段）。

验收：
- 加购 3 件后 summary.count=3、amount=297.00、isFreeShipping=true
- 刷新后仍在（服务端持久化）
- PATCH qty=200 后实际是 99，不报错
- 两个用户的数据互不可见
```

---

## 8. 第 6 轮：交易（最重的一轮）

**规格书**：6.5、7.1~7.4、任务 T6 的前半
**前端**：`stores/checkout.ts`（全 140 行）+ `CheckoutView.vue` 1-71 行

```
本轮做任务 T6 的前半部分：计价 + 下单 + 订单查询，先不做支付。

要求：
1. 先写 PriceCalculator 和它的单元测试（4 个用例：满 99 免运费 / 不满收 5 元 / 券后不满 99 仍收运费 / 券超门槛）。
2. /trade/preview 只读，禁止信任前端价格，请求体里根本没有价格字段。
3. /trade/orders 一个事务里做五件事：幂等校验 → 重新计价 → 乐观锁扣库存 → 写订单+订单行+库存流水 → 锁券 →（fromCart=true 时）删购物车行。
4. 库存扣减 SQL 必须带 stock >= qty AND version = ?，影响 0 行抛 30003。
5. 订单号 OWL + yyyyMMdd + 6 位 Redis 序列。
6. 订单列表 status 参数映射：all/unpaid(10)/shipping(30)/review(40且未评价)/after(60,70)。
7. 取消订单要回补库存、退券、写状态流水、发消息。
8. 订单状态流转统一走 OrderStatus.transitTo()，禁止到处 setStatus。

验收：
- 同一 clientToken 提交两次，返回同一 orderNo，库存只扣一次
- 两个用户抢最后 1 件，一个成功一个 30003
- 预览金额 = 商品 - 券 + 运费，与下单返回的 payAmount 一致
- /trade/orders/counts 的数字与列表各状态条数一致
```

---

## 9. 第 7 轮：支付与退款

**规格书**：6.6、7.4、任务 T6 的后半
**前端**：`CheckoutView.vue` 的 submit 逻辑（63-70 行）

```
本轮做任务 T6 的后半：模拟支付 + 退款，接口 6.6.1~6.6.3。

要求：
1. PaymentChannel 接口（create/query/refund/parseCallback）+ MockPaymentChannel 实现，Service 里不许出现 if (channel == "mock")。
2. 发起支付：已存在 status=0 的支付单就复用，不要重复创建。
3. 回调幂等：UPDATE p_payment SET status=1 WHERE pay_no=? AND status=0，影响 0 行直接返回成功。
4. 回调方法加 Redis 锁 lock:pay:{payNo}，防并发。
5. 支付成功：支付单 1、订单 pay_status=1 + status=20 + pay_time、写流水、发消息。
6. 退款：只允许 pay_status=1 且订单状态在 20/30/40；本期全额退；重复申请 50003。
7. 退款成功同步置：退款单 1、支付单 4、订单 70、pay_status=2、回补库存、发消息。
8. 定时任务每分钟关闭超时 30 分钟的未支付订单（回补库存 + 退券 + 发消息），加 Redis 锁。

验收：
- 支付回调重复 3 次，订单只从 10 变 20 一次
- 取消后再支付返回 50002
- 退款两次，第二次 50003
- 退款后库存回到下单前
```

---

## 10. 第 8 轮：个人中心 + 消息

**规格书**：6.7、6.8、任务 T7
**前端**：`ProfileView.vue` 1-112 行、`MessagesView.vue` 1-63 行、`stores/messages.ts`

```
本轮做任务 T7：个人中心 + 地址 + 券 + 积分 + 站内信。

要求：
1. /m/profile 一次返回 user + assets + orderCounts，orderCounts 用一条 GROUP BY 查完，不要 4 条 count。
2. 地址列表返回真实手机号（本人看自己的），订单里才打码。
3. 设置默认地址在同一事务里把其它地址 is_default 置 0。
4. 券查询时顺手把过期的置为 2（惰性过期），不要只靠定时任务。
5. 消息 time 用 TimeTextUtil 格式化：刚刚 / N 分钟前 / N 小时前 / 昨天 HH:mm / M 月 d 日 / yyyy 年 M 月 d 日，写单元测试覆盖 6 个分支。
6. 消息模板抽枚举（订单创建/支付成功/已发货/退款到账/欢迎），业务代码只传参数。
7. 消息只能操作自己的，越权 10003。

验收：
- /m/profile 的 orderCounts 与订单列表各状态数量一致
- 消息 time 是"12 分钟前"这类中文，不是 ISO
- 已读后 unread 减少
```

---

## 11. 第 9 轮：AI 助手

**规格书**：6.9、任务 T8
**前端**：`services/assistant.ts`（全 335 行，重点 1-145 行）

```
本轮做任务 T8：AI 助手接口，对接 DeepSeek。

要求：
1. POST /assistant/chat，请求体就是前端发的那套：{model, messages, context}，不要改字段名。
2. 响应必须含 reply 字段（前端 pickReply 认这个），另外给 model / usage / degraded。
3. 服务端自己拼 system prompt（规格书 6.9.2），忽略前端传来的 system 消息；只取 user/assistant 最后 10 条。
4. context 不要原样透传，重新组装成 8 个字段，序列化成一个 user 消息。
5. 调 https://api.deepseek.com/chat/completions，temperature 0.6、max_tokens 600、超时 30s。
6. 失败不抛 5xx：返回 code=0 + 兜底话术 + degraded=true。
7. 限流：登录 20 次/小时，匿名 5 次/小时（按 IP）。超限 70002。
8. 每次调用写 ai_usage_log（成功失败都写），记录 latency_ms。
9. API Key 只从环境变量 DEEPSEEK_API_KEY 读，禁止写进 yml 明文，禁止返回前端。

验收：
- 规格书 T8 的 curl 返回中文回复，提到 ¥198 和免运费
- 不带 Key 启动时接口返回兜底文案而不是 500
- 同一用户第 21 次调用返回 70002
```

---

## 12. 第 10 轮：B 端

**规格书**：6.10、任务 T9
**前端**：不需要（前端还没有 admin 页面）

```
本轮做任务 T9：B 端接口，只做接口，不做页面。

要求：
1. 独立鉴权：s_admin 表 + scope=admin 的 JWT + AdminAuthInterceptor，C 端 token 访问 /admin/** 一律 403。
2. 商品新增时 code 由后端生成 {categoryCode}-{序号}，生成过程加 Redis 锁，防并发重复。
3. 商品写操作后失效 home / categories / product:{code} 缓存。
4. 库存调整允许正负，但不能减到负数。
5. 每次写操作用 AOP 记 s_admin_log，不要每个方法手写。
6. /admin/stats/overview 的 gmv 只算 pay_status=1 且未退款的订单。

验收：
- admin/admin123456 能登录，拿到 scope=admin 的 token
- C 端 token 访问 /admin/products 返回 403
- 新增商品后 /c/home 能看到
- 库存从 10 减 20 返回错误，库存不变
```

---

## 13. 第 11 轮：微服务拆分（可选）

**规格书**：第 9 章、任务 T10

```
本轮把 owl-catalog 拆成独立服务，其余保持单体。

要求：
1. 新建 owl-catalog-service 模块，端口 8082，服务名 owl-catalog，保留原模块代码不删。
2. 加 Nacos 注册 + 配置中心，配置从 owl-catalog-dev.yml 读，用 spring.config.import 而不是 bootstrap.yml。
3. 新建 owl-gateway（端口 8080，单体改 8088），路由 /api/v1/c/** → lb://owl-catalog。
4. 网关 AuthGlobalFilter 校验 JWT 并把 userId 放进 X-User-Id，同时剥离外部传入的 X-User-Id。
5. 加 --add-opens JVM 参数（JDK 21 下 Nacos 反射需要），写进 README。
6. 不要引 Seata，跨服务一致性用本地消息表。

验收：
- Nacos 服务列表能看到 owl-catalog
- 通过网关访问 /api/v1/c/home 与直连 8088 结果一致
- 停掉 catalog 服务，网关返回 503 而不是超时挂死
```

---

## 14. 修错 / 审查 / 迁移

### 14.1 编译或启动报错

```
按规格书实现后出现以下错误，请定位并修复。

环境：JDK 21 / Boot 3.2.12 / SCA 2023.0.3.2 / MyBatis-Plus 3.5.7

完整堆栈：
<粘贴，不要只贴最后一行>

相关代码：
<粘贴报错的类>

要求：
1. 先判断是依赖版本、JDK 21 兼容、还是代码问题。
2. 给最小改动方案，不要重写整个类。
3. 说明根因，避免下次再犯。
4. 如果是依赖问题，给完整的 dependency 片段，不要只说"升级一下"。
```

### 14.2 代码审查

```
对照规格书审查以下代码，只列问题，不改代码：

检查项：
1. 跨层调用（Controller 注入 Mapper、Service 返回 Entity）
2. 金额是否全程 long 分，有没有出现 double 或中途转元
3. @Transactional 位置是否正确，是否有自调用失效
4. N+1 查询（循环里查库）
5. 越权：SQL 有没有带 user_id 条件
6. 幂等：clientToken / 支付回调 / 退款
7. Entity 有没有漏 @TableLogic / @TableName
8. 是否有硬编码的密钥、手机号、金额

代码：
<粘贴>

输出格式：问题清单（文件:行号 → 问题 → 影响 → 建议改法），按严重程度排序。
```

### 14.3 前端接后端（最后做）

```
后端接口已全部跑通。现在按规格书第 11 章把前端数据源切到后端。

只改数据层，不改 UI：
1. 新增 src/services/http.ts（带 token、401 自动 refresh 一次）
2. 新增 src/services/catalog.ts、cart.ts、order.ts、message.ts
3. 改 stores/auth.ts：signIn/signUp/signInAsGuest 调接口，token 与 user 存同一个 key（owl.auth.session.v1）
4. 改 stores/cart.ts：写操作后直接用接口返回的 lines 覆盖
5. 改 stores/messages.ts：删掉 SEED，改为 load()
6. stores/checkout.ts 不动，只在提交前调 /trade/preview 校验
7. 页面里 catalog.ts 的静态数据保留作为 fallback，接口失败时回落

要求：
- 每改一个文件，告诉我改了哪几行、为什么。
- 不要动任何 CSS 和 template 结构。
- 不要新增依赖（不要 axios，用原生 fetch）。
```

---

## 附录 A 前端附件清单（精确到行号）

### A.1 必给（第 0 轮）

| 文件 | 行数 | 为什么必须给 |
|---|---|---|
| `src/stores/auth.ts` | 全 300 行 | AuthUser 字段、登录/注册/游客流程、normalize 的类型校验 |
| `src/stores/cart.ts` | 全 180 行 | CartLine 字段、FREE_SHIPPING=99、MAX_QTY=99、summary 算法 |
| `src/stores/checkout.ts` | 全 140 行 | 待购清单与购物车的分离语义 |
| `src/stores/messages.ts` | 全 100 行 | Message 字段、kind 枚举、未读逻辑 |
| `src/services/assistant.ts` | 1-145 行 | AI 请求/响应契约，最关键的 45 行是 91-145 |
| `src/router/index.ts` | 全 87 行 | 路由与鉴权边界（哪些页面要登录） |
| `src/views/home/catalog.ts` | 1-68 行 + 2124-2191 行 | 类型定义 + 导出结构；**不要给 2191 行全文** |
| `src/views/home/useShopActions.ts` | 全 65 行 | "+/- 改待购、加购是开关"的交互语义 |
| `src/utils/format.ts` | 全 | money/round2 的格式化规则 |

### A.2 按轮次给

| 轮次 | 文件 | 区段 |
|---|---|---|
| 第 3 轮 | `views/login/LoginView.vue` | 478-726（script） |
| 第 4 轮 | `views/category/CategoryView.vue` | 1-60 |
| 第 4 轮 | `views/item/ProductDetailView.vue` | 1-80 |
| 第 4 轮 | `views/home/HomeView.vue` | 1-165（搜索逻辑在 51-68） |
| 第 5 轮 | `views/cart/CartView.vue` | 1-64 |
| 第 6 轮 | `views/checkout/CheckoutView.vue` | 1-71 |
| 第 8 轮 | `views/profile/ProfileView.vue` | 1-112 |
| 第 8 轮 | `views/messages/MessagesView.vue` | 1-63 |

### A.3 绝对不要给

- 任何 `.css` 文件（`home.css` 3000+ 行，纯噪音）
- `GalaxyBg.vue`、`doll-scene.js`、`login.css`（登录页动画，与后端无关）
- `assets/products/*.jpg`（二进制）
- `HomeView.vue` 的 template 部分（167 行往后）
- `ProductCard.vue` 的 template（68 行往后）

> 复制方式：VSCode/IDEA 打开文件，选中行号区间，复制。别整个文件夹拖进去。

---

## 附录 B AI 交付验收清单

每轮拿到代码后，按这张表过一遍，全过再进下一轮。

### B.1 编译与启动

- [ ] `mvn -q clean package -DskipTests` 通过，无 error
- [ ] 启动日志无 `BeanCreationException`、无 `NoSuchMethodError`
- [ ] 启动后 `/api/v1/health` 返回 `code=0`
- [ ] 没有出现"升级到 JDK 17 就好了"这类建议（说明它在迁就错误配置）

### B.2 代码规范

- [ ] 包名全是 `com.owl.*`，没有 `com.example`
- [ ] Controller 里没有 `@Autowired` 字段注入
- [ ] Controller 里没有 `Mapper` 类型
- [ ] Service 返回值是 VO，不是 Entity
- [ ] DTO 全在 `dto` 包、VO 全在 `vo` 包，命名是 `XxxRequest` / `XxxVO`
- [ ] Controller 方法签名里没有 Entity，也没有 `Map<String,Object>` 这种临时拼装
- [ ] 抽查 3 个 VO 的字段名，与 `API契约-DTO-VO.md` 完全一致（含大小写）
- [ ] 金额字段是 `long ...Cent`，没有 `double`
- [ ] 没有 `System.out.println`（用 `@Slf4j`）
- [ ] 接口路径与规格书逐字一致（包括单复数）

### B.3 契约对齐

- [ ] 用户 id 是字符串
- [ ] 商品 price 是 `99.00` 这种两位小数，不是 `9900`
- [ ] 时间字段是 13 位毫秒数，不是 ISO 字符串
- [ ] 分页响应是 `{list,total,page,size,pages}`
- [ ] 错误响应是 `{code,message,data,traceId}`，业务失败 HTTP 也是 200

### B.4 业务正确性

- [ ] 满 99 免运费、不满收 5 元，与前端 `FREE_SHIPPING` 一致
- [ ] 单行数量上限 99
- [ ] `Sub.count=0` 的细分没被过滤
- [ ] 库存扣减带 `stock >= qty`
- [ ] 订单状态流转没有跳步

---

## 附录 C 常见跑偏与纠正话术

| AI 的行为 | 为什么错 | 纠正话术 |
|---|---|---|
| 返回 `price: 9900` | 把分直接给前端了 | "price 必须是元，9900 分要返回 99.00。检查 PriceUtil.toYuan 有没有用在 VO 层。" |
| 用户 id 返回数字 | 前端 normalize 会丢会话 | "AuthUser.id 是 string，UserVO 里用 String.valueOf(id)，不要用 Long。" |
| 自作主张加 Spring Security | 与规格书 8.1 冲突 | "去掉 Spring Security 依赖，按规格书 8.1 用 HandlerInterceptor，只保留 spring-security-crypto。" |
| 把 @Transactional 加在 Controller | 事务边界错 | "事务只在 service.impl 的 public 方法上，Controller 不碰事务。" |
| 用 `if (channel == "mock")` | 违反开闭原则 | "按 6.6.3 抽 PaymentChannel 接口，Service 里不许出现渠道判断。" |
| 用 `SELECT *` 再在 Java 里过滤 | 性能与正确性都差 | "条件放 SQL 里，按 5.5 的索引建议写。" |
| 省略方法体写 `// ...` | 代码不可用 | "把省略的部分补全，每个方法都要完整实现。" |
| 改了前端文件 | 越界 | "回滚前端改动，只输出后端代码。" |
| 建议"先跑起来再补" | 交付不完整 | "按验收清单逐条给结果，没过的不算完成。" |
| 一次生成 20 个文件 | 上下文爆炸，质量下降 | "分两步：先 Controller + DTO/VO，我确认后再写 Service + Mapper。" |

---

## 附录 D 一句话启动模板（懒人版）

如果你只想发一条消息就开工，用这个：

```
读附件《OWL 商城后端开发文档》和前端代码，按第 3、4 章和任务 T1，
在我本机创建 E:\OWL-System\owl-server 的 Maven 多模块骨架（JDK 21），
先给文件树和实现计划，我确认后你再写代码。

硬约束：包名 com.owl；金额 long 分；用户 id 返回 String；不用 Spring Security；
不用 JPA；构造器注入；不改前端；不引入未列依赖。
```

---

**这份投喂包结束。** 规格书更新时（比如改表结构），同步改本文件的轮次说明，别让两份文档打架。
