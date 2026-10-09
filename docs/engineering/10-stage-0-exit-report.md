# 10 — Stage 0 退出报告

报告日期：2026-10-09

前一阶段 PoC 文档更新前的 OpenProtégé 检出为 `main`，HEAD `725e9d21bd36d973211e65e67d248ef4a562f10c`。该阶段 PoC 文档已提交于 `5734e976846b5de683caa67c9ae2115d8a5017ae`。本轮镜像验证开始时，`main` 与 `origin/main` 对齐、工作区干净；本轮仅更新工程审计文档，尚未提交。

判定：**部分完成；不宣告 Stage 0 全部退出。**

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## 执行摘要

OpenProtégé 当前已有中英文 README 与工程/需求草案，但仍没有应用源码、应用依赖清单、构建/测试入口、CI 或项目 LICENSE。负责人已确认产品基线决策；需求草案记录这些决策，不构成实现证据。

本轮按固定 SHA 完整克隆并验证了指定的 Protégé Desktop 与 WebProtégé 上游。Desktop 固定提交在 JDK 21 下 Maven `clean verify` 成功（531 tests、0 failures、0 errors、3 skipped）。另在 Desktop 固定提交的 `pizza.owl` 样例上使用 OWLAPI 4.5.29 验证 RDF/XML→Turtle→RDF/XML 文件往返，930 个公理及 ontology ID、注释和 imports 精确保持。WebProtégé 首次构建因缺少 MongoDB 测试依赖失败；提供数据库后，JDK 25 构建因 AutoValue 类型未生成而失败；JDK 21/MongoDB 4.1 初次重试的 5 个模块有 4091 tests、0 failures、0 errors、0 skipped，但 package 依赖解析遇到 GitHub Maven 仓库 HTTP 504，未取得最终状态。后续 Aliyun Central mirror 重试在 JDK 21/MongoDB 4.1 下完成 9/9 模块 `clean package`，Maven 退出码 0，7 个模块测试合计 4124 tests、0 failures、0 errors、0 skipped。该次构建使用已有本地 Maven 缓存，不证明空缓存依赖获取，也不等同于 Web 服务运行、编辑器或 OpenProtégé 功能验证。

尚未完成的关键 PoC 包括编辑器/应用级 OWL/RDF 工作流、桌面/Web 文件交换、服务端身份/授权和项目隔离、插件验证、全依赖许可证审查及安全测试。一个 OWLAPI 单样例文件级往返已通过；另一个探索性样例出现公理差异且原因未明。阶段退出条件尚未全部满足。

## 退出条件逐项判定

| # | 条件 | 判定 | 证据 / 未完成事项 |
|---:|---|---|---|
| 1 | 仓库现状和 Git 基线已记录 | 满足（当前复核） | [00-repository-baseline.md](./00-repository-baseline.md)：当前分支、HEAD、干净状态、提交历史及无应用源码事实已记录；GitHub Releases 仍因未认证未确认。 |
| 2 | 技术栈与主要模块基于真实文件识别 | 满足（OpenProtégé 现状） | 已验证当前没有 OpenProtégé 应用技术栈/运行模块；上游模块结构单独记录，不混同项目模块。见 [01-current-architecture.md](./01-current-architecture.md)、[02-module-inventory.md](./02-module-inventory.md)。 |
| 3 | 构建和测试执行状态如实记录 | 满足（项目自身仍受阻） | [03-build-and-test-baseline.md](./03-build-and-test-baseline.md)：OpenProtégé 无构建入口（BLOCKED）；Desktop 固定提交通过；WebProtégé 固定提交 Aliyun Central mirror 重试完整 package 通过（9 模块，4124 tests、0 failures/errors/skips）；其他失败/中断重试单独记录。 |
| 4 | 产品能力矩阵已建立 | 满足（范围判断） | [04-product-capability-matrix.md](./04-product-capability-matrix.md)：目标能力均按 OpenProtégé 自身实现证据判定，上游能力没有混入。 |
| 5 | 主要安全风险与技术债已登记 | 部分满足 | 静态边界与未验证攻击面已登记；没有 OpenProtégé 应用代码可供运行安全验证，服务端权限 PoC 仍未执行。见 [05-security-review.md](./05-security-review.md)、[07-technical-debt-register.md](./07-technical-debt-register.md)。 |
| 6 | 上游与第三方关系已核实，或阻塞明确 | 部分满足 | 两上游固定 SHA、完整克隆、根许可证及构建/测试证据已核实；单样例 OWLAPI 文件往返通过；Web 完整 package 已通过，但空缓存依赖获取、互操作、全依赖许可证、fork 元数据与替代仓库选择仍未核实。见 [06-dependency-and-upstream-review.md](./06-dependency-and-upstream-review.md)。 |
| 7 | 工程文档已创建并检查 | 部分满足 | 双语 README、Stage 0 与需求草案已提交；本轮更新审计记录。`git diff --check` 退出码 0；16 份工程 Markdown 的本地链接检查覆盖 86 个链接、0 个缺失目标。英文工程文档翻译仍 Pending。 |
| 8 | 未破坏用户已有修改 | 满足（本轮复核） | 本轮开始时工作区干净；仅按要求修改 8 个工程 Markdown 文件，上游固定 SHA 克隆和生成构建结果位于 `/tmp`，未覆盖项目源码或其他既有更改。 |
| 9 | 关键结论均有证据等级 | 满足（本报告范围） | [evidence-ledger.md](./evidence-ledger.md) 记录仓库现状、上游固定 SHA、构建结果和镜像/缓存限制；仍有列明的 UNVERIFIED/BLOCKED 项，不作超出证据的断言。 |
| 10 | 下一阶段输入、输出、验收已明确 | 部分满足 | 产品决策与需求草案已形成；技术/安全/许可 gate 尚需完成，见下文下一项工程任务。 |

