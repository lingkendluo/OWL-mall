#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OWL 商城 · OpenAPI 3.0 生成器（Apifox 可直接导入）

用法：
    python docs/tools/gen_openapi.py
产出：
    docs/owl-mall-openapi.json

约定：
    DTO  = 入参，类名 XxxRequest / XxxDTO / XxxQuery
    VO   = 出参，类名 XxxVO
    金额 = 元（number，两位小数）；时间 = 毫秒时间戳（int64）；ID = string

改接口只改这个脚本，重新生成后在 Apifox 里"覆盖导入"，不要手改 JSON。
"""
import json
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "owl-mall-openapi.json")


# ------------------------------ 构造小工具 ------------------------------
def ref(name):
    return {"$ref": f"#/components/schemas/{name}"}


def f(typ, desc="", example=None, **kw):
    d = {"type": typ}
    if desc:
        d["description"] = desc
    if example is not None:
        d["example"] = example
    d.update(kw)
    return d


def obj(desc, props, required=None):
    s = {"type": "object", "description": desc, "properties": props}
    if required:
        s["required"] = required
    return s


def dto(desc, props, required=None):
    return obj(f"[DTO 入参] {desc}", props, required)


def vo(desc, props, required=None):
    return obj(f"[VO 出参] {desc}", props, required)


def arr(item, desc=""):
    d = {"type": "array", "items": item}
    if desc:
        d["description"] = desc
    return d


def ok(data_schema=None, example=None, desc="成功"):
    parts = [ref("Result")]
    if data_schema is not None:
        parts.append({"type": "object", "properties": {"data": data_schema}})
    media = {"schema": {"allOf": parts}}
    if example is not None:
        media["example"] = example
    return {"200": {"description": desc, "content": {"application/json": media}}}


def page_of(item_schema, total=90):
    return {
        "type": "object",
        "properties": {
            "list": arr(item_schema),
            "total": f("integer", "", total),
            "page": f("integer", "", 1),
            "size": f("integer", "", 20),
            "pages": f("integer", "", 5),
        },
    }


def body(schema, example=None, required=True):
    media = {"schema": schema}
    if example is not None:
        media["example"] = example
    return {"required": required, "content": {"application/json": media}}


def pp(name, desc, example="snacks-1"):
    return {"name": name, "in": "path", "required": True,
            "description": desc, "schema": {"type": "string"}, "example": example}


def qp(name, desc, typ="string", example=None, required=False, enum=None):
    s = {"type": typ}
    if enum:
        s["enum"] = enum
    p = {"name": name, "in": "query", "required": required,
         "description": desc, "schema": s}
    if example is not None:
        p["example"] = example
    return p


schemas = {}
paths = {}


def ep(operation_id, url, method, tags, summary, desc="",
       params=None, req=None, resp=None, public=False):
    op = {
        "tags": tags,
        "summary": summary,
        "description": desc,
        "operationId": operation_id,
        "parameters": params or [],
        "responses": resp or ok(),
    }
    if req is not None:
        op["requestBody"] = req
    if public:
        op["security"] = []
    paths.setdefault(url, {})[method] = op


# ============================== DTO 层 ==============================
schemas["SmsCodeRequest"] = dto("发送验证码", {
    "phone": f("string", "11 位手机号", "13800138000"),
    "scene": f("string", "场景，目前只有 register", "register", enum=["register"]),
}, ["phone", "scene"])

schemas["RegisterRequest"] = dto("注册", {
    "name": f("string", "昵称", "罗耀"),
    "phone": f("string", "手机号", "13800138000"),
    "code": f("string", "6 位短信验证码", "482913"),
    "password": f("string", "8-20 位，必须含字母和数字", "abc12345"),
    "agree": f("boolean", "是否同意用户协议，必须 true", True),
}, ["name", "phone", "code", "password", "agree"])

schemas["LoginRequest"] = dto("登录", {
    "account": f("string", "手机号或邮箱", "13800138000"),
    "password": f("string", "密码", "abc12345"),
    "remember": f("boolean", "记住我，只影响前端存储位置", True),
}, ["account", "password"])

schemas["RefreshTokenRequest"] = dto("刷新 token", {
    "refreshToken": f("string", "登录时下发的 refreshToken"),
}, ["refreshToken"])

schemas["UpdateProfileRequest"] = dto("修改资料（字段可选，只改传入的）", {
    "nickname": f("string", "昵称", "阿枢"),
    "email": f("string", "邮箱", "a@b.com"),
    "avatar": f("string", "头像 URL"),
})

schemas["ChangePasswordRequest"] = dto("修改密码", {
    "oldPassword": f("string", "原密码", "abc12345"),
    "newPassword": f("string", "新密码，8-20 位含字母数字", "xyz98765"),
}, ["oldPassword", "newPassword"])

schemas["CartAddRequest"] = dto("加入购物车", {
    "productId": f("string", "商品 code", "snacks-1"),
    "qty": f("integer", "数量，1-99，越界截断", 1, minimum=1, maximum=99),
}, ["productId"])

schemas["CartUpdateRequest"] = dto("修改购物车行（字段可选）", {
    "qty": f("integer", "数量 1-99", 2, minimum=1, maximum=99),
    "checked": f("boolean", "是否勾选", True),
})

schemas["CartCheckRequest"] = dto("批量勾选", {
    "checked": f("boolean", "true 全选 / false 全不选", True),
    "ids": arr(f("string"), "要操作的商品 code，不传表示全部行"),
}, ["checked"])

schemas["CartMergeLineDTO"] = dto("合并行", {
    "productId": f("string", "商品 code", "snacks-1"),
    "qty": f("integer", "数量", 2, minimum=1, maximum=99),
}, ["productId", "qty"])

schemas["CartMergeRequest"] = dto("登录后合并本地购物车", {
    "lines": arr(ref("CartMergeLineDTO")),
}, ["lines"])

schemas["OrderLineDTO"] = dto("下单行（只有商品与数量，不含价格）", {
    "productId": f("string", "商品 code", "snacks-1"),
    "qty": f("integer", "数量 1-99", 2, minimum=1, maximum=99),
}, ["productId", "qty"])

schemas["PreviewRequest"] = dto("结算预览", {
    "lines": arr(ref("OrderLineDTO")),
    "addressId": f("string", "收货地址 id，不传用默认地址", "1"),
    "couponId": f("string", "用户券 id，可空", "12"),
}, ["lines"])

schemas["OrderCreateRequest"] = dto("创建订单", {
    "lines": arr(ref("OrderLineDTO")),
    "addressId": f("string", "收货地址 id", "1"),
    "couponId": f("string", "用户券 id，可空", "12"),
    "remark": f("string", "订单备注", "放门口"),
    "deliveryType": f("integer", "1 极速达 / 2 次日达 / 3 自提", 1, enum=[1, 2, 3]),
    "fromCart": f("boolean", "是否来自购物车结算；true 会删购物车对应行", True),
    "clientToken": f("string", "幂等键，前端生成 UUID", "c1f4a9e2-8b3d-4a71-9f22-6d0e5b7a1c33"),
}, ["lines", "addressId", "clientToken"])

schemas["OrderCancelRequest"] = dto("取消订单", {
    "reason": f("string", "取消原因", "不想要了"),
}, ["reason"])

schemas["MockCallbackRequest"] = dto("模拟支付回调", {
    "payNo": f("string", "支付单号", "P20261028231001001"),
    "mockToken": f("string", "发起支付时返回的 mockToken", "mock_9f2c1a"),
    "success": f("boolean", "true 支付成功 / false 支付失败", True),
}, ["payNo", "mockToken", "success"])

schemas["RefundRequest"] = dto("申请退款", {
    "orderNo": f("string", "订单号", "OWL20261028000123"),
    "reason": f("string", "退款原因", "买错了"),
}, ["orderNo", "reason"])

schemas["AddressRequest"] = dto("新增/修改收货地址", {
    "receiver": f("string", "收货人", "罗耀"),
    "phone": f("string", "手机号", "13800138000"),
    "province": f("string", "省", "浙江省"),
    "city": f("string", "市", "杭州市"),
    "district": f("string", "区", "西湖区"),
    "detail": f("string", "详细地址", "文三路 88 号 OWL 公寓 3 幢 1201 室"),
    "tag": f("string", "标签：家/公司", "家"),
    "isDefault": f("boolean", "是否默认地址", True),
}, ["receiver", "phone", "province", "city", "district", "detail"])

schemas["ChatMessageDTO"] = dto("对话消息", {
    "role": f("string", "角色", "user", enum=["system", "user", "assistant"]),
    "content": f("string", "内容", "帮我凑到免运费"),
}, ["role", "content"])

schemas["AssistLineDTO"] = dto("助手上下文里的购物车行", {
    "name": f("string", "商品名", "每日混合坚果 750g"),
    "qty": f("integer", "数量", 2),
    "price": f("number", "单价，元", 99.00),
}, ["name", "qty", "price"])

schemas["AssistContextDTO"] = dto("购物车上下文（前端实时算好后传过来）", {
    "cartCount": f("integer", "购物车件数", 2),
    "cartAmount": f("number", "购物车总额，元", 198.00),
    "cartCheckedAmount": f("number", "已勾选金额，元", 198.00),
    "cartSaved": f("number", "相对划线价省下的钱，元", 60.00),
    "toFreeShipping": f("number", "还差多少免运费，元", 0),
    "isFreeShipping": f("boolean", "是否已免运费", True),
    "freeShippingAt": f("number", "免运费门槛，元", 99),
    "lines": arr(ref("AssistLineDTO")),
})

schemas["ChatRequest"] = dto("AI 助手对话（前端原样发送，不要改字段名）", {
    "model": f("string", "模型名", "deepseek-chat"),
    "messages": arr(ref("ChatMessageDTO"), "历史消息，最后一条是本次提问"),
    "context": ref("AssistContextDTO"),
}, ["messages", "context"])

schemas["AdminLoginRequest"] = dto("管理员登录", {
    "username": f("string", "用户名", "admin"),
    "password": f("string", "密码", "admin123456"),
}, ["username", "password"])

schemas["ProductDetailDTO"] = dto("商品详情四段", {
    "slogan": f("string", "一句话卖点", "一天一小包，把坚果吃成习惯"),
    "intro": f("string", "详细介绍"),
    "facts": f("object", "关键参数，有序键值对",
              {"规格": "750g / 30 小包"}, additionalProperties={"type": "string"}),
    "tips": arr(f("string"), "选购/使用贴士"),
}, ["slogan", "intro"])

schemas["AdminProductRequest"] = dto("后台新增/修改商品", {
    "name": f("string", "商品名", "每日混合坚果 750g"),
    "categoryCode": f("string", "一级类目 code", "snacks"),
    "subCode": f("string", "细分 code", "nut"),
    "spec": f("string", "规格文案", "7 种果仁 · 30 小包"),
    "price": f("number", "售价，元，必须 <= list", 99.00),
    "list": f("number", "划线价，元", 129.00),
    "tag": f("string", "标签", "会员价"),
    "emoji": f("string", "无图时回落", "🥜"),
    "image": f("string", "主图 URL", "/static/products/snacks-1.jpg"),
    "stock": f("integer", "库存", 200, minimum=0),
    "isFeatured": f("boolean", "是否首页推荐", True),
    "detail": ref("ProductDetailDTO"),
}, ["name", "categoryCode", "subCode", "spec", "price", "list", "stock", "detail"])

schemas["ProductStatusRequest"] = dto("上下架", {
    "status": f("integer", "1 上架 / 0 下架", 1, enum=[0, 1]),
}, ["status"])

schemas["StockAdjustRequest"] = dto("库存调整", {
    "delta": f("integer", "正数入库，负数出库，不能减到负数", 50),
    "reason": f("string", "调整原因", "补货"),
}, ["delta", "reason"])

schemas["DeliverRequest"] = dto("订单发货", {
    "company": f("string", "物流公司", "顺丰"),
    "trackingNo": f("string", "运单号", "SF1234567890"),
}, ["company", "trackingNo"])

schemas["SubDTO"] = dto("细分（后台类目维护用）", {
    "code": f("string", "细分 code", "nut"),
    "name": f("string", "细分名", "坚果炒货"),
}, ["code", "name"])

schemas["CategoryUpsertRequest"] = dto("后台新增/修改类目", {
    "code": f("string", "类目 code", "snacks"),
    "name": f("string", "类目名", "零食"),
    "hint": f("string", "副文案", "坚果 · 炒货 · 果干"),
    "glyph": f("string", "SVG path 字符串"),
    "sort": f("integer", "排序", 10),
    "subs": arr(ref("SubDTO"), "全量覆盖，已有商品的细分不能删"),
}, ["code", "name", "hint", "subs"])

# ============================== VO 层 ==============================
schemas["Result"] = obj("统一响应体。code=0 成功，其它见文档附录 A", {
    "code": f("integer", "0 成功", 0),
    "message": f("string", "", "ok"),
    "data": {"description": "业务数据，失败时为 null", "nullable": True},
    "traceId": f("string", "", "b7f3c2d1e4a5"),
})

schemas["UserVO"] = vo("用户（与前端 AuthUser 逐字对应）", {
    "id": f("string", "必须是字符串，返回数字会导致前端会话丢失", "1001"),
    "name": f("string", "昵称", "罗耀"),
    "phone": f("string", "", "13800138000"),
    "email": f("string", ""),
    "level": f("string", "会员等级", "black", enum=["black", "normal", "guest"]),
    "memberNo": f("string", "会员编号，游客为空串", "OWL-2026-4821"),
    "points": f("integer", "", 2860),
    "coupons": f("integer", "未使用券数量", 3),
    "balance": f("number", "余额，元", 120.50),
    "joinedAt": f("integer", "注册时间，毫秒时间戳", 1730100000000, format="int64"),
    "isGuest": f("boolean", "", False),
})

schemas["LoginVO"] = vo("登录/注册/刷新结果", {
    "accessToken": f("string"),
    "refreshToken": f("string"),
    "expiresIn": f("integer", "access 有效期，秒", 7200),
    "user": ref("UserVO"),
})

schemas["ProductDetailVO"] = vo("商品详情四段", {
    "slogan": f("string"),
    "intro": f("string"),
    "facts": f("object", "有序键值对，前端按插入顺序渲染",
              {"规格": "750g / 30 小包"}, additionalProperties={"type": "string"}),
    "tips": arr(f("string")),
})

schemas["ProductVO"] = vo("商品", {
    "id": f("string", "商品 code", "snacks-1"),
    "name": f("string", "", "每日混合坚果 750g"),
    "spec": f("string", "", "7 种果仁 · 30 小包"),
    "price": f("number", "售价，元", 99.00),
    "list": f("number", "划线价，元", 129.00),
    "tag": f("string", "", "会员价"),
    "emoji": f("string", "", "🥜"),
    "image": f("string", "", "/static/products/snacks-1.jpg"),
    "sub": f("string", "细分 code", "nut"),
    "detail": ref("ProductDetailVO"),
})

schemas["SubVO"] = vo("细分", {
    "id": f("string", "", "nut"),
    "name": f("string", "", "坚果炒货"),
    "count": f("integer", "该细分商品数，0 表示补货中，不能过滤", 2),
})

schemas["CategoryVO"] = vo("类目", {
    "id": f("string", "", "snacks"),
    "name": f("string", "", "零食"),
    "hint": f("string", "", "坚果 · 炒货 · 果干"),
    "glyph": f("string", "SVG path，前端 v-html 渲染"),
    "productCount": f("integer", "", 6),
    "subs": arr(ref("SubVO")),
    "items": arr(ref("ProductVO"), "首页聚合接口不返回，类目详情才返回"),
    "currentSub": f("string", "当前筛选的细分 code", "nut", nullable=True),
    "fallback": f("boolean", "细分无货退回整类时为 true", False),
})

schemas["HomeVO"] = vo("首页聚合", {
    "brand": obj("品牌条", {
        "name": f("string", "", "OWL 商城"),
        "desc": f("string", "", "15 个大类 · 120 个细分 · 最快 1 小时送达"),
    }),
    "categories": arr(ref("CategoryVO"), "15 个类目，含 8 个细分及 count，不含 items"),
    "featured": arr(ref("ProductVO"), "12 件推荐"),
    "freeShippingAt": f("number", "免运费门槛，元", 99),
    "shippingText": f("string", "", "满 ¥99 免运费 · 最快 1 小时送达"),
})

schemas["CartLineVO"] = vo("购物车行", {
    "id": f("string", "商品 code", "snacks-1"),
    "name": f("string", "", "每日混合坚果 750g"),
    "spec": f("string", "", "7 种果仁 · 30 小包"),
    "price": f("number", "当前售价，元", 99.00),
    "list": f("number", "划线价，元", 129.00),
    "emoji": f("string", "", "🥜"),
    "image": f("string", "", "/static/products/snacks-1.jpg"),
    "qty": f("integer", "", 3),
    "checked": f("boolean", "", True),
    "invalid": f("boolean", "商品下架/删除时为 true，前端置灰", False),
})

schemas["CartSummaryVO"] = vo("购物车汇总（服务端算，前端只展示）", {
    "count": f("integer", "总件数", 3),
    "amount": f("number", "总金额，元", 297.00),
    "saved": f("number", "相对划线价省下的钱，元", 90.00),
    "checkedCount": f("integer", "已勾选件数", 3),
    "checkedAmount": f("number", "已勾选金额，元", 297.00),
    "allChecked": f("boolean", "", True),
    "isFreeShipping": f("boolean", "", True),
    "toFreeShipping": f("number", "还差多少免运费，元", 0),
    "shippingRate": f("integer", "免运费进度 0-100", 100),
    "freeShippingAt": f("number", "", 99),
    "shipFee": f("number", "当前运费，元", 0),
})

schemas["CartVO"] = vo("购物车", {
    "lines": arr(ref("CartLineVO")),
    "summary": ref("CartSummaryVO"),
})

schemas["CartMergeVO"] = vo("合并结果", {
    "lines": arr(ref("CartLineVO")),
    "summary": ref("CartSummaryVO"),
    "skipped": arr(f("string"), "因下架被跳过的商品 code"),
})

schemas["AmountVO"] = vo("金额明细", {
    "goodsAmount": f("number", "商品合计，元", 198.00),
    "discount": f("number", "优惠金额，元", 10.00),
    "shipFee": f("number", "运费，元", 0),
    "payable": f("number", "应付 = 商品 - 优惠 + 运费", 188.00),
    "saved": f("number", "相对划线价省下的钱", 60.00),
})

schemas["ShippingVO"] = vo("运费信息", {
    "freeShippingAt": f("number", "", 99),
    "toFreeShipping": f("number", "", 0),
    "isFreeShipping": f("boolean", "", True),
})

schemas["PreviewItemVO"] = vo("结算商品行", {
    "id": f("string", "", "snacks-1"),
    "name": f("string", ""),
    "spec": f("string", ""),
    "image": f("string", ""),
    "price": f("number", "元", 99.00),
    "list": f("number", "元", 129.00),
    "qty": f("integer", "", 2),
    "total": f("number", "行小计，元", 198.00),
})

schemas["CouponBriefVO"] = vo("结算页可用券", {
    "id": f("string", "", "12"),
    "name": f("string", "", "新人券 满 99 减 10"),
    "discount": f("number", "可减金额，元", 10.00),
    "usable": f("boolean", "是否满足门槛", True),
    "reason": f("string", "不可用原因", nullable=True),
})

schemas["AddressVO"] = vo("收货地址", {
    "id": f("string", "", "1"),
    "receiver": f("string", "", "罗耀"),
    "phone": f("string", "本人查看自己的地址返回真实手机号", "13800138000"),
    "province": f("string", "", "浙江省"),
    "city": f("string", "", "杭州市"),
    "district": f("string", "", "西湖区"),
    "detail": f("string", "", "文三路 88 号 OWL 公寓 3 幢 1201 室"),
    "tag": f("string", "", "家"),
    "isDefault": f("boolean", "", True),
})

schemas["PreviewVO"] = vo("结算预览", {
    "items": arr(ref("PreviewItemVO")),
    "amounts": ref("AmountVO"),
    "shipping": ref("ShippingVO"),
    "address": ref("AddressVO"),
    "coupons": arr(ref("CouponBriefVO")),
    "warnings": arr(f("string"), "库存不足等提示，不阻断"),
})

schemas["OrderBriefVO"] = vo("订单列表项", {
    "orderNo": f("string", "", "OWL20261028000123"),
    "status": f("integer", "10待付款 20待发货 30待收货 40已完成 50已取消 60退款中 70已退款", 10),
    "statusText": f("string", "", "待付款"),
    "payAmount": f("number", "元", 188.00),
    "totalQty": f("integer", "", 2),
    "firstItemName": f("string", "", "每日混合坚果 750g"),
    "coverImage": f("string", "", "/static/products/snacks-1.jpg"),
    "createdAt": f("integer", "", 1730106600000, format="int64"),
    "expireAt": f("integer", "待付款超时时间", 1730110800000, format="int64"),
})

schemas["OrderItemVO"] = vo("订单行（快照，不随商品改价变化）", {
    "id": f("string", "", "snacks-1"),
    "name": f("string", ""),
    "spec": f("string", ""),
    "image": f("string", ""),
    "price": f("number", "", 99.00),
    "list": f("number", "", 129.00),
    "qty": f("integer", "", 2),
    "total": f("number", "", 198.00),
})

schemas["TimelineVO"] = vo("订单时间线", {
    "status": f("integer", "", 10),
    "statusText": f("string", "", "待付款"),
    "at": f("integer", "", 1730106600000, format="int64"),
    "remark": f("string", "", "订单创建"),
})

schemas["OrderDetailVO"] = vo("订单详情", {
    "orderNo": f("string", ""),
    "status": f("integer", "", 10),
    "statusText": f("string", "", "待付款"),
    "payStatus": f("integer", "0未支付 1已支付 2已退款", 0),
    "amounts": ref("AmountVO"),
    "items": arr(ref("OrderItemVO")),
    "address": ref("AddressVO"),
    "deliveryType": f("integer", "", 1),
    "deliveryText": f("string", "", "极速达 · 最快 1 小时送达"),
    "remark": f("string", "", "放门口"),
    "payNo": f("string", "", "P20261028231001001"),
    "createdAt": f("integer", "", 1730106600000, format="int64"),
    "payTime": f("integer", "", None, format="int64", nullable=True),
    "deliverTime": f("integer", "", None, format="int64", nullable=True),
    "receiveTime": f("integer", "", None, format="int64", nullable=True),
    "closeTime": f("integer", "", None, format="int64", nullable=True),
    "cancelReason": f("string", "", None, nullable=True),
    "timeline": arr(ref("TimelineVO")),
})

schemas["OrderCountVO"] = vo("订单角标（个人中心用）", {
    "unpaid": f("integer", "待付款", 1),
    "shipping": f("integer", "待收货", 2),
    "review": f("integer", "待评价", 0),
    "after": f("integer", "退换/售后", 0),
})

schemas["OrderCreateVO"] = vo("下单结果", {
    "orderNo": f("string", "", "OWL20261028000123"),
    "payNo": f("string", "", "P20261028231001001"),
    "payAmount": f("number", "元", 188.00),
    "status": f("integer", "", 10),
    "expireAt": f("integer", "支付截止，毫秒", 1730110800000, format="int64"),
})

schemas["PayVO"] = vo("发起支付结果", {
    "payNo": f("string", "", "P20261028231001001"),
    "channel": f("string", "", "mock"),
    "amount": f("number", "元", 188.00),
    "mockToken": f("string", "模拟渠道凭证，回调时带上", "mock_9f2c1a"),
    "expireAt": f("integer", "", 1730110800000, format="int64"),
})

schemas["PayCallbackVO"] = vo("支付回调结果", {
    "orderNo": f("string", ""),
    "payStatus": f("integer", "1 成功", 1),
    "orderStatus": f("integer", "20 待发货", 20),
})

schemas["PayDetailVO"] = vo("支付单", {
    "payNo": f("string", ""),
    "orderNo": f("string", ""),
    "channel": f("string", "", "mock"),
    "amount": f("number", "元", 188.00),
    "status": f("integer", "0待支付 1成功 2失败 3关闭 4已退款", 1),
    "paidAt": f("integer", "", 1730106660000, format="int64", nullable=True),
})

schemas["RefundVO"] = vo("退款单", {
    "refundNo": f("string", "", "R20261028231500001"),
    "amount": f("number", "元", 188.00),
    "status": f("integer", "0处理中 1成功 2失败", 0),
})

schemas["AssetsVO"] = vo("账户资产", {
    "coupons": f("integer", "未使用券数量", 3),
    "points": f("integer", "", 2860),
    "balance": f("number", "元", 120.50),
})

schemas["ProfileVO"] = vo("个人中心汇总（一次给全）", {
    "user": ref("UserVO"),
    "assets": ref("AssetsVO"),
    "orderCounts": ref("OrderCountVO"),
})

schemas["CouponVO"] = vo("我的优惠券", {
    "id": f("string", "", "12"),
    "name": f("string", "", "新人券 满 99 减 10"),
    "type": f("integer", "1满减 2折扣", 1),
    "threshold": f("number", "门槛，元", 99.00),
    "discount": f("number", "满减金额，元", 10.00),
    "startAt": f("integer", "", 1730100000000, format="int64"),
    "endAt": f("integer", "", 1732700000000, format="int64"),
    "status": f("integer", "0未用 1已用 2过期", 0),
    "usable": f("boolean", "", True),
})

schemas["PointsLogVO"] = vo("积分流水", {
    "id": f("string", ""),
    "change": f("integer", "正加负减", 50),
    "type": f("integer", "1下单 2评价 3退款扣回 4后台调整", 2),
    "remark": f("string", "", "订单评价奖励"),
    "createdAt": f("integer", "", 1730106600000, format="int64"),
})

schemas["MessageVO"] = vo("站内信", {
    "id": f("string", "", "10086"),
    "kind": f("string", "直接对齐前端", "order", enum=["order", "promo", "system"]),
    "title": f("string", "", "订单已发出"),
    "body": f("string", "", "你购买的「谷饲西冷牛排 1kg」已由杭州文三路店拣货完成"),
    "time": f("string", "服务端格式化好的展示文案", "12 分钟前"),
    "read": f("boolean", "", False),
    "createdAt": f("integer", "", 1730105880000, format="int64"),
    "bizNo": f("string", "关联单号，点进去可跳", "OWL20261028000123"),
})

schemas["UnreadVO"] = vo("未读数", {
    "total": f("integer", "", 3),
    "order": f("integer", "", 2),
    "promo": f("integer", "", 1),
    "system": f("integer", "", 0),
})

schemas["UsageVO"] = vo("模型用量", {
    "promptTokens": f("integer", "", 420),
    "completionTokens": f("integer", "", 86),
    "totalTokens": f("integer", "", 506),
})

schemas["ChatResponseVO"] = vo("AI 回复（必须含 reply，前端认这个字段）", {
    "reply": f("string", "", "已经免运费了 —— 已选 ¥198.00，过了 ¥99 门槛。"),
    "model": f("string", "", "deepseek-chat"),
    "usage": ref("UsageVO"),
    "degraded": f("boolean", "模型不可用时的兜底回复标记", False),
})

schemas["AdminVO"] = vo("管理员", {
    "id": f("string", "", "1"),
    "username": f("string", "", "admin"),
    "nickname": f("string", "", "超级管理员"),
    "role": f("string", "", "super", enum=["super", "ops"]),
})

schemas["AdminLoginVO"] = vo("管理员登录结果", {
    "accessToken": f("string"),
    "expiresIn": f("integer", "", 7200),
    "admin": ref("AdminVO"),
})

schemas["StatsVO"] = vo("后台概览", {
    "gmv": f("number", "只算已支付且未退款，元", 128600.00),
    "orderCount": f("integer", "", 326),
    "userCount": f("integer", "", 158),
    "productCount": f("integer", "", 90),
    "lowStockCount": f("integer", "库存 < 10 且上架", 3),
    "todayOrderCount": f("integer", "", 12),
    "todayGmv": f("number", "元", 2680.00),
})

schemas["AdminProductVO"] = vo("后台商品（比 ProductVO 多管理字段）", {
    "id": f("string", "", "snacks-1"),
    "name": f("string", ""),
    "categoryCode": f("string", "", "snacks"),
    "subCode": f("string", "", "nut"),
    "spec": f("string", ""),
    "price": f("number", "", 99.00),
    "list": f("number", "", 129.00),
    "image": f("string", ""),
    "stock": f("integer", "", 200),
    "sales": f("integer", "", 12),
    "status": f("integer", "1上架 0下架", 1),
    "isFeatured": f("boolean", "", True),
    "updatedAt": f("integer", "", 1730106600000, format="int64"),
})


def okd(schema, data_example, desc="成功"):
    return ok(schema, {"code": 0, "message": "ok", "data": data_example,
                       "traceId": "b7f3c2d1e4a5"}, desc)


# ============================== 认证 /auth ==============================
ep("sendSmsCode", "/auth/sms-code", "post", ["认证"], "发送短信验证码",
   "dev 环境会把 code 回传便于联调，prod 返回 null。同一手机号 60 秒一次，同 IP 每小时 20 条。",
   req=body(ref("SmsCodeRequest"), {"phone": "13800138000", "scene": "register"}),
   resp=okd(obj("验证码", {
       "expireIn": f("integer", "有效期，秒", 300),
       "code": f("string", "仅 dev 返回", "482913"),
   }), {"expireIn": 300, "code": "482913"}), public=True)

ep("register", "/auth/register", "post", ["认证"], "注册",
   "手机号已注册 20003；验证码错误 20002；密码需 8-20 位含字母数字。注册成功发新人券并写欢迎消息。",
   req=body(ref("RegisterRequest"), {
       "name": "罗耀", "phone": "13800138000", "code": "482913",
       "password": "abc12345", "agree": True}),
   resp=okd(ref("LoginVO"), {
       "accessToken": "eyJhbGciOi...", "refreshToken": "eyJhbGciOi...", "expiresIn": 7200,
       "user": {"id": "1001", "name": "罗耀", "phone": "13800138000", "email": "",
                "level": "normal", "memberNo": "OWL-2026-4821", "points": 0,
                "coupons": 1, "balance": 0, "joinedAt": 1730100000000, "isGuest": False}}),
   public=True)

ep("login", "/auth/login", "post", ["认证"], "登录",
   "account 支持手机号或邮箱。账号不存在与密码错误统一返回 20001。连错 5 次锁 15 分钟。",
   req=body(ref("LoginRequest"), {"account": "13800138000", "password": "abc12345", "remember": True}),
   resp=okd(ref("LoginVO"), {
       "accessToken": "eyJhbGciOi...", "refreshToken": "eyJhbGciOi...", "expiresIn": 7200,
       "user": {"id": "1001", "name": "罗耀", "phone": "13800138000", "email": "",
                "level": "black", "memberNo": "OWL-2026-4821", "points": 2860,
                "coupons": 3, "balance": 120.50, "joinedAt": 1730100000000, "isGuest": False}}),
   public=True)

ep("guestLogin", "/auth/guest", "post", ["认证"], "游客登录",
   "创建真实用户行，is_guest=1，24 小时后定时清理。游客可加购、下单、看消息，不能改密码。",
   resp=okd(ref("LoginVO"), {
       "accessToken": "eyJhbGciOi...", "refreshToken": "eyJhbGciOi...", "expiresIn": 7200,
       "user": {"id": "1002", "name": "访客", "phone": "", "email": "", "level": "guest",
                "memberNo": "", "points": 0, "coupons": 0, "balance": 0,
                "joinedAt": 1730100000000, "isGuest": True}}),
   public=True)

ep("refreshToken", "/auth/refresh", "post", ["认证"], "刷新 token",
   "refreshToken 必须 type=refresh 且 Redis 中存在。刷新后旧 jti 失效（轮换）。",
   req=body(ref("RefreshTokenRequest"), {"refreshToken": "eyJhbGciOi..."}),
   resp=okd(obj("新 token 对", {
       "accessToken": f("string"), "refreshToken": f("string"),
       "expiresIn": f("integer", "", 7200),
   }), {"accessToken": "eyJ...", "refreshToken": "eyJ...", "expiresIn": 7200}),
   public=True)

ep("logout", "/auth/logout", "post", ["认证"], "退出登录",
   "删除该用户全部 refresh token，当前 access token 剩余 TTL 写入黑名单。")

ep("getMe", "/auth/me", "get", ["认证"], "当前用户", "返回 UserVO，前端刷新页面时用它恢复会话。",
   resp=okd(ref("UserVO"), {
       "id": "1001", "name": "罗耀", "phone": "13800138000", "email": "", "level": "black",
       "memberNo": "OWL-2026-4821", "points": 2860, "coupons": 3, "balance": 120.50,
       "joinedAt": 1730100000000, "isGuest": False}))

ep("updateMe", "/auth/me", "patch", ["认证"], "修改资料", "字段可选，只改传入的。",
   req=body(ref("UpdateProfileRequest"), {"nickname": "阿枢", "email": "a@b.com"}),
   resp=okd(ref("UserVO"), {
       "id": "1001", "name": "阿枢", "phone": "13800138000", "email": "a@b.com",
       "level": "black", "memberNo": "OWL-2026-4821", "points": 2860, "coupons": 3,
       "balance": 120.50, "joinedAt": 1730100000000, "isGuest": False}))

ep("changePassword", "/auth/password", "put", ["认证"], "修改密码",
   "校验旧密码；成功后踢掉所有 refresh token，强制重新登录。",
   req=body(ref("ChangePasswordRequest"), {"oldPassword": "abc12345", "newPassword": "xyz98765"}))

# ============================== 商品 /c ==============================
ep("getHome", "/c/home", "get", ["商品"], "首页聚合",
   "一次返回品牌条、15 个类目（含 8 个细分及 count，不含 items）、12 件推荐。整体缓存 5 分钟。",
   resp=okd(ref("HomeVO"), {
       "brand": {"name": "OWL 商城", "desc": "15 个大类 · 120 个细分 · 最快 1 小时送达"},
       "categories": [{"id": "snacks", "name": "零食", "hint": "坚果 · 炒货 · 果干",
                       "glyph": "<path d=\"M12 3.4...\"/>", "productCount": 6,
                       "subs": [{"id": "nut", "name": "坚果炒货", "count": 2},
                                {"id": "seed", "name": "瓜子松子", "count": 0}]}],
       "featured": [{"id": "snacks-1", "name": "每日混合坚果 750g", "spec": "7 种果仁 · 30 小包",
                     "price": 99.00, "list": 129.00, "tag": "会员价", "emoji": "🥜",
                     "image": "/static/products/snacks-1.jpg", "sub": "nut",
                     "detail": {"slogan": "一天一小包，把坚果吃成习惯", "intro": "……",
                                "facts": {"规格": "750g / 30 小包"}, "tips": ["每天 25-35g"]}}],
       "freeShippingAt": 99, "shippingText": "满 ¥99 免运费 · 最快 1 小时送达"}),
   public=True)

ep("listCategories", "/c/categories", "get", ["商品"], "类目列表",
   "15 个类目，每个含全部 8 个细分（含 count=0）。不含 items。",
   resp=ok(arr(ref("CategoryVO"), "类目数组")), public=True)

ep("getCategory", "/c/categories/{code}", "get", ["商品"], "类目详情",
   "传 sub 且该细分有货 → 只返回该细分；细分 0 件 → 返回整类且 fallback=true；不传 sub → 整类。",
   params=[pp("code", "类目 code", "snacks"),
           qp("sub", "细分 code，可空", example="nut")],
   resp=okd(ref("CategoryVO"), {
       "id": "snacks", "name": "零食", "hint": "坚果 · 炒货 · 果干",
       "glyph": "<path .../>", "productCount": 6,
       "subs": [{"id": "nut", "name": "坚果炒货", "count": 2}],
       "items": [{"id": "snacks-1", "name": "每日混合坚果 750g", "spec": "7 种果仁 · 30 小包",
                  "price": 99.00, "list": 129.00, "tag": "会员价", "emoji": "🥜",
                  "image": "/static/products/snacks-1.jpg", "sub": "nut",
                  "detail": {"slogan": "一天一小包", "intro": "……",
                             "facts": {"规格": "750g / 30 小包"}, "tips": []}}],
       "currentSub": "nut", "fallback": False}),
   public=True)

ep("listProducts", "/c/products", "get", ["商品"], "商品列表 / 搜索",
   "keyword 同时匹配商品名、规格、类目名、类目 hint、细分名（对齐前端 searchHits）。",
   params=[qp("keyword", "关键词，可空", example="坚果"),
           qp("category", "类目 code", example="snacks"),
           qp("sub", "细分 code", example="nut"),
           qp("sort", "排序", enum=["default", "price_asc", "price_desc", "sales_desc"],
              example="default"),
           qp("page", "页码", "integer", 1), qp("size", "每页条数，上限 100", "integer", 20)],
   resp=ok(page_of(ref("ProductVO"), 6)), public=True)

ep("getProduct", "/c/products/{code}", "get", ["商品"], "商品详情",
   "带 siblings：同细分优先，不足用同类补，最多 4 件，排除自己。facts 必须保序。",
   params=[pp("code", "商品 code", "snacks-1")],
   resp=okd(obj("商品详情 + 同类推荐", {
       "id": f("string"), "name": f("string"), "spec": f("string"),
       "price": f("number"), "list": f("number"), "tag": f("string"),
       "emoji": f("string"), "image": f("string"), "sub": f("string"),
       "detail": ref("ProductDetailVO"),
       "siblings": arr(ref("ProductVO")),
   }), {"id": "snacks-1", "name": "每日混合坚果 750g", "spec": "7 种果仁 · 30 小包",
        "price": 99.00, "list": 129.00, "tag": "会员价", "emoji": "🥜",
        "image": "/static/products/snacks-1.jpg", "sub": "nut",
        "detail": {"slogan": "一天一小包，把坚果吃成习惯", "intro": "……",
                   "facts": {"规格": "750g / 30 小包", "果仁含量": "约 88%"},
                   "tips": ["每天 25-35g 就够了"]},
        "siblings": [{"id": "snacks-2", "name": "东北开口榛子 500g", "spec": "原味炒熟 · 皮薄好剥",
                      "price": 45.90, "list": 59.00, "tag": "人气", "emoji": "🌰",
                      "image": "/static/products/snacks-2.jpg", "sub": "nut",
                      "detail": {"slogan": "", "intro": "", "facts": {}, "tips": []}}]}),
   public=True)

ep("listFeatured", "/c/featured", "get", ["商品"], "推荐位",
   "is_featured=1 且上架，按 sort 升序。",
   params=[qp("limit", "条数，默认 12", "integer", 12)],
   resp=ok(arr(ref("ProductVO"))), public=True)

# ============================== 购物车 /cart ==============================
ep("getCart", "/cart", "get", ["购物车"], "查询购物车",
   "lines 按 updated_at 倒序；summary 全部由服务端算，前端不要自己乘。",
   resp=okd(ref("CartVO"), {
       "lines": [{"id": "snacks-1", "name": "每日混合坚果 750g", "spec": "7 种果仁 · 30 小包",
                  "price": 99.00, "list": 129.00, "emoji": "🥜",
                  "image": "/static/products/snacks-1.jpg", "qty": 3,
                  "checked": True, "invalid": False}],
       "summary": {"count": 3, "amount": 297.00, "saved": 90.00, "checkedCount": 3,
                   "checkedAmount": 297.00, "allChecked": True, "isFreeShipping": True,
                   "toFreeShipping": 0, "shippingRate": 100, "freeShippingAt": 99, "shipFee": 0}}))

ep("addCartItem", "/cart/items", "post", ["购物车"], "加入购物车",
   "已存在则累加，上限 99；加入时 checked 置 1。商品下架 30002，库存 0 返回 30003。",
   req=body(ref("CartAddRequest"), {"productId": "snacks-1", "qty": 1}),
   resp=okd(ref("CartVO"), {
       "lines": [{"id": "snacks-1", "name": "每日混合坚果 750g", "spec": "7 种果仁 · 30 小包",
                  "price": 99.00, "list": 129.00, "emoji": "🥜",
                  "image": "/static/products/snacks-1.jpg", "qty": 1,
                  "checked": True, "invalid": False}],
       "summary": {"count": 1, "amount": 99.00, "saved": 30.00, "checkedCount": 1,
                   "checkedAmount": 99.00, "allChecked": True, "isFreeShipping": True,
                   "toFreeShipping": 0, "shippingRate": 100, "freeShippingAt": 99, "shipFee": 0}}))

ep("updateCartItem", "/cart/items/{productId}", "patch", ["购物车"], "修改数量 / 勾选",
   "字段可选。qty 越界截断到 1-99，不报错。行不存在 10004。",
   params=[pp("productId", "商品 code", "snacks-1")],
   req=body(ref("CartUpdateRequest"), {"qty": 3, "checked": True}),
   resp=okd(ref("CartVO"), {
       "lines": [{"id": "snacks-1", "name": "每日混合坚果 750g", "spec": "7 种果仁 · 30 小包",
                  "price": 99.00, "list": 129.00, "emoji": "🥜",
                  "image": "/static/products/snacks-1.jpg", "qty": 3,
                  "checked": True, "invalid": False}],
       "summary": {"count": 3, "amount": 297.00, "saved": 90.00, "checkedCount": 3,
                   "checkedAmount": 297.00, "allChecked": True, "isFreeShipping": True,
                   "toFreeShipping": 0, "shippingRate": 100, "freeShippingAt": 99, "shipFee": 0}}))

ep("removeCartItem", "/cart/items/{productId}", "delete", ["购物车"], "删除购物车行",
   params=[pp("productId", "商品 code", "snacks-1")],
   resp=okd(ref("CartVO"), {"lines": [], "summary": {"count": 0, "amount": 0, "saved": 0,
                                                     "checkedCount": 0, "checkedAmount": 0,
                                                     "allChecked": False, "isFreeShipping": False,
                                                     "toFreeShipping": 99, "shippingRate": 0,
                                                     "freeShippingAt": 99, "shipFee": 5}}))

ep("checkCart", "/cart/checked", "put", ["购物车"], "批量勾选",
   "ids 不传表示全部行。", req=body(ref("CartCheckRequest"), {"checked": True}),
   resp=okd(ref("CartVO"), {
       "lines": [], "summary": {"count": 3, "amount": 297.00, "saved": 90.00, "checkedCount": 3,
                                "checkedAmount": 297.00, "allChecked": True, "isFreeShipping": True,
                                "toFreeShipping": 0, "shippingRate": 100,
                                "freeShippingAt": 99, "shipFee": 0}}))

ep("removeChecked", "/cart/checked", "delete", ["购物车"], "删除已勾选",
   resp=okd(ref("CartVO"), {"lines": [], "summary": {"count": 0, "amount": 0, "saved": 0,
                                                     "checkedCount": 0, "checkedAmount": 0,
                                                     "allChecked": False, "isFreeShipping": False,
                                                     "toFreeShipping": 99, "shippingRate": 0,
                                                     "freeShippingAt": 99, "shipFee": 5}}))

ep("clearCart", "/cart", "delete", ["购物车"], "清空购物车")

ep("mergeCart", "/cart/merge", "post", ["购物车"], "合并本地购物车",
   "登录成功后调用一次，前端拿到结果后清空 localStorage 的 owl.cart.v1。下架商品进 skipped。",
   req=body(ref("CartMergeRequest"), {"lines": [{"productId": "snacks-1", "qty": 2},
                                                {"productId": "fruit-3", "qty": 1}]}),
   resp=okd(ref("CartMergeVO"), {
       "lines": [], "summary": {"count": 2, "amount": 198.00, "saved": 60.00, "checkedCount": 2,
                                "checkedAmount": 198.00, "allChecked": True, "isFreeShipping": True,
                                "toFreeShipping": 0, "shippingRate": 100,
                                "freeShippingAt": 99, "shipFee": 0},
       "skipped": ["fruit-3"]}))

# ============================== 交易 /trade ==============================
ep("preview", "/trade/preview", "post", ["交易"], "结算预览",
   "服务端重新查价，请求体里没有价格字段。商品下架 30002，库存不足只在 warnings 里提示不阻断。",
   req=body(ref("PreviewRequest"), {
       "lines": [{"productId": "snacks-1", "qty": 2}],
       "addressId": "1", "couponId": "12"}),
   resp=okd(ref("PreviewVO"), {
       "items": [{"id": "snacks-1", "name": "每日混合坚果 750g", "spec": "7 种果仁 · 30 小包",
                  "image": "/static/products/snacks-1.jpg", "price": 99.00, "list": 129.00,
                  "qty": 2, "total": 198.00}],
       "amounts": {"goodsAmount": 198.00, "discount": 10.00, "shipFee": 0,
                   "payable": 188.00, "saved": 60.00},
       "shipping": {"freeShippingAt": 99, "toFreeShipping": 0, "isFreeShipping": True},
       "address": {"id": "1", "receiver": "罗耀", "phone": "13800138000", "province": "浙江省",
                   "city": "杭州市", "district": "西湖区",
                   "detail": "文三路 88 号 OWL 公寓 3 幢 1201 室", "tag": "家", "isDefault": True},
       "coupons": [{"id": "12", "name": "新人券 满 99 减 10", "discount": 10.00,
                    "usable": True, "reason": None}],
       "warnings": []}))

ep("createOrder", "/trade/orders", "post", ["交易"], "创建订单",
   "一个事务里：幂等校验 → 重新计价 → 乐观锁扣库存 → 写订单/订单行/库存流水 → 锁券 → 按 fromCart 删购物车行。"
   "clientToken 重复提交返回同一订单；库存不足 30003；券被抢用 40005。",
   req=body(ref("OrderCreateRequest"), {
       "lines": [{"productId": "snacks-1", "qty": 2}],
       "addressId": "1", "couponId": "12", "remark": "放门口",
       "deliveryType": 1, "fromCart": True,
       "clientToken": "c1f4a9e2-8b3d-4a71-9f22-6d0e5b7a1c33"}),
   resp=okd(ref("OrderCreateVO"), {
       "orderNo": "OWL20261028000123", "payNo": "P20261028231001001",
       "payAmount": 188.00, "status": 10, "expireAt": 1730110800000}))

ep("listOrders", "/trade/orders", "get", ["交易"], "订单列表",
   "status 映射：all / unpaid(10) / shipping(30) / review(40 且未评价) / after(60,70)。",
   params=[qp("status", "状态筛选", enum=["all", "unpaid", "shipping", "review", "after"],
              example="all"),
           qp("page", "页码", "integer", 1), qp("size", "每页条数", "integer", 10)],
   resp=ok(page_of(ref("OrderBriefVO"), 3)))

ep("getOrder", "/trade/orders/{orderNo}", "get", ["交易"], "订单详情",
   "只能查自己的订单，越权 10003，不存在 40002。",
   params=[pp("orderNo", "订单号", "OWL20261028000123")],
   resp=okd(ref("OrderDetailVO"), {
       "orderNo": "OWL20261028000123", "status": 10, "statusText": "待付款", "payStatus": 0,
       "amounts": {"goodsAmount": 198.00, "discount": 10.00, "shipFee": 0,
                   "payable": 188.00, "saved": 60.00},
       "items": [{"id": "snacks-1", "name": "每日混合坚果 750g", "spec": "7 种果仁 · 30 小包",
                  "image": "/static/products/snacks-1.jpg", "price": 99.00, "list": 129.00,
                  "qty": 2, "total": 198.00}],
       "address": {"id": "1", "receiver": "罗耀", "phone": "138****8000", "province": "浙江省",
                   "city": "杭州市", "district": "西湖区",
                   "detail": "文三路 88 号 OWL 公寓 3 幢 1201 室", "tag": "家", "isDefault": True},
       "deliveryType": 1, "deliveryText": "极速达 · 最快 1 小时送达", "remark": "放门口",
       "payNo": "P20261028231001001", "createdAt": 1730106600000,
       "payTime": None, "deliverTime": None, "receiveTime": None, "closeTime": None,
       "cancelReason": None,
       "timeline": [{"status": 10, "statusText": "待付款", "at": 1730106600000, "remark": "订单创建"}]}))

ep("cancelOrder", "/trade/orders/{orderNo}/cancel", "post", ["交易"], "取消订单",
   "只有 status=10 可取消。回补库存、退券、写状态流水、发消息。",
   params=[pp("orderNo", "订单号", "OWL20261028000123")],
   req=body(ref("OrderCancelRequest"), {"reason": "不想要了"}),
   resp=okd(ref("OrderDetailVO"), {
       "orderNo": "OWL20261028000123", "status": 50, "statusText": "已取消", "payStatus": 0,
       "amounts": {"goodsAmount": 198.00, "discount": 10.00, "shipFee": 0,
                   "payable": 188.00, "saved": 60.00},
       "items": [], "address": None, "deliveryType": 1, "deliveryText": "",
       "remark": None, "payNo": None, "createdAt": 1730106600000,
       "payTime": None, "deliverTime": None, "receiveTime": None,
       "closeTime": 1730107260000, "cancelReason": "不想要了", "timeline": []}))

ep("confirmOrder", "/trade/orders/{orderNo}/confirm", "post", ["交易"], "确认收货",
   "只有 status=30 可调用，成功后 40 已完成。",
   params=[pp("orderNo", "订单号", "OWL20261028000123")],
   resp=okd(ref("OrderDetailVO"), {
       "orderNo": "OWL20261028000123", "status": 40, "statusText": "已完成", "payStatus": 1,
       "amounts": {"goodsAmount": 198.00, "discount": 10.00, "shipFee": 0,
                   "payable": 188.00, "saved": 60.00},
       "items": [], "address": None, "deliveryType": 1, "deliveryText": "",
       "remark": None, "payNo": None, "createdAt": 1730106600000,
       "payTime": 1730106660000, "deliverTime": 1730110000000,
       "receiveTime": 1730120000000, "closeTime": None, "cancelReason": None, "timeline": []}))

ep("orderCounts", "/trade/orders/counts", "get", ["交易"], "订单角标",
   "个人中心用，一次给四个数字。counts 是精确路径，Spring 会优先匹配，不会落到 {orderNo}。",
   resp=okd(ref("OrderCountVO"), {"unpaid": 1, "shipping": 2, "review": 0, "after": 0}))

# ============================== 支付 /pay ==============================
ep("mockPay", "/pay/mock/{orderNo}", "post", ["支付"], "发起支付（模拟渠道）",
   "订单必须是自己的、status=10、未超时。已存在待支付单则复用，不重复创建。",
   params=[pp("orderNo", "订单号", "OWL20261028000123")],
   resp=okd(ref("PayVO"), {
       "payNo": "P20261028231001001", "channel": "mock", "amount": 188.00,
       "mockToken": "mock_9f2c1a", "expireAt": 1730110800000}))

ep("mockCallback", "/pay/mock/callback", "post", ["支付"], "模拟支付回调",
   "幂等：payment.status 已是 1 时直接返回成功。成功后订单 10→20，写流水、发消息。"
   "白名单接口，用 mockToken 鉴权，不要要求 JWT。",
   req=body(ref("MockCallbackRequest"), {"payNo": "P20261028231001001",
                                         "mockToken": "mock_9f2c1a", "success": True}),
   resp=okd(ref("PayCallbackVO"), {"orderNo": "OWL20261028000123",
                                   "payStatus": 1, "orderStatus": 20}),
   public=True)

ep("getPayment", "/pay/{payNo}", "get", ["支付"], "查询支付单",
   params=[pp("payNo", "支付单号", "P20261028231001001")],
   resp=okd(ref("PayDetailVO"), {
       "payNo": "P20261028231001001", "orderNo": "OWL20261028000123", "channel": "mock",
       "amount": 188.00, "status": 1, "paidAt": 1730106660000}))

ep("refund", "/pay/refund", "post", ["支付"], "申请退款",
   "只允许 pay_status=1 且订单状态在 20/30/40；本期全额退；重复申请 50003。模拟渠道同步成功。",
   req=body(ref("RefundRequest"), {"orderNo": "OWL20261028000123", "reason": "买错了"}),
   resp=okd(ref("RefundVO"), {"refundNo": "R20261028231500001",
                              "amount": 188.00, "status": 0}))

ep("getRefund", "/pay/refunds/{refundNo}", "get", ["支付"], "查询退款单",
   params=[pp("refundNo", "退款单号", "R20261028231500001")],
   resp=okd(ref("RefundVO"), {"refundNo": "R20261028231500001",
                              "amount": 188.00, "status": 1}))

# ============================== 个人中心 /m ==============================
ep("getProfile", "/m/profile", "get", ["个人中心"], "个人中心汇总",
   "一次返回 user + assets + orderCounts。orderCounts 用一条 GROUP BY 查完，不要 4 条 count。",
   resp=okd(ref("ProfileVO"), {
       "user": {"id": "1001", "name": "罗耀", "phone": "13800138000", "email": "",
                "level": "black", "memberNo": "OWL-2026-4821", "points": 2860,
                "coupons": 3, "balance": 120.50, "joinedAt": 1730100000000, "isGuest": False},
       "assets": {"coupons": 3, "points": 2860, "balance": 120.50},
       "orderCounts": {"unpaid": 1, "shipping": 2, "review": 0, "after": 0}}))

ep("listAddresses", "/m/addresses", "get", ["个人中心"], "地址列表",
   "本人查看自己的地址返回真实手机号（编辑要用），订单里才打码。",
   resp=ok(arr(ref("AddressVO")), "地址数组"))

ep("createAddress", "/m/addresses", "post", ["个人中心"], "新增地址",
   "最多 20 条；isDefault=true 时在同一事务里把其它地址置 0。",
   req=body(ref("AddressRequest"), {
       "receiver": "罗耀", "phone": "13800138000", "province": "浙江省", "city": "杭州市",
       "district": "西湖区", "detail": "文三路 88 号 OWL 公寓 3 幢 1201 室",
       "tag": "家", "isDefault": True}),
   resp=okd(ref("AddressVO"), {
       "id": "1", "receiver": "罗耀", "phone": "13800138000", "province": "浙江省",
       "city": "杭州市", "district": "西湖区",
       "detail": "文三路 88 号 OWL 公寓 3 幢 1201 室", "tag": "家", "isDefault": True}))

ep("updateAddress", "/m/addresses/{id}", "put", ["个人中心"], "修改地址",
   params=[pp("id", "地址 id", "1")],
   req=body(ref("AddressRequest"), {
       "receiver": "罗耀", "phone": "13800138000", "province": "浙江省", "city": "杭州市",
       "district": "西湖区", "detail": "文三路 88 号 OWL 公寓 3 幢 1202 室",
       "tag": "家", "isDefault": True}),
   resp=okd(ref("AddressVO"), {
       "id": "1", "receiver": "罗耀", "phone": "13800138000", "province": "浙江省",
       "city": "杭州市", "district": "西湖区",
       "detail": "文三路 88 号 OWL 公寓 3 幢 1202 室", "tag": "家", "isDefault": True}))

ep("deleteAddress", "/m/addresses/{id}", "delete", ["个人中心"], "删除地址",
   params=[pp("id", "地址 id", "1")])

ep("setDefaultAddress", "/m/addresses/{id}/default", "put", ["个人中心"], "设为默认地址",
   params=[pp("id", "地址 id", "1")],
   resp=okd(ref("AddressVO"), {
       "id": "1", "receiver": "罗耀", "phone": "13800138000", "province": "浙江省",
       "city": "杭州市", "district": "西湖区",
       "detail": "文三路 88 号 OWL 公寓 3 幢 1201 室", "tag": "家", "isDefault": True}))

ep("listCoupons", "/m/coupons", "get", ["个人中心"], "我的优惠券",
   "查询时顺手把过期的置为 2（惰性过期），不要只靠定时任务。",
   params=[qp("status", "状态", enum=["unused", "used", "expired"], example="unused"),
           qp("page", "页码", "integer", 1), qp("size", "每页条数", "integer", 20)],
   resp=ok(page_of(ref("CouponVO"), 2)))

ep("listPoints", "/m/points", "get", ["个人中心"], "积分流水",
   params=[qp("page", "页码", "integer", 1), qp("size", "每页条数", "integer", 20)],
   resp=ok(page_of(ref("PointsLogVO"), 8)))

# ============================== 消息 /m/messages ==============================
ep("listMessages", "/m/messages", "get", ["消息"], "消息列表",
   "time 由服务端格式化：刚刚 / N 分钟前 / N 小时前 / 昨天 HH:mm / M 月 d 日 / yyyy 年 M 月 d 日。",
   params=[qp("kind", "类型", enum=["all", "order", "promo", "system"], example="all"),
           qp("page", "页码", "integer", 1), qp("size", "每页条数", "integer", 20)],
   resp=ok(page_of(ref("MessageVO"), 7)))

ep("unreadCount", "/m/messages/unread", "get", ["消息"], "未读数",
   resp=okd(ref("UnreadVO"), {"total": 3, "order": 2, "promo": 1, "system": 0}))

ep("readMessage", "/m/messages/{id}/read", "post", ["消息"], "标记已读",
   params=[pp("id", "消息 id", "10086")])

ep("readAllMessages", "/m/messages/read-all", "post", ["消息"], "全部已读")

ep("deleteMessage", "/m/messages/{id}", "delete", ["消息"], "删除消息",
   params=[pp("id", "消息 id", "10086")])

# ============================== AI /assistant ==============================
ep("chat", "/assistant/chat", "post", ["AI 助手"], "购物助手对话",
   "请求体是前端原样发出的结构。服务端自己拼 system prompt，忽略传入的 system；失败返回兜底文案而不是 5xx。"
   "登录 20 次/小时，匿名 5 次/小时（按 IP）。",
   req=body(ref("ChatRequest"), {
       "model": "deepseek-chat",
       "messages": [{"role": "user", "content": "帮我凑到免运费"}],
       "context": {"cartCount": 2, "cartAmount": 198.00, "cartCheckedAmount": 198.00,
                   "cartSaved": 60.00, "toFreeShipping": 0, "isFreeShipping": True,
                   "freeShippingAt": 99,
                   "lines": [{"name": "每日混合坚果 750g", "qty": 2, "price": 99.00}]}}),
   resp=okd(ref("ChatResponseVO"), {
       "reply": "已经免运费了 —— 已选 ¥198.00，过了 ¥99 门槛。\n\n这单符合极速达条件，最快 1 小时内送到。",
       "model": "deepseek-chat",
       "usage": {"promptTokens": 420, "completionTokens": 86, "totalTokens": 506},
       "degraded": False}),
   public=True)

# ============================== 后台 /admin ==============================
ep("adminLogin", "/admin/auth/login", "post", ["后台"], "管理员登录",
   "签发 scope=admin 的 token。C 端 token 访问 /admin/** 一律 403。",
   req=body(ref("AdminLoginRequest"), {"username": "admin", "password": "admin123456"}),
   resp=okd(ref("AdminLoginVO"), {
       "accessToken": "eyJ...", "expiresIn": 7200,
       "admin": {"id": "1", "username": "admin", "nickname": "超级管理员", "role": "super"}}),
   public=True)

ep("adminListProducts", "/admin/products", "get", ["后台"], "商品列表（管理端）",
   params=[qp("keyword", "关键词", example="坚果"),
           qp("status", "1上架 0下架，不传全部", "integer", 1),
           qp("page", "页码", "integer", 1), qp("size", "每页条数", "integer", 20)],
   resp=ok(page_of(ref("AdminProductVO"), 90)))

ep("adminCreateProduct", "/admin/products", "post", ["后台"], "新增商品",
   "code 由后端生成 {categoryCode}-{序号}，生成过程加 Redis 锁防并发重复。price 必须 <= list。",
   req=body(ref("AdminProductRequest"), {
       "name": "每日混合坚果 750g", "categoryCode": "snacks", "subCode": "nut",
       "spec": "7 种果仁 · 30 小包", "price": 99.00, "list": 129.00, "tag": "会员价",
       "emoji": "🥜", "image": "/static/products/snacks-1.jpg", "stock": 200,
       "isFeatured": True,
       "detail": {"slogan": "一天一小包，把坚果吃成习惯", "intro": "……",
                  "facts": {"规格": "750g / 30 小包"}, "tips": ["每天 25-35g"]}}),
   resp=okd(ref("AdminProductVO"), {
       "id": "snacks-7", "name": "每日混合坚果 750g", "categoryCode": "snacks",
       "subCode": "nut", "spec": "7 种果仁 · 30 小包", "price": 99.00, "list": 129.00,
       "image": "/static/products/snacks-1.jpg", "stock": 200, "sales": 0,
       "status": 1, "isFeatured": True, "updatedAt": 1730106600000}))

ep("adminUpdateProduct", "/admin/products/{code}", "put", ["后台"], "修改商品",
   "写操作后失效 home / categories / product:{code} 缓存。",
   params=[pp("code", "商品 code", "snacks-1")],
   req=body(ref("AdminProductRequest"), {
       "name": "每日混合坚果 750g", "categoryCode": "snacks", "subCode": "nut",
       "spec": "7 种果仁 · 30 小包", "price": 95.00, "list": 129.00, "tag": "会员价",
       "emoji": "🥜", "image": "/static/products/snacks-1.jpg", "stock": 180,
       "isFeatured": True,
       "detail": {"slogan": "一天一小包，把坚果吃成习惯", "intro": "……",
                  "facts": {"规格": "750g / 30 小包"}, "tips": ["每天 25-35g"]}}),
   resp=okd(ref("AdminProductVO"), {
       "id": "snacks-1", "name": "每日混合坚果 750g", "categoryCode": "snacks",
       "subCode": "nut", "spec": "7 种果仁 · 30 小包", "price": 95.00, "list": 129.00,
       "image": "/static/products/snacks-1.jpg", "stock": 180, "sales": 12,
       "status": 1, "isFeatured": True, "updatedAt": 1730106600000}))

ep("adminDeleteProduct", "/admin/products/{code}", "delete", ["后台"], "删除商品",
   "逻辑删除。已产生订单的商品允许删（订单有快照）。",
   params=[pp("code", "商品 code", "snacks-1")])

ep("adminUpdateProductStatus", "/admin/products/{code}/status", "patch", ["后台"], "上下架",
   params=[pp("code", "商品 code", "snacks-1")],
   req=body(ref("ProductStatusRequest"), {"status": 0}),
   resp=okd(ref("AdminProductVO"), {
       "id": "snacks-1", "name": "每日混合坚果 750g", "categoryCode": "snacks",
       "subCode": "nut", "spec": "7 种果仁 · 30 小包", "price": 99.00, "list": 129.00,
       "image": "/static/products/snacks-1.jpg", "stock": 200, "sales": 12,
       "status": 0, "isFeatured": True, "updatedAt": 1730106600000}))

ep("adminAdjustStock", "/admin/products/{code}/stock", "patch", ["后台"], "库存调整",
   "delta 正数入库、负数出库；不能减到负数（返回 10001）。写 c_stock_log type=4。",
   params=[pp("code", "商品 code", "snacks-1")],
   req=body(ref("StockAdjustRequest"), {"delta": 50, "reason": "补货"}),
   resp=okd(ref("AdminProductVO"), {
       "id": "snacks-1", "name": "每日混合坚果 750g", "categoryCode": "snacks",
       "subCode": "nut", "spec": "7 种果仁 · 30 小包", "price": 99.00, "list": 129.00,
       "image": "/static/products/snacks-1.jpg", "stock": 250, "sales": 12,
       "status": 1, "isFeatured": True, "updatedAt": 1730106600000}))

ep("adminListOrders", "/admin/orders", "get", ["后台"], "订单列表（管理端）",
   params=[qp("status", "订单状态，不传全部", "integer", 20),
           qp("keyword", "订单号/收货人", example="OWL2026"),
           qp("page", "页码", "integer", 1), qp("size", "每页条数", "integer", 20)],
   resp=ok(page_of(ref("OrderBriefVO"), 20)))

ep("adminGetOrder", "/admin/orders/{orderNo}", "get", ["后台"], "订单详情（管理端）",
   params=[pp("orderNo", "订单号", "OWL20261028000123")],
   resp=okd(ref("OrderDetailVO"), {
       "orderNo": "OWL20261028000123", "status": 20, "statusText": "待发货", "payStatus": 1,
       "amounts": {"goodsAmount": 198.00, "discount": 10.00, "shipFee": 0,
                   "payable": 188.00, "saved": 60.00},
       "items": [], "address": None, "deliveryType": 1, "deliveryText": "极速达 · 最快 1 小时送达",
       "remark": None, "payNo": "P20261028231001001", "createdAt": 1730106600000,
       "payTime": 1730106660000, "deliverTime": None, "receiveTime": None,
       "closeTime": None, "cancelReason": None, "timeline": []}))

ep("adminDeliverOrder", "/admin/orders/{orderNo}/deliver", "post", ["后台"], "订单发货",
   "只有 status=20 可发货，成功后 30 待收货，写流水、发消息。",
   params=[pp("orderNo", "订单号", "OWL20261028000123")],
   req=body(ref("DeliverRequest"), {"company": "顺丰", "trackingNo": "SF1234567890"}),
   resp=okd(ref("OrderDetailVO"), {
       "orderNo": "OWL20261028000123", "status": 30, "statusText": "待收货", "payStatus": 1,
       "amounts": {"goodsAmount": 198.00, "discount": 10.00, "shipFee": 0,
                   "payable": 188.00, "saved": 60.00},
       "items": [], "address": None, "deliveryType": 1, "deliveryText": "极速达 · 最快 1 小时送达",
       "remark": None, "payNo": "P20261028231001001", "createdAt": 1730106600000,
       "payTime": 1730106660000, "deliverTime": 1730110000000, "receiveTime": None,
       "closeTime": None, "cancelReason": None, "timeline": []}))

ep("adminStats", "/admin/stats/overview", "get", ["后台"], "概览统计",
   "gmv 只算 pay_status=1 且未退款的订单；lowStockCount 是库存 < 10 且上架。",
   resp=okd(ref("StatsVO"), {
       "gmv": 128600.00, "orderCount": 326, "userCount": 158, "productCount": 90,
       "lowStockCount": 3, "todayOrderCount": 12, "todayGmv": 2680.00}))

ep("adminListCategories", "/admin/categories", "get", ["后台"], "类目列表（管理端）",
   resp=ok(arr(ref("CategoryVO")), "类目数组"))

ep("adminCreateCategory", "/admin/categories", "post", ["后台"], "新增类目",
   "subs 全量覆盖。有商品的细分不允许删除，返回 30004。",
   req=body(ref("CategoryUpsertRequest"), {
       "code": "snacks", "name": "零食", "hint": "坚果 · 炒货 · 果干",
       "glyph": "<path d=\"M12 3.4...\"/>", "sort": 10,
       "subs": [{"code": "nut", "name": "坚果炒货"}, {"code": "seed", "name": "瓜子松子"}]}),
   resp=okd(ref("CategoryVO"), {
       "id": "snacks", "name": "零食", "hint": "坚果 · 炒货 · 果干", "glyph": "",
       "productCount": 0, "subs": [], "items": [], "currentSub": None, "fallback": False}))

ep("adminUpdateCategory", "/admin/categories/{code}", "put", ["后台"], "修改类目",
   params=[pp("code", "类目 code", "snacks")],
   req=body(ref("CategoryUpsertRequest"), {
       "code": "snacks", "name": "零食", "hint": "坚果 · 炒货 · 果干",
       "glyph": "<path d=\"M12 3.4...\"/>", "sort": 10,
       "subs": [{"code": "nut", "name": "坚果炒货"}]}),
   resp=okd(ref("CategoryVO"), {
       "id": "snacks", "name": "零食", "hint": "坚果 · 炒货 · 果干", "glyph": "",
       "productCount": 6, "subs": [], "items": [], "currentSub": None, "fallback": False}))

# ============================== 组装与输出 ==============================
TAGS = [
    {"name": "认证", "description": "注册、登录、游客、token 续期"},
    {"name": "商品", "description": "首页聚合、类目、商品详情、搜索，全部公开"},
    {"name": "购物车", "description": "需登录，游客也可用"},
    {"name": "交易", "description": "计价、下单、订单查询与状态流转"},
    {"name": "支付", "description": "模拟渠道，含回调与退款"},
    {"name": "个人中心", "description": "资料、资产、地址、券、积分"},
    {"name": "消息", "description": "站内信"},
    {"name": "AI 助手", "description": "DeepSeek 对话，服务端持 Key"},
    {"name": "后台", "description": "B 端接口，独立 token，C 端 token 403"},
]

doc = {
    "openapi": "3.0.3",
    "info": {
        "title": "OWL 商城 API",
        "version": "1.0.0",
        "description": (
            "OWL 商城 C 端 + B 端接口。\n\n"
            "**约定**：金额单位元（number）；时间毫秒时间戳（int64）；用户/商品/类目 id 均为 string。\n\n"
            "**鉴权**：除标注「公开」的接口外，均需 `Authorization: Bearer <accessToken>`。\n\n"
            "**错误**：HTTP 200 + 业务码，`code=0` 成功；401 表示未登录或 token 过期。\n\n"
            "**数据模型**：`[DTO 入参]` 开头的为请求体，`[VO 出参]` 开头的为响应体，两者不要混用。"
        ),
        "contact": {"name": "OWL-System"},
    },
    "servers": [
        {"url": "http://localhost:8080/api/v1", "description": "本地开发"},
    ],
    "tags": TAGS,
    "security": [{"bearerAuth": []}],
    "paths": paths,
    "components": {
        "schemas": schemas,
        "securitySchemes": {
            "bearerAuth": {
                "type": "http",
                "scheme": "bearer",
                "bearerFormat": "JWT",
                "description": "登录接口返回的 accessToken，直接填值，不要加 Bearer 前缀（Apifox 会自动加）",
            }
        },
    },
}

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as fp:
    json.dump(doc, fp, ensure_ascii=False, indent=2)

ops = sum(len([m for m in v if m in ("get", "post", "put", "patch", "delete")])
          for v in paths.values())
dto_n = len([k for k in schemas if k.endswith(("Request", "DTO"))])
vo_n = len([k for k in schemas if k.endswith("VO")])
print(f"openapi written: {OUT}")
print(f"paths={len(paths)} operations={ops} schemas={len(schemas)} (DTO={dto_n}, VO={vo_n})")
