# 已知缺口与审计边界

审计日期：2026-10-09

本清单描述当前仓库可证实的空缺或未验证项，不把产品目标、上游能力或建议误写为已实现功能。

## P0：开始工程开发前必须澄清

| 缺口 | 状态 | 影响 / 可执行后续 |
|---|---|---|
| 已建立产品决策记录与需求基线草案，但尚未冻结详细角色矩阵、OWL 2 Profile、格式保真判据和 API/冲突策略。 | SOURCE-OBSERVED / UNVERIFIED | 评审并冻结需求草案，将未决技术项继续关联 PoC 与 ADR。（PROPOSED） |
| 没有项目许可证，也没有代码来源或第三方依赖清单。 | VERIFIED | 明确项目许可证与上游/依赖复用策略；复用代码前逐项审查许可证和归属。（PROPOSED） |
| 没有可供维护、构建、测试或运行的应用实现。 | VERIFIED | MVP 方案必须从真实技术选择和可验证 PoC 开始，不得宣称已有产品能力。（PROPOSED） |
| OpenProtégé 仍未集成 Protégé Desktop/WebProtégé；Desktop 固定提交构建通过，Web 完整 package 首次受 HTTP 504 阻塞、恢复后重试进行中；OWLAPI 单样例格式往返 PoC 已通过。 | VERIFIED / BLOCKED | 记录恢复后固定提交构建结果；评估 Web 专用仓库/替代维护仓库，继续验证编辑器级格式往返、插件、桌面/Web 交换和权限，再作架构选择。（PROPOSED） |

## P1：功能与质量证据缺口

以下能力在当前代码中都没有实现证据；列项是需要需求化/验证的范围，不是对方案的既定承诺。

| 能力 | 状态 | 后续验证方向 |
|---|---|---|
| 桌面端、Web 端、后端、持久化与 API | UNVERIFIED | 基于已确认技术栈建立可运行的最小端到端切片。 |
| 项目、工作区、成员、权限、版本、审计与项目隔离 | UNVERIFIED | 建立权限模型和越权读取/写入集成测试。 |
| OpenProtégé 本体导入、校验、保存、导出和 OWL/RDF 往返保真 | UNVERIFIED；上游 OWLAPI 独立 PoC 部分验证 | 固定 `pizza.owl` 单样例通过 OWLAPI RDF/XML↔Turtle 精确公理往返；仍需编辑器/应用实际流程、错误输入、安全限制及更多 OWL 2 构造验证。 |
| 身份认证、授权、API 错误与版本策略 | UNVERIFIED | 形成安全边界和接口契约，并执行未授权/越权测试。 |
| 外部 IRI 解析策略与 SSRF 防护 | UNVERIFIED | 定义默认拒绝/允许边界并验证网络访问行为。 |
| XSS、敏感信息泄露、供应链风险 | UNVERIFIED | 应用存在后执行威胁建模、输入输出测试和依赖扫描。 |
| 备份、恢复、升级及迁移 | UNVERIFIED | 明确恢复点/恢复时间目标并实施演练及迁移测试。 |
| 插件、国际化、可访问性、社区协作和 AI/Agent | UNVERIFIED | 作为需求讨论项；不得把 README 的“AI assistance”当作已实现能力。 |

## P2：构建、发布和协作证据

- 没有测试套件、CI、构建/运行脚本、部署手册或发布工作流。（VERIFIED）
- 中英文项目 README 和中文工程/需求文档已存在；英文工程/需求文档翻译仍 Pending，尚无完整双语开发、架构和部署手册。（VERIFIED：当前文档树）
- 没有截图、下载物、正式版本或生产部署配置。（SOURCE-OBSERVED）
- 上游固定提交已完整克隆；Desktop Maven 构建通过；OWLAPI 4.5.29 对固定 `pizza.owl` 的文件格式往返通过。WebProtégé JDK 21 + MongoDB 4.1 初次重试完成 4091 项模块测试，但 package 曾被项目 GitHub Maven 仓库 HTTP 504 阻塞；endpoint 后恢复到 HTTP 200，新的 package 重试正在执行。应用运行、编辑器流程、桌面/Web 交换、授权安全、插件兼容与完整传递依赖许可证兼容仍未验证。（部分 VERIFIED、BLOCKED、UNVERIFIED；具体范围见 [03-build-and-test-baseline.md](./03-build-and-test-baseline.md) 与 [upstream-audit.md](./upstream-audit.md)）

## 边界说明

1. “未发现”表示只检查了本次工作区/固定文件范围，不代表其他分支、远端未检出历史、外部系统或上游仓库没有该能力。
2. 上游 README 中的特性陈述是上游声明，不是 OpenProtégé 的证据；固定提交、读取文件和静态结构观察不等于运行验证。
3. 当前没有应用，因此无法对漏洞是否存在作有意义的运行时判定；这不是“已通过安全审计”。
4. 上游依赖许可证只看到根 POM 中部分声明与许可证文件，没有完整依赖树/传递依赖结论。

## 下一项明确任务

先确认 WebProtégé 依赖阻塞的具体 artifact 及仓库可用替代途径；仅在依赖可获取后，重试固定提交并保留 Maven 最终退出状态。随后验证 Desktop/Web 文件交换和 Web 服务端授权；完善更多 OWL 2 格式样例、编辑器级往返、完整依赖许可证审查与 OpenProtégé 最终许可证决策。（PROPOSED；尚未完成）
