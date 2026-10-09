# 本体文件导入/导出模块验收标准

**版本**：0.1-draft  
**日期**：2026-10-09  
**关联规格**：[导入/导出规格](./ontology-file-import-export-spec.md)、[ADR-0003](../architecture/adr/0003-ontology-file-management.md)

---

## 1. 功能验收标准

### 1.1 格式识别与拒绝

| AC ID | 验收条件 | 测试用例 | 期望结果 | 验收方法 |
|---|---|---|---|---|
| AC-FMT-001 | 用户显式指定格式时，优先使用指定格式而忽略文件扩展名 | 上传 `pizza.json` 但指定 format="RDF/XML" | 系统尝试按 RDF/XML 解析；若文件非 RDF/XML 则返回 PARSING_ERROR，而非 FORMAT_NOT_RECOGNIZED | POST /api/projects/{id}/ontologies/import，验证错误代码 |
| AC-FMT-002 | 未指定格式时，优先使用文件扩展名识别 | 上传 `pizza.owl` (RDF/XML 内容) | 自动识别为 RDF/XML；解析成功 | 导入成功，验证 format 字段为 "RDF/XML" |
| AC-FMT-003 | 自动识别支持格式（RDF/XML、Turtle） | 上传无扩展名的 Turtle 内容 | 按内容头尝试 RDF/XML、再试 Turtle；成功识别为 Turtle | 导入成功，验证 format 字段为 "Turtle" |
| AC-FMT-004 | 不支持的格式拒绝 | 上传 `pizza.jsonld` (JSON-LD 内容，未来支持但当前不支持) | 返回 HTTP 400 + `FORMAT_NOT_SUPPORTED` | 检查 HTTP 400、errorCode="FORMAT_NOT_SUPPORTED"、错误消息包含支持格式列表 |
| AC-FMT-005 | 格式不一致时返回警告 | 上传 `pizza.owl` 但内容实际为 Turtle | 若指定 format="RDF/XML"，解析失败返回 PARSING_ERROR；若未指定，自动识别为 Turtle | 验证错误代码与消息 |

### 1.2 导入流程与权限

| AC ID | 验收条件 | 测试用例 | 期望结果 | 验收方法 |
|---|---|---|---|---|
| AC-IMP-001 | Viewer 不能导入本体 | Viewer 用户上传 pizza.owl 至其所属项目 | 返回 HTTP 403 + `AUTHORIZATION_ERROR` | 检查错误响应 |
| AC-IMP-002 | Editor 可导入本体 | Editor 用户上传 pizza.owl | 导入成功，创建新版本，返回 versionId | 检查 HTTP 200、versionId 非空 |
| AC-IMP-003 | 非项目成员无法导入 | 用户上传至自己不属于的项目 | 返回 HTTP 404（项目不存在）或 403（无权限），不返回 500 | 实际权限实现决定具体状态码 |
| AC-IMP-004 | 导入成功后创建新版本记录 | Editor 导入 3 个不同文件 | 数据库 ontology_versions 表行数增加 3 | 查询数据库表或 GET /api/projects/{id}/ontologies/versions 验证版本列表 |
| AC-IMP-005 | 导入成功后记录审计日志 | Editor 导入 pizza.owl | ontology_audit_logs 表增加 1 条记录，action="IMPORT"，result="SUCCESS" | 查询审计表 |
| AC-IMP-006 | 导入失败后不创建版本 | 上传畸形 RDF/XML | 返回 HTTP 400 + 错误信息；ontology_versions 无新行 | 导入前后查询版本列表；验证审计日志 action="IMPORT", result="FAILED" |

### 1.3 导出流程与权限

