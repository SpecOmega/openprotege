# 审计证据台账

审计日期：2026-10-09
状态词严格使用 `VERIFIED`、`SOURCE-OBSERVED`、`PROPOSED`、`UNVERIFIED`、`BLOCKED`。

| ID | 状态 | 结论 | 证据 / 重现方法 | 限制 |
|---|---|---|---|---|
| E-01 | VERIFIED | 初始审计时分支为 `main`，跟踪 `origin/main`，工作区干净。 | `git status --short --branch` 输出 `## main...origin/main`；`git status --porcelain=v1` 无输出；`git branch --show-current` 输出 `main`；2026-10-09。 | 仅代表初始审计时的本地检出；当前复核见 E-17。 |
| E-02 | VERIFIED | 初始审计时 origin 指向项目指定 GitHub 地址；HEAD 为初始提交。 | `git remote -v`；`git --no-pager log -1 --format='%H%n%cI%n%s'` 输出 SHA `d9caafee6858a958ea7a2944d574407a79f88309`、`2026-10-09T13:04:01+08:00`、`Initial commit`。 | GitHub 页面状态未单独审阅。 |
| E-03 | VERIFIED | 初始审计时 Git 跟踪树只有 README.md；历史仅一个提交。 | 初始 HEAD 上的 `git ls-tree -r --name-only HEAD`、`git ls-files --stage`、`git --no-pager log -8 --oneline --decorate`。 | 仅描述初始根提交；当前跟踪树见 E-17。Git 忽略文件与 `.git` 内部对象不属于跟踪源码；工作目录盘点见 E-04。 |
| E-04 | SOURCE-OBSERVED | 初始审计时工作目录没有应用源码、项目清单、已有文档、测试、CI、部署配置；README 是唯一普通项目文件。 | `ls -la`、`find . -maxdepth 4 -type d -not -path './.git*' -print`、限定文件类型的配置检索、`rg` 占位符检索；2026-10-09 初始审计。 | 后续先新增文档，再新增 Web foundation；E-04 仅为初始审计时点事实，不描述当前工作树。 |
| E-05 | SOURCE-OBSERVED | 初始根提交中的 README 只有项目名和简短产品定位，未记录安装/运行/构建/测试方法。 | 阅读初始 `README.md`；`git show d9caafee6858a958ea7a2944d574407a79f88309:README.md`。 | 当前 README 已更新；产品描述本身不作为功能验证。 |
| E-06 | VERIFIED | 审计环境工具版本为 Git 2.55.0、Java 25.0.4.1、Maven 3.9.16、Node v24.21.0、npm 11.19.0、Python 3.14.2。 | `command -v` 与对应 `--version` 输出。 | 不表示本项目支持这些版本。 |
| E-07 | VERIFIED | Protégé Desktop 的远端 `HEAD`/`master` 解析为 `bf03cccc65ea664e139d6829bba6d209ef58ee55`。 | `git ls-remote https://github.com/protegeproject/protege.git HEAD refs/heads/master refs/heads/main refs/tags/5.6.6`。 | 指针可能后续移动；本文以 SHA 固定。 |
| E-08 | SOURCE-OBSERVED | Desktop 固定提交根目录包含 Maven 多模块、桌面/editor 模块及 `license.txt`；POM 坐标 `edu.stanford.protege:protege-parent:5.6.10-SNAPSHOT`；README 指向 BSD 2-clause。 | 固定 SHA 完整克隆后的根目录、`README.md`、`pom.xml`、`license.txt`。 | 构建测试见 E-18；未启动 GUI、执行本体往返或解析完整依赖许可证。 |
| E-09 | VERIFIED | WebProtégé 远端 `HEAD`/`master` 解析为 `1e84fa02aef68be45f18c08dbeae94bec9b04a41`。 | `git ls-remote https://github.com/protegeproject/webprotege.git HEAD refs/heads/master refs/heads/main`。 | 指针可能后续移动；本文以 SHA 固定。 |
| E-10 | SOURCE-OBSERVED | WebProtégé 固定提交根目录含 Maven、多项 client/server/shared 模块及 Docker/Compose 配置；README 声明仓库正在被更细粒度仓库取代；`license.txt` 含 BSD 2-Clause 风格条款。 | 固定 SHA 完整克隆后的根目录、`README.md`、`pom.xml`、`license.txt`。 | 构建结果见 E-19～E-21；未启动 Web 应用、执行服务端授权安全验证或审计传递依赖许可证。 |
| E-11 | SOURCE-OBSERVED | OpenProtégé 当前检出没有可观察到的上游代码集成证据。 | E-03 的单文件历史与 E-04 的目录盘点；比较 [upstream-audit.md](./upstream-audit.md) 所固定的上游模块。 | 不对仓库外关系作断言，也不声称已审阅全部上游源码历史。 |
| E-12 | BLOCKED | 无法在本仓库执行应用构建、测试、部署或端到端产品验证。 | 检查未发现源码、构建脚本、测试或部署入口。 | 这是缺少目标物造成的阻塞，并非某个构建命令执行失败。 |
| E-13 | UNVERIFIED | 两个上游完整运行、编辑器/应用级 OWL/RDF 工作流、桌面/Web 互操作、身份/授权安全、插件兼容和全依赖许可证兼容性仍未验证；WebProtégé 完整 package 因 E-21 网络阻塞未完成。 | 上游构建测试见 E-18～E-21；单样例 OWLAPI 文件往返见 E-24；其余未执行项见 [upstream-audit.md](./upstream-audit.md)。 | E-24 只验证固定 OWLAPI 版本上的单一文件解析/序列化，不覆盖 Desktop GUI、编辑器、Web 或 OpenProtégé 产品功能。 |
| E-14 | PROPOSED | 完成需求基线未决项、架构/安全决策和上游 PoC，再决定技术复用与代码引入；Apache-2.0 仍待许可证评估。 | 由 E-15、E-18～E-23 及 [SRS](../requirements/SRS.md) 推导。 | 建议，不是已批准的技术实现或最终许可证决定。 |
| E-15 | VERIFIED（负责人决策） | 负责人确认个人与团队用户、OWL 2 核心及 RDF/XML/Turtle 优先验证、桌面/Web 职责、首期文件交换、自托管/公开私有与角色权限、Apache-2.0 评估、AI 建议/只读检索/写入确认边界。 | 用户消息，2026-10-09；需求记录见 [SRS 第 8 节](../requirements/SRS.md#8-需求基线决策与技术决策分层)。 | 仅验证决策已确认，不验证技术实现、可行性、许可证兼容或功能存在。 |
| E-16 | SOURCE-OBSERVED | 需求基线起草时 HEAD 仍为根提交 `d9caafee…`，只跟踪 `README.md`；现有工程审计文档为未跟踪用户工作并已保留。 | `git status --short --branch`、`git rev-parse HEAD`、`git ls-files` 和目录枚举；本轮审计开始时执行。 | 仅描述本地检出与本轮开始时工作区。 |
| E-17 | VERIFIED | 上一轮文档修改前，OpenProtégé `main` HEAD 为 `e595add49b43a83bb68c7c618ee4f0416b2b2e6b`，与 `origin/main` 对齐且工作区干净；已有中英文 README 和已提交审计/需求草案，但仍无应用源码或应用依赖/构建入口。 | `git status --short --branch`、`git rev-parse HEAD`、`git --no-pager log -3 --oneline --decorate`、`git ls-files`；2026-10-09。 | 仅代表上一轮审计文档编辑前的本地状态；随后文档更新已提交为 E-26 的 HEAD。 |
| E-18 | VERIFIED | Protégé Desktop 固定提交 `bf03cccc65ea664e139d6829bba6d209ef58ee55` 在 JDK 21.0.12.1/Maven 3.9.16 下 `mvn -B -ntp -Drelease.signing.disabled=true clean verify` 退出码 0；报告汇总 531 tests、0 failures、0 errors、3 skipped。 | `/tmp/openprotege-stage1-poc.9qWMGG/protege` 固定 SHA checkout；Surefire/Failsafe XML；Maven 输出；统计脚本汇总。 | 不证明 GUI 运行、本体往返保真、插件兼容、Web 互操作或 OpenProtégé 集成。 |
| E-19 | VERIFIED | WebProtégé 固定提交在 JDK 21.0.12.1/Maven 3.9.16 执行首次 `mvn -B -ntp clean package` 退出码 1；`EntityTagsRepositoryImpl_TestCase` 3 个测试因 `localhost:27017` 拒绝连接而超时。 | 固定 SHA checkout 下 Maven 输出；报告显示 469 个文件、3562 tests、0 failures、3 errors，但该失败构建在测试阶段提前终止，计数不完整。 | 失败由本次环境缺少 MongoDB 前置依赖导致；不能据此判定 Web 功能失败。 |
| E-20 | VERIFIED | 启动隔离的 `mongo:4.1-bionic`（容器内 `4.1.13`）后，JDK 25 `mvn -B -ntp clean package` 及 `-Dmaven.compiler.proc=full` 诊断构建仍因源码引用的 `AutoValue_*` 类型未生成而退出码 1。 | `/tmp/openprotege-stage1-poc.9qWMGG/webprotege-clean-package-mongo4.1.log` 与 `webprotege-clean-package-jdk25-procfull.log`；POM 声明 AutoValue 1.7.1。 | 表明 JDK 25/注解处理工具链有兼容风险；需用符合上游支持条件的 JDK 再验证。 |
| E-21 | BLOCKED | WebProtégé JDK 21/Maven 3.9.16 + MongoDB 4.1.13 的 `clean package` 完整结果未取得；5 个已完成模块的 Maven 报告合计 4091 tests、0 failures、0 errors、0 skipped，之后依赖解析被项目 GitHub Maven 仓库 HTTP 504 阻塞，约 19 分钟无进展后停止容器。 | `/tmp/openprotege-stage1-poc.9qWMGG/webprotege-clean-package-jdk21-mongo4.1.log` 中 5 个模块聚合测试行、依赖解析末尾；容器临时 Maven 缓存中的 `.lastUpdated` 记录过 504。 | 管道未启用 `pipefail`，不能将 wrapper 退出码解释为 Maven 结果；不报告全构建成功或失败。可重试时需检查/镜像缺失依赖或改善上游仓库可达性。 |
| E-22 | SOURCE-OBSERVED | Desktop 有 Rio Turtle 格式映射单元测试；Web 有多种下载序列化格式的测试源码及角色/访问管理测试源码。 | 固定 SHA 上游测试文件，包括 `DocumentFormatMapper_TestCase`、`FileDownloadParametersTestCase`、`AccessManagerImpl_IT`、`ProjectAccessManagerImpl_IT`。 | 源码/测试存在不等于完整格式往返、授权安全或集成行为已验证。 |
| E-23 | SOURCE-OBSERVED | 两上游固定提交含 BSD 2-Clause 风格根许可证；WebProtégé README 表示该仓库正被细粒度仓库取代。 | 完整克隆的固定 SHA 下 `license.txt`、README、根 POM；Web README 指明更细粒度仓库。 | 未审计完整传递依赖许可证，也未确认替代仓库的候选版本或 OpenProtégé 采用决策。 |
| E-24 | VERIFIED | Desktop 固定提交测试资源 `pizza.owl`（SHA-256 `a48b154e4a7d6c370b5f9f67c1ef42773877e6742415f697019d8ac4594d60ae`）经 OWLAPI OSGi distribution 4.5.29 执行 RDF/XML 输入→Turtle 保存/重载→RDF/XML 保存/重载；公理数为 930/930/930，本体 ID、公理集合、本体注释及 imports 均保持一致，Java PoC 退出码 0。 | `docs/engineering/poc/OwlFormatRoundTrip.java`；`mvn -B -ntp -f docs/engineering/poc/owlapi-poc-pom.xml dependency:build-classpath -Dmdep.outputFile=/tmp/owlapi-poc-classpath.txt`；`java --class-path "$(cat /tmp/owlapi-poc-classpath.txt)" docs/engineering/poc/OwlFormatRoundTrip.java <pizza.owl> /tmp/pizza-roundtrip.ttl /tmp/pizza-roundtrip.rdf`；宿主 OpenJDK 25.0.4.1/Maven 3.9.16。 | 仅单样例 OWLAPI 文件解析/序列化；不验证 GUI/编辑器操作、Web 上传下载、其他 OWL 2 构造或 OpenProtégé 集成。`photography.owl` 的探索性比较出现 672→707 公理差异，原因未确定，不纳入通过结果。 |
| E-25 | VERIFIED | 2026-10-09 对项目 GitHub Maven releases endpoint 先后观测到 HTTP 504（11.04 秒）、HTTP 200（21.46 秒）；Maven Central 根 endpoint 两次均为 HTTP 200。 | `curl -sS -L --connect-timeout 10 --max-time 25 -o /dev/null -w 'Protege Maven repo HTTP %{http_code}; time %{time_total}s\\n' https://github.com/protegeproject/mvn-repo/raw/master/releases/`；对应 Maven Central URL 为 `https://repo.maven.apache.org/maven2/`。 | endpoint 可达不保证具体 Maven artifact 可获取，也不证明 package 成功；完整构建结果见 E-27。 |
| E-26 | VERIFIED | Web foundation 开始前，`main` HEAD 为 `725e9d21bd36d973211e65e67d248ef4a562f10c`，提交数 4、工作区干净，相对 `origin/main` ahead 1；该快照尚无 OpenProtégé 应用源码或项目构建入口。 | `git status --short --branch`、`git rev-parse HEAD`、`git rev-list --count HEAD`、`git show -s --format='%H%n%cI%n%s' HEAD`、`git ls-files`；2026-10-09。 | 仅描述模块实现前快照；之后的 Web foundation 证据见 E-31～E-35。 |
| E-27 | VERIFIED | WebProtégé 固定提交 `1e84fa02aef68be45f18c08dbeae94bec9b04a41` 在 JDK `21.0.12.1`、Maven `3.9.16`、Docker `29.8.0-1`、临时 MongoDB `4.1-bionic` 容器下，以 Aliyun Central mirror 执行 `clean package`：9/9 reactor 模块成功，Maven `BUILD SUCCESS`、退出码 0、耗时 22:18；7 个模块聚合测试合计 4124 tests、0 failures、0 errors、0 skipped；GWT client 14 个 permutations 编译成功。 | 临时克隆 `/tmp/openprotege-webprotege-aliyun-retry`；完整 Maven 日志 `/tmp/openprotege-webprotege-aliyun-retry/aliyun-clean-package.log`；命令：`docker run --rm --network host -v /tmp/openprotege-webprotege-aliyun-retry:/repo -v "$HOME/.m2:/root/.m2" -w /repo maven:3.9.16-eclipse-temurin-21 mvn -s /repo/maven-settings-aliyun.xml -B -ntp -Drelease.signing.disabled=true clean package`。临时 settings 仅将 `central` 映射到 `https://maven.aliyun.com/repository/central`；镜像 artifact 探测 HTTP 200。 | Maven 容器挂载了已有 `$HOME/.m2` 缓存，且 POM 自定义 Sonatype/Protege 仓库仍启用；未在空缓存环境验证，也未证明 Aliyun 替代 Protege 专用仓库或导致此前 504 的 artifact。日志含旧 JAXB `${tools.jar}` model error 行和 GWT 警告，但最终 reactor 成功。该上游构建不证明 Web 应用启动、授权安全、格式往返、互操作或 OpenProtégé 集成。 |
| E-28 | VERIFIED | 对 Aliyun Central 和清华候选 Maven 镜像作具体 artifact URL 探测：Aliyun `edu/stanford/protege/graphtree/1.1.1/graphtree-1.1.1.pom` HTTP 200；清华 `https://mirrors.tuna.tsinghua.edu.cn/maven/edu/stanford/protege/graphtree/1.1.1/graphtree-1.1.1.pom` HTTP 404。随后 Aliyun Central 的 `javax.annotation-api:1.2` POM 探测亦 HTTP 200。 | `curl -sS -L --connect-timeout 10 --max-time 30 -o /dev/null -w ...` 对相应 Aliyun/Tsinghua artifact URL 执行；实际 WebProtégé Aliyun Central package 见 E-27。 | 只验证两个指定 artifact URL 在探测时的可达性，不代表镜像包含全部依赖；清华该 URL 的 404 不足以断言清华所有 Maven 仓库不可用。 |
| E-29 | VERIFIED | 本轮镜像重试前，OpenProtégé `main` HEAD 为 `5734e976846b5de683caa67c9ae2115d8a5017ae`、与 `origin/main` 对齐且工作区干净。完成审计记录后，仅 8 个工程 Markdown 文件有未提交修改；`git diff --check` 退出码 0；engineering 下 16 个 Markdown 文件的本地链接检查 86 个链接、0 个缺失。 | `git status --short --branch`、`git rev-parse HEAD`、`git diff --check`；本地链接检查 Python 脚本，2026-10-09。 | 本记录描述本轮结束时工作树状态；改动尚未提交或推送。 |
| E-30 | BLOCKED | 使用新建隔离 Maven 缓存 `/tmp/openprotege-webprotege-empty-m2-20261009` 对固定 WebProtégé SHA 执行 Aliyun Central mirror `clean package`；Maven 已开始解析并下载至少 81 个标记为 `aliyun-central` 的构件，随后线程停留在 HTTP 响应头/校验和读取。开发环境重启中断了容器和执行上下文；可用日志仅有 `[INFO] Scanning for projects...`，无最终 Maven 状态、退出码或可关联的具体阻塞 artifact。 | Docker 容器使用 `-Dmaven.repo.local=/maven-cache` 并只挂载新建空目录；Maven thread dump 经容器 `jcmd 1 Thread.print` 检查，栈位于 Aether `BasicRepositoryConnector.fetchChecksum` / Apache HTTP response parser / TLS socket read。 | 不能判断所等请求属于 Aliyun、Protege GitHub 仓库还是其他仓库；不能记为构建成功或失败。需在可持续执行的容器会话中重试并保留日志及仓库 URL。 |
| E-31 | VERIFIED | OpenProtégé Web foundation 的 Testcontainers 集成测试在 Java `21.0.12.1`/Maven `3.9.16` 容器中对 PostgreSQL `17-alpine` 执行 3 tests，0 failures/errors/skips，Maven `BUILD SUCCESS`、退出码 0；覆盖 DB 可用 readiness `UP`/HTTP 200、JDBC 查询、测试专用 Flyway migration，以及停止数据库后的 readiness `DOWN`/HTTP 503。 | 命令：`docker run --rm --network host -v "$PWD:/repo" -v "$HOME/.m2:/root/.m2" -v /var/run/docker.sock:/var/run/docker.sock -w /repo maven:3.9.16-eclipse-temurin-21 mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test`；输出保存在本轮工具临时输出 `/tmp/1791539414422-copilot-tool-output-999-5a52a44b-23cf-4fda-a29d-2b8c4813988c.txt`。 | 仅验证 Testcontainers 集成流程；不验证 Compose bridge 部署、业务 schema、生产备份或账户/项目功能。 |
| E-32 | VERIFIED / BLOCKED | Docker client/server `29.8.0-1` 报告 API `1.56`、daemon 最低 API `1.40`。Testcontainers `1.21.3` 默认尝试 API `1.32` 时退出码 1；显式 `-Dapi.version=1.40` 后 E-31 测试成功。`docker compose config --quiet` 退出码 0。 | `docker version --format 'client={{.Client.Version}} client_api={{.Client.APIVersion}} server={{.Server.Version}} server_api={{.Server.APIVersion}} min_server_api={{.Server.MinAPIVersion}}'`；Maven 完整输出见 E-31 的临时文件。 | API `1.40` 是当前 daemon 支持的测试覆盖设置；其他 daemon 应使用其实际支持版本。仅一个 Compose 配置结果，不代表运行成功。 |
| E-33 | BLOCKED | Compose 镜像 build 成功且 PostgreSQL 17.11 容器 healthy，但应用容器访问 `database:5432` 超时；第一次无 Web healthcheck 的 `docker compose up --build --wait` 退出码 0 但应用后续退出码 1，readiness curl 连接重置（curl 退出码 56）。加入应用 readiness healthcheck 后再次执行相同 `up --build --wait`，命令退出码 1 并报告 `container openprotege-server-1 exited (1)`。 | 命令：`DB_USER=openprotege DB_PASSWORD=<本地临时值> docker compose up --build --wait`；healthcheck 后完整命令输出及实际退出码记录在 `/tmp/openprotege-compose-healthcheck.log`；应用启动失败的容器日志摘要在 `/tmp/openprotege-compose-server.log`。 | 容器内 PostgreSQL 健康不代表 bridge 网络可达；当前环境不允许以 Compose 结果宣称服务可运行。已加入健康检查避免进程“running”被误报为 ready。 |
| E-34 | VERIFIED | 使用同一构建的 Web 镜像通过 Docker host network 连接 Compose PostgreSQL 的映射端口，Flyway 校验 0 个迁移后服务启动；`curl --fail ... /actuator/health/readiness` 返回 `{"status":"UP"}`、HTTP 200。数据库容器内 `pg_isready` 接受连接且 `SELECT 1` 返回 `1`。 | 命令：`docker run --rm --network host -e DB_URL=jdbc:postgresql://127.0.0.1:5432/openprotege -e DB_USER=openprotege -e DB_PASSWORD=<本地临时值> openprotege-server`；请求 `curl --fail --silent --show-error --write-out '\\nHTTP_STATUS=%{http_code}\\n' http://127.0.0.1:8080/actuator/health/readiness`。服务日志显示 Spring Boot 3.5.6、Java 21.0.12.1、PostgreSQL 17.11。 | 仅是 host-network runtime workaround，不替代 Compose bridge 网络部署验收；Flyway 报告无生产迁移，这是预期的空业务 schema。 |
| E-35 | BLOCKED | 在 `openprotege_default` bridge 上运行的临时 `postgres:17-alpine pg_isready -h database -p 5432` 未获得响应；Compose server 日志显示 JDBC connect timeout，而数据库容器本地 `pg_isready`/SQL 查询正常。全程未执行破坏性网络/数据库操作。完成后执行 `docker compose down` 退出码 0，移除本轮容器及网络、保留命名卷。 | `docker network inspect openprotege_default`；`docker run --rm --network openprotege_default postgres:17-alpine pg_isready -h database -p 5432 -U openprotege -d openprotege`；`docker exec openprotege-database-1 pg_isready ...`；`docker exec ... psql ... -c 'SELECT 1'`；清理命令 `DB_USER=... DB_PASSWORD=... docker compose down`。 | 当前 Docker bridge 限制原因无法仅凭本次检查判定为主机 daemon 配置或产品编排缺陷；须在正常 bridge 网络环境重试。数据库卷保留，未 `down --volumes`。 |
| E-36 | VERIFIED | 首版身份/团队/项目 API 的 Java 21 Testcontainers 验证通过：3 个测试类共 5 tests、0 failures/errors/skips，Maven `BUILD SUCCESS`、退出码 0。覆盖 readiness UP/DOWN、Flyway 业务/测试迁移、管理员引导成功/密码散列/缺配置拒绝、邀请成功/过期/重放拒绝/非管理员拒绝/token 摘要存储、正确/错误登录、session/CSRF/logout、当前用户和团队/项目列表、成员列表权限、邀请响应 no-store、公开项目匿名读取、私有项目匿名与团队成员非项目成员拒绝、Member/Viewer 管理拒绝及团队项目显式成员要求。 | 验证代码是基于 HEAD `d25593ba3bfb57dc84573589f83478d167e86f5e` 的当前未提交工作树源码副本，复制到可写隔离目录 `/tmp/openprotege-server-validation-clean`；实际命令：`docker run --rm --network host -v /tmp/openprotege-server-validation-clean:/repo -v /tmp/openprotege-m2:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock -w /repo maven:3.9.16-eclipse-temurin-21 mvn -B -ntp -Dapi.version=1.40 test`；完整输出 `/tmp/openprotege-identity-java21-test.log`；2026-10-09。 | 修改尚未提交；复制的源码快照是该工作树时点。没有验证完整 Owner/Admin/Editor 矩阵、登录限速、多实例 session、浏览器 UI 或 Compose bridge。`server/target` 和默认 Maven 缓存目录对当前用户不可写。 |
| E-37 | VERIFIED | 在扩展后的当前工作树上执行完整服务测试：3 个测试类共 5 tests、0 failures/errors/skips，Maven `BUILD SUCCESS`、退出码 0。身份/项目集成测试新增团队 Admin 成员管理与项目创建、项目 Owner/Admin 管理、Editor/Viewer 元数据只读、Owner 保护，以及团队成员退出后公开项目角色清空、私有团队项目拒绝访问的回归覆盖。 | Java `25.0.4.1`、Maven `3.9.16`；命令：`mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test`；PostgreSQL `17-alpine` Testcontainers；完整输出 `/tmp/1791625543659-copilot-tool-output-2239-4e983b79-4f5b-490c-98ab-209f65c68d86.txt`；2026-10-10。 | 仅验证当前测试覆盖的服务端元数据授权；团队/项目删除转移、并发成员变更、Editor 本体写操作、UI 与 Compose bridge 仍未验证。 |
| E-38 | VERIFIED | 本体文件首个服务端切片通过完整 Maven 测试：5 个测试类共 9 tests、0 failures/errors/skips，Maven `BUILD SUCCESS`、退出码 0。包括 PostgreSQL 身份/项目与本体 API 集成测试，以及 RDF/XML/Turtle 解析、远程 `owl:imports` 不自动加载、超限文件拒绝等单元测试。 | Java `25.0.4.1`、Maven `3.9.16`；命令：`mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test`；PostgreSQL 17 Testcontainers；完整输出 `/tmp/1791626969691-copilot-tool-output-2239-1334bc2f-b9f4-4422-9a41-918cf88e234a.txt`；2026-10-10。工作树 `git diff --check` 通过，需求追踪 CSV 为 44 行、9 列且字段数一致。 | 仅证明当前测试数据和覆盖路径；未验证 Pizza 往返保真、500 MiB 负载/内存性能、解析超时/复杂度、XXE/系统调用安全、版本恢复/删除、Compose bridge 或完整安全验收。 |
| E-39 | VERIFIED | 当前工作树新增 React 19/TypeScript Web workspace、同源 Spring Boot 静态资源打包和默认关闭的 OpenAI-compatible AI chat proxy；前端包含登录/邀请接受、团队/项目、本体版本导入/导出及 AI chat UI。Java Maven 测试 6 个测试类共 12 tests、0 failures/errors/skips；前端严格 TypeScript + Vite production build 通过；Compose 生产镜像含 frontend build stage 且构建成功；`docker compose config --quiet` 与 `git diff --check` 通过。AI 单元测试用 mock OpenAI-compatible endpoint 验证请求体、Bearer header 和响应处理；缺密钥/关闭配置不调用 provider。 | 2026-10-10；后端：`mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test`，日志 `/tmp/1791627940677-copilot-tool-output-2239-414e1396-78d6-407e-949d-5c4015307e27.txt`；前端：`npm ci --prefix web --no-audit --no-fund && npm --prefix web run build`；镜像：`DB_USER=openprotege DB_PASSWORD=local-test docker compose build server`，日志 `/tmp/1791628193188-copilot-tool-output-2239-24e1e356-0812-4055-ae35-e5b1b7f49eb5.txt`；Compose config 与 diff check 均退出码 0。 | 不代表真实 DeepSeek/第三方账号连接、浏览器 E2E/可访问性、AI 外发数据政策审查或生产密钥轮换已验收；不新增 AI 本体上下文、语义检索、变更确认/写回、账号恢复/限速、桌面端。Compose bridge 服务运行仍未验证；最大尺寸和本体完整安全验收仍待做。 |

## 上游 PoC 命令记录（2026-10-09）

上游固定 SHA、克隆路径与日志目录：

```text
Desktop:   /tmp/openprotege-stage1-poc.9qWMGG/protege
WebProtégé:/tmp/openprotege-stage1-poc.9qWMGG/webprotege
```

实际构建命令/结果汇总：

```text
Desktop:
mvn -B -ntp -Drelease.signing.disabled=true clean verify
JDK 21.0.12.1 / Maven 3.9.16；退出码 0；见 E-18。

WebProtégé，首次（未启动 MongoDB）:
mvn -B -ntp clean package
JDK 21.0.12.1 / Maven 3.9.16；退出码 1；见 E-19。

WebProtégé，MongoDB 4.1.13、JDK 25:
mvn -B -ntp clean package
mvn -B -ntp clean package -Dmaven.compiler.proc=full
两次退出码 1，AutoValue_* 生成类型缺失；见 E-20。

WebProtégé，隔离容器 JDK 21.0.12.1、MongoDB 4.1.13:
mvn -B -ntp clean package
5 个模块输出合计 4091 tests、0 failures、0 errors、0 skipped；依赖解析 HTTP 504 后人工停止，未取得 Maven 最终状态；见 E-21。

WebProtégé，固定 SHA `1e84fa02aef68be45f18c08dbeae94bec9b04a41`，隔离容器 JDK 21.0.12.1、MongoDB 4.1、Aliyun Central mirror:
mvn -s /repo/maven-settings-aliyun.xml -B -ntp -Drelease.signing.disabled=true clean package
9/9 模块 `BUILD SUCCESS`，Maven 退出码 0、耗时 22:18；模块测试汇总为 4124 tests、0 failures、0 errors、0 skipped。settings 将 `central` 映射为 `https://maven.aliyun.com/repository/central`；容器同时挂载已有 `$HOME/.m2`，且上游 POM 的自定义仓库仍启用。因此不证明空缓存下仅依靠 Aliyun 可完整解析依赖；完整日志路径和 Docker 命令见 E-27。

OWLAPI 格式往返 PoC，宿主 OpenJDK 25.0.4.1、Maven 3.9.16:
mvn -B -ntp -f docs/engineering/poc/owlapi-poc-pom.xml dependency:build-classpath -Dmdep.outputFile=/tmp/owlapi-poc-classpath.txt
java --class-path "$(cat /tmp/owlapi-poc-classpath.txt)" docs/engineering/poc/OwlFormatRoundTrip.java <固定 Desktop clone>/protege-editor-owl/src/test/resources/ontologies/pizza.owl /tmp/pizza-roundtrip.ttl /tmp/pizza-roundtrip.rdf
两命令退出码均为 0；930 个公理及本体 ID/注释/imports 精确保持；见 E-24。

Web 专用 Maven 仓库和 Maven Central HTTP 可达性探测:
curl -sS -L --connect-timeout 10 --max-time 25 -o /dev/null -w 'Protege Maven repo HTTP %{http_code}; time %{time_total}s\\n' https://github.com/protegeproject/mvn-repo/raw/master/releases/
curl -sS -L --connect-timeout 10 --max-time 25 -o /dev/null -w 'Maven Central HTTP %{http_code}; time %{time_total}s\\n' https://repo.maven.apache.org/maven2/
结果分别为 HTTP 504 (11.04s)、HTTP 200 (0.06s)；见 E-25。
```

上述 Web JDK 21 命令由 Docker `maven:3.9.16-eclipse-temurin-21` 容器执行，输出保存至 `webprotege-clean-package-jdk21-mongo4.1.log`。首次和 JDK 25 运行输出分别为 `webprotege-clean-package-mongo4.1.log` 与 `webprotege-clean-package-jdk25-procfull.log`；均位于同一临时目录。初始审计命令记录如下仍只描述初始仓库基线。

## 本次执行命令记录

```text
git status --short --branch
git branch --show-current
git --no-pager log -1 --format='%H%n%cI%n%s'
git --no-pager log -8 --oneline --decorate
git remote -v
git status --porcelain=v1
git show --no-patch --format=fuller HEAD
git ls-tree -r --name-only HEAD
git ls-files --stage
git count-objects -v
git show HEAD:README.md
ls -la
find . -maxdepth 4 -type d -not -path './.git*' -print
git ls-remote https://github.com/protegeproject/protege.git HEAD refs/heads/master refs/heads/main refs/tags/5.6.6
git ls-remote https://github.com/protegeproject/webprotege.git HEAD refs/heads/master refs/heads/main
```

初始审计中，上游文件通过 GitHub 文件接口按 E-07/E-09 固定 SHA 查询；当时未执行构建/测试。随后 PoC 命令和结果见上节及 E-18～E-23。
| E-40 | VERIFIED | 最终 Web/后端工作树 Maven 全套测试通过：6 个测试类共 13 tests、0 failures/errors/skips，`BUILD SUCCESS`；覆盖 AI mock provider、email 角色分配及 SPA 入口/静态资源路径未被 Spring Security 拒绝。前端 `npm ci` 与严格 TypeScript/Vite production build 通过；最终 Compose 生产镜像执行 React build 和 Maven package 成功；Compose 配置、追踪矩阵 CSV（44 行、9 列）及 `git diff --check` 通过。 | 2026-10-10；`mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test`，输出 `/tmp/1791628669655-copilot-tool-output-2239-9dff9167-a4f5-4e68-90e7-89dde8a98ab0.txt`；`npm ci --prefix web --no-audit --no-fund && npm --prefix web run build`；`DB_USER=openprotege DB_PASSWORD=local-test docker compose build server`；`DB_USER=openprotege DB_PASSWORD=local-test docker compose config --quiet`。 | 没有运行 Compose 服务/E2E；不能据此宣称浏览器流程、真实 AI provider、生产数据隐私、Compose bridge、桌面端、本体编辑/语义检索、版本恢复/删除、500 MiB 资源安全或完整安全验收通过。 |
| E-41 | VERIFIED | HermiT 推理切片实现并完成验证：服务端全套测试 7 个测试类共 16 tests、0 failures/errors/skips；专门覆盖 OWL 2 DL profile/一致 ontology 下不可满足类、推断类层级、未知类和 axiom 上限拒绝。React 严格 TypeScript/Vite production build、生产 Docker 镜像 build、Compose 配置、追踪矩阵 CSV（46 行、9 列）和 `git diff --check` 通过。 | 2026-10-10；`mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test`，输出 `/tmp/1791630133411-copilot-tool-output-2239-b78b7277-67ad-4d89-a630-409c2a22a3e5.txt`；`npm --prefix web run build`；`DB_USER=openprotege DB_PASSWORD=local-test docker compose build server`；`DB_USER=openprotege DB_PASSWORD=local-test docker compose config --quiet`。 | 未执行浏览器 E2E/API 级权限矩阵、超时/中断/并发压力、HermiT 超时后实际资源释放验证、复杂/大型/恶意本体性能与资源安全。Axiom 上限解析后生效，不保护解析阶段内存。 |
| E-42 | VERIFIED | 推理与规则切片扩展并验证：Maven 全套 7 个测试类共 19 tests、0 failures/errors/skips；解释测试断言冲突集确实包含两条类型断言和互斥公理，测试自动分类、临时 SWRL 推理及不可用引擎。React 严格 TypeScript/Vite production build、最终生产 Docker 镜像 build、Compose 配置、追踪矩阵 CSV（47 行、9 列）及 `git diff --check` 通过。 | 2026-10-10；`mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test`，输出 `/tmp/1791631497969-copilot-tool-output-2239-78467880-649e-4815-b3e1-6f855ce30fc7.txt`；`npm --prefix web run build`；`DB_USER=openprotege DB_PASSWORD=local-test docker compose build server`；`DB_USER=openprotege DB_PASSWORD=local-test docker compose config --quiet`；追踪矩阵 CSV 结构检查及 `git diff --check`。 | 未执行浏览器 E2E/API 权限矩阵、解释超限路径、超时取消/中断/并发压力、大型/恶意本体性能和资源安全；Docker build 跳过 Maven 测试，测试证据来自独立 Maven 命令。 |
| E-43 | VERIFIED | 本体历史版本恢复首个切片完成验证：Maven 全套 7 个测试类共 19 tests、0 failures/errors/skips；PostgreSQL HTTP 集成验证 Owner 恢复生成新版本、版本元数据与文件内容保持一致、审计记录来源版本及 Viewer 拒绝。前端严格 TypeScript/Vite build、生产 Docker 镜像 build、Compose 配置、追踪矩阵 CSV（49 行、9 列）及 `git diff --check` 通过。 | 2026-10-10；`mvn -B -ntp -Dapi.version=1.40 -f server/pom.xml test`，输出 `/tmp/1791632054349-copilot-tool-output-2239-11c96fc8-c311-4e2c-ad80-c1fd50e8b684.txt`；`npm --prefix web run build`；`DB_USER=openprotege DB_PASSWORD=local-test docker compose build server`；`DB_USER=openprotege DB_PASSWORD=local-test docker compose config --quiet`；CSV 结构检查及 `git diff --check`。 | 未验证 Editor restore 的独立 HTTP 路径、跨项目/未知版本拒绝、恢复文件大小边界、恢复并发、浏览器 E2E、审计日志保留/查询界面；Docker build 跳过 Maven 测试，测试证据来自独立 Maven 命令。 |
