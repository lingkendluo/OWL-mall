# OWL 商城 · API 契约（DTO / VO 分层）

> 生成时间：由 `docs/tools/gen_api_contract.py` 自动生成，**不要手改**
> 唯一真相：`docs/owl-mall-openapi.json`（由 `docs/tools/gen_openapi.py` 生成）
> 规格书：`docs/OWL商城-后端开发文档.md` v1.1
> 统计：63 个接口 · 30 个 DTO · 39 个 VO

---

## 一、导入 Apifox（3 步）

### 1. 导入文件

Apifox → 打开项目 → 左侧「接口」→ 右上角 `+` → **导入数据** → 选 **OpenAPI/Swagger** → 上传 `docs/owl-mall-openapi.json` → 导入。

导入后会得到：

- 9 个目录（认证 / 商品 / 购物车 / 交易 / 支付 / 个人中心 / 消息 / AI 助手 / 后台）
- 63 个接口，每个都带**请求示例**，点「发送」前只需改 `baseUrl`
- 数据模型里 70 个模型，名字前缀 `[DTO 入参]` / `[VO 出参]` 一眼区分

### 2. 建环境

项目设置 → 环境管理 → 新建「本地」：

| 变量 | 值 |
|---|---|
| `baseUrl` | `http://localhost:8080/api/v1` |

把目录的「前置 URL」设为 `{{baseUrl}}`（导入时已写在 `servers` 里，通常不用改）。

### 3. 登录后自动存 token

打开 `POST /auth/login` → **后置操作** → 添加「提取变量」：

| 项 | 值 |
|---|---|
| 提取来源 | 响应 JSON |
| JSONPath | `$.data.accessToken` |
| 变量名 | `token` |
| 作用域 | 临时变量 |

再给「认证」「购物车」「交易」「支付」「个人中心」「消息」「后台」这些目录设置 **Auth → Bearer Token → `{{token}}`**。

之后跑完登录接口，其它接口自动带 token，不用手动粘。

> 后台接口用另一套 token：在 `POST /admin/auth/login` 里提取 `$.data.accessToken` 到 `adminToken`，后台目录单独用 `{{adminToken}}`。

### 覆盖导入的注意点

改了接口后重新生成 JSON，再用「导入 → 覆盖」：

- 选**按 operationId 覆盖**，不要选「新建副本」，否则会重复
- 导入前先提交一次 Apifox 的变更，出问题能回滚
- 手改过的接口描述会被覆盖，所以**字段说明改脚本，不要改 Apifox**

---

## 二、接口清单（63 个）