| AC ID | 验收条件 | 测试用例 | 期望结果 | 验收方法 |
|---|---|---|---|---|
| AC-EXP-001 | 匿名用户可导出公开项目的本体 | 匿名访问公开项目的导出端点 | HTTP 200，返回本体文件内容 | 无认证访问 GET /api/projects/{public-id}/ontologies/versions/{vid}/export |
| AC-EXP-002 | 非项目成员不能导出私有项目本体 | 非项目成员访问私有项目导出端点 | HTTP 403 或 404（不泄露项目存在性） | 验证错误响应 |
| AC-EXP-003 | 导出成功返回文件流 | Viewer 用户导出 pizza.owl (RDF/XML) | HTTP 200，Content-Type: application/rdf+xml，Content-Disposition: attachment；文件内容非空 | 检查响应头与内容 |
| AC-EXP-004 | 导出成功记录审计 | 导出版本 | ontology_audit_logs 增加 1 条记录，action="EXPORT"，result="SUCCESS" | 查询审计表 |
| AC-EXP-005 | 导出失败不返回损坏文件 | 版本数据损坏或不可读 | HTTP 500 + 错误消息；不返回空或部分数据 | 模拟数据损坏，验证错误恢复 |

### 1.4 版本管理

| AC ID | 验收条件 | 测试用例 | 期望结果 | 验收方法 |
|---|---|---|---|---|
| AC-VER-001 | 列出项目的所有版本 | GET /api/projects/{id}/ontologies/versions | 返回按 created_at 降序排列的版本列表，包含 versionId、format、axiomCount、createdBy、createdAt | 检查列表结构与字段 |
| AC-VER-002 | 恢复历史版本后内容完全相同 | 导入 v1 → 导入 v2 → 导出 v1 → 比对 | v1 的导出内容与首次导入时完全相同（公理集、IRI、注释） | OWLAPI 公理集比较；见往返保真测试 |
| AC-VER-003 | 删除版本 | Admin 用户删除某版本 | 版本记录从数据库删除；GET versions 列表中消失 | 删除前后查询版本列表 |
| AC-VER-004 | 非 Admin/Owner 不能删除版本 | Member 或 Viewer 尝试删除版本 | HTTP 403 + `AUTHORIZATION_ERROR` | 检查错误响应 |
| AC-VER-005 | 获取版本元数据 | GET /api/projects/{id}/ontologies/versions/{vid} | 返回 versionId、format、ontologyIRI、axiomCount、classCount、propertyCount、annotations 等 | 检查响应数据完整性 |

---

## 2. 往返保真验收标准（Round-Trip Fidelity）

### 2.1 Pizza 本体往返测试

| AC ID | 格式转换 | 期望结果 | 验证方法 | 度量 |
|---|---|---|---|---|
| AC-FID-PIZ-001 | RDF/XML → RDF/XML | 导入 Pizza RDF/XML → 导出 RDF/XML → 再导入 | OWLAPI `assertEquals(original.getAxioms(), reimported.getAxioms())` | 0 个差异公理 |
| AC-FID-PIZ-002 | RDF/XML → Turtle → RDF/XML | 导入 Pizza RDF/XML → 导出 Turtle → 再导入为 RDF/XML | OWLAPI 公理集相等；检查 IRI 与注释 | 0 个差异公理、0 个丢失 IRI、所有注释保存 |
| AC-FID-PIZ-003 | Turtle → Turtle | 导入 Pizza Turtle → 导出 Turtle → 再导入 | OWLAPI 公理集相等 | 0 个差异公理 |
| AC-FID-PIZ-004 | 公理计数 | 导入后查询 axiomCount | 期望 930 个公理（Pizza 的固定值） | axiomCount = 930 |
| AC-FID-PIZ-005 | 注释保存 | 导入带 dc:creator 和 dc:description 的 Pizza | 导出后再导入，注释数量与关键注释值相同 | 注释数量一致；dc:creator、dc:description 值保存 |

### 2.2 自构最小本体往返测试

创建约 2 KB 的最小 OWL 2 本体，包含：
- 1 个类 (Class)
- 1 个对象属性 (ObjectProperty)
- 1 个数据属性 (DataProperty)
- 2 个个体 (Individual)
- 1 个约束 (Class 的 rdfs:subClassOf)

