# 02 — 模块清单

审计日期：2026-10-09

当前复核范围：`725e9d21bd36d973211e65e67d248ef4a562f10c` 的 Git 跟踪树及工作区。最初无源码基线提交为 `d9caafee6858a958ea7a2944d574407a79f88309`。

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## 模块索引

仓库当前没有应用实现模块可供按业务边界拆分审计。当前跟踪到的 README 与审计/需求文档不属于运行模块：

| 模块/文件 | 入口与职责 | 依赖 / 数据结构 / 对外接口 | 测试 | 限制与处理建议 |
|---|---|---|---|---|
| `README.md`、`README.en.md` | 中英文项目说明；不是应用入口。 | 无依赖声明、数据结构或 API。 | 无应用测试覆盖。 | 保持项目介绍与已验证事实一致；不能据此推断功能。 |
| `docs/` | 已跟踪的工程审计、需求与架构草案；非应用模块。 | Markdown/CSV 文档，无运行时依赖。 | 对文档执行链接/格式检查；不代表应用测试。 | 保留证据等级并随实测结果增量更新。 |
| `docs/engineering/poc/` | 独立 OWLAPI 文件格式往返 PoC harness 与依赖声明；不是产品源码或构建配置。 | OWLAPI 4.5.29 单一依赖；不被应用引用。 | 在固定 Desktop 测试样例上手动执行，结果见 E-24。 | 单样例格式读写证据，不代表 Desktop GUI/Web/应用级流程。 |

## 未发现的预期边界

在 OpenProtégé 当前提交中，未发现桌面客户端、Web 客户端、服务端/API、领域模型、本体文件处理、存储、身份授权、后台任务、插件或 AI/Agent 模块。故入口、依赖方向、核心数据结构、接口和应用测试覆盖都无法识别。（VERIFIED：当前跟踪树与目录盘点）上游项目各自的模块结构和构建结果另见 [upstream-audit.md](./upstream-audit.md)，不属于 OpenProtégé 模块。

| 审计项 | 结果 | 状态 |
|---|---|---|
| 产品编程语言/版本 | 无应用源码；不可识别。独立 PoC harness 使用 Java，不能推断产品栈。 | UNVERIFIED |
| UI 技术 / 桌面框架 | 未发现 | SOURCE-OBSERVED |
| 后端 / 服务入口 | 未发现 | SOURCE-OBSERVED |
| 数据库、文件存储、缓存 | 未发现 | SOURCE-OBSERVED |
| RDF、OWL、SPARQL、SHACL 库 | 未发现依赖或源码引用 | SOURCE-OBSERVED |
| 解析器、推理机、校验器、转换器 | 未发现 | SOURCE-OBSERVED |
| API、事件、异步任务 | 未发现 | SOURCE-OBSERVED |
| 构建、测试、打包、发布模块 | 未发现 | SOURCE-OBSERVED |

## 保留、改进、隔离或替换

当前没有运行代码可作模块替换决策。新增实现应在需求与许可确认后以小步、可测试切片接入，不应为“整齐”而预先创建空模块或复制上游工程。（PROPOSED）

上游项目的模块边界、构建与测试证据属于上游自身，不是 OpenProtégé 模块；详见 [06-dependency-and-upstream-review.md](./06-dependency-and-upstream-review.md)。