| # | 方法 | 路径 | 说明 | 鉴权 | 请求 DTO | 响应 VO |
|---|---|---|---|---|---|---|
| 1 | `POST` | `/auth/sms-code` | 发送短信验证码 | 公开 | SmsCodeRequest | object |
| 2 | `POST` | `/auth/register` | 注册 | 公开 | RegisterRequest | LoginVO |
| 3 | `POST` | `/auth/login` | 登录 | 公开 | LoginRequest | LoginVO |
| 4 | `POST` | `/auth/guest` | 游客登录 | 公开 | — | LoginVO |
| 5 | `POST` | `/auth/refresh` | 刷新 token | 公开 | RefreshTokenRequest | object |
| 6 | `POST` | `/auth/logout` | 退出登录 | Bearer | — | null |
| 7 | `GET` | `/auth/me` | 当前用户 | Bearer | — | UserVO |
| 8 | `PATCH` | `/auth/me` | 修改资料 | Bearer | UpdateProfileRequest | UserVO |
| 9 | `PUT` | `/auth/password` | 修改密码 | Bearer | ChangePasswordRequest | null |
| 10 | `GET` | `/c/home` | 首页聚合 | 公开 | — | HomeVO |
| 11 | `GET` | `/c/categories` | 类目列表 | 公开 | — | array<CategoryVO> |
| 12 | `GET` | `/c/categories/{code}` | 类目详情 | 公开 | — | CategoryVO |
| 13 | `GET` | `/c/products` | 商品列表 / 搜索 | 公开 | — | PageResult<ProductVO> |
| 14 | `GET` | `/c/products/{code}` | 商品详情 | 公开 | — | object |
| 15 | `GET` | `/c/featured` | 推荐位 | 公开 | — | array<ProductVO> |
| 16 | `GET` | `/cart` | 查询购物车 | Bearer | — | CartVO |
| 17 | `DELETE` | `/cart` | 清空购物车 | Bearer | — | null |
| 18 | `POST` | `/cart/items` | 加入购物车 | Bearer | CartAddRequest | CartVO |
| 19 | `PATCH` | `/cart/items/{productId}` | 修改数量 / 勾选 | Bearer | CartUpdateRequest | CartVO |
| 20 | `DELETE` | `/cart/items/{productId}` | 删除购物车行 | Bearer | — | CartVO |
| 21 | `PUT` | `/cart/checked` | 批量勾选 | Bearer | CartCheckRequest | CartVO |
| 22 | `DELETE` | `/cart/checked` | 删除已勾选 | Bearer | — | CartVO |
| 23 | `POST` | `/cart/merge` | 合并本地购物车 | Bearer | CartMergeRequest | CartMergeVO |
| 24 | `POST` | `/trade/preview` | 结算预览 | Bearer | PreviewRequest | PreviewVO |
| 25 | `POST` | `/trade/orders` | 创建订单 | Bearer | OrderCreateRequest | OrderCreateVO |
| 26 | `GET` | `/trade/orders` | 订单列表 | Bearer | — | PageResult<OrderBriefVO> |
| 27 | `GET` | `/trade/orders/{orderNo}` | 订单详情 | Bearer | — | OrderDetailVO |
| 28 | `POST` | `/trade/orders/{orderNo}/cancel` | 取消订单 | Bearer | OrderCancelRequest | OrderDetailVO |
| 29 | `POST` | `/trade/orders/{orderNo}/confirm` | 确认收货 | Bearer | — | OrderDetailVO |
| 30 | `GET` | `/trade/orders/counts` | 订单角标 | Bearer | — | OrderCountVO |
| 31 | `POST` | `/pay/mock/{orderNo}` | 发起支付（模拟渠道） | Bearer | — | PayVO |
| 32 | `POST` | `/pay/mock/callback` | 模拟支付回调 | 公开 | MockCallbackRequest | PayCallbackVO |
| 33 | `GET` | `/pay/{payNo}` | 查询支付单 | Bearer | — | PayDetailVO |
| 34 | `POST` | `/pay/refund` | 申请退款 | Bearer | RefundRequest | RefundVO |
| 35 | `GET` | `/pay/refunds/{refundNo}` | 查询退款单 | Bearer | — | RefundVO |
| 36 | `GET` | `/m/profile` | 个人中心汇总 | Bearer | — | ProfileVO |
| 37 | `GET` | `/m/addresses` | 地址列表 | Bearer | — | array<AddressVO> |
| 38 | `POST` | `/m/addresses` | 新增地址 | Bearer | AddressRequest | AddressVO |
| 39 | `PUT` | `/m/addresses/{id}` | 修改地址 | Bearer | AddressRequest | AddressVO |
| 40 | `DELETE` | `/m/addresses/{id}` | 删除地址 | Bearer | — | null |
| 41 | `PUT` | `/m/addresses/{id}/default` | 设为默认地址 | Bearer | — | AddressVO |
| 42 | `GET` | `/m/coupons` | 我的优惠券 | Bearer | — | PageResult<CouponVO> |
| 43 | `GET` | `/m/points` | 积分流水 | Bearer | — | PageResult<PointsLogVO> |
| 44 | `GET` | `/m/messages` | 消息列表 | Bearer | — | PageResult<MessageVO> |
| 45 | `GET` | `/m/messages/unread` | 未读数 | Bearer | — | UnreadVO |
| 46 | `POST` | `/m/messages/{id}/read` | 标记已读 | Bearer | — | null |
| 47 | `POST` | `/m/messages/read-all` | 全部已读 | Bearer | — | null |
| 48 | `DELETE` | `/m/messages/{id}` | 删除消息 | Bearer | — | null |
| 49 | `POST` | `/assistant/chat` | 购物助手对话 | 公开 | ChatRequest | ChatResponseVO |
| 50 | `POST` | `/admin/auth/login` | 管理员登录 | 公开 | AdminLoginRequest | AdminLoginVO |
| 51 | `GET` | `/admin/products` | 商品列表（管理端） | Bearer | — | PageResult<AdminProductVO> |
| 52 | `POST` | `/admin/products` | 新增商品 | Bearer | AdminProductRequest | AdminProductVO |
| 53 | `PUT` | `/admin/products/{code}` | 修改商品 | Bearer | AdminProductRequest | AdminProductVO |
| 54 | `DELETE` | `/admin/products/{code}` | 删除商品 | Bearer | — | null |
| 55 | `PATCH` | `/admin/products/{code}/status` | 上下架 | Bearer | ProductStatusRequest | AdminProductVO |
| 56 | `PATCH` | `/admin/products/{code}/stock` | 库存调整 | Bearer | StockAdjustRequest | AdminProductVO |
| 57 | `GET` | `/admin/orders` | 订单列表（管理端） | Bearer | — | PageResult<OrderBriefVO> |
| 58 | `GET` | `/admin/orders/{orderNo}` | 订单详情（管理端） | Bearer | — | OrderDetailVO |
| 59 | `POST` | `/admin/orders/{orderNo}/deliver` | 订单发货 | Bearer | DeliverRequest | OrderDetailVO |
| 60 | `GET` | `/admin/stats/overview` | 概览统计 | Bearer | — | StatsVO |
| 61 | `GET` | `/admin/categories` | 类目列表（管理端） | Bearer | — | array<CategoryVO> |
| 62 | `POST` | `/admin/categories` | 新增类目 | Bearer | CategoryUpsertRequest | CategoryVO |
| 63 | `PUT` | `/admin/categories/{code}` | 修改类目 | Bearer | CategoryUpsertRequest | CategoryVO |

