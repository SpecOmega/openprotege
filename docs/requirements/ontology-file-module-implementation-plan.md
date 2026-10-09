# 本体文件导入/导出模块 — 实现计划与设计框架

**版本**：0.1-draft  
**日期**：2026-10-09  
**关联文档**：
- [导入/导出规格](./ontology-file-import-export-spec.md)
- [验收标准](./ontology-file-import-export-acceptance-criteria.md)
- [ADR-0003: 本体文件管理](../architecture/adr/0003-ontology-file-management.md)

---

## 1. 模块概述

本体文件导入/导出模块为 OpenProtégé Web 后端提供核心本体工程能力：
- 识别与导入 OWL 2 本体文件（RDF/XML、Turtle）
- 解析、校验、存储本体版本
- 按权限导出并验证往返保真
- 记录审计日志

### 目标交付清单

| 项目 | 优先级 | 目标交付 | 验收依据 |
|---|---|---|---|
| Flyway 数据库迁移 | P0 | `V2__ontology_versions_and_audit.sql` | 5 个表创建，Compose 验证 |
| OntologyVersion、OntologyAuditLog 数据模型 | P0 | Spring Data JPA entity | 单元测试覆盖序列化/比较 |
| OntologyService 核心服务 | P0 | 导入、导出、版本查询、权限检查 | Testcontainers 集成测试，≥ 60% 代码覆盖 |
| OntologyController REST 端点 | P0 | POST /import、GET /export、GET /versions | API 合约测试，≥ 8 个端点测试 |
| OWLAPI 5.1.20 集成 | P0 | 格式识别、解析、序列化 | Pizza 往返测试（RDF/XML ↔ Turtle） |
| 往返保真测试 | P1 | Pizza + 3 个自构本体 | AC-FID-001 ~ AC-FID-004 全部通过 |
| 权限与安全测试 | P1 | Viewer/Editor/Admin 矩阵、XXE 防护 | AC-SEC-PERM-001 ~ AC-SEC-XXE-002 全部通过 |
| 文档 | P2 | API、集成指南、安全指南 | 3 个 MD 文件，链接完整 |

---

## 2. 代码结构与模块划分

### 2.1 包结构

```
server/src/main/java/com/specomega/openprotege/server/
├── ontology/
│   ├── model/
│   │   ├── OntologyVersion.java
│   │   ├── OntologyAuditLog.java
│   │   └── OntologyMetadata.java
│   ├── repository/
│   │   ├── OntologyVersionRepository.java
│   │   └── OntologyAuditLogRepository.java
│   ├── service/
│   │   ├── OntologyService.java
│   │   ├── OntologyFormatValidator.java
│   │   ├── OntologyParseError.java
│   │   └── OntologyPermissionService.java
│   ├── controller/
│   │   └── OntologyController.java
│   └── exception/
│       ├── OntologyImportException.java
│       ├── OntologyParsingException.java
│       ├── OntologyValidationException.java
│       ├── OntologyFormatNotSupportedException.java
│       └── OntologyFileSizeExceededException.java

server/src/test/java/com/specomega/openprotege/server/
├── ontology/
│   ├── OntologyServiceUnitTest.java
│   ├── OntologyFormatValidatorTest.java
│   ├── OntologyPermissionServiceTest.java
│   └── OntologyControllerApiContractTest.java
└── ontology/integration/
    ├── OntologyImportExportIntegrationTest.java
    ├── OntologyRoundTripFidelityTest.java
    ├── OntologyPermissionIntegrationTest.java
    └── OntologySecurityIntegrationTest.java

server/src/test/resources/
└── ontologies/
    ├── pizza.owl (RDF/XML, ~120 KB)
    ├── pizza.ttl (Turtle, ~80 KB)
    ├── minimal.owl (自构，~2 KB)
    ├── with-imports.ttl (自构，~5 KB)
    └── with-annotations.owl (自构，~3 KB)
```

### 2.2 核心类设计

#### OntologyVersion Entity

```java
@Entity
@Table(name = "ontology_versions", indexes = {
  @Index(name = "idx_project_created", columnList = "project_id, created_at DESC")
})
public class OntologyVersion {
    @Id
    private String id;  // UUID
    
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
    
    @Enumerated(EnumType.STRING)
    private OWLFormat format;  // RDF/XML, Turtle, ...
    
    @Lob
    private byte[] content;  // 本体文件二进制
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private OntologyMetadata metadata;  // axiomCount, iri, ...
    
    private String createdBy;  // User email
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // getters, setters, equals, hashCode
}
```

#### OntologyAuditLog Entity

