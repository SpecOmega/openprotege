# 02 — 模块清单

审计日期：2026-10-09

当前复核范围：`main` HEAD `5734e976846b5de683caa67c9ae2115d8a5017ae` 及本轮新增的 Web foundation 工作树。最初无源码基线提交为 `d9caafee6858a958ea7a2944d574407a79f88309`。

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## 模块索引

当前新增 Web 后端基础与首个身份/团队/项目业务切片；桌面端、本体工程和 Web UI 仍待逐步实现和确认。

| 模块/文件 | 入口与职责 | 依赖 / 数据结构 / 对外接口 | 测试 | 限制与处理建议 |
|---|---|---|---|---|
| `README.md`、`README.en.md` | 中英文项目说明；不是应用入口。 | 无依赖声明、数据结构或 API。 | 无应用测试覆盖。 | 保持项目介绍与已验证事实一致；不能据此推断功能。 |
| `docs/` | 已跟踪的工程审计、需求与架构草案；非应用模块。 | Markdown/CSV 文档，无运行时依赖。 | 对文档执行链接/格式检查；不代表应用测试。 | 保留证据等级并随实测结果增量更新。 |
| `docs/engineering/poc/` | 独立 OWLAPI 文件格式往返 PoC harness 与依赖声明；不是产品源码或构建配置。 | OWLAPI 4.5.29 单一依赖；不被应用引用。 | 在固定 Desktop 测试样例上手动执行，结果见 E-24。 | 单样例格式读写证据，不代表 Desktop GUI/Web/应用级流程。 |
| `server/` | Java 21/Spring Boot 3 服务；Actuator；本地邀请、密码散列/会话/CSRF；团队、项目和成员角色 API；服务端对象授权。 | Spring Security/JDBC；PostgreSQL/Flyway；`users`、`user_invitations`、`teams`、`team_memberships`、`projects`、`project_memberships`、`audit_events`。 | Java 21/Maven 3.9.16/Docker API 1.40 下 5/5 测试通过：健康探针、迁移、邀请过期/重用/授权、会话/CSRF/logout、公开/私有和团队项目授权。 | 未有 UI、账户恢复/登录限速、持久/多副本会话、本体 API；Compose bridge 仍受环境阻塞。 |
| `compose.yaml` | 自托管/本地开发的 PostgreSQL 与 Web 服务容器编排。 | PostgreSQL 17 Alpine named volume；DB_USER/DB_PASSWORD 和首次管理员 `ADMIN_BOOTSTRAP_*`；端口默认绑定 loopback。 | 配置曾验证通过；本次修改后需复跑 `docker compose config --quiet`。Compose bridge 运行仍受当前环境限制。 | 邀请 token 需运维人员安全交付；无生产密钥管理、备份/恢复方案。 |

## 未发现的预期边界

Web service foundation 与初始用户/团队/项目领域及受测 API 已实现；仍未发现/实现 React Web UI、桌面客户端、本体文件处理、后台任务、插件或 AI/Agent。账户恢复、完整授权矩阵、生产运行和多副本语义仍待定义/验证。上游项目各自的模块结构和构建结果另见 [upstream-audit.md](./upstream-audit.md)，不属于 OpenProtégé 模块。

| 审计项 | 结果 | 状态 |
|---|---|---|
| 产品编程语言/版本 | Web 后端目标为 Java 21（已获批，构建验证待完成）；桌面栈未定。 | PROPOSED / UNVERIFIED |
| UI 技术 / 桌面框架 | React + TypeScript 已获批为 Web UI 方向，尚未实现；桌面框架仍待决。 | SOURCE-OBSERVED |
| 后端 / 服务入口 | `server/` Spring Boot 3.5.6；Actuator health 与 `/api` 认证、团队、项目 REST API。 | SOURCE-OBSERVED |
| 数据库、文件存储、缓存 | PostgreSQL 17 + Flyway；身份/团队/项目 schema 已建并测试；本体文件存储未建。 | VERIFIED（schema测试）；SOURCE-OBSERVED（配置） |
| RDF、OWL、SPARQL、SHACL 库 | OWLAPI 4.5.29 仅存在于隔离 PoC；尚未加入服务 POM。后续解析将复核版本和许可证。 | SOURCE-OBSERVED |
| 解析器、推理机、校验器、转换器 | 未发现 | SOURCE-OBSERVED |
| API、事件、异步任务 | 本地认证/邀请、团队及项目 API；管理操作落审计事件；无异步任务/消息队列。 | SOURCE-OBSERVED |
| 构建、测试、打包、发布模块 | Maven 服务构建、Testcontainers/PostgreSQL 集成测试、Dockerfile/Compose。 | VERIFIED（测试）；BLOCKED（Compose bridge） |

## 保留、改进、隔离或替换

首个 Web foundation 模块按 ADR-0001 增量引入；业务模块应按需求、授权模型、数据所有权和验收条件逐项评审，不复制完整上游工程或提前创建空模块。（PROPOSED）

上游项目的模块边界、构建与测试证据属于上游自身，不是 OpenProtégé 模块；详见 [06-dependency-and-upstream-review.md](./06-dependency-and-upstream-review.md)。
