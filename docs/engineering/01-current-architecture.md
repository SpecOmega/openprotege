# 01 — 当前架构事实与边界

审计日期：2026-10-09

对象：OpenProtégé 当前检出 HEAD `725e9d21bd36d973211e65e67d248ef4a562f10c`。初始无源码基线提交为 `d9caafee6858a958ea7a2944d574407a79f88309`。

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## 当前状态

| ID | 架构面 | 当前证据 | 状态 |
|---|---|---|---|
| AR-01 | 应用入口与模块边界 | 当前 HEAD 含 README 与工程/需求文档；无应用源代码入口或运行模块。 | VERIFIED |
| AR-02 | 前端、桌面端、后端、服务进程 | 未发现实现文件、项目配置或启动入口。 | SOURCE-OBSERVED |
| AR-03 | 数据库、对象存储、缓存、事件队列 | 未发现依赖、schema 或连接配置。 | SOURCE-OBSERVED |
| AR-04 | 本体解析、推理、RDF/OWL/SPARQL/SHACL | 未发现解析器/推理机依赖或代码引用。 | SOURCE-OBSERVED |
| AR-05 | 身份、授权、审计、项目隔离 | 未发现认证或授权实现。 | SOURCE-OBSERVED |
| AR-06 | AI、检索、Agent | README 有 AI 定位文字，但无实现证据。 | SOURCE-OBSERVED |
| AR-07 | 可运行的系统架构 | 当前无法绘制代码级组件关系。 | UNVERIFIED |

README 中的产品定位不能证明任何运行功能。具体模块盘点见 [02-module-inventory.md](./02-module-inventory.md)；目标能力状态见 [04-product-capability-matrix.md](./04-product-capability-matrix.md)。

## 系统边界图

```text
产品目标（双端、本体工程、协作、AI）
                │
                └── 尚无对应的应用组件或可运行边界（当前 HEAD 的其他内容是 README 与文档）
```

图中的“产品目标”源自用户任务说明，不是仓库实现。当前仓库有需求与架构草案文档，但没有可运行产品组件。该图不是建议的目标架构，也不暗示已经存在服务、数据库或客户端。

## 技术选择结论

- 当前没有产品源码或可运行组件；因此产品语言、框架、运行时、API、数据库或本体工具链不可识别。独立文档 PoC 使用 Java 与 OWLAPI 4.5.29，不构成产品技术栈。（SOURCE-OBSERVED）
- 不能仅因指定上游采用 Java/Maven，就将其推断为 OpenProtégé 产品栈；当前 Maven POM 只服务于隔离的 PoC harness。（VERIFIED：当前产品目录与 [PoC POM](./poc/owlapi-poc-pom.xml)）
- 暂不提出具体目标部署拓扑。先由产品/工程决策确认桌面与 Web 的边界、数据托管模型、离线需求与信任边界。（PROPOSED）
- 安全设计应将服务端授权、隔离及不可信本体文件视为未来需要验证的设计约束；这不是现状检查发现的漏洞。（PROPOSED）

## 目标架构输入（待决）

建立架构前需明确：桌面是独立编辑器、服务端项目的本地工作客户端，还是两者兼具；Web 与桌面是否共享项目协议/存储；本体解析与校验执行位置；网络受控/离线方式；身份提供方；协作冲突语义；项目与数据驻留要求。当前仓库未提供这些决策。（UNVERIFIED）
