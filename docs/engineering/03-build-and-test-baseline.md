# 03 — 构建与测试基线

审计日期：2026-10-09

OpenProtégé 当前 HEAD（本轮检查前）：`725e9d21bd36d973211e65e67d248ef4a562f10c`。

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## OpenProtégé 仓库检查

当前仓库没有应用源码、依赖清单、构建/测试脚本或 CI 配置。以下项目级检查未执行；这是缺少可执行项目入口造成的阻塞，不是构建失败。（VERIFIED：当前 Git 跟踪树与工作区盘点）

| 检查 | 是否执行 | 结果 | 状态 |
|---|---:|---|---|
| 依赖完整性 | 否 | 无依赖清单可选用包管理器 | BLOCKED |
| 编译/应用构建 | 否 | 无构建入口 | BLOCKED |
| 单元、集成、端到端测试 | 否 | 无应用测试源码或测试配置 | BLOCKED |
| 类型、格式、静态分析 | 否 | 无应用源码或工具配置 | BLOCKED |
| 安全依赖扫描 | 否 | 无依赖清单 | BLOCKED |
| 部署、冒烟、备份恢复 | 否 | 无产品或部署配置 | BLOCKED |

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

执行了三次 `clean package` 检查，必须区分工具链和数据库条件：

| 检查 | 命令/条件 | 结果 |
|---|---|---|
| 初次构建 | JDK `21.0.12.1`、Maven `3.9.16`；`mvn -B -ntp clean package`，未启动 MongoDB | 退出码 1，`BUILD FAILURE`；服务器 `EntityTagsRepositoryImpl_TestCase` 中 3 个测试因连接 `localhost:27017` 失败，抛出 Mongo timeout/connection refused。共生成 469 份报告、3562 tests、0 failures、3 errors；该数是失败构建截至中断模块的部分执行量，不代表完整测试总量。 |
| 提供上游 Compose 指定数据库后的检查 | JDK `25.0.4.1`、Maven `3.9.16`；MongoDB `4.1.13` 已运行；`mvn -B -ntp clean package` | 退出码 1，`BUILD FAILURE`；`webprotege-shared` 编译未生成源码所引用的 `AutoValue_*` 类型。 |
| 注解处理诊断 | 相同 JDK/Mongo；`mvn -B -ntp clean package -Dmaven.compiler.proc=full` | 退出码 1；同类 `AutoValue_*` 缺失仍在，不能据此断定该版本的 JDK/注解处理兼容问题已解决。 |
| JDK 21 初次重试 | 容器 `maven:3.9.16-eclipse-temurin-21`、MongoDB `4.1.13`；`mvn -B -ntp clean package` | 未得到完整 Maven 退出码，也没有 `BUILD SUCCESS`/`BUILD FAILURE` 汇总。5 个已完成模块报告 4091 tests、0 failures、0 errors、0 skipped；随后 Maven 依赖解析停在项目 GitHub Maven 仓库。约 19 分钟无测试进展后停止隔离容器。完整 package 此次执行未完成，不能把部分测试计作全套通过。 |
| JDK 21 恢复后重试 | 容器 `maven:3.9.16-eclipse-temurin-21`、MongoDB `4.1.13`；`mvn -B -ntp clean package` | 在仓库 endpoint 又一次探测 HTTP 504 后，随后探测得到 HTTP 200（21.46 秒），故启动新的 package 重试；容器 Maven 命令正在执行，最终退出码/测试结果尚待记录。 |

JDK 25 编译错误说明环境/编译处理不兼容的可能性，不足以单独判定上游源码缺陷。JDK 21 初次重试日志位于 `/tmp/openprotege-stage1-poc.9qWMGG/webprotege-clean-package-jdk21-mongo4.1.log`；当时的 shell 用 Docker 管道保存输出且未启用 pipefail，故 wrapper 的退出码不能代表 Maven 退出码。随后 endpoint 先返回 HTTP 504（11.04 秒）再返回 HTTP 200（21.46 秒），遂开启恢复后重试，日志位于 `/tmp/openprotege-stage1-poc.9qWMGG/webprotege-clean-package-jdk21-mongo4.1-retry.log`；该命令完成前不得报告结果。探测命令为 `curl -sS -L --connect-timeout 10 --max-time 25 -o /dev/null -w 'Protege Maven repo HTTP %{http_code}; time %{time_total}s\\n' https://github.com/protegeproject/mvn-repo/raw/master/releases/`；Maven Central 根地址两次均返回 HTTP 200。（VERIFIED；E-25）即使构建成功，也不等同于部署、格式往返、真实协作或权限隔离测试。

## 边界与可复现性

- 上游源码均为 detached checkout 的固定 SHA；本轮没有修改其 tracked 源码。
- MongoDB 测试依赖来自上游 Compose 声明的 `mongo:4.1-bionic`；容器使用临时文件系统，没有挂载宿主项目数据。
- 未运行 Web 服务或 Desktop GUI，未执行编辑器级往返、跨客户端交换、权限攻击、安全扫描或全传递依赖许可证检查；上述 OWLAPI 单样例 PoC 不覆盖这些流程。
- 本表中所有“未执行”与“执行失败”均分开记录。任何上游成功结果都不能计作 OpenProtégé 当前功能。
