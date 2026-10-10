# OpenProtégé 仓库与源码审计

审计日期：2026-10-09

审计范围：OpenProtégé 当前本地检出（本报告最初记录的代码基线为 `d9caafee6858a958ea7a2944d574407a79f88309`，本轮检查前复核为 `725e9d21bd36d973211e65e67d248ef4a562f10c`），以及两个指定上游的固定提交。

状态约定：`VERIFIED` 表示执行了可复现检查并取得证据；`SOURCE-OBSERVED` 表示从文件或配置中观察到但未运行验证；`PROPOSED` 表示建议；`UNVERIFIED` 表示尚未验证；`BLOCKED` 表示受环境、权限或网络限制。

## 后续 Web 模块更新（2026-10-09）

本报告前述“当前复核”记录的是本轮身份/项目模块实现前的检出状态，不应解释为当前代码事实。其后 Web foundation 与身份/团队/项目 API 已加入工作树：本地邀请/会话、管理员引导、团队/项目成员关系与初步对象授权；Java 21 PostgreSQL suite 5/5 tests 通过，具体范围和未验证边界见 E-36、[当前架构](./01-current-architecture.md)、[模块清单](./02-module-inventory.md)及[安全审计](./05-security-review.md)。React UI、本体功能和 Compose bridge 部署仍未完成/验证。

## 摘要

### 当前复核补充（2026-10-09）

本轮检查前当前分支为 `main`，HEAD `725e9d21bd36d973211e65e67d248ef4a562f10c`，相对 `origin/main` ahead 1，工作区干净，历史有四个提交。项目已有中英文 README 和已提交的工程/需求草案；应用源码、应用依赖清单、产品构建/测试入口、CI 和 LICENSE 仍未发现。（VERIFIED：`git status --short --branch`、`git rev-parse HEAD`、`git rev-list --count HEAD`、`git ls-files`）

两个指定上游此后已完整克隆并固定 checkout，Desktop 固定提交在 JDK 21 下 `clean verify` 成功；WebProtégé 的首次构建因缺少 MongoDB 失败、JDK 25 构建遇到 AutoValue 生成类编译失败；该次复核时 JDK 21 重试尚无最终状态，后续镜像重试结果见下节及 [03-build-and-test-baseline.md](./03-build-and-test-baseline.md)。这些上游结果不改变 OpenProtégé 尚无产品实现的结论。

### Maven 镜像与构建结果补充（2026-10-09）

在固定 WebProtégé commit `1e84fa02aef68be45f18c08dbeae94bec9b04a41` 上，使用 Aliyun Central mirror、JDK 21、MongoDB 4.1 及已有 Maven 本地缓存，`clean package` 的 9 模块 reactor 成功，Maven 退出码 0，7 个模块聚合测试合计 4124 tests、0 failures/errors/skips。清华镜像对本次探测的一个具体 Maven artifact URL 返回 HTTP 404；这不代表清华所有仓库路径均不可用。Aliyun 构建未验证空缓存，也未证明其覆盖 POM 中的 Protege 专用仓库。详见 E-27、E-28。OpenProtégé 本身仍无应用构建入口或上游集成。

| 结论 | 状态 | 证据 |
|---|---|---|
| 工作分支为 `main`，与 `origin/main` 对齐；工作树干净。 | VERIFIED | E-01、E-02 |
| 初始代码基线是根提交 `d9caafee6858a958ea7a2944d574407a79f88309`，标题为 `Initial commit`，只包含 `README.md`。 | VERIFIED（历史基线） | E-01、E-03 |
| README 仅声明项目为“带 AI 辅助的开源本体建模平台”；仓库中没有实现这些功能的代码。 | SOURCE-OBSERVED | E-03、E-04 |
| 当前仓库没有桌面/Web 应用、后端、数据库、API、插件、构建脚本、测试、CI、部署配置或项目许可证文件。 | VERIFIED | 对 Git 跟踪树与工作目录的盘点，E-03、E-04 |
| 不能据此声称 OpenProtégé 已基于或集成 Protégé Desktop/WebProtégé；现有仓库没有可观察到的集成证据。 | SOURCE-OBSERVED | E-03、E-04；上游比较见 [upstream-audit.md](./upstream-audit.md) |

本审计中原始 Git/目录表格反映初始提交时的状态；当前复核以上述补充和 [00-repository-baseline.md](./00-repository-baseline.md) 为准。上游信息是独立固定提交的源码/构建证据，不视为 OpenProtégé 的实现或验证结果。

## Git 与工作区

| 项目 | 结果 | 状态 |
|---|---|---|
| 当前分支和状态（本轮开始前） | `main`；`## main...origin/main [ahead 1]`；工作区干净 | VERIFIED |
| 初始审计时状态 | `## main...origin/main`，无工作区改动 | VERIFIED（历史基线） |
| 远程 | `origin` fetch/push 均为 `https://github.com/SpecOmega/openprotege` | VERIFIED |
| 当前 HEAD（本轮开始前） | `725e9d21bd36d973211e65e67d248ef4a562f10c` | VERIFIED |
| 提交时间与标题 | `2026-10-09T13:04:01+08:00`；`Initial commit` | VERIFIED |
| 当前历史 | 四个提交；初始基线是无父提交的根提交 | VERIFIED |
| 初始 HEAD 跟踪文件 | `README.md` | VERIFIED（历史基线） |

