# ADR-0003: 本体文件导入/导出与版本管理

**状态**：Proposed  
**决策日期**：2026-10-09  
**更新日期**：2026-10-09  

---

## 1. 背景与问题

OpenProtégé 需要支持本体文件的导入、导出、版本管理和往返保真。关键决策包括：

1. **存储策略**：本体内容在何处存储，如何管理版本历史
2. **OWLAPI 集成**：与 Protégé Desktop/WebProtégé 的兼容性
3. **并发与冲突**：多个用户同时编辑本体时如何处理
4. **权限与审计**：谁可以导入/导出，如何追踪操作历史

## 2. 决策

### 2.1 本体存储设计

**选择**：直接存储本体文件二进制内容（BYTEA）于数据库，不采用外部文件系统或对象存储。

**理由**：

- **备份与一致性**：本体与项目元数据同库，事务原子性强；异地备份只需数据库备份
- **权限控制**：文件访问与数据库权限统一
- **部署简化**：自托管用户无需额外 S3/GCS 配置；Compose 只需 PostgreSQL 卷
- **初期适用**：单机 PostgreSQL 的存储足够 MVP（几十 GB 数据库）

**权衡与限制**：

- 数据库膨胀：>50 GB 本体存储后需评估迁移至对象存储
- 导出延迟：直接从二进制反序列化，延迟较小但随文件增大而增加
- 后续优化：可设计分层存储（冷本体 → 对象存储）；本 ADR 不涉及

**验收**：
- Testcontainers 集成测试覆盖二进制存储与检索（AC-STR-001）
- 支持至少 500 MB 本体文件的导入/导出（AC-STR-002）

---

### 2.2 版本管理模型

**选择**：采用"版本快照"模型，每次导入或修改都创建新版本记录，而不是增量/差异存储。

**模型描述**：

```
Project
├── OntologyVersion v1 (RDF/XML, created 2026-10-09 10:00)
│   ├── content (BYTEA)
│   ├── metadata (axiomCount, iri, ...)
│   └── created_by: user@example.com
├── OntologyVersion v2 (Turtle, created 2026-10-09 11:00)
│   └── ...
└── current_ontology_version_id → v2
```

**为何选择快照**：

- **简单性**：每个版本是独立、完整、可恢复的单位
- **往返保真**：无需重放更改日志；直接导出即可获得当时状态
- **兼容性**：OWLAPI 天然支持完整本体加载
- **恢复便利**：用户可直接恢复任意历史版本

**权衡**：

- **存储成本**：相同部分重复存储；可在后续迭代优化为差异存储
- **合并复杂性**：多用户编辑时需显式合并或冲突检测；MVP 通过文件交换隔离

**接口**：

```java
OntologyVersion createVersion(ProjectId, OWLOntology, Format, UserId);
OntologyVersion getVersion(VersionId);
List<OntologyVersion> listVersions(ProjectId, pagination);
void deleteVersion(VersionId, AdminUserContext);
OWLOntology loadOntology(VersionId);
```

**验收**：
- Testcontainers 测试验证创建、查询、删除版本（AC-VER-001）
- 恢复历史版本后内容完全相同（AC-VER-002）

---

### 2.3 OWLAPI 版本与兼容性

**选择**：使用 OWLAPI 5.1.20（与 Protégé Desktop 5.6.6 对齐）。

**理由**：

- Protégé Desktop HEAD（e-26 时点）依赖 OWLAPI 5.1.20
- 稳定且广泛使用；相对于 4.x 有更好的 OWL 2 支持
- 库自身不依赖容易冲突的重型框架（如 Jena 或 Eclipse RDF4J）

**依赖声明**（pom.xml）：

```xml
<dependency>
    <groupId>net.sourceforge.owlapi</groupId>
    <artifactId>owlapi-core</artifactId>
    <version>5.1.20</version>
</dependency>
<dependency>
    <groupId>net.sourceforge.owlapi</groupId>
    <artifactId>owlapi-parsers</artifactId>
    <version>5.1.20</version>
</dependency>
```

**不依赖项**：
- HermiT/Pellet 推理机（后续评估；MVP 不需要）
- 自定义注解处理器（OWLAPI 内置支持）

**验收**：
- PoC：OWLAPI 5.1.20 对 Pizza 本体的往返验证已完成（E-24，见 evidence-ledger.md）
- 集成测试：使用 OWLAPI 5.1.20 完成 Pizza 往返测试（AC-OWLAPI-001）

---

