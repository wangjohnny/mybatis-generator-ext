package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * 添加 lombok 注解
 * 
 * @author lao king
 */
public class LombokAnnotationPlugin extends PluginAdapter {
    
    private boolean enableBuilder;

    @Override
    public boolean validate(List<String> warnings) {
        enableBuilder = Boolean.parseBoolean(properties.getProperty("enableBuilder", "true"));
        return true;
    }
    
    @Override
    public boolean modelGetterMethodGenerated(Method method, 
                                            TopLevelClass topLevelClass,
                                            IntrospectedColumn introspectedColumn, 
                                            IntrospectedTable introspectedTable,
                                            ModelClassType modelClassType) {
        // 阻止生成getter方法
        return false;
    }

    @Override
    public boolean modelSetterMethodGenerated(Method method, 
                                            TopLevelClass topLevelClass,
                                            IntrospectedColumn introspectedColumn, 
                                            IntrospectedTable introspectedTable,
                                            ModelClassType modelClassType) {
        // 阻止生成setter方法
        return false;
    }
    
    @Override
    public boolean modelFieldGenerated(Field field,
                                     TopLevelClass topLevelClass,
                                     IntrospectedColumn introspectedColumn,
                                     IntrospectedTable introspectedTable,
                                     ModelClassType modelClassType) {

        // 主键字段参与 equals 与 hashCode，按主键列判断，不依赖固定的主键列名(sid / id 均可)
        if (isPrimaryKeyColumn(introspectedColumn, introspectedTable)) {
            field.addAnnotation("@EqualsAndHashCode.Include");
        }

        return true;
    }

    /**
     * 判断字段对应的列是否为主键列
     *
     * @param column 字段对应的列
     * @param introspectedTable 表的元数据
     * @return 是主键列返回 true
     */
    private boolean isPrimaryKeyColumn(IntrospectedColumn column, IntrospectedTable introspectedTable) {
        String columnName = column.getActualColumnName();
        return introspectedTable.getPrimaryKeyColumns().stream()
                .anyMatch(pkColumn -> pkColumn.getActualColumnName().equals(columnName));
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,
                                               IntrospectedTable introspectedTable) {
        // 添加 @Data 注解
        topLevelClass.addAnnotation("@Data");
        topLevelClass.addImportedType("lombok.Data");

        topLevelClass.addAnnotation("@ToString");
        topLevelClass.addImportedType("lombok.ToString");

        List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();

        // 实体配置了 extendsEntity（joined 继承，有业务父类），或为联合主键时，
        // equals/hashCode 必须调用父类（callSuper=true），否则继承自父类的字段不参与比较
        boolean hasParentEntity = stringHasValue(
                introspectedTable.getTableConfigurationProperty("extendsEntity"));
        if (hasParentEntity || primaryKeyColumns.size() >= 2) {
            topLevelClass.addAnnotation("@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)");
        } else {
            topLevelClass.addAnnotation("@EqualsAndHashCode(onlyExplicitlyIncluded = true)");
        }
        topLevelClass.addImportedType("lombok.EqualsAndHashCode");

        topLevelClass.addAnnotation("@NoArgsConstructor");
        topLevelClass.addImportedType("lombok.NoArgsConstructor");

//        topLevelClass.addAnnotation("@AllArgsConstructor");
//        topLevelClass.addImportedType("lombok.AllArgsConstructor");

        if (enableBuilder) {
            topLevelClass.addAnnotation("@SuperBuilder");
            topLevelClass.addImportedType("lombok.experimental.SuperBuilder");
        }

        return true;
    }
    
    /**
     * 给存在联合主键 Key 类，添加lombok
     */
    @Override
    public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {

        topLevelClass.addAnnotation("@Data");
        topLevelClass.addImportedType("lombok.Data");

        topLevelClass.addAnnotation("@ToString");
        topLevelClass.addImportedType("lombok.ToString");

        // 联合主键 Key 类的每个字段都是主键，modelFieldGenerated 会给它们加 @EqualsAndHashCode.Include，
        // 因此这里必须补上 @EqualsAndHashCode 类级注解（及 import），否则编译报「程序包 EqualsAndHashCode 不存在」
        topLevelClass.addAnnotation("@EqualsAndHashCode(onlyExplicitlyIncluded = true)");
        topLevelClass.addImportedType("lombok.EqualsAndHashCode");

        topLevelClass.addAnnotation("@NoArgsConstructor");
        topLevelClass.addImportedType("lombok.NoArgsConstructor");

        topLevelClass.addAnnotation("@SuperBuilder");
        topLevelClass.addImportedType("lombok.experimental.SuperBuilder");

        return true;
    }
}