# 本体文件导入/导出模块 — 规格概览与确认清单

**编写日期**：2026-10-09
**状态**：0.1 规格草案；不表示全部条款已获批准、已实现或已验收。当前事实以[验收标准](./acceptance-criteria.md)、[需求追踪矩阵](./traceability-matrix.csv)和[工程缺口清单](../engineering/known-gaps.md)为准。

---

## 概述

本套草案记录 OpenProtégé **本体文件导入/导出与版本管理**的目标需求、拟议设计、验收条件和初始实现计划；详细范围和关键验收方法仍需结合产品决策与当前实现复核。

该模块聚焦于 Web 后端对本体文件生命周期的支持，是桌面端与 Web 端协作的关键基础。

---

## 核心决策摘要

| 决策项 | 决定 | 依据 |
|---|---|---|
| **OWL 目标** | OWL 2 为产品核心方向 | 负责人确认；具体 OWL 2 Profile 支持范围仍待冻结 |
| **首版格式方向** | 优先验证 RDF/XML、Turtle | 负责人确认优先级；不代表完整格式兼容已验收。其他格式为待评估方向 |
| **存储方式** | PostgreSQL BYTEA / MySQL LONGBLOB | 当前工作树按数据库 vendor 使用相应 schema；最大文件资源安全尚未验收 |
| **版本模型** | 不可变文件快照 | 已实现导入新快照和恢复为新快照；并发冲突语义、版本删除未实现 |
| **OWLAPI 版本** | 5.1.20 | 当前服务依赖版本；不构成上游集成或全格式兼容结论 |
| **权限策略** | Owner/Admin/Editor 可导入，Viewer 可读取/导出 | 当前 API 有部分权限路径测试；完整角色/对象矩阵与浏览器 E2E 仍待验收；版本删除未实现 |
| **往返保真** | 需通过评审冻结 OWL 语义等价规则 | 属于验收目标草案；当前没有经批准的语料/比较准则，Pizza 项目集成测试未完成 |
| **测试语料** | Pizza、Minimal、With-Imports、With-Annotations 为拟议语料 | 当前双语 Turtle fixtures 与小型 RDF/XML 样例有 API 测试；拟议语料集合尚未全部建立/验收 |
| **安全界限** | 配置默认上传上限 500 MiB；imports 不自动联网 | 代码有大小限制及本地 import 处理；最大尺寸、XXE、解析资源消耗的专项测试仍未完成 |
| **错误契约** | 本文中的错误码/状态码为目标契约草案 | 需与实际 controller/error handler 逐项对齐并验收；不得将草案表格视为已实现契约 |
| **审计** | 记录导入/导出/恢复相关事件 | 代码已有关键事件；删除版本未实现，保留期、查询/导出与完整运维策略尚未定义 |

### 当前实现与验收边界（2026-10-11）

- 当前代码包含 RDF/XML/Turtle 文件导入、不可变快照、历史版本分页、导出/格式转换、历史版本恢复为新快照及对应审计路径；详细测试范围和不覆盖项见[已知缺口](../engineering/known-gaps.md)及[证据台账](../engineering/evidence-ledger.md)。
- HermiT OWL 2 DL profile/一致性/解释/层级/个体分类和单条临时 SWRL 推理属于已实现切片；并不等同于完整的本体实体编辑器。
- Testcontainers 集成测试必须用 `-Dopenprotege.testcontainers.enabled=true` 显式启用。当前默认 Maven 测试有 9 项跳过；不能以默认 Maven BUILD SUCCESS 代替 PostgreSQL/MySQL 集成验收。
- 桌面端、本体实体级编辑、版本删除、批准的 OWL 语义保真测试语料、浏览器 E2E、最大文件资源安全和生产级恢复演练均未完成或未验收。

---

## 文档套件结构

本规格由以下文档组成：

### 1. **需求规格** 
📄 [`ontology-file-import-export-spec.md`](./ontology-file-import-export-spec.md)

- 模块目标与输入/输出
- 格式支持与识别规则（第 2.1 节）
- 导入流程（第 2.2 节，7 步流程图）
- 导出流程（第 2.3 节，6 步流程图）
- 往返保真定义与测试语料（第 2.4 节）
- **错误契约表** 12 种错误代码与恢复建议（第 2.5 节）
- 安全与资源限制（第 2.6 节）
- 数据模型扩展（第 3 节）
- API 规格 4 个端点（第 4 节）
- 安全考虑（第 5 节）
- 验收标准概览（第 6 节）

