# 03 — 构建与测试基线

审计日期：2026-10-09

本文件保留 Stage 0 时无应用代码的历史检查结果；当前首个应用模块的增量检查见下节。Stage 0 快照 HEAD：`725e9d21bd36d973211e65e67d248ef4a562f10c`。

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## Stage 0 仓库检查（历史基线）

Stage 0 快照的仓库没有应用源码、依赖清单、构建/测试脚本或 CI 配置。下表只记录该历史快照；它不是当前工作树结论。（VERIFIED：固定快照的 Git 跟踪树与工作区盘点）

| 检查 | 是否执行 | 结果 | 状态 |
|---|---:|---|---|
| 依赖完整性 | 否 | 无依赖清单可选用包管理器 | BLOCKED |
| 编译/应用构建 | 否 | 无构建入口 | BLOCKED |
| 单元、集成、端到端测试 | 否 | 无应用测试源码或测试配置 | BLOCKED |
| 类型、格式、静态分析 | 否 | 无应用源码或工具配置 | BLOCKED |
| 安全依赖扫描 | 否 | 无依赖清单 | BLOCKED |
| 部署、冒烟、备份恢复 | 否 | 无产品或部署配置 | BLOCKED |

## Web foundation 模块（2026-10-09 增量验证）

当前工作树新增 `server/` Maven/Spring Boot 模块与 `compose.yaml`，因此上面的“无应用入口”只描述 Stage 0 快照，不描述当前工作树。

| 检查 | 命令 | 结果 |
|---|---|---|
| Docker/Compose 配置 | `DB_USER=openprotege DB_PASSWORD='[redacted local test value]' docker compose config --quiet` | 退出码 0。 |
| Testcontainers/PostgreSQL 集成测试 | 在 `maven:3.9.16-eclipse-temurin-21` 容器执行 `mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test` | 退出码 0；3 tests、0 failures、0 errors、0 skipped。覆盖 PostgreSQL 17、readiness 上/下状态、JDBC 查询与测试专用 Flyway migration。 |
| Docker API 默认协商 | 同上测试但不传 `-Dapi.version=1.40` | 退出码 1；Testcontainers 1.21.3 使用 Docker API 1.32，daemon 最低 API 为 1.40。该失败不是应用测试断言失败。 |
| Compose 镜像构建与启动（未加 Web healthcheck 的首次执行） | `DB_USER=openprotege DB_PASSWORD='[redacted local test value]' docker compose up --build --wait` | 命令退出码 0；镜像构建成功且 PostgreSQL 17.11 healthy。但应用进程随后因 JDBC 连接超时退出码 1。首次 Compose 未设置应用 healthcheck，所以其 `--wait` 只确认容器运行，不能作为成功证据。之后已增加 HTTP readiness healthcheck。 |
| Compose 镜像构建与启动（启用 Web healthcheck 后复测） | `DB_USER=openprotege DB_PASSWORD='[redacted local test value]' docker compose up --build --wait` | Docker 镜像构建成功、数据库 healthy；`up --wait` 退出码 1：`container openprotege-server-1 exited (1)`。Web 日志显示 PostgreSQL JDBC connection attempt timed out。该执行环境中即使独立同 bridge 探测容器也无法连接 `database:5432`；Compose bridge 网络限制阻断部署级验收。 |
| Compose readiness HTTP 请求 | `curl --fail --silent --show-error --write-out '\nHTTP_STATUS=%{http_code}\n' http://127.0.0.1:8080/actuator/health/readiness` | 退出码 56；connection reset。原因与 Web 容器启动时 DB 连接超时一致；未声称该次 Compose HTTP readiness 通过。 |
| Standalone 容器运行/探针 | `docker run --rm --network host -e DB_URL=jdbc:postgresql://127.0.0.1:5432/openprotege -e DB_USER=openprotege -e DB_PASSWORD=<本地临时值> openprotege-server`；随后 curl readiness | 应用连接 Compose PostgreSQL 17.11，Flyway 报告 0 个业务迁移并成功启动；readiness 返回 `{"status":"UP"}`、HTTP 200。该验证绕过了 Compose bridge，不替代 Compose 联网验收。 |
| 数据库本地状态 | 容器内 `pg_isready` 和 `psql -c 'SELECT 1'` | PostgreSQL 容器内部接受连接、查询返回 1；单独的同 bridge 探测容器无法连接服务名 `database:5432`，构成当前环境容器网络限制的证据。 |

