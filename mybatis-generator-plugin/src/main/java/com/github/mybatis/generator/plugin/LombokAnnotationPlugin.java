package com.github.mybatis.generator.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * 添加注解
 * @author lao king
 */
public class LombokAnnotationPlugin extends PluginAdapter {
    
//    private boolean enableLombok;
//    private boolean enableSwagger;
//    private boolean enableBuilder;
//    
    @Override
    public boolean validate(List<String> warnings) {
//        enableLombok = Boolean.parseBoolean(properties.getProperty("enableLombok", "true"));
//        enableSwagger = Boolean.parseBoolean(properties.getProperty("enableSwagger", "true"));
//        enableValidation = Boolean.parseBoolean(properties.getProperty("enableValidation", "true"));
//        enableBuilder = Boolean.parseBoolean(properties.getProperty("enableBuilder", "false"));
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

        topLevelClass.addAnnotation("@SuperBuilder");
        topLevelClass.addImportedType("lombok.experimental.SuperBuilder");

        // 添加 Lombok 注解
//        if (enableLombok) {
//            addLombokAnnotations(topLevelClass);
//        }
        
        // 添加 Builder 模式
//        if (enableBuilder) {
//            addBuilderSupport(topLevelClass);
//        }
        return true;
    }
    
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
    
    private void addLombokAnnotations(TopLevelClass topLevelClass) {
        // 添加 @Data 注解
        topLevelClass.addAnnotation("@Data");
        topLevelClass.addImportedType("lombok.Data");
        
        // 添加 @Builder 注解
        topLevelClass.addAnnotation("@Builder");
        topLevelClass.addImportedType("lombok.Builder");
        
        // 添加 @NoArgsConstructor 和 @AllArgsConstructor
//        topLevelClass.addAnnotation("@NoArgsConstructor");
//        topLevelClass.addAnnotation("@AllArgsConstructor");
//        topLevelClass.addImportedType("lombok.NoArgsConstructor");
//        topLevelClass.addImportedType("lombok.AllArgsConstructor");
    }
    
    private void addBuilderSupport(TopLevelClass topLevelClass) {
        // 添加 builder() 静态方法
        Method builderMethod = new Method("builder");
        builderMethod.setStatic(true);
        builderMethod.setReturnType(new FullyQualifiedJavaType(topLevelClass.getType().getShortName() + ".Builder"));
        builderMethod.setVisibility(JavaVisibility.PUBLIC);
        builderMethod.addBodyLine("return new Builder();");
        
        topLevelClass.addMethod(builderMethod);
        
        // 添加 Builder 内部类
        InnerClass builderClass = new InnerClass(new FullyQualifiedJavaType("Builder"));
        builderClass.setStatic(true);
        builderClass.setVisibility(JavaVisibility.PUBLIC);
        
        // 为 Builder 类添加字段和方法
        List<Field> fields = topLevelClass.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            FullyQualifiedJavaType fieldType = field.getType();
            
            // 添加 Builder 字段
            Field builderField = new Field(fieldName, fieldType);
            builderField.setVisibility(JavaVisibility.PRIVATE);
            builderClass.addField(builderField);
            
            // 添加 Builder setter 方法
            Method setterMethod = new Method(fieldName);
            setterMethod.setReturnType(new FullyQualifiedJavaType("Builder"));
            setterMethod.setVisibility(JavaVisibility.PUBLIC);
            setterMethod.addParameter(new Parameter(fieldType, fieldName));
            setterMethod.addBodyLine("this." + fieldName + " = " + fieldName + ";");
            setterMethod.addBodyLine("return this;");
            builderClass.addMethod(setterMethod);
        }
        
        // 添加 build() 方法
        Method buildMethod = new Method("build");
        buildMethod.setReturnType(topLevelClass.getType());
        buildMethod.setVisibility(JavaVisibility.PUBLIC);
        StringBuilder buildBody = new StringBuilder("return new ")
            .append(topLevelClass.getType().getShortName())
            .append("(");
        
        for (int i = 0; i < fields.size(); i++) {
            buildBody.append(fields.get(i).getName());
            if (i < fields.size() - 1) {
                buildBody.append(", ");
            }
        }
        buildBody.append(");");
        buildMethod.addBodyLine(buildBody.toString());
        builderClass.addMethod(buildMethod);
        
        topLevelClass.addInnerClass(builderClass);
    }
}