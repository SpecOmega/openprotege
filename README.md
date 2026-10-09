# OpenProtégé

面向本体工程、语义知识建模与协作治理的开放平台。

> **项目阶段：Web 后端基础与身份/团队/项目 API**
>
> 当前已实现 Spring Boot/PostgreSQL 基础、本地账号邀请/会话、团队/项目元数据及初始服务端角色授权 API。React UI、桌面端、本体文件导入/编辑/版本、完整协作功能及 Compose bridge 部署验收尚未完成。

**语言：** 中文 | [English](README.en.md)

## 产品方向

OpenProtégé 的目标是支持个人与团队用户开展本体工程，并逐步形成桌面端与 Web 端协同的平台：

- **桌面端：** 完整的本地本体编辑工作流。
- **Web 端：** 项目管理与团队协作，支持公开/私有项目及角色权限。
- **首期跨端方式：** 通过用户发起的文件导入与导出衔接；实时同步留待评估。
- **本体格式：** 以 OWL 2 为核心，优先验证 RDF/XML、Turtle 等常用格式的支持与保真。
- **部署方向：** 自托管优先。
- **AI 扩展：** 首期目标为建议式建模与只读语义检索；采纳 AI 建议产生的写入必须由用户确认。

上述内容记录已确认的产品方向，不代表相关技术已选定、实现或通过验证。尤其是解析器与推理工具、桌面/Web 集成方式、角色权限细则以及实时同步方案，仍需后续验证与决策。

## 当前仓库状态

初始无源码基线提交 `d9caafee6858a958ea7a2944d574407a79f88309` 只包含 README。当前工作树已新增 `server/` 后端与 PostgreSQL Compose 基础，不应再把初始仓库状态当作当前代码状态。

当前范围与边界：

- 已添加 Java 21 / Spring Boot 3 服务、PostgreSQL 17、Flyway schema、Actuator readiness、显式首管理员引导、本地会话/CSRF、一次性邀请、团队/项目 API 与基础服务端角色校验。PostgreSQL Testcontainers 测试通过，覆盖邀请复用拒绝、会话/CSRF/logout、团队与项目隔离边界；Compose bridge 仍受当前执行环境阻塞。
- 目前不含 React Web UI、桌面应用、本体文件上传/解析/编辑/版本、完整账户恢复/防暴力破解机制或生产级多实例会话方案。
- 尚未验证本体导入、编辑、校验、保存、导出或格式往返保真。
- AI、语义检索和 Agent 均不是当前已实现功能。
- 本项目许可证尚未确定；Apache-2.0 正在作为候选方案评估。

审计基线、限制与未验证事项见[工程文档索引](docs/engineering/README.md)。

## 文档

| 文档 | 内容 |
|---|---|
| [产品愿景](docs/product/vision.md) | 产品定位、已确认方向与范围边界 |
| [需求规格草案](docs/requirements/SRS.md) | 功能、非功能需求及产品决策/技术验证的区分 |
| [用例草案](docs/requirements/use-cases.md) | 个人编辑、团队项目、文件交换和 AI 辅助场景 |
| [验收标准草案](docs/requirements/acceptance-criteria.md) | 未来可执行的验收条件；Web 基础项 AC-13 已有测试证据，其他产品功能仍未验证 |
| [需求追踪矩阵](docs/requirements/traceability-matrix.csv) | 需求到验收项和实现状态的追踪 |
| [身份/团队/项目数据模型](docs/architecture/data-model.md) | 首版数据实体、权限边界与待验证项 |
| [ADR-0002](docs/architecture/adr/0002-identity-team-project-authorization.md) | 身份认证与项目授权实现基线及验收门槛 |
| [工程审计与阶段报告](docs/engineering/README.md) | 仓库基线、架构盘点、构建/测试状态、安全、上游、风险和路线图 |

需求和工程文档以中文为主。各文档的英文翻译状态在[工程文档索引](docs/engineering/README.md)中标注；英文 README 不表示其他文档已有完整英文译本。

## 开发、构建与测试

Web 服务模块要求 Docker Engine/Compose v2。全部 5 项测试（含 PostgreSQL Testcontainers 集成测试）在 Java 21 / Maven 3.9.16 环境中通过，使用 Docker API `1.40`；覆盖服务基础与身份/项目授权。当前工作区的 `server/target` 和默认 Maven 缓存路径不可写，因此本次验证使用隔离源码副本和临时 Maven 缓存执行。可在正常可写的本地检出中运行：

```sh
mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test
```

首次启动空数据库还须设置 `ADMIN_BOOTSTRAP_EMAIL` 和 `ADMIN_BOOTSTRAP_PASSWORD`，不得使用仓库默认凭证。Compose bridge 网络限制见[构建与测试基线](docs/engineering/03-build-and-test-baseline.md)。当前没有可用的 Web UI 或本体导入/编辑工作流。

## 上游与许可证

Protégé Desktop 和 WebProtégé 是技术评估对象，不应视为当前 OpenProtégé 已集成的组件或已具备的功能。已固定上游提交的有限静态审计及验证限制见[上游审计](docs/engineering/upstream-audit.md)。

OpenProtégé 的许可证尚未确定。Apache-2.0 仅为待评估候选；在完成许可证及依赖审查之前，不应据此推断代码复用或分发条件。

## 参与项目

贡献流程、行为准则和安全报告渠道尚未在仓库中建立。相关政策完成并发布后，本节将补充对应说明。

## 联系与项目信息

- GitHub：<https://github.com/SpecOmega/openprotege>
- 目标网站：<https://openprotege.com>（项目目标；当前站点状态和服务可用性未经验证）
