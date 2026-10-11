# 已知缺口、验收状态与后续要求

复核日期：2026-10-11

本文件按当前工作树描述缺口，不沿用初始无源码提交时的状态。`VERIFIED` 仅表示所列路径在所列测试条件下通过；`PARTIAL` 表示有实现和证据但未满足完整验收；`UNVERIFIED` 表示缺少运行/质量证据；`BLOCKED` 表示当前缺少实现或客观环境条件阻止验收。详细需求状态以[验收标准](../requirements/acceptance-criteria.md)、逐项追踪以[追踪矩阵](../requirements/traceability-matrix.csv)为准。

## 当前已实现且有测试证据的切片

- Java 21 / Spring Boot 后端、React/Vite Web、PostgreSQL 17 与 MySQL 8.4 profiles、按数据库 vendor 隔离的 Flyway migrations。
- 本机 `dev` 直接连接 PostgreSQL/MySQL；生产 `prod` Compose 保持独立；Vite/API proxy 与 VS Code 调试配置已提供。
- 本地会话/CSRF、关闭自助注册、显式 bootstrap 管理员、一次性管理员邀请、Team/Project 角色和服务端授权。
- Web 本体文件 API：RDF/XML/Turtle 导入、不可变快照、历史分页、导出/格式转换、恢复为新快照和审计。
- HermiT OWL 2 DL profile/一致性/解释/类层级/个体分类及单条临时 SWRL 推理预览。
- PostgreSQL/MySQL Testcontainers 验收以及中文/英文 Turtle 业务 fixture。双语 fixture 在 PostgreSQL HTTP 集成测试中执行导入/导出、HermiT 分类和 SWRL 推理；身份/团队/项目权限则由 API 集成测试覆盖。
- 完整 dev 和自托管配置说明见[开发与部署指南](../guides/development-and-deployment.md)。可执行业务案例：[中文](../guides/examples/business-scenario-zh.md)、[English](../guides/examples/business-scenario-en.md)。

上述内容不是完整产品验收：测试容器成功不证明本机原生数据库、浏览器、生产 Compose、资源上限或备份恢复已经通过。

## P0 — 阻止本机开发/发布验收的事项

| 缺口 | 当前状态 | 详细完成要求 |
|---|---|---|
| 无容器原生 dev 验收（AC-21） | BLOCKED：当前检查环境没有本机 PostgreSQL/MySQL 服务；已有 PostgreSQL/MySQL 容器测试不能替代本机服务 | 在无 Docker daemon 主机上分别安装 PostgreSQL 17 与 MySQL 8.4；从空数据库启动 dev profile，验证 Flyway/readiness、bootstrap 登录、邀请、团队/项目角色、本体导入/导出/恢复与推理；同时启动 Vite 并验证浏览器流程。保存 OS/JDK/DB/Node 版本、命令、退出码和日志。 |
| 双语场景 runner 原生执行（AC-22） | PARTIAL：Python 脚本已实现且语法检查通过；本机数据库链路未运行 | 以一次性或可丢弃开发库分别运行[中文场景](../guides/examples/business-scenario-zh.md)和[英文场景](../guides/examples/business-scenario-en.md)，确认输出 `PASS` 和权限负向断言。脚本会新增记录，不会清理；不可连接生产库。 |
| PostgreSQL/MySQL Compose 整栈 | BLOCKED/UNVERIFIED：当前环境曾出现 Compose server 与数据库间连接超时；MySQL Compose 整栈未取得可靠验收 | 在标准 Docker 主机从空卷分别执行 PostgreSQL、MySQL `docker compose up --build --wait`；验证 app→DB host/port、readiness、登录、关键 API、重启后持久性、日志无密钥泄露。每个数据库使用独立卷。 |
| 浏览器端到端与角色 UI | PARTIAL：React 有登录/团队/项目/本体/Reasoner/Rules 页面；API 测试不是浏览器验收 | 建立隔离浏览器测试：管理员邀请用户；Owner/Admin/Editor/Viewer 登录后分别验证可见控件及允许/拒绝操作；在 UI 完成导入、推理/SWRL、历史导出/恢复；检查刷新后会话、CSRF、错误提示和窄屏/可访问性基础行为。 |

## P1 — 产品核心和质量保证