> `—` 表示无请求体（GET/DELETE）；`null` 表示 data 为空。

---

## 三、DTO 层（入参，30 个）

包路径规则：`com.owl.{模块}.dto.{类名}`

| 类名 | 字段 | 用途 |
|---|---|---|
| `AddressRequest` | `receiver` (string)、`phone` (string)、`province` (string)、`city` (string)、`district` (string)、`detail` (string)、`tag` (string)、`isDefault` (boolean) | 新增/修改收货地址 |
| `AdminLoginRequest` | `username` (string)、`password` (string) | 管理员登录 |
| `AdminProductRequest` | `name` (string)、`categoryCode` (string)、`subCode` (string)、`spec` (string)、`price` (number)、`list` (number)、`tag` (string)、`emoji` (string)、`image` (string)、`stock` (integer)、`isFeatured` (boolean)、`detail` (ProductDetailDTO) | 后台新增/修改商品 |
| `AssistContextDTO` | `cartCount` (integer)、`cartAmount` (number)、`cartCheckedAmount` (number)、`cartSaved` (number)、`toFreeShipping` (number)、`isFreeShipping` (boolean)、`freeShippingAt` (number)、`lines` (array<AssistLineDTO>) | 购物车上下文（前端实时算好后传过来） |
| `AssistLineDTO` | `name` (string)、`qty` (integer)、`price` (number) | 助手上下文里的购物车行 |
| `CartAddRequest` | `productId` (string)、`qty` (integer) | 加入购物车 |
| `CartCheckRequest` | `checked` (boolean)、`ids` (array<string>) | 批量勾选 |
| `CartMergeLineDTO` | `productId` (string)、`qty` (integer) | 合并行 |
| `CartMergeRequest` | `lines` (array<CartMergeLineDTO>) | 登录后合并本地购物车 |
| `CartUpdateRequest` | `qty` (integer)、`checked` (boolean) | 修改购物车行（字段可选） |
| `CategoryUpsertRequest` | `code` (string)、`name` (string)、`hint` (string)、`glyph` (string)、`sort` (integer)、`subs` (array<SubDTO>) | 后台新增/修改类目 |
| `ChangePasswordRequest` | `oldPassword` (string)、`newPassword` (string) | 修改密码 |
| `ChatMessageDTO` | `role` (string)、`content` (string) | 对话消息 |
| `ChatRequest` | `model` (string)、`messages` (array<ChatMessageDTO>)、`context` (AssistContextDTO) | AI 助手对话（前端原样发送，不要改字段名） |
| `DeliverRequest` | `company` (string)、`trackingNo` (string) | 订单发货 |
| `LoginRequest` | `account` (string)、`password` (string)、`remember` (boolean) | 登录 |
| `MockCallbackRequest` | `payNo` (string)、`mockToken` (string)、`success` (boolean) | 模拟支付回调 |
| `OrderCancelRequest` | `reason` (string) | 取消订单 |
| `OrderCreateRequest` | `lines` (array<OrderLineDTO>)、`addressId` (string)、`couponId` (string)、`remark` (string)、`deliveryType` (integer)、`fromCart` (boolean)、`clientToken` (string) | 创建订单 |
| `OrderLineDTO` | `productId` (string)、`qty` (integer) | 下单行（只有商品与数量，不含价格） |
| `PreviewRequest` | `lines` (array<OrderLineDTO>)、`addressId` (string)、`couponId` (string) | 结算预览 |
| `ProductDetailDTO` | `slogan` (string)、`intro` (string)、`facts` (map<string,string>)、`tips` (array<string>) | 商品详情四段 |
| `ProductStatusRequest` | `status` (integer) | 上下架 |
| `RefreshTokenRequest` | `refreshToken` (string) | 刷新 token |
| `RefundRequest` | `orderNo` (string)、`reason` (string) | 申请退款 |
| `RegisterRequest` | `name` (string)、`phone` (string)、`code` (string)、`password` (string)、`agree` (boolean) | 注册 |
| `SmsCodeRequest` | `phone` (string)、`scene` (string) | 发送验证码 |
| `StockAdjustRequest` | `delta` (integer)、`reason` (string) | 库存调整 |
| `SubDTO` | `code` (string)、`name` (string) | 细分（后台类目维护用） |
| `UpdateProfileRequest` | `nickname` (string)、`email` (string)、`avatar` (string) | 修改资料（字段可选，只改传入的） |

