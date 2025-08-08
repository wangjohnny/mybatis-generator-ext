package com.github.mybatis.generator.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 *
 * @author lao king
 */
public class SwaggerAnnotationPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelFieldGenerated(Field field,
                                     TopLevelClass topLevelClass,
                                     IntrospectedColumn introspectedColumn,
                                     IntrospectedTable introspectedTable,
                                     ModelClassType modelClassType) {

        // 获取数据库字段注释
        String remarks = introspectedColumn.getRemarks();
        
        // 如果字段有注释，则添加@Schema注解
        if (remarks != null && !remarks.isEmpty()) {
            // 同时设置title和description
            field.addAnnotation(String.format(
                "@Schema(title = \"%s\", description = \"%s\")", 
                remarks, remarks));
        } else {
            field.addAnnotation("@Schema");
        }
        
        // 确保导入了Schema注解类
        topLevelClass.addImportedType("io.swagger.v3.oas.annotations.media.Schema");
        
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,
                                               IntrospectedTable introspectedTable) {
        // 在类级别添加@Schema注解，注解内容转移到子类
//        String tableRemarks = introspectedTable.getRemarks();
//        if (tableRemarks != null && !tableRemarks.isEmpty()) {
//            topLevelClass.addAnnotation(String.format("@Schema(title = \"%s\", description = \"%s\")", 
//                    tableRemarks, tableRemarks));
//        } else {
//            topLevelClass.addAnnotation("@Schema");
//        }
//        
//        topLevelClass.addImportedType("io.swagger.v3.oas.annotations.media.Schema");
        return true;
    }
}