## 身份/团队/项目模块（2026-10-09 增量验证）

| 检查 | 命令/执行方式 | 结果 |
|---|---|---|
| Java 21 PostgreSQL 集成测试 | 将 `server/pom.xml` 与 `server/src` 复制到可写 `/tmp/openprotege-server-validation-clean`；执行 `docker run --rm --network host -v /tmp/openprotege-server-validation-clean:/repo -v /tmp/openprotege-m2:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock -w /repo maven:3.9.16-eclipse-temurin-21 mvn -B -ntp -Dapi.version=1.40 test` | 退出码 0；3 test classes，5 tests，0 failures/errors/skips。包括 foundation 3 tests、身份/项目 API 1 个端到端集成测试和 bootstrap 缺配置单元测试。 |
| 身份/授权 API 测试范围 | 上述 `IdentityProjectApiIntegrationTest` 与 `AdministratorBootstrapTest` | invitation acceptance/expired/replay/非管理员拒绝、token digest、bootstrap password hash/缺配置拒绝、login success/failure、session/logout、CSRF、匿名 public read/private deny、team member but not project member deny、Member/Viewer management deny。完整角色组合仍未覆盖。 |
| 当前工作区 Maven 直接执行 | `mvn -B -ntp -f server/pom.xml -Dapi.version=1.40 test` | 未能执行成功：默认 Maven cache path AccessDenied；使用可写 Maven cache 后，现有 `server/target/classes/application.yml` 替换失败（Operation not permitted）。不是测试断言失败；改在隔离副本验证。 |

容器运行时 Java `21.0.12.1`、Maven `3.9.16`、Testcontainers `1.21.3`，需显式 `-Dapi.version=1.40`。完整测试输出 `/tmp/openprotege-identity-java21-test.log`，证据见 [evidence-ledger.md](./evidence-ledger.md) E-36。该结果不验证 Compose bridge、Web UI、完整角色矩阵、登录限速或多副本运行。

## 本体文件 API 首个切片（2026-10-10）

| 检查 | 命令/执行方式 | 结果 |
|---|---|---|
| 完整服务测试 | `mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test` | 退出码 0，Maven `BUILD SUCCESS`；5 个测试类共 9 tests，0 failures、0 errors、0 skipped。包括服务 readiness/迁移、身份与项目 API、本体 API 集成、管理员引导、解析器和大小限制单元测试。 |
| 本体 API 覆盖范围 | `IdentityProjectApiIntegrationTest`、`OntologyParserTest`、`OntologyServiceTest` | 测试覆盖 RDF/XML/Turtle 导入及转换导出、版本查询、导入/导出审计、Viewer 导出与导入拒绝、远程 imports 不自动加载、超配置大小拒绝。 |
| 工作树与追踪表检查 | `git diff --check`；Python `csv` 标准库检查 `docs/requirements/traceability-matrix.csv` | 空白检查通过；追踪表 44 行、9 列，所有记录列数一致。 |

完整测试输出与限制见 [evidence-ledger.md](./evidence-ledger.md) E-38。小型测试样例通过不代表 Pizza 往返保真、500 MiB 负载性能、解析超时/复杂度、XXE/系统调用安全、版本恢复/删除或 Compose bridge 已验收。

## 固定上游 PoC 执行环境

以下仅是上游项目的独立 PoC，不是 OpenProtégé 的构建或功能验证：