### DTO 写法约定

```java
package com.owl.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "请输入手机号或邮箱")
    private String account;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    /** 只影响前端存 localStorage 还是 sessionStorage，后端不处理 */
    private Boolean remember = Boolean.TRUE;
}
```

硬性要求：

1. **只做入参**，不承载业务逻辑，不实现 `Serializable`。
2. 校验注解写在这里，Service 里不再判空（`@Valid` 在 Controller 上触发）。
3. 字段类型用包装类型（`Integer` / `Boolean`），不要用 `int`，否则无法区分"没传"和"传了 0"。
4. 金额字段是 `BigDecimal`（元），接收后立刻用 `PriceUtil.toCent()` 转 `long` 分。
5. 命名：`XxxRequest`（写操作）、`XxxQuery`（查询参数对象）、`XxxDTO`（嵌套结构）。
6. **禁止**把 DTO 传给 Mapper，也禁止把 Entity 当 DTO 用。

---

## 四、VO 层（出参，39 个）

包路径规则：`com.owl.{模块}.vo.{类名}`

| 类名 | 字段 | 说明 |
|---|---|---|
| `AddressVO` | `id` (string)、`receiver` (string)、`phone` (string)、`province` (string)、`city` (string)、`district` (string)、`detail` (string)、`tag` (string)、`isDefault` (boolean) | 收货地址 |
| `AdminLoginVO` | `accessToken` (string)、`expiresIn` (integer)、`admin` (AdminVO) | 管理员登录结果 |
| `AdminProductVO` | `id` (string)、`name` (string)、`categoryCode` (string)、`subCode` (string)、`spec` (string)、`price` (number)、`list` (number)、`image` (string)、`stock` (integer)、`sales` (integer)、`status` (integer)、`isFeatured` (boolean)、…共 13 个 | 后台商品（比 ProductVO 多管理字段） |
| `AdminVO` | `id` (string)、`username` (string)、`nickname` (string)、`role` (string) | 管理员 |
| `AmountVO` | `goodsAmount` (number)、`discount` (number)、`shipFee` (number)、`payable` (number)、`saved` (number) | 金额明细 |
| `AssetsVO` | `coupons` (integer)、`points` (integer)、`balance` (number) | 账户资产 |
| `CartLineVO` | `id` (string)、`name` (string)、`spec` (string)、`price` (number)、`list` (number)、`emoji` (string)、`image` (string)、`qty` (integer)、`checked` (boolean)、`invalid` (boolean) | 购物车行 |
| `CartMergeVO` | `lines` (array<CartLineVO>)、`summary` (CartSummaryVO)、`skipped` (array<string>) | 合并结果 |
| `CartSummaryVO` | `count` (integer)、`amount` (number)、`saved` (number)、`checkedCount` (integer)、`checkedAmount` (number)、`allChecked` (boolean)、`isFreeShipping` (boolean)、`toFreeShipping` (number)、`shippingRate` (integer)、`freeShippingAt` (number)、`shipFee` (number) | 购物车汇总（服务端算，前端只展示） |
| `CartVO` | `lines` (array<CartLineVO>)、`summary` (CartSummaryVO) | 购物车 |
| `CategoryVO` | `id` (string)、`name` (string)、`hint` (string)、`glyph` (string)、`productCount` (integer)、`subs` (array<SubVO>)、`items` (array<ProductVO>)、`currentSub` (string)、`fallback` (boolean) | 类目 |
| `ChatResponseVO` | `reply` (string)、`model` (string)、`usage` (UsageVO)、`degraded` (boolean) | AI 回复（必须含 reply，前端认这个字段） |
| `CouponBriefVO` | `id` (string)、`name` (string)、`discount` (number)、`usable` (boolean)、`reason` (string) | 结算页可用券 |
| `CouponVO` | `id` (string)、`name` (string)、`type` (integer)、`threshold` (number)、`discount` (number)、`startAt` (integer)、`endAt` (integer)、`status` (integer)、`usable` (boolean) | 我的优惠券 |
| `HomeVO` | `brand` (object)、`categories` (array<CategoryVO>)、`featured` (array<ProductVO>)、`freeShippingAt` (number)、`shippingText` (string) | 首页聚合 |
| `LoginVO` | `accessToken` (string)、`refreshToken` (string)、`expiresIn` (integer)、`user` (UserVO) | 登录/注册/刷新结果 |
| `MessageVO` | `id` (string)、`kind` (string)、`title` (string)、`body` (string)、`time` (string)、`read` (boolean)、`createdAt` (integer)、`bizNo` (string) | 站内信 |
| `OrderBriefVO` | `orderNo` (string)、`status` (integer)、`statusText` (string)、`payAmount` (number)、`totalQty` (integer)、`firstItemName` (string)、`coverImage` (string)、`createdAt` (integer)、`expireAt` (integer) | 订单列表项 |
| `OrderCountVO` | `unpaid` (integer)、`shipping` (integer)、`review` (integer)、`after` (integer) | 订单角标（个人中心用） |
| `OrderCreateVO` | `orderNo` (string)、`payNo` (string)、`payAmount` (number)、`status` (integer)、`expireAt` (integer) | 下单结果 |
| `OrderDetailVO` | `orderNo` (string)、`status` (integer)、`statusText` (string)、`payStatus` (integer)、`amounts` (AmountVO)、`items` (array<OrderItemVO>)、`address` (AddressVO)、`deliveryType` (integer)、`deliveryText` (string)、`remark` (string)、`payNo` (string)、`createdAt` (integer)、…共 18 个 | 订单详情 |
| `OrderItemVO` | `id` (string)、`name` (string)、`spec` (string)、`image` (string)、`price` (number)、`list` (number)、`qty` (integer)、`total` (number) | 订单行（快照，不随商品改价变化） |
| `PayCallbackVO` | `orderNo` (string)、`payStatus` (integer)、`orderStatus` (integer) | 支付回调结果 |
| `PayDetailVO` | `payNo` (string)、`orderNo` (string)、`channel` (string)、`amount` (number)、`status` (integer)、`paidAt` (integer) | 支付单 |
| `PayVO` | `payNo` (string)、`channel` (string)、`amount` (number)、`mockToken` (string)、`expireAt` (integer) | 发起支付结果 |
| `PointsLogVO` | `id` (string)、`change` (integer)、`type` (integer)、`remark` (string)、`createdAt` (integer) | 积分流水 |
| `PreviewItemVO` | `id` (string)、`name` (string)、`spec` (string)、`image` (string)、`price` (number)、`list` (number)、`qty` (integer)、`total` (number) | 结算商品行 |
| `PreviewVO` | `items` (array<PreviewItemVO>)、`amounts` (AmountVO)、`shipping` (ShippingVO)、`address` (AddressVO)、`coupons` (array<CouponBriefVO>)、`warnings` (array<string>) | 结算预览 |
| `ProductDetailVO` | `slogan` (string)、`intro` (string)、`facts` (map<string,string>)、`tips` (array<string>) | 商品详情四段 |
| `ProductVO` | `id` (string)、`name` (string)、`spec` (string)、`price` (number)、`list` (number)、`tag` (string)、`emoji` (string)、`image` (string)、`sub` (string)、`detail` (ProductDetailVO) | 商品 |
| `ProfileVO` | `user` (UserVO)、`assets` (AssetsVO)、`orderCounts` (OrderCountVO) | 个人中心汇总（一次给全） |
| `RefundVO` | `refundNo` (string)、`amount` (number)、`status` (integer) | 退款单 |
| `ShippingVO` | `freeShippingAt` (number)、`toFreeShipping` (number)、`isFreeShipping` (boolean) | 运费信息 |
| `StatsVO` | `gmv` (number)、`orderCount` (integer)、`userCount` (integer)、`productCount` (integer)、`lowStockCount` (integer)、`todayOrderCount` (integer)、`todayGmv` (number) | 后台概览 |
| `SubVO` | `id` (string)、`name` (string)、`count` (integer) | 细分 |
| `TimelineVO` | `status` (integer)、`statusText` (string)、`at` (integer)、`remark` (string) | 订单时间线 |
| `UnreadVO` | `total` (integer)、`order` (integer)、`promo` (integer)、`system` (integer) | 未读数 |
| `UsageVO` | `promptTokens` (integer)、`completionTokens` (integer)、`totalTokens` (integer) | 模型用量 |
| `UserVO` | `id` (string)、`name` (string)、`phone` (string)、`email` (string)、`level` (string)、`memberNo` (string)、`points` (integer)、`coupons` (integer)、`balance` (number)、`joinedAt` (integer)、`isGuest` (boolean) | 用户（与前端 AuthUser 逐字对应） |

