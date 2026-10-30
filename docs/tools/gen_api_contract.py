#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从 owl-mall-openapi.json 生成《API 契约 · DTO/VO》文档。

用法：
    python docs/tools/gen_api_contract.py
产出：
    docs/API契约-DTO-VO.md
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "owl-mall-openapi.json")
OUT = os.path.join(HERE, "..", "API契约-DTO-VO.md")

METHODS = ("get", "post", "put", "patch", "delete")


def load():
    with open(SRC, encoding="utf-8") as fp:
        return json.load(fp)


def schema_name(s):
    """把 schema 转成可读类型名"""
    if not s:
        return "-"
    if "$ref" in s:
        return s["$ref"].split("/")[-1]
    t = s.get("type", "object")
    if t == "array":
        return f"array<{schema_name(s.get('items'))}>"
    if t == "object":
        props = s.get("properties") or {}
        if "list" in props and "total" in props:
            return f"PageResult<{schema_name(props['list'].get('items'))}>"
        ap = s.get("additionalProperties")
        if isinstance(ap, dict):
            return f"map<string,{schema_name(ap)}>"
        return "object"
    return t


def field_list(schema, schemas, limit=12):
    """把 properties 压成一行：name (type), ..."""
    if "$ref" in schema:
        schema = schemas.get(schema["$ref"].split("/")[-1], {})
    props = schema.get("properties") or {}
    parts = []
    for name, p in list(props.items())[:limit]:
        parts.append(f"`{name}` ({schema_name(p)})")
    if len(props) > limit:
        parts.append(f"…共 {len(props)} 个")
    return "、".join(parts) if parts else "—"