| 环境项 | 实际值 |
|---|---|
| OS | Docker Desktop Linux 容器；Docker server `29.8.0-1`（宿主探测） |
| Maven | `3.9.16` |
| Java | Eclipse Temurin OpenJDK `21.0.12.1`（Docker image `maven:3.9.16-eclipse-temurin-21`，digest `sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320`） |
| WebProtégé 测试数据库 | 官方 `mongo:4.1-bionic`，容器内版本 `4.1.13`；仅临时容器、无挂载数据卷、端口绑定 `127.0.0.1:27017` |
| Desktop 固定提交 | `bf03cccc65ea664e139d6829bba6d209ef58ee55` |
| WebProtégé 固定提交 | `1e84fa02aef68be45f18c08dbeae94bec9b04a41` |

临时克隆位于 `/tmp/openprotege-stage1-poc.9qWMGG/`，未加入 OpenProtégé 仓库。完整命令/退出结果以本节和 [上游审计](./upstream-audit.md) 记录为准。

本轮另使用宿主 OpenJDK `25.0.4.1`（Microsoft）和 Maven `3.9.16` 执行独立 OWLAPI PoC；该环境不同于 Desktop 的 JDK 21 构建环境。

## Protégé Desktop

| 检查 | 命令 | 结果 |
|---|---|---|
| 构建与测试 | `mvn -B -ntp -Drelease.signing.disabled=true clean verify` | 退出码 0，Maven 输出 `BUILD SUCCESS`；76 个 Surefire/Failsafe XML 报告，合计 531 tests、0 failures、0 errors、3 skipped。 |

环境：OpenJDK `21.0.12.1`、Maven `3.9.16`。日志输出于构建会话，报告位于临时克隆各模块 `target/{surefire,failsafe}-reports/`。该结果只证明固定提交在该环境下的 Maven 验证成功；不证明安装后桌面运行、完整编辑工作流、Web 互操作、插件兼容或 OpenProtégé 集成。

## OWL 2 文件格式往返 PoC（限于 OWLAPI）

在固定 Desktop 提交 `bf03cccc65ea664e139d6829bba6d209ef58ee55` 的测试资源 `protege-editor-owl/src/test/resources/ontologies/pizza.owl`（SHA-256 `a48b154e4a7d6c370b5f9f67c1ef42773877e6742415f697019d8ac4594d60ae`）上，使用 POM 声明的 OWLAPI OSGi distribution `4.5.29` 执行独立序列化测试。使用 [PoC POM](./poc/owlapi-poc-pom.xml) 解析依赖，并由 [OwlFormatRoundTrip.java](./poc/OwlFormatRoundTrip.java) 严格比较本体 ID、公理集合、本体注释和 imports。

实际命令（从 OpenProtégé 仓库根目录执行；`/tmp/protege-poc` 是固定 SHA 的独立 clone）：

```text
git clone --no-checkout https://github.com/protegeproject/protege.git /tmp/protege-poc
git -C /tmp/protege-poc checkout --detach bf03cccc65ea664e139d6829bba6d209ef58ee55
mvn -B -ntp -f docs/engineering/poc/owlapi-poc-pom.xml dependency:build-classpath -Dmdep.outputFile=/tmp/owlapi-poc-classpath.txt
java --class-path "$(cat /tmp/owlapi-poc-classpath.txt)" docs/engineering/poc/OwlFormatRoundTrip.java /tmp/protege-poc/protege-editor-owl/src/test/resources/ontologies/pizza.owl /tmp/pizza-roundtrip.ttl /tmp/pizza-roundtrip.rdf
```

实际复跑使用既有固定 clone `/tmp/openprotege-stage1-poc.9qWMGG/protege`；Maven classpath 解析退出码 0，仓库内 Java PoC 退出码 0。输入被识别为 RDF/XML；输入、Turtle 再加载、再次 RDF/XML 再加载的公理数分别为 930/930/930；本体 ID、公理集合、本体注释和 imports 精确相等。输出文件大小分别为 Turtle 107572 bytes、RDF/XML 190380 bytes。运行有 SLF4J 无 provider 提示和 Caffeine 使用 `sun.misc.Unsafe` 的 JDK 弃用警告；未影响本次 PoC。（VERIFIED；证据见 E-24）