### VO 写法约定

```java
package com.owl.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录结果")
public class LoginVO {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "access 有效期，秒", example = "7200")
    private Integer expiresIn;

    private UserVO user;
}
```

硬性要求：

1. **字段名与前端类型逐字一致**，不要"规范化"成 `userName`、`priceCent`。
2. 金额是 `BigDecimal`（元，两位小数），由 `PriceUtil.toYuan(cent)` 产出。
3. 用户/商品/类目 `id` 是 `String`，`String.valueOf(id)` 或直接用 `code`。
4. 时间字段是 `Long`（毫秒时间戳），字段名以 `At` 结尾。
5. 空集合返回 `List.of()`，不要返回 `null`；`@JsonInclude(NON_NULL)` 只加在可选字段上。
6. `VO` 里不出现 `Entity` 的数据库字段（`deleted`、`version`、`created_at` 原样名）。

---

## 五、分层与转换

```
Controller  ──接收──>  DTO（@Valid 校验）
    │
    ├─> Service  ──查询──>  Entity
    │                │
    │                └──转换──>  VO
    │
    └─<──返回──  Result<VO>
```

规则：

1. **Controller 不碰 Entity**，方法签名里只出现 DTO / VO / `Result`。
2. **Service 不返回 Entity**，`service` 方法返回 VO 或 `List<VO>`。
3. 转换器放 `converter/XxxConverter`，用静态方法，不引 MapStruct（这个项目体量不值得多一个注解处理器）。

