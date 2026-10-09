# 02 — 模块清单

审计日期：2026-10-09

固定范围：`d9caafee6858a958ea7a2944d574407a79f88309` 的 Git 跟踪树及本地工作区目录。

语言：中文主文档；英文翻译状态为 Pending，见 [索引](./README.md)。

## 模块索引

仓库当前没有实现模块可供按业务边界拆分审计。唯一跟踪文件如下：

| 模块/文件 | 入口与职责 | 依赖 / 数据结构 / 对外接口 | 测试 | 限制与处理建议 |
|---|---|---|---|---|
| `README.md` | 仓库唯一跟踪文件；包含标题和一句产品说明。不是应用入口。 | 无依赖声明、数据结构或 API。 | 无测试覆盖。 | 保留并在后续补充可验证项目说明；不能据此推断功能。 |
| `docs/engineering/` | 当前未跟踪审计文档，非应用模块。 | Markdown 文档，无运行时依赖。 | 检查相对链接及 `git diff --check`。 | 保留审计证据；不属于基线 HEAD 的源码。 |

## 未发现的预期边界

未发现桌面客户端、Web 客户端、服务端/API、领域模型、本体文件处理、存储、身份授权、后台任务、插件或 AI/Agent 模块。故入口、依赖方向、核心数据结构、接口和单元测试覆盖都无法识别。（SOURCE-OBSERVED）

| 审计项 | 结果 | 状态 |
|---|---|---|
| 编程语言/版本 | 无项目源码；不可识别 | UNVERIFIED |
| UI 技术 / 桌面框架 | 未发现 | SOURCE-OBSERVED |
| 后端 / 服务入口 | 未发现 | SOURCE-OBSERVED |
| 数据库、文件存储、缓存 | 未发现 | SOURCE-OBSERVED |
| RDF、OWL、SPARQL、SHACL 库 | 未发现依赖或源码引用 | SOURCE-OBSERVED |
| 解析器、推理机、校验器、转换器 | 未发现 | SOURCE-OBSERVED |
| API、事件、异步任务 | 未发现 | SOURCE-OBSERVED |
| 构建、测试、打包、发布模块 | 未发现 | SOURCE-OBSERVED |

## 保留、改进、隔离或替换

当前没有运行代码可作模块替换决策。新增实现应在需求与许可确认后以小步、可测试切片接入，不应为“整齐”而预先创建空模块或复制上游工程。（PROPOSED）

上游项目的模块边界属于上游自身，不是 OpenProtégé 模块；详见 [06-dependency-and-upstream-review.md](./06-dependency-and-upstream-review.md)。
