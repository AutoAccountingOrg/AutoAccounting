感谢您愿意贡献时间和精力！

我们鼓励并珍视各种形式的贡献。在开始之前，请务必阅读相关部分，以确保为维护者和所有参与者提供最佳的体验。我们期待您的贡献！

> 如果您喜欢这个项目但无法亲自参与，也可以通过以下简单方式支持我们：
> - 点亮 Star
> - 在社交媒体上分享
> - 在您的项目中引用本项目
> - 向朋友或同事推荐

## 行为准则

自动记账是一款纯粹的自动化记账插件，遵循GPLv3授权，禁止涉及政治或侵权的行为。

## 我有一个问题

> 请在提问前先阅读[文档](https://auto.ankio.net)。

提问前，请先搜索现有的[问题](/issues)，看看是否已有相关内容。如果找到类似问题但仍需澄清，您可以在该问题下留言。我们建议您也先在互联网上搜索答案。

如果仍需提问，请：

- 新建一个[问题](/issues/new)。
- 尽可能详细描述问题的上下文。
- 提供项目和平台版本（如Xposed版本、日志等）。

我们会尽快回复。

## 我想要贡献

### 报告错误

#### 提交错误报告前

为了帮助我们尽快修复问题，请确保错误报告详细且信息充分。请提前完成以下步骤：

- 使用最新版本（**持续构建版**）。
- 确认错误确实存在，而非因不兼容环境引起。
- 查看是否已有类似的错误报告，或在[bug 跟踪器](issues?q=label%3Abug)中查看。
- 在互联网上搜索其他讨论。
- 收集相关信息：
  - Xposed版本、日志
  - 自动记账版本、日志
  - Hook的应用版本（如能，请提供安装包）。
  - 具体操作步骤（哪一步出错了？）
  - 是否可以稳定复现问题？是否在旧版本中重现？

#### 如何提交有效的错误报告？

> [!IMPORTANT]
> 请勿将安全问题、漏洞或敏感信息的 bug 公开发布。此类问题请发送邮件至 <ankio@ankio.net>。

我们通过 GitHub 问题跟踪错误。如果遇到问题，请：

- 新建一个[问题](/issues/new)。
- 说明预期行为与实际行为的差异。
- 提供尽可能多的上下文信息，并描述重现步骤。
- 提交您收集的相关信息。

提交后：

- 项目团队会为问题贴上标签。
- 团队成员将尝试使用您提供的步骤重现问题。如果无法重现，团队将要求您提供更多信息，并标记为 `needs-repro`。带有 `needs-repro` 标签的问题将不会被解决，直到它们被重现。
- 如果问题可复现，将标记为 `needs-fix` 并分配相关标签。

### 提建议

如果您有增强建议（新功能或改进），请遵循以下步骤：

#### 提交建议前

- 使用最新版本（**持续构建版**）。
- 仔细阅读[文档](https://auto.ankio.net)，确认功能尚未覆盖。
- 搜索现有建议，如有类似问题，请评论而非新开问题。
- 查看我们的[开发计划](https://github.com/orgs/AutoAccountingOrg/projects/1/views/2)。
- 确认建议符合项目的范围和目标，并提供有力的论据。

#### 如何提交有效的增强建议？

增强建议应通过[GitHub 问题](/issues)进行跟踪。

- 使用**清晰的标题**描述建议。
- 提供**详细的建议步骤说明**。
- **描述当前行为**，**解释您希望的改进**及其原因。
- 如果有助于说明，您可以**包含截图或GIF**。
- **解释为什么该建议对大多数用户有用**，并指出可能的替代解决方案。

### 代码贡献指南

我们欢迎您为自动记账贡献代码。以下是一些指导建议：

- 自动记账主体使用Kotlin编写，并使用Gradle构建。
- UI部分采用Material You设计风格, 请参考[Material Design](https://material.io/design)。

如果您不熟悉这些工具，请先阅读相关文档。

#### 代码规范

- 确保代码符合我们的规范。使用`./gradlew ktlintFormat`格式化代码，`./gradlew detekt`检查代码质量。

#### 提交代码

> 我们推荐使用：[gitmoji](https://gitmoji.dev/)规范提交信息。
> 推荐使用[AI-Git-Commit](https://plugins.jetbrains.com/plugin/24851-ai-git-commit) 自动生成提交信息（区域：美国），自定义提示词请使用根目录下`commit-prompt.txt`文件内容。 
> 如果无法使用AI-Git-Commit插件，也可以使用[gitmoji-intellij-plugin](https://github.com/AnkioTomas/gitmoji-intellij-plugin/releases/tag/v1.14.0)来减少Emoji记忆。

commit格式采用： :[emoji]: [更改内容] #关联issue

例如修复issue(#1)：

```shell
git commit -m ":bug: 修复xx问题 #1"
```

**注意：**
- emoji与提交文本之间需要有一个空格
- 每条commit信息只允许包含一个关联issue

可参考以下emoji列表：

- `:art:` : 改进代码结构/格式。
- `:zap:` : 提升性能。
- `:fire:` : 删除代码或文件。
- `:bug:` : 修复错误。
- `:ambulance:` : 关键热修复。
- `:sparkles:` : 引入新功能。
- `:memo:` : 添加或更新文档。
- `:rocket:` : 部署内容。
- `:lipstick:` : 添加或更新 UI 和样式文件。
- `:tada:` : 开始一个项目。
- `:white_check_mark:` : 添加、更新或通过测试。
- `:lock:` : 修复安全或隐私问题。
- `:closed_lock_with_key:` : 添加或更新秘密。
- `:bookmark:` : 发布/版本标签。
- `:rotating_light:` : 修复编译器/代码检查警告。
- `:construction:` : 进行中的工作。
- `:green_heart:` : 修复 CI 构建。
- `:arrow_down:` : 降级依赖。
- `:arrow_up:` : 升级依赖。
- `:pushpin:` : 将依赖固定到特定版本。
- `:construction_worker:` : 添加或更新 CI 构建系统。
- `:chart_with_upwards_trend:` : 添加或更新分析或跟踪代码。
- `:recycle:` : 重构代码。
- `:heavy_plus_sign:` : 添加依赖。
- `:heavy_minus_sign:` : 移除依赖。
- `:wrench:` : 添加或更新配置文件。
- `:hammer:` : 添加或更新开发脚本。
- `:globe_with_meridians:` : 国际化和本地化。
- `:pencil2:` : 修复拼写错误。
- `:poop:` : 编写需要改进的坏代码。
- `:rewind:` : 还原变更。
- `:twisted_rightwards_arrows:` : 合并分支。
- `:package:` : 添加或更新编译文件或包。
- `:alien:` : 更新因外部 API 变更的代码。
- `:truck:` : 移动或重命名资源（如文件、路径、路由）。
- `:page_facing_up:` : 添加或更新许可证。
- `:boom:` : 引入重大变更。
- `:bento:` : 添加或更新资产。
- `:wheelchair:` : 提升无障碍性。
- `:bulb:` : 在源代码中添加或更新注释。
- `:beers:` : 酒后编写代码。
- `:speech_balloon:` : 添加或更新文本和文字。
- `:card_file_box:` : 执行数据库相关更改。
- `:loud_sound:` : 添加或更新日志。
- `:mute:` : 移除日志。
- `:busts_in_silhouette:` : 添加或更新贡献者。
- `:children_crossing:` : 改善用户体验/可用性。
- `:building_construction:` : 进行架构变更。
- `:iphone:` : 从事响应式设计。
- `:clown_face:` : 模拟事物。
- `:egg:` : 添加或更新复活节彩蛋。
- `:see_no_evil:` : 添加或更新 .gitignore 文件。
- `:camera_flash:` : 添加或更新快照。
- `:alembic:` : 进行实验。
- `:mag:` : 提升 SEO。
- `:label:` : 添加或更新类型。
- `:seedling:` : 添加或更新种子文件。
- `:triangular_flag_on_post:` : 添加、更新或移除功能标志。
- `:goal_net:` : 捕获错误。
- `:dizzy:` : 添加或更新动画和过渡。
- `:wastebasket:` : 弃用需要清理的代码。
- `:passport_control:` : 处理与授权、角色和权限相关的代码。
- `:adhesive_bandage:` : 非关键问题的简单修复。
- `:monocle_face:` : 数据探索/检查。
- `:coffin:` : 删除无用代码。
- `:test_tube:` : 添加失败的测试。
- `:necktie:` : 添加或更新业务逻辑。
- `:stethoscope:` : 添加或更新健康检查。
- `:bricks:` : 基础设施相关变更。
- `:technologist:` : 改善开发者体验。
- `:money_with_wings:` : 添加赞助或资金相关的基础设施。
- `:thread:` : 添加或更新与多线程或并发相关的代码。
- `:safety_vest:` : 添加或更新验证相关的代码。

