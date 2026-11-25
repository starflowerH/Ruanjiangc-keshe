# 高校招生宣传小程序（前端 + 后端全栈源码）

面向高校招生宣传与高三考生升学咨询场景的四角色微信小程序系统，包含 **uni-app + Vue 3 微信小程序前端** 与 **Spring Boot 3.2 + MyBatis-Plus + MySQL 8.0 后端服务**。

---

## 目录结构

```text
.
├── src/                                  # 微信小程序前端源码（uni-app + Vue 3）
│   ├── api/                              # 按业务模块封装的接口请求
│   ├── components/                       # 公共组件与原型组件（高校/高中搜索、卡片、导航等）
│   ├── config/env.js                     # 接口 BASE_URL 环境配置
│   ├── pages/
│   │   ├── index/                        # 启动页与角色入口
│   │   ├── common/                       # 通用页面（登录、高校详情、问答详情、个人资料、处罚记录等）
│   │   ├── student/                      # 高中生端（高校列表、问答广场、提问、通知、个人中心）
│   │   ├── volunteer/                    # 大学生志愿者端（报名、队伍协作、队长通讯录、答疑、论坛审核）
│   │   ├── teacher/                      # 大学老师端（工作台、宣传资料维护、队伍管理）
│   │   └── admin/                        # 管理员端（运营看板、审核管理、教师授权、列表管理）
│   └── store/                            # Pinia 状态管理
├── static/                               # 前端静态资源
├── backend/                              # Java 后端服务源码
│   ├── pom.xml                           # Maven 依赖配置（Java 17 + Spring Boot 3.2.0）
│   ├── seed_data.sql                     # 演示超管及高校/高中基础种子数据
│   └── src/
│       ├── main/java/com/demo/miniprogram/
│       │   ├── common/                   # 统一响应 Result、全局异常处理与常量
│       │   ├── config/                   # SecurityConfig、JwtAuthFilter、MyBatisPlusConfig 等配置
│       │   ├── controller/               # 四角色业务控制器（认证、高校、志愿者、队伍、问答、治理、管理端）
│       │   ├── dto/                      # 请求参数校验 DTO
│       │   ├── entity/                   # 数据库实体映射
│       │   ├── mapper/                   # MyBatis-Plus Mapper 接口
│       │   ├── security/                 # 权限注解与切面、XSS 过滤、限流拦截、密码校验
│       │   ├── service/                  # 核心业务逻辑（组队、风控、问答、教师分权等）
│       │   ├── util/                     # JWT 工具等辅助类
│       │   └── vo/                       # 视图返回对象
│       ├── main/resources/
│       │   ├── application.yml           # 应用配置（敏感凭据均通过环境变量注入）
│       │   ├── schema.sql                # 核心数据库表结构
│       │   ├── db/                       # 增量数据库变更脚本
│       │   └── static/logo/              # 高校校徽静态图标资源
│       └── test/                         # 后端 JUnit 测试类（11 个）
├── tests/                                # 前端规则与契约检查脚本（13 个）
├── scripts/                              # 图标生成、数据整理与小程序上传辅助脚本
├── package.json                          # 前端依赖与构建脚本
├── vite.config.js                        # Vite 构建配置
└── project.config.json                   # 微信小程序工程配置
```

---

## 技术栈

- **前端**：Vue 3 (`^3.4.21`)、uni-app (`3.0.0`)、Pinia (`^2.1.7`)、Vite (`^5.2.8`)、Sass
- **后端**：Java 17、Spring Boot 3.2.0、Spring Security、Spring AOP、MyBatis-Plus 3.5.5、JWT (`jjwt 0.12.3`)、Hutool 5.8.24、Knife4j 4.4.0
- **数据库**：MySQL 8.0（表结构见 [`backend/src/main/resources/schema.sql`](backend/src/main/resources/schema.sql) 与 [`backend/src/main/resources/db/`](backend/src/main/resources/db/)）

---

## 四角色核心功能

1. **高中生端**：免登录浏览高校列表（按最近年份最低录取位次排序）与历年分数线、搜索标准高中名册绑定生源校、向同校学长一对一私密提问、在公开问答广场发帖交流与举报。
2. **大学生志愿者端**：高校动态报名、自由创建/加入或配合教师自动组队、队长移交与退队闭环、答疑中心回复提问、参与校级论坛内容审核与举报处理。
3. **大学老师端**：基于“报名审核 / 组队管理 / 宣传资料 / 论坛治理”四项动态权限管理本校志愿者与宣传队伍，并与本校授权志愿者协同维护高校宣传图文。
4. **管理员端**：统筹审核大学生报名、组队及论坛举报申诉，动态控制大学老师权限开关，查看已审核生效业务统计数据。

---

## 本地启动说明

### 1. 初始化数据库

创建 MySQL 数据库（默认库名 `miniprogram`，字符集 `utf8mb4`），依次导入建表脚本、增量脚本与基础种子数据：

- [`backend/src/main/resources/schema.sql`](backend/src/main/resources/schema.sql)
- [`backend/src/main/resources/db/`](backend/src/main/resources/db/) 下的增量 SQL 脚本
- [`backend/seed_data.sql`](backend/seed_data.sql)（可选基础名册与默认测试超管账号）

### 2. 启动后端服务（`backend/`）

后端默认监听 **`8090`** 端口，敏感配置在 [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml) 中通过环境变量注入，启动前请先配置环境变量：

| 环境变量 | 说明 | 默认回退 / 示例 |
| :--- | :--- | :--- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 连接地址、端口、库名 | `localhost` / `3306` / `miniprogram` |
| `DB_USER` / `DB_PASSWORD` | 数据库账号与密码 | `miniprogram` / 必填 |
| `JWT_SECRET` | JWT 签名密钥（建议 ≥ 256 位） | 必填 |
| `WX_MINIAAPP_SECRET` | 微信小程序 AppSecret | 必填 |
| `SENTRY_DSN` | 错误监控配置（可选） | 留空即可 |

进入 `backend/` 目录运行：

```bash
cd backend
mvn spring-boot:run
```

启动后可访问 Swagger / Knife4j 接口文档：`http://localhost:8090/swagger-ui.html`

### 3. 启动前端小程序

1. 安装依赖（Node.js >= 16）：

```bash
npm install
```

2. 修改 [`src/config/env.js`](src/config/env.js) 中的 `BASE_URL` 为你的后端地址（如本地调试地址 `http://localhost:8090` 或部署域名）。
3. 编译运行微信小程序开发包，并在微信开发者工具中导入生成目录：

```bash
# 开发模式
npm run dev:mp-weixin

# 生产构建
npm run build:mp-weixin
```

---

## 测试

- **后端测试**：进入 `backend/` 执行 `mvn test`（包含 `backend/src/test/` 下 11 个测试类）。
- **前端规则/契约检查**：在仓库根目录执行 `node --test tests/*.mjs` 或 `node --test tests/*.cjs`（包含 `tests/` 下 13 个检查脚本）。
