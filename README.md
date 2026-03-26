# AI Commit Message Generator

一个 IntelliJ IDEA 插件，利用 AI 自动分析代码变更并生成高质量、结构化的 Git Commit Message。

## 功能特性

- **一键生成**：在 Commit 对话框点击「Git助手」按钮，自动生成 commit message
- **智能分析**：自动识别新增、修改、删除文件，精准获取 diff 内容
- **结构化输出**：遵循 `type(scope): 描述` 的 Conventional Commits 规范
- **多 Provider 支持**：内置 Ollama、OpenAI、OpenRouter 三种 AI 后端
- **连接测试**：设置页面提供 Test Connection 按钮，验证配置是否正确
- **可定制 Prompt**：支持自定义 System Prompt，调整生成风格
- **取消生成**：生成过程中可随时取消

## 支持的 AI Provider

| Provider | 默认 Endpoint | 默认模型 | 说明 |
|----------|--------------|---------|------|
| **Ollama** | `http://localhost:11434` | `qwen3:8b` | 本地部署，免费使用 |
| **OpenAI** | `https://api.openai.com` | `gpt-4o-mini` | 需要 API Key |
| **OpenRouter** | `https://openrouter.ai/api` | `deepseek/deepseek-r1-0528:free` | 聚合多模型，部分免费 |

> Endpoint 支持自定义，可接入任何 OpenAI 兼容的 API 服务。

## 安装

### 从源码构建

```bash
git clone https://github.com/zss823158062/ai-generator-commit-message.git
cd ai-generator-commit-message

# 需要 JDK 17+
set JAVA_HOME=D:\path\to\jdk-17-or-higher
gradlew buildPlugin
```

构建产物：`build/distributions/ai-generator-commit-message-1.0.2.zip`

### 安装到 IDEA

1. 打开 IntelliJ IDEA → `Settings` → `Plugins`
2. 点击 `⚙️` → `Install Plugin from Disk...`
3. 选择构建出的 `.zip` 文件
4. 重启 IDEA

## 使用

1. **配置 Provider**：`Settings` → `Tools` → `AI Commit Message Generator`，选择 Provider 并填写相关配置
2. **测试连接**：点击 `Test Connection` 验证配置
3. **生成 Commit Message**：
   - 打开提交面板（`Ctrl+K` / `Cmd+K`）
   - 选中要提交的文件
   - 点击工具栏的「**Git助手**」按钮
   - 等待 AI 生成，结果自动填入提交信息框

## 生成示例

```
feat(commit): 增强 git diff 获取逻辑以支持新文件和删除文件

- 添加文件变更类型识别（新增/删除/修改）
- 针对新文件使用 git diff --cached HEAD 命令
- 针对删除文件使用专门的 diff 命令序列
- 改进日志输出，显示文件类型和 diff 字符数
```

## 兼容性

- IntelliJ IDEA 2023.2 ~ 2026.1（build 232 ~ 261）
- 需要 JDK 17+ 构建
- 支持所有基于 IntelliJ 平台的 IDE（WebStorm、PyCharm、GoLand 等）

## 项目结构

```
src/main/java/com/github/jdami/aicommit/
├── actions/          # Action - 提交面板按钮
├── service/          # AI 服务层
│   ├── provider/     # Ollama / OpenAI / OpenRouter 客户端
│   ├── model/        # 请求模型
│   └── util/         # Prompt 构建 & 响应清洗
├── settings/         # 设置面板 & 持久化
│   └── model/        # Provider 配置模型
├── startup/          # 插件启动/升级逻辑
└── vcs/              # VCS CheckIn Handler
```

## 技术栈

- **构建**：Gradle + IntelliJ Platform Gradle Plugin 2.10.5
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
