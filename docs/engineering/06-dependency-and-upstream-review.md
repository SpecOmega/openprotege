# 06 — 依赖与上游关系审查

审计日期：2026-10-09

OpenProtégé 当前 HEAD：`725e9d21bd36d973211e65e67d248ef4a562f10c`。初始无源码基线 HEAD：`d9caafee6858a958ea7a2944d574407a79f88309`。

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## OpenProtégé 当前依赖

当前 HEAD `725e9d21bd36d973211e65e67d248ef4a562f10c` 跟踪 README 与审计/需求文档；存在一个仅供格式往返 PoC 使用的 OWLAPI 4.5.29 POM，但未发现应用依赖清单、应用锁文件、子模块、上游代码拷贝或应用组件引用。（VERIFIED：`git ls-files`、当前提交和目录盘点）

因此 OpenProtégé 的应用直接依赖版本、传递依赖、许可证清单、漏洞状态及可复现应用构建均为 `UNVERIFIED`；目前无法执行有意义的依赖解析或许可证扫描。两个上游已分别按固定 SHA 完整克隆，其中 Desktop 的构建通过、WebProtégé 构建情况见下表和 [upstream-audit.md](./upstream-audit.md)；上游构建不构成 OpenProtégé 已集成或已构建的证据。

## 上游固定提交

| 上游仓库 | 本次解析的固定 SHA | 来源证据 | 观察结果与限制 |
|---|---|---|---|
| `protegeproject/protege` | `bf03cccc65ea664e139d6829bba6d209ef58ee55`，提交时间 `2026-09-23T22:12:40+01:00` | 完整克隆并 detached checkout；HEAD 复核；固定提交根目录、POM、README、许可证、CI、测试。 | Desktop Maven `clean verify` 在 JDK 21.0.12.1/Maven 3.9.16 下退出码 0；531 tests、0 failures、0 errors、3 skipped。OWLAPI 4.5.29 单样例 RDF/XML↔Turtle 往返 PoC 通过；未运行 GUI、编辑器交互或互操作。 |
| `protegeproject/webprotege` | `1e84fa02aef68be45f18c08dbeae94bec9b04a41`，提交时间 `2026-07-28T13:56:32-07:00` | 完整克隆并 detached checkout；HEAD 复核；固定提交根目录、POM、README、许可证、测试。 | JDK 25 两次 `clean package` 失败（先因 Mongo 缺失、后因 AutoValue 类型未生成）；JDK 21 + MongoDB 4.1.13 重试完成 5 个模块的 4091 项测试且无失败，但完整 package 阶段在项目 GitHub Maven 仓库 HTTP 504 后阻塞并停止，未取得 Maven 最终状态。README 称该仓库正被细粒度仓库取代。 |

逐项来源和措辞边界见 [upstream-audit.md](./upstream-audit.md)。

## Fork、复用、标准与许可证

| 问题 | 结论 | 状态 |
|---|---|---|
| 是否存在 Git fork 关系 | 当前 OpenProtégé 历史为自主根提交并无上游 remote；未取得 GitHub fork 元数据，不能断言远端 fork 标记状态。 | SOURCE-OBSERVED / UNVERIFIED |
| 是否复用上游源码 | 当前 OpenProtégé 跟踪树没有上游源码或子模块。 | VERIFIED（当前检出） |
| 是否使用上游组件/协议/格式 | OpenProtégé 当前没有依赖配置或应用代码引用，尚未观察到组件集成；未来使用 OWL/RDF 格式属于已确认需求，不是现有代码能力。 | SOURCE-OBSERVED |
| 上游当前固定版本及依赖 | 表内固定 SHA 是远端 HEAD 解析结果，不是 OpenProtégé 已采用的版本；OpenProtégé 无版本声明。 | VERIFIED / SOURCE-OBSERVED |
| 上游许可证兼容性 | 两上游的 `license.txt` 为 BSD 2-Clause 风格；OpenProtégé 尚无 LICENSE，且上游传递依赖许可证未审计，兼容性未判定。负责人确认 Apache-2.0 为待评估方案，不是决定。 | SOURCE-OBSERVED / UNVERIFIED |
| 重复实现/协议兼容 | 没有 OpenProtégé 实现可比较。 | UNVERIFIED |
| 上游维护/迁移风险 | WebProtégé README 明确提示该仓库正在被更细粒度仓库取代；具体替代仓库选择和支持状态未验证。 | SOURCE-OBSERVED / UNVERIFIED |

不得因项目名称或方向相似而断言代码继承。BSD 风格许可证观察不等于 OpenProtégé 已选择该许可证，也不代替完整法律审查。

## 决策前 PoC

已完成固定提交克隆、部分构建/测试 PoC，以及基于 OWLAPI 4.5.29 的独立样例格式往返 PoC；WebProtégé JDK 21 package 仍受项目 Maven 仓库 HTTP 504 阻塞，端点复核见 [03-build-and-test-baseline.md](./03-build-and-test-baseline.md)。下一步解决依赖获取阻塞，再验证编辑器/应用级导入—编辑—保存—导出往返、插件扩展点、Desktop/Web 文件交换、Web 服务端认证授权与项目隔离；解析完整依赖树及许可证。保存完整命令、环境、可得退出状态、日志和测试数据哈希。（PROPOSED；未完成部分为 UNVERIFIED/BLOCKED）

在上述结果、产品职责和许可证策略确认之前，不选择“整体 fork”或“重写”，也不宣称已有互操作能力。
