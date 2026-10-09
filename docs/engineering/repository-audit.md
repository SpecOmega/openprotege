# OpenProtégé 仓库与源码审计

审计日期：2026-10-09

审计范围：当前本地检出及其可访问的 Git 元数据、跟踪文件、两个指定上游仓库的固定提交。

状态约定：`VERIFIED` 表示执行了可复现检查并取得证据；`SOURCE-OBSERVED` 表示从文件或配置中观察到但未运行验证；`PROPOSED` 表示建议；`UNVERIFIED` 表示尚未验证；`BLOCKED` 表示受环境、权限或网络限制。

## 摘要

| 结论 | 状态 | 证据 |
|---|---|---|
| 工作分支为 `main`，与 `origin/main` 对齐；工作树干净。 | VERIFIED | E-01、E-02 |
| 当前提交是根提交 `d9caafee6858a958ea7a2944d574407a79f88309`，标题为 `Initial commit`，只包含 `README.md`。 | VERIFIED | E-01、E-03 |
| README 仅声明项目为“带 AI 辅助的开源本体建模平台”；仓库中没有实现这些功能的代码。 | SOURCE-OBSERVED | E-03、E-04 |
| 当前仓库没有桌面/Web 应用、后端、数据库、API、插件、构建脚本、测试、CI、部署配置或项目许可证文件。 | VERIFIED | 对 Git 跟踪树与工作目录的盘点，E-03、E-04 |
| 不能据此声称 OpenProtégé 已基于或集成 Protégé Desktop/WebProtégé；现有仓库没有可观察到的集成证据。 | SOURCE-OBSERVED | E-03、E-04；上游比较见 [upstream-audit.md](./upstream-audit.md) |

本审计反映指定检出状态，不代表远端后来发生的变更。上游信息仅作为固定提交的静态源码证据，不视为本项目的实现或验证结果。

## Git 与工作区

| 项目 | 结果 | 状态 |
|---|---|---|
| 分支 | `main` | VERIFIED |
| `git status --short --branch` | `## main...origin/main`，无工作区改动 | VERIFIED |
| `git status --porcelain=v1` | 空输出 | VERIFIED |
| 远程 | `origin` fetch/push 均为 `https://github.com/SpecOmega/openprotege` | VERIFIED |
| HEAD | `d9caafee6858a958ea7a2944d574407a79f88309` | VERIFIED |
| 提交时间与标题 | `2026-10-09T13:04:01+08:00`；`Initial commit` | VERIFIED |
| 历史 | 仅一个提交；根提交无父提交 | VERIFIED |
| HEAD 跟踪文件 | `README.md` | VERIFIED |

## 文件、模块与技术栈

### 仓库内容

检查工作目录时，除 `.git/` 外只有 `README.md`；Git 跟踪树也仅列出 `README.md`。当前没有既存的工程文档目录；本次新增的审计文档是首批文档。

README 内容为标题 `openprotege` 与一句项目描述。它没有给出安装、运行、构建、测试或部署步骤。

### 技术栈及运行组件

| 类别 | 本仓库观察结果 | 状态 |
|---|---|---|
| 编程语言、框架、运行时 | 未发现源码或项目清单，无法识别 | SOURCE-OBSERVED |
| 构建系统与依赖管理 | 未发现构建/依赖配置 | SOURCE-OBSERVED |
| 桌面客户端、Web 前端、后端 | 未发现 | SOURCE-OBSERVED |
| 数据库、API、插件体系 | 未发现 | SOURCE-OBSERVED |
| 运行方式 | README 未提供；无可运行应用 | SOURCE-OBSERVED |
| AI、语义检索、协同编辑 | README 中只有产品描述，没有实现 | SOURCE-OBSERVED |

本环境中探测到 Git `2.55.0`、OpenJDK `25.0.4.1`、Maven `3.9.16`、Node.js `v24.21.0`、npm `11.19.0`、Python `3.14.2`。这是审计环境工具版本，不是项目要求或受支持的运行时版本。（VERIFIED）

## 已实现、占位与缺失

- 仓库当前唯一可观察内容是项目名称和一句定位说明。（SOURCE-OBSERVED）
- 没有可运行功能、部分实现模块、占位实现或 TODO/FIXME 源码可供确认。（VERIFIED：对当前工作目录进行文件和关键词盘点）
- 因无实现代码，不能对用户、项目、成员、权限、版本、审计、本体格式保真、身份认证或错误处理作功能性判断；均为 `UNVERIFIED`，而不是“已经实现”或“已验证缺陷”。
- “AI assistance”目前只是 README 的文字声明，不构成 AI 功能的实现证据。（SOURCE-OBSERVED）

## 测试、CI、构建、部署

| 检查项 | 结果 | 状态 |
|---|---|---|
| 测试文件或测试配置 | 未发现 | VERIFIED |
| CI 工作流 | 未发现 `.github/workflows/` 或其他 CI 配置 | VERIFIED |
| 构建/启动脚本 | 未发现 | VERIFIED |
| 部署配置 | 未发现 | VERIFIED |
| 项目构建或测试执行 | 没有项目构建入口，未执行 | BLOCKED |
| 运行产品及端到端验证 | 没有应用可运行，未执行 | BLOCKED |

不将环境中已安装的 Java/Maven 等工具解释为项目栈，也未用这些工具虚构一次项目构建或测试结果。

## 许可证与开源发布风险

- 当前 Git 跟踪树没有 `LICENSE`、版权声明或第三方代码来源清单。（VERIFIED）
- README 使用“开源”描述，但未声明本项目的许可证；允许复用、分发及其条件均未定义。（SOURCE-OBSERVED）
- 当前没有仓库依赖清单，无法开展依赖许可证兼容性或供应链审计。（SOURCE-OBSERVED）
- 在加入上游代码、依赖或发布二进制前，应先确定本项目许可证并建立第三方来源/许可证清单。（PROPOSED）

## Protégé 上游关系

本次固定并静态检查的上游提交及其可观察事实见 [upstream-audit.md](./upstream-audit.md)。当前 OpenProtégé 只有一个新建根提交和一份泛化 README；未发现上游 Git 历史、代码、Maven 坐标、依赖声明、子模块、版权声明或来源说明。因此仅能得出“当前检出没有上游集成证据”，不能推断没有任何项目外的关系，也不能将上游功能归入 OpenProtégé。（SOURCE-OBSERVED）

## 执行记录

审计过程中执行了以下本地只读检查：

```text
git status --short --branch
git branch --show-current
git --no-pager log -1 --format='%H%n%cI%n%s'
git --no-pager log -8 --oneline --decorate
git remote -v
git status --porcelain=v1
git show --no-patch --format=fuller HEAD
git ls-tree -r --name-only HEAD
git ls-files --stage
find . -maxdepth 4 -type d -not -path './.git*' -print
```

以上检查均正常完成；具体结果已在本文件及 [evidence-ledger.md](./evidence-ledger.md) 中记录。上游固定引用解析与文件观察见 [upstream-audit.md](./upstream-audit.md)。没有对仓库进行构建、测试、写入或联网推送。

## 证据索引与后续

逐项证据与状态见 [evidence-ledger.md](./evidence-ledger.md)；从现状派生的缺口见 [known-gaps.md](./known-gaps.md)。

下一项可执行工程任务（PROPOSED）：由项目负责人确认阶段目标与许可证策略，然后基于本审计建立需求基线；在获得明确架构/许可证决策前，不开始上游代码引入或 MVP 实现。
