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

CREATE TABLE IF NOT EXISTS n_notice (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  title      VARCHAR(120) NOT NULL,
  body       TEXT         NOT NULL,
  type       TINYINT      NOT NULL DEFAULT 1 COMMENT '1 公告 2 活动 3 维护 4 上新',
  pinned     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 普通 1 置顶',
  status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 草稿 1 已发布 2 已下线',
  admin_id   BIGINT       NULL COMMENT '最后操作的管理员',
  publish_at DATETIME     NULL COMMENT '发布时间',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_notice_pub (status, pinned, publish_at),
  KEY idx_notice_type (type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城公告';

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
