# OpenProtégé 本体文件导入/导出模块规格

**版本**：0.1-draft
**日期**：2026-10-09
**状态**：需求定义与设计规格
**审核状态**：待确认

---

## 1. 模块概述

### 1.1 目标

基于 OWLAPI 5.x 库，为 OpenProtégé Web 后端实现本体文件的：
- 格式识别与验证
- 导入与结构校验
- 导出与往返保真
- 版本跟踪与审计

首版范围严格限制在 **OWL 2** 核心规范和 **RDF/XML、Turtle** 两种序列化格式，建立安全边界和测试保真标准。

### 1.2 模块输入与输出

**输入**：
- 用户上传的本体文件（OWL/RDF/Turtle）
- Web 项目元数据（项目 ID、导入用户、权限）
- 格式显式指定或自动识别选项

**输出**：
- 导入成功 → 本体存储为项目版本，记录审计
- 导入失败 → 明确错误分类与定位，回滚版本
- 导出成功 → 用户可下载指定格式的本体文件
- 往返验证 → 导入→编辑→导出后再导入，公理集与 IRI 精确一致

### 1.3 依赖与技术栈

```
Java 21、Spring Boot 3、PostgreSQL 17
OWLAPI 5.1.20（与 Protégé Desktop/WebProtégé 兼容）
Testcontainers 1.21.3、Flyway 10.x
JUnit 5.x、AssertJ
```

---

## 2. 需求详细规格

### 2.1 格式支持与识别

| 格式 | MIME 类型 | 扩展名 | 优先级 | 识别方式 | 状态 |
|---|---|---|---|---|---|
| RDF/XML | `application/rdf+xml` | `.owl` `.rdf` `.xml` | MUST | OWLAPI 自动或用户指定 | 0.1-draft |
| Turtle | `text/turtle` | `.ttl` | MUST | OWLAPI 自动或用户指定 | 0.1-draft |
| OWL/XML | `application/owl+xml` | `.owx` | SHOULD | OWLAPI 自动或用户指定 | TBD |
| Functional OWL | `application/owl-functional` | `.ofn` | SHOULD | OWLAPI 自动或用户指定 | TBD |
| JSON-LD | `application/ld+json` | `.jsonld` | SHOULD | OWLAPI（可选） | TBD |

**识别规则**：
1. 优先级顺序：用户指定 > 文件扩展名 + 内容检测 > 纯内容嗅探
2. 文件扩展名与内容不符时，返回警告，让用户确认
3. 无法识别的格式返回 `UnsupportedFormatError`

### 2.2 导入流程

```
1. 接收文件与项目上下文
   ↓
2. 文件格式识别与 MIME 检查
   ↓ [失败] → 返回 FormatNotRecognizedException
   ↓ [成功]
3. 文件大小检查（见 2.6）
   ↓ [失败] → FileSizeExceededException
   ↓ [成功]
4. OWLAPI OntologyManager 解析
   ↓ [失败] → OntologyParsingException（记录行列号）
   ↓ [成功]
5. 本体结构一致性与约束验证
   ↓ [失败] → OntologyValidationException（记录冲突细节）
   ↓ [成功]
6. 权限检查（当前用户有项目 Editor/Admin/Owner 权限）
   ↓ [失败] → AuthorizationException（权限被 ProjectService 检查）
   ↓ [成功]
7. 数据库事务：
   - 创建新版本记录（OntologyVersion）
   - 存储本体元数据与内容
   - 添加审计日志
   ↓ [失败] → StorageException
   ↓ [成功]
8. 返回导入结果与版本 ID
```

### 2.3 导出流程

```
1. 接收项目 ID、版本 ID、目标格式、请求用户上下文
   ↓
2. 权限检查（Viewer+ 能读取项目）
   ↓ [失败] → AuthorizationException
   ↓ [成功]
3. 从数据库加载本体版本内容
   ↓ [失败] → VersionNotFoundException
   ↓ [成功]
4. OWLAPI 反序列化加载本体
   ↓ [失败] → OntologyLoadException
   ↓ [成功]
5. 序列化为目标格式
   ↓ [失败] → FormatConversionException
   ↓ [成功]
6. 准备 HTTP 响应（Content-Disposition、Cache-Control）
7. 返回文件二进制流
```

### 2.4 往返保真规则（Round-Trip Fidelity）

**定义**：导入某格式的本体 F1 → 存储 → 导出为格式 F2 → 再导入 → 公理集、IRI、注释与 imports 精确一致。

