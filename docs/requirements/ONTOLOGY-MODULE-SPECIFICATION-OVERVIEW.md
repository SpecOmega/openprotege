# 本体文件导入/导出模块 — 规格概览与确认清单

**日期**：2026-10-09  
**状态**：规格定稿，待用户确认

---

## 概述

在用户确认身份/团队/项目权限模块后，本套规格为 OpenProtégé 的第二个主要模块——**本体文件导入/导出与版本管理**——定义了完整的需求、设计决策、验收标准和实现计划。

该模块聚焦于 Web 后端对本体文件生命周期的支持，是桌面端与 Web 端协作的关键基础。

---

## 核心决策摘要

| 决策项 | 决定 | 依据 |
|---|---|---|
| **OWL 目标** | OWL 2 核心规范 | 负责人确认（需求基线决策） |
| **首版格式** | RDF/XML、Turtle | 负责人确认；优先验证；后续支持 OWL/XML、Functional |
| **存储方式** | 本体二进制 → PostgreSQL BYTEA 列 | ADR-0003：事务原子性、备份简化、部署便利 |
| **版本模型** | 快照模型（每次导入新版本） | ADR-0003：简单、可恢复、天然往返保真 |
| **OWLAPI 版本** | 5.1.20 | 与 Protégé Desktop 5.6.6 对齐；稳定且广泛使用 |
| **权限策略** | 导入 Editor+、导出 Viewer+、删除 Admin+ | 与现有项目角色一致；对象级授权 |
| **往返保真** | OWL 2 语义等价 | ADR-0003：公理集、IRI、注释精确一致（允许相对化、顺序变化） |
| **测试语料** | Pizza (930 axioms)、Minimal、With-Imports、With-Annotations | 业界标准 + 边界用例 |
| **安全界限** | 文件 ≤100 MB、禁用 XXE、相对 IRI 不访问文件系统 | ADR-0003：DoS 防护、注入防护、网络安全 |
| **错误契约** | 12 种错误分类，HTTP 400/403/404/413/500 | 规格第 2.5 节；明确错误代码与消息 |
| **审计** | 记录导入/导出/删除，不记录敏感数据 | ADR-0003 第 2.8 节；支持合规审计 |

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

### 代码度量目标

| 指标 | 目标 | 验证方式 |
|---|---|---|
| 单元测试覆盖率 | ≥ 60% | `mvn jacoco:report` |
| 集成测试数量 | ≥ 25 个 | Testcontainers 测试类 |
| 单元测试通过率 | 100% | Maven BUILD SUCCESS |
| 集成测试通过率 | 100% | Testcontainers BUILD SUCCESS |
| 往返保真测试 | 4 个本体 × 多个格式组合 | OWLAPI 公理集比较 |

### 开发工期估计

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

在启动代码开发前，请逐项确认：

### 📋 需求与决策

- [ ] **格式支持**：首版 RDF/XML + Turtle；后续支持 OWL/XML、Functional、JSON-LD
  
- [ ] **OWL 目标**：OWL 2 核心规范（不涵盖 DL/RL/EL Profile 扩展）

- [ ] **往返保真定义**：OWL 2 语义等价
  - 公理集相等（OWLAPI 集合比较）
  - IRI 相等（允许相对化）
  - 注释保存（数量与关键值一致）
  - 允许的变化：相对 IRI 绝对化、格式特定语法糖移除、公理顺序变化

- [ ] **错误契约**：12 种错误代码（HTTP 400/403/404/413/500）如规格第 2.5 节所示

### 🔒 安全与权限

- [ ] **权限模型**：
  - 导入 ≥ Editor
  - 导出 ≥ Viewer
  - 删除/修改元数据 ≥ Admin/Owner

- [ ] **资源限制**：
  - 单文件 ≤ 100 MB
  - XML 递归深度 ≤ 100 层
  - 单次导入超时 ≤ 5 分钟

