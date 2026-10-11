# OpenProtégé 开发与部署保姆级指南

本指南从全新检出开始，分别说明：

1. **本机开发（dev）**：Java 服务和 Web 前端直接运行在本机；数据库使用本机安装的 PostgreSQL 或 MySQL；不依赖 Docker 或任何容器。
2. **自托管部署（prod）**：使用 Docker Compose 运行 Web 服务和 PostgreSQL/MySQL 数据库。

两种模式运行同一套业务代码。`dev` 与 `prod` 的区别在日志/错误显示、数据库连接和部署方式等环境配置，不是两套功能实现。请不要用开发密码部署生产环境。

> 当前支持边界：Java 21、PostgreSQL 17 或 MySQL 8.4、Node.js 22+。MySQL 与 PostgreSQL 使用不同的 Flyway schema migration；切换数据库引擎不会迁移数据。完整业务范围及尚未验收的功能见[验收标准](../requirements/acceptance-criteria.md)。

## 目录

- [一、选择运行方式](#一选择运行方式)
- [二、准备开发工具](#二准备开发工具)
- [三、本机安装并准备数据库](#三本机安装并准备数据库)
- [四、设置本地开发配置](#四设置本地开发配置)
- [五、编译并运行后端与前端](#五编译并运行后端与前端)
- [六、登录并检查功能](#六登录并检查功能)
- [6.3、中英文端到端开发场景](./examples/business-scenario-zh.md)
- [七、使用 VS Code 调试](#七使用-vs-code-调试)
- [八、运行测试与生产构建](#八运行测试与生产构建)
- [九、使用 Docker Compose 部署生产模式](#九使用-docker-compose-部署生产模式)
- [十、运行维护、备份与升级](#十运行维护备份与升级)
- [十一、常见故障排查](#十一常见故障排查)

## 一、选择运行方式

| 方式 | 适用场景 | 需要安装 | 如何启动 |
|---|---|---|---|
| 本机开发 `dev` | 编写代码、断点调试、前端热更新 | Java、Maven、Node.js，以及本机 PostgreSQL 或 MySQL | Maven 启动后端；Vite 启动前端 |
| Compose `prod` | 本机试运行、自托管部署 | Docker Engine 与 Compose v2 | `docker compose up --build --wait` |

选择 `dev` 时，无需安装 Docker。需要安装真实数据库服务，因为应用使用 JDBC 和 vendor-specific Flyway migrations，当前没有内嵌数据库模式。

选择 `prod` 时，Compose 默认启动 PostgreSQL 17 与后端服务；也可以切换到 MySQL 8.4。这里的 `prod` 是生产运行配置，不会自动提供 TLS 反向代理、备份系统或高可用方案。

## 二、准备开发工具

### 2.1 必需工具

- **Git**：克隆项目。
- **Java JDK 21**：后端目标版本。请安装 JDK，而不仅是 JRE。
- **Maven 3.9 或更新的 3.x**：执行后端构建和运行。
- **Node.js 22 或更新的 22.x**：构建并启动 React/Vite 前端；npm 随 Node.js 安装。
- **PostgreSQL 17 或 MySQL 8.4**：本地开发数据库，二选一即可。
- **Docker Engine + Docker Compose v2**：仅生产 Compose 部署和 Testcontainers 集成测试需要。

可从这些项目的官方页面安装工具：

- [Java / Eclipse Temurin](https://adoptium.net/)、[Maven](https://maven.apache.org/install.html)、[Node.js](https://nodejs.org/en/download)
- [PostgreSQL 下载与安装](https://www.postgresql.org/download/)
- [MySQL 下载与安装](https://dev.mysql.com/downloads/)
- [Docker Engine](https://docs.docker.com/engine/install/) 与 [Compose](https://docs.docker.com/compose/install/)

Windows 可用 PostgreSQL 官方 Windows installer 或 MySQL Installer；macOS、Linux 请按对应操作系统页面安装数据库。安装数据库时保留默认本机监听端口，除非你会同步修改 `.env` 中的 `DB_PORT`。

### 2.2 检查工具版本

在终端运行：

```sh
java -version
mvn -version
node --version
npm --version
git --version
```

检查 Java 输出为 21、Node.js 主版本至少为 22。若机器上安装了多个 JDK，请检查 `mvn -version` 显示的 Java 版本；Maven 使用的 Java 也必须是 21。

### 2.3 克隆项目

```sh
git clone https://github.com/SpecOmega/openprotege.git
cd openprotege
```

如果已经克隆，只需进入仓库目录并按团队流程更新代码。不要把本地 `.env` 或数据库密码提交到 Git。

## 三、本机安装并准备数据库

以下数据库初始化只需执行一次。若已有 PostgreSQL 17/MySQL 8.4 服务，可跳到相应的“创建数据库”步骤。

### 3.1 PostgreSQL 17

1. 安装 PostgreSQL 17 并启动其本机服务。
2. 确认服务监听 `127.0.0.1:5432`。若端口不同，后续将 `DB_PORT` 改成实际端口。
3. 使用 PostgreSQL 管理员账户连接数据库（例如用 `psql -U postgres`，或用 pgAdmin）。
4. 创建应用专用用户和数据库。把密码替换成稍后 `.env` 中的 `DB_PASSWORD`：

```sql
CREATE ROLE openprotege LOGIN PASSWORD '替换为本机开发密码';
CREATE DATABASE openprotege OWNER openprotege;
```

如果提示角色或数据库已存在，不要反复创建；确认已有对象的所有者和密码与 `.env` 一致即可。应用用户需要是数据库 owner，以便首次启动时 Flyway 创建 schema。

上面的 `openprotege` 用户名和库名对应示例 `.env`。如果你改了 `DB_USER` 或 `DB_NAME`，SQL 和后续连接命令也要同步替换。

5. 验证新用户能登录目标数据库：

```sh
psql -h 127.0.0.1 -p 5432 -U openprotege -d openprotege -c "SELECT 1;"
```

Windows 若未将 `psql` 加入 `PATH`，可以在 PostgreSQL SQL Shell（psql）或 pgAdmin 中完成连接/查询。

### 3.2 MySQL 8.4

1. 安装 MySQL 8.4 并启动本机服务。
2. 确认服务监听 `127.0.0.1:3306`。若端口不同，后续将 `DB_PORT` 改成实际端口。
3. 使用 MySQL 管理员账户连接（例如 `mysql -u root -p`，或用 MySQL Workbench）。
4. 创建数据库、应用用户并授权。密码替换成稍后 `.env` 中的 `DB_PASSWORD`：

```sql
CREATE DATABASE openprotege CHARACTER SET utf8mb4;
CREATE USER 'openprotege'@'127.0.0.1' IDENTIFIED BY '替换为本机开发密码';
GRANT ALL PRIVILEGES ON openprotege.* TO 'openprotege'@'127.0.0.1';
```

MySQL 用户名包含 host 限定；这里使用 `127.0.0.1` 是因为示例 JDBC URL 通过该地址连接。若数据库账户是在 Workbench/Installer 中创建的，请确认存在与连接 host 匹配的账户并有 `openprotege.*` 的读写及 schema 管理权限。

如果修改 `.env` 中的 `DB_USER` 或 `DB_NAME`，请在上述 SQL 及验证命令中同步替换。

5. 验证应用用户能连接：

```sh
mysql -h 127.0.0.1 -P 3306 -u openprotege -p openprotege
```

输入密码后运行 `SELECT 1;`，再运行 `exit` 退出。

### 3.3 数据库隔离与切换

PostgreSQL 和 MySQL 的 schema 不同，必须让 `DB_VENDOR`、实际运行的数据库、端口与账号彼此对应。切换引擎前先备份数据，并使用另一引擎的新数据库；**更换 `DB_VENDOR` 不会转换或搬迁已有数据**。

## 四、设置本地开发配置

### 4.1 创建 `.env`

从仓库根目录执行：

```sh
cp .env.dev.example .env
```

用编辑器打开根目录 `.env`，至少核对以下值：

```dotenv
APP_PROFILE=dev
DB_VENDOR=postgres
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=openprotege
DB_USER=openprotege
DB_PASSWORD=替换为创建数据库用户时设置的密码
ADMIN_BOOTSTRAP_EMAIL=admin@localhost.test
ADMIN_BOOTSTRAP_PASSWORD=本机专用的长密码至少12个字符
```

若使用 MySQL，将 `DB_VENDOR=mysql`、`DB_PORT=3306`；其他值须与建库时设置匹配。数据库已运行在其他本机端口时同步修改 `DB_PORT`。

管理员引导密码须为 12–128 个字符。**本仓库示例密码仅供本地首次运行参考，不是生产凭证**；请替换它，不要复用任何真实账户密码。`.env` 已被 Git 忽略，但仍应限制其文件访问权限，不要复制进 issue、日志或截图。

> Linux/macOS 的下面命令以 Bash/Zsh 为例。若密码包含空格、`$`、反引号或 shell 特殊字符，不要直接 `source .env`；在终端安全地逐项导出环境变量，或使用 VS Code 的 `envFile` 启动配置。PowerShell 见[Windows 终端方式](#windows-powershell-方式)。

## 五、编译并运行后端与前端

第一次启动前端时安装依赖；以后更新 `web/package-lock.json` 后再运行 `npm ci`。

### 5.1 启动后端

在终端 1（仓库根目录）加载 `.env` 并启动 Spring Boot：

```sh
set -a
. ./.env
set +a
mvn -f server/pom.xml spring-boot:run
```

首次构建会下载 Maven 依赖，需要可访问项目 Maven 仓库。等待日志出现 Spring Boot started 信息；数据库连接失败时应用会以启动失败结束，不会报告 ready。服务默认监听 `http://localhost:8080`。

### 5.2 启动热更新 Web 前端

在终端 2（仓库根目录）执行：

```sh
npm ci --prefix web
npm --prefix web run dev
```

终端会显示 Vite 本地地址，默认是 `http://localhost:5173`。在浏览器打开该地址；Vite 会把 `/api` 和 `/actuator` 请求代理到本机后端 `http://localhost:8080`。若后端端口不同，启动前端时可设置 `VITE_API_TARGET`，例如：

```sh
VITE_API_TARGET=http://localhost:8081 npm --prefix web run dev
```

开发模式下打开 `http://localhost:8080` 通常不会提供 React 页面：Vite 在独立开发服务器上提供页面，后端只提供 API。生产 Compose 则由后端同源提供打包后的页面。

### Windows PowerShell 方式

PowerShell 不会自动把 `.env` 文件内容载入当前 shell。安装好数据库并创建用户后，在仓库根目录的 PowerShell 窗口设置环境变量并启动后端：

```powershell
$env:APP_PROFILE = "dev"
$env:DB_VENDOR = "postgres"
$env:DB_HOST = "127.0.0.1"
$env:DB_PORT = "5432"
$env:DB_NAME = "openprotege"
$env:DB_USER = "openprotege"
$env:DB_PASSWORD = "替换为本机开发数据库密码"
$env:ADMIN_BOOTSTRAP_EMAIL = "admin@localhost.test"
$env:ADMIN_BOOTSTRAP_PASSWORD = "本机专用的长密码至少12个字符"
mvn -f server/pom.xml spring-boot:run
```

另开 PowerShell 窗口启动前端：

```powershell
npm ci --prefix web
npm --prefix web run dev
```

PowerShell 窗口关闭后，该窗口内设置的 `$env:` 变量会失效。使用 MySQL 时，将 `DB_VENDOR` 改成 `mysql`，将 `DB_PORT` 改成 `3306`，并把 DB 凭证设置为 MySQL 用户凭证。

## 六、登录并检查功能

### 6.1 确认后端 ready

```sh
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/readiness
```

预期响应包含 `{"status":"UP"}`。此探针表示应用和数据库就绪，不代表所有产品功能均已验收。

如没有 `curl`，可以在浏览器访问该地址，或使用 PowerShell：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health/readiness
```

### 6.2 首次管理员登录

1. 确认后端和 Vite 前端都在运行。
2. 打开 `http://localhost:5173`。
3. 使用 `.env` 中 `ADMIN_BOOTSTRAP_EMAIL` 和 `ADMIN_BOOTSTRAP_PASSWORD` 登录。
4. 首次启动时，应用仅在 `users` 表为空时创建首个平台管理员，并保存密码哈希，不保存明文。

如果数据库已经有用户，修改 `ADMIN_BOOTSTRAP_PASSWORD` 不会重置任何用户密码，也不会创建第二个管理员。不要为了重置账户随意清空数据库；应按账户管理流程处理。

邀请功能不发送邮件。管理员签发的单次邀请 token 必须由运维人员通过安全渠道交给受邀者。

### 6.3 中英文端到端开发场景

在本机开发数据库中，可使用随仓库提供的场景演示邀请/角色、团队项目、本体导入导出、HermiT 分类、临时 SWRL 和不可变版本恢复：

- [中文：社区健康服务本体场景](./examples/business-scenario-zh.md)
- [English: learning services ontology scenario](./examples/business-scenario-en.md)

请先阅读场景中的数据持久化提示：每次运行会新增演示团队、项目、账号和版本，不会自动清理，必须使用开发或一次性验收数据库。自动化脚本调用后端 API，不是浏览器 E2E；场景文档明确列出仍未验收的 UI 和产品能力。

### 6.4 用 curl 检查 CSRF 登录接口（可选）

Web UI 通常会自动完成认证流程。若要手动检查 API，先取得并保存 CSRF cookie：

```sh
curl -i -c /tmp/openprotege-cookies.txt \
  http://127.0.0.1:8080/api/auth/csrf
```

响应 JSON 提供 `token`。将下面 `<csrf-token>` 替换成该值，并用当前终端已设置的 bootstrap 账号测试：

```sh
curl -i -b /tmp/openprotege-cookies.txt -c /tmp/openprotege-cookies.txt \
  -H 'Content-Type: application/json' \
  -H 'X-XSRF-TOKEN: <csrf-token>' \
  --data '{"email":"admin@localhost.test","password":"替换为实际管理员密码"}' \
  http://127.0.0.1:8080/api/auth/login
```

认证状态保存在 cookie 中；不要把 token、cookie 或密码贴到公开渠道。状态变更请求均须符合 API 的 CSRF 要求。

## 七、使用 VS Code 调试

1. 安装 VS Code 的 **Extension Pack for Java**，并确认 VS Code 使用 Java 21。
2. 依照前述步骤创建仓库根目录 `.env` 并启动本机数据库。
3. 在项目根目录打开工作区；首次启动前在终端执行 `npm ci --prefix web`。
4. 打开 Run and Debug，选择 **OpenProtégé Server (dev)**，按 F5。
5. VS Code 会通过 `envFile` 将 `.env` 注入 Java 进程，并通过 pre-launch task 启动 Vite。浏览器访问 `http://localhost:5173`。

如 F5 提示找不到 Java 项目或主类，先用 Maven 命令启动一次以完成项目导入；确认 Java 扩展识别 `server/pom.xml`，然后重新加载 VS Code 窗口。

## 八、运行测试与生产构建

### 8.1 前端检查与构建

```sh
npm ci --prefix web
npm --prefix web run build
```

build 会执行 TypeScript 项目构建和 Vite production build；生成物在 `web/dist/`（该目录不应提交）。

### 8.2 后端测试

```sh
mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test
```

测试包含 Testcontainers 数据库集成测试，**需要 Docker Engine/Compose 可用**；这只影响测试基础设施，不影响前述无容器 `dev` 方式运行应用。测试会启动隔离数据库容器，不应将生产数据库或包含真实数据的容器交给测试。

### 8.3 生产镜像构建

生产镜像通过多阶段构建生成前端静态资源、打包 Java 服务，再组成运行时镜像。生产 Compose 启动命令会自动构建镜像：

```sh
docker compose build server
```

Docker build 需要 Node/Maven registry 和依赖仓库可用。镜像构建通过不是生产运行或业务 E2E 验收的替代品。

## 九、使用 Docker Compose 部署生产模式

### 9.1 安装并检查 Docker

安装 Docker Engine 和 Compose v2，确认以下命令可用：

```sh
docker version
docker compose version
```

Windows/macOS 用户可安装 Docker Desktop（提供 Docker Engine 和 Compose）。不要在没有信任边界的共享机器上使用示例账号或暴露 Docker daemon。

### 9.2 配置生产 `.env`

从仓库根目录执行：

```sh
cp .env.example .env
chmod 600 .env
```

Windows 没有 `chmod` 命令时可略过该命令，并通过文件属性将 `.env` 限制为仅部署账户可读。用密码管理器生成独立的强凭证，然后编辑 `.env`，**不要保留空值或示例密码**：

```dotenv
DB_USER=openprotege
DB_PASSWORD=<数据库专用强密码>
ADMIN_BOOTSTRAP_EMAIL=管理员真实可用的邮箱
ADMIN_BOOTSTRAP_PASSWORD=<独立密码，12至128个字符>
APP_PROFILE=prod
DB_VENDOR=postgres
DB_NAME=openprotege
DB_IMAGE=postgres:17-alpine
SERVER_PORT=8080
DB_PORT=5432
DB_CONTAINER_PORT=5432
DB_VOLUME=postgres_data
DB_DATA_PATH=/var/lib/postgresql/data
```

生产密码应使用不会破坏 dotenv/shell 解析的安全字符集；不要把值写入 Dockerfile、前端环境文件或版本控制。首管理员密码只在空数据库启动时消费；确认管理员可登录后，从部署环境中移除 `ADMIN_BOOTSTRAP_PASSWORD`。

### 9.3 选择 PostgreSQL 或 MySQL

- 默认 PostgreSQL：使用 `.env.example` 对应值。
- 选择 MySQL 8.4：使用单独数据库卷，并把配置改为：

```dotenv
DB_VENDOR=mysql
DB_IMAGE=mysql:8.4
DB_PORT=3306
DB_CONTAINER_PORT=3306
DB_VOLUME=mysql_data
DB_DATA_PATH=/var/lib/mysql
MYSQL_ROOT_PASSWORD=<与DB_PASSWORD不同的独立强root密码>
```

同时保留已设置的 `DB_USER`、`DB_PASSWORD` 和 `DB_NAME`。应用容器通过 Compose 服务名连接数据库；无需在 `.env` 中设置 `DB_HOST`。数据库端口映射到宿主机 loopback 供本机管理工具使用。

切换引擎不会迁移数据。**绝不可把 PostgreSQL 数据卷交给 MySQL，或反过来。**从已有部署更换数据库前，需另外规划、验证并备份数据迁移；本项目没有自动跨引擎迁移工具。

### 9.4 启动并验证

在仓库根目录运行：

```sh
docker compose config --quiet
docker compose up --build --wait
```

第一条命令检查 Compose 配置，第二条构建并启动数据库和 Web 服务，等待健康检查。首次启动会下载基础镜像与构建依赖，可能需要数分钟。

检查健康状态：

```sh
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/readiness
docker compose ps
```

预期 readiness 为 `UP`，`database` 和 `server` 均为 healthy/running。访问 `http://127.0.0.1:8080`，使用 `.env` 中配置的首管理员账号登录。

应用和数据库端口默认只绑定 `127.0.0.1`，适用于本机验收。需要公网访问时，应另行配置并验收 TLS 反向代理、防火墙、域名、访问控制及备份；不要直接把数据库端口暴露到公网。本仓库未附带生产 TLS 反代配置。

### 9.5 MySQL 数据库已有内容时的注意事项

MySQL 官方容器只会在数据目录为空时使用 `MYSQL_DATABASE`、`MYSQL_USER` 和 `MYSQL_PASSWORD` 初始化数据库。更改 `.env` 并不会修改现有 MySQL 卷内的账户或密码。若健康检查失败，先核对现有数据库账户和卷状态，不要用删除卷的方式“修复”连接问题。

### 9.6 可选 AI 与运行限制配置

AI 默认关闭。若部署者已评估向外部模型服务发送用户输入的隐私和数据处理政策，可在服务端 `.env` 中设置：

```dotenv
OPENPROTEGE_AI_ENABLED=true
OPENPROTEGE_AI_PROVIDER=deepseek
OPENPROTEGE_AI_BASE_URL=https://api.deepseek.com/v1
OPENPROTEGE_AI_MODEL=deepseek-chat
OPENPROTEGE_AI_API_KEY=<服务端密钥>
```

密钥只允许放在受保护的服务端运行环境中，不能写入 `web/.env*`、前端代码、镜像或 Git。当前 chat 不读取本体，也不写入项目；启用外部 provider 前仍须自行评估传输内容和服务商条款。

可按部署环境调整以下限制：

| 变量 | 默认值 | 说明 |
|---|---:|---|
| `OPENPROTEGE_ONTOLOGY_MAX_FILE_SIZE` | `500MB` | 上传文件大小限制；默认限制不是最大文件性能或内存安全验收证据。 |
| `OPENPROTEGE_ONTOLOGY_MAX_REQUEST_SIZE` | `501MB` | HTTP multipart 请求上限，应大于单文件限制。 |
| `OPENPROTEGE_REASONING_MAX_AXIOMS` | `100000` | 推理请求的公理数上限；该检查发生在解析后，不是解析期内存限制。 |
| `OPENPROTEGE_REASONING_TIMEOUT` | `60s` | 推理时间限制。超时取消和高负载资源边界仍待进一步验收。 |

修改后重建/重启服务，并查看 readiness 和服务日志确认配置生效；不要只因为设置了更高限制，就认定大文件或推理负载可安全运行。

## 十、运行维护、备份与升级

### 10.1 查看日志、停止与恢复服务

```sh
docker compose logs --tail=200
docker compose logs -f server
docker compose down
docker compose up -d
```

`docker compose down` 会停止并移除容器/网络，命名数据库卷默认保留。**`docker compose down --volumes` 会删除 Compose 管理的持久化数据库卷，可能永久丢失数据。**除非已备份且明确要销毁数据，不要运行该命令。

### 10.2 数据备份（操作示例，不代替恢复演练）

PostgreSQL 容器可将逻辑备份写到宿主机：

```sh
docker compose exec -T database sh -c \
  'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' \
  > "openprotege-$(date +%F).sql"
```

MySQL 容器可使用：

```sh
docker compose exec -T database sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -h 127.0.0.1 -u "$MYSQL_USER" "$MYSQL_DATABASE"' \
  > "openprotege-$(date +%F).sql"
```

确认命令退出码为 0、备份文件非空，并将副本加密存放在数据库主机以外的受控位置。备份可能包含账户邮箱、本体文件和审计数据，应按敏感数据保护。上述命令是逻辑备份示例，不能替代定期的隔离环境恢复演练；当前项目尚未建立正式 RPO/RTO 或生产备份服务。

### 10.3 升级应用

1. 阅读目标版本的 release notes/迁移说明。
2. 先备份数据库，并在非生产环境验证恢复和 Flyway migration。
3. 拉取已审阅的代码版本并构建/启动：

```sh
git pull
docker compose up --build --wait
```

Flyway 会在应用启动时执行所选数据库对应的迁移。不要在应用运行的数据库上手工删除或修改 `flyway_schema_history` 来绕过校验；若 migration validation 失败，应保留日志并按迁移故障处理。

## 十一、常见故障排查

| 现象 | 检查方式与处理 |
|---|---|
| 后端提示无法连接数据库 | 确认数据库服务已启动；检查 `.env` 的 `DB_VENDOR`、`DB_HOST`、`DB_PORT`、库名、用户名和密码；先用 `psql`/`mysql` 以同一 host/port/账户连接。dev 使用宿主机地址；Compose 内部使用服务名 `database`。 |
| Flyway 报认证失败或 migration 错误 | 不同数据库引擎不能共用 schema；核对 vendor/数据库。确认用户具有 schema DDL 权限；查看后端完整启动日志。不要直接清理生产库或 migration history。 |
| 提示空用户库必须提供管理员配置 | 在 `.env` 中设置有效 `ADMIN_BOOTSTRAP_EMAIL` 和 12–128 字符的 `ADMIN_BOOTSTRAP_PASSWORD`，然后重启应用。已有用户数据库不会消费新的 bootstrap 配置，也不会借此重置密码。 |
| 浏览器页面能打开但 API 请求失败 | 确认 Spring 后端已启动并且 readiness 为 `UP`；Vite 默认代理到 8080，后端端口变动时设置 `VITE_API_TARGET` 后重新启动 Vite。浏览器开发地址应是 Vite 地址 `localhost:5173`。 |
| 8080、5173、5432 或 3306 端口已被占用 | 查找并停止占用进程，或更改应用/数据库监听端口。更改数据库端口时同步更新 `DB_PORT`；更改 Vite 目标时设置 `VITE_API_TARGET`。 |
| Maven/Gradle 提示 Java 版本不兼容 | 本项目使用 Maven；检查 `java -version` 和 `mvn -version` 是否都指向 Java 21。更新 `JAVA_HOME`/PATH 后重开终端。 |
| `npm ci` 或 Docker build 下载失败 | 检查 npm registry、Maven Central、项目 Maven 仓库和 Docker registry 的网络访问；不要随意删除 lockfile 或绕过依赖校验。 |
| Compose 提示 `Set DB_USER` / `Set DB_PASSWORD` | `.env` 必须位于 `compose.yaml` 同目录的仓库根目录，且对应变量不可为空。运行 `docker compose config --quiet` 验证配置。 |
| Compose 的 database healthy，但 server 连接超时 | 查看 `docker compose logs --tail=200 database server`，确认两边 vendor、端口、凭证匹配。若在特定受限 Docker 环境中复现 bridge 连接超时，请在支持正常 Compose 网络的 Docker 主机重试；Testcontainers 通过不等于 Compose 网络验收通过。 |
| 修改 `.env` 后数据库仍用旧密码 | 数据库容器只在空数据目录首次初始化时消费初始化账号变量；既有命名卷不会随 `.env` 自动更新。通过数据库自身的账号管理流程改密并同步运行配置，不要删除卷。 |
| 测试停在 Testcontainers 启动或报 Docker API 错误 | 后端集成测试需要 Docker daemon。先确认 `docker info` 正常；本机 dev 启动后端不需要运行这些容器测试。 |

仍无法解决时，请收集脱敏后的操作系统、Java/Maven/Node/Docker 版本、执行命令、退出码和错误日志摘要。提交前删除密码、API key、CSRF token、session cookie、私有本体内容和个人数据。

## 配置速查

| 变量 | 本机开发 | Compose 部署 |
|---|---|---|
| `APP_PROFILE` | `dev`（默认） | `prod`（默认） |
| `DB_VENDOR` | `postgres` 或 `mysql` | `postgres` 或 `mysql` |
| `DB_HOST` | `127.0.0.1` | 服务名 `database`，由 Compose 注入 |
| `DB_PORT` | 本机监听端口：5432 / 3306 | 宿主机映射端口 |
| `DB_CONTAINER_PORT` | 通常无需设置 | 容器内端口：5432 / 3306 |
| `DB_USER` / `DB_PASSWORD` | 本机 DB 用户凭证 | Compose 初始化的应用 DB 用户凭证 |
| `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD` | 仅空用户库首次启动使用 | 仅空用户库首次启动使用 |
| `DB_VOLUME` / `DB_DATA_PATH` | 不使用 | 数据库容器的持久化卷及容器路径 |
| `OPENPROTEGE_AI_*` | 可选；默认关闭 | 可选；密钥仅配置在服务端 |
| `OPENPROTEGE_ONTOLOGY_*` / `OPENPROTEGE_REASONING_*` | 可选限制值 | 可选限制值 |

更多需求与限制见[软件需求规格](../requirements/SRS.md)、[需求追踪矩阵](../requirements/traceability-matrix.csv)、[当前架构](../engineering/01-current-architecture.md)及[工程证据台账](../engineering/evidence-ledger.md)。