```java
package com.owl.auth.converter;

public final class UserConverter {

    private UserConverter() {}

    public static UserVO toVO(MUser user, int couponCount) {
        UserVO vo = new UserVO();
        vo.setId(String.valueOf(user.getId()));          // 必须 String
        vo.setName(user.getNickname());
        vo.setPhone(user.getPhone() == null ? "" : user.getPhone());
        vo.setEmail(user.getEmail() == null ? "" : user.getEmail());
        vo.setLevel(user.getLevel());
        vo.setMemberNo(user.getMemberNo() == null ? "" : user.getMemberNo());
        vo.setPoints(user.getPoints());
        vo.setCoupons(couponCount);
        vo.setBalance(PriceUtil.toYuan(user.getBalanceCent()));
        vo.setJoinedAt(user.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        vo.setIsGuest(user.getIsGuest() == 1);
        return vo;
    }
}
```

---

## 六、DTO / VO 与数据库字段的映射要点

| 场景 | 坑 | 正确做法 |
|---|---|---|
| 金额 | `price_cent=9900` 直接塞进 VO | `PriceUtil.toYuan(9900)` → `99.00` |
| 用户 id | `Long` 直接返回，前端 `normalize()` 丢弃会话 | `String.valueOf(id)` |
| 商品 id | 返回数据库自增 id | 返回 `code`（`snacks-1`） |
| 时间 | `LocalDateTime` 序列化成 `"2026-10-28T23:10:00"` | `.toInstant().toEpochMilli()` |
| 空手机号 | 游客 `phone=null`，前端 `undefined` | VO 里转成 `""` |
| 会员号 | 游客 `member_no=null` | VO 里转成 `""` |
| 细分 count | 用表字段 | SQL 聚合算，`count=0` 保留 |
| 商品 facts | `Map` 无序 | `LinkedHashMap` 或直接返回 JSON 原文 |

---

## 七、改接口的正确姿势

1. 改 `docs/tools/gen_openapi.py`（加字段、加接口）。
2. 跑 `python docs/tools/gen_openapi.py` → 更新 JSON。
3. 跑 `python docs/tools/gen_api_contract.py` → 更新本文档。
4. Apifox 里「覆盖导入」，选按 operationId 覆盖。
5. 同步改规格书第 6 章与投喂包对应轮次。

**不要**在 Apifox 里手改字段后再导出，那样两边会分叉。
