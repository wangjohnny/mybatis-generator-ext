package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.JavaFormatter;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.api.dom.java.CompilationUnit;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;

/**
 * 生成Model 的相关子类
 * 
 * @author lao king
 *
 */
public class ModelAndExampleSubClassPlugin extends PluginAdapter {

    private ShellCallback shellCallback = null;

    /**
     * Model基类文件包名
     */
    private String baseModelPackage;

    /**
     * Model类的前缀名称
     */
    private String baseModelNamePrefix;

    public ModelAndExampleSubClassPlugin() {
        shellCallback = new DefaultShellCallback(false);
    }

    @Override
    public boolean validate(List<String> warnings) {

        baseModelPackage = properties.getProperty("baseModelPackage");
        if (!stringHasValue(baseModelPackage)) {
            baseModelPackage = ModelAndExampleBaseClassPlugin.DEFAULT_BASE_MODEL_PACKAGE;
        }

        baseModelNamePrefix = properties.getProperty("baseModelNamePrefix");
        if (!stringHasValue(baseModelNamePrefix)) {
            baseModelNamePrefix = ModelAndExampleBaseClassPlugin.DEFAULT_BASE_MODEL_NAME_PREFIX;
        }

        return true;
    }

    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        System.out.println("===============开始：生成Model子类文件================");

        JavaFormatter javaFormatter = context.getJavaFormatter();

        List<GeneratedJavaFile> classJavaFiles = new ArrayList<GeneratedJavaFile>();
        for (GeneratedJavaFile javaFile : introspectedTable.getGeneratedJavaFiles()) {

            String targetProject = javaFile.getTargetProject();
            
            CompilationUnit unit = javaFile.getCompilationUnit();
            FullyQualifiedJavaType baseModelJavaType = unit.getType();

            String fullyQualifiedName = baseModelJavaType.getFullyQualifiedName();
            if (fullyQualifiedName.endsWith("Key")) {
				continue;
			}

            TopLevelClass subModelClass = new TopLevelClass(getSubModelType(baseModelJavaType));

            subModelClass.setVisibility(JavaVisibility.PUBLIC);
            subModelClass.addImportedType(baseModelJavaType);
            subModelClass.setSuperClass(baseModelJavaType);
            
            // 给 model 子类添加注解
            addLombokAnnotation(subModelClass);

            // 在类级别添加@Schema注解
            addSwaggerAnnotation(subModelClass, introspectedTable);

			if (!fullyQualifiedName.endsWith("Example")) {// 对Example类不能添加序列化版本字段
                Field field = new Field("serialVersionUID", new FullyQualifiedJavaType("long"));
                field.setStatic(true);
                field.setFinal(true);
                field.setVisibility(JavaVisibility.PRIVATE);
                field.setInitializationString("1L");
                subModelClass.addField(field);
            }

            FullyQualifiedJavaType subModelJavaType = subModelClass.getType();
            String subModelPackageName = subModelJavaType.getPackageName();

            try {
                GeneratedJavaFile subClassJavafile = new GeneratedJavaFile(subModelClass, targetProject, javaFormatter);

                File subModelDir = shellCallback.getDirectory(targetProject, subModelPackageName);

                File subModelFile = new File(subModelDir, subClassJavafile.getFileName());

                // 文件不存在
                if (!subModelFile.exists()) {

                    classJavaFiles.add(subClassJavafile);
                }
            } catch (ShellException e) {
                e.printStackTrace();
            }

        }

        System.out.println("===============结束：生成Model子类文件================");

        return classJavaFiles;
    }

    private String getSubModelType(FullyQualifiedJavaType fullyQualifiedJavaType) {
        String type = fullyQualifiedJavaType.getFullyQualifiedName();
        String defaultPrefix = baseModelPackage + "." + baseModelNamePrefix;
        String newType = type.replace(defaultPrefix, "");
        return newType;
    }

    private void addLombokAnnotation(TopLevelClass topLevelClass) {

        topLevelClass.addAnnotation("@Data");
        topLevelClass.addImportedType("lombok.Data");
        
        topLevelClass.addAnnotation("@ToString(callSuper = true)");
        topLevelClass.addImportedType("lombok.ToString");
        
        topLevelClass.addAnnotation("@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)");
        topLevelClass.addImportedType("lombok.EqualsAndHashCode");

        topLevelClass.addAnnotation("@NoArgsConstructor");
        topLevelClass.addImportedType("lombok.NoArgsConstructor");

//        topLevelClass.addAnnotation("@AllArgsConstructor");
//        topLevelClass.addImportedType("lombok.AllArgsConstructor");

        topLevelClass.addAnnotation("@SuperBuilder");
        topLevelClass.addImportedType("lombok.experimental.SuperBuilder");
    }
    
    private void addSwaggerAnnotation(TopLevelClass subModelClass, IntrospectedTable introspectedTable) {

        String tableRemarks = introspectedTable.getRemarks();
        if (tableRemarks != null && !tableRemarks.isEmpty()) {
            subModelClass.addAnnotation(
                    String.format("@Schema(title = \"%s\", description = \"%s\")", tableRemarks, tableRemarks));
        } else {
            subModelClass.addAnnotation("@Schema");
        }
        subModelClass.addImportedType("io.swagger.v3.oas.annotations.media.Schema");
    }
}
