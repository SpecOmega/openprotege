# 06 — 依赖与上游关系审查

审计日期：2026-10-09

OpenProtégé 基线 HEAD：`d9caafee6858a958ea7a2944d574407a79f88309`。

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## OpenProtégé 当前依赖

当前 HEAD 只跟踪 `README.md`；未发现 Maven、Gradle、npm、Python、Go、Rust 等依赖清单、锁文件、子模块、上游代码拷贝或组件引用。（VERIFIED：`git ls-files`、提交历史与目录盘点）

因此 OpenProtégé 的直接依赖版本、传递依赖、许可证清单、漏洞状态及可复现构建均为 `UNVERIFIED`；目前无法执行有意义的依赖解析或许可证扫描。

## 上游固定提交

| 上游仓库 | 本次解析的固定 SHA | 来源证据 | 观察结果与限制 |
|---|---|---|---|
| `protegeproject/protege` | `bf03cccc65ea664e139d6829bba6d209ef58ee55` | `git ls-remote` 的 `HEAD`/`master`；GitHub API 固定 SHA 下根目录、README、根 POM、`license.txt` | 上游自述为 Protégé Desktop；Maven 多模块，含 `protege-desktop`、编辑器和 launcher；BSD 2-Clause 风格许可证文件。未完整克隆、构建、运行或审计传递依赖。 |
| `protegeproject/webprotege` | `1e84fa02aef68be45f18c08dbeae94bec9b04a41` | `git ls-remote` 的 `HEAD`/`master`；GitHub API 固定 SHA 下根目录、README、根 POM、`license.txt` | 上游自述为 Web 应用；根目录含 Maven client/server/shared 模块及 Docker 配置；README 表示仓库正被细粒度仓库取代；BSD 2-Clause 风格许可证文件。未完整克隆或验证运行行为。 |

逐项来源和措辞边界见 [upstream-audit.md](./upstream-audit.md)。

## Fork、复用、标准与许可证

| 问题 | 结论 | 状态 |
|---|---|---|
| 是否存在 Git fork 关系 | 本地 OpenProtégé 是无父提交的单提交根历史，当前无 upstream remote；本次未能通过 GitHub 认证元数据确认 fork 标记。不得断言 GitHub 上绝对不是 fork。 | SOURCE-OBSERVED / UNVERIFIED |
| 是否复用上游源码 | 当前跟踪树没有上游源码或子模块。 | VERIFIED（当前检出） |
| 是否使用上游组件/协议/格式 | 当前无依赖配置或代码引用，无法确认使用。 | SOURCE-OBSERVED |
| 上游当前固定版本及依赖 | 表内固定 SHA 是远端 HEAD 解析结果，不是 OpenProtégé 已采用的版本；OpenProtégé 无版本声明。 | VERIFIED / SOURCE-OBSERVED |
| 上游许可证兼容性 | 上游许可证文件观察为 BSD 2-Clause 风格；OpenProtégé 尚无许可证，且未审计传递依赖，兼容性未判定。 | SOURCE-OBSERVED / UNVERIFIED |
| 重复实现/协议兼容 | 没有 OpenProtégé 实现可比较。 | UNVERIFIED |
| 上游维护/迁移风险 | WebProtégé README 明确提示该仓库正在被更细粒度仓库取代；具体替代仓库选择和支持状态未验证。 | SOURCE-OBSERVED / UNVERIFIED |

不得因项目名称或方向相似而断言代码继承。BSD 风格许可证观察不等于 OpenProtégé 已选择该许可证，也不代替完整法律审查。

## 决策前 PoC

建议分别以固定提交和受支持工具链验证 Desktop、WebProtégé 的构建/测试；验证 OWL/RDF 导入—修改—保存—导出往返、插件扩展点、Desktop/Web 数据交换、Web 服务端认证授权与项目隔离；解析完整依赖树及许可证。保存完整命令、环境、退出码、日志和测试数据哈希。（PROPOSED）

在上述结果、产品职责和许可证策略确认之前，不选择“整体 fork”或“重写”，也不宣称已有互操作能力。
