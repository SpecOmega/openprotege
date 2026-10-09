# 审计证据台账

审计日期：2026-10-09
状态词严格使用 `VERIFIED`、`SOURCE-OBSERVED`、`PROPOSED`、`UNVERIFIED`、`BLOCKED`。

| ID | 状态 | 结论 | 证据 / 重现方法 | 限制 |
|---|---|---|---|---|
| E-01 | VERIFIED | 初始审计时分支为 `main`，跟踪 `origin/main`，工作区干净。 | `git status --short --branch` 输出 `## main...origin/main`；`git status --porcelain=v1` 无输出；`git branch --show-current` 输出 `main`；2026-10-09。 | 仅代表初始审计时的本地检出；当前复核见 E-17。 |
| E-02 | VERIFIED | 初始审计时 origin 指向项目指定 GitHub 地址；HEAD 为初始提交。 | `git remote -v`；`git --no-pager log -1 --format='%H%n%cI%n%s'` 输出 SHA `d9caafee6858a958ea7a2944d574407a79f88309`、`2026-10-09T13:04:01+08:00`、`Initial commit`。 | GitHub 页面状态未单独审阅。 |
| E-03 | VERIFIED | 初始审计时 Git 跟踪树只有 README.md；历史仅一个提交。 | 初始 HEAD 上的 `git ls-tree -r --name-only HEAD`、`git ls-files --stage`、`git --no-pager log -8 --oneline --decorate`。 | 仅描述初始根提交；当前跟踪树见 E-17。Git 忽略文件与 `.git` 内部对象不属于跟踪源码；工作目录盘点见 E-04。 |
| E-04 | SOURCE-OBSERVED | 初始审计时工作目录没有应用源码、项目清单、已有文档、测试、CI、部署配置；README 是唯一普通项目文件。 | `ls -la`、`find . -maxdepth 4 -type d -not -path './.git*' -print`、限定文件类型的配置检索、`rg` 占位符检索；2026-10-09 初始审计。 | 后续已新增并提交文档；应用源码/构建入口当前仍未发现（E-17）。空缺仅对该检出成立，不证明其他分支或远端历史。 |
| E-05 | SOURCE-OBSERVED | 初始根提交中的 README 只有项目名和简短产品定位，未记录安装/运行/构建/测试方法。 | 阅读初始 `README.md`；`git show d9caafee6858a958ea7a2944d574407a79f88309:README.md`。 | 当前 README 已更新；产品描述本身不作为功能验证。 |
| E-06 | VERIFIED | 审计环境工具版本为 Git 2.55.0、Java 25.0.4.1、Maven 3.9.16、Node v24.21.0、npm 11.19.0、Python 3.14.2。 | `command -v` 与对应 `--version` 输出。 | 不表示本项目支持这些版本。 |
| E-07 | VERIFIED | Protégé Desktop 的远端 `HEAD`/`master` 解析为 `bf03cccc65ea664e139d6829bba6d209ef58ee55`。 | `git ls-remote https://github.com/protegeproject/protege.git HEAD refs/heads/master refs/heads/main refs/tags/5.6.6`。 | 指针可能后续移动；本文以 SHA 固定。 |
| E-08 | SOURCE-OBSERVED | Desktop 固定提交根目录包含 Maven 多模块、桌面/editor 模块及 `license.txt`；POM 坐标 `edu.stanford.protege:protege-parent:5.6.10-SNAPSHOT`；README 指向 BSD 2-clause。 | 固定 SHA 完整克隆后的根目录、`README.md`、`pom.xml`、`license.txt`。 | 构建测试见 E-18；未启动 GUI、执行本体往返或解析完整依赖许可证。 |
| E-09 | VERIFIED | WebProtégé 远端 `HEAD`/`master` 解析为 `1e84fa02aef68be45f18c08dbeae94bec9b04a41`。 | `git ls-remote https://github.com/protegeproject/webprotege.git HEAD refs/heads/master refs/heads/main`。 | 指针可能后续移动；本文以 SHA 固定。 |
| E-10 | SOURCE-OBSERVED | WebProtégé 固定提交根目录含 Maven、多项 client/server/shared 模块及 Docker/Compose 配置；README 声明仓库正在被更细粒度仓库取代；`license.txt` 含 BSD 2-Clause 风格条款。 | 固定 SHA 完整克隆后的根目录、`README.md`、`pom.xml`、`license.txt`。 | 构建结果见 E-19～E-21；未启动 Web 应用、执行服务端授权安全验证或审计传递依赖许可证。 |
| E-11 | SOURCE-OBSERVED | OpenProtégé 当前检出没有可观察到的上游代码集成证据。 | E-03 的单文件历史与 E-04 的目录盘点；比较 [upstream-audit.md](./upstream-audit.md) 所固定的上游模块。 | 不对仓库外关系作断言，也不声称已审阅全部上游源码历史。 |
| E-12 | BLOCKED | 无法在本仓库执行应用构建、测试、部署或端到端产品验证。 | 检查未发现源码、构建脚本、测试或部署入口。 | 这是缺少目标物造成的阻塞，并非某个构建命令执行失败。 |
| E-13 | UNVERIFIED | 两个上游的完整运行、OWL/RDF 往返保真、桌面/Web 互操作、身份/授权安全、插件兼容和全依赖许可证兼容性仍未验证；WebProtégé 完整 package 因 E-21 网络阻塞未完成。 | 上游构建测试见 E-18～E-21；未执行项见 [upstream-audit.md](./upstream-audit.md)。 | Desktop 构建通过和 Web 的部分模块测试不覆盖上述未执行项；后续 PoC 需独立记录。 |
| E-14 | PROPOSED | 完成需求基线未决项、架构/安全决策和上游 PoC，再决定技术复用与代码引入；Apache-2.0 仍待许可证评估。 | 由 E-15、E-18～E-23 及 [SRS](../requirements/SRS.md) 推导。 | 建议，不是已批准的技术实现或最终许可证决定。 |
| E-15 | VERIFIED（负责人决策） | 负责人确认个人与团队用户、OWL 2 核心及 RDF/XML/Turtle 优先验证、桌面/Web 职责、首期文件交换、自托管/公开私有与角色权限、Apache-2.0 评估、AI 建议/只读检索/写入确认边界。 | 用户消息，2026-10-09；需求记录见 [SRS 第 8 节](../requirements/SRS.md#8-需求基线决策与技术决策分层)。 | 仅验证决策已确认，不验证技术实现、可行性、许可证兼容或功能存在。 |
| E-16 | SOURCE-OBSERVED | 需求基线起草时 HEAD 仍为根提交 `d9caafee…`，只跟踪 `README.md`；现有工程审计文档为未跟踪用户工作并已保留。 | `git status --short --branch`、`git rev-parse HEAD`、`git ls-files` 和目录枚举；本轮审计开始时执行。 | 仅描述本地检出与本轮开始时工作区。 |
| E-17 | VERIFIED | 本轮文档修改前，OpenProtégé `main` HEAD 为 `e595add49b43a83bb68c7c618ee4f0416b2b2e6b`，与 `origin/main` 对齐且工作区干净；已有中英文 README 和已提交审计/需求草案，但仍无应用源码或应用依赖/构建入口。 | `git status --short --branch`、`git rev-parse HEAD`、`git --no-pager log -3 --oneline --decorate`、`git ls-files`；2026-10-09。 | 仅代表本轮审计文档编辑前的本地状态；本轮更新尚未提交。 |
| E-18 | VERIFIED | Protégé Desktop 固定提交 `bf03cccc65ea664e139d6829bba6d209ef58ee55` 在 JDK 21.0.12.1/Maven 3.9.16 下 `mvn -B -ntp -Drelease.signing.disabled=true clean verify` 退出码 0；报告汇总 531 tests、0 failures、0 errors、3 skipped。 | `/tmp/openprotege-stage1-poc.9qWMGG/protege` 固定 SHA checkout；Surefire/Failsafe XML；Maven 输出；统计脚本汇总。 | 不证明 GUI 运行、本体往返保真、插件兼容、Web 互操作或 OpenProtégé 集成。 |
| E-19 | VERIFIED | WebProtégé 固定提交在 JDK 21.0.12.1/Maven 3.9.16 执行首次 `mvn -B -ntp clean package` 退出码 1；`EntityTagsRepositoryImpl_TestCase` 3 个测试因 `localhost:27017` 拒绝连接而超时。 | 固定 SHA checkout 下 Maven 输出；报告显示 469 个文件、3562 tests、0 failures、3 errors，但该失败构建在测试阶段提前终止，计数不完整。 | 失败由本次环境缺少 MongoDB 前置依赖导致；不能据此判定 Web 功能失败。 |
| E-20 | VERIFIED | 启动隔离的 `mongo:4.1-bionic`（容器内 `4.1.13`）后，JDK 25 `mvn -B -ntp clean package` 及 `-Dmaven.compiler.proc=full` 诊断构建仍因源码引用的 `AutoValue_*` 类型未生成而退出码 1。 | `/tmp/openprotege-stage1-poc.9qWMGG/webprotege-clean-package-mongo4.1.log` 与 `webprotege-clean-package-jdk25-procfull.log`；POM 声明 AutoValue 1.7.1。 | 表明 JDK 25/注解处理工具链有兼容风险；需用符合上游支持条件的 JDK 再验证。 |
| E-21 | BLOCKED | WebProtégé JDK 21/Maven 3.9.16 + MongoDB 4.1.13 的 `clean package` 完整结果未取得；5 个已完成模块的 Maven 报告合计 4091 tests、0 failures、0 errors、0 skipped，之后依赖解析被项目 GitHub Maven 仓库 HTTP 504 阻塞，约 19 分钟无进展后停止容器。 | `/tmp/openprotege-stage1-poc.9qWMGG/webprotege-clean-package-jdk21-mongo4.1.log` 中 5 个模块聚合测试行、依赖解析末尾；容器临时 Maven 缓存中的 `.lastUpdated` 记录过 504。 | 管道未启用 `pipefail`，不能将 wrapper 退出码解释为 Maven 结果；不报告全构建成功或失败。可重试时需检查/镜像缺失依赖或改善上游仓库可达性。 |
| E-22 | SOURCE-OBSERVED | Desktop 有 Rio Turtle 格式映射单元测试；Web 有多种下载序列化格式的测试源码及角色/访问管理测试源码。 | 固定 SHA 上游测试文件，包括 `DocumentFormatMapper_TestCase`、`FileDownloadParametersTestCase`、`AccessManagerImpl_IT`、`ProjectAccessManagerImpl_IT`。 | 源码/测试存在不等于完整格式往返、授权安全或集成行为已验证。 |
| E-23 | SOURCE-OBSERVED | 两上游固定提交含 BSD 2-Clause 风格根许可证；WebProtégé README 表示该仓库正被细粒度仓库取代。 | 完整克隆的固定 SHA 下 `license.txt`、README、根 POM；Web README 指明更细粒度仓库。 | 未审计完整传递依赖许可证，也未确认替代仓库的候选版本或 OpenProtégé 采用决策。 |

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
