# 03 — 构建与测试基线

审计日期：2026-10-09

OpenProtégé 当前 HEAD：`e595add49b43a83bb68c7c618ee4f0416b2b2e6b`。

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

## Protégé Desktop

| 检查 | 命令 | 结果 |
|---|---|---|
| 构建与测试 | `mvn -B -ntp -Drelease.signing.disabled=true clean verify` | 退出码 0，Maven 输出 `BUILD SUCCESS`；76 个 Surefire/Failsafe XML 报告，合计 531 tests、0 failures、0 errors、3 skipped。 |

环境：OpenJDK `21.0.12.1`、Maven `3.9.16`。日志输出于构建会话，报告位于临时克隆各模块 `target/{surefire,failsafe}-reports/`。该结果只证明固定提交在该环境下的 Maven 验证成功；不证明安装后桌面运行、本体往返保真、Web 互操作、插件兼容或 OpenProtégé 集成。

## WebProtégé

执行了三次 `clean package` 检查，必须区分工具链和数据库条件：

| 检查 | 命令/条件 | 结果 |
|---|---|---|
| 初次构建 | JDK `21.0.12.1`、Maven `3.9.16`；`mvn -B -ntp clean package`，未启动 MongoDB | 退出码 1，`BUILD FAILURE`；服务器 `EntityTagsRepositoryImpl_TestCase` 中 3 个测试因连接 `localhost:27017` 失败，抛出 Mongo timeout/connection refused。共生成 469 份报告、3562 tests、0 failures、3 errors；该数是失败构建截至中断模块的部分执行量，不代表完整测试总量。 |
| 提供上游 Compose 指定数据库后的检查 | JDK `25.0.4.1`、Maven `3.9.16`；MongoDB `4.1.13` 已运行；`mvn -B -ntp clean package` | 退出码 1，`BUILD FAILURE`；`webprotege-shared` 编译未生成源码所引用的 `AutoValue_*` 类型。 |
| 注解处理诊断 | 相同 JDK/Mongo；`mvn -B -ntp clean package -Dmaven.compiler.proc=full` | 退出码 1；同类 `AutoValue_*` 缺失仍在，不能据此断定该版本的 JDK/注解处理兼容问题已解决。 |
| JDK 21 重试 | 容器 `maven:3.9.16-eclipse-temurin-21`、MongoDB `4.1.13`；`mvn -B -ntp clean package` | 未得到完整 Maven 退出码，也没有 `BUILD SUCCESS`/`BUILD FAILURE` 汇总。5 个已完成模块报告 4091 tests、0 failures、0 errors、0 skipped；随后 Maven 依赖解析停在项目 GitHub Maven 仓库响应（观察到对该仓库的 HTTP 504），19 分钟无测试进展后停止隔离容器。完整 package 标为 BLOCKED，不能把部分测试计作全套通过。 |

JDK 25 编译错误说明环境/编译处理不兼容的可能性，不足以单独判定上游源码缺陷。JDK 21 日志位于 `/tmp/openprotege-stage1-poc.9qWMGG/webprotege-clean-package-jdk21-mongo4.1.log`；该 shell 用 Docker 管道保存输出且未启用 pipefail，故 Docker wrapper 的退出码不能代表 Maven 退出码。Maven 日志无完整构建结尾，因此最终状态是网络阻塞而非通过/失败。即使构建成功，也不等同于部署、格式往返、真实协作或权限隔离测试。

## 边界与可复现性

- 上游源码均为 detached checkout 的固定 SHA；本轮没有修改其 tracked 源码。
- MongoDB 测试依赖来自上游 Compose 声明的 `mongo:4.1-bionic`；容器使用临时文件系统，没有挂载宿主项目数据。
- 未运行 Web 服务、桌面 GUI、格式往返、跨客户端交换、权限攻击、安全扫描或全传递依赖许可证检查。
- 本表中所有“未执行”与“执行失败”均分开记录。任何上游成功结果都不能计作 OpenProtégé 当前功能。
