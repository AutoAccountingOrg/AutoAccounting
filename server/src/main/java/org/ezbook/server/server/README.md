# 服务端路由模块文档

## 概述

本模块包含了自动记账应用的所有API路由定义，采用了模块化设计，每个功能域都有独立的路由文件。

## 路由文件列表

### 核心路由

- **BaseRoutes.kt** - 应用基础信息和健康检查
- **JsRoutes.kt** - JavaScript执行和智能账单分析（核心功能）

### 数据管理路由

- **LogRoutes.kt** - 系统日志管理
- **DataRoutes.kt** - 应用原始数据管理
- **SettingRoutes.kt** - 系统设置管理
- **DatabaseRoutes.kt** - 数据库备份与恢复

### 规则引擎路由

- **RuleRoutes.kt** - 账单解析规则管理

### 资产管理路由

- **AssetsRoutes.kt** - 资产账户管理
- **AssetsMapRoutes.kt** - 资产名称映射
- **BookRoutes.kt** - 记账账本管理

### 分类管理路由

- **CategoryRoutes.kt** - 收支分类管理
- **CategoryMapRoutes.kt** - 分类名称映射
- **CategoryRuleRoutes.kt** - 自动分类规则

### 账单管理路由

- **BillRoutes.kt** - 账单完整生命周期管理

### AI集成路由

- **AiApiRoutes.kt** - AI服务集成接口

## 设计特点

### 1. 统一的响应格式

所有API都使用统一的 `ResultModel` 响应格式：

```kotlin
ResultModel(code: Int, message: String, data: Any? = null)
```

### 2. 完整的文档注释

每个路由函数都包含：

- 功能描述
- 参数说明
- 返回值说明
- 使用示例

### 3. 一致的错误处理

- 参数验证
- 异常捕获
- 友好的错误信息

### 4. 代码简化

- 使用 Kotlin 扩展函数简化逻辑
- 统一的分页参数处理
- 智能的插入/更新判断

## 主要功能模块

### 智能账单分析 (JsRoutes)

这是系统的核心功能，支持：

- 规则引擎解析
- AI智能分析
- 自动分类
- 资产映射
- 状态管理

### 数据同步机制

支持与外部系统的数据同步：

- MD5校验确保数据完整性
- 批量数据更新
- 增量同步支持

### 分页查询

统一的分页机制：

- page: 页码（从1开始）
- limit: 每页条数
- 支持搜索和筛选

## 使用示例

### 获取账单列表

```http
POST /bill/list
Content-Type: application/x-www-form-urlencoded

page=1&limit=20&type=Edited,Synced
```

### 智能账单分析

```http
POST /js/analysis
Content-Type: text/plain

app=com.tencent.mm&type=Notification&ai=false

[原始通知数据]
```

### 获取设置值

```http
POST /setting/get?key=USE_AI
```

## 开发指南

### 添加新路由

1. 创建新的路由文件（如 `NewFeatureRoutes.kt`）
2. 定义路由扩展函数
3. 在 `ServerApplication.kt` 中注册路由
4. 添加完整的文档注释

### 代码规范

- 使用有意义的变量名
- 添加详细的文档注释
- 统一的错误处理
- 遵循 Kotlin 编码规范

## 性能优化

### 数据库查询优化

- 自动清理过期数据
- 分页查询避免大量数据加载
- 合理使用索引

### 缓存策略

- 设置数据缓存
- 规则引擎结果缓存
- AI分析结果缓存

## 安全考虑

### 数据验证

- 参数类型检查
- 数据范围验证
- SQL注入防护

### 错误信息

- 不暴露敏感信息
- 统一的错误响应格式
- 详细的日志记录 