| AC ID | 验收条件 | 测试用例 | 期望结果 |
|---|---|---|---|
| AC-FID-MIN-001 | 所有元素往返保存 | 导入 RDF/XML → 导出 Turtle → 再导入 | 1 个类、1 个对象属性、1 个数据属性、2 个个体、约束全部保存 |
| AC-FID-MIN-002 | 属性限制保存 | 导入包含 someValuesFrom、allValuesFrom 的本体 | 导出后重新导入，这些约束保存 |
| AC-FID-MIN-003 | 命名空间与 IRI 保存 | 导入定义了本体 IRI 与命名空间的本体 | IRI 与命名空间相同（可绝对化） |

### 2.3 带 Import 的本体往返测试

创建约 5 KB 的本体，使用相对 IRI 导入自身或另一个本体：

| AC ID | 验收条件 | 期望结果 | 注意 |
|---|---|---|---|
| AC-FID-IMP-001 | 相对 IRI import 保存 | 导入 → 导出 → 再导入，import 声明保存或绝对化后保存 | 相对 IRI 可安全绝对化，不改变语义 |
| AC-FID-IMP-002 | import 的本体不自动加载 | 导入包含 owl:imports 的本体，不试图从网络下载 | 导入成功；axiomCount 仅计算本本体，不含导入本体的公理 |

### 2.4 带注释的本体往返测试

创建包含 rdfs:comment、dc:creator、dc:date 等注释的本体：

| AC ID | 验收条件 | 期望结果 |
|---|---|---|
| AC-FID-ANN-001 | 注释数量保存 | 导入 → 导出 → 再导入，注释数量相同 |
| AC-FID-ANN-002 | 注释值保存 | 关键注释值（如 dc:creator）内容相同 |

---

## 3. 安全与资源限制验收

### 3.1 文件大小限制

| AC ID | 验收条件 | 测试方法 | 期望结果 |
|---|---|---|---|
| AC-SEC-SIZE-001 | 文件大小限制 100 MB | 创建 101 MB 的有效 RDF/XML；尝试导入 | HTTP 413 + `FILE_SIZE_EXCEEDED` |
| AC-SEC-SIZE-002 | 100 MB 以下文件接受 | 创建 99 MB 的有效 RDF/XML；导入 | 导入成功或失败于解析而非大小 |
| AC-SEC-SIZE-003 | 空文件拒绝 | 上传 0 字节文件 | HTTP 400 + `PARSING_ERROR` |

### 3.2 权限与隔离

| AC ID | 验收条件 | 测试方法 | 期望结果 |
|---|---|---|---|
| AC-SEC-PERM-001 | 不同项目的版本隔离 | 用户 A 查询项目 1 的版本；用户 B 查询项目 2 的版本 | 两者看到的版本列表分别只包含各自项目的版本 |
| AC-SEC-PERM-002 | 私有项目版本只有成员可查 | 非项目成员访问 GET /api/projects/{private}/ontologies/versions | HTTP 403 或 404，不返回版本列表 |
| AC-SEC-PERM-003 | 公开项目版本匿名可查 | 匿名访问 GET /api/projects/{public}/ontologies/versions | HTTP 200，返回公开项目的版本列表 |

### 3.3 XML 外部实体（XXE）防护

| AC ID | 验收条件 | 测试方法 | 期望结果 | 备注 |
|---|---|---|---|---|
| AC-SEC-XXE-001 | XXE payload 被拒绝或不执行 | 上传包含外部实体定义的 RDF/XML: `<!DOCTYPE rdf:RDF [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>` | PARSING_ERROR；不尝试读取系统文件 | OWLAPI 5.1.20 默认不启用 XXE；此测试验证默认配置 |
| AC-SEC-XXE-002 | 无文件访问证据 | 上传 XXE 后查询服务器日志和审计日志 | 无 `/etc/passwd` 或其他系统文件读取迹象 | 日志、系统调用跟踪 |

### 3.4 错误定位与调试信息

| AC ID | 验收条件 | 测试方法 | 期望结果 |
|---|---|---|---|
| AC-SEC-ERR-001 | 解析错误返回行号 | 上传包含语法错误的 RDF/XML（第 42 行） | 错误消息包含行号信息（e.g., "line 42"） |
| AC-SEC-ERR-002 | 错误不泄露敏感信息 | 解析失败时查询错误响应 | 错误消息不包含服务器内部路径、源码路径、完整 stack trace 或敏感配置 |

