# 00 — 仓库与 Git 基线

审计日期：2026-10-09

审计根目录：`/workspaces/openprotege`

当前复核提交：`725e9d21bd36d973211e65e67d248ef4a562f10c`

初始代码基线提交：`d9caafee6858a958ea7a2944d574407a79f88309`

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## 结论摘要

### 当前复核（2026-10-09；本轮 PoC 开始前）

本轮开始前，当前分支为 `main`，HEAD 为 `725e9d21bd36d973211e65e67d248ef4a562f10c`，工作区干净；最初状态显示相对 `origin/main` ahead 1，之后复核时本地 `origin/main` tracking ref 已指向同一 SHA。历史含四个提交；最新提交为审计文档更新。当前已有中文 [README](../../README.md)、英文 [README.en.md](../../README.en.md) 和已提交的文档基线；仍未发现 OpenProtégé 应用源码、应用依赖清单、产品构建/测试入口或项目 LICENSE。（VERIFIED：`git status --short --branch`、`git rev-parse HEAD`、`git --no-pager log -3 --oneline --decorate`、`git rev-list --count HEAD`、`git ls-files`）

下表原始检查结果保留为初始审计时的历史基线，不应与当前 HEAD 混用。当前无应用代码的结论仍成立，但“只跟踪 README”“工作区有未跟踪 docs”等描述仅针对初始提交/初始审计时点。

| ID | 发现 | 状态 | 影响 / 风险 | 后续任务 |
|---|---|---|---|---|
| RB-01 | 初始代码基线位于 `main`，HEAD 是唯一根提交 `d9caafee…`；当时与 `origin/main` 对齐。 | VERIFIED（历史基线） | 高：初始代码审计对象只有项目根提交。 | N-01 |
| RB-02 | 初始 HEAD 只跟踪 `README.md`；当前仍未发现应用代码和模块。 | VERIFIED | 高：没有可构建或测试的 OpenProtégé 产品。 | N-02 |
| RB-03 | 初始审计前工作树干净；后续文档已提交；本轮开始前 HEAD 干净且相对 origin ahead 1。 | VERIFIED | 低：当前文档及本轮 PoC 文件均需保留，不做清理。 | N-02 |
| RB-04 | 本地及远端 Git tags 均未返回标签；GitHub CLI 发布列表因未登录无法查询。 | VERIFIED / BLOCKED | 中：不能据此断言不存在 GitHub Release；发布信息不完整。 | N-03 |
| RB-05 | 仓库中未发现 LICENSE、贡献指南、行为准则、安全政策、CI、构建/测试配置或部署配置。 | VERIFIED（当前检出范围） | 高：发布合规、协作与质量门禁尚无项目内基线。 | N-04 |

风险等级是影响判断，不是漏洞严重性评分：高/中/低。

## Git 与目录

实际命令与结果（初始仓库审计）：

| 命令 | 结果 |
|---|---|
| `pwd` | `/workspaces/openprotege` |
| `git status --short --branch` | `## main...origin/main`；初始审计基线时无改动 |
| `git branch --show-current` | `main` |
| `git rev-parse HEAD` | 初始基线：`d9caafee6858a958ea7a2944d574407a79f88309` |
| `git --no-pager log -10 --oneline --decorate` | 仅 `d9caafe (HEAD -> main, origin/main, origin/HEAD) Initial commit` |
| `git rev-list --parents -n 1 HEAD` | 仅一个 SHA；无父提交 |
| `git remote -v` | `origin` 的 fetch/push URL 均为 `https://github.com/SpecOmega/openprotege` |
| `git tag --list` | 空输出 |
| `git ls-remote --tags origin` | 无标签引用；退出码 0 |
| `git ls-files` | 仅 `README.md` |
| `find . -maxdepth 5 -type d -not -path './.git*' -print` | `.`、`./docs`、`./docs/engineering` |
| `git submodule status` | 空输出 |
| `git-lfs version` / `git lfs ls-files` | Git LFS 3.8.0 可用；未列出 LFS 文件 |
| `git ls-files .gitmodules .gitattributes '*lock*' 'Dockerfile*' 'Makefile' '.github/**'` | 空输出 |

GitHub Release 查询：`gh release list --repo SpecOmega/openprotege --limit 10`，退出码 4；CLI 提示需要 `gh auth login` 或 `GH_TOKEN`。（BLOCKED）未向 CLI 提供令牌，也没有尝试写入远端。可在获准的只读 GitHub API/CLI 认证环境复核发布信息。

初始审计时，顶层除 Git 元数据外为 `README.md` 与未跟踪的 `docs/`；当前复核时文档已提交，且无应用实现。初始基线跟踪树与后续文档及本轮 PoC 文件应区分。

## 入口、政策与工程配置清单

| 文件/配置 | 当前检出观察 |
|---|---|
| `README.md`、`README.en.md` | 当前检出含中英文项目介绍；最初根提交中的 README 只有项目名和简短定位，不构成产品能力证据。 |
| `LICENSE*` | 未发现。 |
| `CONTRIBUTING*` / 行为准则 | 未发现。 |
| `SECURITY*` | 未发现。 |
| `.github/workflows/` 或其他 CI 配置 | 未发现。 |
| 应用 Maven/Gradle/npm/Python/Rust/Go 清单、锁文件 | 未发现；存在仅供独立 PoC 使用的 [owlapi-poc-pom.xml](./poc/owlapi-poc-pom.xml)。 |
| 应用测试框架配置、测试源码 | 未发现；仅有 [OwlFormatRoundTrip.java](./poc/OwlFormatRoundTrip.java) 这项非产品 PoC harness。 |
| `.gitmodules`、Git LFS 跟踪文件 | 未发现。 |
| Docker、部署清单、发布脚本 | 未发现。 |

“未发现”仅适用于当前检出与本次工作区盘点，不排除其他未检出分支、远端 Release 附件或外部系统。

## 执行环境

工具版本探测结果：Git `2.55.0`、OpenJDK `25.0.4.1`、Maven `3.9.16`、Node.js `v24.21.0`、npm `11.19.0`、Python `3.14.2`、GitHub CLI `2.100.0`、Git LFS `3.8.0`。（VERIFIED）这些是审计环境版本，不是项目支持矩阵。

## 可复现性与边界

本基线以固定 HEAD 和上述只读命令为界。Git 查询、目录枚举、LFS 查询均成功；远端标签查询退出码 0。Release 查询受 GitHub CLI 未认证阻塞。没有执行构建、测试、部署或写入操作。

## 后续行动

- N-01：后续每轮固定审计 SHA，并重新记录分支/工作树状态。
- N-02：经产品范围确认后，选择和引入最小可验证代码；保留现存审计文档。
- N-03：在有授权的只读 GitHub 会话中查询 Releases/附件并记录访问时间与结果。
- N-04：在任何代码或依赖引入前，确定许可证及贡献、安全、CI 政策。
