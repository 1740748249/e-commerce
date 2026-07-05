# 电商系统后端接口与数据库设计文档

## 项目信息

- 项目名称：ecommerceSystem
- 架构：微服务（Spring Cloud Alibaba + Nacos）
- 网关端口：8080
- 服务端口：8081 ~ 8086

---

## 一、数据库设计

### 1.1 用户服务数据库 `ecommerce_user`

```sql
-- 用户表
CREATE TABLE e_user (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL COMMENT '用户名（登录用）',
    password        VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密密码',
    name            VARCHAR(50)  NOT NULL COMMENT '姓名',
    phone           CHAR(11)     NOT NULL COMMENT '手机号（第二登录凭证）',
    role            TINYINT      NOT NULL DEFAULT 0 COMMENT '角色: 0=普通用户, 1=商家, 2=管理员',
    avatar          VARCHAR(255) COMMENT '头像 URL',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态: 0=禁用, 1=正常, 2=注销',
    last_login_time DATETIME     COMMENT '最近登录时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE INDEX uk_username (username) COMMENT '登录B+Tree唯一索引',
    UNIQUE INDEX uk_phone (phone) COMMENT '手机号B+Tree唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户收货地址表
CREATE TABLE e_user_address (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    receiver_name   VARCHAR(50)  NOT NULL COMMENT '收货人姓名',
    receiver_phone  CHAR(11)     NOT NULL COMMENT '收货人手机号',
    province        VARCHAR(50)  NOT NULL COMMENT '省',
    city            VARCHAR(50)  NOT NULL COMMENT '市',
    district        VARCHAR(50)  NOT NULL COMMENT '区/县',
    detail          VARCHAR(255) NOT NULL COMMENT '详细地址',
    is_default      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认地址: 0=否, 1=是',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_user_updatetime (user_id, update_time DESC) COMMENT '按用户+更新时间倒序，最近更新地址排首位'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址表';
```

### 1.2 商品服务数据库 `ecommerce_product`

```sql
-- 店铺表
CREATE TABLE e_shop (
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL COMMENT '店铺名称',
    logo           VARCHAR(255) COMMENT '店铺 LOGO',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=关闭, 1=营业中',
    owner_id       BIGINT UNSIGNED NOT NULL COMMENT '店主用户ID',
    approved       TINYINT      NOT NULL DEFAULT 0 COMMENT '审批状态: 0=待审批, 1=已通过, 2=已拒绝',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_owner (owner_id) COMMENT '一个用户只能开一家店'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

-- 商品分类表（支持多级分类）
CREATE TABLE e_category (
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    parent_id      BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID（0=顶级分类）',
    name           VARCHAR(50)  NOT NULL COMMENT '分类名称',
    icon           VARCHAR(255) COMMENT '分类图标 URL',
    sort_order     INT          DEFAULT 0 COMMENT '排序',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_parent (parent_id) COMMENT '查询子分类'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 商品 SPU 表（标准产品单元，不直接持有价格和库存）
CREATE TABLE e_product (
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name           VARCHAR(200) NOT NULL COMMENT '商品 SPU 名称（如 iPhone 15）',
    category_id    BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    shop_id        BIGINT UNSIGNED NOT NULL COMMENT '所属店铺ID',
    min_price      INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '最低 SKU 售价（分），列表展示用',
    total_stock    INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总库存（所有 SKU 库存之和）',
    image          VARCHAR(255) COMMENT 'SPU 主图 URL',
    description    TEXT         COMMENT '商品描述',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态: 0=下架, 1=上架',
    sales          INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总销量（所有 SKU 销量之和）',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0,
    INDEX idx_category (category_id),
    INDEX idx_shop (shop_id),
    INDEX idx_sales (sales DESC) COMMENT '热销排行，避免全表扫描+filesort'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品 SPU 表';

-- 商品 SKU 表（库存量单位，价格和库存的持有者）
CREATE TABLE e_product_sku (
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    product_id     BIGINT UNSIGNED NOT NULL COMMENT '所属 SPU ID',
    sku_code       VARCHAR(100) NOT NULL COMMENT 'SKU 编码（商家自定义，如 IP15-128-BLK）',
    specs          JSON         NOT NULL COMMENT '规格属性 [{name:"颜色",value:"深空黑"},{name:"存储",value:"128G"}]',
    price          INT UNSIGNED NOT NULL COMMENT 'SKU 售价（分）',
    stock          INT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'SKU 库存',
    image          VARCHAR(255) COMMENT 'SKU 图片（可覆盖 SPU 主图）',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=启用',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sku_code (sku_code) COMMENT 'SKU 编码唯一',
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品 SKU 表';
-- price 和 stock 都由 SKU 持有，SPU 只存 min_price / total_stock 用于列表展示
-- 查询商品详情时 JOIN e_product_sku WHERE product_id = ? 返回所有 SKU 供用户选择

-- 秒杀场次表（平台定义时间段，管理员统一管理）
CREATE TABLE e_flash_session (
    id           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(50)  NOT NULL COMMENT '场次名称，如"618秒杀专场"',
    start_time   DATETIME     NOT NULL COMMENT '场次开始时间',
    end_time     DATETIME     NOT NULL COMMENT '场次结束时间',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=未开始 1=进行中 2=已结束（查询时动态计算，定时任务同步）',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀场次表';

-- 秒杀活动表（商家报名 → 管理员审核 → 场次时间到达后生效）
CREATE TABLE e_flash_sale (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    session_id      BIGINT UNSIGNED NOT NULL COMMENT '所属场次ID',
    product_id      BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    shop_id         BIGINT UNSIGNED NOT NULL COMMENT '报名商家ID',
    flash_price     INT UNSIGNED NOT NULL COMMENT '秒杀价（分）',
    stock           INT UNSIGNED NOT NULL COMMENT '秒杀库存',
    sold            INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已秒数量',
    per_user_limit  INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    approval_status TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态: 0=待审核, 1=已通过, 2=已拒绝',
    reject_reason   VARCHAR(255) COMMENT '拒绝原因',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_session_product (session_id, product_id) COMMENT '同一商品同一场次仅可报名一次',
    INDEX idx_session_approval (session_id, approval_status) COMMENT '管理员按场次+审核状态查询',
    INDEX idx_shop (shop_id) COMMENT '商家查看自己的报名记录',
    INDEX idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';
-- 秒杀时间跟随场次，不再单独存储 start_time / end_time
-- 秒杀状态由 approval_status + session 时间范围动态判断

-- 秒杀订单记录表（配合 per_user_limit 实现限购）
CREATE TABLE e_flash_sale_order (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '参与秒杀的用户ID',
    flash_sale_id   BIGINT UNSIGNED NOT NULL COMMENT '秒杀活动ID',
    product_id      BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    quantity        INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '购买数量',
    price           INT UNSIGNED NOT NULL COMMENT '秒杀成交价（分）',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=待支付, 1=已支付, 2=已取消, 3=已退款, 4=支付超时',
    pay_time        DATETIME     COMMENT '支付时间',
    cancel_time     DATETIME     COMMENT '取消时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_flash (user_id, flash_sale_id) COMMENT '查询用户在某活动的下单数，配合 per_user_limit 防刷',
    INDEX idx_flash (flash_sale_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单记录表';
```

### 1.3 订单服务数据库 `ecommerce_order`

```sql
-- 购物车表
CREATE TABLE e_cart (
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    product_id     BIGINT UNSIGNED NOT NULL COMMENT '商品SPU ID',
    sku_id         BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'SKU ID（0=无规格的商品）',
    sku_name       VARCHAR(100) COMMENT '规格名称（如"128G 深空黑"）',
    quantity       INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '数量',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, create_time) COMMENT '用户购物车列表查询（按时间排序）',
    UNIQUE KEY uk_user_sku (user_id, product_id, sku_id) COMMENT '同一用户同一商品同一规格仅一条记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 订单表
CREATE TABLE e_order (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '内部自增主键（不对外暴露）',
    order_no        BIGINT UNSIGNED NOT NULL COMMENT '订单号（Snowflake生成，对外展示）',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '下单用户ID',
    shop_id         BIGINT UNSIGNED NOT NULL COMMENT '商家店铺ID',
    total_amount    INT UNSIGNED NOT NULL COMMENT '订单总金额（分）',
    discount_amount INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '优惠金额（分）',
    coupon_id       BIGINT UNSIGNED COMMENT '使用的优惠券ID',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=待支付, 1=已支付, 2=已发货, 3=已完成, 4=已取消, 5=退款中, 6=已退款',
    address_id      BIGINT UNSIGNED NOT NULL COMMENT '收货地址ID（关联 e_user_address）',
    receiver_name   VARCHAR(50)  NOT NULL COMMENT '收货人姓名（快照）',
    receiver_phone  CHAR(11)     NOT NULL COMMENT '收货人手机号（快照）',
    receiver_addr   VARCHAR(255) NOT NULL COMMENT '收货完整地址（快照）',
    remark          VARCHAR(500) COMMENT '备注',
    pay_no          VARCHAR(64)  COMMENT '第三方支付流水号（支付宝 trade_no）',
    pay_time        DATETIME     COMMENT '支付时间（财务对账 + 超时取消定时任务）',
    cancel_time     DATETIME     COMMENT '取消时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_no (order_no) COMMENT '订单号唯一索引',
    INDEX idx_user_status (user_id, status) COMMENT '覆盖"我的订单"按状态筛选',
    INDEX idx_shop_status (shop_id, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单商品明细表（快照）
CREATE TABLE e_order_item (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    product_id      BIGINT UNSIGNED NOT NULL COMMENT '商品SPU ID',
    sku_id          BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'SKU ID（0=无规格）',
    sku_name        VARCHAR(100) COMMENT '规格名称快照（如"128G 深空黑"）',
    product_name    VARCHAR(200) NOT NULL COMMENT '商品名称快照',
    product_image   VARCHAR(255) COMMENT '商品图片快照',
    price           INT UNSIGNED NOT NULL COMMENT '成交单价（分）',
    quantity        INT UNSIGNED NOT NULL COMMENT '数量',
    shop_id         BIGINT UNSIGNED NOT NULL COMMENT '店铺ID',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品明细表（快照）';

-- 优惠券模板表
CREATE TABLE e_coupon (
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type           TINYINT UNSIGNED NOT NULL COMMENT '类型: 0=满减券, 1=无门槛券',
    threshold      INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '使用门槛（分），0 表示无门槛',
    reduce         INT UNSIGNED NOT NULL COMMENT '减免金额（分）',
    category_id    BIGINT UNSIGNED COMMENT '限制品类ID，NULL 表示全场通用',
    valid_days     INT UNSIGNED NOT NULL DEFAULT 7 COMMENT '有效天数',
    limit_per_user INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '每人限领数量',
    total_count    INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '总发行量',
    claimed_count  INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已领取量（乐观锁防超发）',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态: 0=停用, 1=启用',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

-- 用户优惠券表
CREATE TABLE e_user_coupon (
    id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    coupon_id      BIGINT UNSIGNED NOT NULL COMMENT '优惠券ID',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=未使用, 1=已使用, 2=已过期',
    claimed_at     DATETIME     NOT NULL COMMENT '领取时间',
    expire_at      DATETIME     NOT NULL COMMENT '过期时间',
    used_at        DATETIME     COMMENT '使用时间',
    order_no       BIGINT UNSIGNED COMMENT '使用的订单号（关联 e_order.order_no）',
    INDEX idx_user_status (user_id, status) COMMENT '覆盖"我的优惠券"按状态筛选',
    INDEX idx_coupon (coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

-- 支付流水记录表
CREATE TABLE e_payment_record (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    pay_no          VARCHAR(64)  NOT NULL COMMENT '支付宝交易号（唯一）',
    order_no        BIGINT UNSIGNED NOT NULL COMMENT '关联订单号（e_order.order_no）',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '支付用户ID',
    total_amount    INT UNSIGNED NOT NULL COMMENT '支付金额（分）',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=待支付, 1=支付成功, 2=已关闭',
    pay_time        DATETIME     COMMENT '支付成功时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pay_no (pay_no),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水记录表';

-- 退款流水记录表
CREATE TABLE e_refund_record (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    refund_no       VARCHAR(64)  NOT NULL COMMENT '支付宝退款交易号',
    out_request_no  VARCHAR(64)  NOT NULL COMMENT '商户退款请求号（雪花ID，幂等关键）',
    pay_no          VARCHAR(64)  NOT NULL COMMENT '关联支付流水号',
    order_no        BIGINT UNSIGNED NOT NULL COMMENT '关联订单号（e_order.order_no）',
    refund_amount   INT UNSIGNED NOT NULL COMMENT '退款金额（分）',
    reason          VARCHAR(256) COMMENT '退款原因',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=处理中, 1=退款成功, 2=退款失败',
    refund_time     DATETIME     COMMENT '退款到账时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refund_no (refund_no),
    UNIQUE KEY uk_out_request_no (out_request_no),
    INDEX idx_pay_no (pay_no),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款流水记录表';
```

