# 00 — 仓库与 Git 基线

审计日期：2026-10-09

审计根目录：`/workspaces/openprotege`

当前复核提交：`e595add49b43a83bb68c7c618ee4f0416b2b2e6b`

初始代码基线提交：`d9caafee6858a958ea7a2944d574407a79f88309`

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## 结论摘要

### 当前复核（2026-10-09；文档修改开始前）

修改文档前，当前分支为 `main`，HEAD 为 `e595add49b43a83bb68c7c618ee4f0416b2b2e6b`（`docs: clean baseline markdown formatting`），与 `origin/main` 对齐且工作区干净。历史含三个提交：初始 README、工程/需求文档、Markdown 空白清理。当前已有中文 [README](../../README.md)、英文 [README.en.md](../../README.en.md) 和已提交的文档基线；仍未发现 OpenProtégé 应用源码、依赖清单、构建/测试入口或项目 LICENSE。（VERIFIED：`git status --short --branch`、`git rev-parse HEAD`、`git --no-pager log -3 --oneline --decorate`、`git ls-files`）

下表原始检查结果保留为初始审计时的历史基线，不应与当前 HEAD 混用。当前无代码的结论仍成立，但“只跟踪 README”“工作区有未跟踪 docs”等描述仅针对初始提交/初始审计时点。

| ID | 发现 | 状态 | 影响 / 风险 | 后续任务 |
|---|---|---|---|---|
| RB-01 | 当前检出位于 `main`，HEAD 是唯一的根提交 `d9caafee…`；HEAD 与 `origin/main` 对齐。 | VERIFIED | 高：代码审计对象只是项目初始提交。 | N-01 |
| RB-02 | HEAD 只跟踪 `README.md`；应用代码和模块未发现。 | VERIFIED | 高：没有可构建或测试的 OpenProtégé 产品。 | N-02 |
| RB-03 | 执行审计前工作树干净；复核时仅有此前阶段新增的 `docs/` 未跟踪文件，未见其他用户修改。 | VERIFIED | 低：已存在文档需保留，不能覆盖或清理。 | N-02 |
| RB-04 | 本地及远端 Git tags 均未返回标签；GitHub CLI 发布列表因未登录无法查询。 | VERIFIED / BLOCKED | 中：不能据此断言不存在 GitHub Release；发布信息不完整。 | N-03 |
| RB-05 | 仓库中未发现 LICENSE、贡献指南、行为准则、安全政策、CI、构建/测试配置或部署配置。 | VERIFIED（当前检出范围） | 高：发布合规、协作与质量门禁尚无项目内基线。 | N-04 |

风险等级是影响判断，不是漏洞严重性评分：高/中/低。

## Git 与目录

实际命令与结果（审计期间）：

| 命令 | 结果 |
|---|---|
| `pwd` | `/workspaces/openprotege` |
| `git status --short --branch` | `## main...origin/main`；基线时无改动 |
| `git branch --show-current` | `main` |
| `git rev-parse HEAD` | `d9caafee6858a958ea7a2944d574407a79f88309` |
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

截至审计时，顶层除 Git 元数据外为 `README.md` 和此前审计新增的 `docs/`。基线提交跟踪树与当前未提交文档应区分；文档变动不会改变对 HEAD 的判断。

## 入口、政策与工程配置清单

| 文件/配置 | 当前检出观察 |
|---|---|
| `README.md` | 标题 `openprotege`；一句“Open-source ontology modeling platform with AI assistance.”；无用法。 |
| `LICENSE*` | 未发现。 |
| `CONTRIBUTING*` / 行为准则 | 未发现。 |
| `SECURITY*` | 未发现。 |
| `.github/workflows/` 或其他 CI 配置 | 未发现。 |
| Maven/Gradle/npm/Python/Rust/Go 等清单、锁文件 | 未发现。 |
| 测试框架配置、测试源码 | 未发现。 |
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