**验收标准**：
- 同一格式往返（RDF/XML → RDF/XML）：公理集、IRI、注释、imports 100% 相同（OWL2 语义等价）
- 跨格式往返（RDF/XML → Turtle → RDF/XML）：所有公理、IRI、注释保存（可视为 OWL2 抽象句法的往返）
- 允许的变化：
  - IRI 的绝对化（相对 → 绝对）
  - 格式特定的语法糖移除（如 XML 命名空间优化）
  - 公理顺序变化（OWL2 集合语义，不涉及顺序）

**测试语料**：
- 官方 Pizza 本体（930 个公理、多格式）
- 自构 minimal OWL 2 本体（类、属性、个体、约束）
- 自构包含 import 的本体与相对 IRI
- 自构带注释与 annotation 的本体

**往返验证方法**：
```java
OWLOntology original = loadOntology(file1, format1);
byte[] exported = exportOntology(original, format2);
OWLOntology reimported = loadOntology(new ByteArrayInputStream(exported), format2);
// 比较：
assertAxiomSetsEqual(original.getAxioms(), reimported.getAxioms());
assertIRIsEqual(original.getOntologyID(), reimported.getOntologyID());
assertAnnotationsEqual(original.getAnnotations(), reimported.getAnnotations());
```

### 2.5 错误契约与分类

**错误分类表**：

| 错误代码 | HTTP 状态 | 消息 | 恢复建议 |
|---|---|---|---|
| `FORMAT_NOT_RECOGNIZED` | 400 | "无法识别文件格式。请指定格式或使用已支持的扩展名。" | 用户指定或修改文件 |
| `FORMAT_NOT_SUPPORTED` | 400 | "不支持的格式 {format}。支持的格式：RDF/XML、Turtle。" | 转换文件格式或等待功能支持 |
| `FILE_SIZE_EXCEEDED` | 413 | "文件大小 {actual} 超过限制 {max}。" | 分割文件或压缩本体 |
| `PARSING_ERROR` | 400 | "本体文件解析失败（第 {line} 行）：{detail}。" | 修复语法错误 |
| `VALIDATION_ERROR` | 400 | "本体结构不满足 OWL 2 约束：{detail}。" | 修复模型错误或改用 OWL 2 RL/EL 约束 |
| `UNRESOLVED_IMPORT` | 400 | "导入失败：{iri}。本模块暂不支持自动解析外部 IRI。" | 手动获取导入或移除导入声明 |
| `AUTHORIZATION_ERROR` | 403 | "无权导入/导出此项目。" | 请求项目管理员授予权限 |
| `PROJECT_NOT_FOUND` | 404 | "项目 {id} 不存在或已删除。" | 检查项目 ID |
| `VERSION_NOT_FOUND` | 404 | "版本 {id} 不存在。" | 检查版本历史或恢复 |
| `STORAGE_ERROR` | 500 | "存储本体失败。" | 联系管理员检查数据库 |
| `FORMAT_CONVERSION_ERROR` | 500 | "格式转换失败：{detail}。" | 重试或联系支持 |

### 2.6 安全与资源限制

**文件大小限制**：
- 单文件最大 100 MB（可配置，建议上限）
- 理由：防止 DoS；本体工程实践中，单文件通常 <10 MB

**解析资源限制**：
- XML 递归深度：最大 100 层（OWLAPI 默认配置）
- 公理数量：对应大小限制后的合理上界
- 处理超时：单次导入 5 分钟（后续 async 处理可调整）

**不可信输入处理**：
- 禁用 XML 外部实体（XXE）处理
  - OWLAPI 5.x 自身不启用 XXE；确认配置后补充安全测试
- 禁用 SPARQL/Reasoner 远程评估
  - 本模块不使用推理；后续评估词汇一致性检查前需确认
- 相对 IRI 不允许指向系统路径
  - OWLAPI 将 IRI 视为 URI，不解析本地路径；但校验器应拒绝 `file://` 或 `/etc/passwd` 式导入

**访问控制**：
- 导入：要求 Editor/Admin/Owner 权限（ProjectService 检查）
- 导出：要求 Viewer+ 权限
- 删除版本：要求 Admin/Owner 权限
- 修改元数据：要求 Admin/Owner 权限

---

## 3. 数据模型扩展

### 3.1 新增表（Flyway 迁移）

