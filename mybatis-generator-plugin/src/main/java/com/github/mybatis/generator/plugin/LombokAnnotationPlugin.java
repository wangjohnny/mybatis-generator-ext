package com.github.mybatis.generator.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * 添加注解
 * @author lao king
 */
public class LombokAnnotationPlugin extends PluginAdapter {
    
    private boolean enableBuilder;
//    
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

        String fieldName = field.getName();
        if (fieldName.equals("sid")) {
            field.addAnnotation("@EqualsAndHashCode.Include");
        }

        return true;
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
        if (primaryKeyColumns.size() < 2) {
            topLevelClass.addAnnotation("@EqualsAndHashCode(onlyExplicitlyIncluded = true)");            
        } else {
            // 联合主键，需要添加继承关系
            topLevelClass.addAnnotation("@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)");
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

        topLevelClass.addAnnotation("@NoArgsConstructor");
        topLevelClass.addImportedType("lombok.NoArgsConstructor");

        topLevelClass.addAnnotation("@SuperBuilder");
        topLevelClass.addImportedType("lombok.experimental.SuperBuilder");

        return true;
    }
}