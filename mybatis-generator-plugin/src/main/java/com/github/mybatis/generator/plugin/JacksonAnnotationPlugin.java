package com.github.mybatis.generator.plugin;

import java.util.List;
import java.util.Set;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * 添加 Jackson 注解
 * 
 * @author lao king
 */
public class JacksonAnnotationPlugin extends PluginAdapter {
    
    private Set<String> ignoreColumnsForSerialization;
    
    @Override
    public boolean validate(List<String> warnings) {
        String ignoreColumns = properties.getProperty("ignoreColumnsForSerialization", "");
        ignoreColumnsForSerialization = Set.of(ignoreColumns.split(","));
        System.out.printf("需要忽略的字段：%s%n", ignoreColumnsForSerialization);

        return true;
    }

    @Override
    public boolean modelFieldGenerated(Field field,
                                     TopLevelClass topLevelClass,
                                     IntrospectedColumn introspectedColumn,
                                     IntrospectedTable introspectedTable,
                                     ModelClassType modelClassType) {

        String ignoredColumn = introspectedColumn.getActualColumnName();
        boolean ignored = ignoreColumnsForSerialization.stream()
                .anyMatch(s -> {
                    return s.equalsIgnoreCase(ignoredColumn);
                });

        if (ignored) {
            System.out.printf("序列化时忽略字段: %s%n", ignoredColumn);

            field.addAnnotation("@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)");
            topLevelClass.addImportedType("com.fasterxml.jackson.annotation.JsonProperty");
        } else {
            field.addAnnotation("@JsonView(BasicView.Public.class)");
            topLevelClass.addImportedType("com.fasterxml.jackson.annotation.JsonView");
        }

        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,
                                               IntrospectedTable introspectedTable) {

        List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
        if (primaryKeyColumns.size() < 2) {
            addInnerClass(topLevelClass);
        } else {
            // 联合主键，需要把 View 类的代码添加到父类 Key 文件里
//            addInnerClass(topLevelClass);
        }

        return true;
    }

    /**
     * 给存在联合主键 Key 类，添加代码
     */
    @Override
    public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        addInnerClass(topLevelClass);
        return true;
    }
    
    private void addInnerClass(TopLevelClass topLevelClass) {
        InnerClass basicViewClass = new InnerClass("BasicView");
        basicViewClass.setVisibility(JavaVisibility.PUBLIC);
        basicViewClass.setStatic(true);
        
        InnerClass publicViewClass = new InnerClass("Public");
        publicViewClass.setVisibility(JavaVisibility.PUBLIC);
        publicViewClass.setStatic(true);
        
        InnerClass internalViewClass = new InnerClass("Internal");
        internalViewClass.setSuperClass(publicViewClass.getType());
        internalViewClass.setVisibility(JavaVisibility.PUBLIC);
        internalViewClass.setStatic(true);

        basicViewClass.addInnerClass(publicViewClass);
        basicViewClass.addInnerClass(internalViewClass);
        
        topLevelClass.addInnerClass(basicViewClass);
    }
    
}