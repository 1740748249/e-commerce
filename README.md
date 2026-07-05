# 多多商城 — 微服务电商平台 v2.0

基于 Spring Cloud Alibaba 的全栈多商户电商平台，采用微服务架构，覆盖**高并发秒杀、Redis Lua 原子领券、支付宝支付、实时消息推送**等核心电商场景。

> **v2.0 更新：** 新增优惠券系统（Lua 原子领券 + MQ 异步落库）、通知系统重构（双表已读隔离 + 三 Lua 脚本原子计数）、全平台 Bug 修复与性能优化。

---

## 系统架构

```mermaid
graph TD
    subgraph 客户端
        U["🖥️ 用户端 (Vue 3)"]
        A["🛠️ 管理后台 (Vue 3)"]
    end

    subgraph 基础设施
        GW["🚪 API Gateway<br/>(Spring Cloud Gateway :8080)"]
        Nacos["⚙️ Nacos<br/>注册中心 / 配置中心"]
        MQ["📨 RabbitMQ<br/>消息队列"]
        Redis["⚡ Redis<br/>缓存 / 分布式锁 / Lua"]
        XXL["⏰ XXL-Job<br/>分布式任务调度"]
        DB["🗄️ MySQL × 5<br/>用户库 / 商品库 / 订单库 / 支付库 / 通知库"]
    end

    subgraph 微服务
        User["👤 e-user :8081<br/>用户 / 认证 / 地址"]
        Product["📦 e-product :8082<br/>商品 / SKU / 秒杀 / 店铺"]
        Order["📋 e-order :8083<br/>订单 / 购物车 / 优惠券"]
        Notif["🔔 e-notification :8084<br/>消息通知 / WebSocket"]
        File["📁 e-file :8085<br/>文件上传 / OSS"]
        Payment["💳 e-payment :8086<br/>支付宝 / 退款 / 对账"]
    end

    subgraph 共享模块
        API["🔗 e-api<br/>Feign 客户端"]
        Common["📚 e-common<br/>公共组件 / Redisson / 自动配置"]
    end

    U --> GW
    A --> GW
    GW --> User & Product & Order & Notif & File & Payment
    User & Product & Order & Notif & File & Payment --> Nacos
    User & Product & Order & Notif & Payment --> MQ
    User & Product & Order & Payment --> Redis
    Payment --> XXL
    User & Product & Order & Payment --> DB
    Order & Payment -.->|Feign| API
    API -.->|Feign| User & Product & Order
```

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot + Spring Cloud + Spring Cloud Alibaba | 2.7.12 / 2021.0.3 / 2021.0.1.0 |
| **网关** | Spring Cloud Gateway | 2021.0.3 |
| **注册/配置** | Nacos | 2.x |
| **数据库** | MySQL 8.0 + MyBatis Plus | 3.5.3.1 |
| **缓存** | Redis + Redisson（分布式锁） | 3.13.6 |
| **消息队列** | RabbitMQ（普通 / 延时 / 死信队列） | 3.x |
| **定时任务** | XXL-Job | 2.3.1 |
| **支付** | 支付宝开放平台 SDK（沙箱环境） | — |
| **API 文档** | Swagger / Knife4j | 3.0.3 |
| **前端** | Vue 3 + Vite + Pinia + TailwindCSS + Element Plus | — |
| **构建** | Maven | — |

---

## 项目规模

| 维度 | 数据 |
|------|------|
| Java 源文件 | 358 个 |
| Vue 页面 | 37 个（用户端 21 + 管理后台 16） |
| 数据库 | 5 个（user / product / order / payment / notification） |
| 数据表 | 18 张 |
| Redis Lua 脚本 | 10 个（秒杀 3 + 优惠券 2 + 订单库存 2 + 通知 3） |
| 微服务模块 | 9 个（6 业务 + Gateway + API + Common） |

---

## 模块结构

```
ecommerce/
├── ecommerceSystem/          # 后端微服务
│   ├── e-gateway/            # 🚪 API 网关（JWT 鉴权、路由转发、令牌桶限流、CORS）
│   ├── e-user/               # 👤 用户服务（注册/登录、角色管理、收货地址）
│   ├── e-product/            # 📦 商品服务（SPU/SKU、分类、秒杀活动、店铺管理）
│   ├── e-order/              # 📋 订单服务（下单、购物车、优惠券领取与使用）
│   ├── e-payment/            # 💳 支付服务（支付宝支付、退款、XXL-Job 对账）
│   ├── e-notification/       # 🔔 通知服务（订单通知、系统消息、WebSocket 推送）
│   ├── e-file/               # 📁 文件服务（图片上传、OSS/COS 存储）
│   ├── e-api/                # 🔗 Feign 接口定义（跨服务调用契约）
│   └── e-common/             # 📚 公共模块（Redisson、全局异常、工具类、MQ 常量）
├── frontend/                 # 🛒 用户端（买家商城 + 商家后台）
├── admin-frontend/           # 🛠️ 管理后台（平台管理员）
└── docs/
    └── API_and_DB_Design.md  # 接口与数据库设计文档（84 个接口）
```

