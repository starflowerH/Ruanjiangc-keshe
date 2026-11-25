# 招生宣传系统 - 微信小程序前端

基于 uni-app + Vue 3 开发的招生宣传系统微信小程序前端。

## 项目结构

```
src/
├── api/                    # API 接口模块
│   ├── index.js           # API 基础配置和请求封装
│   ├── auth.js            # 认证相关 API
│   ├── enrollment.js      # 报名相关 API
│   ├── team.js            # 队伍相关 API
│   ├── university.js      # 高校信息 API
│   └── qa.js              # 问答相关 API
├── components/             # 公共组件
│   ├── NavBar.vue         # 自定义导航栏
│   ├── Empty.vue          # 空状态组件
│   ├── UniversityCard.vue # 高校卡片
│   ├── StudentCard.vue    # 学生卡片
│   └── QuestionItem.vue   # 问答条目
├── pages/                  # 页面
│   ├── index/             # 启动页（角色选择）
│   ├── common/            # 公共页面（登录、高校详情、问答详情）
│   ├── student/           # 高中生页面
│   │   ├── home.vue       # 首页
│   │   ├── university-list.vue  # 高校列表
│   │   ├── ask-question.vue     # 提问
│   │   ├── my-questions.vue     # 我的提问
│   │   └── qa-detail.vue        # 问答详情
│   └── volunteer/         # 大学生页面
│       ├── home.vue       # 首页
│       ├── register.vue   # 报名
│       ├── profile.vue    # 个人中心
│       ├── team-view.vue  # 队伍查看
│       ├── qa-center.vue  # 答疑中心
│       └── answer-question.vue  # 回答问题
├── store/                  # Pinia 状态管理
│   ├── index.js
│   └── modules/
│       ├── user.js        # 用户状态
│       └── app.js         # 应用状态
├── utils/                  # 工具函数
│   ├── request.js         # HTTP 请求封装
│   ├── auth.js            # 认证工具
│   └── storage.js         # 本地存储工具
├── static/                 # 静态资源
│   └── images/            # 图片资源
├── App.vue                # 应用入口
├── main.js                # 主入口
├── pages.json             # 页面配置
├── manifest.json          # 应用配置
└── uni.scss               # 全局样式变量
```

## 功能模块

### 1. 高中生端
- **首页**：展示到本校宣传的大学列表和学长学姐
- **高校信息**：浏览各高校简介、专业、分数线
- **在线提问**：向学长学姐提交问题
- **我的提问**：查看提问记录和回答

### 2. 大学生端（志愿者）
- **首页**：报名状态、待回答问题
- **志愿者报名**：填写报名信息、选择目标高中
- **队伍查看**：查看同一目标高中的其他大学队伍
- **答疑中心**：回答高中生的提问
- **个人中心**：个人信息、功能入口

## 开发环境

### 前置条件
- Node.js >= 16
- 微信开发者工具
- HBuilderX（推荐）

### 安装依赖
```bash
npm install
```

### 开发运行
```bash
# 微信小程序
npm run dev:mp-weixin
```

### 构建打包
```bash
npm run build:mp-weixin
```

## 配置说明

### 1. API 地址配置
编辑 `src/api/index.js`，修改 `BASE_URL` 为实际的后端 API 地址。

### 2. 微信小程序 AppID
编辑 `src/manifest.json`，填入你的微信小程序 AppID。

### 3. TabBar 图标
在 `src/static/images/` 目录下放置以下图标文件：
- `tab-home.png` / `tab-home-active.png`
- `tab-university.png` / `tab-university-active.png`
- `tab-qa.png` / `tab-qa-active.png`
- `tab-profile.png` / `tab-profile-active.png`

图标建议尺寸：81x81 像素，PNG 格式。

### 4. 默认头像
在 `src/static/images/` 目录下放置：
- `default-avatar.png` - 默认用户头像
- `default-uni.png` - 默认大学 Logo

## 后端 API 接口

项目需要以下后端 API 支持：

### 认证模块
- `POST /auth/wx-login` - 微信登录
- `POST /auth/student/register` - 高中生注册
- `GET /auth/profile` - 获取用户信息

### 报名模块
- `GET /enrollment/universities` - 获取大学列表
- `GET /enrollment/high-schools` - 获取高中名册
- `POST /enrollment/apply` - 提交报名
- `GET /enrollment/my` - 查询我的报名状态

### 队伍模块
- `GET /team/my` - 获取我的队伍
- `GET /team/by-high-school/:id` - 按高中获取队伍

### 高校信息模块
- `GET /university/list` - 高校列表
- `GET /university/:id` - 高校详情
- `GET /university/by-high-school/:id` - 按高中获取高校

### 问答模块
- `POST /qa/ask` - 提交问题
- `GET /qa/my-questions` - 我的提问
- `GET /qa/received` - 收到的问题
- `POST /qa/answer` - 回答问题
- `GET /qa/:id` - 问答详情
- `GET /qa/featured` - 精选问答

## 开发说明

### 模拟数据
当前项目包含模拟数据，在 API 请求失败时会自动使用模拟数据进行开发调试。实际开发时请对接真实后端 API。

### 状态管理
使用 Pinia 进行状态管理，主要包含：
- `userStore`：用户信息、登录状态、角色
- `appStore`：系统信息、选中的高中

### 样式规范
- 使用 rpx 单位适配不同屏幕
- 主题色：#4A90D9
- 全局样式变量定义在 `uni.scss`

## 注意事项

1. 首次使用需要在微信开发者工具中导入项目
2. 确保微信小程序已配置合法域名
3. 模板消息功能需要后端配置公众号模板
4. 真机调试需要开启微信开发者工具的「不校验合法域名」选项