```java
@Entity
@Table(name = "ontology_audit_logs", indexes = {
  @Index(name = "idx_project_action", columnList = "project_id, action, created_at DESC")
})
public class OntologyAuditLog {
    @Id
    private String id;  // UUID
    
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
    
    @Enumerated(EnumType.STRING)
    private AuditAction action;  // IMPORT, EXPORT, DELETE, ...
    
    private String actorEmail;
    
    @ManyToOne
    @JoinColumn(name = "version_id", nullable = true)
    private OntologyVersion version;
    
    private String fileName;
    private Long fileSizeBytes;
    private String format;
    
    @Enumerated(EnumType.STRING)
    private AuditResult result;  // SUCCESS, FAILED
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    private LocalDateTime createdAt;
    
    // getters, setters
}
```

#### OntologyService 核心方法签名

```java
@Service
@Transactional
public class OntologyService {
    
    private final OntologyVersionRepository versionRepo;
    private final OntologyAuditLogRepository auditRepo;
    private final ProjectService projectService;
    private final OntologyPermissionService permissionService;
    private final OWLOntologyManager ontologyManager;
    
    /**
     * 导入本体文件。
     * @param projectId 项目 ID
     * @param userId 当前用户
     * @param fileContent 文件二进制
     * @param fileName 文件名（用于审计）
     * @param format 格式（可空，则自动识别）
     * @return 创建的版本
     * @throws AuthorizationException 无权限
     * @throws OntologyFormatNotSupportedException 格式不支持
     * @throws FileSizeExceededException 文件过大
     * @throws OntologyParsingException 解析失败
     * @throws OntologyValidationException 校验失败
     */
    public OntologyVersion importOntology(
        String projectId,
        String userId,
        byte[] fileContent,
        String fileName,
        String format
    ) throws OntologyException {
        // 1. 权限检查
        projectService.requireProjectRole(projectId, userId, Role.Editor);
        
        // 2. 格式识别与验证
        OWLFormat resolvedFormat = OntologyFormatValidator.identify(fileName, fileContent, format);
        
        // 3. 大小检查
        if (fileContent.length > MAX_FILE_SIZE_BYTES) {
            throw new FileSizeExceededException(...);
        }
        
        try {
            // 4. 解析本体
            OWLOntology ontology = parseOntology(fileContent, resolvedFormat);
            
            // 5. 校验
            validateOntology(ontology);
            
            // 6. 创建版本记录
            OntologyVersion version = createVersionRecord(
                projectId, ontology, resolvedFormat, userId, fileName, fileContent
            );
            
            // 7. 记录审计
            logAudit(AuditAction.IMPORT, projectId, userId, fileName, version, 
                    AuditResult.SUCCESS, null);
            
            return version;
        } catch (Exception e) {
            logAudit(AuditAction.IMPORT, projectId, userId, fileName, null, 
                    AuditResult.FAILED, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 导出本体文件。
     * @param projectId 项目 ID
     * @param versionId 版本 ID
     * @param userId 当前用户
     * @param targetFormat 目标格式（可空则使用原格式）
     * @return 文件二进制
     */
    public byte[] exportOntology(
        String projectId,
        String versionId,
        String userId,
        String targetFormat
    ) throws OntologyException {
        // 1. 权限检查
        projectService.requireProjectRole(projectId, userId, Role.Viewer);
        
        // 2. 加载版本
        OntologyVersion version = versionRepo.findById(versionId)
            .orElseThrow(() -> new VersionNotFoundException(versionId));
        
        // 3. 反序列化本体
        OWLOntology ontology = deserializeOntology(version.getContent(), version.getFormat());
        
        // 4. 确定导出格式
        OWLFormat exportFormat = targetFormat != null ? 
            OWLFormat.valueOf(targetFormat) : version.getFormat();
        
        try {
            // 5. 序列化
            byte[] exported = serializeOntology(ontology, exportFormat);
            
            // 6. 记录审计
            logAudit(AuditAction.EXPORT, projectId, userId, null, version, 
                    AuditResult.SUCCESS, null);
            
            return exported;
        } catch (Exception e) {
            logAudit(AuditAction.EXPORT, projectId, userId, null, version, 
                    AuditResult.FAILED, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 列出项目的所有本体版本。
     */
    public Page<OntologyVersion> listVersions(
        String projectId,
        String userId,
        int pageNum,
        int pageSize
    ) {
        projectService.requireProjectRole(projectId, userId, Role.Viewer);
        Pageable paging = PageRequest.of(pageNum - 1, pageSize, 
            Sort.by("createdAt").descending());
        return versionRepo.findByProject_Id(projectId, paging);
    }
    
    /**
     * 删除版本。
     */
    public void deleteVersion(String projectId, String versionId, String userId) {
        projectService.requireProjectRole(projectId, userId, Role.Admin);
        OntologyVersion version = versionRepo.findById(versionId)
            .orElseThrow(() -> new VersionNotFoundException(versionId));
        versionRepo.delete(version);
        logAudit(AuditAction.DELETE, projectId, userId, null, version, 
                AuditResult.SUCCESS, null);
    }
    
    // 辅助方法
    private OWLOntology parseOntology(byte[] content, OWLFormat format) 
        throws OntologyParsingException { ... }
    
    private OWLOntology deserializeOntology(byte[] content, OWLFormat format) 
        throws OntologyLoadException { ... }
    
    private byte[] serializeOntology(OWLOntology ontology, OWLFormat format) 
        throws FormatConversionException { ... }
    
    private void validateOntology(OWLOntology ontology) 
        throws OntologyValidationException { ... }
    
    private OntologyVersion createVersionRecord(...) { ... }
    
    private void logAudit(...) { ... }
}
```