---

## 核心功能

### 🛒 用户端（买家）
- 商品浏览、搜索、分类筛选
- 多规格 SKU 选择（PC 端内联 + 移动端底部弹出面板）
- 购物车管理 + 直接购买
- 支付宝支付（PC 网页支付）
- 订单全生命周期（待支付 → 已支付 → 已发货 → 已完成）
- 订单退款（退款中 → 已退款）
- 优惠券领取与使用
- 个人中心（收货地址、资料编辑、密码修改）
- 申请开店

### 🏪 用户端（商家后台）
- 商品管理（上架/下架/编辑/多规格 SKU）
- 订单管理（发货、筛选）
- 秒杀活动报名
- 实时通知（WebSocket 推送新订单/支付/取消）
- 销售数据概览

### ⚙️ 管理后台
- 商家入驻审批
- 用户管理（封禁/解封）
- 秒杀审核与场次管理
- 全平台商品/订单管理
- 优惠券创建与发放
- 全站广播通知

### ⚡ 高并发设计亮点

**秒杀系统**
- Redis Lua 原子扣减库存（校验 + 扣减 + 限购一次完成）
- RabbitMQ 异步下单削峰
- Redisson 分布式锁防重复下单
- DB 乐观锁 CAS（`WHERE stock ≥ quantity`）双保险
- XXL-Job 定时对账修复 Redis-DB 差异
- 库存预热 + 独立库存池（秒杀 / 普通隔离）

**优惠券系统**
- Redis Lua 原子领券（库存检查 + 状态校验 + 单人限领 + 扣减 + 计数）
- MQ 异步落库 + 失败双阶段回滚（MQ 失败回滚 Redis → DB 失败再次回滚）
- DB 乐观锁双保险（`WHERE claimed_count < total_count`）
- Pipeline 批量读取可用券列表
- 缓存防穿透（`_empty` 标记）
- XXL-Job 双向对账（Redis 丢失从 DB 恢复，DB 偏差以 Redis 为准）
- 取消/超时/退款 三条路径自动退券

**支付体系**
- 三级确认：异步通知验签 → 主动查询 API → XXL-Job 定时对账
- PROCESSING 标记模式：退款前落库标记处理中，API 异常立即主动查询

**通知系统**（v2.0 重构）
- 双表架构：`e_notification`（内容）+ `e_notification_read`（每店铺已读状态）
- 三 Lua 脚本原子维护未读计数（INCR+EXPIRE / DECR+清理 / 批量广播）
- WebSocket STOMP 实时推送（按店铺 + 广播双通道）
- 双重去重（订单通知 / 管理员通知 / 广播分别去重策略）

**数据一致性四层保障**
1. **Redis Lua** — 原子操作，消除 TOCTOU 竞态
2. **RabbitMQ 异步削峰** — 热路径与慢 IO 解耦
3. **死信队列 DLQ 补偿** — 消费失败自动重试
4. **XXL-Job 定时对账** — 最终一致性兜底

### 🔐 安全
- JWT RS256 无状态鉴权（Gateway 全局过滤器）
- BCrypt 密码加密
- 接口级角色权限控制（用户 / 商家 / 管理员）
- Feign 调用上下文透传

---

## 快速启动

### 环境要求
- JDK 11+
- Maven 3.6+
- MySQL 8.0
- Redis 6+
- RabbitMQ 3.x
- Nacos 2.x
- XXL-Job 2.3.x

### 1. 启动基础设施

确保 MySQL、Redis、RabbitMQ、Nacos、XXL-Job 均已启动。

### 2. 导入 Nacos 配置

将 Nacos 共享配置导入控制台：
- `shared-spring.yaml` — Spring 通用配置
- `shared-mybatis.yaml` — MyBatis 数据源
- `shared-redis.yaml` — Redis / Redisson
- `shared-mq.yaml` — RabbitMQ
- `shared-xxljob.yaml` — XXL-Job
- `shared-logs.yaml` — 日志

### 3. 初始化数据库

创建 5 个数据库并执行建表脚本：
- `ecommerce_user` — 用户、地址
- `ecommerce_product` — 商品、分类、SKU、秒杀、店铺
- `ecommerce_order` — 订单、购物车、优惠券
- `ecommerce_payment` — 支付记录、退款记录
- `ecommerce_notification` — 通知、已读记录