**长度**：13 KB / 13,015 字

---

### 2. **架构决策记录**
📄 [`docs/architecture/adr/0003-ontology-file-management.md`](../architecture/adr/0003-ontology-file-management.md)

8 个关键决策，每个包含：
- 决策内容（选择了什么）
- 理由（为什么这样选）
- 权衡与限制（代价是什么）
- 验收标准（如何验证实现）

**包含的决策**：
1. 本体存储设计 → 数据库 BYTEA
2. 版本管理模型 → 快照
3. OWLAPI 版本选择 → 5.1.20
4. 格式支持与识别优先级 → RDF/XML、Turtle 首版
5. 权限模型 → 对象级、角色分级
6. 往返保真定义 → OWL 2 语义等价
7. 错误恢复与事务一致性 → 原子导入、失败回滚
8. 审计日志设计 → 不记录敏感数据

**长度**：7.8 KB / 7,799 字

---

### 3. **验收标准**
📄 [`ontology-file-import-export-acceptance-criteria.md`](./ontology-file-import-export-acceptance-criteria.md)

**60+ 条具体验收条件**，分 6 个类别：

| 类别 | 条数 | 范围 |
|---|---|---|
| 功能验收 | 18 | 格式、导入、导出、版本、权限 |
| 往返保真 | 14 | Pizza（4 个往返）、Minimal、With-Imports、With-Annotations |
| 安全与资源 | 11 | 文件大小、权限隔离、XXE、错误定位 |
| 审计与追踪 | 6 | 审计日志、敏感数据、操作链 |
| API 合约 | 14 | 导入、导出、版本列表、元数据端点 |
| 非功能 | 6 | 代码质量、文档 |

**格式**：每条验收条件包含 AC ID、期望结果、测试方法、验证方式

**长度**：10.7 KB / 10,698 字

---

### 4. **实现计划**
📄 [`ontology-file-module-implementation-plan.md`](./ontology-file-module-implementation-plan.md)

**工程蓝图**：
- 模块概述与交付清单
- 代码结构与包设计（完整目录树）
- 4 个核心类的详细设计
  - `OntologyVersion` entity
  - `OntologyAuditLog` entity
  - `OntologyService` （11 个关键方法签名）
  - `OntologyController` （5 个 REST 端点）
- Maven 依赖清单
- 与现有模块的集成点
- 7 个开发阶段（共 12-16 天预估）
- 测试策略与覆盖目标（≥ 60% + ≥ 25 个集成测试）
- 验收提交清单（代码、文档、测试记录）

**长度**：16.6 KB / 16,628 字

---

### 5. **需求追踪矩阵更新**
📄 [`docs/requirements/traceability-matrix.csv`](./traceability-matrix.csv)

已追加本体模块相关需求：
- `OP-ONT-001` ~ `OP-ONT-008`：本体核心需求
- 关联的验收标准与测试 ID

---

## 关键数据与指标

### 草案中的参考指标（未确认、未测量）

下表保留 2026-10-09 草案提出的参考目标，不是负责人已批准的发布门槛，也不是当前测试结果。是否继续采用需单独评审。

| 指标 | 目标 | 验证方式 |
|---|---|---|
| 单元测试覆盖率 | ≥ 60% | `mvn jacoco:report` |
| 集成测试数量 | ≥ 25 个 | Testcontainers 测试类 |
| 单元测试通过率 | 100% | Maven BUILD SUCCESS |
| 集成测试通过率 | 100% | Testcontainers BUILD SUCCESS |
| 往返保真测试 | 4 个本体 × 多个格式组合 | OWLAPI 公理集比较 |

### 历史粗略工期估计（非承诺）

下列估算仅来自初始草案，不代表当前剩余工作的估时、排期、资源承诺或完成日期。

| 阶段 | 任务 | 预估时间 |
|---|---|---|
| Phase 1 | 数据库与模型 | 1-2 天 |
| Phase 2 | OWLAPI 集成与格式处理 | 2-3 天 |
| Phase 3 | 服务层与权限 | 2-3 天 |
| Phase 4 | REST 端点与 API | 1-2 天 |
| Phase 5 | 集成测试与往返保真 | 2-3 天 |
| Phase 6 | 安全与 XXE 防护 | 1-2 天 |
| Phase 7 | 文档与验收 | 1 天 |
| **总计** | | **12-16 天** |

