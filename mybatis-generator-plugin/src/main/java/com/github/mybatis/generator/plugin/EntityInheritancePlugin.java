package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * 实现类似 Hibernate joined 继承的实体继承关系。
 * <p>
 * 通过在 {@code <table>} 上配置 {@code extendsEntity} 属性，指定当前表生成的实体类要继承的父类实体。
 * 典型场景：父表 {@code inbound_order} 生成 {@code generated.BaseInboundOrder}，
 * 子表 {@code import_order} 配置 {@code extendsEntity=BaseInboundOrder} 后，
 * 生成的 {@code generated.BaseImportOrder} 会 {@code extends BaseInboundOrder}。
 * <p>
 * 用法（generatorConfig.xml）：
 * <pre>
 * &lt;table tableName="inbound_order"&gt;&lt;/table&gt;
 * &lt;table tableName="import_order"&gt;
 *     &lt;property name="extendsEntity" value="BaseInboundOrder"/&gt;
 * &lt;/table&gt;
 * </pre>
 * <p>
 * 约定：
 * <ul>
 * <li>joined 继承中子表主键即父表主键（外键），父类实体里的全部字段（主键 + 业务列）
 *     都不该在子类里重复生成，本插件按数据库列名（actualColumnName）比对，
 *     过滤掉父类已有的字段及其 getter/setter，避免字段遮蔽。</li>
 * <li>父类实体与子类实体必须在同一个 generated 包下（两者都走
 *     {@link ModelAndExampleBaseClassPlugin} 的 generated.Base 命名）。</li>
 * <li>只处理实体类（baseRecordClass），不处理 Example / Key 类。</li>
 * </ul>
 *
 * @author oking
 */
public class EntityInheritancePlugin extends PluginAdapter {

    /** 在 {@code <table>} 上声明父类实体名的 property key */
    private static final String PROP_EXTENDS_ENTITY = "extendsEntity";

    /**
     * 跨表共享的「实体简单名 → 该实体的全部字段列名」缓存。
     * <p>
     * MBG 对同一个 context 里的所有表复用同一个插件实例，且所有表的 initialize()
     * 会先于字段生成阶段全部执行完，因此这里可以安全地在 initialize 阶段注册所有表的字段，
     * 在字段生成阶段按父类字段做全量去重，不存在顺序依赖。
     */
    private final Map<String, Set<String>> entityColumnNamesRegistry = new HashMap<>();

    /**
     * 「表 → 该表实体要继承的父类实体简单名」。
     * <p>
     * MBG 的生成是「先对所有表 initialize，再逐表生成字段」两阶段，钩子里拿到的
     * introspectedTable 是当前正在处理的表。用 introspectedTable 做 key 取状态，
     * 避免用单实例字段承载当前表状态而在多表间串值。
     */
    private final Map<IntrospectedTable, String> parentEntityByTable = new HashMap<>();

    @Override
    public boolean validate(List<String> warnings) {
        // 本插件无必填配置，恒有效
        return true;
    }

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        // 记录当前表生成的实体简单名。baseRecordType 此时已被
        // ModelAndExampleBaseClassPlugin 改写为 xxx.generated.BaseXxx，取短名作为注册 key
        String baseRecordType = introspectedTable.getBaseRecordType();
        if (stringHasValue(baseRecordType)) {
            int dot = baseRecordType.lastIndexOf('.');
            String entitySimpleName = baseRecordType.substring(dot + 1);

            // 注册本表实体的全部字段列名，供后续作为父类时被子表查询
            Set<String> ownColumnNames = new HashSet<>();
            for (IntrospectedColumn column : introspectedTable.getAllColumns()) {
                ownColumnNames.add(column.getActualColumnName());
            }
            entityColumnNamesRegistry.put(entitySimpleName, ownColumnNames);
        }

        String extendsEntity = introspectedTable.getTableConfigurationProperty(PROP_EXTENDS_ENTITY);
        if (stringHasValue(extendsEntity)) {
            parentEntityByTable.put(introspectedTable, extendsEntity.trim());
            System.out.printf("表 %s 配置了实体继承：extends %s%n",
                    introspectedTable.getFullyQualifiedTable().getIntrospectedTableName(),
                    extendsEntity.trim());
        }
    }

    /**
     * 取当前表要继承的父类实体简单名，未配置继承则返回 null。
     */
    private String parentEntityOf(IntrospectedTable introspectedTable) {
        return parentEntityByTable.get(introspectedTable);
    }

    /**
     * 取父类实体的全部字段列名；父类未注册（理论上不应发生）时返回空集，退化为不过滤。
     */
    private Set<String> parentFieldColumnsOf(IntrospectedTable introspectedTable) {
        String parentName = parentEntityOf(introspectedTable);
        if (parentName == null) {
            return null;
        }
        Set<String> columns = entityColumnNamesRegistry.get(parentName);
        return columns != null ? columns : Set.of();
    }

    /**
     * 判断给定列是否需要在子类里被过滤：是父类已有的字段（按列名比对）。
     * 主键字段必然包含在父类字段集合中（joined 继承主键即父表主键）。
     */
    private boolean isInheritedField(IntrospectedTable introspectedTable, String actualColumnName) {
        Set<String> parentColumns = parentFieldColumnsOf(introspectedTable);
        if (parentColumns == null) {
            return false;
        }
        return parentColumns.contains(actualColumnName);
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        String parentEntitySimpleName = parentEntityOf(introspectedTable);
        if (parentEntitySimpleName == null) {
            // 父表或普通表，不处理
            return true;
        }

        // 父类实体与子类实体同在 generated 包，取当前类所在包拼接父类简单名
        String currentPackage = topLevelClass.getType().getPackageName();
        FullyQualifiedJavaType parentType = new FullyQualifiedJavaType(currentPackage + "." + parentEntitySimpleName);

        topLevelClass.addImportedType(parentType);
        topLevelClass.setSuperClass(parentType);

        System.out.printf("实体 %s extends %s%n", topLevelClass.getType().getShortName(), parentEntitySimpleName);

        return true;
    }

    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass,
            IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable,
            ModelClassType modelClassType) {
        // 只处理主记录类（baseRecordClass），Example / 主键 Key 类不受继承影响
        if (modelClassType != ModelClassType.BASE_RECORD) {
            return true;
        }
        // joined 继承：父类已有的字段（主键 + 业务列）全部继承自父类，不重复生成，避免字段遮蔽
        if (isInheritedField(introspectedTable, introspectedColumn.getActualColumnName())) {
            System.out.printf("跳过字段 %s（继承自父类 %s）%n",
                    field.getName(), parentEntityOf(introspectedTable));
            return false;
        }
        return true;
    }

    @Override
    public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable,
            ModelClassType modelClassType) {
        return shouldGenerateAccessor(introspectedColumn, introspectedTable, modelClassType);
    }

    @Override
    public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable,
            ModelClassType modelClassType) {
        return shouldGenerateAccessor(introspectedColumn, introspectedTable, modelClassType);
    }

    /**
     * 判断字段的 getter/setter 是否应该生成。
     * 被过滤的继承字段（主键 + 父类业务字段）其 accessor 也不应生成，继承自父类。
     */
    private boolean shouldGenerateAccessor(IntrospectedColumn introspectedColumn,
            IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        if (modelClassType != ModelClassType.BASE_RECORD) {
            return true;
        }
        return !isInheritedField(introspectedTable, introspectedColumn.getActualColumnName());
    }
}
