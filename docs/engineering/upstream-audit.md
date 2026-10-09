# Protégé 上游源码与关系审计

审计日期：2026-10-09

范围：任务指定的两个上游仓库在本次审计取得的固定提交。仓库已完整克隆至临时目录并 detached checkout 固定 SHA；执行了 Maven 构建/测试。没有在本轮启动 Desktop/Web 应用或实施跨端互操作。

## 固定提交与证据来源

通过以下命令解析 GitHub `HEAD` 与默认 `master` 引用：

```text
git ls-remote https://github.com/protegeproject/protege.git HEAD refs/heads/master refs/heads/main refs/tags/5.6.6
git ls-remote https://github.com/protegeproject/webprotege.git HEAD refs/heads/master refs/heads/main
```

| 上游 | 固定提交 | 固定提交可观察内容 | 状态 |
|---|---|---|---|
| [protegeproject/protege](https://github.com/protegeproject/protege/tree/bf03cccc65ea664e139d6829bba6d209ef58ee55) | `bf03cccc65ea664e139d6829bba6d209ef58ee55`；提交时间 `2026-09-23T22:12:40+01:00` | 完整克隆；Maven 多模块工程，含桌面/editor 模块。 | SOURCE-OBSERVED |
| [protegeproject/webprotege](https://github.com/protegeproject/webprotege/tree/1e84fa02aef68be45f18c08dbeae94bec9b04a41) | `1e84fa02aef68be45f18c08dbeae94bec9b04a41`；提交时间 `2026-07-28T13:56:32-07:00` | 完整克隆；Maven client/server/shared 模块，根目录含 Docker/Compose 配置。README 声明该仓库正被细粒度仓库取代。 | SOURCE-OBSERVED |

固定 SHA 通过 Git 远端引用解析后进行完整克隆和 checkout，并以 `git rev-parse HEAD` 复核；两检出均无源码工作区改动。（VERIFIED）上游目录、依赖、CI、格式及测试源码均为固定提交的源码观察。（SOURCE-OBSERVED）

## Protégé Desktop

- 固定提交 README 将项目称为 Protege Desktop，并声明其为支持 OWL 2.0、具有可插拔架构的本体编辑器；这些是上游自身描述，不是本次运行验证。（SOURCE-OBSERVED）
- 固定提交根 `pom.xml` 的根坐标为 `edu.stanford.protege:protege-parent:5.6.10-SNAPSHOT`，packaging 为 `pom`。模块列出 `protege-launcher`、`protege-common`、`protege-editor-core`、`protege-editor-owl`、`protege-desktop`。（SOURCE-OBSERVED）
- 根 POM 中可见 OWL API OSGi distribution、Felix、JUnit 等依赖/插件配置；这不等于完整依赖许可证审查。（SOURCE-OBSERVED）
- `license.txt` 包含 Stanford Board of Trustees 版权声明及 BSD 2-Clause 风格的再分发条件和免责声明；README 亦指向 BSD 2-clause。此观察不替代律师意见或对传递依赖的审查。（SOURCE-OBSERVED）
- CI workflow 声明 `default`、`ide`、`release` profiles × JDK 11/21 的矩阵并运行 `clean verify`。（SOURCE-OBSERVED）
- 在 JDK 21.0.12.1、Maven 3.9.16 下执行 `mvn -B -ntp -Drelease.signing.disabled=true clean verify`，退出码 0，输出 `BUILD SUCCESS`；76 份 Surefire/Failsafe XML 报告合计 531 tests、0 failures、0 errors、3 skipped。（VERIFIED）
- `DocumentFormatMapper_TestCase` 覆盖 Rio Turtle 格式映射及前缀传递，这是局部单元测试，不是 RDF/XML/Turtle 本体往返保真证明。（SOURCE-OBSERVED；测试执行包含于固定提交构建报告）
- 基于固定提交中的 `pizza.owl` 和其声明的 OWLAPI OSGi distribution 4.5.29，独立运行 RDF/XML 输入→Turtle 保存/再加载→RDF/XML 保存/再加载。930 个公理、本体 ID、本体注释及 imports 完全一致，Java PoC 退出码 0。（VERIFIED；PoC 代码、命令和边界见 [03-build-and-test-baseline.md](./03-build-and-test-baseline.md) 和 [证据台账](./evidence-ledger.md)）
- 另一个上游测试样例 `photography.owl` 的探索性序列化检查出现公理集合变化（输入 672、Turtle 再加载 707）；差异原因尚未查明，该样例不计作通过。（VERIFIED：实际观察到集合不等；原因 UNVERIFIED）
- 未运行桌面 GUI；未验证编辑器内导入—编辑—保存—导出—再次加载或 Web 互操作。（UNVERIFIED）

## WebProtégé

- 固定提交 README 将 WebProtégé 描述为浏览器访问的协同本体开发 Web 应用；这是上游文档描述而非运行验证。（SOURCE-OBSERVED）
- 固定提交根 POM 的坐标为 `edu.stanford.protege:webprotege:5.0.0-SNAPSHOT`，packaging 为 `pom`；根目录模块包括 `webprotege-cli`、`webprotege-client`、server、shared 等模块。（SOURCE-OBSERVED）
- 固定提交 README 明确说明该仓库正被更细粒度的仓库集合取代，并说明仓库转向微服务架构。因而该仓库不能在没有进一步维护状态/目标版本评估的情况下，直接作为默认新集成基线。（SOURCE-OBSERVED）
- README 提到 Docker/Compose 运行方式，固定提交根目录含 Dockerfile、Compose 与 Maven 配置；本次没有构建镜像或启动服务。（SOURCE-OBSERVED）
- `license.txt` 包含 Stanford Board of Trustees 版权声明及 BSD 2-Clause 风格条款。传递依赖许可证未审计。（SOURCE-OBSERVED）
- 根 POM 坐标为 `edu.stanford.protege:webprotege:5.0.0-SNAPSHOT`；根 compiler release 为 11，`webprotege-shared` 模块配置 release 8，并声明 AutoValue 1.7.1。（SOURCE-OBSERVED）
- 源码包含 access manager、角色及项目授权实现/测试；`AccessManagerImpl_IT`、`ProjectAccessManagerImpl_IT` 为集成测试源码。存在代码/测试不等于通过权限安全审计。（SOURCE-OBSERVED）
- Compose 文件声明 MongoDB `mongo:4.1-bionic`；本次使用该版本的临时容器。首次无数据库构建测试连接失败；提供 Mongo 后，JDK 25 构建仍在编译 `AutoValue_*` 生成类型处失败。JDK 21 初次重试有 5 个模块共 4091 tests、0 failures、0 errors、0 skipped；随后 package 因项目 GitHub Maven 仓库 HTTP 504 阻塞，无最终 Maven 退出状态。该 endpoint 后续返回 HTTP 200，本轮已启动恢复后完整 package 重试，执行结果待记录。当前不能断言完整 package 成功或失败。（VERIFIED：日志及探测命令，见 [03-build-and-test-baseline.md](./03-build-and-test-baseline.md)）
- Web 的文件下载参数测试源码覆盖多种序列化格式选择；不能替代真实项目上传/下载往返保真或跨端交换测试。（SOURCE-OBSERVED）

## 与 OpenProtégé 当前检出的关系

OpenProtégé 当前检出含 README 与工程/需求审计文档，但没有应用源代码。仓库内未见上游代码、依赖、子模块、历史或来源声明；因而现有证据只能支持：**本次审计检出的 OpenProtégé 尚无可观察到的 Desktop/WebProtégé 集成实现**。（SOURCE-OBSERVED）

“使用成熟本体编辑技术”是项目目标，不是技术集成事实。两个上游的存在、声明功能、构建文件或许可证都不能证明 OpenProtégé 继承/实现了相同能力。（SOURCE-OBSERVED）

## 未执行的验证及约束

下列工作属于后续阶段，不在本次仓库/源码盘点中虚报为通过：

| 验证 | 状态 | 尚缺证据 |
|---|---|---|
| 上游构建、测试及其日志/退出码 | Desktop Maven 验证 VERIFIED；WebProtégé JDK 21 恢复后 package 重试进行中 | Desktop 通过；Web JDK 21 初次有 4091 项已完成模块测试，后续 package 遇 HTTP 504；endpoint 已恢复 HTTP 200，完整重试尚未结束 |
| OWLAPI 单样例 RDF/XML↔Turtle 文件往返 | VERIFIED（固定 `pizza.owl`） | 930 个公理、本体 ID、注释和 imports 精确保持；不涵盖编辑器或 Web 产品流程 |
| 编辑器/应用级 OWL/RDF 导入—编辑—保存—导出—再加载 | UNVERIFIED | 未启动桌面 GUI 或运行编辑器工作流 |
| Desktop 与 Web 数据交换 | UNVERIFIED | 未配置或运行互操作 PoC |
| Web 身份认证、服务端授权和项目隔离 | UNVERIFIED | 未部署服务或做集成测试 |
| 插件加载/扩展 API 兼容性 | UNVERIFIED | 未检查完整 API，也未运行插件 |
| 全依赖清单、许可证兼容及供应链风险 | UNVERIFIED | 根许可证已查看；传递依赖许可证与漏洞仍未审计 |

后续构建前应重新固定和复核上游提交、按受支持环境实际执行构建/测试，并保存命令、环境、日志和退出码；WebProtégé 还应先评估固定仓库的维护状态与其替代仓库。（PROPOSED）
