package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.util.HashSet;
import java.util.List;
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
 * <li>joined 继承中子表主键即父表主键（外键），子表实体不应重复生成主键字段，
 *     主键的 getter/setter 一并继承自父类，本插件会拦截主键字段及对应方法。</li>
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
     * 当前表生成实体时的父类实体简单名（不含包），未配置继承时为 null。
     * 在 initialized 阶段解析，供后续钩子使用。
     */
    private String parentEntitySimpleName;

    /**
     * 当前表主键列的 actualColumnName 集合，配置了继承时需要拦截这些字段的生成。
     */
    private Set<String> primaryKeyColumnNames;

    @Override
    public boolean validate(List<String> warnings) {
        // 本插件无必填配置，恒有效
        return true;
    }

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        parentEntitySimpleName = null;
        primaryKeyColumnNames = null;

        String extendsEntity = introspectedTable.getTableConfigurationProperty(PROP_EXTENDS_ENTITY);
        if (stringHasValue(extendsEntity)) {
            parentEntitySimpleName = extendsEntity.trim();

            // 记录主键列名，joined 继承中子表主键继承自父类，不需要在子类里重复生成
            primaryKeyColumnNames = new HashSet<>();
            for (IntrospectedColumn pkColumn : introspectedTable.getPrimaryKeyColumns()) {
                primaryKeyColumnNames.add(pkColumn.getActualColumnName());
            }

            System.out.printf("表 %s 配置了实体继承：extends %s%n",
                    introspectedTable.getFullyQualifiedTable().getIntrospectedTableName(),
                    parentEntitySimpleName);
        }
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
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
        if (primaryKeyColumnNames == null) {
            return true;
        }
        // joined 继承：子表主键字段继承自父类，不生成，避免字段遮蔽
        if (primaryKeyColumnNames.contains(introspectedColumn.getActualColumnName())) {
            System.out.printf("跳过主键字段 %s（继承自父类 %s）%n",
                    field.getName(), parentEntitySimpleName);
            return false;
        }
        return true;
    }

    @Override
    public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable,
            ModelClassType modelClassType) {
        return shouldGenerateAccessor(introspectedColumn, modelClassType);
    }

    @Override
    public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass,
            IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable,
            ModelClassType modelClassType) {
        return shouldGenerateAccessor(introspectedColumn, modelClassType);
    }

    /**
     * 判断字段的 getter/setter 是否应该生成。
     * 主键字段已被拦截，其 accessor 也不应生成（继承自父类）。
     */
    private boolean shouldGenerateAccessor(IntrospectedColumn introspectedColumn, ModelClassType modelClassType) {
        if (modelClassType != ModelClassType.BASE_RECORD) {
            return true;
        }
        if (primaryKeyColumnNames == null) {
            return true;
        }
        return !primaryKeyColumnNames.contains(introspectedColumn.getActualColumnName());
    }
}
