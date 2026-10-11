# OpenProtégé

面向本体工程、语义知识建模与协作治理的开放平台。

> **项目阶段：Web 工作区、后端 API 与首个本体文件/AI chat/推理切片**
>
> 当前已实现 Spring Boot/Web 基础，可配置 PostgreSQL 或 MySQL 数据库，本地账号邀请/会话、团队/项目 API、React Web 工作区、RDF/XML/Turtle 本体文件导入/版本/导出/历史版本恢复、HermiT OWL 2 DL profile/一致性/解释/类层级/个体分类及临时 SWRL 规则推理切片，以及默认关闭且服务端配置密钥的 OpenAI-compatible AI chat。桌面端、本体编辑、格式往返验收、最大文件资源测试、完整协作功能、浏览器 E2E 及 Compose bridge 部署验收尚未完成。

**语言：** 中文 | [English](README.en.md)

## 产品方向

OpenProtégé 的目标是支持个人与团队用户开展本体工程，并逐步形成桌面端与 Web 端协同的平台：

- **桌面端：** 完整的本地本体编辑工作流。
- **Web 端：** 项目管理与团队协作，支持公开/私有项目及角色权限。
- **首期跨端方式：** 通过用户发起的文件导入与导出衔接；实时同步留待评估。
- **本体格式：** 以 OWL 2 为核心，优先验证 RDF/XML、Turtle 等常用格式的支持与保真。
- **部署方向：** 自托管优先。
- **AI 扩展：** 首期目标为建议式建模与只读语义检索；采纳 AI 建议产生的写入必须由用户确认。

上述内容记录已确认的产品方向，不代表所有技术已实现或通过验证。推理首版已选用 HermiT；桌面/Web 集成方式、角色权限细则以及实时同步方案仍需后续验证与决策。

## 当前仓库状态

初始无源码基线提交 `d9caafee6858a958ea7a2944d574407a79f88309` 只包含 README。当前工作树已新增 `server/` 后端与 PostgreSQL Compose 基础，不应再把初始仓库状态当作当前代码状态。

当前范围与边界：

- 已添加 Java 21 / Spring Boot 3 服务、PostgreSQL 17/MySQL 8.4 可选后端、独立 Flyway migration、Actuator readiness、显式首管理员引导、本地会话/CSRF、一次性邀请、团队/项目 API 与基础服务端角色校验。PostgreSQL 与 MySQL Testcontainers 集成路径有历史通过记录（见工程证据 E-47/E-48）；当前默认 `mvn test` 会跳过这三组容器集成测试，Compose bridge 在当前执行环境中仍不能完成端到端运行验收。
- Web UI 已包含本地登录/邀请接受、团队/项目列表与创建、项目成员设置、本体快照导入/版本浏览/文件下载/格式转换和将历史版本恢复为新快照，以及 AI chat。恢复和导入仅能由 Owner/Admin/Editor 执行，不覆盖旧版本。具体浏览器流程尚未做 E2E 验收；本体实体编辑/版本删除、桌面应用、完整账户恢复/防暴力破解机制和生产级多实例会话方案未实现。
- 本体解析有小型集成样例；Pizza 本体往返保真、500 MiB 最大尺寸下的性能与资源安全尚未验证。
- 推理首版使用 HermiT 提供 OWL 2 DL profile/一致性检查、不可满足类与全局不一致解释、目标类层级查询、个体自动分类和单次只读 SWRL 规则应用；Web 提供 HermiT 启动、Ctrl+R/Cmd+R 快捷键及 Reasoner/Rules 面板。服务端单 worker、100,000 公理和 60 秒默认上限；全局冲突解释最多检查 2,000 个逻辑公理。功能单元测试覆盖有效 profile、冲突解释、个体分类和 SWRL 推断；超时取消、并发压力、大型/恶意本体资源消耗及浏览器 E2E 未验收。
- AI chat 可通过服务端环境变量选择 OpenAI-compatible provider（默认 DeepSeek 配置模板）；功能默认关闭，密钥不下发浏览器且不存入数据库。AI 不会读取本体或写入项目，语义检索和 Agent 未实现；真实 provider 联通/隐私审查尚未完成。
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
| [开发与部署保姆级指南](docs/guides/development-and-deployment.md) | 从工具安装、本机数据库初始化、dev 前后端启动，到 PostgreSQL/MySQL Compose 部署、备份和故障排查 |
| [中文端到端开发场景](docs/guides/examples/business-scenario-zh.md) | 社区健康服务本体、角色权限及可执行 API 全流程；标注 UI/E2E 验收边界 |
| [English end-to-end developer scenario](docs/guides/examples/business-scenario-en.md) | Learning ontology, role permissions, executable API workflow, and UI/E2E limits |
| [身份/团队/项目数据模型](docs/architecture/data-model.md) | 首版数据实体、权限边界与待验证项 |
| [ADR-0002](docs/architecture/adr/0002-identity-team-project-authorization.md) | 身份认证与项目授权实现基线及验收门槛 |
| [工程审计与阶段报告](docs/engineering/README.md) | 仓库基线、架构盘点、构建/测试状态、安全、上游、风险和路线图 |

需求和工程文档以中文为主。各文档的英文翻译状态在[工程文档索引](docs/engineering/README.md)中标注；英文 README 不表示其他文档已有完整英文译本。

## 开发、构建与测试

本地开发运行不依赖 Docker；需要 Java 21、Maven 3.9+、Node.js 22+，以及本机安装并运行的 PostgreSQL 17 或 MySQL 8.4。Docker Engine/Compose v2 仅用于生产式容器部署和可选的容器化测试。