#### `ontology_versions`
```sql
CREATE TABLE ontology_versions (
  id VARCHAR(36) PRIMARY KEY,
  project_id VARCHAR(36) NOT NULL,
  format VARCHAR(50) NOT NULL,  -- 'RDF/XML', 'Turtle', ...
  content BYTEA NOT NULL,        -- 本体文件二进制内容
  metadata_json JSONB NOT NULL,  -- {iri, namespace, axiomCount, ...}
  created_by VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE,
  FOREIGN KEY(created_by) REFERENCES users(email) ON DELETE RESTRICT,
  INDEX idx_project_created(project_id, created_at DESC)
);
```

#### `ontology_version_tags`（版本标签与分支）
```sql
CREATE TABLE ontology_version_tags (
  id VARCHAR(36) PRIMARY KEY,
  version_id VARCHAR(36) NOT NULL,
  tag_name VARCHAR(255) NOT NULL,  -- e.g., 'v1.0', 'release'
  created_by VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(version_id) REFERENCES ontology_versions(id) ON DELETE CASCADE,
  UNIQUE(version_id, tag_name)
);
```

#### `ontology_audit_logs`（导入/导出审计）
```sql
CREATE TABLE ontology_audit_logs (
  id VARCHAR(36) PRIMARY KEY,
  project_id VARCHAR(36) NOT NULL,
  action VARCHAR(50) NOT NULL,  -- 'IMPORT', 'EXPORT', 'DELETE', 'UPDATE_METADATA'
  actor_email VARCHAR(255) NOT NULL,
  version_id VARCHAR(36),
  file_name VARCHAR(255),
  file_size_bytes BIGINT,
  format VARCHAR(50),
  result VARCHAR(50) NOT NULL,  -- 'SUCCESS', 'FAILED', 'PARTIAL'
  error_message TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE,
  FOREIGN KEY(actor_email) REFERENCES users(email) ON DELETE RESTRICT,
  INDEX idx_project_action(project_id, action, created_at DESC)
);
```

### 3.2 项目表扩展

向 `projects` 表新增字段：
```sql
ALTER TABLE projects ADD COLUMN (
  current_ontology_version_id VARCHAR(36) DEFAULT NULL,
  ontology_namespace VARCHAR(255),  -- 首版本的 ontology IRI
  FOREIGN KEY(current_ontology_version_id) REFERENCES ontology_versions(id)
);
```

---

## 4. API 规格

### 4.1 导入本体

**端点**：`POST /api/projects/{projectId}/ontologies/import`

**请求头**：
- `Authorization: Bearer <session_token>` 或 Cookie 中 session
- `Content-Type: multipart/form-data`

**请求体**：
```
multipart:
  - file: <本体文件二进制>（multipart field）
  - format: "RDF/XML" | "Turtle" | "" （可选，不指定则自动识别）
  - overwrite: true | false （可选，默认 false；false 则新建版本）
```

**响应成功（200）**：
```json
{
  "status": "success",
  "projectId": "proj-123",
  "versionId": "ver-456",
  "format": "RDF/XML",
  "ontologyIRI": "http://example.com/pizza",
  "axiomCount": 930,
  "importedAt": "2026-10-09T14:30:00Z",
  "message": "本体导入成功。"
}
```

**响应失败（400/403/413/500）**：
```json
{
  "status": "error",
  "errorCode": "PARSING_ERROR",
  "message": "本体文件解析失败（第 42 行）：unexpected tag 'rdf:RDF'。",
  "timestamp": "2026-10-09T14:30:00Z"
}
```

### 4.2 导出本体

**端点**：`GET /api/projects/{projectId}/ontologies/versions/{versionId}/export`

**查询参数**：
- `format`: "RDF/XML" | "Turtle" | ...（默认原格式）

**响应成功（200）**：
```
Content-Type: application/rdf+xml
Content-Disposition: attachment; filename="pizza-v1.owl"
Content-Length: <文件大小>
Cache-Control: public, max-age=3600

<本体文件二进制流>
```

**响应失败**：
同导入错误契约。

### 4.3 获取项目本体版本列表

**端点**：`GET /api/projects/{projectId}/ontologies/versions`

**查询参数**：
- `pageNum`: 页码（默认 1）
- `pageSize`: 每页数量（默认 10，最大 100）
- `sortBy`: "created_at" | "axiomCount"（默认 created_at DESC）

**响应（200）**：
```json
{
  "versions": [
    {
      "id": "ver-456",
      "format": "RDF/XML",
      "ontologyIRI": "http://example.com/pizza",
      "axiomCount": 930,
      "fileSizeBytes": 123456,
      "importedBy": "user@example.com",
      "createdAt": "2026-10-09T14:30:00Z",
      "tags": ["v1.0", "release"]
    }
  ],
  "total": 42,
  "pageNum": 1,
  "pageSize": 10
}
```

