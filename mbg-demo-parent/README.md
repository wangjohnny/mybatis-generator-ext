# mbg-demo-parent

[mybatis-generator-ext](https://github.com/wangjohnny/mybatis-generator-ext) 的示例工程，演示「生成的不修改，修改的不生成」的代码生成方式，以及 `EntityInheritancePlugin` 的实体继承能力。

## 工程结构

按 maven 常见的目录分类拆分：

| 模块 | 作用 |
|---|---|
| mbg-demo-model | 生成的实体（`generated` 子包 + 手写扩展子类） |
| mbg-demo-mapper | Mapper 接口 + XML 映射文件 |
| mbg-demo-business | Service 层 |
| mbg-demo-webapp | Web 入口 |
| mbg-demo-generator | 代码生成器（命名模式：`项目名-generator`） |

> 小项目也可以不拆这么多模块，全部放一个工程里，工具同样支持。

## 快速开始

1. 初始化数据库（demo 用 HSQLDB 文件库，无需单独安装）：
   ```bash
   # 见下文「HSQLDB 初始化」
   ```
2. 生成代码：
   ```bash
   cd mbg-demo-generator
   ./mybatis-generator.sh
   ```
3. 启动 web 演示：
   ```bash
   cd mbg-demo-webapp
   ./run.sh
   # 访问 http://localhost:8080/demo/user/list
   ```

## 关键配置

| 文件 | 说明 |
|---|---|
| `generatorConfig.properties` | 数据库连接信息、代码生成路径 |
| `generatorConfig.xml` | MBG 标准配置，插件装配、表清单都在这里 |

## EntityInheritancePlugin 继承 demo

用 `sales_order`、`import_order`、`export_order` 三张表演示类似 Hibernate joined 继承的实体继承关系：

- 父表 `sales_order`，子表 `import_order`、`export_order`，子表主键即父表主键（joined 外键）
- `export_order` 进一步继承 `import_order`，形成三级继承链

在 `generatorConfig.xml` 里通过 `<table>` 的 `extendsEntity` 属性声明继承关系：

```xml
<table tableName="sales_order"></table>
<table tableName="import_order">
    <property name="extendsEntity" value="BaseSalesOrder"/>
</table>
<table tableName="export_order">
    <property name="extendsEntity" value="BaseImportOrder"/>
</table>
```

生成的实体继承链（SQL 由自己写，插件只负责实体继承关系）：

```
generated.BaseSalesOrder                                  id / orderNo / status / createdDatetime
  └─ generated.BaseImportOrder extends BaseSalesOrder      importType / customsNo
        └─ generated.BaseExportOrder extends BaseImportOrder   exportType / destination
```

每个子类只保留自己表的独有字段，父类已有的字段（主键 + 业务列，按数据库列名比对）全部继承、不重复生成。最终再由子类机制产出 `SalesOrder`、`ImportOrder`、`ExportOrder` 作为手写扩展层。

### filterDuplicateFields 开关

插件级配置 `filterDuplicateFields`（默认 `true`）控制去重策略：

```xml
<plugin type="com.github.mybatis.generator.plugin.EntityInheritancePlugin">
    <property name="filterDuplicateFields" value="false"/>
</plugin>
```

| 值 | 行为 |
|---|---|
| `true`（默认） | 过滤父类**全部**字段（主键 + 业务列），标准 joined 继承 |
| `false` | 只过滤**主键**，父类里重名的普通业务列仍留在子类（子类重新定义） |

## HSQLDB 初始化

demo 用 HSQLDB 文件库（`mbg-demo-mapper/src/main/resources/db/demo`），表结构定义在 `schema.sql`。

> `schema.sql` 是 MySQL 语法，HSQLDB 不直接兼容（反引号、`COMMENT` 等），初始化前需先做语法清洗。建议固化一个初始化脚本（如 `init-hsql.sh`），核心逻辑：
> 1. 去掉反引号 `` ` ``
> 2. 去掉 `COMMENT 'xxx'` 子句
> 3. 若表名是 SQL 保留字（如 `order`），用双引号 + 大写包裹（HSQLDB 双引号保留大小写，大写才能匹配 MBG 内省）

## 体验「生成的不修改，修改的不生成」

- 修改子类（Model 与 Mapper）后重新生成 —— 子类不会被覆盖
- 删除生成的代码（Model 与 Mapper）后重新生成 —— 会重新产出

## 注意事项

- demo 默认使用 HSQLDB，可换成任意数据库（分页插件目前只支持 MySQL 与 HSQLDB，其他数据库需自行适配）
- 命令脚本均在 macOS 下编写，Linux 一般无需修改，Windows 可能需要调整