## 文件、模块与技术栈

### 仓库内容

初始审计时，除 `.git/` 外只有 `README.md`；Git 跟踪树也仅列出 `README.md`。Stage 0 文档审计时另有中英文 README、工程/需求文档，以及 PoC harness 与独立依赖 POM；这些文档不是产品运行模块。Web foundation 是之后新增的模块，见本文件后续增量。

最初根提交中的 README 内容为标题 `openprotege` 与一句项目描述；后续已拆分中英文 README。README 与审计文档都不能证明产品功能可运行。

### Stage 0 技术栈及运行组件（历史快照）

| 类别 | 本仓库观察结果 | 状态 |
|---|---|---|
| 产品语言、框架、运行时 | 未发现应用源码或项目清单，无法识别；文档 PoC harness 使用 Java | SOURCE-OBSERVED |
| 产品构建系统与依赖管理 | 未发现应用构建/依赖配置；独立 PoC 使用 OWLAPI 4.5.29 Maven POM | SOURCE-OBSERVED |
| 桌面客户端、Web 前端、后端 | 未发现 | SOURCE-OBSERVED |
| 数据库、API、插件体系 | 未发现 | SOURCE-OBSERVED |
| 运行方式 | README 未提供；无可运行应用 | SOURCE-OBSERVED |
| AI、语义检索、协同编辑 | README 中只有产品描述，没有实现 | SOURCE-OBSERVED |

本环境中探测到 Git `2.55.0`、OpenJDK `25.0.4.1`、Maven `3.9.16`、Node.js `v24.21.0`、npm `11.19.0`、Python `3.14.2`。这是 Stage 0 审计环境工具版本，不是项目要求或受支持的运行时版本。（VERIFIED）

## 已实现、占位与缺失

- 初始根提交唯一内容是项目名称和一句定位说明；当前检出另有工程、需求与 PoC 文档。（SOURCE-OBSERVED）
- Stage 0 快照没有可运行产品功能、部分实现模块、占位实现或 TODO/FIXME 产品源码可供确认；仅有用于 OWLAPI 单样例格式验证的独立 PoC harness。（VERIFIED：当时对工作目录进行文件和关键词盘点）
- 因无实现代码，不能对用户、项目、成员、权限、版本、审计、本体格式保真、身份认证或错误处理作功能性判断；均为 `UNVERIFIED`，而不是“已经实现”或“已验证缺陷”。
- “AI assistance”目前只是 README 的文字声明，不构成 AI 功能的实现证据。（SOURCE-OBSERVED）

## 测试、CI、构建、部署

| 检查项 | 结果 | 状态 |
|---|---|---|
| 产品测试文件或测试配置 | 未发现；仅有独立文档 PoC harness | VERIFIED |
| CI 工作流 | 未发现 `.github/workflows/` 或其他 CI 配置 | VERIFIED |
| 构建/启动脚本 | 未发现 | VERIFIED |
| 部署配置 | 未发现 | VERIFIED |
| 项目构建或测试执行 | 没有项目构建入口，未执行 | BLOCKED |
| 运行产品及端到端验证 | 没有应用可运行，未执行 | BLOCKED |

不将环境中已安装的 Java/Maven 等工具解释为项目栈，也未用这些工具虚构一次项目构建或测试结果。

## Web foundation 增量（Stage 0 之后）

后续在用户确认 Java 21、Spring Boot 3、PostgreSQL/Flyway 技术基线后，新增 `server/` Maven 服务、Testcontainers 集成测试、Dockerfile 和 PostgreSQL Compose 配置；随后实现身份/团队/项目 API，以及 OWLAPI 5.1.20 支持的首个本体文件导入/版本/导出 API 切片。完整测试结果见工程证据台账 E-37 及本轮新增证据。带应用 healthcheck 的 Compose 启动在当前 Docker bridge 网络环境以退出码 1 失败；本体编辑、恢复、完整格式保真和最大文件资源验证仍待完成，具体见 [构建和测试基线](./03-build-and-test-baseline.md)。

## 许可证与开源发布风险

- 当前 Git 跟踪树没有 `LICENSE`、版权声明或第三方代码来源清单。（VERIFIED）
- README 使用“开源”描述，但未声明本项目的许可证；允许复用、分发及其条件均未定义。（SOURCE-OBSERVED）
- 当前没有仓库依赖清单，无法开展依赖许可证兼容性或供应链审计。（SOURCE-OBSERVED）
- 在加入上游代码、依赖或发布二进制前，应先确定本项目许可证并建立第三方来源/许可证清单。（PROPOSED）

## Protégé 上游关系

本次固定并静态检查的上游提交及其可观察事实见 [upstream-audit.md](./upstream-audit.md)。Stage 0 审计快照只有中英文 README 和工程/需求/PoC 文档；之后新增 Web foundation Maven 模块，不含上游代码、上游 Maven 坐标或子模块。当前仍没有上游集成证据，因此不能推断没有任何项目外关系，也不能将上游功能归入 OpenProtégé。（SOURCE-OBSERVED）

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