前端可单独构建：

```sh
npm ci --prefix web
npm --prefix web run dev
```

默认 Maven 测试命令运行非容器测试；Testcontainers 的 PostgreSQL/MySQL 集成测试默认跳过。当前记录的一次默认测试执行发现 22 项，其中 13 项通过、9 项跳过，不能据此宣称本机数据库或容器数据库集成通过。启用容器集成测试需显式设置：

```sh
mvn -B -ntp -Dopenprotege.testcontainers.enabled=true -Dapi.version=1.40 -f server/pom.xml test
```

此命令要求 Docker daemon 与 Testcontainers 兼容；当前检查环境曾报告 daemon 最低 API 为 1.40，而默认协商到 API 1.32，故容器集成测试未能在该环境复验。相关状态与限制见[构建/测试基线](docs/engineering/03-build-and-test-baseline.md)。

### 本地开发与调试

完整的逐步安装、建库、运行、登录和排错过程请先阅读[开发与部署保姆级指南](docs/guides/development-and-deployment.md)。dev 直接连接本机 PostgreSQL/MySQL，不依赖容器；生产 Compose 的 `prod` 配置不变。

```sh
cp .env.dev.example .env
# 编辑 .env，填入本机数据库账户及首次管理员信息；先启动本机数据库。
set -a; . ./.env; set +a
mvn -f server/pom.xml spring-boot:run
# 另开终端
npm ci --prefix web
npm --prefix web run dev
```

后端默认监听 `http://localhost:8080`，Vite 前端默认监听 `http://localhost:5173`。VS Code F5 可启动前后端；Windows PowerShell 启动步骤和 PostgreSQL/MySQL 建库 SQL 见[完整指南](docs/guides/development-and-deployment.md)。开发时访问 Vite 地址；生产 Docker 镜像由后端同源提供 Web 页面。

### PostgreSQL / MySQL 切换

PostgreSQL 17 是默认开发和生产选择。开发时在 `.env` 中将 `DB_VENDOR` 设为 `postgres` 或 `mysql`，`DB_HOST` 指向本机数据库，`DB_PORT` 使用数据库监听端口（PostgreSQL 默认 5432，MySQL 默认 3306）；两个引擎使用各自的 JDBC 驱动和 Flyway migration。MySQL 本体文件使用 `LONGBLOB`，UUID 以规范文本形式存储。本地切换数据库不迁移数据，须先备份并分别配置数据库账户。

生产 Compose 可通过 `.env` 切换至 MySQL 8.4；生产数据库容器的数据卷必须专用，不能把同一个数据卷在 PostgreSQL 与 MySQL 之间共用。MySQL Compose 示例设置如下：

```dotenv
APP_PROFILE=prod
DB_VENDOR=mysql
DB_IMAGE=mysql:8.4
DB_PORT=3306
DB_CONTAINER_PORT=3306
DB_VOLUME=mysql_data
DB_DATA_PATH=/var/lib/mysql
MYSQL_ROOT_PASSWORD=<独立的强 root 密码>
```

`DB_USER`、`DB_PASSWORD`、`DB_NAME`、管理员引导配置沿用 `.env`。这个切换只针对新建数据库，不是 PostgreSQL 到 MySQL 的数据迁移工具。运行 `docker compose up --build --wait` 前备份现有数据并确认 `DB_VOLUME` 指向目标数据库专用卷。

Maven 服务端测试（含 PostgreSQL Testcontainers 集成测试）使用 Docker API `1.40`：

```sh
mvn -B -ntp -Dopenprotege.testcontainers.enabled=true -Dapi.version=1.40 -f server/pom.xml test
```

### 生产 Compose 部署（本机）

完整 PostgreSQL/MySQL 配置、启动验证、日志查看和数据保护步骤见[生产部署章节](docs/guides/development-and-deployment.md#九使用-docker-compose-部署生产模式)。概览命令如下：

```sh
cp .env.example .env
chmod 600 .env
# 编辑 .env：设置独立的数据库密码和首管理员邮箱/密码，密码 12–128 个字符。
docker compose config --quiet
docker compose up --build --wait
curl --fail http://127.0.0.1:8080/actuator/health/readiness
```

应用和数据库端口默认只绑定到 `127.0.0.1`。管理员只在空数据库首次启动时引导创建；登录成功后移除 bootstrap 密码。`docker compose down` 默认保留命名数据库卷，删除卷会造成数据丢失。对外服务前须另行配置 TLS 反向代理、访问控制与备份恢复；这些不会由上述命令自动配置。上传限制、推理上限、可选 AI 和当前部署验收限制见[完整指南](docs/guides/development-and-deployment.md)及[工程文档索引](docs/engineering/README.md)。

## 上游与许可证

Protégé Desktop 和 WebProtégé 是技术评估对象，不应视为当前 OpenProtégé 已集成的组件或已具备的功能。已固定上游提交的有限静态审计及验证限制见[上游审计](docs/engineering/upstream-audit.md)。

OpenProtégé 的许可证尚未确定。Apache-2.0 仅为待评估候选；在完成许可证及依赖审查之前，不应据此推断代码复用或分发条件。

## 参与项目

贡献流程、行为准则和安全报告渠道尚未在仓库中建立。相关政策完成并发布后，本节将补充对应说明。

## 联系与项目信息

- GitHub：<https://github.com/SpecOmega/openprotege>
- 目标网站：<https://openprotege.com>（项目目标；当前站点状态和服务可用性未经验证）