#### OntologyController REST 端点

```java
@RestController
@RequestMapping("/api/projects/{projectId}/ontologies")
public class OntologyController {
    
    @PostMapping("/import")
    public ResponseEntity<?> importOntology(
        @PathVariable String projectId,
        @RequestParam(required = false) String format,
        @RequestParam("file") MultipartFile file,
        HttpSession session
    ) {
        try {
            String userId = getCurrentUserId(session);
            byte[] fileContent = file.getBytes();
            OntologyVersion version = ontologyService.importOntology(
                projectId, userId, fileContent, file.getOriginalFilename(), format
            );
            return ResponseEntity.ok(new ImportSuccessResponse(version));
        } catch (AuthorizationException e) {
            return ResponseEntity.status(403)
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        } catch (FileSizeExceededException e) {
            return ResponseEntity.status(413)
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        } catch (OntologyException e) {
            return ResponseEntity.status(400)
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
        }
    }
    
    @GetMapping("/versions/{versionId}/export")
    public ResponseEntity<byte[]> exportOntology(
        @PathVariable String projectId,
        @PathVariable String versionId,
        @RequestParam(required = false) String format,
        HttpSession session
    ) {
        String userId = getCurrentUserId(session);
        byte[] content = ontologyService.exportOntology(projectId, versionId, userId, format);
        return ResponseEntity.ok()
            .header("Content-Type", "application/rdf+xml")  // 根据格式设置
            .header("Content-Disposition", "attachment; filename=\"ontology.owl\"")
            .body(content);
    }
    
    @GetMapping("/versions")
    public ResponseEntity<?> listVersions(
        @PathVariable String projectId,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        HttpSession session
    ) {
        String userId = getCurrentUserId(session);
        Page<OntologyVersion> versions = ontologyService.listVersions(
            projectId, userId, pageNum, pageSize
        );
        return ResponseEntity.ok(new VersionListResponse(versions));
    }
    
    @GetMapping("/versions/{versionId}")
    public ResponseEntity<?> getVersionMetadata(
        @PathVariable String projectId,
        @PathVariable String versionId,
        HttpSession session
    ) {
        String userId = getCurrentUserId(session);
        OntologyVersion version = ontologyService.getVersion(projectId, versionId, userId);
        return ResponseEntity.ok(new VersionMetadataResponse(version));
    }
    
    @DeleteMapping("/versions/{versionId}")
    public ResponseEntity<?> deleteVersion(
        @PathVariable String projectId,
        @PathVariable String versionId,
        HttpSession session
    ) {
        String userId = getCurrentUserId(session);
        ontologyService.deleteVersion(projectId, versionId, userId);
        return ResponseEntity.noContent().build();
    }
    
    // 辅助方法
    private String getCurrentUserId(HttpSession session) { ... }
}
```

---

## 3. 依赖与集成

### 3.1 Maven 依赖（server/pom.xml）

```xml
<!-- OWLAPI -->
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

<!-- JSON 支持（metadata） -->
<dependency>
    <groupId>com.vladmihalceanu</groupId>
    <artifactId>hibernate-types-60</artifactId>
    <version>2.21.1</version>
</dependency>
```

### 3.2 与现有模块的集成

- **ProjectService**：权限检查与项目查询
- **SecurityConfiguration**：权限拦截器（已存在）
- **Flyway**：数据库迁移（新增 V2 迁移）
- **ExceptionHandler**：统一错误映射（需扩展）

---

## 4. 开发路线图

### Phase 1: 数据库与模型（1-2 天）

- [ ] 创建 `V2__ontology_versions_and_audit.sql` 迁移
- [ ] 实现 `OntologyVersion`、`OntologyAuditLog` entity
- [ ] 创建 `OntologyVersionRepository`、`OntologyAuditLogRepository`
- [ ] 单元测试（entity 序列化、JPA 注解）

### Phase 2: OWLAPI 集成与格式处理（2-3 天）