### 1.4 通知服务数据库 `ecommerce_notification`

```sql
-- 通知表（不含 is_read，已读状态统一走 e_notification_read）
CREATE TABLE e_notification (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    shop_id        BIGINT       NOT NULL COMMENT '接收通知的店铺ID, 0=全站广播',
    type           TINYINT      NOT NULL COMMENT '类型: 0=新订单, 1=系统通知, 2=促销活动',
    title          VARCHAR(200) NOT NULL COMMENT '通知标题',
    content        TEXT         NOT NULL COMMENT '通知内容',
    order_id       BIGINT       COMMENT '关联订单ID',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 通知已读记录表（所有已读状态统一存此处，按店铺隔离）
-- 普通通知和广播的已读都在此表，各店铺互不影响
CREATE TABLE e_notification_read (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id  BIGINT   NOT NULL COMMENT '通知ID, FK → e_notification.id',
    shop_id          BIGINT   NOT NULL COMMENT '店铺ID',
    is_read          TINYINT  NOT NULL DEFAULT 1 COMMENT '0=未读, 1=已读',
    read_time        DATETIME DEFAULT NULL COMMENT '已读时间',
    UNIQUE KEY uk_notif_shop (notification_id, shop_id),
    INDEX idx_shop (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知已读记录表';
```

---

## 二、API 接口设计

> 所有接口前缀经过 Gateway 剥离一层路径后转发到对应服务。  
> 响应格式统一为 `R<T>`：`{ code: 200, message: "ok", data: T, requestId: "..." }`

---

### 2.0 网关鉴权（Gateway Auth）

JWT 在登录时签发，payload 包含 `user`（用户ID）和 `role`（角色值），RSA256 签名。

| 角色值 | 含义 | 说明 |
|--------|------|------|
| 0 | USER（普通用户） | 默认角色 |
| 1 | VENDOR（商家） | 开店审批通过后自动变更 |
| 2 | ADMIN（管理员） | 后台管理 |

**白名单路径**（无需 Token，仅允许 GET/OPTIONS 或 login/register POST）：
```
/users/login, /users/register, /admin/login
/products, /categories, /shops, /shops/*/products
/flash-sales, /flash-sales/*/result, /ranking
/files/**, /payment/notify, /payment/return
```

**角色保护路径**（需 Token + 指定角色才可访问）：

| 路径模式 | 允许角色 | 说明 |
|----------|----------|------|
| `/admin/**` | `[2]` | 所有管理员接口 |
| `/notifications/**` | `[1, 2]` | 通知接口（商家 + 管理员） |

未匹配 `rolePaths` 的路径仅需登录（有效 Token），不限角色。请求被拒绝时返回 `401`（未认证）或 `403`（无权限）。

---

### 2.1 用户服务 `user-service`（8081）

#### 2.1.1 用户注册
```
POST /users/register
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 登录用户名，4-20位 |
| password | string | 是 | 密码（6-20位） |
| name | string | 是 | 姓名 |
| phone | string | 否 | 手机号（11位，唯一，用于登录/找回密码） |
| avatar | string | 否 | 头像 URL（已通过文件服务上传后获得的地址） |

> 注册仅创建普通用户账号（role=0，status=1）。如需开店，登录后通过「申请开店」功能提交店铺申请，由管理员审批。

响应：`R<UserVO>` — `{ id, username, name, phone, role, avatar, status, lastLoginTime }`

**响应示例：**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 5,
    "username": "new_user",
    "name": "新用户",
    "phone": "13800138005",
    "role": 0,
    "avatar": null,
    "status": 1,
    "lastLoginTime": null
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.2 用户登录
```
POST /users/login
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

响应：`R<LoginVO>` — 包含 JWT token + 用户信息

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjoxLCJleHAiOjE3MTg2MDAwMDB9.signature",
    "user": {
      "id": 1,
      "username": "zhangsan",
      "name": "张三",
      "phone": "13800138001",
      "role": 0,
      "shopId": null,
      "shopName": null,
      "avatar": "https://cdn.example.com/avatar/1.jpg",
      "logo": null,
      "status": 1,
      "lastLoginTime": "2026-06-17 12:00:00"
    }
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.3 获取当前用户信息
```
GET /users/me
```
Header：`Authorization: Bearer <token>`

响应：`R<UserVO>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "name": "张三",
    "phone": "13800138001",
    "role": 0,
    "avatar": "https://cdn.example.com/avatar/1.jpg",
    "status": 1,
    "lastLoginTime": "2026-06-17 12:00:00"
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.4 修改个人信息
```
PUT /users/me
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 姓名 |
| phone | string | 否 | 手机号（11位，唯一） |
| avatar | string | 否 | 头像 URL（已通过文件服务上传后获得的地址） |
| logo | string | 否 | 店铺 LOGO URL（仅 role=1 商家可修改，通过文件服务上传后获得的地址） |

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "个人信息已更新",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.5 修改密码
```
PUT /users/password
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| oldPassword | string | 是 | 旧密码 |
| newPassword | string | 是 | 新密码（6-20位） |

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.6 获取收货地址列表
```
GET /addresses
```
响应：`R<List<AddressVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "receiverName": "张三",
      "receiverPhone": "13800138001",
      "province": "北京市",
      "city": "北京市",
      "district": "朝阳区",
      "detail": "望京SOHO T1 1208室",
      "isDefault": 1
    },
    {
      "id": 2,
      "receiverName": "张三",
      "receiverPhone": "13800138001",
      "province": "上海市",
      "city": "上海市",
      "district": "浦东新区",
      "detail": "张江高科技园区 15号楼 301",
      "isDefault": 0
    }
  ],
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.7 获取单个收货地址
```
GET /addresses/{id}
```
响应：`R<AddressVO>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "id": 1,
    "receiverName": "张三",
    "receiverPhone": "13800138001",
    "province": "北京市",
    "city": "北京市",
    "district": "朝阳区",
    "detail": "望京SOHO T1 1208室",
    "isDefault": 1
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.8 新增收货地址
```
POST /addresses
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| receiverName | string | 是 | 收货人姓名 |
| receiverPhone | string | 是 | 收货人手机号（11位） |
| province | string | 是 | 省 |
| city | string | 是 | 市 |
| district | string | 是 | 区/县 |
| detail | string | 是 | 详细地址 |
| isDefault | int | 否 | 是否默认: 0=否, 1=是 |

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "地址添加成功",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.9 修改收货地址
```
PUT /addresses/{id}
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| receiverName | string | 否 | 收货人姓名 |
| receiverPhone | string | 否 | 收货人手机号 |
| province | string | 否 | 省 |
| city | string | 否 | 市 |
| district | string | 否 | 区/县 |
| detail | string | 否 | 详细地址 |
| isDefault | int | 否 | 是否默认 |

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "地址已更新",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.10 删除收货地址
```
DELETE /addresses/{id}
```
响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.10 申请开店

```
POST /shops/apply
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 店铺名称 |
| logo | string | 否 | 店铺 LOGO URL（已通过文件服务上传后获得的地址） |

> 已登录用户（role=0）可申请开店。系统创建店铺记录（`approved=0` 待审批），提交后等待管理员审批。审批通过后用户 role 自动变更为 1（商家）。

响应：`R<Void>` — `{ shopId, name, logo, approved, approvedText }`

---

#### 2.1.11 获取当前用户店铺信息

```
GET /shops/me
```

> 通过 `e_shop.owner_id = 当前用户ID` 查询店铺，返回 `null` 表示该用户没有店铺。用于前端判断是否显示「申请开店」入口。

响应：`R<ShopVO>` — 无店铺时 `data` 为 `null`

**有店铺（已审批）响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "id": 5,
    "name": "王五的数码店",
    "logo": "https://cdn.example.com/logo/5.png",
    "status": 1,
    "approved": 1
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.12 管理员 - 登录

```
POST /admin/login
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 管理员用户名 |
| password | string | 是 | 密码 |

> 仅 `role=2`（管理员）可登录。服务层校验：非管理员角色返回 `"无管理员权限"` 错误，已禁用账号返回 `"用户不存在或账号已被禁用"`。

响应：`R<LoginVO>` — 包含 JWT token + 用户信息

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "token": "eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjoyLCJleHAiOjE3MTg2MDAwMDB9.signature",
    "user": {
      "id": 2,
      "username": "admin",
      "name": "管理员",
      "phone": "13800000000",
      "role": 2,
      "avatar": null,
      "status": 1,
      "lastLoginTime": "2026-06-17 09:00:00"
    }
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.13 管理员 - 获取用户列表
```
GET /admin/users?page=1&size=20&status=&role=&keyword=&startTime=&endTime=&isAsc=true&sortBy=
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认20 |
| isAsc | boolean | 否 | 是否升序，默认true |
| sortBy | string | 否 | 排序字段 |
| status | int | 否 | 状态筛选：0=禁用, 1=正常 |
| role | int | 否 | 角色筛选：0=普通用户, 1=商家, 2=管理员 |
| keyword | string | 否 | 搜索关键词（模糊匹配用户名/姓名/手机号） |
| startTime | datetime | 否 | 注册时间起始（ISO格式，如 `2026-06-01T00:00:00`） |
| endTime | datetime | 否 | 注册时间截止 |

响应：`R<PageDTO<UserVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 19,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "id": 1,
        "username": "zhangsan",
        "name": "张三",
        "phone": "13800138001",
        "role": 0,
        "avatar": "https://cdn.example.com/avatar/1.jpg",
        "status": 1,
        "lastLoginTime": "2026-06-17 12:00:00"
      },
      {
        "id": 2,
        "username": "shop_owner_1",
        "name": "李四",
        "phone": "13800138002",
        "role": 1,
        "avatar": null,
        "status": 1,
        "lastLoginTime": "2026-06-16 10:30:00"
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.14 管理员 - 禁用/启用用户
```
PUT /admin/users/{id}/status
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 是 | 0=禁用, 1=正常 |

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.1.15 管理员 - 获取用户统计概览

```
GET /admin/statistics
```

> 返回仪表盘核心数据。其中 `pendingMerchants` 统计 `e_shop.approved = 0` 的待审批店铺数。

响应：`R<AdminStatisticsVO>` — `{ totalUsers, totalMerchants, pendingMerchants, totalOrders, totalSales }`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "totalUsers": 126,
    "totalMerchants": 9,
    "pendingMerchants": 3,
    "totalOrders": 1580,
    "totalSales": 28650000
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

### 2.2 商品服务 `product-service`（8082）

