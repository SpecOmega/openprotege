# 02 — 模块清单

审计日期：2026-10-09

当前复核范围：`main` HEAD `5734e976846b5de683caa67c9ae2115d8a5017ae` 及本轮新增的 Web foundation 工作树。最初无源码基线提交为 `d9caafee6858a958ea7a2944d574407a79f88309`。

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## 模块索引

当前包含 Web 后端基础、身份/团队/项目 API、本体文件导入/版本/导出首个后端切片及 React Web 工作区；桌面端仍待逐步实现和确认。

| 模块/文件 | 入口与职责 | 依赖 / 数据结构 / 对外接口 | 测试 | 限制与处理建议 |
|---|---|---|---|---|
| `README.md`、`README.en.md` | 中英文项目说明；不是应用入口。 | 无依赖声明、数据结构或 API。 | 无应用测试覆盖。 | 保持项目介绍与已验证事实一致；不能据此推断功能。 |
| `docs/` | 已跟踪的工程审计、需求与架构草案；非应用模块。 | Markdown/CSV 文档，无运行时依赖。 | 对文档执行链接/格式检查；不代表应用测试。 | 保留证据等级并随实测结果增量更新。 |
| `docs/engineering/poc/` | 独立 OWLAPI 文件格式往返 PoC harness 与依赖声明；不是产品源码或构建配置。 | OWLAPI 4.5.29 单一依赖；不被应用引用。 | 在固定 Desktop 测试样例上手动执行，结果见 E-24。 | 单样例格式读写证据，不代表 Desktop GUI/Web/应用级流程。 |
| `server/` | Java 21/Spring Boot 3 服务；Actuator；本地邀请、密码散列/会话/CSRF；团队/项目 API；OWLAPI RDF/XML/Turtle 导入、版本与导出 API；可选 OpenAI-compatible AI 聊天 API；服务端对象授权。 | Spring Security/JDBC；OWLAPI 5.1.20；PostgreSQL/Flyway；AI 密钥仅从环境配置读取、不持久化；身份/项目、本体版本及审计表。 | Maven/Testcontainers 测试覆盖健康探针、业务迁移、身份/项目权限、本体小样例导入导出、AI 客户端请求、版本元数据和审计。 | AI 关闭为默认；无建议采纳/写回、账号恢复/登录限速、持久/多副本会话、本体编辑/恢复/删除；最大尺寸与完整安全测试未验证。 |
| `web/` | React 19 + TypeScript 浏览器工作区；本地登录/邀请接受、项目/团队视图、本体文件导入导出/版本、AI chat。 | Vite 6；同源 REST/CSRF session；开发代理到 `localhost:8080`，Compose 镜像将静态 bundle 放入服务资源。 | `npm --prefix web run build` 执行 TypeScript 严格检查及 Vite production build。 | 尚无自动化浏览器/E2E、可访问性、国际化、账号恢复或新用户自助注册；团队/项目角色分配按账户 email 执行。 |
| `compose.yaml` | 自托管/本地开发的 PostgreSQL 与 Web 服务容器编排。 | PostgreSQL 17 Alpine named volume；DB_USER/DB_PASSWORD 和首次管理员 `ADMIN_BOOTSTRAP_*`；端口默认绑定 loopback。 | 配置曾验证通过；本次修改后需复跑 `docker compose config --quiet`。Compose bridge 运行仍受当前环境限制。 | 邀请 token 需运维人员安全交付；无生产密钥管理、备份/恢复方案。 |

## 未发现的预期边界

Web service foundation、初始用户/团队/项目领域 API、本体文件后端切片和 React 浏览器工作区已实现；桌面客户端、本体编辑、后台任务、插件、AI 建议采纳和语义检索仍未实现。AI chat 可按环境配置连接 OpenAI-compatible provider，但默认关闭且不读取本体内容。账户恢复、生产运行和多副本语义仍待定义/验证。上游项目各自的模块结构和构建结果另见 [upstream-audit.md](./upstream-audit.md)，不属于 OpenProtégé 模块。

| 审计项 | 结果 | 状态 |
|---|---|---|
| 产品编程语言/版本 | Web 后端目标为 Java 21（已获批，构建验证待完成）；桌面栈未定。 | PROPOSED / UNVERIFIED |
| UI 技术 / 桌面框架 | React + TypeScript Web 工作区已实现；桌面框架仍待决。 | SOURCE-OBSERVED |
| 后端 / 服务入口 | `server/` Spring Boot 3.5.6；Actuator health 与 `/api` 身份、团队、项目、本体文件和可选 AI chat REST API。 | SOURCE-OBSERVED |
| 数据库、文件存储、缓存 | PostgreSQL 17 + Flyway；身份/团队/项目、本体原始版本 BYTEA 与本体审计 schema 已实现并由集成测试验证。 | VERIFIED（迁移/小文件测试）；最大尺寸未验证 |
| RDF、OWL、SPARQL、SHACL 库 | OWLAPI 5.1.20 已加入服务，用于 RDF/XML/Turtle 的解析和转换；完整许可证审查与 OWL 2 profile 符合性未完成。 | SOURCE-OBSERVED / PARTIAL |
| 解析器、推理机、校验器、转换器 | OWLAPI 解析/转换已实现；无推理器或 OWL profile 验证器。 | PARTIAL |
| API、事件、异步任务 | 本地认证/邀请、团队、项目及本体导入/导出/版本 API；审计事件持久化；无异步任务/消息队列。 | SOURCE-OBSERVED |
| 构建、测试、打包、发布模块 | Maven 服务构建、Testcontainers/PostgreSQL 集成测试、Dockerfile/Compose。 | VERIFIED（测试）；BLOCKED（Compose bridge） |

## 保留、改进、隔离或替换

首个 Web foundation 模块按 ADR-0001 增量引入；业务模块应按需求、授权模型、数据所有权和验收条件逐项评审，不复制完整上游工程或提前创建空模块。（PROPOSED）

上游项目的模块边界、构建与测试证据属于上游自身，不是 OpenProtégé 模块；详见 [06-dependency-and-upstream-review.md](./06-dependency-and-upstream-review.md)。