- [ ] **不可信输入处理**：
  - XXE 禁用（OWLAPI 5.1.20 默认配置）
  - 相对 IRI 不访问 `file://` 协议
  - 不自动解析远程 import

### 📊 测试与验收

- [ ] **测试语料**：
  - Pizza.owl + Pizza.ttl （官方，930 公理）
  - Minimal.owl （自构，~10 公理，完整元素）
  - With-Imports.ttl （自构，含相对 IRI）
  - With-Annotations.owl （自构，含 rdfs:comment/dc:*）

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

**A**：OWLAPI 当前最新版仍为 5.1.20（2019 年发布，仍在 LTS 支持中）。Protégé Desktop 5.6.6 也依赖此版本。切换更新版本需评估兼容性；本 MVP 保持一致以确保互操作。

### Q2：文件 100 MB 的限制可调整吗？

**A**：是的。规格中 100 MB 是初始建议，基于典型本体工程实践（单文件通常 <10 MB）。部署时可通过配置参数调整；超出时返回 HTTP 413。

### Q3：为什么不自动解析 import IRI？

**A**：自动网络访问引入多个风险：SSRF、外部服务可用性依赖、缓存策略复杂。MVP 要求用户显式提供导入本体或删除 import 声明。后续迭代可在网络/安全评估后考虑受限支持。

### Q4：往返保真测试为何允许相对 IRI 绝对化？

**A**：OWL 2 语义层无相对/绝对 IRI 区别。在没有 Base IRI 上下文的情况下，绝对化是安全的。测试验证的是语义等价而非字节完全一致。

### Q5：代码 60% 覆盖率是否过低？

**A**：60% 是实用的最小值，覆盖核心路径（导入、导出、权限、往返）。未覆盖项主要为错误路径与边界；实际实现中会更高（75-85% 目标）。

### Q6：何时支持其他格式（OWL/XML、JSON-LD）？

**A**：后续迭代决策。首版专注 RDF/XML + Turtle 的稳定性与往返保真。新格式需完整 PoC 与验收。

### Q7：是否计划支持大文件流式处理？

**A**：不在 MVP 范围。PostgreSQL BYTEA 列在 100 MB 级别性能可接受。若后续超过 500 MB，评估对象存储迁移。

---

## 文件清单与链接

| 文件 | 位置 | 大小 | 状态 |
|---|---|---|---|
| 需求规格 | `docs/requirements/ontology-file-import-export-spec.md` | 13 KB | ✅ 已创建 |
| 验收标准 | `docs/requirements/ontology-file-import-export-acceptance-criteria.md` | 10.7 KB | ✅ 已创建 |
| ADR-0003 | `docs/architecture/adr/0003-ontology-file-management.md` | 7.8 KB | ✅ 已创建 |
| 实现计划 | `docs/requirements/ontology-file-module-implementation-plan.md` | 16.6 KB | ✅ 已创建 |
| 本规格概览 | `docs/requirements/ontology-file-module-specification-overview.md` | 本文件 | ✅ 已创建 |
| SRS 更新 | `docs/requirements/SRS.md` | 更新部分 | ✅ 已更新 |

**总计规格文档**：~60 KB，内容完整、链接无缺

---

## 最后确认

所有规格文档已生成、链接验证、内容一致。

**请确认**：

1. ✅ 已阅读上述 5 份文档或摘要
2. ✅ 同意核心决策（格式、权限、往返保真、错误契约）
3. ✅ 确认测试语料与验收标准
4. ✅ 接受开发工期估计（12-16 天）
5. ✅ 准备启动代码实现

---

**确认方式**：请回复以下确认，例如：

```
✅ 确认所有规格文档
✅ 同意核心决策
✅ 确认验收标准
✅ 可以启动代码开发
```

收到确认后，立即启动 Phase 1 数据库与模型实现。

---

**文档生成日期**：2026-10-09  
**最后更新**：2026-10-09  
**状态**：✅ **规格完成，待用户确认**