整体结论：**Stage 0 部分完成**。已建立可复核的仓库与固定上游证据，但上游验证、安全与许可证门尚未充分完成。此报告不把文档完成、源码存在或单次 Maven 测试通过等同于产品功能交付。

## 产品决策与技术验证区分

负责人已确认：支持个人与团队用户；OWL 2 为核心且优先验证 RDF/XML/Turtle；桌面端完整本地编辑、Web 端项目管理与协作、首期文件交换；自托管及公开/私有项目与角色权限；Apache-2.0 仅作为候选评估；AI 首期建议式建模、只读语义检索、写入前用户确认。证据见 [SRS 第 8 节](../requirements/SRS.md#8-需求基线决策与技术决策分层) 和 E-15。

上述是已确认的产品决策，不代表上游在指定流程下已满足要求，也不代表 OpenProtégé 已实现、许可证兼容或通过安全验证。

## 新增/修改文件

- [00-repository-baseline.md](./00-repository-baseline.md)
- [02-module-inventory.md](./02-module-inventory.md)
- [03-build-and-test-baseline.md](./03-build-and-test-baseline.md)
- [04-product-capability-matrix.md](./04-product-capability-matrix.md)
- [06-dependency-and-upstream-review.md](./06-dependency-and-upstream-review.md)
- [07-technical-debt-register.md](./07-technical-debt-register.md)
- [10-stage-0-exit-report.md](./10-stage-0-exit-report.md)
- [evidence-ledger.md](./evidence-ledger.md)
- [repository-audit.md](./repository-audit.md)
- [upstream-audit.md](./upstream-audit.md)
- [OWLAPI 往返 PoC harness](./poc/OwlFormatRoundTrip.java)
- [OWLAPI PoC 依赖声明](./poc/owlapi-poc-pom.xml)

## 未完成与阻塞项

1. Aliyun Central mirror 配置下的 WebProtégé 固定提交完整 package 已成功；先前 HTTP 504 重试及一次因环境重启丢失结果的运行不作为成功证据。此次成功运行使用已有 `$HOME/.m2` 缓存，空缓存与专用仓库依赖的可达性仍未验证。
2. OWLAPI 对固定 `pizza.owl` 的 RDF/XML/Turtle 文件往返精确保持 930 个公理，但未运行 Desktop GUI 或 Web 服务；编辑器级导入—编辑—保存—导出、更多 OWL 2 数据集和双端交换仍未验证。探索性 `photography.owl` 序列化比较出现公理变化（672→707），原因待查。
3. Web 授权实现与集成测试源码可观察，但未验证未授权、跨项目读写拒绝或实际服务隔离。
4. 未解析上游完整传递依赖许可证/漏洞；OpenProtégé 尚未选定最终许可证。Apache-2.0 是负责人要求评估的候选。
5. GitHub Release/远端 fork 元数据仍待授权只读查询；WebProtégé README 指向替代的细分仓库，候选替代版本尚未评估。
6. 英文工程/需求文档翻译 Pending；本轮 Markdown 空白检查与 10 份工程文档的相对链接检查已通过，完整双语文档一致性仍未完成。
7. OpenProtégé 当前没有应用源码，项目自身构建、测试、运行、安全、性能及部署验证仍 BLOCKED。

## 下一项明确工程任务

### 输入

- 当前证据台账及固定上游 SHA。
- 已确认的产品决策和需求草案。
- Desktop JDK 21 构建证据；WebProtégé JDK 21/MongoDB 4.1/Aliyun Central mirror 的完整构建及测试证据（同时保留早期 HTTP 504 的历史记录）。

### 输出

- WebProtégé Maven 仓库在空缓存下的具体缺失 artifact 和可达替代途径；本轮已有缓存条件下的固定提交构建结果记录于 E-27。
- 多个经审阅的 RDF/XML/Turtle 样例、目标 OWL 2 Profile 和完整应用级保真判据。
- 固定样例下 Desktop/Web 文件交换结果；不得用格式选择单测或 OWLAPI 单样例 PoC 替代。
- Web 服务端匿名/越权/跨项目访问测试结果（如可在隔离实例执行）。
- 上游完整依赖和许可证/来源审查；未决项及技术 ADR。

### 验收标准

1. 每项实测记录固定 SHA、环境/镜像、命令、实际可取得的退出状态/测试数、日志位置和限制；网络阻塞时不虚报构建退出码。
2. 文件 PoC 记录测试本体 SHA-256、固定依赖版本、导入/保存/导出/再加载步骤及获批语义不变量；格式序列化 PoC 与编辑器/应用流程分开报告。
3. 权限 PoC 从服务端请求验证允许/拒绝结果，不以 UI 隐藏作为授权证明。
4. 许可证清单覆盖直接与传递依赖并附来源；未解决项明确阻塞最终选型。
5. OpenProtégé 功能状态仍仅以本仓库实现和测试证据为准。

未经明确授权，不推送、发布或生产部署。
