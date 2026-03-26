# AI Commit Message Generator

一个 IntelliJ IDEA 插件，利用 AI 自动分析代码变更并生成高质量、结构化的 Commit Message。

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
