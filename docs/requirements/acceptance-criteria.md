# OpenProtégé 验收标准（基线草案）

版本：0.1-draft

日期：2026-10-09

语言：中文主文档；英文翻译状态：Pending，见 [工程文档索引](../engineering/README.md)。

## 状态说明

本文件定义验收方式，不表示所有产品功能已交付。所有标准均需针对实际实现执行并保存结果。当前工作区有 React/Spring Web 切片，支持 PostgreSQL/MySQL 持久化、本体/AI chat API 和首版推理；AC-13、AC-20 及双语样例相关的部分路径有 Testcontainers 集成测试证据，但这些测试类须显式设置 `-Dopenprotege.testcontainers.enabled=true` 才运行。2026-10-11 的默认 Maven 测试执行中 22 项里 13 项通过、9 项跳过，不能视为数据库集成测试通过。开发配置可直接连接本机数据库，不要求容器；本机原生数据库端到端验收见 AC-21。Python 场景 runner 尚未在本机数据库实际执行；浏览器 E2E、完整协作和其他业务验收仍未完成。负责人确认的方向不等于技术验证。

## 验收项

| AC ID | 验收条件 | 验证方式 | 当前状态 |
|---|---|---|---|
| AC-01 | 个人用户能在桌面端选择一个经确认的测试本体，在本地编辑并保存；关闭并重新打开后，编辑结果仍可读。 | 桌面端自动化/手工验收；记录版本、输入文件哈希、命令/步骤和保存结果。 | BLOCKED：无桌面应用。 |
| AC-02 | 团队用户能在 Web 创建或打开项目，并能按确认的规则设置公开或私有状态。 | Web 集成/E2E；分别验证匿名和认证访问行为。 | PARTIAL：React UI 和后端 API 支持项目创建/列表/visibility 设置；public/private API 由 PostgreSQL 集成测试覆盖；浏览器 E2E 未完成。 |
| AC-03 | 团队角色为 Owner/Admin/Member，项目角色为 Owner/Admin/Editor/Viewer；匿名仅可读取公开项目。团队项目要求调用者同时是团队成员和显式项目成员。项目 Owner/Admin 可管理元数据/成员，Viewer 只读；Editor 的本体写能力留待 ontology 模块。 | 对每个角色/资源/操作组合运行正反向 API 集成用例，含匿名公开读、私有拒绝、团队成员但非项目成员拒绝和跨项目访问。 | PARTIAL：元数据角色矩阵和本体 Editor/Viewer 导入/导出边界已有 PostgreSQL 测试；删除/撤销生命周期及完整角色组合仍待覆盖。 |
| AC-04 | 按批准的测试语料，RDF/XML 和 Turtle 支持范围内的文件可识别、导入、校验和导出。 | 固定文件及哈希；覆盖有效、无效、损坏、不支持格式；记录解析器版本和诊断。 | PARTIAL：OWLAPI 5.1.20 已支持服务端 RDF/XML/Turtle 小样例导入、版本存储、导出及格式转换；Pizza 固定样例/格式保真/最大尺寸尚未验收。 |
| AC-05 | 导出的本体再次加载后满足事先批准的 OWL 2 语义保真准则；差异有可解释结果。 | 固定本体样本；采用评审通过的语义比较办法；逐一记录格式与工具版本。 | BLOCKED：准则、工具和样本集未冻结。 |
| AC-06 | 首期桌面/Web 文件交换由用户显式触发；错误、目标项目和结果清晰；不宣称实时同步。 | 往返 E2E：Web 导出→桌面编辑→Web 导入；验证失败路径与目标基线。 | BLOCKED：两端均无实现。 |
| AC-07 | 文件过大、损坏、畸形或解析超限时，系统拒绝或中止操作，显示错误，且不把失败数据提交为成功结果。 | 边界/负向测试；大小和资源限值须先由产品/部署决策确定，不在本草案虚构数值。 | PARTIAL：默认 500 MiB 上传限制、超限服务单元测试、畸形文件拒绝及失败不创建版本已有实现/测试；最大尺寸、超时/复杂度和 XXE 专项测试未完成。 |
| AC-08 | 外部 IRI/URI 处理遵守明确网络策略；测试不能导致访问策略禁止的目标。 | 在隔离网络中对允许/拒绝 URI 和重定向行为进行测试，不触碰生产数据。 | PARTIAL：OWL imports 被重映射到临时本地空文档，解析不自动联网；其他外部 IRI、XXE 与 SSRF 专项验证仍未完成。 |
| AC-09 | AI 建议在用户明确确认之前不改变本体；用户拒绝或校验失败时无持久化写入。 | 记录输入、建议、校验、用户动作及变更前后状态；包含未确认/拒绝/越权用例。 | PARTIAL：默认关闭的通用 AI chat 已实现且无本体写入口；结构化建模建议、校验、用户确认和写入审计未实现，无法完成端到端验收。 |
| AC-10 | 首期语义检索只返回用户有权访问的结果，且不改变任何项目数据。 | 检索前后状态比较，使用多项目授权测试数据。 | BLOCKED：语义检索未实现；身份/项目基础权限仅覆盖 AC-03 所列路径，尚不足以验收检索过滤。 |
| AC-11 | 自托管部署、备份、恢复及升级步骤按冻结的支持环境可重复执行。 | 使用干净环境执行部署/恢复演练，保存工具版本、命令、退出码和日志。 | PARTIAL：Compose 部署路径和详细运行/备份操作文档已存在；Compose 网络端到端、备份恢复/升级演练与 RPO/RTO 尚未验收。 |
| AC-12 | 任何需求验收都能从需求 ID 追踪到实现与测试；未实现项不得标记为完成。 | 检查 [追踪矩阵](./traceability-matrix.csv) 中实现、测试和证据列。 | 追踪矩阵覆盖需求实现/测试状态；AC-13、AC-20、AC-22 等具有部分可复现证据。仍有需求需补充完整测试链接并随实际验收更新状态。 |
| AC-13 | Web 服务在所选数据库不可用时不得报告就绪；数据库可用时健康探针为 `UP`，Flyway 可执行待应用迁移后才允许报告就绪。 | 用 Testcontainers 分别验证 PostgreSQL/MySQL readiness、schema migration 与数据库查询；验证 Compose 应用 healthcheck 与数据库健康依赖。Testcontainers 测试需显式启用，不能以默认 Maven 测试中被跳过的结果作为证据。 | PostgreSQL readiness UP/DOWN 和 migration 测试通过；MySQL 8.4 Spring HTTP readiness、Flyway、认证 API 和关键业务链路已由 AC-20 Testcontainers 集成测试覆盖（见工程证据 E-47）。当前默认 Maven 执行 22 项中 9 项跳过，且容器集成未重跑。隔离 Compose 的 PostgreSQL 与 server bridge 启动仍因当前环境连接超时而 BLOCKED；MySQL Compose 整栈 readiness 仍须在常规 Docker 主机复测。 |
| AC-14 | 首版没有自助注册；管理员签发的邀请 token 只能使用一次且有期限；受邀者设置本地密码后才能登录。 | PostgreSQL API 测试覆盖成功、过期/重复 token、邀请邮箱规范化和密码不落明文。 | PARTIAL：成功、过期、重放拒绝、规范化邮箱、token 摘要存储、非管理员拒绝签发和设置密码路径已测试；邀请撤销与完整账户生命周期仍待实现。 |
| AC-15 | 本地会话登录、CSRF 防护和退出失效工作；未认证的受保护操作被拒绝。 | HTTP 集成测试验证登录前后会话、无/错误 CSRF token、退出后复用旧会话以及密码错误。 | VERIFIED：API 测试验证有效登录、错误密码拒绝、CSRF 请求头、缺失 token 拒绝及退出后私有资源不可读。 |
| AC-16 | 空库管理员引导必须使用部署者显式提供的配置；不得创建默认管理员或匿名提权入口。 | 启动测试覆盖空库有配置/无配置及已有用户再次启动；审查日志无密码。 | PARTIAL：空库有配置的引导/密码散列由 PostgreSQL 测试验证；缺少配置失败由单元测试验证；已有用户重复启动和完整日志审查未覆盖。 |
| AC-17 | 授权用户能启动已安装的 HermiT；系统检查 OWL 2 DL Profile/一致性/不可满足类，展示可用解释，并支持命名类层级及命名个体自动分类。全局冲突解释在逻辑公理超过 2,000 时应明确标记不可用；超限、超时、并发忙碌、无权和未知实体均不得伪装为成功。 | HermiT 最小 OWL fixtures 覆盖 profile、全局冲突解释公理、层级和个体分类；HTTP 集成覆盖访问控制/错误码；验证解释上限提示、部署限值、超时和并发。 | PARTIAL：服务层测试覆盖 profile、冲突解释、推断层级/个体分类、未知类、axiom 上限及不可用引擎；浏览器交互、HTTP 权限矩阵、解释上限负向路径、超时取消和并发压力未验证。 |
| AC-18 | 授权用户能对指定本体版本临时应用一条 SWRL 规则，并看到目标个体的推断类型；规则及结果不得修改或持久化到不可变版本；非法规则、未知个体和推理失败须显式报告。 | 服务单元测试验证完整 IRI Manchester SWRL 规则可推出预期命名类型；比较本体版本前后哈希/内容，验证无写入；HTTP/UI 测试验证错误与只读行为。 | PARTIAL：服务单元测试验证临时规则推出类型且实现不写数据库；HTTP/UI 端到端、非法规则/未知个体的持久性检查及资源边界尚未验收。 |
| AC-19 | Owner/Admin/Editor 可选择同一项目任意历史版本恢复；系统必须创建新的不可变版本并保留全部既有版本，恢复内容与来源版本一致且格式/本体元数据保留；Viewer/跨项目/未知版本请求被拒绝；审计记录来源与新版本关系。 | PostgreSQL HTTP 集成测试覆盖成功恢复、响应新 ID、源文件导出逐字节一致、历史数增加、审计来源关系及 Viewer 拒绝；另测跨项目/不存在版本。 | PARTIAL：HTTP 集成测试验证 Owner 成功、Viewer 拒绝、新快照 ID、格式/文件名、导出内容和来源审计；Web 支持每页 50 条浏览历史版本；跨项目/未知版本、浏览器 E2E 与最大尺寸恢复边界未验收。 |
| AC-20 | 运维人员可用配置选择 dev/prod 运行配置与 PostgreSQL/MySQL 数据后端；所选后端启动时应用匹配的 schema migration，支持身份/项目数据及本体二进制快照读写。 | PostgreSQL 与 MySQL Testcontainers 测试迁移、readiness 与业务 API；MySQL 验证 UUID、团队/项目成员角色 upsert、会话登录、项目创建、本体导入/导出/恢复、HermiT 个体分类、审计与 LONGBLOB 往返；分别检查 Compose 配置和 profile 选择。Testcontainers 测试需显式启用。 | PARTIAL：PostgreSQL 与 MySQL 集成路径有已记录的 Testcontainers 通过证据（MySQL HTTP 流程见 E-47）；当前默认 Maven 执行不运行容器测试，22 项中 9 项跳过。dev/prod Spring 配置、Vite API proxy 和 PostgreSQL/MySQL Compose config 已实现。Compose 容器间网络、生产 MySQL Compose 整栈、备份恢复及跨引擎迁移未验收。 |
| AC-21 | 开发者能无需 Docker/容器，在本机安装的 PostgreSQL 或 MySQL 上直接构建并运行后端和 Web 前端；登录、授权、项目、本体、推理等业务行为由 dev/prod 共用代码实现，dev/prod 差异只来自明确的环境配置；生产 Compose 配置与行为保持不变。 | 在无 Docker daemon 的主机分别使用 PostgreSQL、MySQL 原生服务启动 dev 后端，检查 Flyway/readiness，并执行登录、授权、项目与本体读写 API；启动 Vite 前端验证 API 代理和用户流程；在 prod 模式执行相同 API 回归及 Compose 配置校验。 | PARTIAL：已增加 localhost DB host/port 配置、无容器启动说明及 VS Code 后端 + Vite F5；PostgreSQL/MySQL 共用服务代码及容器集成测试已覆盖若干数据路径。当前环境未安装本机数据库服务，尚未完成无 Docker 的原生服务端到端验收及完整 dev/prod API 同构测试。 |
| AC-22 | 提供中英文可执行的开发业务样例，覆盖受邀用户、团队/项目角色、私有项目隔离、本体导入/导出、推理与历史恢复；每个通过/失败/未验证边界均有记录。 | 在隔离 dev 数据库分别运行 `docs/guides/examples/run_business_scenario.py` 的中文/英文参数；自动检查 API 状态及返回内容，并用浏览器手工/E2E 检查前端角色流程。 | PARTIAL：已加入中文/英文 Turtle fixtures、对应操作手册和共用 Python API runner；PostgreSQL HTTP 集成测试覆盖两个 fixture 的 Turtle 导入/导出、HermiT 分类和临时 SWRL，已有 API 测试覆盖邀请/角色/本体权限。runner 本身尚未在本机原生数据库执行，浏览器 E2E 未完成；具体状态见双语场景与[已知缺口](../engineering/known-gaps.md)。 |

## 通用测试记录要求

每次验收至少记录：需求/验收 ID、代码提交 SHA、环境与工具版本、测试数据来源与哈希、实际命令/步骤、退出码、实际通过/失败数量（若工具提供）、错误日志摘要、限制和复现方式。未执行须标 `UNVERIFIED` 或 `BLOCKED`；不能以空输出或页面存在判定通过。

## 冻结前待确认

- RDF/XML/Turtle 的详细语法/OWL 2 Profile 支持范围和对相对 IRI 的策略。
- 语义保真对等标准、样本集合及规范化/比较方法。
- 项目角色 API 的完整允许/拒绝操作细节与 API 错误语义；基础读写边界已由负责人确认。
- 邀请有效期/撤销、登录限速、密码恢复与会话多实例策略。
- 文件大小、解析时间/复杂度、性能门槛和支持环境。
- AI 建议来源追踪、外部数据处理、检索索引权限过滤及审计字段。
- 自托管部署、备份保留和恢复目标。
