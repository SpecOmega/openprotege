# Protégé 上游源码与关系审计

审计日期：2026-10-09  
范围：任务指定的两个上游仓库在本次审计取得的固定提交。此文档为基于仓库根目录、README、许可证及 Maven 根 POM 的静态审查；没有克隆、构建或运行上游项目。

## 固定提交与证据来源

通过以下命令解析 GitHub `HEAD` 与默认 `master` 引用：

```text
git ls-remote https://github.com/protegeproject/protege.git HEAD refs/heads/master refs/heads/main refs/tags/5.6.6
git ls-remote https://github.com/protegeproject/webprotege.git HEAD refs/heads/master refs/heads/main
```

| 上游 | 固定提交 | 固定提交可观察内容 | 状态 |
|---|---|---|---|
| [protegeproject/protege](https://github.com/protegeproject/protege/tree/bf03cccc65ea664e139d6829bba6d209ef58ee55) | `bf03cccc65ea664e139d6829bba6d209ef58ee55`（HEAD 与 `master`） | 根目录包括 `pom.xml`、`license.txt`，以及 `protege-common`、`protege-desktop`、`protege-editor-core`、`protege-editor-owl`、`protege-launcher` 等模块。 | SOURCE-OBSERVED |
| [protegeproject/webprotege](https://github.com/protegeproject/webprotege/tree/1e84fa02aef68be45f18c08dbeae94bec9b04a41) | `1e84fa02aef68be45f18c08dbeae94bec9b04a41`（HEAD 与 `master`） | 根目录包括 `pom.xml`、`license.txt`、Docker/Compose 文件、CLI、客户端及多个 server/shared 模块。 | SOURCE-OBSERVED |

引用解析命令成功完成，返回表中的固定 SHA。（VERIFIED）GitHub 固定版本文件读取成功；根目录、README、POM、许可证内容为源码观察。（SOURCE-OBSERVED）

## Protégé Desktop

- 固定提交 README 将项目称为 Protege Desktop，并声明其为支持 OWL 2.0、具有可插拔架构的本体编辑器；这些是上游自身描述，不是本次运行验证。（SOURCE-OBSERVED）
- 固定提交根 `pom.xml` 的根坐标为 `edu.stanford.protege:protege-parent:5.6.10-SNAPSHOT`，packaging 为 `pom`。模块列出 `protege-launcher`、`protege-common`、`protege-editor-core`、`protege-editor-owl`、`protege-desktop`。（SOURCE-OBSERVED）
- 根 POM 中可见 OWL API OSGi distribution、Felix、JUnit 等依赖/插件配置；这不等于完整依赖许可证审查。（SOURCE-OBSERVED）
- `license.txt` 包含 Stanford Board of Trustees 版权声明及 BSD 2-Clause 风格的再分发条件和免责声明；README 亦指向 BSD 2-clause。此观察不替代律师意见或对传递依赖的审查。（SOURCE-OBSERVED）
- 未在本阶段验证特定输入下的导入、编辑、保存、导出、再次加载、OWL/RDF 保真、插件兼容、构建、测试、身份或授权行为。（UNVERIFIED）

## WebProtégé

- 固定提交 README 将 WebProtégé 描述为浏览器访问的协同本体开发 Web 应用；这是上游文档描述而非运行验证。（SOURCE-OBSERVED）
- 固定提交根 POM 的坐标为 `edu.stanford.protege:webprotege:5.0.0-SNAPSHOT`，packaging 为 `pom`；根目录模块包括 `webprotege-cli`、`webprotege-client`、server、shared 等模块。（SOURCE-OBSERVED）
- 固定提交 README 明确说明该仓库正被更细粒度的仓库集合取代，并说明仓库转向微服务架构。因而该仓库不能在没有进一步维护状态/目标版本评估的情况下，直接作为默认新集成基线。（SOURCE-OBSERVED）
- README 提到 Docker/Compose 运行方式，固定提交根目录含 Dockerfile、Compose 与 Maven 配置；本次没有构建镜像或启动服务。（SOURCE-OBSERVED）
- `license.txt` 包含 Stanford Board of Trustees 版权声明及 BSD 2-Clause 风格条款。传递依赖许可证未审计。（SOURCE-OBSERVED）
- 未在本阶段验证协作、认证、服务端权限、隔离、导入导出、格式保真、插件、构建或测试行为。（UNVERIFIED）

## 与 OpenProtégé 当前检出的关系

OpenProtégé 当前根提交只含一份简短 README。仓库内未见上游代码、依赖、子模块、历史或来源声明；因而现有证据只能支持：**本次审计检出的 OpenProtégé 尚无可观察到的 Desktop/WebProtégé 集成实现**。（SOURCE-OBSERVED）

“使用成熟本体编辑技术”是项目目标，不是技术集成事实。两个上游的存在、声明功能、构建文件或许可证都不能证明 OpenProtégé 继承/实现了相同能力。（SOURCE-OBSERVED）

## 未执行的验证及约束

下列工作属于后续阶段，不在本次仓库/源码盘点中虚报为通过：

| 验证 | 状态 | 尚缺证据 |
|---|---|---|
| 固定提交完整克隆、记录 commit 日期与源码快照 | UNVERIFIED | 本次使用远端固定 SHA 读取少量文件，未完整克隆上游 |
| 上游构建、测试及其日志/退出码 | UNVERIFIED | 未运行 Maven、Docker 或测试 |
| OWL/RDF 导入—编辑—保存—导出—再加载与保真 | UNVERIFIED | 未准备测试本体或执行产品 |
| Desktop 与 Web 数据交换 | UNVERIFIED | 未配置或运行互操作 PoC |
| Web 身份认证、服务端授权和项目隔离 | UNVERIFIED | 未部署服务或做集成测试 |
| 插件加载/扩展 API 兼容性 | UNVERIFIED | 未检查完整 API，也未运行插件 |
| 全依赖清单、许可证兼容及供应链风险 | UNVERIFIED | 未解析依赖树或扫描许可证 |

后续构建前应重新固定和复核上游提交、按受支持环境实际执行构建/测试，并保存命令、环境、日志和退出码；WebProtégé 还应先评估固定仓库的维护状态与其替代仓库。（PROPOSED）