- [ ] 集成 OWLAPI 5.1.20
- [ ] 实现 `OntologyFormatValidator`（格式识别与验证）
- [ ] 实现导入解析流程（`parseOntology`）
- [ ] 实现导出序列化流程（`serializeOntology`）
- [ ] 单元测试（Pizza RDF/XML + Turtle）

### Phase 3: 服务层与权限（2-3 天）

- [ ] 实现 `OntologyService` 核心方法
- [ ] 实现 `OntologyPermissionService`
- [ ] 实现审计日志记录
- [ ] 集成 ProjectService 权限检查
- [ ] 单元测试（≥ 60% 覆盖）

### Phase 4: REST 端点与 API（1-2 天）

- [ ] 实现 `OntologyController` 所有端点
- [ ] 请求验证与错误映射
- [ ] API 合约测试（8+ 个测试）

### Phase 5: 集成测试与往返保真（2-3 天）

- [ ] Testcontainers 集成测试（导入、导出、权限、版本）
- [ ] Pizza 往返保真测试（RDF/XML ↔ Turtle）
- [ ] Minimal 往返保真测试
- [ ] With-Imports、With-Annotations 往返测试

### Phase 6: 安全与 XX防护（1-2 天）

- [ ] XXE 防护验证测试
- [ ] 权限矩阵集成测试（Viewer/Editor/Admin）
- [ ] 大小限制测试（100+ MB）
- [ ] 错误隐藏信息测试

### Phase 7: 文档与验收（1 天）

- [ ] API 文档完成
- [ ] 集成指南完成
- [ ] 安全指南完成
- [ ] 验收签字

---

## 5. 测试策略

### 5.1 单元测试目标

```
target: ≥ 60% 代码覆盖
files: OntologyService, OntologyFormatValidator, OntologyPermissionService
tests: ≥ 20 个
```

### 5.2 集成测试目标

```
framework: Testcontainers + PostgreSQL 17
files: OntologyImportExportIntegrationTest, OntologyRoundTripFidelityTest, 
       OntologyPermissionIntegrationTest, OntologySecurityIntegrationTest
tests: ≥ 25 个
```

### 5.3 往返保真测试

```
test data:
  - Pizza.owl (RDF/XML) 930 axioms
  - Pizza.ttl (Turtle) 930 axioms
  - Minimal.owl (自构) ~10 axioms
  - With-Imports.ttl (自构) with owl:imports
  - With-Annotations.owl (自构) with rdfs:comment, dc:*

format combinations:
  - RDF/XML → RDF/XML
  - RDF/XML → Turtle → RDF/XML
  - Turtle → Turtle
  - Turtle → RDF/XML → Turtle

validation:
  - OWLAPI.getAxioms() 集合相等
  - OntologyID 与 IRI 相同（允许相对化）
  - 注释数量与关键值相同
```

---

## 6. 验收提交

完成后需提交：

1. **代码提交**（未推送）
   - `server/src/main/java/com/specomega/openprotege/server/ontology/`
   - `server/src/test/java/com/specomega/openprotege/server/ontology/`
   - `server/src/test/resources/ontologies/`
   - `server/src/main/resources/db/migration/V2__ontology_versions_and_audit.sql`

2. **文档提交**（本文件夹已生成）
   - `docs/requirements/ontology-file-import-export-spec.md` ✓
   - `docs/requirements/ontology-file-import-export-acceptance-criteria.md` ✓
   - `docs/architecture/adr/0003-ontology-file-management.md` ✓
   - `docs/developer/ontology-import-export-guide.md` （开发阶段生成）
   - `docs/security/ontology-security.md` （开发阶段生成）

3. **验收记录**
   - 单元测试覆盖率报告（≥ 60%）
   - 集成测试运行日志（≥ 25 个通过）
   - 往返保真验证结果（Pizza + 3 个自构本体）
   - 安全测试结果（权限、XXE、大小限制）

4. **工程文档更新**
   - `docs/requirements/SRS.md` （追加本体模块需求） ✓
   - `docs/requirements/traceability-matrix.csv` （追加本体需求追踪）

---

## 7. 检验清单（开发前）

完成规格文档后，启动代码开发前需逐项确认：

- [ ] 用户确认本实现计划
- [ ] Flyway 迁移脚本设计已获同意
- [ ] API 端点与错误合约已确认
- [ ] 往返保真标准（OWL 2 语义等价）已确认
- [ ] 测试环境与依赖已准备（OWLAPI、Testcontainers、Pizza 本体语料）
- [ ] 代码审查流程与质量门槛已明确（≥ 60% 覆盖、0 个高优先级问题）

---

**下一步**：在用户确认本计划后，立即启动 Phase 1 数据库与模型实现。