---

## 4. 审计与可追踪性验收

| AC ID | 验收条件 | 测试方法 | 期望结果 |
|---|---|---|---|
| AC-AUD-001 | 导入成功审计记录 | 导入 pizza.owl | ontology_audit_logs 新增 1 条：project_id=..., action=IMPORT, actor_email=..., file_name=pizza.owl, result=SUCCESS |
| AC-AUD-002 | 导入失败审计记录 | 上传畸形 RDF/XML | ontology_audit_logs 新增 1 条：action=IMPORT, result=FAILED, error_message 填充 |
| AC-AUD-003 | 导出审计记录 | 导出版本 | ontology_audit_logs 新增 1 条：action=EXPORT, result=SUCCESS |
| AC-AUD-004 | 删除审计记录 | Admin 删除版本 | ontology_audit_logs 新增 1 条：action=DELETE, result=SUCCESS |
| AC-AUD-005 | 审计日志无敏感数据 | 查询审计表中存储的导入文件记录 | 不记录本体内容、用户密码、会话 token |
| AC-AUD-006 | 可追踪操作链 | 导入 → 修改项目元数据 → 导出 | 审计日志清晰标记每个操作的顺序、用户、时间，支持审计事件重放 |

---

## 5. API 合约验收

### 5.1 导入端点

**端点**：`POST /api/projects/{projectId}/ontologies/import`

| AC ID | 验收条件 | 测试方法 | 期望结果 |
|---|---|---|---|
| AC-API-IMP-001 | 请求头要求 Authorization | 不提供 Authorization 头 | HTTP 401 或 303 重定向到登录 |
| AC-API-IMP-002 | 请求体为 multipart/form-data | 上传 file、format 字段 | 系统接受请求 |
| AC-API-IMP-003 | 响应包含 versionId | 导入成功 | 响应 JSON 包含 "versionId" 字段，值为有效 UUID/ID |
| AC-API-IMP-004 | 响应包含元数据 | 导入成功 | 响应包含 format、ontologyIRI、axiomCount、importedAt 等字段 |
| AC-API-IMP-005 | 失败响应遵循错误格式 | 导入失败 | HTTP 400/403/413/500，JSON 包含 "status", "errorCode", "message", "timestamp" |

### 5.2 导出端点

**端点**：`GET /api/projects/{projectId}/ontologies/versions/{versionId}/export`

| AC ID | 验收条件 | 测试方法 | 期望结果 |
|---|---|---|---|
| AC-API-EXP-001 | 查询参数 format 生效 | `?format=Turtle` | 返回 Turtle 格式文件 |
| AC-API-EXP-002 | 默认格式为原格式 | 不提供 format 参数；版本原格式为 RDF/XML | 返回 RDF/XML 格式 |
| AC-API-EXP-003 | 响应头 Content-Disposition | 导出成功 | Content-Disposition: attachment; filename="..." |
| AC-API-EXP-004 | 响应头 Content-Type | 导出 RDF/XML | Content-Type: application/rdf+xml（或对应格式） |
| AC-API-EXP-005 | 缓存控制 | 导出成功 | Cache-Control 标记为公开可缓存或私有不缓存（策略待确认） |

### 5.3 版本列表端点

**端点**：`GET /api/projects/{projectId}/ontologies/versions`

| AC ID | 验收条件 | 测试方法 | 期望结果 |
|---|---|---|---|
| AC-API-VER-001 | 分页参数生效 | `?pageNum=2&pageSize=5` | 返回第 2 页的 5 个版本 |
| AC-API-VER-002 | 排序参数生效 | `?sortBy=created_at` | 版本按 created_at 降序排列（新的在前） |
| AC-API-VER-003 | 响应包含总数 | 查询版本列表 | 响应包含 "total" 字段，表示项目的总版本数 |
| AC-API-VER-004 | 单版本元数据端点 | GET /api/projects/{id}/ontologies/versions/{vid} | 返回该版本的完整元数据（格式、IRI、公理数、注释等） |

---

## 6. 非功能验收

### 6.1 代码质量