| 能力/需求 | 状态与当前边界 | 推荐完成要求 |
|---|---|---|
| 桌面端完整编辑（AC-01） | BLOCKED：仓库尚无桌面应用；Web 文件快照 API 不等同于本地实体编辑器 | 确认 Desktop 架构/许可证/支持平台；实现打开、实体编辑、保存、重新打开、格式导出；使用真实本体执行自动化及手工编辑验收。 |
| Web 实体级编辑、版本删除与完整工作区操作 | BLOCKED/PARTIAL：当前 Web 是项目和文件快照工作区，无实体编辑或版本删除 | 先冻结编辑模型、冲突策略、撤销/审计及删除保留策略；实现后补充 API/浏览器测试，不要把文件上传等同于完整 ontology editor。 |
| OWL 2 格式/语义保真（AC-04/05） | PARTIAL：支持 RDF/XML/Turtle 的小样例解析；双语 fixture 的原格式导出能逐字节往返；Pizza/更广构造和经批准的语义等价基准未完成 | 冻结 OWL 2 profile 与错误诊断要求；建立许可明确的固定 corpus（含相对 IRI、imports、注释/语言标签、匿名个体、复杂公理）；通过解析后语义不变量或经批准的规范化比较定义保真，记录 OWLAPI 版本和哈希。 |
| 上传与推理资源安全（AC-07/17/18） | PARTIAL：有 500 MiB 上传上限及推理 worker/公理数/超时配置；最大输入性能、取消、并发压力与对抗样本未验 | 对 0/边界/超限文件、深层/复杂/恶意本体、解析失败、超时取消、线程耗尽和同项目并发执行做隔离测试；观察 CPU、内存、临时文件和失败后的资源清理；阈值须先审批。 |
| 推理 API/Web 验收（AC-17/18） | PARTIAL：服务测试和 HTTP/场景 fixtures 覆盖基本 HermiT/SWRL；全部 HTTP 负向权限路径和浏览器 Reasoner/Rules E2E 未完成 | 验证 Viewer 只读调用、跨项目/未知版本/未知实体、非法规则、非 OWL 2 DL、本体不一致、全局解释 2,000 公理限制、超时和 busy 响应；证明 SWRL 不更改版本内容/审计。 |
| 邀请与账户生命周期（AC-14/15/16） | PARTIAL：邀请不发送邮件；无自助注册、撤销邀请、停用账户、找回密码、登录限速/MFA；服务端会话是初始单实例方案 | 决定安全邮件/邀请传递、撤销、速率限制、恢复、MFA、会话过期/跨实例策略；完成威胁分析、浏览器流程、审计和自动化安全测试。 |
| 团队/项目完整权限生命周期（AC-03） | PARTIAL：核心角色及若干拒绝路径已有 PostgreSQL API 测试；完整操作矩阵、并发成员变更和所有本体资源跨项目验证不足 | 形成主体×资源×操作矩阵，涵盖匿名/public、Team Owner/Admin/Member、Project Owner/Admin/Editor/Viewer、成员撤销、角色降级、跨团队、并发更新和对象 ID 猜测；API 与 UI 都需负向测试。 |
| 审计管理 | PARTIAL：关键成员/本体操作有数据库审计；保留期、查询/导出 UI 与完整运维政策缺失 | 定义审计字段、隐私/保留期、访问控制、轮转/清除和运维查询导出；测试失败事件不泄露 token/password。 |
| 备份、恢复、升级与跨引擎迁移（AC-11/20） | UNVERIFIED：指南给出运行/备份方向，但没有生产级恢复演练；PostgreSQL 与 MySQL schema 不互通，未实现跨引擎数据迁移 | 决定 RPO/RTO/保留策略；分别执行数据库一致性备份、隔离环境恢复、版本升级/迁移演练；若需换引擎，设计并验证独立 ETL，不得将换 vendor 当作自动数据迁移。 |
| AI/DeepSeek provider、安全与语义检索（AC-09/10） | PARTIAL：可选 OpenAI-compatible chat 配置默认关闭、密钥在服务端；真实 provider 连接/隐私审查、结构化建议确认写入和语义检索未完成 | 明确第三方数据流/同意和日志脱敏；对真实 provider 做受控联通测试；结构化建议经验证、用户确认、权限校验后才可写；语义搜索必须过滤授权项目并可追溯。 |

## P2 — 许可、安全和上线工程

1. **许可证与来源**：项目 LICENSE 尚未确定；完成直接/传递依赖、第三方代码来源和许可证兼容审查后再批准项目许可证。
2. **安全评估**：完成认证/会话、CSRF、对象级授权、上传解析、外部 IRI/SSRF/XXE、AI 提示注入/数据外发、日志脱敏、依赖供应链的威胁建模和专项测试。OWL imports 当前重映射为本地空文档，不应据此宣称所有外部 IRI 风险已彻底关闭。
3. **CI/发布**：当前没有 `.github/workflows` 自动化流水线；为前端 build、Maven 单测/Testcontainers、migration、容器镜像/扫描及可复现版本建立 CI。明确 Testcontainers 所需 Docker runner。
4. **运维**：生产配置目前不自动提供 TLS 反向代理、高可用、监控告警或密钥管理；部署者需明确外围组件和可恢复性。

## P3 — 目标产品能力与体验

- 实时协同、冲突解决、评论/评审、通知、跨端同步未实现；首期只应描述为显式文件交换。
- 完整国际化、键盘可访问性、屏幕阅读器支持及正式可用性评估未验收。
- 插件系统、受限 Agent/自动化工具未实现；若启动须先设计沙箱、工具 allowlist、人类审批及不可绕过授权的审计。

## 推荐后续开发顺序

1. 完成本机 PostgreSQL/MySQL 无容器 dev 验收，并实际运行两份场景脚本；逐条更新 AC-21/22 和证据台账。
2. 加入浏览器 E2E 和角色权限矩阵；修复真实发现的问题，保持 API 和 Web 行为一致。
3. 完成 PostgreSQL/MySQL Compose 整栈及备份/恢复/升级演练，确定生产部署验收门槛。
4. 冻结 OWL 2 profile、保真 corpus 和资源安全阈值；补齐推理/上传边界和负向测试。
5. 另行排期桌面编辑器、Web 实体编辑、完整账户生命周期、AI provider/搜索与协作增强。

## 记录失败与证据的要求

每次执行应在[验收标准](../requirements/acceptance-criteria.md)或[证据台账](./evidence-ledger.md)记录 commit、操作系统/JDK/数据库/Node/Python 版本、配置（去掉密钥）、准确命令、退出码、通过/失败数量、日志摘要和复现方式。未执行为 `UNVERIFIED`，环境阻塞为 `BLOCKED`，部分覆盖为 `PARTIAL`；不得把容器数据库测试写成原生 no-container 验收，也不得把 API runner 写成浏览器 E2E。
