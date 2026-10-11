# 09 — 工程路线图

复核日期：2026-10-11

本路线图描述当前工作树的实际进度和建议的下一步，不代表已承诺日期、人员或预算。具体差距、完成条件和阻塞原因见[已知缺口](./known-gaps.md)；需求验收状态见[验收标准](../requirements/acceptance-criteria.md)和[追踪矩阵](../requirements/traceability-matrix.csv)。

## 当前工程基线

- **已有代码切片**：Spring Boot/React Web、PostgreSQL 17/MySQL 8.4 持久层、dev/prod profile、本地登录/邀请、团队/项目角色、RDF/XML/Turtle 文件版本、HermiT 和临时 SWRL API/Web 面板。
- **已有可重现证据**：PostgreSQL/MySQL Testcontainers；身份/项目权限 API 测试；两份中英 Turtle fixtures 的 HTTP 导入/导出、HermiT 分类和 SWRL API 测试；可运行的双语 API 场景脚本。Testcontainers 不等同于本机原生数据库运行或 Compose 整栈部署；Python runner 和浏览器 E2E 当前环境未完整验收。
- **本机开发路径**：本机 PostgreSQL/MySQL + 后端 Maven + Vite，不依赖容器；现有检查环境没有本机数据库服务，因此 AC-21/22 的 host-native 运行仍是待完成验收。
- **产品缺口**：桌面端、实体级本体编辑、经批准的格式语义保真、生产级安全/性能边界、完整浏览器测试、AI 结构化建议/语义检索和实时协作仍未完成。

## P0 — 完成可复现开发和整栈验收

1. **原生 dev**：在无 Docker daemon 的主机分别安装 PostgreSQL 17/MySQL 8.4；从空库验证迁移、readiness、bootstrap、邀请、角色、Ontology、HermiT/SWRL 和前端代理。完成后用实际结果更新 AC-21。
2. **双语业务脚本**：在可丢弃的两种原生数据库上运行[中文场景](../guides/examples/business-scenario-zh.md)和[英文场景](../guides/examples/business-scenario-en.md)，保留 PASS/FAIL、版本及失败日志。脚本会写入记录、不会自动清库。
3. **Compose 整栈**：在标准 Docker 主机分别验收 PostgreSQL、MySQL Compose 的 server→database 网络、健康检查、关键业务 API 和重启持久性；将环境连接超时与产品故障分开记录。
4. **浏览器 E2E**：通过浏览器实现角色矩阵、文件导入/导出/恢复和 Reasoner/Rules 用户路径；API 集成成功不得替代 UI 验收。
5. **CI 入口**：项目当前未提供 `.github/workflows`；建立前端 build、Maven/testcontainers、迁移及容器构建的持续验证，并要求测试报告归档。

**阶段出口**：AC-21/22 从 `PARTIAL` 更新需有无容器真实数据库与完整场景输出；Compose 与浏览器项需各自保留独立环境证据。

## P1 — 稳定本体、推理与数据安全边界

- 批准 OWL 2 profile、RDF/XML/Turtle 语义保真标准和固定许可样例 corpus；包括两个双语 fixture 之外的复杂构造与规模样本。
- 验证最大上传、解析/推理超时取消、并发、异常资源释放、解释上限及恶意/损坏输入；阈值需经产品/部署决策确定，不能依据默认配置推断已满足需求。
- 补齐 HTTP/browser reasoning 和 SWRL 失败路径、越权、跨项目、未知实体、非法规则及无持久化变更验证。
- 完成账户撤销/停用、邀请安全传递、登录限速/恢复/MFA 与单/多实例会话决策。
- 完成项目角色主体×资源×操作矩阵、撤销成员、并发角色更新和所有 API 资源授权回归。
- 明确审计保留/查询、生产备份与恢复、升级、数据迁移和 RPO/RTO 并执行演练。

## P2 — 产品目标、许可和上线准备

- 评估并决定项目许可证，逐项审查上游/第三方代码来源及直接、传递依赖许可证。
- 确认桌面/Web 职责及跨端文件交换，按决定构建桌面编辑器和 Web 实体编辑/保存/冲突体验。
- 对外部 IRI/SSRF/XXE、上传解析、会话授权、AI provider 数据外发和提示注入进行威胁建模与安全审查；当前 `owl:imports` 本地重映射不应误读为所有网络风险已验收。
- 明确生产 TLS 反向代理、密钥管理、监控/告警、恢复演练和升级策略。
- 完成界面国际化、可访问性和正式浏览器/操作系统支持矩阵。

## P3 — 可选扩展

- 在本体编辑、审计和授权基础稳定后评估实时协作、冲突解决、评审/通知。
- AI 先支持可追溯的结构化建议及用户确认写入；授权过滤语义检索单独验收。
- Agent/插件若进入范围，先满足沙箱、工具 allowlist、最小权限、人类审批、审计和拒绝越权测试。

## 过程约束

每项工作都必须关联需求 ID、验收、实现和运行证据；部分实现保留 `PARTIAL`，未运行保持 `UNVERIFIED`，环境不具备时标 `BLOCKED`。不得用文档存在、服务启动或单个健康检查替代完整产品验收。英文翻译状态见[工程文档索引](./README.md)。