---

## 用户确认清单

以下清单是本规格草案的评审清单；已实现的代码不自动代表相应产品标准已批准或验收：

### 📋 需求与决策

- [ ] **格式支持**：确认首期 RDF/XML/Turtle 支持边界；OWL/XML、Functional、JSON-LD 是否纳入待评审
  
- [ ] **OWL 目标**：OWL 2 核心规范（不涵盖 DL/RL/EL Profile 扩展）

- [ ] **往返保真定义**：OWL 2 语义等价
  - 公理集相等（OWLAPI 集合比较）
  - IRI / Base IRI / imports 行为需先评审，不能预先假设相对 IRI 绝对化安全
  - 注释保存（数量与关键值一致）
  - 格式往返中的允许差异需经评审确认

- [ ] **错误契约**：12 种错误代码（HTTP 400/403/404/413/500）如规格第 2.5 节所示
  - 上述数量与状态码是该规格草案提出的方案，须按实际 API 对齐后确认

### 🔒 安全与权限

- [ ] **权限模型**：
  - 导入 ≥ Editor
  - 导出 ≥ Viewer
  - 元数据/成员管理角色边界以已确认的项目角色要求为准；版本删除当前未实现

- [ ] **资源限制**：
  - 单文件 500 MiB 为当前配置默认值，可由 `OPENPROTEGE_ONTOLOGY_MAX_FILE_SIZE` 调整；产品支持上限和最大尺寸性能仍待验收
  - XML 递归深度及导入解析超时没有本规格以外的已确认阈值；需先确定后实现/验证

- [ ] **不可信输入处理**：
  - 显式验证 XXE 与外部实体行为；不得仅凭 OWLAPI 默认配置推断防护通过
  - 相对 IRI 不访问 `file://` 协议
  - 不自动解析远程 import

### 📊 测试与验收

- [ ] **测试语料**：
  - Pizza.owl + Pizza.ttl（候选语料；来源、版本、哈希和公理数须复核）
  - Minimal.owl（候选自构样例）
  - With-Imports.ttl（候选自构样例；含相对 IRI）
  - With-Annotations.owl（候选自构样例；含注释）

- [ ] **往返保真测试**：
  - 4 个本体 × 多个格式组合（至少 RDF/XML ↔ Turtle）
  - OWLAPI 公理集 100% 相等
  - IRI 与注释保存

- [ ] **代码质量门槛**：
  - 单元测试 ≥ 60% 覆盖
  - 集成测试 ≥ 25 个
  - 0 个高优先级代码审查问题

### 📚 文档与交付

- [ ] **API 文档**：导入、导出、版本列表、版本元数据 4 个端点完整
  - 输入、输出、权限、错误

- [ ] **集成指南**：如何使用本体 API 的开发者文档

- [ ] **安全指南**：XXE、权限、大小限制、相对 IRI 处理文档

- [ ] **验收记录**：单元/集成测试结果、往返保真验证、安全测试记录

### 🚀 部署与运维

- [ ] **Flyway 迁移**：`V2__ontology_versions_and_audit.sql` 创建 5 个表

- [ ] **PostgreSQL 17 兼容**：确认 BYTEA、JSONB、indexes 语法

- [ ] **Testcontainers 集成**：覆盖 PostgreSQL + OWLAPI 完整链路

### ✅ 实现约束

- [ ] **不在本模块实现**：
  - OWL 推理与一致性检查（后续迭代）
  - 并发编辑冲突合并（文件交换隔离）
  - 自动 import IRI 解析（用户手动）
  - 多格式边界情形（PoC 验证后）

- [ ] **后续迭代计划**：
  - Phase 2：桌面端本体编辑
  - Phase 3：Web 端本体编辑 UI
  - Phase 4：实时协作与冲突解决

---

## 相关已有文档

以下来自前期审计与需求阶段的文档：

- [OpenProtégé 需求规格基线（SRS）](./SRS.md)：产品范围、用户、功能需求概览
- [身份/团队/项目权限 ADR](../architecture/adr/0002-identity-team-project-authorization.md)：前置权限设计
- [OpenProtégé 数据模型](../architecture/data-model.md)：表设计方案
- [Web 后端基础架构](../engineering/01-current-architecture.md)：Java 21、Spring Boot 3、PostgreSQL 17

---

## 后续步骤（用户确认后）

### 立即启动