### 2.4 格式支持与识别优先级

**选择**：

1. **首版必须格式**：RDF/XML、Turtle
2. **可选格式**（后续）：OWL/XML、Functional OWL、JSON-LD
3. **识别优先级**：用户指定 > 文件头 + 文件扩展名 > 纯内容嗅探

**理由**：

- RDF/XML 与 Turtle 是 Protégé 标准导入/导出格式
- MIME 类型不可信（用户上传）；优先用户指定
- 文件扩展名 + 内容头可覆盖 90%+ 使用场景
- 不支持的格式显式拒绝（返回 400 + 支持格式列表）

**格式识别算法**：

```
if user specifies format:
  use user's format
else:
  if file extension in supported_extensions:
    try parse with that format
  if parse fails or no extension:
    try parse with [RDF/XML, Turtle, OWL/XML] in order
    if all fail:
      return FORMAT_NOT_RECOGNIZED with suggestions
```

**验收**：
- 测试用户指定格式优先级（AC-FMT-001）
- 测试自动识别 RDF/XML 与 Turtle（AC-FMT-002）
- 测试不支持格式返回 400（AC-FMT-003）

---

### 2.5 权限模型

**选择**：
- 导入：Editor+ 权限
- 导出：Viewer+ 权限
- 删除版本/修改元数据：Admin/Owner 权限
- 权限检查：通过现有 `ProjectService.checkProjectAccess()` 或新增 `OntologyService.checkOntologyAccess()`

**理由**：

- 与现有项目角色设计一致
- 写操作（导入、删除）限制 Editor+，保护项目
- 读操作（导出）允许 Viewer，支持只读团队成员查阅

**实现**：

```java
public OntologyVersion importOntology(ProjectId, UserId, OWLOntology, Format) 
  throws AuthorizationException {
  projectService.requireProjectRole(projectId, userId, Role.Editor);
  // proceed with import
}

public byte[] exportOntology(VersionId, UserId, Format)
  throws AuthorizationException {
  projectService.requireProjectRole(projectId, userId, Role.Viewer);
  // proceed with export
}
```

**验收**：
- Testcontainers 测试权限检查（Viewer 不能导入，Editor 能，Admin 能删除）（AC-PERM-001）

---

### 2.6 往返保真定义与验收

**选择**：采用"OWL 2 抽象句法等价"作为往返保真的定义。

**等价性标准**：

1. **公理集相等**：所有 OWL 2 公理（除去顺序与句法差异）语义等价
2. **IRI 相等**：本体 IRI、命名空间 IRI 与所有实体 IRI 相同
3. **注释保存**：annotation 与 annotation 属性保存（可能格式变化）
4. **导入声明保存**：所有 `owl:imports` 保存

**允许的变化**：

- 相对 IRI 可绝对化（e.g., `#Class1` → `http://example.com/ont#Class1`）
- 格式特定语法糖移除（e.g., XML 命名空间前缀优化）
- 公理顺序改变
- 注释值的文本规范化（空白）

**不允许的变化**：

- 丢失或修改任何公理
- 改变 IRI（除相对化）
- 丢失注释

**测试方法**：

```java
OWLOntology original = loadOntology(file1, format1);
OWLOntology exported_reimported = 
  loadOntology(export(original, format2), format2);

// 断言公理集相等（由 OWLAPI equals 判定）
assertEquals(original.getAxioms(), exported_reimported.getAxioms());

// 断言 IRI 相等
assertEquals(original.getOntologyID(), exported_reimported.getOntologyID());

// 注释数量相等（内容需手工审查或语义比对）
assertEquals(original.getAnnotations().size(), 
             exported_reimported.getAnnotations().size());
```

**测试语料**：

| 本体 | 来源 | 大小 | 格式 | 用途 |
|---|---|---|---|---|
| Pizza | 官方 | ~120 KB | RDF/XML | 实际工程本体；930 公理 |
| Minimal | 自构 | ~2 KB | RDF/XML | 最小化本体；测试完整元素 |
| With-Imports | 自构 | ~5 KB | Turtle | 包含相对 import；测试 IRI 处理 |
| With-Annotations | 自构 | ~3 KB | RDF/XML | 带 annotation；测试注释保存 |

**验收**：
- Pizza 往返 RDF/XML → Turtle → RDF/XML，公理集 100% 一致（AC-FID-001）
- Minimal 往返多个格式，所有元素（类、属性、个体、约束）保存（AC-FID-002）
- With-Imports 往返，相对 IRI 正确保存或绝对化（AC-FID-003）
- With-Annotations 往返，注释数量与关键注释值保存（AC-FID-004）

