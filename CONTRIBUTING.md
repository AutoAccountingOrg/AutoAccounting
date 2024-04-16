
首先，感谢您抽出时间来贡献！

我们鼓励和重视各种类型的贡献。在贡献之前，请确保阅读相关部分。这将使我们这些维护者的工作变得更容易，也会让所有参与者的体验更加顺畅。社区期待您的贡献。

> 如果您喜欢这个项目，但没有时间贡献，那也没关系。还有其他简单的方式来支持该项目并表示您的赞赏，我们也会非常高兴：
> - 给项目点个Star
> - 在 社交媒体 上分享
> - 在您项目的 readme 中引用该项目
> - 推广该项目并告诉您的朋友/同事


## 行为准则

自动记账是一个纯粹的自动化记账插件，采用GPLV3授权使用，禁止涉及政治、侵权等行为。

## 我有一个问题

> 如果您想要提问，我们假设您已阅读了可用的[文档](https://auto.ankio.net)。

在您提问之前，最好先搜索现有的[问题](/issues)，看看是否有相关的问题可以帮助您。如果您找到了合适的问题但仍需要澄清，您可以在这个问题中写下您的问题。此外，建议您先在互联网上搜索答案。

如果您仍然觉得有必要提问并需要澄清，我们建议您：

- 打开一个[问题](/issues/new)。
- 尽可能详细地提供有关您遇到的问题的上下文。
- 提供项目和平台版本（Xposed版本、日志等信息）。

我们会尽快处理这个问题。

## 我想要贡献

### 报告错误

#### 提交错误报告前

一个好的错误报告不应该让其他人需要追问您以获取更多信息。因此，我们要求您仔细调查、收集信息，并详细描述您的报告中的问题。请提前完成以下步骤，以帮助我们尽快修复任何潜在的错误。

- 确保您正在使用最新版本（**持续构建版**）。
- 确定您的错误是否真的是一个错误，而不是您自己的错误，例如使用不兼容的环境（确保您已阅读了[文档](https://auto.ankio.net)。
- 查看是否已经存在您的错误或错误的 bug 报告，以查看其他用户是否已经遇到（并且可能已经解决）您遇到的相同问题，在[bug 跟踪器](issues?q=label%3Abug)中是否已经存在您的 bug 或错误的报告。
- 也要确保搜索互联网（包括 Stack Overflow），看看 GitHub 社区之外的用户是否讨论了这个问题。
- 收集有关错误的信息：
    - Xposed版本、日志
    - 自动记账版本、日志（包括/sdcard/Android/data/net.ankio.auto.xposed/文件夹下的所有文件)
    - Hook的应用版本（如果可以请提供对应应用的安装包）。
    - 做过的操作（执行哪一步出错了？）
    - 您能够可靠地重现这个问题吗？您也可以使用旧版本重现吗？

#### 如何提交一个好的错误报告？

> 您绝不能将与安全相关的问题、漏洞或包含敏感信息的 bug 报告到问题跟踪器或其他公共地方。敏感的 bug 必须通过电子邮件发送给 <ankio@ankio.net>。

我们使用 GitHub 问题来跟踪 bug 和错误。如果您在项目中遇到问题：

- 打开一个[问题](/issues/new)。（因为我们目前无法确定这是否是一个 bug，请不要立即谈论 bug，也不要为问题贴上标签。）
- 解释您期望的行为和实际行为。
- 请提供尽可能多的上下文，并描述其他人可以按照的*重现步骤*。这通常包括您的代码。对于良好的 bug 报告，您应该隔离问题并创建一个简化的测试案例。
- 提供您在上一节中收集的信息。

提交后：

- 项目团队将相应地为问题贴上标签。
- 团队成员将尝试使用您提供的步骤重现问题。如果没有重现步骤或没有明 显的重现问题的方法，团队将要求您提供这些步骤，并将问题标记为 `needs-repro`。带有 `needs-repro` 标签的 bug 将不会得到解决，直到它们被重现。
- 如果团队能够重现问题，它将被标记为 `needs-fix`，以及可能的其他标签（如 `critical`），并且问题将留待[某人实现](#your-first-code-contribution)。

### 建议增强功能

本节指导您提交 自动记账 的增强建议，**包括全新功能和对现有功能的次要改进**。遵循这些准则将帮助维护者和社区了解您的建议并找到相关的建议。

#### 提交增强建议前

- 确保您正在使用最新版本（**持续构建版**）。
- 仔细阅读[文档](https://auto.ankio.net)，找出功能是否已经覆盖。
- 进行搜索，查看是否已经提出了增强建议。如果有，请在现有问题中添加评论，而不是新开一个问题。
- 查看我们的[开发计划](https://github.com/orgs/AutoAccountingOrg/projects/1/views/2)，判断你的建议是否已经包含在计划中。
- 弄清楚您的想法是否符合项目的范围和目标。您需要提出有力的论据来说服项目的开发者这一功能的优点。请记住，我们希望的功能是对大多数用户有用的，**而不仅仅是一小部分用户**。如果您只针对少数用户，请考虑编写一个附加/插件库。

#### 如何提交一个好的增强建议？

增强建议被跟踪为[GitHub 问题](/issues)。

- 对问题使用**清晰和描述性的标题**以识别建议。
- 提供**建议的详细步骤说明**，尽可能详细。
- **描述当前行为**并**解释您希望看到的行为**以及为什么。在这一点上，您还可以说明哪些替代方法对您无效。
- 您可能希望**包含截图和动画 GIF**，以帮助您演示步骤或指出建议相关的部分。您可以使用[这个工具](https://www.cockos.com/licecap/)在 macOS 和 Windows 上录制 GIF，以及[这个工具](https://github.com/colinkeenan/silentcast)或[这个工具](https://github.com/GNOME/byzanz)在 Linux 上录制。
- **解释为什么这个增强功能对大多数 自动记账 用户有用**。您还可以指出其他解决方案更好的项目，这些项目可以作为启发。

### 您的第一个代码贡献

我们鼓励您为自动记账贡献代码。这可能是您的第一个开源贡献，这是一个很好的开始。这是一些可以帮助您的指导：

自动记账本体采用Kotlin编写，使用Gradle构建。如果您不熟悉这些工具，我们建议您先阅读相关文档。

自动记账服务采用C++编写，涉及quickjs的部分为第三方库，如果您不熟悉C++，请勿修改C++部分。

#### 代码规范

- 请确保您的代码符合我们的代码规范。我们使用[ktlint](https://ktlint.github.io/)来强制执行代码规范。您可以使用`./gradlew ktlintFormat`来格式化您的代码。
- 我们使用[detekt](https://detekt.github.io/detekt/)来检查代码质量。您可以使用`./gradlew detekt`来检查您的代码。

#### 提交代码

commit格式采用： :[emoji]: [更改内容] #关联issue

例如修复issue(#1)：

```shell
git commit -m ":bug: 修复xx问题 #1"
```

**注意：**
- emoji(:)和提交文本之间需要有一个空格
- 一个commit信息只允许包含一个关联issue

emoji可以参考如下列表，也可以使用该插件[gitmoji-intellij-plugin](https://github.com/AnkioTomas/gitmoji-intellij-plugin/releases/tag/v1.14.0)：

| Emoji | Entity | Code | Description | Name | Semver |
|-------|--------|------|------------|------|--------|
| 🎨 | &#x1f3a8; | :art: | 改善代码的结构/格式。 | art |  |
| ⚡️ | &#x26a1; | :zap: | 提高性能。 | zap | patch |
| 🔥 | &#x1f525; | :fire: | 删除代码或文件。 | fire |  |
| 🐛 | &#x1f41b; | :bug: | 修复错误。 | bug | patch |
| 🚑️ | &#128657; | :ambulance: | 关键热修复。 | ambulance | patch |
| ✨ | &#x2728; | :sparkles: | 引入新功能。 | sparkles | minor |
| 📝 | &#x1f4dd; | :memo: | 添加或更新文档。 | memo |  |
| 🚀 | &#x1f680; | :rocket: | 部署代码。 | rocket |  |
| 💄 | &#xff99cc; | :lipstick: | 添加或更新 UI 和样式文件。 | lipstick | patch |
| 🎉 | &#127881; | :tada: | 开始一个新项目。 | tada |  |
| ✅ | &#x2705; | :white_check_mark: | 添加、更新或通过测试。 | white-check-mark |  |
| 🔒️ | &#x1f512; | :lock: | 修复安全问题。 | lock | patch |
| 🔐 | &#x1f510; | :closed_lock_with_key: | 添加或更新秘密信息。 | closed-lock-with-key |  |
| 🔖 | &#x1f516; | :bookmark: | 发布/版本标签。 | bookmark |  |
| 🚨 | &#x1f6a8; | :rotating_light: | 修复编译器/检查器警告。 | rotating-light |  |
| 🚧 | &#x1f6a7; | :construction: | 工作正在进行中。 | construction |  |
| 💚 | &#x1f49a; | :green_heart: | 修复 CI 构建。 | green-heart |  |
| ⬇️ | ⬇️ | :arrow_down: | 降级依赖项。 | arrow-down | patch |
| ⬆️ | ⬆️ | :arrow_up: | 升级依赖项。 | arrow-up | patch |
| 📌 | &#x1f4cc; | :pushpin: | 将依赖项固定到特定版本。 | pushpin | patch |
| 👷 | &#x1f477; | :construction_worker: | 添加或更新 CI 构建系统。 | construction-worker |  |
| 📈 | &#x1f4c8; | :chart_with_upwards_trend: | 添加或更新分析或代码跟踪。 | chart-with-upwards-trend | patch |
| ♻️ | &#x267b; | :recycle: | 重构代码。 | recycle |  |
| ➕ | &#10133; | :heavy_plus_sign: | 添加依赖项。 | heavy-plus-sign | patch |
| ➖ | &#10134; | :heavy_minus_sign: | 删除依赖项。 | heavy-minus-sign | patch |
| 🔧 | &#x1f527; | :wrench: | 添加或更新配置文件。 | wrench | patch |
| 🔨 | &#128296; | :hammer: | 添加或更新开发脚本。 | hammer |  |
| 🌐 | &#127760; | :globe_with_meridians: | 国际化和本地化。 | globe-with-meridians | patch |
| ✏️ | &#59161; | :pencil2: | 修复拼写错误。 | pencil2 | patch |
| 💩 | &#58613; | :poop: | 编写需要改进的糟糕代码。 | poop |  |
| ⏪️ | &#9194; | :rewind: | 撤销更改。 | rewind | patch |
| 🔀 | &#128256; | :twisted_rightwards_arrows: | 合并分支。 | twisted-rightwards-arrows |  |
| 📦️ | &#1F4E6; | :package: | 添加或更新已编译文件或软件包。 | package | patch |
| 👽️ | &#1F47D; | :alien: | 因外部 API 更改而更新代码。 | alien | patch |
| 🚚 | &#1F69A; | :truck: | 移动或重命名资源（例如：文件、路径、路由）。 | truck |  |
| 📄 | &#1F4C4; | :page_facing_up: | 添加或更新许可证。 | page-facing-up |  |
| 💥 | &#x1f4a5; | :boom: | 引入重大更改。 | boom | major |
| 🍱 | &#1F371 | :bento: | 添加或更新资源。 | bento | patch |
| ♿️ | &#9855; | :wheelchair: | 改善可访问性。 | wheelchair | patch |
| 💡 | &#128161; | :bulb: | 在源代码中添加或更新注释。 | bulb |  |
| 🍻 | &#x1f37b; | :beers: | 醉酒地编写代码。 | beers |  |
| 💬 | &#128172; | :speech_balloon: | 添加或更新文本和文字。 | speech-balloon | patch |
| 🗃️ | &#128451; | :card_file_box: | 执行与数据库相关的更改。 | card-file-box | patch |
| 🔊 | &#128266; | :loud_sound: | 添加或更新日志。 | loud-sound |  |
| 🔇 | &#128263; | :mute: | 删除日志。 | mute |  |
| 👥 | &#128101; | :busts_in_silhouette: | 添加或更新贡献者。 | busts-in-silhouette |  |
| 🚸 | &#128696; | :children_crossing: | 改善用户体验/可用性。 | children-crossing | patch |
| 🏗️ | &#1f3d7; | :building_construction: | 进行架构更改。 | building-construction |  |
| 📱 | &#128241; | :iphone: | 工作在响应式设计上。 | iphone | patch |
| 🤡 | &#129313; | :clown_face: | 嘲笑事物。 | clown-face |  |
| 🥚 | &#129370; | :egg: | 添加或更新彩蛋。 | egg | patch |
| 🙈 | &#8bdfe7; | :see_no_evil: | 添加或更新 .gitignore 文件。 | see-no-evil |  |
| 📸 | &#128248; | :camera_flash: | 添加或更新快照。 | camera-flash |  |
| ⚗️ | &#128248; | :alembic: | 进行实验。 | alembic | patch |
| 🔍️ | &#128269; | :mag: | 改善搜索引擎优化。 | mag | patch |
| 🏷️ | &#127991; | :label: | 添加或更新类型。 | label | patch |
| 🌱 | &#127793; | :seedling: | 添加或更新种子文件。 | seedling |  |
| 🚩 | &#x1F6A9; | :triangular_flag_on_post: | 添加、更新或删除功能标志。 | triangular-flag-on-post | patch |
| 🥅 | &#x1F945; | :goal_net: | 捕捉错误。 | goal-net | patch |
| 💫 | &#x1f4ab; | :dizzy: | 添加或更新动画和过渡。 | animation | patch |
| 🗑️ | &#x1F5D1; | :wastebasket: | 弃用需要清理的代码。 | wastebasket | patch |
| 🛂 | &#x1F6C2; | :passport_control: | 处理与授权、角色和权限相关的代码。 | passport-control | patch |
| 🩹 | &#x1FA79; | :adhesive_bandage: | 对非关键问题进行简单修复。 | adhesive-bandage | patch |
| 🧐 | &#x1F9D0; | :monocle_face: | 数据探索/检查。 | monocle-face |  |
| ⚰️ | &#x26B0; | :coffin: | 删除无用的代码。 | coffin |  |
| 🧪 | &#x1F9EA; | :test_tube: | 添加失败的测试。 | test-tube |  |
| 👔 | &#128084; | :necktie: | 添加或更新业务逻辑。 | necktie | patch |
| 🩺 | &#x1FA7A; | :stethoscope: | 添加或更新健康检查。 | stethoscope |  |
| 🧱 | &#x1f9f1; | :bricks: | 与基础设施相关的更改。 | bricks |  |
| 🧑‍💻 | &#129489;&#8205;&#128187; | :technologist: | 改善开发者体验。 | technologist |  |
| 💸 | &#x1F4B8; | :money_with_wings: | 添加赞助或与货币相关的基础设施。 | money-with-wings |  |
| 🧵 | &#x1F9F5; | :thread: | 添加或更新与多线程或并发相关的代码。 | thread |  |
| 🦺 | &#x1F9BA; | :safety_vest: | 添加或更新与验证相关的代码。 | safety-vest |  |