def main():
    d = load()
    schemas = d["components"]["schemas"]
    paths = d["paths"]

    rows = []
    idx = 0
    for tag in [t["name"] for t in d["tags"]]:
        for path, ms in paths.items():
            for m, op in ms.items():
                if m not in METHODS or tag not in op.get("tags", []):
                    continue
                idx += 1
                auth = "公开" if op.get("security") == [] else "Bearer"
                req = "—"
                rb = op.get("requestBody")
                if rb:
                    req = schema_name(rb["content"]["application/json"]["schema"])
                resp = "—"
                try:
                    allof = op["responses"]["200"]["content"]["application/json"]["schema"]["allOf"]
                    data = allof[1]["properties"]["data"]
                    resp = schema_name(data)
                except (KeyError, IndexError):
                    resp = "null"
                rows.append((idx, m.upper(), path, op.get("summary", ""), auth, req, resp))

    api_table = "\n".join(
        f"| {i} | `{m}` | `{p}` | {s} | {a} | {r} | {v} |"
        for i, m, p, s, a, r, v in rows
    )

    dto_rows = []
    vo_rows = []
    for name in sorted(schemas):
        s = schemas[name]
        desc = s.get("description", "").replace("[DTO 入参] ", "").replace("[VO 出参] ", "")
        line = f"| `{name}` | {field_list(s, schemas)} | {desc} |"
        if name.endswith(("Request", "DTO")):
            dto_rows.append(line)
        elif name.endswith("VO"):
            vo_rows.append(line)

    dto_table = "\n".join(dto_rows)
    vo_table = "\n".join(vo_rows)

    md = f"""# OWL 商城 · API 契约（DTO / VO 分层）

> 生成时间：由 `docs/tools/gen_api_contract.py` 自动生成，**不要手改**
> 唯一真相：`docs/owl-mall-openapi.json`（由 `docs/tools/gen_openapi.py` 生成）
> 规格书：`docs/OWL商城-后端开发文档.md` v1.1
> 统计：{len(rows)} 个接口 · {sum(1 for k in schemas if k.endswith(('Request', 'DTO')))} 个 DTO · {sum(1 for k in schemas if k.endswith('VO'))} 个 VO

---

## 一、导入 Apifox（3 步）

### 1. 导入文件

Apifox → 打开项目 → 左侧「接口」→ 右上角 `+` → **导入数据** → 选 **OpenAPI/Swagger** → 上传 `docs/owl-mall-openapi.json` → 导入。

导入后会得到：

- 9 个目录（认证 / 商品 / 购物车 / 交易 / 支付 / 个人中心 / 消息 / AI 助手 / 后台）
- {len(rows)} 个接口，每个都带**请求示例**，点「发送」前只需改 `baseUrl`
- 数据模型里 70 个模型，名字前缀 `[DTO 入参]` / `[VO 出参]` 一眼区分

### 2. 建环境

项目设置 → 环境管理 → 新建「本地」：

| 变量 | 值 |
|---|---|
| `baseUrl` | `http://localhost:8080/api/v1` |

把目录的「前置 URL」设为 `{{{{baseUrl}}}}`（导入时已写在 `servers` 里，通常不用改）。

### 3. 登录后自动存 token

打开 `POST /auth/login` → **后置操作** → 添加「提取变量」：

| 项 | 值 |
|---|---|
| 提取来源 | 响应 JSON |
| JSONPath | `$.data.accessToken` |
| 变量名 | `token` |
| 作用域 | 临时变量 |

再给「认证」「购物车」「交易」「支付」「个人中心」「消息」「后台」这些目录设置 **Auth → Bearer Token → `{{{{token}}}}`**。

之后跑完登录接口，其它接口自动带 token，不用手动粘。

> 后台接口用另一套 token：在 `POST /admin/auth/login` 里提取 `$.data.accessToken` 到 `adminToken`，后台目录单独用 `{{{{adminToken}}}}`。

### 覆盖导入的注意点

改了接口后重新生成 JSON，再用「导入 → 覆盖」：

- 选**按 operationId 覆盖**，不要选「新建副本」，否则会重复
- 导入前先提交一次 Apifox 的变更，出问题能回滚
- 手改过的接口描述会被覆盖，所以**字段说明改脚本，不要改 Apifox**

---

## 二、接口清单（{len(rows)} 个）

| # | 方法 | 路径 | 说明 | 鉴权 | 请求 DTO | 响应 VO |
|---|---|---|---|---|---|---|
{api_table}

> `—` 表示无请求体（GET/DELETE）；`null` 表示 data 为空。

---

## 三、DTO 层（入参，{sum(1 for k in schemas if k.endswith(('Request', 'DTO')))} 个）

包路径规则：`com.owl.{{模块}}.dto.{{类名}}`

| 类名 | 字段 | 用途 |
|---|---|---|
{dto_table}

### DTO 写法约定

```java
package com.owl.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {{

    @NotBlank(message = "请输入手机号或邮箱")
    private String account;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    /** 只影响前端存 localStorage 还是 sessionStorage，后端不处理 */
    private Boolean remember = Boolean.TRUE;
}}
```

硬性要求：

1. **只做入参**，不承载业务逻辑，不实现 `Serializable`。
2. 校验注解写在这里，Service 里不再判空（`@Valid` 在 Controller 上触发）。
3. 字段类型用包装类型（`Integer` / `Boolean`），不要用 `int`，否则无法区分"没传"和"传了 0"。
4. 金额字段是 `BigDecimal`（元），接收后立刻用 `PriceUtil.toCent()` 转 `long` 分。
5. 命名：`XxxRequest`（写操作）、`XxxQuery`（查询参数对象）、`XxxDTO`（嵌套结构）。
6. **禁止**把 DTO 传给 Mapper，也禁止把 Entity 当 DTO 用。

---

## 四、VO 层（出参，{sum(1 for k in schemas if k.endswith('VO'))} 个）

包路径规则：`com.owl.{{模块}}.vo.{{类名}}`

| 类名 | 字段 | 说明 |
|---|---|---|
{vo_table}

### VO 写法约定

```java
package com.owl.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录结果")
public class LoginVO {{

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "access 有效期，秒", example = "7200")
    private Integer expiresIn;

    private UserVO user;
}}
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

public final class UserConverter {{

    private UserConverter() {{}}

    public static UserVO toVO(MUser user, int couponCount) {{
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
    }}
}}
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
"""

    with open(OUT, "w", encoding="utf-8") as fp:
        fp.write(md)
    print(f"written: {OUT}")
    print(f"apis={len(rows)} dto={sum(1 for k in schemas if k.endswith(('Request', 'DTO')))} "
          f"vo={sum(1 for k in schemas if k.endswith('VO'))}")


if __name__ == "__main__":
    main()