---

### 2.7 错误恢复与事务一致性

**选择**：
- 单个导入/导出操作为原子事务
- 导入失败时不创建版本记录；回滚所有数据库修改
- 导出失败时不修改项目状态

**实现**：

```java
@Transactional(rollbackFor = Exception.class)
public OntologyVersion importOntology(...) {
  File temp = downloadToTemp(uploadedFile);
  try {
    OWLOntology ont = parseOntology(temp);
    validateOntology(ont);
    Version ver = createVersionRecord(ont, format, ...);
    updateProjectCurrentVersion(ver);
    logAuditEvent(IMPORT, SUCCESS, ...);
    return ver;
  } catch (Exception e) {
    logAuditEvent(IMPORT, FAILED, e.getMessage());
    throw new OntologyImportException(e);
  } finally {
    deleteTemp(temp);
  }
}
```

**验收**：
- 导入失败后，项目本体版本列表不增加新条目（AC-TXN-001）
- 导出失败后，客户端不接收损坏文件（AC-TXN-002）

---

### 2.8 审计日志设计

**选择**：记录所有本体操作（导入、导出、删除、元数据修改）于独立审计表。

**记录字段**：

```sql
ontology_audit_logs:
  - id (UUID)
  - project_id
  - action ('IMPORT', 'EXPORT', 'DELETE', 'UPDATE_METADATA')
  - actor_email
  - version_id (可选，export 时为 null 或填充)
  - file_name
  - file_size_bytes
  - format
  - result ('SUCCESS', 'FAILED', 'PARTIAL')
  - error_message (仅失败时填充)
  - created_at
```

**不记录项**：
- 本体文件内容或摘要（太大；避免敏感数据日志）
- 用户会话 ID 或 IP（后续补充）
- 密码或 token

**验收**：
- 导入成功后记录审计条目，action='IMPORT'，result='SUCCESS'（AC-AUD-001）
- 导入失败后记录审计条目，action='IMPORT'，result='FAILED'，error_message 填充（AC-AUD-002）
- 审计表中没有敏感数据泄漏（AC-AUD-003）

---

## 3. 后续决策留白

| 项目 | 当前状态 | 触发条件 | 预期决策时间 |
|---|---|---|---|
| 多用户并发编辑冲突检测 | UNVERIFIED | 多用户同时编辑同项目 | 第 2 阶段评估后 |
| 版本差异与智能合并 | PROPOSED | 三路合并需求 | 第 2 阶段 |
| 增量/差异存储优化 | PROPOSED | 本体数据库 > 50 GB | 容量评估后 |
| 完整 OWL 推理与验证 | PROPOSED | 需要一致性检查或前向推理 | 需求确认后 |
| 外部 import IRI 自动解析 | UNVERIFIED | 大规模导入本体库场景 | 网络/安全评估后 |
| 对象存储迁移（S3/GCS）| PROPOSED | 自托管存储超限 | 容量评估与成本分析后 |

---

## 4. 符合性检查表

完成实现前，逐项验证：

- [ ] 数据库迁移脚本创建并通过 Flyway 验证
- [ ] OntologyVersion/OntologyAuditLog 模型类实现
- [ ] OntologyService 实现至少 60% 代码覆盖
- [ ] OntologyController 实现所有 REST 端点
- [ ] OWLAPI 5.1.20 导入/导出流程测试通过
- [ ] Pizza 往返测试通过（公理集与 IRI 100% 一致）
- [ ] Minimal/With-Imports/With-Annotations 往返测试通过
- [ ] 权限检查测试通过（Viewer/Editor/Admin 矩阵）
- [ ] 大小限制测试通过（100 MB+）
- [ ] 错误恢复与事务一致性测试通过
- [ ] 审计日志完整记录所有操作
- [ ] 文档与 ADR 已更新

---

## 5. 参考

- [OWL 2 Web Ontology Language Document Overview](https://www.w3.org/TR/owl2-overview/)
- [OWLAPI 5.1.20 Changelog](https://github.com/owlcs/owlapi/releases/tag/5.1.20)
- [Protégé Desktop 5.6.6 Release](https://github.com/protegeproject/protege/releases/tag/v5.6.6)
- 本项目 ADR-0002（身份、团队、项目权限）
- 本项目数据模型文档（data-model.md）

---

**决策负责人**：架构师  
**最后审查日期**：2026-10-09