### 4.4 获取版本元数据

**端点**：`GET /api/projects/{projectId}/ontologies/versions/{versionId}`

**响应（200）**：
```json
{
  "versionId": "ver-456",
  "format": "RDF/XML",
  "ontologyIRI": "http://example.com/pizza",
  "metadata": {
    "axiomCount": 930,
    "classCount": 98,
    "objectPropertyCount": 42,
    "dataPropertyCount": 28,
    "individualsCount": 50,
    "imports": [
      "http://www.w3.org/2002/07/owl#"
    ],
    "annotations": {
      "dc:creator": "Pizza Ontology Team",
      "dc:description": "..."
    }
  },
  "fileSizeBytes": 123456,
  "importedBy": "user@example.com",
  "createdAt": "2026-10-09T14:30:00Z"
}
```

### 4.5 删除版本

**端点**：`DELETE /api/projects/{projectId}/ontologies/versions/{versionId}`

**权限**：Admin/Owner

**响应（204）**：无内容

**响应失败**：
- 403：无权限
- 404：版本不存在
- 400：不能删除当前版本

---

## 5. 安全考虑

### 5.1 对象级访问控制

- 导入/导出：通过 `ProjectService.checkProjectAccess()` 验证用户对项目的角色权限
- 版本修改/删除：限制 Admin/Owner
- 审计：所有操作记录用户、IP（后续补充）、操作类型

### 5.2 不可信内容处理

| 威胁 | 防御措施 | 验证状态 |
|---|---|---|
| XXE 注入 | OWLAPI 5.x 默认禁用；显式检查 XML 解析器配置 | TBD 安全测试 |
| 恶意 IRI/导入指向 | 接受相对 IRI；禁用 `file://` 协议；不自动解析远程 import | SOURCE-OBSERVED |
| 文件炸弹 | 100 MB 大小限制 | PROPOSED |
| 无限循环导入 | 不支持自动导入；用户手动；检查循环标记（后续）| TBD |
| RDF 外部实体解析 | 使用安全的 Turtle/RDF/XML 解析器；检查 OWLAPI 文档 | TBD 安全测试 |

### 5.3 数据隐私

- 本体文件存储于数据库，应用与数据库间使用 TLS（生产部署要求）
- 不在日志中记录本体内容；只记录操作类型、用户、大小、格式、成功/失败
- 删除项目时，其所有版本应级联删除

---

## 6. 验收标准

### 6.1 功能验收

| ID | 验收条件 | 测试方法 |
|---|---|---|
| AC-ONT-001 | 系统识别并拒绝不支持的格式 | 上传 JSON 文件，验证返回 `FORMAT_NOT_SUPPORTED` |
| AC-ONT-002 | 系统导入 RDF/XML Pizza 本体，930 个公理完整保存 | 导入 → 查询 axiomCount，期望 930 |
| AC-ONT-003 | 系统导入 Turtle 本体，IRI 与注释保存 | 导入 → 导出为 Turtle → 比对公理与注释 |
| AC-ONT-004 | 同格式往返保真：RDF/XML → 导出 → 再导入，公理集 100% 一致 | 导入 Pizza → 导出 RDF/XML → 再导入 → assertAxiomSetsEqual |
| AC-ONT-005 | 权限检查：Viewer 不能导入；Editor 能导入；Admin 能删除版本 | 分别用 Viewer/Editor/Admin 账户测试 |
| AC-ONT-006 | 大小限制：>100 MB 的文件返回 `FILE_SIZE_EXCEEDED` | 创建 101 MB 文件，上传，验证错误 |
| AC-ONT-007 | 错误定位：解析失败时返回行列号 | 上传包含语法错误的 RDF/XML，验证错误消息 |
| AC-ONT-008 | 版本审计：所有导入/导出/删除操作记录于审计表，包括用户、时间、结果 | 导入 3 个版本，查询 ontology_audit_logs，验证 3 条记录 |

### 6.2 安全验收

| ID | 验收条件 | 测试方法 |
|---|---|---|
| AC-SEC-001 | 私有项目的本体版本只有项目成员可导出 | 非项目成员访问导出端点，验证 403 |
| AC-SEC-002 | 不能通过 `..%2F..` 等路径穿越上传到项目外 | 上传文件并验证只存储于指定项目 |
| AC-SEC-003 | XML 外部实体解析被禁用；恶意 XXE 不执行 | 上传包含 XXE payload 的 RDF/XML，验证解析不执行 |
| AC-SEC-004 | 删除项目时，其本体版本级联删除；不遗留数据库孤儿记录 | 删除项目 → 查询 ontology_versions，期望行数为 0 |