| AC ID | 验收条件 | 度量方法 |
|---|---|---|
| AC-QUAL-001 | 单元测试覆盖率 ≥ 60% | 运行 `mvn jacoco:report`，检查 core 模块覆盖率 |
| AC-QUAL-002 | 集成测试数量 ≥ 8 个 | Testcontainers 集成测试类 ≥ 2 个，共 ≥ 8 个测试方法 |
| AC-QUAL-003 | 代码审查无高优先级问题 | 代码审查清单通过 |

### 6.2 文档完整性

| AC ID | 验收条件 | 检查项 |
|---|---|---|
| AC-DOC-001 | API 文档完整 | 所有端点的输入、输出、权限、错误文档存在 |
| AC-DOC-002 | 集成指南存在 | `docs/developer/ontology-import-export-guide.md` 存在，说明如何使用 API |
| AC-DOC-003 | 安全指南存在 | `docs/security/ontology-security.md` 记录 XXE、权限、大小限制等设计 |

---

## 7. 验收流程

### 7.1 验收准备

1. 建立测试数据集：Pizza.owl (RDF/XML + Turtle)、Minimal.owl、With-Imports.ttl、With-Annotations.owl
2. 设置测试环境：PostgreSQL 17、Java 21、Maven 3.9.16
3. 准备测试账户：Viewer、Editor、Admin 账户及对应的公开/私有项目

### 7.2 验收执行顺序

1. **单元测试**：运行 `mvn test`，覆盖 OntologyService、OntologyController、OWLAPI 集成
2. **集成测试**：运行 Testcontainers 测试，覆盖数据库、权限、端点
3. **往返保真测试**：手工或自动化验证 Pizza、Minimal 等本体的往返正确性
4. **安全测试**：验证权限检查、大小限制、XXE 防护
5. **API 合约测试**：验证端点响应格式、状态码、错误消息
6. **用户验收测试**（可选）：由产品负责人通过 Web UI 或 API 手工测试关键流程

### 7.3 验收文档

所有验收结果应记录于：

```
docs/engineering/verification/ontology-file-module-verification.md
```

记录内容：

```markdown
# 本体文件导入/导出模块验收记录

**验收日期**：2026-10-XX  
**验收人员**：...  
**环境**：Java 21, PostgreSQL 17, Maven 3.9.16  
**结论**：PASSED / FAILED

## 测试摘要

| 类别 | 测试数 | 通过 | 失败 | 跳过 |
|---|---|---|---|---|
| 单元测试 | X | X | 0 | 0 |
| 集成测试 | X | X | 0 | 0 |
| 往返保真 | X | X | 0 | 0 |
| 安全测试 | X | X | 0 | 0 |
| API 合约 | X | X | 0 | 0 |

## 验收标准清单

- [ ] AC-FMT-001 通过
- [ ] AC-FMT-002 通过
- ...
- [ ] AC-DOC-003 通过

## 发现问题

（无或列出）

## 签字

**开发负责人**：___________  
**QA 负责人**：___________  
**产品负责人**：___________
```

---

## 8. 检验清单

在宣布模块完成前，逐项确认：

- [ ] 所有验收标准代码已实现
- [ ] 单元测试 ≥ 60% 覆盖，0 个失败
- [ ] Testcontainers 集成测试 ≥ 8 个，0 个失败
- [ ] Pizza 往返保真通过（4 个格式组合）
- [ ] Minimal 往返保真通过（至少 3 个格式组合）
- [ ] 权限检查覆盖 Viewer/Editor/Admin/匿名
- [ ] 大小限制测试通过（≥ 100 MB 拒绝）
- [ ] XXE 防护验证通过（无文件访问）
- [ ] 审计日志记录完整，无敏感数据泄漏
- [ ] API 端点响应格式与文档一致
- [ ] 代码审查通过（无高优先级问题）
- [ ] 文档完整（API、集成指南、安全）
- [ ] 验收记录已签字

---

**关键依赖**：
- 本体文件导入/导出模块规格
- ADR-0003（本体文件管理决策）
- OWLAPI 5.1.20 集成
- PostgreSQL 17 / Flyway 迁移

**下一步**：用户确认本验收标准后，开始代码实现。
