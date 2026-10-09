# 01 — 当前架构事实与边界

审计日期：2026-10-09

对象：初始无源码基线提交为 `d9caafee6858a958ea7a2944d574407a79f88309`；当前 Web 基础模块工作树基于 `main` HEAD `5734e976846b5de683caa67c9ae2115d8a5017ae`。

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## 当前状态

| ID | 架构面 | 当前证据 | 状态 |
|---|---|---|---|
| AR-01 | 应用入口与模块边界 | 新增 `server/` Java 21/Spring Boot 3.5.6 服务入口与 Maven 模块；Testcontainers 集成测试通过，Compose bridge 验收受阻。 | VERIFIED（测试范围）；BLOCKED（Compose） |
| AR-02 | 前端、桌面端、后端、服务进程 | 有 Actuator HTTP 服务和 Compose 容器定义；React UI 与桌面端尚未实现。 | SOURCE-OBSERVED |
| AR-03 | 数据库、对象存储、缓存、事件队列 | PostgreSQL 17/Flyway 和用户、邀请、团队、项目、成员、审计事件业务 schema 已添加并通过集成测试；对象存储/缓存/事件队列尚无。 | VERIFIED（迁移和测试范围）；SOURCE-OBSERVED（其余） |
| AR-04 | 本体解析、推理、RDF/OWL/SPARQL/SHACL | OWLAPI 已获批作为后续解析器选择，但尚未加入服务依赖或调用；当前无解析功能。 | SOURCE-OBSERVED |
| AR-05 | 身份、授权、审计、项目隔离 | 已实现本地密码散列/会话、CSRF、一次性邀请、首管理员显式引导与初步团队/项目授权；关键访问路径有 PostgreSQL API 集成测试。 | VERIFIED（测试覆盖路径）；SOURCE-OBSERVED（实现范围） |
| AR-06 | AI、检索、Agent | README 有 AI 定位文字，但无实现证据。 | SOURCE-OBSERVED |
| AR-07 | 可运行的系统架构 | Testcontainers 的 3 项 PostgreSQL 集成测试通过，standalone 服务经 PostgreSQL 返回 HTTP 200 readiness；Compose healthcheck 会检测到本环境 bridge 连接故障并返回失败。 | VERIFIED（隔离测试/standalone 范围）；BLOCKED（Compose bridge） |

README 中的产品定位不能证明任何运行功能。具体模块盘点见 [02-module-inventory.md](./02-module-inventory.md)；目标能力状态见 [04-product-capability-matrix.md](./04-product-capability-matrix.md)。

## 系统边界图

```text
浏览器/HTTP 客户端（REST API；当前无 UI）
                │
                ▼
Java 21 / Spring Boot Web 服务（认证、团队/项目 API 与服务端授权）
                │ JDBC + Flyway                         │服务端 session + CSRF
                ▼
PostgreSQL 17（账号、邀请、团队、项目、成员、审计）
```

此图描述当前后端实现边界；不表示 React UI、桌面编辑、本体导入/导出、文件交换或完整协作功能已经实现。

## 技术选择结论

- 负责人已批准新 Web 平台基础采用 Java 21、Spring Boot 3、PostgreSQL/Flyway；React + TypeScript 用于未来 Web UI；OWLAPI 用于后续本体解析模块。栈选择是负责人决策，不等于构建或技术可行性验证。（VERIFIED：负责人选择；代码状态见下一项）
- 已新增 Web foundation 模块的 Maven POM、服务入口、环境化数据库连接、Flyway 自动配置、只暴露状态的 Actuator readiness 和本地 Compose 配置。readiness health group 同时包含 Spring readiness state 与数据库；健康端点不展示组件细节。（SOURCE-OBSERVED；集成测试及 standalone runtime 已验证）
- Compose 服务互联依赖本次 Docker bridge 网络；应用容器无法连接数据库时会 fail closed，healthcheck/`--wait` 明确失败。standalone host-network 成功不能替代 Compose 部署验收。（VERIFIED）
- 本地认证、团队/项目元数据和基础对象级授权已实现并经有限路径集成测试；账户恢复、暴力尝试防护、持久化/共享会话、完整角色矩阵和本体 API 仍未实现或未验证。（VERIFIED / UNVERIFIED）
- 旧 WebProtégé 上游没有被复制进此模块；固定版本评估与其维护风险仍以 [上游审计](./upstream-audit.md) 为准。
- 安全设计应将服务端授权、隔离及不可信本体文件视为未来需要验证的设计约束；这不是现状检查发现的漏洞。（PROPOSED）

## 目标架构输入（待决）

仍待明确：账户恢复/邀请撤销、支持多实例的 session store、完整并发与冲突语义、项目本体文件存储及版本迁移策略。负责人已确认桌面端完整本地编辑、Web 项目管理与协作、首期通过文件交换、实时同步以后评估；这些职责方向尚无跨端协议或互操作实现。（已确认决策见 SRS；实现状态 UNVERIFIED）