### 6.3 保真验证

| ID | 测试场景 | 期望结果 |
|---|---|---|
| AC-FID-001 | Pizza 930 公理往返 | 公理集、IRI、导入精确一致 |
| AC-FID-002 | 自构包含相对 IRI 的本体往返 | 相对 IRI 保存或绝对化后保存（不改变语义） |
| AC-FID-003 | 带 annotation 的本体往返 | annotation 完整保存 |
| AC-FID-004 | RDF/XML ↔ Turtle ↔ RDF/XML 多格式往返 | 公理集与 IRI 保持 |

---

## 7. 模块实现清单

### 第一阶段（本轮）

- [ ] Flyway 迁移脚本（本体版本、审计表）
- [ ] 数据模型类（OntologyVersion、OntologyAuditLog）
- [ ] OntologyService 核心实现
  - [ ] 格式识别与验证
  - [ ] 导入核心流程（文件→OWLAPI→验证→存储）
  - [ ] 导出流程（查询→OWLAPI→序列化）
- [ ] OntologyController REST 端点
- [ ] 单元测试（至少 60% 代码覆盖）
- [ ] Testcontainers 集成测试（导入/导出/权限/往返基本路径）
- [ ] Pizza 本体导入→导出→再导入保真测试
- [ ] 安全测试（权限、大小限制、XXE 禁用检查）

### 第二阶段（后续迭代）

- [ ] 更多 OWL 2 Profile 支持（DL/RL/EL）
- [ ] 本体推理与词汇一致性检查
- [ ] 递增式导入与冲突检测
- [ ] 版本差异与合并
- [ ] 支持更多格式（OWL/XML、Functional、JSON-LD）
- [ ] 桌面端与 Web 端的实时协作同步
- [ ] 导入进度条与后台任务

---

## 8. 已知限制与待验证项

| 项目 | 状态 | 原因 | 后续行动 |
|---|---|---|---|
| 自动解析 import IRI | UNVERIFIED | 需网络访问；MVP 不支持 | 用户手动获取或移除 import |
| OWL 推理与一致性检查 | UNVERIFIED | 添加 HermiT/Pellet 库 | 后续迭代评估 |
| 并发编辑冲突合并 | UNVERIFIED | 设计与实现复杂度高 | 先以文件交换隔离；后续需完整 CRDt 或版本控制 |
| 多格式综合往返 | SOURCE-OBSERVED | Pizza PoC 验证 RDF/XML/Turtle 单向 | 本轮完成往返测试 |
| 完整依赖许可证与兼容性 | UNVERIFIED | OWLAPI 本身及传递依赖 | 法务评估 Apache-2.0 兼容性 |

---

## 9. 检验清单

完成本模块前需逐项确认：

- [ ] 用户确认需求规格（本文）
- [ ] 数据模型与数据库迁移已审批
- [ ] API 接口已审批
- [ ] 安全与访问控制方案已审批
- [ ] 往返保真测试语料与验收标准已审批
- [ ] 代码实现完成且代码审查通过
- [ ] 单元测试 ≥ 60% 覆盖率
- [ ] Testcontainers 集成测试通过（≥8 个测试）
- [ ] 往返保真测试通过（Pizza 与自构本体）
- [ ] 安全测试通过（权限、大小限制、XXE 检查）
- [ ] 代码与文档已提交且未推送
- [ ] 用户评审与确认

---

## 10. 参考资源

### 文档

- [OWL 2 Web Ontology Language Document Overview](https://www.w3.org/TR/owl2-overview/)
- [Turtle RDF Serialization](https://www.w3.org/TR/turtle/)
- [RDF/XML Syntax Specification](https://www.w3.org/TR/rdf-xml/)

### 库与工具

- [OWLAPI GitHub](https://github.com/owlcs/owlapi)
- [OWLAPI JavaDoc](https://github.com/owlcs/owlapi/wiki)
- [Pizza Ontology](https://protege.stanford.edu/ontologies/pizza/pizza.owl)

### 相关规格

- [OpenProtégé SRS](./SRS.md)
- [OpenProtégé 数据模型](../architecture/data-model.md)
- [项目权限与访问控制 ADR](../architecture/adr/0002-identity-team-project-authorization.md)

---

**下一步**：用户确认本规格后，开始第一阶段代码实现。
