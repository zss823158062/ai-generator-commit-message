# AI Commit Message Generator

一个轻量级的 IntelliJ IDEA 插件，利用 AI 自动分析代码变更并生成高质量、结构化的 Commit Message。无需复杂配置，开箱即用。

**支持任意 VCS**：Git、SVN、Mercurial、Perforce 等所有 IntelliJ 支持的版本控制系统。

## 功能特性

- **一键生成**：在 Commit 对话框点击「Commit助手」按钮，自动生成 commit message
- **VCS 无关**：不依赖 Git，支持所有 IntelliJ 平台的 VCS
- **智能分析**：自动识别新增、修改、删除文件，使用 IntelliJ 原生 API 获取 diff
- **结构化输出**：遵循 `type(scope): 描述` 的 Conventional Commits 规范
- **多 Provider 支持**：内置 Ollama、OpenAI、OpenRouter 三种 AI 后端
- **上下文窗口管理**：提供 4K~128K+ 多档预设，自动压缩和截断超长 diff
- **自定义端点**：URL 以 `#` 结尾可跳过路径拼接，兼容任意 API 服务
- **连接测试**：设置页面提供 Test Connection 按钮，验证配置是否正确
- **可定制 Prompt**：支持自定义 System Prompt，调整生成风格
- **取消生成**：生成过程中可随时取消

## 推荐 System Prompt

以下是一个经过优化的 System Prompt，可在插件设置中直接使用：

<details>
<summary>点击展开完整 Prompt</summary>

```
你是一个专业的 Git Commit Message 生成器。你的任务是分析代码变更并生成高质量、结构化的提交信息。

## 变更类型识别规则

根据 diff 内容，准确判定变更类型（按优先级排序）：

1. **feat** (新功能)
   - 新增文件、类、方法、功能模块
   - 添加新的业务逻辑或特性
   - 新增 API 接口或组件

2. **fix** (修复)
   - 修复 bug、错误、异常
   - 修正逻辑错误或数据处理问题
   - 解决已知问题

3. **refactor** (重构)
   - 代码结构优化、重组
   - 提取方法、合并重复代码
   - 改进代码可读性和可维护性（不改变功能）

4. **perf** (性能优化)
   - 算法优化、性能提升
   - 减少资源消耗、提高执行效率

5. **style** (代码风格)
   - 格式调整、命名优化
   - 代码美化、注释调整（不影响逻辑）

6. **docs** (文档)
   - 文档更新、README 修改
   - 注释完善（仅文档性质）

7. **test** (测试)
   - 测试用例添加或修改

8. **chore** (构建/工具)
   - 构建配置、依赖更新
   - 工具链、CI/CD 配置

**混合变更处理**：如果包含多种类型，选择最主要、影响最大的类型。

## 核心逻辑分析方法

1. **识别主要改动**
   - 哪些文件/类/方法被修改？
   - 新增了什么？删除了什么？修改了什么？

2. **提取业务逻辑**
   - 改动的根本目的是什么？
   - 解决了什么问题？实现了什么功能？
   - 关键的逻辑变化是什么？

3. **总结影响范围**
   - 影响哪个模块/组件？
   - 改动的范围和深度如何？

## 输出格式要求

**严格遵循以下格式**（违反将导致失败）：

type(scope): 简明扼要的变更总结

- 详细变更点 1
- 详细变更点 2
- 详细变更点 3

**格式说明**：
- `type`: 从上述 8 种类型中选择最合适的一个
- `scope`: 影响的模块/组件/文件名（简短、精确）
- 第一行总结：高度概括所有变更的核心内容（50字以内）
- 详细变更点：列出 2-5 个关键改动，每个一行，使用 `-` 开头

## 绝对禁止的输出

❌ 不要输出任何解释性文字，如：
   - "基于 git diff 分析..."
   - "这是分析结果..."
   - "建议使用以下 commit message："
   - "这个提交信息："

❌ 不要使用 Markdown 代码块（```）
❌ 不要添加任何额外的说明或注释
❌ 不要使用英文描述（除了 type 和 scope）

## 输出示例

**正确示例 1 - 新功能**：
feat(commit): 增强 git diff 获取逻辑以支持新文件和删除文件

- 添加文件变更类型识别（新增/删除/修改）
- 针对新文件使用 git diff --cached HEAD 命令
- 针对删除文件使用专门的 diff 命令序列
- 改进日志输出，显示文件类型和 diff 字符数

**正确示例 2 - 修复**：
fix(api): 修复用户登录接口空指针异常