1. **代码开发**（Phases 1-7）
   - 创建 Flyway V2 迁移
   - 实现 entity 与 repository
   - 集成 OWLAPI 5.1.20
   - 实现 service 与 controller
   - 编写单元/集成测试

2. **并行工作**
   - 准备测试语料（Pizza、Minimal 等）
   - 搭建测试环境（PostgreSQL 17 + Testcontainers）
   - 配置代码质量检查（jacoco、SonarQube 可选）

3. **每日进度**
   - 完成每个 Phase 后进行自测
   - 代码审查通过后提交（未推送）
   - 记录测试日志与验收结果

### 验收签字

完成所有 Phase 后：

1. 运行完整测试套件，记录结果
2. 执行 60+ 验收标准检查
3. 生成验收记录文档并签字
4. 向用户提交验收报告

### 后续计划

1. 用户审批验收记录
2. 合并至 main 分支并推送
3. 启动下一模块（桌面端本体编辑或 Web UI）

---

## 常见问题

### Q1：为什么选择 OWLAPI 5.1.20 而非最新版？

**状态**：当前服务的 Maven 配置固定使用 OWLAPI 5.1.20。选择该版本的完整技术决策、与当前上游版本的兼容性，以及升级风险仍需以依赖审查和构建验证为依据；本文不声称它是最新版或仍处于 LTS。

### Q2：文件 500 MiB 的限制可调整吗？

**当前事实**：配置默认上限为 500 MiB，可由 `OPENPROTEGE_ONTOLOGY_MAX_FILE_SIZE` 调整；服务拒绝超配置大小输入。达到该上限的性能、资源和安全边界未测试。本文没有独立的负责人决策记录可证明该数值属于已批准产品门槛。

### Q3：为什么不自动解析 import IRI？

**当前事实**：当前解析路径不会自动下载 `owl:imports`，并将 imports 映射到本地空文档。是否允许受限联网、由用户提供导入本体或要求编辑 import 声明仍是待决产品/安全策略；网络安全动机不等于该策略已获确认。

### Q4：往返保真测试为何允许相对 IRI 绝对化？

**状态**：这属于草案提出的比较方式，不应直接假设所有 Base IRI 场景下相对/绝对 IRI 互换均安全。需先定义 imports、ontology IRI、Base IRI 与语义比较规则，再冻结测试方法。

### Q5：代码 60% 覆盖率是否过低？

**状态**：60% 及其他覆盖率数值仅是旧草案中的未批准建议；当前仓库没有本体模块覆盖率结果，也没有已批准的覆盖率门槛。不得据此声称覆盖达到某一比例。

### Q6：何时支持其他格式（OWL/XML、JSON-LD）？

**状态**：RDF/XML 与 Turtle 是优先验证方向；是否支持 OWL/XML、Functional Syntax 或 JSON-LD 尚无已确认的交付计划。

### Q7：是否计划支持大文件流式处理？

**状态**：当前已配置 500 MiB 上传上限，但并未完成该大小下的端到端性能和资源测试。临时文件、数据库读取/写入与 OWLAPI 解析的整体内存行为须通过目标环境测量，不以配置值推断为已安全支持。

---

## 规格状态与待确认事项

下列文档分别记录需求草案、架构决策、验收条件和实现计划；文件存在不等于内容已逐项评审、链接已全面验证或验收已通过：

- [导入/导出规格](./ontology-file-import-export-spec.md)
- [本体文件验收草案](./ontology-file-import-export-acceptance-criteria.md)
- [ADR-0003](../architecture/adr/0003-ontology-file-management.md)
- [本体模块实现计划](./ontology-file-module-implementation-plan.md)
- [总体验收标准](./acceptance-criteria.md)
- [需求追踪矩阵](./traceability-matrix.csv)
- [工程证据台账](../engineering/evidence-ledger.md)
- [未完成项与后续要求](../engineering/known-gaps.md)

本仓库未记录用户对本概览中的详细往返比较规则、错误契约、全部测试语料、覆盖率/集成测试数量门槛或 12–16 天工期估算的逐项批准。因此这些内容继续作为草案或历史估算，不得作为已确认要求、当前进度、人员承诺或验收结论。产品已确认方向与待技术验证事项的区分见[SRS](./SRS.md)。

**最后复核：2026-10-11。** 所有状态仍须按新的运行证据更新；不要将文档创建、源代码存在、默认 Maven BUILD SUCCESS 或单一集成路径等同于完整验收。