边界：这是 OWLAPI 对单一上游测试样例的文件解析/序列化 PoC，不运行 Desktop GUI，不执行编辑器修改，不测 WebProtégé 上传/下载或 OpenProtégé 集成，也不覆盖其他 OWL 2 Profile/构造、大型文件、恶意输入或所有 RDF/XML/Turtle 文档。另一个上游 `photography.owl` 探索性比较曾观察到 672 个输入公理与 707 个 Turtle 重载公理不一致；具体差异原因未归因，故该样例不计入通过结果。

## WebProtégé

执行了多次 `clean package` 检查，必须区分工具链、数据库和依赖仓库条件：

| 检查 | 命令/条件 | 结果 |
|---|---|---|
| 初次构建 | JDK `21.0.12.1`、Maven `3.9.16`；`mvn -B -ntp clean package`，未启动 MongoDB | 退出码 1，`BUILD FAILURE`；服务器 `EntityTagsRepositoryImpl_TestCase` 中 3 个测试因连接 `localhost:27017` 失败，抛出 Mongo timeout/connection refused。共生成 469 份报告、3562 tests、0 failures、3 errors；该数是失败构建截至中断模块的部分执行量，不代表完整测试总量。 |
| 提供上游 Compose 指定数据库后的检查 | JDK `25.0.4.1`、Maven `3.9.16`；MongoDB `4.1.13` 已运行；`mvn -B -ntp clean package` | 退出码 1，`BUILD FAILURE`；`webprotege-shared` 编译未生成源码所引用的 `AutoValue_*` 类型。 |
| 注解处理诊断 | 相同 JDK/Mongo；`mvn -B -ntp clean package -Dmaven.compiler.proc=full` | 退出码 1；同类 `AutoValue_*` 缺失仍在，不能据此断定该版本的 JDK/注解处理兼容问题已解决。 |
| JDK 21 初次重试 | 容器 `maven:3.9.16-eclipse-temurin-21`、MongoDB `4.1.13`；`mvn -B -ntp clean package` | 未得到完整 Maven 退出码，也没有 `BUILD SUCCESS`/`BUILD FAILURE` 汇总。5 个已完成模块报告 4091 tests、0 failures、0 errors、0 skipped；随后 Maven 依赖解析停在项目 GitHub Maven 仓库。约 19 分钟无测试进展后停止隔离容器。完整 package 此次执行未完成，不能把部分测试计作全套通过。 |
| JDK 21 endpoint 恢复后重试 | 容器 `maven:3.9.16-eclipse-temurin-21`、MongoDB `4.1.13`；`mvn -B -ntp clean package` | endpoint 恢复后启动；日志和容器随开发容器重启而丢失，未取得最终退出码。不能据此报告成功或失败。 |
| JDK 21 Aliyun Central 镜像重试 | 容器 `maven:3.9.16-eclipse-temurin-21`、MongoDB `4.1.13`；`mvn -s /repo/maven-settings-aliyun.xml -B -ntp -Drelease.signing.disabled=true clean package` | 完整 9 模块 reactor `BUILD SUCCESS`，Maven 退出码 0，用时 22:18；7 个模块测试汇总合计 4124 tests、0 failures、0 errors、0 skipped。Web client 的 GWT 编译完成 14 个 permutations。日志 `/tmp/openprotege-webprotege-aliyun-retry/aliyun-clean-package.log`。 |
| Aliyun + 空 Maven 缓存重试 | 固定 SHA；JDK `21.0.12.1`、Maven `3.9.16`、MongoDB `4.1.13`；独立空目录 `/tmp/openprotege-webprotege-empty-m2-20261009` 作为本地仓库；Central 映射 Aliyun | Maven 开始项目扫描并向远端发起请求；线程栈显示在读取 HTTP 响应头/校验 artifact checksum 时等待。开发环境会话中断时命令、容器和日志均未保留最终错误或退出状态；没有可靠证据标识当时的具体 artifact。结果为 BLOCKED / 无最终状态，不得视为构建失败或通过。 |