详见 [`API_and_DB_Design.md`](./API_and_DB_Design.md)

### 4. 启动微服务

```bash
cd ecommerceSystem

# 先编译共享模块
mvn clean install -pl e-common,e-api -DskipTests

# 启动网关
mvn spring-boot:run -pl e-gateway

# 启动业务服务（无顺序依赖）
mvn spring-boot:run -pl e-user,e-product,e-order,e-payment,e-notification,e-file
```

### 5. 启动前端

```bash
# 用户端 (localhost:5173)
cd frontend && npm install && npm run dev

# 管理后台 (localhost:5174)
cd admin-frontend && npm install && npm run dev
```

---

## API 文档

启动各服务后，访问 Swagger 文档：
- 用户服务: `http://localhost:8081/doc.html`
- 商品服务: `http://localhost:8082/doc.html`
- 订单服务: `http://localhost:8083/doc.html`
- 支付服务: `http://localhost:8086/doc.html`

完整接口文档见 [`API_and_DB_Design.md`](./API_and_DB_Design.md)，包含 84 个接口及响应示例。

---

## 更新日志

### v2.0 — 多多商城
- **新增** 优惠券系统（Lua 原子领券 + MQ 异步 + 双向对账）
- **重构** 通知系统（双表已读隔离 + 三 Lua 脚本原子计数 + WebSocket 实时推送）
- **修复** 库存生命周期 7 条路径覆盖与对账纠偏
- **优化** 支付三级确认 + PROCESSING 标记模式
- **完善** 前端 37 页面、API 文档 84 接口、10 个 Lua 脚本

### v1.0 — 初始版本
- 微服务基础架构搭建
- 用户 / 商品 / 订单 / 支付核心流程
- 秒杀高并发设计
- Docker 中间件一键部署

---

## 项目截图

### 用户端（买家 + 商家）

| | | |
|:---:|:---:|:---:|
| ![首页](screenshots/用户端效果图/13.用户购物界面.png) | ![商品详情](screenshots/用户端效果图/01.商家首页预览.png) | ![购物车](screenshots/用户端效果图/09.用户购物车.png) |
| 商城首页 | 商品浏览 | 购物车 |
| ![下单](screenshots/用户端效果图/15.用户下单后实时websocket通知商家效果图.png) | ![支付](screenshots/用户端效果图/14.用户跳转支付宝沙箱支付.png) | ![订单](screenshots/用户端效果图/10.用户我的订单后台模块.png) |
| 下单 + 实时通知 | 支付宝支付 | 我的订单 |
| ![退款](screenshots/用户端效果图/12.用户申请退款.png) | ![秒杀](screenshots/用户端效果图/07.用户限时秒杀.png) | ![优惠券](screenshots/用户端效果图/08.用户优惠卷中心.png) |
| 申请退款 | 限时秒杀 | 优惠券中心 |

**商家后台：**

| | | |
|:---:|:---:|:---:|
| ![商家后台](screenshots/用户端效果图/02.商家管理后台.png) | ![商品管理](screenshots/用户端效果图/06.商家商品管理.png) | ![订单管理](screenshots/用户端效果图/05.商家订单管理.png) |
| 管理后台首页 | 商品管理 | 订单管理 |
| ![秒杀报名](screenshots/用户端效果图/03.商家秒杀报名.png) | ![通知](screenshots/用户端效果图/04.商家信息通知管理.png) | ![订单详情](screenshots/用户端效果图/11.用户订单详情.png) |
| 秒杀报名 | 实时通知 | 订单详情 |

### 管理后台（平台管理员）

| | | |
|:---:|:---:|:---:|
| ![仪表盘](screenshots/管理端效果图/01.后台仪表盘总览.png) | ![商家审批](screenshots/管理端效果图/02.商家审批界面.png) | ![店铺管理](screenshots/管理端效果图/03.店铺操作.png) |
| 仪表盘总览 | 商家审批 | 店铺管理 |
| ![用户管理](screenshots/管理端效果图/06.用户管理.png) | ![秒杀审核](screenshots/管理端效果图/05.秒杀审核管理.png) | ![秒杀场次](screenshots/管理端效果图/04.秒杀场次管理.png) |
| 用户管理 | 秒杀审核 | 秒杀场次 |
| ![优惠券](screenshots/管理端效果图/07.优惠卷管理.png) | ![通知](screenshots/管理端效果图/08.通知管理.png) | ![订单](screenshots/管理端效果图/09.订单管理.png) |
| 优惠券管理 | 通知管理 | 订单管理 |
| ![销量排行](screenshots/管理端效果图/10.销量排行.png) |
| 销量排行 |

---

## License

MIT License
