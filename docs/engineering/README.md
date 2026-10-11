# Stage 0 Engineering Evidence / 工程证据索引

审计基准日期 / Audit baseline date: 2026-10-09

本次文档复核基准 Git HEAD / Git HEAD at this documentation review: `9df91e479c53a9a78f83e9e2f9143f9281bddba9`（本次文档更新尚未提交 / documentation updates from this review are uncommitted）

初始无源码基线 / Initial no-source baseline: `d9caafee6858a958ea7a2944d574407a79f88309`

语言 / Language: 中文为主；逐篇英文翻译状态见下表。 / Chinese primary; English translation status is listed below.

## Evidence labels / 证据等级

- `VERIFIED`: 可重复检查、实际测试或权威来源支持。 / Reproducible check, executed test, or authoritative source.
- `SOURCE-OBSERVED`: 从当前文件、配置或历史直接观察；没有运行验证。 / Directly observed in files, configuration, or history; not runtime-verified.
- `PROPOSED`: 建议或目标，不是当前实现。 / Proposal or target, not current implementation.
- `UNVERIFIED`: 证据不足。 / Insufficient evidence.
- `BLOCKED`: 客观条件阻止验证。 / Verification prevented by an objective constraint.

## Stage 0 documents / 阶段 0 文档

| File | Purpose / 用途 | English translation |
|---|---|---|
| [00-repository-baseline.md](./00-repository-baseline.md) | 仓库与 Git 基线 / Repository and Git baseline | Pending |
| [01-current-architecture.md](./01-current-architecture.md) | 当前架构事实与边界 / Current architecture and boundaries | Pending |
| [02-module-inventory.md](./02-module-inventory.md) | 模块清单 / Module inventory | Pending |
| [03-build-and-test-baseline.md](./03-build-and-test-baseline.md) | 构建与测试基线 / Build and test baseline | Pending |
| [04-product-capability-matrix.md](./04-product-capability-matrix.md) | 产品能力矩阵 / Product capability matrix | Pending |
| [05-security-review.md](./05-security-review.md) | 安全审计边界与风险 / Security review boundaries and risks | Pending |
| [06-dependency-and-upstream-review.md](./06-dependency-and-upstream-review.md) | 依赖与上游关系 / Dependencies and upstream | Pending |
| [07-technical-debt-register.md](./07-technical-debt-register.md) | 技术债登记 / Technical debt register | Pending |
| [08-assumptions-and-open-questions.md](./08-assumptions-and-open-questions.md) | 假设与待决事项 / Assumptions and open questions | Pending |
| [09-engineering-roadmap.md](./09-engineering-roadmap.md) | 分阶段路线图 / Phased roadmap | Pending |
| [10-stage-0-exit-report.md](./10-stage-0-exit-report.md) | 阶段退出判定 / Stage exit decision | Pending |

Pending means that an English translation has not been prepared or independently reviewed. The Chinese files are not presented as complete English documentation.

## Requirements baseline / 需求基线

开发者与运维人员的逐步运行手册见[开发与部署保姆级指南](../guides/development-and-deployment.md)。
中英文 dev 业务链路及可执行 API 场景见[示例索引](../guides/examples/business-scenario-zh.md)和[English scenario](../guides/examples/business-scenario-en.md)；场景脚本不是浏览器 E2E 验收。

| File | Purpose / 用途 | English translation |
|---|---|---|
| [Product vision](../product/vision.md) | 产品愿景与决策边界 / Vision and decision boundaries | Pending |
| [SRS](../requirements/SRS.md) | 软件需求规格草案 / Draft software requirements specification | Pending |
| [Use cases](../requirements/use-cases.md) | 首期用户场景 / Initial user scenarios | Pending |
| [Acceptance criteria](../requirements/acceptance-criteria.md) | 可验证验收草案 / Verifiable acceptance draft | Pending |
| [Traceability matrix](../requirements/traceability-matrix.csv) | 需求到验收的追踪 / Requirements-to-acceptance mapping | Machine-oriented English fields; normative Chinese wording is in the SRS. English documentation translation is Pending. |

The baseline records product decisions confirmed by the owner separately from technical decisions awaiting upstream source/runtime validation and unresolved product details. These documents do not imply implementation or test completion.

本次复核执行的 Maven 默认测试命令不会自动运行 Testcontainers 集成测试；这些集成测试须显式启用 `-Dopenprotege.testcontainers.enabled=true`。当前默认测试结果为 22 项、13 项通过、9 项跳过。详情及命令见[构建与测试基线](./03-build-and-test-baseline.md)和[已知缺口](./known-gaps.md)。

## Audit evidence / 审计证据

此前的详细审计、固定上游提交和证据台账仍保留：

- [repository-audit.md](./repository-audit.md)
- [upstream-audit.md](./upstream-audit.md)
- [evidence-ledger.md](./evidence-ledger.md)
- [known-gaps.md](./known-gaps.md)

The upstream audit includes fixed-commit source review, limited build/test evidence, and a single-sample OWLAPI serialization PoC; WebProtégé packaging remains blocked. It is not a complete runtime, editor-workflow, security, interoperability, or dependency-license review.