- 添加用户对象空值检查
- 优化异常处理逻辑
- 完善错误日志输出

**正确示例 3 - 重构**：
refactor(service): 重构订单处理服务以提高代码可维护性

- 提取订单验证逻辑到独立方法
- 简化订单状态更新流程
- 移除重复的数据库查询代码

## 关键要求

1. ✅ 使用中文描述（type 和 scope 除外）
2. ✅ 第一行必须是完整的变更总结
3. ✅ 详细变更点要具体、可操作
4. ✅ 直接输出 commit message，不要任何前缀
5. ✅ 准确识别变更类型
6. ✅ 清晰描述核心逻辑

**立即开始**：直接以 `type(scope):` 格式输出，不要任何其他内容。
```

</details>

## 支持的 AI Provider

| Provider | 默认 Endpoint | 默认模型 | 说明 |
|----------|--------------|---------|------|
| **Ollama** | `http://localhost:11434` | `qwen3:8b` | 本地部署，免费使用 |
| **OpenAI** | `https://api.openai.com` | `gpt-4o-mini` | 需要 API Key |
| **OpenRouter** | `https://openrouter.ai/api` | `deepseek/deepseek-r1-0528:free` | 聚合多模型，部分免费 |

> Endpoint 支持自定义，可接入任何 OpenAI 兼容的 API 服务。URL 以 `#` 结尾时将直接使用该地址，不拼接默认路径。

## 安装

### 从源码构建

```bash
git clone https://github.com/zss823158062/ai-generator-commit-message.git
cd ai-generator-commit-message

# 需要 JDK 17+
set JAVA_HOME=D:\path\to\jdk-17-or-higher
gradlew buildPlugin
```

构建产物：`build/distributions/ai-generator-commit-message-1.0.4.zip`

### 安装到 IDEA

1. 打开 IntelliJ IDEA → `Settings` → `Plugins`
2. 点击 `⚙️` → `Install Plugin from Disk...`
3. 选择构建出的 `.zip` 文件
4. 重启 IDEA

## 使用

1. **配置 Provider**：`Settings` → `Tools` → `AI Commit Message Generator`，选择 Provider 并填写相关配置
2. **选择上下文窗口**：根据模型能力选择合适的上下文窗口预设（小模型 8K / 中等 16K / 大模型 32K 等）
3. **测试连接**：点击 `Test Connection` 验证配置
4. **生成 Commit Message**：
   - 打开提交面板（`Ctrl+K` / `Cmd+K`）
   - 选中要提交的文件
   - 点击工具栏的「**Commit助手**」按钮
   - 等待 AI 生成，结果自动填入提交信息框

## 生成示例

```
feat(commit): 增强 diff 获取逻辑以支持新文件和删除文件

- 添加文件变更类型识别（新增/删除/修改）
- 使用 IntelliJ 原生 API 生成 unified diff
- 支持 unversioned 文件的 diff 生成
- 超长 diff 自动压缩和截断，避免 token 超限
```

## 兼容性

- IntelliJ IDEA 2023.2 ~ 2026.1（build 232 ~ 261）
- 需要 JDK 17+ 构建
- 支持所有基于 IntelliJ 平台的 IDE（WebStorm、PyCharm、GoLand 等）
- 支持所有 VCS（Git、SVN、Mercurial、Perforce 等）

## 项目结构

```
src/main/java/com/github/jdami/aicommit/
├── actions/          # Action - 提交面板按钮
├── service/          # AI 服务层
│   ├── provider/     # Ollama / OpenAI / OpenRouter 客户端
│   ├── model/        # 请求模型
│   └── util/         # Prompt 构建 & Diff 压缩/截断 & 响应清洗
├── settings/         # 设置面板 & 持久化
│   └── model/        # Provider 配置模型
├── startup/          # 插件启动/升级逻辑
├── util/             # UnifiedDiffGenerator（IntelliJ 原生 diff）
└── vcs/              # VCS CheckIn Handler
```

## 技术栈

- **构建**：Gradle 8.13 + IntelliJ Platform Gradle Plugin 2.10.5
- **语言**：Java 17
- **HTTP**：OkHttp 4.12.0
- **JSON**：Gson 2.10.1
- **平台**：IntelliJ Platform SDK

## 社区

欢迎加入 [LINUX DO](https://linux.do) 社区交流讨论！

如有问题、建议或想法，欢迎：
- 在 [GitHub Issues](https://github.com/zss823158062/ai-generator-commit-message/issues) 提交反馈
- 到 [LINUX DO](https://linux.do) 社区发帖交流

## License

MIT License
