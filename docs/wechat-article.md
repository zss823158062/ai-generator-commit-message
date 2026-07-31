# AI Commit Message Generator：一个轻量级的 AI 提交信息生成插件

## 写代码 10 分钟，写 Commit Message 10 分钟？

每个开发者都经历过这样的场景：

代码改完了，`Ctrl+K` 打开提交面板，然后——

对着提交信息框发呆。

"这次改了啥来着？"
"fix bug？太笼统了吧..."
"算了，先写个 update 提交再说。"

于是 git log 里全是这样的记录：

```
update
fix
修改
调整
...
```

三个月后回头看，完全不知道当时改了什么。

**有没有一种可能，这件事可以交给 AI？**

---

## 介绍：AI Commit Message Generator

这是一个轻量级的 IntelliJ IDEA 插件，一键分析你的代码变更，自动生成结构化的 Commit Message。

不需要复杂配置，不需要学习新工具，装上就能用。

### 它能做什么？

点击提交面板上的「Commit助手」按钮，插件会：

1. 自动获取你所有暂存文件的 diff
2. 智能分析变更类型（新功能？修 Bug？重构？）
3. 生成符合 Conventional Commits 规范的提交信息

生成效果：

```
feat(service): 新增用户权限校验模块

- 添加 PermissionService 权限校验服务
- 实现基于角色的接口访问控制
- 新增权限不足时的统一异常处理
```

```
fix(api): 修复订单列表分页查询返回空数据

- 修正分页参数偏移量计算错误
- 添加空结果集的默认返回处理
```

**告别 "update"、"fix"、"修改"，每一次提交都清晰可追溯。**

---

## 三大 AI 后端，灵活选择

| 方案 | 适合谁 | 费用 |
|------|--------|------|
| **Ollama（本地）** | 注重隐私、有 GPU 的开发者 | 完全免费 |
| **OpenAI** | 追求最佳效果 | 按量付费 |
| **OpenRouter** | 想尝试各种模型 | 部分免费 |

**默认配置就是 Ollama + qwen3:8b**，本地运行，代码不出你的电脑。

Endpoint 支持自定义，也就是说——**任何兼容 OpenAI 接口的服务都能接入**。国内各种大模型 API、自建服务，统统没问题。

---

## 不只是 Git

很多类似的插件只支持 Git。

这个不一样——**它支持所有 VCS**。

Git、SVN、Mercurial、Perforce，只要 IntelliJ 支持的版本控制系统，它都能用。因为底层用的是 IntelliJ 原生的 diff API，而不是调用 git 命令。

---

## 大文件也不怕

提交了几十个文件，diff 内容超长怎么办？

插件内置了智能处理：

- **Diff 压缩**：自动过滤无关内容，只保留有意义的变更行
- **上下文窗口预设**：根据你用的模型大小（4K / 8K / 16K / 32K / 128K+），自动截断到合适长度
- **超限提示**：如果 API 返回 token 超限错误，会给出中文建议

不用手动删减文件，插件帮你处理。

---

## 安装只需 3 步

**第一步：下载插件**

从 GitHub 下载最新 Release 的 `.zip` 文件。

**第二步：安装到 IDEA**

`Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk...` → 选择 zip 文件 → 重启

**第三步：配置 AI 后端**

`Settings` → `Tools` → `AI Commit Message Generator`

选择 Provider，填入地址和 Key（Ollama 本地的话什么都不用填），点击 `Test Connection` 确认连接正常。

**搞定。**

---

## 使用方式

和你平时提交代码的流程完全一样：

1. `Ctrl+K` 打开提交面板
2. 选中要提交的文件
3. 点击「**Commit助手**」按钮
4. 等几秒，提交信息自动填好
5. 检查一下，提交

就这么简单。

---

## 开源地址

项目已在 GitHub 开源，欢迎 Star：

**https://github.com/zss823158062/ai-generator-commit-message**

兼容 IntelliJ IDEA 2023.2 ~ 2026.1，支持所有 JetBrains 系 IDE（WebStorm、PyCharm、GoLand 等）。

## 社区交流

欢迎到 LINUX DO 社区交流反馈：

**https://linux.do**

有问题、有建议、有想法，随时聊。

---

*让 AI 帮你写 Commit Message，把时间花在真正重要的事情上。*
