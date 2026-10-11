# 02 — 模块清单

复核日期：2026-10-11

当前复核基准：Git HEAD `9df91e479c53a9a78f83e9e2f9143f9281bddba9`；本次文档更新尚未提交。最初无源码基线提交为 `d9caafee6858a958ea7a2944d574407a79f88309`。

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## 模块索引

当前包含 Web 后端基础、身份/团队/项目 API、本体文件导入/版本/导出、HermiT 推理服务及 React Web 工作区；双语业务 fixture 与 Python API 场景 runner 提供可重复演示路径；桌面端仍待逐步实现和确认。运行阻塞与验收边界见[已知缺口](./known-gaps.md)。

| 模块/文件 | 入口与职责 | 依赖 / 数据结构 / 对外接口 | 测试 | 限制与处理建议 |
|---|---|---|---|---|
| `README.md`、`README.en.md` | 中英文项目说明；不是应用入口。 | 无依赖声明、数据结构或 API。 | 无应用测试覆盖。 | 保持项目介绍与已验证事实一致；不能据此推断功能。 |
| `docs/` | 已跟踪的工程审计、需求与架构草案；含中英文业务示例、Turtle fixture 和开发者 API runner；非应用运行时模块。 | Markdown/CSV 文档、标准库 Python 脚本；不新增运行时依赖。 | 双语 ontology fixtures 由 PostgreSQL HTTP 集成测试导入/推理；runner 语法检查通过；native DB 和 browser E2E 仍待执行。 | 保留证据等级并随实测结果增量更新；脚本对开发数据库写入数据且不清理。 |
| `docs/engineering/poc/` | 独立 OWLAPI 文件格式往返 PoC harness 与依赖声明；不是产品源码或构建配置。 | OWLAPI 4.5.29 单一依赖；不被应用引用。 | 在固定 Desktop 测试样例上手动执行，结果见 E-24。 | 单样例格式读写证据，不代表 Desktop GUI/Web/应用级流程。 |
| `server/` | Java 21/Spring Boot 3 服务；dev/prod profiles；本地 Maven 启动无需容器，生产 Compose 仍可容器部署；Actuator；本地邀请、密码散列/会话/CSRF；团队/项目 API；OWLAPI RDF/XML/Turtle 导入、版本/导出/历史版本恢复 API；HermiT 推理与规则 API；可选 OpenAI-compatible AI 聊天 API；服务端对象授权。 | Spring Security/JDBC；PostgreSQL/MySQL 8.4 + vendor-specific Flyway migrations；OWLAPI 5.1.20、HermiT 1.4.5.519；推理单 worker/axiom/timeout 限制；AI 密钥仅从环境配置读取、不持久化。 | 默认 Maven 测试运行非容器测试；Testcontainers 集成测试须设置 `-Dopenprotege.testcontainers.enabled=true`。历史 PostgreSQL/MySQL 集成证据见 E-47/E-48；本次默认执行 22 项中 9 项跳过。另有推理、AI mock 与审计测试。 | 原生本机 DB 的无容器运行仍待验收；当前环境 Docker API 不兼容，容器集成复跑受阻；MySQL 生产 Compose bridge、跨引擎数据迁移、备份恢复未验证；AI 关闭为默认；无建议采纳/写回、账号恢复/登录限速、持久/多副本会话、本体实体编辑/版本删除；最大尺寸、推理超时取消与完整安全测试未验证。 |
| `web/` | React 19 + TypeScript 浏览器工作区；本地登录/邀请接受、项目/团队视图、本体快照导入导出/格式转换和历史版本恢复、按版本运行 HermiT/SWRL、AI chat。 | Vite 6；同源 REST/CSRF session；开发代理到 `localhost:8080`，Compose 镜像将静态 bundle 放入服务资源。 | `npm --prefix web run build` 执行 TypeScript 严格检查及 Vite production build。 | 尚无自动化浏览器/E2E、可访问性、国际化、账号恢复或新用户自助注册；团队/项目角色分配按账户 email 执行。 |
| `compose.yaml` | 生产式自托管 PostgreSQL 或 MySQL 与 Web 服务容器编排；非本地开发必需项。 | PostgreSQL 17 Alpine 默认，可由 `DB_VENDOR`/`DB_IMAGE` 切换至 MySQL 8.4；独立数据卷变量，DB_USER/DB_PASSWORD 和首次管理员 `ADMIN_BOOTSTRAP_*`；端口默认绑定 loopback。 | PostgreSQL/MySQL Compose 配置均已校验；Testcontainers MySQL 测试通过。容器间 bridge 启动仍受当前环境限制。 | 本机 dev 直接运行方式见 README；不提供 PostgreSQL↔MySQL 数据迁移；邀请 token 需运维人员安全交付；无生产备份/恢复方案。 |

