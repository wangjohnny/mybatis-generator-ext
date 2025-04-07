package com.github.mybatis.generator.plugin;

import java.sql.Types;
import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * 添加 jakarta.validation 的注解
 * 
 * @author Johnny
 */
public class ModelValidationPlugin extends PluginAdapter {

    @Override
    public boolean modelFieldGenerated(Field field, 
            TopLevelClass topLevelClass, 
            IntrospectedColumn introspectedColumn,
            IntrospectedTable introspectedTable, 
            ModelClassType modelClassType) {
        
        // 遇到 sid 或者 sid 结尾的字段，不要添加 validation
        String columnName = introspectedColumn.getActualColumnName();
        if (columnName.equalsIgnoreCase("sid") || columnName.toUpperCase().endsWith("_sid")) {
            return true;
        }

        // 添加必要的import
        addImports(topLevelClass);

        // 非空检查
        if (!introspectedColumn.isNullable()) {
            field.addAnnotation("@NotNull(message = \"" + field.getName() + "不能为空\")");
        }

        // 字符串长度检查
        if (introspectedColumn.isStringColumn()) {
            int length = introspectedColumn.getLength();
            field.addAnnotation(
                    String.format("@Size(max = %d, message = \"%s长度不能超过%d个字符\")", length, field.getName(), length));
        }

        // 数字类型检查
        if (isNumberColumn(introspectedColumn)) {
            field.addAnnotation("@Digits(integer = 19, fraction = 0, message = \"必须是有效数字\")");
        }

        // 特定字段特殊验证
        if ("email".equalsIgnoreCase(introspectedColumn.getActualColumnName())) {
            field.addAnnotation("@Email(message = \"邮箱格式不正确\")");
        }

        return true;
    }

    private void addImports(TopLevelClass topLevelClass) {
        // 只添加一次所需的import
        if (!topLevelClass.getImportedTypes()
                .contains(new FullyQualifiedJavaType("jakarta.validation.constraints.NotNull"))) {
            topLevelClass.addImportedType("jakarta.validation.constraints.NotNull");
        }
        if (!topLevelClass.getImportedTypes()
                .contains(new FullyQualifiedJavaType("jakarta.validation.constraints.Size"))) {
            topLevelClass.addImportedType("jakarta.validation.constraints.Size");
        }
        if (!topLevelClass.getImportedTypes()
                .contains(new FullyQualifiedJavaType("jakarta.validation.constraints.Digits"))) {
            topLevelClass.addImportedType("jakarta.validation.constraints.Digits");
        }
        if (!topLevelClass.getImportedTypes()
                .contains(new FullyQualifiedJavaType("jakarta.validation.constraints.Email"))) {
            topLevelClass.addImportedType("jakarta.validation.constraints.Email");
        }
    }

    private boolean isNumberColumn(IntrospectedColumn column) {
        int jdbcType = column.getJdbcType();
        // @formatter:off
        return jdbcType == Types.BIT ||
               jdbcType == Types.TINYINT ||
               jdbcType == Types.SMALLINT ||
               jdbcType == Types.INTEGER ||
               jdbcType == Types.BIGINT ||
               jdbcType == Types.FLOAT ||
               jdbcType == Types.REAL ||
               jdbcType == Types.DOUBLE ||
               jdbcType == Types.NUMERIC ||
               jdbcType == Types.DECIMAL;
        // @formatter:on
    }

//    private String getFieldName(Field field) {
//        String name = field.getName();
//        return name.substring(0, 1).toUpperCase() + name.substring(1);
//    }

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }
}