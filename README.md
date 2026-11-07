# mybatis-generator-ext

针对 [MyBatis Generator](https://github.com/mybatis/generator)（下文简称 MBG）的扩展项目，核心思路是「**生成的不修改，修改的不生成**」。

## 问题与思路

直接使用 MBG 时，一旦修改数据库结构就要重新生成 Mapper 与 Model 类。但生成后的代码往往已被手写过（加字段、加方法、改 SQL），重新生成会把改动覆盖掉。

本工具利用 MyBatis 的两个特点来化解这个矛盾：

- **一个表可以有多个 XML 映射文件**
- **Model 类可以继承**

通过 MBG 的 plugin 机制改造生成结果，最终每张表产出「可覆盖」与「不可覆盖」两组文件，让生成与手写各归其位：

| 类别 | 文件 | 是否覆盖 |
|---|---|---|
| Model | `BaseXxx`（含 `BaseXxxExample`） | 覆盖 |
| Model | `Xxx`（含 `XxxExample`） | 不覆盖 |
| Mapper | `XxxMapper` 接口 | 不覆盖 |
| XML | `generated/XxxMapper.xml` | 覆盖 |
| XML | `XxxMapper.xml`（扩展） | 不覆盖 |

## 用 user 表举例

1. 生成两个 Model 类：`BaseUser` 与 `User`，`User` 继承 `BaseUser`，`BaseUser` 继承 `BaseModel`。
   - `BaseModel` 由 `mybatis-model` 模块提供，包含几个基础字段。
   - `BaseUser` 是 MBG 生成、通过 plugin 改名得到的基类，重新生成时会被覆盖。
   - `User` 是留给程序员手写的子类，可加属性、方法，重新生成时不覆盖。
2. 两个 XML 文件名相同，放在不同目录：`generated/UserMapper.xml`（生成、覆盖）与 `UserMapper.xml`（扩展、不覆盖）。

**注意：暂未考虑联合主键。**

## 模块结构

| 模块 | 说明 |
|---|---|
| `mybatis-pagination` | `Pagination<T>` 分页对象 |
| `mybatis-model` | `BaseModel<PK>`、`BaseModelExample`、`BaseCriteria`、`Criterion` 基础模型 |
| `mybatis-mapper` | `GenericMapper<T, TE, PK>` 通用 Mapper 接口 |
| `mybatis-service` | `GenericService` / `GenericServiceImpl` 通用 Service |
| `mybatis-generator-plugin` | MBG 插件集合（核心） |
| `mbg-demo-parent` | 示例工程（未纳入根 pom modules） |

## 插件清单（mybatis-generator-plugin）

| 插件 | 说明 |
|---|---|
| `ModelAndExampleBaseClassPlugin` | 核心：改写类型名为 `generated.BaseXxx`，分离 XML 路径与 namespace |
| `ModelAndExampleSubClassPlugin` | 生成手写扩展子类 |
| `EntityInheritancePlugin` | 实体 joined 继承（`extendsEntity`） |
| `MapperPlugin` | 生成 Mapper 接口 |
| `MysqlPaginationPlugin` | 分页（目前支持 MySQL 与 HSQLDB） |
| `LombokAnnotationPlugin` | 生成 Lombok 注解 |
| `JacksonAnnotationPlugin` | 生成 Jackson 注解 |
| `SwaggerAnnotationPlugin` | 生成 Swagger 注解 |
| `EqualsHashCodePlugin` | equals/hashCode |
| `ModelValidationPlugin` | 模型校验 |
| `BooleanColumnPlugin` | 布尔列处理 |
| `ColumnDynamicAliasAndPrefixPlugin` | 动态别名/前缀 |
| `CustomJavaTypeResolver` | 自定义 Java 类型解析 |

> 具体启用哪些插件，在 `generatorConfig.xml` 里自行装配。

## 命名约定

- 用 `generated` 包名/目录名统一标记「工具生成、会被覆盖」的产物，与手写子类分开：
  - Model 基类：`modelTargetPackage.generated.BaseUser`
  - 生成 XML：`xmlTargetPackage.generated/UserMapper.xml`
- 类名前缀 `Base` 不变，包名用 `generated` 标记。
- 扩展 XML 与 `generated` 目录同级：`mapper/generated/UserMapper.xml`（生成）与 `mapper/UserMapper.xml`（扩展）。

## 快速开始

完整示例见 [mbg-demo-parent](https://github.com/wangjohnny/mbg-demo-parent)（或本仓库 `mbg-demo-parent` 目录），演示了基本生成与 `EntityInheritancePlugin` 的实体继承能力。

## 构建

```bash
# 必须使用 JDK 21
JAVA_HOME=$HOME/.sdkman/candidates/java/21.0.10-oracle mvn clean install
```

## 示例代码

[mybatis generator demo](https://github.com/wangjohnny/mbg-demo-parent)
