# 中文端到端开发场景：社区健康服务本体

本场景用虚构的社区健康/教育服务数据演示开发版 Web 后端的邀请、角色、项目、本体文件和推理 API。示例文件为 [zh-care-ontology.ttl](./zh-care-ontology.ttl)，自动化流程为 [run_business_scenario.py](./run_business_scenario.py)。

> 该脚本会在当前数据库中创建团队、私有项目、4 个受邀账号和本体版本；它不会清理数据。请仅对本机开发数据库或一次性验收数据库运行，不要对生产或含真实数据的数据库运行。每次执行都会新建一组记录。

## 前置条件

1. 按[开发与部署指南](../development-and-deployment.md)完成 Java 21、Maven、Node.js 和本机 PostgreSQL/MySQL 配置。
2. 启动后端及本机数据库；前端可另用 `npm --prefix web run dev` 启动。后端默认地址为 `http://127.0.0.1:8080`。
3. 数据库中已有可登录的平台管理员。首次启动的管理员凭证取自 `.env` 的 `ADMIN_BOOTSTRAP_EMAIL` 和 `ADMIN_BOOTSTRAP_PASSWORD`。
4. 安装 Python 3.10+（只使用标准库，无需 pip 安装第三方包）。

仓库根目录执行以下命令。请在当前终端设置管理员环境变量，不要把真实密码写入脚本或提交到 Git：

```sh
export ADMIN_BOOTSTRAP_EMAIL='替换为本机管理员邮箱'
export ADMIN_BOOTSTRAP_PASSWORD='替换为本机管理员密码'
export DEMO_MEMBER_PASSWORD='本机演示账号专用密码（至少 12 个字符）'
python3 docs/guides/examples/run_business_scenario.py \
  --ontology docs/guides/examples/zh-care-ontology.ttl \
  --team-name '社区健康服务团队' \
  --project-name '社区健康知识模型' \
  --iris 'https://example.org/openprotege/zh-care' \
  --individual lin
```

Windows PowerShell：

```powershell
$env:ADMIN_BOOTSTRAP_EMAIL = '替换为本机管理员邮箱'
$env:ADMIN_BOOTSTRAP_PASSWORD = '替换为本机管理员密码'
$env:DEMO_MEMBER_PASSWORD = '本机演示账号专用密码（至少 12 个字符）'
python docs/guides/examples/run_business_scenario.py `
  --ontology docs/guides/examples/zh-care-ontology.ttl `
  --team-name '社区健康服务团队' `
  --project-name '社区健康知识模型' `
  --iris 'https://example.org/openprotege/zh-care' `
  --individual lin
```

脚本默认连接 `http://127.0.0.1:8080`；如服务地址不同，可设置 `OPENPROTEGE_BASE_URL` 或传 `--base-url`。成功时以 `PASS` 结束并列出新建记录 ID、各角色演示邮箱；四个演示账号均使用你设置的 `DEMO_MEMBER_PASSWORD`，可据输出登录 Web 前端检查页面。请勿复用管理员密码或真实账户密码。非预期 HTTP 状态、网络失败或推理结果不符会以非零状态和明确错误退出。

## 业务与权限路径

| 身份 | 团队角色 | 项目角色 | 本场景允许的操作 |
|---|---|---|---|
| 本机平台管理员 | 团队 `OWNER` | 项目 `OWNER` | 创建团队和私有项目，邀请/分配成员，完整管理 |
| `team-admin-*` | `ADMIN` | `ADMIN` | 查看团队成员、查看/管理项目成员、修改项目元数据 |
| `editor-*` | `MEMBER` | `EDITOR` | 导入本体、恢复历史版本为新快照；不能管理项目成员或修改项目元数据 |
| `viewer-*` | `MEMBER` | `VIEWER` | 查看项目和导出本体；不能导入/恢复或管理成员 |
| `team-only-*` | `MEMBER` | 未加入 | 虽是团队成员，但私有项目仍不可见；需显式项目成员资格 |

平台管理员签发一次性邀请，演示脚本在本机直接使用响应中的 token 完成受邀账号激活。产品当前不发送邮件，生产环境必须通过安全渠道分发邀请 token。项目成员必须先属于团队，再被显式加入团队项目。

脚本依次执行并断言：

1. 管理员登录并创建团队，发出四个一次性邀请并激活账号。
2. 管理员设置团队角色、创建私有团队项目，并指派项目 `ADMIN`、`EDITOR`、`VIEWER`。
3. 验证团队成员但未加入项目的账号收到私有项目拒绝；Editor/Viewer 不能管理项目成员；Editor 不能更改项目元数据。
4. Editor 导入 Turtle 本体；Viewer 导出原始 Turtle 并验证内容与导入文件一致；Viewer 导入被拒绝。
5. Viewer 使用 HermiT 检查 OWL 2 DL profile/一致性，并验证 `Student` 个体可推导为 `Person`。
6. 对指定版本预览 SWRL 规则 `Student(?x) -> ActiveStudent(?x)`，验证推理类型；该操作不修改本体版本。
7. Team Admin 修改项目元数据；Editor 将源版本恢复为一个新的不可变快照；比较恢复导出与源文件，并验证历史记录共两版。

## Web 前端操作与已知边界

前端位于 `http://localhost:5173`，可登录、浏览团队/项目、创建项目、设置项目成员，以及导入/浏览/导出/恢复本体并查看 Reasoner/Rules 面板。可用脚本创建的项目 ID 在脚本输出中，之后可从前端项目列表打开。Owner/Admin/Editor 的本体写操作和 Viewer 只读由服务端执行。

自动脚本直接驱动 HTTP API，而不是浏览器；因此它验证真实服务端业务链路，但**不等同于浏览器 E2E 验收**，也不证明前端所有角色的控件可见性/可用性。浏览器 E2E、邀请邮件交付、账号停用/撤销、实体级本体编辑、并发冲突、500 MiB 压力和推理超时/并发边界仍未验收。若业务流程在本机数据库、前端或当前配置上失败，请保留完整报错和环境信息，并按[已知缺口](../../engineering/known-gaps.md)记录；不要将失败描述为已通过。