#### 2.2.1 获取商品列表
```
GET /products?page=1&size=20&keyword=&categoryId=&sort=default
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认20 |
| keyword | string | 否 | 搜索关键词 |
| categoryId | long | 否 | 分类ID |
| sort | string | 否 | default / price_asc / price_desc / sales |

响应：`R<PageDTO<ProductVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 45,
    "page": 1,
    "size": 20,
    "pages": 3,
    "records": [
      {
        "id": 1,
        "name": "iPhone 15",
        "categoryId": 1,
        "categoryName": "手机数码",
        "shopId": 1,
        "shopName": "李四的店铺",
        "minPrice": 699900,
        "totalStock": 320,
        "image": "https://cdn.example.com/product/1.jpg",
        "description": "Apple iPhone 15 256GB",
        "status": 1,
        "sales": 1280
      },
      {
        "id": 2,
        "name": "Nike Air Jordan 1",
        "categoryId": 3,
        "categoryName": "运动鞋服",
        "shopId": 1,
        "shopName": "李四的店铺",
        "minPrice": 129900,
        "totalStock": 150,
        "image": "https://cdn.example.com/product/2.jpg",
        "description": "经典复刻，潮流百搭",
        "status": 1,
        "sales": 560
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.2 获取商品详情
```
GET /products/{id}
```
响应：`R<ProductDetailVO>`（含 SKU 列表）

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "id": 1,
    "name": "iPhone 15",
    "categoryId": 1,
    "categoryName": "手机数码",
    "shopId": 1,
    "shopName": "李四的店铺",
    "minPrice": 699900,
    "totalStock": 320,
    "image": "https://cdn.example.com/product/1.jpg",
    "description": "Apple iPhone 15，A16 芯片，4800 万像素主摄",
    "status": 1,
    "sales": 1280,
    "skus": [
      {
        "id": 101,
        "productId": 1,
        "skuCode": "IP15-128-BLK",
        "specs": [
          { "name": "颜色", "value": "黑色" },
          { "name": "存储", "value": "128GB" }
        ],
        "price": 699900,
        "stock": 120,
        "image": null,
        "status": 1
      },
      {
        "id": 102,
        "productId": 1,
        "skuCode": "IP15-256-WHT",
        "specs": [
          { "name": "颜色", "value": "白色" },
          { "name": "存储", "value": "256GB" }
        ],
        "price": 799900,
        "stock": 80,
        "image": null,
        "status": 1
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.3 获取全部分类
```
GET /categories
```
响应：`R<List<CategoryVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    { "id": 1, "name": "手机数码", "icon": "📱" },
    { "id": 2, "name": "电脑办公", "icon": "💻" },
    { "id": 3, "name": "运动鞋服", "icon": "👟" },
    { "id": 4, "name": "家用电器", "icon": "🏠" },
    { "id": 5, "name": "食品生鲜", "icon": "🍎" }
  ],
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.4 获取全部店铺
```
GET /shops
```
响应：`R<List<ShopVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    { "id": 1, "name": "李四的店铺", "logo": "https://cdn.example.com/logo/1.png", "status": 1 },
    { "id": 2, "name": "数码旗舰店", "logo": "https://cdn.example.com/logo/2.png", "status": 1 },
    { "id": 3, "name": "潮流服饰馆", "logo": null, "status": 1 }
  ],
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.5 商家 - 发布商品
```
POST /products
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 商品 SPU 名称 |
| categoryId | long | 是 | 分类ID |
| image | string | 否 | SPU 主图 URL（通过文件服务上传后获得的地址，type=product） |
| description | string | 否 | 商品描述 |
| skus | array | 否 | SKU 列表（无规格商品也至少传一个，价格/库存在此） |
| skus[].skuCode | string | 否 | 商家自定义 SKU 编码 |
| skus[].specs | array | 否 | 规格属性，如 `[{name:"颜色",value:"黑色"}, {name:"存储",value:"128G"}]` |
| skus[].price | int | 是 | SKU 售价（分） |
| skus[].stock | int | 是 | 库存 |
| skus[].image | string | 否 | SKU 图片 URL（通过文件服务上传后获得的地址，type=product） |

> 无规格商品只需传一个 SKU，specs=[]，price 和 stock 填上即可

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "商品发布成功",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.6 商家 - 编辑商品
```
PUT /products/{id}
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | SPU 名称 |
| categoryId | long | 否 | 分类ID |
| image | string | 否 | SPU 主图 URL（通过文件服务上传后获得的地址，type=product） |
| description | string | 否 | 商品描述 |
| skus | array | 否 | SKU 列表（全量替换，传入完整列表） |
| skus[].id | long | 否 | SKU ID（已有 SKU 传原 ID 做更新，不传或为 0 则新增） |
| skus[].skuCode | string | 否 | 商家自定义 SKU 编码 |
| skus[].specs | array | 否 | 规格属性，如 `[{name:"颜色",value:"黑色"}]` |
| skus[].price | int | 是 | SKU 售价（分） |
| skus[].stock | int | 是 | 库存 |
| skus[].image | string | 否 | SKU 图片 URL（通过文件服务上传后获得的地址，type=product） |

> **SKU 全量替换逻辑：** 传入完整 SKU 列表，带 `id` 的为更新，不带 `id`（或 `id=0`）的为新增，数据库有但列表中没有的 SKU 会被删除（无关联订单时才可删）。

**请求示例：**
```json
{
  "name": "iPhone 15",
  "categoryId": 1,
  "image": "https://cdn.example.com/product/1.jpg",
  "description": "Apple iPhone 15，A16 芯片",
  "skus": [
    {
      "id": 101,
      "skuCode": "IP15-128-BLK",
      "specs": [{ "name": "颜色", "value": "黑色" }, { "name": "存储", "value": "128GB" }],
      "price": 699900,
      "stock": 120
    },
    {
      "skuCode": "IP15-512-WHT",
      "specs": [{ "name": "颜色", "value": "白色" }, { "name": "存储", "value": "512GB" }],
      "price": 999900,
      "stock": 50
    }
  ]
}
```

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "商品更新成功",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---
#### 2.2.6.1 商家 - 管理 SKU ~~（已废弃，建议直接使用编辑接口全量替换）~~

```
POST   /products/{id}/skus         新增 SKU
PUT    /products/{id}/skus/{skuId} 修改 SKU（价格/库存/规格/状态/图片）
DELETE /products/{id}/skus/{skuId} 删除 SKU（无关联订单时才可删）
```

**新增/修改 SKU 参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| skuCode | string | 否 | 商家自定义 SKU 编码 |
| specs | array | 否 | 规格属性 |
| price | int | 是 | SKU 售价（分） |
| stock | int | 是 | 库存 |
| image | string | 否 | SKU 图片 URL（通过文件服务上传后获得的地址，type=product） |
| status | int | 否 | 状态: 0=禁用, 1=启用 |

> 保留兼容旧代码，新开发建议使用 `2.2.6 编辑商品` 接口的 `skus` 字段做全量替换。

---

#### 2.2.7 商家 - 上下架商品
```
PUT /products/{id}/status
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 是 | 0=下架, 1=上架 |

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "商品状态更新成功",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.8 商家 - 获取自己的商品列表
```
GET /products/my?page=1&size=20
```
响应：`R<PageDTO<ProductVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 8,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "id": 1,
        "name": "iPhone 15",
        "categoryId": 1,
        "categoryName": "手机数码",
        "shopId": 1,
        "shopName": "李四的店铺",
        "minPrice": 699900,
        "totalStock": 320,
        "image": "https://cdn.example.com/product/1.jpg",
        "description": "Apple iPhone 15 256GB",
        "status": 1,
        "sales": 1280,
        "skus": [
          {
            "id": 101,
            "productId": 1,
            "skuCode": "IP15-128-BLK",
            "specs": [
              { "name": "颜色", "value": "黑色" },
              { "name": "存储", "value": "128GB" }
            ],
            "price": 699900,
            "stock": 120,
            "image": null,
            "status": 1
          },
          {
            "id": 102,
            "productId": 1,
            "skuCode": "IP15-256-WHT",
            "specs": [
              { "name": "颜色", "value": "白色" },
              { "name": "存储", "value": "256GB" }
            ],
            "price": 799900,
            "stock": 80,
            "image": null,
            "status": 1
          }
        ]
      },
      {
        "id": 2,
        "name": "Nike Air Jordan 1",
        "categoryId": 3,
        "categoryName": "运动鞋服",
        "shopId": 1,
        "shopName": "李四的店铺",
        "minPrice": 129900,
        "totalStock": 150,
        "image": "https://cdn.example.com/product/2.jpg",
        "description": "经典复刻，潮流百搭",
        "status": 0,
        "sales": 560,
        "skus": []
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.9 获取店铺商品列表（公开）
```
GET /shops/{shopId}/products?page=1&size=20&sort=default
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认20 |
| sort | string | 否 | default / price_asc / price_desc / sales |

> 前端「进店」功能使用，返回指定店铺的所有上架商品

响应：`R<PageDTO<ProductVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 3,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "id": 1,
        "name": "iPhone 15",
        "categoryId": 1,
        "categoryName": "手机数码",
        "shopId": 1,
        "shopName": "李四的店铺",
        "minPrice": 699900,
        "totalStock": 320,
        "image": "https://cdn.example.com/product/1.jpg",
        "description": "Apple iPhone 15 256GB",
        "status": 1,
        "sales": 1280
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.10 获取秒杀商品列表（公开）
```
GET /flash-sales?sessionId=
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | long | 否 | 场次ID，不传则返回当前活跃场次的秒杀商品 |

> 只返回 `approval_status=1`（已通过）的商品。秒杀时间跟随场次，状态由场次时间动态判断。

响应：`R<FlashSaleVO>`

```java
// FlashSaleVO（外层：场次信息）
{ id, name, startTime, endTime,
  items: [{
    // FlashSaleItemVO（内层：秒杀商品条目）
    id, productId, productName, productImage,
    originalPrice, flashPrice, stock, sold,
    shopId, shopName, progress, perUserLimit
  }] }
// progress = sold / (sold + stock) × 100
```

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "id": 1,
    "name": "618秒杀专场",
    "startTime": "2026-06-18 10:00:00",
    "endTime": "2026-06-18 12:00:00",
    "items": [
      {
        "id": 1,
        "productId": 5,
        "productName": "AirPods Pro 2",
        "productImage": "https://cdn.example.com/product/5.jpg",
        "originalPrice": 199900,
        "flashPrice": 99900,
        "stock": 100,
        "sold": 67,
        "shopId": 10,
        "shopName": "数码旗舰店",
        "progress": 40,
        "perUserLimit": 2
      },
      {
        "id": 2,
        "productId": 8,
        "productName": "Nike Dunk Low",
        "productImage": "https://cdn.example.com/product/8.jpg",
        "originalPrice": 89900,
        "flashPrice": 39900,
        "stock": 50,
        "sold": 50,
        "shopId": 12,
        "shopName": "潮流服饰馆",
        "progress": 100,
        "perUserLimit": 1
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.11 商家 - 报名秒杀

> ⚠️ **此节内容已迁移至 [2.2.15](#2215-商家---报名秒杀)**。正确路径为 `POST /merchant/flash-sales`，非 `POST /flash-sales`。

---

#### 2.2.12 用户 - 秒杀下单  ⚠️ 高并发核心接口

```
POST /flash-sales/{id}/order
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| quantity | int | 否 | 购买数量（默认1） |

**处理流程：** Redis Lua 原子扣减 → INSERT `e_flash_sale_order` → MQ 异步通知 `order-service` 创建正式订单 → 返回秒杀结果。详见下方 [秒杀技术方案详解](#-秒杀技术方案详解)。

**响应：** `R<FlashSaleOrderVO>`（成功 200 / 库存不足 410 / 超出限购 409 / 频繁 429）

**秒杀成功响应示例：**
```json
{
  "code": 200,
  "message": "秒杀成功",
  "data": {
    "id": 88,
    "flashSaleId": 1,
    "productName": "AirPods Pro 2",
    "quantity": 1,
    "price": 99900,
    "status": 0,
    "statusText": "待支付",
    "payTime": null,
    "cancelTime": null,
    "createTime": "2026-06-17 10:00:01"
  }
}
```

**秒杀失败响应示例：**
```json
// 已售罄 (410)
{ "code": 410, "message": "秒杀库存已耗尽", "data": null }

// 超出限购 (409)
{ "code": 409, "message": "已超出该商品每人限购数量", "data": null }

// 请求过频 (429)
{ "code": 429, "message": "操作太频繁，请稍后再试", "data": null }
```

---

##### 2.2.12.1 秒杀结果查询

```
GET /flash-sales/{id}/result
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | long | 是 | 秒杀活动ID（路径参数） |

**处理流程：** 查询 `e_flash_sale_order` 中当前用户对该秒杀商品的订单记录。若存在则返回订单详情（待支付/已支付/超时），若不存在则检查 Redis 库存：库存 > 0 返回"可抢"，库存 = 0 返回"已售罄"。

**响应：** `R<FlashSaleOrderVO>`

**响应示例（已抢到）：**
```json
{
  "code": 200,
  "data": {
    "id": 88,
    "flashSaleId": 1,
    "productName": "AirPods Pro 2",
    "quantity": 1,
    "price": 99900,
    "status": 0,
    "statusText": "待支付",
    "createTime": "2026-06-17 10:00:01"
  }
}
```

**响应示例（未参与 / 可抢）：**
```json
{ "code": 200, "data": null }
```

---

### 🔥 秒杀技术方案详解

秒杀是电商系统并发最高的场景，核心矛盾是**库存准确不超卖 + 防刷 + 高性能**。

---

#### 架构层次

```
用户请求 → Gateway(令牌桶限流) → product-service
                                       │
                                    ┌──▼──────────────────┐
                                    │ Redis Lua 原子扣减    │  ← 核心：单线程原子操作
                                    │ stock > 0 ? DECR : 0 │
                                    └──┬──────────────────┘
                                       │ 扣减成功
                                       ▼
                                    INSERT 秒杀订单记录
                                    (应用层校验 per_user_limit)
                                       │
                                       ▼
                                    MQ → order-service
                                         异步创建正式订单
```

---

#### 1. Gateway 层：令牌桶限流

```yaml
# Nacos 动态配置
ecommerce:
  gateway:
    rate-limit:
      flash-sale:
        replenishRate: 5000      # 每秒允许 5000 个请求通过
        burstCapacity: 10000     # 突发容量 10000
```

超过限流的请求直接返回 `429 Too Many Requests`，不进入业务层。

---

#### 2. Redis Lua 脚本：原子校验 + 扣减

```lua
-- flash_deduct.lua
-- KEYS[1] = flash:stock:{flashSaleId}     (秒杀库存缓存，提前预热)
-- KEYS[2] = flash:user_count:{flashSaleId} (用户购买次数 HASH，field=userId, value=已购数量)
-- ARGV[1] = userId
-- ARGV[2] = quantity
-- ARGV[3] = perUserLimit
-- 返回值: 1=成功, 0=库存不足, -1=超出限购

-- 检查用户已购数量是否超出限购
local bought = redis.call('HGET', KEYS[2], ARGV[1])
bought = bought and tonumber(bought) or 0
if bought + tonumber(ARGV[2]) > tonumber(ARGV[3]) then
    return -1
end

-- 原子扣减库存
local stock = redis.call('DECRBY', KEYS[1], ARGV[2])
if stock < 0 then
    -- 库存不足，回滚
    redis.call('INCRBY', KEYS[1], ARGV[2])
    return 0
end

-- 累加用户购买次数
redis.call('HINCRBY', KEYS[2], ARGV[1], ARGV[2])
return 1
```

---

#### 3. 缓存预热

秒杀场次开始前 5 分钟，定时任务扫描该场次下所有已通过的秒杀商品，将库存加载到 Redis：

```java
// 在 session.startTime 前 5 分钟触发
void warmUpFlashSession(Long sessionId) {
    // 查询该场次下所有 approval_status=1 的秒杀商品
    List<EFlashSale> items = flashSaleMapper.selectList(
        new LambdaQueryWrapper<EFlashSale>()
            .eq(EFlashSale::getSessionId, sessionId)
            .eq(EFlashSale::getApprovalStatus, 1)
    );
    for (EFlashSale item : items) {
        String stockKey   = "flash:stock:" + item.getId();
        String userCountKey = "flash:user_count:" + item.getId();
        redisTemplate.opsForValue().set(stockKey, item.getStock());
        redisTemplate.delete(userCountKey);  // 清空上一轮的用户购买计数
        
        // 设置过期时间：场次结束后 1 小时
        FlashSession session = flashSessionMapper.selectById(sessionId);
        long ttl = ChronoUnit.SECONDS.between(now(), session.getEndTime()) + 3600;
        redisTemplate.expire(stockKey, Duration.ofSeconds(ttl));
    }
}
```

---

#### 4. Lua 脚本返回后的处理

| Lua 返回值 | 含义 | 后端操作 | HTTP 状态码 |
|-----------|------|---------|------------|
| `1` | 扣减成功 | INSERT 秒杀订单记录（status=0 待支付） | 200 |
| `0` | 库存不足 | 返回错误 | 410 Gone |
| `-1` | 超出限购 | 返回错误（若用户之前订单已取消/超时，额度已释放，可重新参与） | 409 Conflict |

---

#### 5. 异步创建正式订单（product-service ↔ order-service 联动）

秒杀 Lua 扣减成功后，`product-service` 写入 `e_flash_sale_order`（status=0 待支付），然后通过 MQ 异步通知 `order-service` 创建正式订单，解耦秒杀服务和订单服务，避免秒杀接口阻塞在订单创建上。

**联动时序：**

```
用户点击抢购
     │
     ▼
product-service (EFlashSaleServiceImpl.order)
     │
     ├─① Redis Lua 原子扣减库存 + 校验限购
     │    返回值: 1=成功 / 0=库存不足 / -1=超出限购
     │
     ├─② INSERT e_flash_sale_order (status=0 待支付)
     │
     ├─③ 发送 MQ 消息 ──────────────────────────────────┐
     │    topic: flash-sale-order                       │
     │    body: { flashSaleOrderId, userId, productId,  │
     │            quantity, price, shopId, ... }         │
     │                                                   ▼
     │                                          order-service (MQ Consumer)
     │                                                   │
     │                                          ├─④ 幂等校验（基于 flashSaleOrderId 去重）
     │                                          ├─⑤ INSERT e_order (status=0 待支付)
     │                                          ├─⑥ INSERT e_order_item
     │                                          └─⑦ 扣减 e_product_sku 真实库存
     │
     └─⑧ 返回 R.ok(FlashSaleOrderVO) 给用户
```

**MQ 消息体定义：**

```json
{
  "flashSaleOrderId": 88,
  "userId": 1001,
  "productId": 5001,
  "skuId": 7001,
  "quantity": 1,
  "price": 99900,
  "shopId": 2001,
  "addressId": 3001,
  "messageId": "uuid-for-idempotent"
}
```

**order-service 消费端关键逻辑：**

```java
@RabbitListener(queues = "flash-sale-order.queue")
public void handleFlashSaleOrder(FlashSaleOrderMessage msg) {
    // 1. 幂等校验：基于 messageId / flashSaleOrderId 判断是否已处理
    String idempotentKey = "order:flash:" + msg.getFlashSaleOrderId();
    Boolean exists = redisTemplate.opsForValue()
            .setIfAbsent(idempotentKey, "1", Duration.ofHours(24));
    if (Boolean.FALSE.equals(exists)) return;  // 已处理，跳过
    
    // 2. 创建正式订单
    EOrder order = buildOrder(msg);
    orderMapper.insert(order);
    
    // 3. 创建订单明细
    EOrderItem item = buildOrderItem(msg, order.getId());
    orderItemMapper.insert(item);
    
    // 4. 扣减 SKU 真实库存
    productSkuMapper.deductStock(msg.getSkuId(), msg.getQuantity());
}
```

**失败重试与补偿：**

| 场景 | 处理方式 |
|------|---------|
| MQ 发送失败 | `product-service` 本地事务表 + 定时任务补发 |
| order-service 消费失败 | RabbitMQ 死信队列，最多重试 3 次 |
| 重试全部失败 | 写入 `e_compensate_task` 表，人工处理 |
| order-service 宕机 | MQ 消息持久化，重启后继续消费 |

**支付回调联动：**

用户支付成功后，`order-service` 回调通知 `product-service`：

```
POST /orders/{orderNo}/pay-callback
→ order-service 更新 e_order.status = 1（已支付）
→ order-service 通过 MQ 通知 product-service
→ product-service 更新 e_flash_sale_order.status = 1（已支付）
→ product-service 更新 e_flash_sale.sold += quantity、e_product.sales += quantity
```

---

#### 6. 超时未支付处理

XXL-Job 定时任务每 30 秒扫描 `e_flash_sale_order` 表中 `status=0 AND create_time < NOW() - 15分钟` 的记录：

```java
// 1. 批量更新 status → 4 (支付超时)
// 2. Redis 回补库存：INCRBY flash:stock:{flashSaleId}, quantity
// 3. HDECRBY 减少用户购买次数（释放限购额度，允许再次参与）
```

---

#### 7. 数据库层面的防护

```sql
-- idx_user_flash：查询用户在某活动下的已下单数，与 per_user_limit 对比防止超限
INDEX idx_user_flash (user_id, flash_sale_id)
```

下单时先 `SELECT COUNT(*) FROM e_flash_sale_order WHERE user_id=? AND flash_sale_id=?`，与 `per_user_limit` 比较，超出则拒绝。后续可升级为 Redis 计数器 + Lua 原子操作，进一步提升性能。

**为什么不在 Redis 扣减后立刻插入 DB？** 因为 INSERT 需要 ~10ms，而 Redis Lua 只需要 ~1ms。高并发下先把库存扣了，DB 插入异步/后置，吞吐量提升 10 倍以上。

---

#### 2.2.13 管理员 - 秒杀场次管理

##### 2.2.13.1 创建场次
```
POST /admin/flash-sessions
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 场次名称 |
| startTime | datetime | 是 | 开始时间 |
| endTime | datetime | 是 | 结束时间 |

响应：`R<FlashSessionVO>` — 返回创建的场次对象（含 id、status 等）

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "id": 5,
    "name": "618秒杀专场",
    "startTime": "2026-06-18 10:00:00",
    "endTime": "2026-06-18 12:00:00",
    "status": 0,
    "statusText": "未开始",
    "itemCount": 0
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

##### 2.2.13.2 查询场次列表
```
GET /admin/flash-sessions?page=1&size=10
```
响应：`R<PageDTO<FlashSessionVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 3,
    "page": 1,
    "size": 10,
    "pages": 1,
    "records": [
      {
        "id": 5,
        "name": "618秒杀专场",
        "startTime": "2026-06-18 10:00:00",
        "endTime": "2026-06-18 12:00:00",
        "status": 2,
        "statusText": "已结束",
        "itemCount": 8
      },
      {
        "id": 6,
        "name": "周末特惠场",
        "startTime": "2026-06-20 14:00:00",
        "endTime": "2026-06-20 18:00:00",
        "status": 0,
        "statusText": "未开始",
        "itemCount": 3
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

##### 2.2.13.3 编辑场次
```
PUT /admin/flash-sessions/{id}
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 场次名称 |
| startTime | datetime | 否 | 开始时间 |
| endTime | datetime | 否 | 结束时间 |

> 仅"未开始"状态的场次可编辑，进行中/已结束的场次不可修改时间。

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "场次已更新",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

##### 2.2.13.4 删除场次
```
DELETE /admin/flash-sessions/{id}
```
> 仅"未开始"且无已通过报名的场次可删除。

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "场次已删除",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.14 管理员 - 秒杀报名审核

##### 2.2.14.1 查询报名列表
```
GET /admin/flash-sales/applications?sessionId=&approvalStatus=&page=1&size=20
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | long | 否 | 场次ID筛选 |
| approvalStatus | int | 否 | 审核状态筛选：0=待审核, 1=已通过, 2=已拒绝 |

响应：`R<PageDTO<FlashSaleApplicationVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 5,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "id": 10,
        "sessionId": 6,
        "sessionName": "周末特惠场",
        "productId": 25,
        "productName": "AirPods Pro 2",
        "productImage": "https://cdn.example.com/product/25.jpg",
        "shopId": 3,
        "shopName": "数码旗舰店",
        "flashPrice": 99900,
        "originalPrice": 199900,
        "stock": 100,
        "sold": 0,
        "perUserLimit": 2,
        "approvalStatus": 0,
        "approvalStatusText": "待审核",
        "rejectReason": null,
        "createTime": "2026-06-19 10:00:00"
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

##### 2.2.14.2 审核通过 / 拒绝
```
PUT /admin/flash-sales/{id}/approve
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approved | boolean | 是 | true=通过, false=拒绝 |
| rejectReason | string | 否 | 拒绝原因（拒绝时必填） |

> 审核通过后，场次时间到达时秒杀自动生效。拒绝后商家可查看拒绝原因。

响应：`R<Void>`

**响应示例（通过）：**
```json
{
  "code": 200,
  "message": "秒杀报名已通过",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**响应示例（拒绝）：**
```json
{
  "code": 200,
  "message": "秒杀报名已拒绝",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.15 商家 - 报名秒杀
```
POST /merchant/flash-sales
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | long | 是 | 秒杀场次ID |
| productId | long | 是 | 商品ID（必须属于当前商家店铺） |
| flashPrice | int | 是 | 秒杀价（分） |
| stock | int | 是 | 秒杀库存 |
| perUserLimit | int | 否 | 每人限购数量（默认1） |

> 后端从 `UserContext.getShopId()` 获取商家店铺ID，并校验 `productId` 是否属于该店铺。
> 同一商品在同一场次仅可报名一次（`uk_session_product` 唯一约束）。
> 报名后 `approval_status=0`（待审核），管理员审核通过后方可生效。

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "报名已提交，等待管理员审核",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.16 商家 - 查看可报名场次
```
GET /merchant/flash-sessions
```
> 返回所有状态为"未开始"的场次，供商家选择报名。

响应：`R<List<FlashSessionVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "id": 6,
      "name": "周末特惠场",
      "startTime": "2026-06-20 14:00:00",
      "endTime": "2026-06-20 18:00:00",
      "status": 0,
      "statusText": "未开始",
      "itemCount": 5
    }
  ],
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.17 商家 - 查看我的报名记录
```
GET /merchant/flash-sales/applications?approvalStatus=&page=1&size=20
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approvalStatus | ApprovalStatus | 否 | 审核状态筛选：PENDING(0)=待审核, APPROVED(1)=已通过, REJECTED(2)=已拒绝（传数字即可） |

> 后端从 `UserContext.getShopId()` 获取当前商家店铺，仅返回该店铺的报名记录。

响应：`R<PageDTO<FlashSaleApplicationVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 2,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "id": 10,
        "sessionId": 6,
        "sessionName": "周末特惠场",
        "productId": 25,
        "productName": "AirPods Pro 2",
        "productImage": "https://cdn.example.com/product/25.jpg",
        "shopId": 3,
        "shopName": "数码旗舰店",
        "flashPrice": 99900,
        "originalPrice": 199900,
        "stock": 100,
        "sold": 0,
        "perUserLimit": 2,
        "approvalStatus": 0,
        "approvalStatusText": "待审核",
        "rejectReason": null,
        "createTime": "2026-06-19 10:00:00"
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.18 获取商品销量排行榜

```
GET /ranking?limit=10
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | int | 否 | 返回条数，默认10，最大100 |

> 按 `sales` 字段倒序排列，走 `idx_sales` 索引，查询 `status = 1 AND deleted = 0` 的上架商品。可用于首页「热销榜单」、管理员 Dashboard。

响应：`R<List<RankingItemVO>>`

```java
// RankingItemVO
{ rank, productId, productName, productImage, minPrice, sales, shopName }
```

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "rank": 1,
      "productId": 1,
      "productName": "iPhone 15",
      "productImage": "https://cdn.example.com/product/1.jpg",
      "minPrice": 699900,
      "sales": 1280,
      "shopName": "李四的店铺"
    },
    {
      "rank": 2,
      "productId": 3,
      "productName": "MacBook Air M4",
      "productImage": "https://cdn.example.com/product/3.jpg",
      "minPrice": 899900,
      "sales": 980,
      "shopName": "数码旗舰店"
    },
    {
      "rank": 3,
      "productId": 2,
      "productName": "Nike Air Jordan 1",
      "productImage": "https://cdn.example.com/product/2.jpg",
      "minPrice": 129900,
      "sales": 560,
      "shopName": "李四的店铺"
    }
  ],
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

#### 2.2.19 管理员 - 获取待审批店铺列表

```
GET /admin/shops/pending?page=1&size=20
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认20 |

> 查询条件：`e_shop.approved = 0`。通过 Feign 调用 user-service 获取申请人用户名和手机号。

响应：`R<PageDTO<ShopApplicationVO>>` — `{ shopId, shopName, logo, ownerId, userName, userPhone, createTime }`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 3,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "shopId": 5,
        "shopName": "王五的数码店",
        "logo": "https://cdn.example.com/logo/5.png",
        "ownerId": 10,
        "userName": "new_shop_1",
        "userPhone": "13800138010",
        "createTime": "2026-06-17 10:00:00"
      },
      {
        "shopId": 6,
        "shopName": "赵六服饰",
        "logo": null,
        "ownerId": 11,
        "userName": "new_shop_2",
        "userPhone": "13800138011",
        "createTime": "2026-06-17 11:30:00"
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.20 管理员 - 获取待审批店铺数量

```
GET /admin/shops/pending/count
```

> 轻量级接口，仅返回待审批店铺总数，供 admin 仪表盘展示角标。

响应：`R<Long>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": 3,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.2.21 管理员 - 审批店铺（批准/拒绝）

```
PUT /admin/shops/{shopId}/approve
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approved | boolean | 是 | true=批准, false=拒绝 |

> **批准：** `UPDATE e_shop SET approved = 1 WHERE id = ? AND approved = 0`，同时通过 Feign 调用 user-service 将用户 role 变更为 1（商家）  
> **拒绝：** `UPDATE e_shop SET approved = 2 WHERE id = ? AND approved = 0`，用户 role 不变。

响应：`R<Void>`

**响应示例（批准成功）：**
```json
{
  "code": 200,
  "message": "店铺审批通过",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

---

#### 2.2.22 内部 - 秒杀订单状态查询

```
GET /flash-sale/orders/{id}/status
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | long | 是 | 秒杀订单ID（`e_flash_sale_order.id`，路径参数） |

> **调用链路：** payment-service（发起支付校验）→ Feign → product-service（本接口）。
> 返回秒杀订单实时状态，用于付款前校验秒杀订单是否已超时（status=4 支付超时则拒绝支付）。仅内部 Feign 调用，不通过 Gateway 对外暴露。

响应：`R<Integer>` — 秒杀订单状态：0=待支付, 1=已支付, 2=已取消, 3=已退款, 4=支付超时

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": 0,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

### 2.3 订单服务 `order-service`（8083）

#### 2.3.1 获取购物车列表
```
GET /cart
```
响应：`R<List<CartVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "cartItemId": 1,
      "productId": 1,
      "productName": "iPhone 15",
      "productImage": "https://cdn.example.com/product/1.jpg",
      "price": 799900,
      "skuId": 102,
      "skuName": "白色 / 256GB",
      "quantity": 2,
      "shopId": 1,
      "shopName": "李四的店铺"
    },
    {
      "cartItemId": 2,
      "productId": 2,
      "productName": "Nike Air Jordan 1",
      "productImage": "https://cdn.example.com/product/2.jpg",
      "price": 129900,
      "skuId": 0,
      "skuName": null,
      "quantity": 1,
      "shopId": 1,
      "shopName": "李四的店铺"
    }
  ],
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.2 添加商品到购物车
```
POST /cart
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productId | long | 是 | 商品SPU ID |
| skuId | long | 否 | SKU ID（有规格时必填，无规格传0或不传） |
| quantity | int | 是 | 数量 |

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "已添加到购物车",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.3 修改购物车商品数量
```
PUT /cart/{cartItemId}
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| quantity | int | 是 | 新数量 |

> `cartItemId` 为购物车记录的主键 ID（非 productId），因为同一商品不同 SKU 是独立条目

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "数量已更新",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.4 删除购物车商品
```
DELETE /cart/{cartItemId}
```
> 删除单条购物车记录，同 productId 不同 skuId 互不影响

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "已从购物车移除",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.5 清空购物车
```
DELETE /cart
```
响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "购物车已清空",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.6 创建订单（下单）
```
POST /orders
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| items | array | 是 | 订单商品列表 |
| items[].productId | long | 是 | 商品SPU ID |
| items[].skuId | long | 是 | SKU ID（无规格商品传0） |
| items[].quantity | int | 是 | 数量 |
| addressId | long | 是 | 收货地址ID |
| couponId | long | 否 | 使用的优惠券ID（仅限未使用且未过期的券） |
| remark | string | 否 | 备注 |

> **下单完整流程（Lua 原子扣库存 + MQ 异步落库）：**  
> 1. 校验收货地址归属 + 商品上下架状态  
> 2. 如果有 `couponId`：校验优惠券是否可用（未使用、未过期、满足满减门槛），计算 `discountAmount`  
> 3. **Redis Lua 脚本原子扣减各 SKU 库存**：每个 SKU 的 stock key 在单次 Lua 中完成「读库存 → 判断是否充足 → 扣减」，无锁无排队  
> 4. 扣减成功：Snowflake 生成 `orderNo`，快照收货地址到 `receiver_*` 字段，创建订单 + 订单明细  
> 5. 若使用优惠券：`UPDATE e_user_coupon SET status=1, used_at=NOW(), order_no=? WHERE id=? AND status=0`（乐观锁防重）  
> 6. 发送 MQ 消息异步同步库存扣减到 DB（最终一致性），同时通知 notification-service  
> 7. 订单创建后启动延时任务（30 分钟），超时未支付 → Lua 补偿库存 + 取消订单  
>
> **库存一致性保障：**  
> | 环节 | 成功 | 失败 |
> |------|------|------|
> | Lua 扣库存 | 继续下单 | 直接返回「库存不足」，连 DB 都不碰 |
> | 创建订单 | 发送 MQ 落库 | 回补 Redis 库存（Lua `INCR`） |
> | MQ 落库存到 DB | 完成 | 重试，最终一致 |
> | 超时未支付 | Lua 补偿库存 + 取消订单 | 重试 |
>
> **为什么不加分布式锁：**  
> Lua 脚本自身是 Redis 单线程原子执行的，天然互斥，无需额外加锁。分布式锁只用于**秒杀一人一单**场景（锁粒度 `user:{id}+product:{id}`），普通下单不涉及。

响应：`R<Long>` — 返回生成的订单号（Snowflake ID）

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": 1734567890123456789,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.7 立即购买（跳过购物车直接下单）

```
POST /orders/buy-now
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productId | long | 是 | 商品SPU ID |
| skuId | long | 是 | SKU ID（无规格传0） |
| quantity | int | 是 | 数量 |
| addressId | long | 是 | 收货地址ID |
| couponId | long | 否 | 使用的优惠券ID |
| remark | string | 否 | 备注 |

> 与 `2.3.6` 下单流程完全一致（Lua 原子扣库存 + MQ 异步落库），只是入参简化为单个商品 + 单个 SKU，方便商品详情页「立即购买」按钮直调。

响应：`R<Long>` — 返回生成的订单号（Snowflake ID）

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": 1734567890123456799,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.8 订单金额预览（计算优惠）

```
POST /orders/preview
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| items[].productId | long | 是 | 商品SPU ID |
| items[].skuId | long | 是 | SKU ID |
| items[].quantity | int | 是 | 数量 |
| couponId | long | 否 | 使用的优惠券ID（校验是否可用） |

> 纯计算接口，不扣库存、不创建订单。返回商品总价、优惠金额、实付金额，供用户确认后再调用 `2.3.6` 或 `2.3.7` 正式下单。

响应：`R<OrderPreviewVO>` — `{ items: [PreviewItemVO], totalAmount, discountAmount, payAmount }`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "items": [
      {
        "productId": 1,
        "skuId": 102,
        "productName": "iPhone 15",
        "skuName": "白色 / 256GB",
        "price": 799900,
        "quantity": 2,
        "subtotal": 1599800
      },
      {
        "productId": 2,
        "skuId": 0,
        "productName": "Nike Air Jordan 1",
        "skuName": null,
        "price": 129900,
        "quantity": 1,
        "subtotal": 129900
      }
    ],
    "totalAmount": 1729700,
    "discountAmount": 10000,
    "payAmount": 1719700
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.9 买家 - 查看我的订单
```
GET /orders/my?page=1&size=20&status=
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 否 | 0=待支付, 1=已支付, 2=已发货, 3=已完成, 4=已取消, 5=退款中, 6=已退款 |

响应：`R<PageDTO<OrderVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 12,
    "page": 1,
    "size": 20,
    "pages": 1,
    "records": [
      {
        "id": 256,
        "orderNo": 1734567890123456789,
        "totalAmount": 1729800,
        "discountAmount": 10000,
        "payAmount": 1719800,
        "status": 0,
        "statusText": "待支付",
        "receiverName": "张三",
        "receiverPhone": "138****8001",
        "receiverAddr": "北京市朝阳区望京SOHO...",
        "payTime": null,
        "items": [
          {
            "productId": 1,
            "skuId": 102,
            "skuName": "白色 / 256GB",
            "productName": "iPhone 15",
            "productImage": "https://cdn.example.com/product/1.jpg",
            "price": 799900,
            "quantity": 2
          }
        ],
        "createTime": "2026-06-17 14:30:00"
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.10 买家 - 查看订单详情
```
GET /orders/{orderNo}
```
响应：`R<OrderDetailVO>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "id": 256,
    "orderNo": 1734567890123456789,
    "totalAmount": 1729800,
    "discountAmount": 10000,
    "payAmount": 1719800,
    "couponId": 5,
    "status": 1,
    "statusText": "已支付",
    "receiverName": "张三",
    "receiverPhone": "13800138001",
    "receiverAddr": "北京市朝阳区望京SOHO T1 1208室",
    "remark": "请尽快发货",
    "payNo": "2026061722001430501407786280",
    "payTime": "2026-06-17 15:00:00",
    "items": [
      {
        "productId": 1,
        "skuId": 102,
        "skuName": "白色 / 256GB",
        "productName": "iPhone 15",
        "productImage": "https://cdn.example.com/product/1.jpg",
        "price": 799900,
        "quantity": 2
      }
    ],
    "createTime": "2026-06-17 14:30:00"
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.11 买家 - 取消订单
```
PUT /orders/{orderNo}/cancel
```
响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "订单已取消",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.12 商家 - 查看店铺订单
```
GET /orders/shop?page=1&size=20&status=
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 否 | 0=待支付, 1=已支付, 2=已发货, 3=已完成, 4=已取消, 5=退款中, 6=已退款 |

响应：`R<PageDTO<OrderVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 45,
    "page": 1,
    "size": 20,
    "pages": 3,
    "records": [
      {
        "id": 256,
        "orderNo": 1734567890123456789,
        "totalAmount": 1729800,
        "discountAmount": 10000,
        "payAmount": 1719800,
        "status": 1,
        "statusText": "已支付",
        "receiverName": "张三",
        "receiverPhone": "138****8001",
        "receiverAddr": "北京市朝阳区望京SOHO...",
        "payTime": "2026-06-17 15:00:00",
        "items": [],
        "createTime": "2026-06-17 14:30:00"
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.13 商家 - 修改订单状态
```
PUT /orders/{orderNo}/status
```
请求体：`UpdateOrderStatusDTO`
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | 是 | 2=已发货, 3=已完成 |

> 状态只能单向流转：待支付→已支付→已发货→已完成

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "订单状态已更新为「已发货」",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.14 管理员分页查优惠券模板
```
GET /admin/coupons?page=1&size=10&status=1&type=0&keyword=满减
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认10 |
| status | int | 否 | 状态: 0=停用, 1=启用 |
| type | int | 否 | 类型: 0=满减券, 1=无门槛券 |
| keyword | string | 否 | 关键词（匹配名称） |

响应：`R<PageDTO<CouponVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "pages": 2,
    "total": 8,
    "records": [
      {
        "id": 1,
        "name": "新用户满减券",
        "type": 0,
        "threshold": 500000,
        "reduce": 50000,
        "description": "满500元减50元",
        "validDays": 7,
        "limitPerUser": 1,
        "categoryId": null,
        "claimedCount": 86
      },
      {
        "id": 2,
        "name": "618无门槛券",
        "type": 1,
        "threshold": 0,
        "reduce": 2000,
        "description": "无门槛立减20元",
        "validDays": 3,
        "limitPerUser": 1,
        "categoryId": null,
        "claimedCount": 452
      },
      {
        "id": 3,
        "name": "数码满减券",
        "type": 0,
        "threshold": 300000,
        "reduce": 30000,
        "description": "满300元减30元",
        "validDays": 14,
        "limitPerUser": 2,
        "categoryId": 5,
        "claimedCount": 120
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.15 管理员查优惠券详情
```
GET /admin/coupons/{id}
```
响应：`R<CouponVO>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "id": 1,
    "name": "新用户满减券",
    "type": 0,
    "threshold": 500000,
    "reduce": 50000,
    "description": "满500元减50元",
    "validDays": 7,
    "limitPerUser": 1,
    "categoryId": null,
    "claimedCount": 86
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.16 管理员创建优惠券
```
POST /admin/coupons
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 优惠券名称 |
| type | int | 是 | 类型: 0=满减券, 1=无门槛券 |
| threshold | int | 否 | 使用门槛（分），满减券必填，0=无门槛 |
| reduce | int | 是 | 减免金额（分） |
| categoryId | long | 否 | 限制品类ID，null=全场通用 |
| validDays | int | 是 | 有效天数 |
| limitPerUser | int | 是 | 每人限领数量 |
| totalCount | int | 是 | 总发行量 |

响应：`R<CouponVO>` — 返回创建成功的优惠券对象

**请求示例：**
```json
{
  "name": "新用户满减券",
  "type": 0,
  "threshold": 500000,
  "reduce": 50000,
  "validDays": 7,
  "limitPerUser": 1,
  "totalCount": 500
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "id": 1,
    "name": "新用户满减券",
    "type": 0,
    "threshold": 500000,
    "reduce": 50000,
    "description": null,
    "validDays": 7,
    "limitPerUser": 1,
    "categoryId": null,
    "claimedCount": 0
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.17 管理员更新优惠券
```
PUT /admin/coupons/{id}
```
参数同 2.3.16，所有字段均可选。

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.18 管理员上下架优惠券
```
PUT /admin/coupons/{id}/status?status=0
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | int | 是 | 0=停用, 1=启用 |

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.19 管理员删除优惠券
```
DELETE /admin/coupons/{id}
```
响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.20 获取可领优惠券列表
```
GET /coupons
```
响应：`R<List<CouponVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "name": "新用户满减券",
      "type": 0,
      "threshold": 500000,
      "reduce": 50000,
      "description": "满500元减50元",
      "validDays": 7,
      "limitPerUser": 1,
      "categoryId": null,
      "claimedCount": 86
    },
    {
      "id": 2,
      "name": "618无门槛券",
      "type": 1,
      "threshold": 0,
      "reduce": 2000,
      "description": "无门槛立减20元",
      "validDays": 3,
      "limitPerUser": 1,
      "categoryId": null,
      "claimedCount": 452
    }
  ],
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.21 领取优惠券
```
POST /coupons/{couponId}/claim
```
> 校验：优惠券启用中、未领完、用户未超限；领取后 claimedCount +1。

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "领取成功",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.22 获取我的优惠券
```
GET /coupons/my
```
响应：`R<List<UserCouponVO>>` — 按 available / used / expired 分组

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "available": [
      {
        "id": 10,
        "couponId": 1,
        "couponName": "新用户满减券",
        "type": 0,
        "threshold": 500000,
        "reduce": 50000,
        "description": "满500元减50元",
        "status": 0,
        "statusText": "未使用",
        "claimedAt": "2026-06-17 10:00:00",
        "expireAt": "2026-06-24 10:00:00"
      },
      {
        "id": 11,
        "couponId": 3,
        "couponName": "数码满减券",
        "type": 0,
        "threshold": 300000,
        "reduce": 30000,
        "description": "满300元减30元",
        "status": 0,
        "statusText": "未使用",
        "claimedAt": "2026-06-25 14:00:00",
        "expireAt": "2026-07-09 14:00:00"
      }
    ],
    "used": [
      {
        "id": 9,
        "couponId": 2,
        "couponName": "618无门槛券",
        "type": 1,
        "threshold": 0,
        "reduce": 2000,
        "description": "无门槛立减20元",
        "status": 1,
        "statusText": "已使用",
        "claimedAt": "2026-06-16 10:00:00",
        "expireAt": "2026-06-19 10:00:00"
      }
    ],
    "expired": []
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.17 支付回调（由 payment-service 调用）

```
PUT /orders/{orderNo}/pay-callback
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| payNo | string | 是 | 第三方支付流水号（Query 参数） |
| payTime | string | 是 | 支付时间（Query 参数，格式 yyyy-MM-dd HH:mm:ss） |

> **调用链路：** payment-service（支付宝异步通知）→ Feign → order-service（本接口）
>
> **前置：** payment-service 已对支付宝回调验签 + 金额校验（`实付金额 >= 订单金额`），本接口仅做状态更新和后续业务处理。
>
> **处理流程：**  
> 1. 幂等校验：查询订单，若 status 已为 PAID 直接返回成功  
> 2. 乐观锁更新：`UPDATE e_order SET status=PAID, pay_no=?, pay_time=? WHERE order_no=? AND status=PENDING_PAYMENT`  
> 3. 更新商品销量（Redis INCR）  
> 4. 发送支付成功 MQ（`order.pay`）供下游消费  
> 5. 事务提交后：发送通知 MQ → notification-service 创建支付通知

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "支付确认成功",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.18 退款回调（由 payment-service 调用）

```
PUT /orders/{orderNo}/refund-callback
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refundAmount | int | 是 | 退款金额（分，Query 参数） |

> **调用链路：** payment-service（支付宝退款通知）→ Feign → order-service（本接口）
>
> **前置条件：** 仅 PAYMENT_SUCCESS 或 SHIPPED 状态可退款。
>
> **处理流程：**  
> 1. 乐观锁更新订单状态：`UPDATE e_order SET status=REFUNDED WHERE order_no=? AND status IN (PAID, SHIPPED)`  
> 2. 回补 Redis 库存（Redis Pipeline INCRBY）  
> 3. 发送库存同步 + 库存恢复 MQ → product-service  
> 4. 退还优惠券：`UPDATE e_user_coupon SET status=UNUSED WHERE order_no=?`  
> 5. 事务提交后：发送通知 MQ → notification-service 创建退款通知

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "退款处理成功",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.19 订单统计（管理员仪表盘）

```
GET /orders/statistics
```

> 供 admin 仪表盘调用，返回全平台订单总量与销售额。内部 Feign 接口，由 `OrderClient.getOrderStatistics()` 调用。

响应：`R<OrderStatisticsDTO>`

| 字段 | 类型 | 说明 |
|------|------|------|
| totalOrders | Long | 订单总数 |
| totalSales | Long | 总销售额（分） |

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "totalOrders": 12580,
    "totalSales": 35689000
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.20 订单超时自动取消（MQ 延迟消息）

```
消息队列: order.delay.queue → OrderTimeoutListener
```

> 下单时发送延迟消息（普通订单 30 分钟，秒杀订单 15 分钟），到期后消费者执行：
>
> 1. 乐观锁更新：`UPDATE e_order SET status=CANCELLED WHERE order_no=? AND status=PENDING_PAYMENT`  
> 2. 回补 Redis 库存（如已预扣）  
> 3. 退还优惠券：`UPDATE e_user_coupon SET status=UNUSED WHERE order_no=?`  
> 4. 事务提交后：发送通知 MQ → notification-service 创建超时取消通知

---

#### 2.3.21 系统超时同步取消订单

```
PUT /orders/{orderNo}/timeout-cancel
```

> **调用链路：** product-service（`FlashSaleTimeoutJob`）→ Feign → order-service（本接口）。
> 仅限秒杀订单，由秒杀超时任务在回补 flash 库存后同步取消 EOrder，防止用户继续支付已释放库存的订单。不通过 Gateway 对外暴露，非秒杀订单调用会被拒绝。

> **处理流程：**
> 1. 校验订单 `flashSaleOrderId != null`（仅秒杀订单可用），否则拒绝
> 2. 乐观锁更新：`UPDATE e_order SET status=CANCELLED, cancel_time=NOW() WHERE order_no=? AND status=PENDING_PAYMENT`
> 3. 退还优惠券（如有）
> 4. 交易提交后：发送取消通知 MQ → notification-service

> ⚠️ **注意：** 本接口不恢复 SKU 库存、不发送 `ORDER_CANCELLED_NOTIFY_KEY`——库存回补已在 product-service 的 `FlashSaleTimeoutJob` 中完成。

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.3.22 管理员 - 修改用户角色

```
PUT /admin/users/{id}/role
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| role | int | 是 | 角色值：0=普通用户, 1=商家, 2=管理员 |

> **调用链路：** product-service（店铺审批通过后）→ Feign → user-service（本接口）。
> 管理员审批开店通过后，product-service 调用此接口将申请人角色从 0 变更为 1（商家）。仅内部 Feign 调用。

响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "角色已更新",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

### 2.4 通知服务 `notification-service`（8084）

#### 2.4.1 商家 - 获取通知列表
```
GET /notifications/shop?page=1&size=50
```
响应：`R<PageDTO<NotificationVO>>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "total": 28,
    "page": 1,
    "size": 50,
    "pages": 1,
    "records": [
      {
        "id": 101,
        "shopId": 1,
        "type": 0,
        "title": "新订单通知",
        "content": "用户 张三 下单购买了 iPhone 15 等 2 件商品，订单金额 ¥17198.00",
        "orderId": 256,
        "isRead": 0,
        "createTime": "2026-06-17 14:30:00"
      },
      {
        "id": 100,
        "shopId": 1,
        "type": 2,
        "title": "促销活动提醒",
        "content": "618 大促即将开始，请提前设置秒杀活动",
        "orderId": null,
        "isRead": 1,
        "createTime": "2026-06-17 09:00:00"
      }
    ]
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.4.2 获取未读通知数

```
GET /notifications/unread-count
```
> 轻量级接口，仅返回 `{ unreadCount: int }`，供 Dashboard 实时展示角标。走 Redis 缓存 `shop:{shopId}:unread`（有新通知时 INCR，已读时 DECR）。

响应：`R<{ unreadCount: int }>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "unreadCount": 5
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.4.3 标记单条通知为已读
```
PUT /notifications/{id}/read
```
响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "已标记为已读",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.4.4 全部标记为已读
```
PUT /notifications/read-all
```
响应：`R<Void>`

**响应示例：**
```json
{
  "code": 200,
  "message": "全部已标记为已读",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.4.5 管理员 - 发送通知（系统通知 / 促销活动）

```
POST /notifications
```

> **权限：** `role=2`（管理员）。Gateway 层校验（见 [2.0 网关鉴权](#20-网关鉴权gateway-auth)），同时服务层双重校验 `UserContext.getRole() == 2`。
> **实现说明：** type=0（新订单）由 order-service 通过 MQ 自动触发，不通过此接口。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| shopId | Long | 是 | 目标店铺ID（不可为空） |
| type | Integer | 是 | 1=系统通知，2=促销活动（0 禁止） |
| title | String | 是 | 通知标题，最长 200 字符 |
| content | String | 是 | 通知内容，最长 5000 字符 |
| orderId | Long | 否 | 关联订单号（系统通知可关联订单） |

响应：`R<Void>`

**请求示例：**
```json
{
  "shopId": 1,
  "type": 1,
  "title": "平台公告",
  "content": "平台将于 2026-07-10 进行系统维护，届时暂停交易 2 小时",
  "orderId": null
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "通知已发送",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

> **校验规则：**
> - `type` 仅允许 1（系统通知）和 2（促销活动），禁止通过此接口创建 type=0（新订单）
> - `shopId` 必填，会在 service 层校验店铺是否存在
> - `title` 非空、不超过 200 字符
> - `content` 非空、不超过 5000 字符

> **触发链路对比：**
> ```
> type=0 新订单 → order-service (MQ) → notification-service
> type=1 系统通知 → admin-frontend (HTTP) → Gateway → notification-service
> type=2 促销活动 → admin-frontend (HTTP) → Gateway → notification-service
> ```

---

### 2.5 文件服务 `file-service`（8085）

#### 2.5.1 上传文件

```
POST /files/upload
```

Content-Type: `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 文件（支持 jpg / jpeg / png / gif / webp / bmp，最大 10MB） |
| type | string | 是 | 文件类型：`avatar` / `logo` / `product` / `category` / `banner` |

存储规则：
| type | 存储路径 | URL 示例 |
|------|---------|----------|
| avatar | `/opt/uploads/avatar/` | `http://ip:8080/static/avatar/uuid.jpg` |
| logo | `/opt/uploads/logo/` | `http://ip:8080/static/logo/uuid.png` |
| product | `/opt/uploads/product/` | `http://ip:8080/static/product/uuid.webp` |
| category | `/opt/uploads/category/` | `http://ip:8080/static/category/uuid.png` |
| banner | `/opt/uploads/banner/` | `http://ip:8080/static/banner/uuid.jpg` |

响应：`R<FileUploadVO>` — `{ fileName, url, type, size }`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "fileName": "avatar.jpg",
    "url": "http://192.168.150.101:8080/static/avatar/a1b2c3d4e5f67890.jpg",
    "type": "avatar",
    "size": 204800
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

### 2.6 支付服务 `payment-service`（8086）

> **支付流程概览：**  
> 1. 前端调用 `POST /payment/pay` 发起支付，后端返回支付宝页面支付表单 HTML  
> 2. 用户扫码/登录支付宝完成支付  
> 3. 支付宝异步通知 `POST /payment/notify`，payment-service 验签后调用 `PUT /orders/{orderNo}/pay-callback` 推动订单流转  
> 4. 支付宝同步跳转 `GET /payment/return`，展示支付结果页（仅展示，不驱动业务）  
>
> **退款流程概览：**  
> 1. 商家/买家在前端发起退款 → order-service 调用 `POST /payment/refund`  
> 2. 支付宝处理退款并异步通知 → payment-service 处理退款结果，回调 order-service 更新订单状态  
>
> **幂等性设计：**  
> - 支付：`orderNo` 已支付不可重复支付（`e_payment_record` 的 `uk_pay_no` 唯一索引防重）  
> - 退款：`outRequestNo`（商户退款请求号）作为支付宝 `out_request_no`，`e_refund_record` 的 `uk_out_request_no` 唯一索引保证幂等  

---

#### 2.6.1 发起支付宝支付（页面支付）

```
POST /payment/pay
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | long | 是 | 订单号 |

> **处理流程：**  
> 1. 通过 Feign 调用 order-service 查询订单信息（金额、商品摘要），校验订单归属用户  
> 2. 校验 `e_order.status = 0`（待支付）  
> 3. 构建 Alipay `alipay.trade.page.pay` 请求（产品码 `FAST_INSTANT_TRADE_PAY`）  
> 4. 调用支付宝 SDK 生成支付表单 HTML  
> 5. 写入 `e_payment_record`（status=0 待支付），若 `out_trade_no` 冲突 → 查询已有记录，若未支付则返回已有 form，若已支付则直接跳转结果页  

响应：`text/html`（支付宝页面支付表单，前端直接 `document.write` 或新窗口打开）

---

#### 2.6.2 支付宝异步通知（支付回调）

```
POST /payment/notify
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| （支付宝 POST form 参数） | — | — | `notify_id`、`out_trade_no`、`trade_no`、`total_amount`、`trade_status` 等 |

> **处理流程：**  
> 1. 验签：调用 `AlipaySignature.rsaCheckV1()` 校验支付宝签名  
> 2. 幂等：`SELECT status FROM e_payment_record WHERE pay_no = ?`，若 status=1 直接返回 `success`  
> 3. 状态校验：仅 `TRADE_SUCCESS` / `TRADE_FINISHED` 为有效支付完成，其余状态（如 `WAIT_BUYER_PAY`）仅记录日志不驱动业务  
> 4. 金额校验：`alipay_total_amount` = 订单实付金额（分），防止金额篡改  
> 5. 更新 `e_payment_record`：status=1, pay_time=NOW()  
> 6. Feign 调用 order-service `PUT /orders/{orderNo}/pay-callback` 推动订单状态流转  
> 7. 如果回调成功返回 `success`，失败返回 `failure`（支付宝将按递增间隔重试）

> ⚠️ **安全关键：** 异步通知是唯一驱动订单状态流转的支付确认渠道。同步跳转（`/payment/return`）仅做结果展示，不更新订单状态。

响应：`text/plain` — `success` 或 `failure`

---

#### 2.6.3 支付宝同步跳转（支付结果页）

```
GET /payment/return?orderNo={orderNo}
```

> 支付宝支付完成后浏览器同步跳转到此页面。**不做业务状态更新**，仅查询支付结果并返回 JSON。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | long | 是 | 订单号 |

响应：`R<PayResultVO>` — `{ orderNo, payNo, totalAmount, status, statusText, payTime }`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "orderNo": 202606290001,
    "payNo": "2026062922001417920503312345",
    "totalAmount": 19800,
    "status": 1,
    "statusText": "支付成功",
    "payTime": "2026-06-29 15:00:00"
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.6.4 查询支付状态

```
GET /payment/status/{orderNo}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | long | 是 | 订单号（路径参数） |

> 先查本地 `e_payment_record`，若本地无记录或状态=0 则主动向支付宝发起查询（`alipay.trade.query`），同步最新状态后返回。

响应：`R<PayResultVO>`

---

#### 2.6.5 发起退款

```
POST /payment/refund
```
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| orderNo | long | 是 | 订单号 |
| refundAmount | int | 是 | 退款金额（分），需 ≤ 原支付金额 |
| reason | string | 否 | 退款原因（≤256字符） |

> **处理流程：**  
> 1. 查询 `e_payment_record` 确认支付成功且 `refundAmount ≤ total_amount`  
> 2. 生成 `outRequestNo`（Snowflake ID，幂等关键）  
> 3. 调用 `alipay.trade.refund`，传入 `out_request_no`、`trade_no`、`refund_amount`  
> 4. INSERT `e_refund_record`（status=0 处理中）  
> 5. 支付宝同步返回退款结果时：若成功 → 更新 refund_record status=1，回调 order-service；若失败 → status=2

响应：`R<RefundResultVO>` — `{ orderNo, refundNo, outRequestNo, refundAmount, status, statusText, refundTime }`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "orderNo": 202606290001,
    "refundNo": "2026062922001417920503312346",
    "outRequestNo": "1234567890123456789",
    "refundAmount": 19800,
    "status": 1,
    "statusText": "退款成功",
    "refundTime": "2026-06-29 16:00:00"
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.6.6 查询退款状态

```
GET /payment/refund/{outRequestNo}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| outRequestNo | string | 是 | 商户退款请求号（路径参数） |

> 先查本地 `e_refund_record`，若本地无记录或状态=0 则向支付宝查询（`alipay.trade.fastpay.refund.query`），同步最新状态后返回。

响应：`R<RefundResultVO>`

**响应示例：**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "orderNo": 202606290001,
    "refundNo": "2026062922001417920503312346",
    "outRequestNo": "1234567890123456789",
    "refundAmount": 19800,
    "status": 1,
    "statusText": "退款成功",
    "refundTime": "2026-06-29 16:00:00"
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

#### 2.6.7 支付宝退款异步通知

```
POST /payment/refund/notify
```

> 处理支付宝退款异步通知（`trade_status = REFUND_SUCCESS`）。验签后更新 `e_refund_record` 和回调 order-service 更新订单状态为已退款。

响应：`text/plain` — `success` 或 `failure`

---

#### 2.6.8 内部 Feign：订单基础信息查询

```
GET /orders/feign/{orderNo}
```

> payment-service 通过此接口查询订单归属用户和金额，用于支付发起时的校验。返回 `OrderBasicDTO`：`{ orderNo, userId, totalAmount, discountAmount, status, subject }`。

---

### 2.7 内部 Feign 接口（e-api 模块）

> 库存扣减/恢复通过 MQ 异步同步，无需 Feign 接口。订单超时恢复同理走 MQ。

| 接口 | 方法 | 路径 | 调用方 |
|------|------|------|--------|
| UserClient.updateUserRole(id, role) | PUT | /admin/users/{id}/role | product-service（店铺审批通过后变更用户角色） |
| UserClient.getUsersByIds(ids) | GET | /admin/users/batch | product-service（管理员查看待审批店铺时获取申请人信息） |
| UserClient.getAddressById(id) | GET | /addresses/{id} | order-service（下单时校验收货地址） |
| ProductClient.getDetailsByIds(ids) | GET | /products/list | order-service（购物车/下单时批量查商品信息） |
| ProductClient.getDetail(id) | GET | /products/{id} | order-service（立即购买时查单个商品） |
| OrderClient.getOrderBasic(orderNo) | GET | /orders/feign/{orderNo} | payment-service（发起支付时校验订单金额和归属） |
| OrderClient.payCallback(orderNo, …) | PUT | /orders/{orderNo}/pay-callback | payment-service（支付回调推动订单状态） |
| OrderClient.cancelOrderByTimeout(orderNo) | PUT | /orders/{orderNo}/timeout-cancel | product-service（秒杀超时同步取消订单） |
| OrderClient.refundCallback(orderNo, amount) | PUT | /orders/{orderNo}/refund-callback | payment-service（退款回调推动订单状态） |
| OrderClient.getOrderStatistics() | GET | /orders/statistics | user-service（管理员仪表盘订单统计） |
| ProductClient.getPendingShopCount() | GET | /admin/shops/pending/count | user-service（管理员仪表盘待审批数） |
| ProductClient.getFlashOrderStatus(id) | GET | /flash-sale/orders/{id}/status | payment-service（支付前校验秒杀状态） |

---

## 三、VO / DTO 数据结构

### 3.1 用户相关
```java
// UserVO
{ id, username, name, phone, role, avatar, status, lastLoginTime }

// LoginVO
{ token, user: UserVO }

// AddressVO
{ id, receiverName, receiverPhone, province, city, district, detail, isDefault }

// RegisterDTO
{ username, password, name, phone, avatar }
// 注册仅创建普通用户（role=0），开店需登录后单独申请

// ShopApplyDTO
{ name, logo }
// 申请开店时传入，ownerId 从 JWT 中获取

// ShopApplyVO
{ shopId, name, logo, approved, approvedText }
// approved: 0=待审批, 1=已通过, 2=已拒绝

// ShopApplicationVO（管理员端）
{ shopId, shopName, logo, ownerId, userName, userPhone, createTime }

// AdminStatisticsVO
{ totalUsers, totalMerchants, pendingMerchants, totalOrders, totalSales }
// totalMerchants = COUNT(e_user WHERE role=1); pendingMerchants = COUNT(e_shop WHERE approved=0)
```

### 3.2 商品相关
```java
// ProductVO（列表用，商家自己的商品列表 `GET /products/my` 含 skus）
{ id, name, categoryId, categoryName, shopId, shopName,
  minPrice, totalStock, image, description, status, sales, skus: [SkuVO] }

// ProductDetailVO（详情用，含 SKU 列表）
{ ...ProductVO, skus: [SkuVO] }

// SkuVO
{ id, productId, skuCode, specs: [{name, value}], price, stock, image, status }

// CategoryVO
{ id, name, icon }

// ShopVO
{ id, name, logo, status, approved }

// FlashSessionVO
{ id, name, startTime, endTime, status, statusText, itemCount }
// status: 0=未开始, 1=进行中, 2=已结束（查询时动态计算）

// FlashSaleVO（公开秒杀列表返回）
// 外层包含场次信息，items 为商品列表
{ id, name, startTime, endTime,
  items: [{ id, productId, productName, productImage, originalPrice,
            flashPrice, stock, sold, shopId, shopName, progress, perUserLimit }] }
// progress = sold / (sold + stock) × 100，秒杀进度百分比

// FlashSaleApplicationVO（管理员审核列表 & 商家报名记录）
{ id, sessionId, sessionName, productId, productName, productImage,
  shopId, shopName, flashPrice, originalPrice, stock, sold, perUserLimit,
  approvalStatus, approvalStatusText, rejectReason, createTime }
// approvalStatus: 0=待审核, 1=已通过, 2=已拒绝

// FlashSaleOrderVO
{ id, flashSaleId, productName, quantity, price, status, statusText, payNo, payTime, cancelTime, createTime }
// statusText: 前端展示用，0→待支付, 1→已支付, 2→已取消, 3→已退款, 4→支付超时

// RankingItemVO
{ rank, productId, productName, productImage, minPrice, sales, shopName }
```

### 3.3 订单相关
```java
// CartVO
{ productId, productName, productImage, price, skuId, skuName, quantity, shopId, shopName }
// skuId=0 且 skuName=null 表示该商品无规格，直接按 productId 购买

// OrderVO
{ id, orderNo, totalAmount, discountAmount, status, statusText,
  receiverName, receiverPhone, receiverAddr, payNo, payTime,
  items: [OrderItemVO], createTime }

// OrderDetailVO
{ id, orderNo, totalAmount, discountAmount, couponId, status, statusText,
  receiverName, receiverPhone, receiverAddr, remark, payNo, payTime,
  items: [OrderItemVO], createTime }

// OrderItemVO
{ productId, skuId, skuName, productName, productImage, price, quantity }

// OrderPreviewVO
{ items: [PreviewItemVO], totalAmount, discountAmount, payAmount }
// PreviewItemVO: { productId, skuId, productName, skuName, price, quantity, subtotal }

// CouponVO
{ id, name, type, threshold, reduce, description, validDays, limit,
  categoryId, claimed }

// UserCouponVO
{ id, couponId, couponName, type, threshold, reduce, description,
  status, statusText, claimedAt, expireAt }
// statusText: 0→未使用, 1→已使用, 2→已过期
// 前端分组: status=0 且 expireAt > now → 可用; status=1 → 已使用; status=2 或已过期 → 已过期
```

### 3.4 通知相关
```java
// NotificationVO
{ id, shopId, type, title, content, orderId, isRead, createTime }
// type: 0=新订单(MQ自动), 1=系统通知(管理员), 2=促销活动(管理员)

// CreateNotificationDTO（管理员发送通知用）
{ shopId, type, title, content, orderId }
// 约束: type ∈ {1, 2}, shopId 必填, title ≤ 200 字, content ≤ 5000 字

// OrderNotificationMessage（MQ 消息体，order-service → notification-service）
{ orderNo, shopId, type, title, content }
// type=0（新订单/支付/取消/超时/退款/秒杀）由 order-service 通过 MQ 自动触发，共 6 个触发点
```

### 3.5 支付相关
```java
// PayRequestDTO
{ orderNo }

// PayResultVO
{ orderNo, payNo, totalAmount, status, statusText, payTime }
// statusText: 0→待支付, 1→支付成功, 2→已关闭

// RefundRequestDTO
{ orderNo, refundAmount, reason }

// RefundResultVO
{ orderNo, refundNo, outRequestNo, refundAmount, status, statusText, refundTime }
// statusText: 0→处理中, 1→退款成功, 2→退款失败

// AlipayNotifyDTO
{ notifyId, outTradeNo, tradeNo, totalAmount, tradeStatus, gmtPayment, sign, signType, ... }
// 支付宝异步通知参数，用于验签和业务处理

// OrderBasicDTO（e-api 模块，feign 接口用）
{ orderNo, userId, totalAmount, discountAmount, status, subject, flashSaleOrderId }
// subject: 订单摘要「商品A等3件」，用于支付宝支付页面展示
// flashSaleOrderId: 秒杀订单ID（关联 e_flash_sale_order），null 表示非秒杀订单
```

---

## 四、服务间通信

### 4.1 HTTP 路由（通过 Gateway）

```
[Gateway :8080]
     │
     ├── /users/**         ──→  user-service :8081
     ├── /addresses/**     ──→  user-service :8081
     ├── /admin/**         ──→  user-service :8081 / product-service :8082 / order-service :8083
     ├── /products/**      ──→  product-service :8082
     ├── /categories       ──→  product-service :8082
     ├── /shops/**         ──→  product-service :8082
     ├── /flash-sales/**   ──→  product-service :8082
     ├── /merchant/**      ──→  product-service :8082
     ├── /ranking/**       ──→  product-service :8082
     ├── /orders/**        ──→  order-service :8083
     ├── /cart/**          ──→  order-service :8083
     ├── /coupons/**       ──→  order-service :8083
     ├── /notifications/** ──→  notification-service :8084
     ├── /files/**         ──→  file-service :8085
     └── /payment/**       ──→  payment-service :8086
```

### 4.2 Feign 调用

| 调用方 | 被调方 | 接口 | 用途 |
|--------|--------|------|------|
| order-service | product-service | ProductClient | 查询商品信息、SKU 价格 |
| order-service | user-service | UserClient | 查询用户地址 |
| order-service | payment-service | — | 发起退款 |
| payment-service | order-service | OrderClient | 查询订单信息、支付回调、退款回调 |
| payment-service | user-service | UserClient | 查询用户信息 |
| product-service | user-service | UserClient | 店铺审批后变更用户角色 |
| user-service | order-service | OrderClient | 管理员统计（订单数/销售额） |
| user-service | product-service | ProductClient | 管理员统计（待审批店铺数） |

### 4.3 MQ 消息队列

**交换机：** `order.topic`（TopicExchange） | `product.topic`（TopicExchange）

| 队列 | Routing Key | 生产者 | 消费者 | 用途 |
|------|------------|--------|--------|------|
| `cart.sync.queue` | `cart.sync` | order-service | order-service (CartSyncListener) | 登录后合并离线购物车 |
| `coupon.claim.queue` | `coupon.claim` | order-service | order-service (CouponClaimListener) | 异步领取优惠券 |
| `stock.sync.queue` | `stock.sync` | order-service | product-service (StockSyncListener) | Redis 库存同步到 DB |
| `stock.restore.queue` | `stock.restore` | order-service | product-service | 取消/退款时回补库存 |
| `order.delay.queue` | `order.delay` | order-service | order-service (OrderTimeoutListener) | 订单超时延迟取消（30min/15min） |
| `order.flash.create.queue` | `order.flash.create` | product-service | order-service (FlashSaleOrderListener) | 秒杀异步创建订单 |
| `order.notify.queue` | `order.notify` | order-service | notification-service | 订单通知（新订单/支付/取消/超时/退款/秒杀） |

> 通知 MQ 共 6 个触发点：普通下单 → 支付 → 取消 → 超时取消 → 退款 → 秒杀下单，全部在 `afterCommit` 中发送，确保事务提交后才投递。
> 所有队列持久化 + 消息持久化（`DeliveryMode.PERSISTENT`），失败投递至 DLQ（`RepublishMessageRecoverer`）。

## 五、Nacos 共享配置

| 配置ID | 内容 | 使用方 |
|--------|------|--------|
| shared-spring.yaml | 通用 Spring 配置 | 所有服务 |
| shared-mybatis.yaml | 数据源 + MyBatis-Plus 配置 | e-user, e-product, e-roder, e-notification |
| shared-logs.yaml | 日志配置 | 所有服务 |
| shared-redis.yaml | Redis 连接配置 | e-roder |
| shared-mq.yaml | RabbitMQ 连接配置 | e-roder, e-notification |

---

## 六、前端秒杀模块防刷设计

对应后端 API `2.2.12` 和 `e_flash_sale_order` 表。秒杀时间跟随场次（`e_flash_session`），前端根据 `GET /flash-sales` 返回的 `sessionStartTime` / `sessionEndTime` 计算倒计时。

### 6.1 秒杀按钮状态机

```
[即将开始] → 倒计时结束 → [立即秒杀] → 点击 → [请求中(置灰)] → 响应
                                                              ├─ 成功 → [已抢到]
                                                              └─ 失败 → [已售罄] / [超出限购]
```

### 6.2 前端防刷逻辑

1. **首次有效，后续忽略**：点击「立即秒杀」的瞬间立即发送请求，同时按钮锁定置灰，后续所有点击直接丢弃，不排队、不等待。与 debounce 不同——debounce 是等用户停下来才发，秒杀场景正好用反了
2. **本地去重**：秒杀成功后 `localStorage` 记录 `flash_order_{flashSaleId}` 计数器，与 `perUserLimit` 比较，达到上限后按钮直接显示"已抢到"，跳过请求
3. **请求级去重**：同一秒杀活动 `pending` 状态的请求未返回前，不发起第二个请求（防止用户快速切换页面重复提交）
4. **库存进度条**：`sold / (sold + remainingStock) × 100%`，WebSocket 实时推送剩余库存
5. **失败处理**：
   - `code=409` 超出限购 → 提示「已超出每人限购数量」
   - `code=410` 已售罄 → 按钮变灰显示「已售罄」
   - `code=429` 请求过频 → 提示「操作太频繁，请稍后再试」

### 6.3 秒杀进度实时推送

```
ws://notification-service/ws/flash-sale?flashSaleId={id}
```
服务端每秒广播当前秒杀进度：`{ sold, remainingStock, progress }`

---

## 七、前端 SKU 规格选择与购物车展示

### 7.1 商品详情页 — SKU 选择器

对应 `e_cart.uk_user_sku`，同一商品不同规格在购物车中独立存储。

```
设计思路：
- 商品 SPU（如 iPhone 15） → 包含多个 SKU（128G黑色, 256G白色, 512G蓝色）
- 用户进入详情页后，先选择规格再点「加入购物车」
- 未选规格时按钮置灰，提示「请选择规格」
```

SKU 选择器交互：

```
┌─────────────────────────────────┐
│  iPhone 15                      │
│  ¥6999                          │
│                                  │
│  颜色： ○黑色  ●白色  ○蓝色      │
│  存储： ○128G  ●256G  ○512G     │
│                                  │
│  当前选择：白色 / 256G  ¥7999    │  ← 价格随规格联动
│                                  │
│  [加入购物车]                    │  ← 已选规格才可点击
└─────────────────────────────────┘
```

### 7.2 购物车页 — SKU 展示

```
┌──────────────────────────────────────┐
│ [图] iPhone 15                       │
│       规格：白色 / 256G              │  ← 规格名称独立显示
│       ¥7999          qty: 2          │
│       [删除]                         │
├──────────────────────────────────────┤
│ [图] Nike Air Jordan 1               │
│       ¥1299           qty: 1          │
│       [删除]                         │
└──────────────────────────────────────┘
```

同一 SPU 不同 SKU 视为独立购物车条目，`uk_user_sku (user_id, product_id, sku_id)` 唯一约束确保不会重复添加。
