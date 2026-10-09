# 审计证据台账

审计日期：2026-10-09
状态词严格使用 `VERIFIED`、`SOURCE-OBSERVED`、`PROPOSED`、`UNVERIFIED`、`BLOCKED`。

| ID | 状态 | 结论 | 证据 / 重现方法 | 限制 |
|---|---|---|---|---|
| E-01 | VERIFIED | 当前分支 `main`，跟踪 `origin/main`，工作区干净。 | `git status --short --branch` 输出 `## main...origin/main`；`git status --porcelain=v1` 无输出；`git branch --show-current` 输出 `main`。 | 仅代表审计执行时的本地检出。 |
| E-02 | VERIFIED | origin 指向项目指定 GitHub 地址；HEAD 为初始提交。 | `git remote -v`；`git --no-pager log -1 --format='%H%n%cI%n%s'` 输出 SHA `d9caafee6858a958ea7a2944d574407a79f88309`、`2026-10-09T13:04:01+08:00`、`Initial commit`。 | GitHub 页面状态未单独审阅。 |
| E-03 | VERIFIED | Git 跟踪树只有 README.md；历史仅一个提交。 | `git ls-tree -r --name-only HEAD`、`git ls-files --stage`、`git --no-pager log -8 --oneline --decorate`。 | Git 忽略文件与 `.git` 内部对象不属于跟踪源码；工作目录盘点见 E-04。 |
| E-04 | SOURCE-OBSERVED | 工作目录没有应用源码、项目清单、已有文档、测试、CI、部署配置；README 是唯一普通项目文件。 | `ls -la`、`find . -maxdepth 4 -type d -not -path './.git*' -print`、限定文件类型的配置检索、`rg` 占位符检索。 | 空缺仅对当前检出成立，不证明其他分支或远端历史。 |
| E-05 | SOURCE-OBSERVED | README 只有项目名和简短产品定位，未记录安装/运行/构建/测试方法。 | 阅读 `README.md`；`git show HEAD:README.md`。 | 产品描述本身不作为功能验证。 |
| E-06 | VERIFIED | 审计环境工具版本为 Git 2.55.0、Java 25.0.4.1、Maven 3.9.16、Node v24.21.0、npm 11.19.0、Python 3.14.2。 | `command -v` 与对应 `--version` 输出。 | 不表示本项目支持这些版本。 |
| E-07 | VERIFIED | Protégé Desktop 的远端 `HEAD`/`master` 解析为 `bf03cccc65ea664e139d6829bba6d209ef58ee55`。 | `git ls-remote https://github.com/protegeproject/protege.git HEAD refs/heads/master refs/heads/main refs/tags/5.6.6`。 | 指针可能后续移动；本文以 SHA 固定。 |
| E-08 | SOURCE-OBSERVED | Desktop 固定提交根目录包含 Maven 多模块、桌面/editor 模块及 `license.txt`；POM 坐标 `edu.stanford.protege:protege-parent:5.6.10-SNAPSHOT`；README 指向 BSD 2-clause。 | GitHub 文件 API 读取固定 SHA 的根目录、`README.md`、`pom.xml`、`license.txt`。 | 未完整克隆、构建、运行、测试或解析完整依赖许可证。 |
| E-09 | VERIFIED | WebProtégé 远端 `HEAD`/`master` 解析为 `1e84fa02aef68be45f18c08dbeae94bec9b04a41`。 | `git ls-remote https://github.com/protegeproject/webprotege.git HEAD refs/heads/master refs/heads/main`。 | 指针可能后续移动；本文以 SHA 固定。 |
| E-10 | SOURCE-OBSERVED | WebProtégé 固定提交根目录含 Maven、多项 client/server/shared 模块及 Docker/Compose 配置；README 声明仓库正在被更细粒度仓库取代；`license.txt` 含 BSD 2-Clause 风格条款。 | GitHub 文件 API 读取固定 SHA 的根目录、`README.md`、`pom.xml`、`license.txt`。 | 未运行构建、容器、测试或服务端安全验证；未审计传递依赖许可证。 |
| E-11 | SOURCE-OBSERVED | OpenProtégé 当前检出没有可观察到的上游代码集成证据。 | E-03 的单文件历史与 E-04 的目录盘点；比较 [upstream-audit.md](./upstream-audit.md) 所固定的上游模块。 | 不对仓库外关系作断言，也不声称已审阅全部上游源码历史。 |
| E-12 | BLOCKED | 无法在本仓库执行应用构建、测试、部署或端到端产品验证。 | 检查未发现源码、构建脚本、测试或部署入口。 | 这是缺少目标物造成的阻塞，并非某个构建命令执行失败。 |
| E-13 | UNVERIFIED | 上游完整构建、测试、互操作、OWL/RDF 往返保真、身份/授权、插件与全依赖许可证兼容性尚未验证。 | [upstream-audit.md](./upstream-audit.md) 中逐项列明。 | 属于后续固定环境的验证工作。 |
| E-14 | PROPOSED | 在引入代码前确认产品范围和许可证；随后建立需求基线及安全/架构决策，再以固定上游版本进行 PoC。 | 由 E-03、E-08、E-10、E-12 推导。 | 建议，不是已批准的设计或已完成工作。 |
| E-15 | VERIFIED（负责人决策） | 负责人确认个人与团队用户、OWL 2 核心及 RDF/XML/Turtle 优先验证、桌面/Web 职责、首期文件交换、自托管/公开私有与角色权限、Apache-2.0 评估、AI 建议/只读检索/写入确认边界。 | 用户消息，2026-10-09；需求记录见 [SRS 第 8 节](../requirements/SRS.md#8-需求基线决策与技术决策分层)。 | 仅验证决策已确认，不验证技术实现、可行性、许可证兼容或功能存在。 |
| E-16 | SOURCE-OBSERVED | 需求基线起草时 HEAD 仍为根提交 `d9caafee…`，只跟踪 `README.md`；现有工程审计文档为未跟踪用户工作并已保留。 | `git status --short --branch`、`git rev-parse HEAD`、`git ls-files` 和目录枚举；本轮审计开始时执行。 | 仅描述本地检出与本轮开始时工作区。 |

## 本次执行命令记录

```text
git status --short --branch
git branch --show-current
git --no-pager log -1 --format='%H%n%cI%n%s'
git --no-pager log -8 --oneline --decorate
git remote -v
git status --porcelain=v1
git show --no-patch --format=fuller HEAD
git ls-tree -r --name-only HEAD
git ls-files --stage
git count-objects -v
git show HEAD:README.md
ls -la
find . -maxdepth 4 -type d -not -path './.git*' -print
git ls-remote https://github.com/protegeproject/protege.git HEAD refs/heads/master refs/heads/main refs/tags/5.6.6
git ls-remote https://github.com/protegeproject/webprotege.git HEAD refs/heads/master refs/heads/main
```

上游文件通过 GitHub 文件接口按 E-07/E-09 固定 SHA 查询。未执行写入代码的命令；没有运行构建或测试。