JDK 25 编译错误说明环境/编译处理不兼容的可能性，不足以单独判定上游源码缺陷。JDK 21 初次重试日志位于 `/tmp/openprotege-stage1-poc.9qWMGG/webprotege-clean-package-jdk21-mongo4.1.log`；当时的 shell 用 Docker 管道保存输出且未启用 pipefail，故 wrapper 的退出码不能代表 Maven 退出码。探测命令为 `curl -sS -L --connect-timeout 10 --max-time 25 -o /dev/null -w 'Protege Maven repo HTTP %{http_code}; time %{time_total}s\\n' https://github.com/protegeproject/mvn-repo/raw/master/releases/`；端点先后返回 HTTP 504（11.04 秒）和 HTTP 200（21.46 秒），Maven Central 根地址两次均返回 HTTP 200。（VERIFIED；E-25）

Aliyun 重试使用一次性 Maven settings，将 `central` 映射到 `https://maven.aliyun.com/repository/central`；WebProtégé POM 中的 Sonatype snapshots 和 Protege GitHub Maven 仓库仍保留。构建容器挂载了宿主 `$HOME/.m2` 缓存，因此本次成功**不证明**空缓存下所有依赖均可由 Aliyun 获取，也不证明 Aliyun 替代了 Protege 专用仓库或排除了该仓库的 HTTP 504。构建日志还包含 JAXB 旧 POM 的 `${tools.jar}` `systemPath` 模型错误行及 GWT 安全模板警告，但 reactor 最终成功；不得将这些日志行隐去，也不应误记为 Maven 构建失败。复现命令和环境记录见 E-27。即使构建成功，也不等同于部署、格式往返、真实协作或权限隔离测试。

空缓存重试使用单独创建的 `/tmp/openprotege-webprotege-empty-m2-20261009`，命令额外传入 `-Dmaven.repo.local=/maven-cache` 并只挂载该空缓存。容器运行期间观察到 81 个 Aliyun-central 标记的构件已下载，但 Maven 随后停留在 HTTP 响应/校验和获取；开发环境重启导致执行上下文与隔离 Mongo 容器终止，日志只保留 `[INFO] Scanning for projects...`，未有完整 Maven 输出或退出码。由于未能关联挂起请求到具体坐标，不推断是 Aliyun、Protege 专用仓库或网络中的哪一方导致阻塞，也不虚构失败 artifact。

本次临时 settings 内容：

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
  <mirrors>
    <mirror>
      <id>aliyun-central</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/central</url>
    </mirror>
  </mirrors>
</settings>
```

实际 Maven 命令在固定 SHA 的临时克隆目录中执行：

```text
docker run --rm --network host \
  -v /tmp/openprotege-webprotege-aliyun-retry:/repo \
  -v "$HOME/.m2:/root/.m2" \
  -w /repo maven:3.9.16-eclipse-temurin-21 \
  mvn -s /repo/maven-settings-aliyun.xml -B -ntp \
  -Drelease.signing.disabled=true clean package
```

## 边界与可复现性

- 上游源码均为 detached checkout 的固定 SHA；本轮没有修改其 tracked 源码。
- MongoDB 测试依赖来自上游 Compose 声明的 `mongo:4.1-bionic`；容器使用临时文件系统，没有挂载宿主项目数据。
- 未运行 Web 服务或 Desktop GUI，未执行编辑器级往返、跨客户端交换、权限攻击、安全扫描或全传递依赖许可证检查；上述 OWLAPI 单样例 PoC 不覆盖这些流程。
- 本表中所有“未执行”与“执行失败”均分开记录。任何上游成功结果都不能计作 OpenProtégé 当前功能。