## 未发现的预期边界

Web service foundation、初始用户/团队/项目领域 API、本体文件后端切片和 React 浏览器工作区已实现；桌面客户端、本体编辑、后台任务、插件、AI 建议采纳和语义检索仍未实现。AI chat 可按环境配置连接 OpenAI-compatible provider，但默认关闭且不读取本体内容。账户恢复、生产运行和多副本语义仍待定义/验证。上游项目各自的模块结构和构建结果另见 [upstream-audit.md](./upstream-audit.md)，不属于 OpenProtégé 模块。

| 审计项 | 结果 | 状态 |
|---|---|---|
| 产品编程语言/版本 | Web 后端目标为 Java 21（已获批，构建验证待完成）；桌面栈未定。 | PROPOSED / UNVERIFIED |
| UI 技术 / 桌面框架 | React + TypeScript Web 工作区已实现；桌面框架仍待决。 | SOURCE-OBSERVED |
| 后端 / 服务入口 | `server/` Spring Boot 3.5.6；Actuator health 与 `/api` 身份、团队、项目、本体文件和可选 AI chat REST API。 | SOURCE-OBSERVED |
| 数据库、文件存储、缓存 | PostgreSQL 17 或 MySQL 8.4 + vendor-specific Flyway schema；身份/团队/项目、本体原始版本 BYTEA/LONGBLOB 与审计 schema 已实现并由集成测试验证。 | VERIFIED（迁移/小文件测试）；最大尺寸未验证 |
| RDF、OWL、SPARQL、SHACL 库 | OWLAPI 5.1.20 已加入服务，用于 RDF/XML/Turtle 的解析和转换；完整许可证审查与 OWL 2 profile 符合性未完成。 | SOURCE-OBSERVED / PARTIAL |
| 解析器、推理机、校验器、转换器 | OWLAPI 解析/转换；HermiT OWL 2 DL Profile、一致性/解释、层级/个体分类与临时 SWRL 推理已实现并有目标单元测试。 | PARTIAL |
| API、事件、异步任务 | 本地认证/邀请、团队、项目及本体导入/导出/版本 API；审计事件持久化；无异步任务/消息队列。 | SOURCE-OBSERVED |
| 构建、测试、打包、发布模块 | Maven 服务构建、默认非容器测试、可选 Testcontainers/PostgreSQL/MySQL 集成测试、Dockerfile/Compose。 | VERIFIED（本次默认构建测试）；历史集成测试有记录（E-47/E-48），当前 Docker 兼容性导致复跑 BLOCKED；Compose bridge 端到端 BLOCKED |

## 保留、改进、隔离或替换

首个 Web foundation 模块按 ADR-0001 增量引入；业务模块应按需求、授权模型、数据所有权和验收条件逐项评审，不复制完整上游工程或提前创建空模块。（PROPOSED）

上游项目的模块边界、构建与测试证据属于上游自身，不是 OpenProtégé 模块；详见 [06-dependency-and-upstream-review.md](./06-dependency-and-upstream-review.md)。
