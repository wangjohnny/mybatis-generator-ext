package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import org.mybatis.generator.api.dom.java.InnerClass;
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
            addSwaggerAnnotation(subModelClass, introspectedTable.getRemarks());

            // 给 Model 类添加 view 类
            addViewClass(subModelClass, baseModelJavaType);

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
                } else {
                    if (!fullyQualifiedName.endsWith("Example")) {// Example 类不需要更新 Schema
                        updateOrAddSchema(subModelFile, introspectedTable.getRemarks());
                    }
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

    /**
     * 给子类添加 Lombok 注解
     * 
     * @param topLevelClass
     */
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
    
    /**
     * 给子类添加 Swagger 注解
     * 
     * @param subModelClass
     * @param introspectedTable
     */
    private void addSwaggerAnnotation(TopLevelClass subModelClass, String tableRemarks) {

        if (tableRemarks != null && !tableRemarks.isEmpty()) {
            subModelClass.addAnnotation(
                    String.format("@Schema(title = \"%s\", description = \"%s\")", tableRemarks, tableRemarks));
        } else {
            subModelClass.addAnnotation("@Schema");
        }
        subModelClass.addImportedType("io.swagger.v3.oas.annotations.media.Schema");
    }
    
    /**
     * 给子类添加 jsonview 支持类
     * 
     * @param topLevelClass
     * @param baseModelJavaType
     */
    private void addViewClass(TopLevelClass topLevelClass, FullyQualifiedJavaType baseModelJavaType) {

        InnerClass viewClass = new InnerClass("View");
        
        viewClass.setSuperClass(baseModelJavaType.getShortName() + ".BasicView");
        viewClass.setVisibility(JavaVisibility.PUBLIC);
        viewClass.setStatic(true);

        InnerClass listPageViewClass = new InnerClass("ListPage");
        listPageViewClass.setVisibility(JavaVisibility.PUBLIC);
        listPageViewClass.setStatic(true);
        
        viewClass.addInnerClass(listPageViewClass);
        topLevelClass.addInnerClass(viewClass);
    }
    
    private void updateSchemaAnnotation(File file, String newRemarks) {
        try {
            // 读取所有行
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            
            String schema = String.format("@Schema(title = \"%s\", description = \"%s\")", newRemarks, newRemarks);
            boolean hasSchema = false;
            // 遍历每一行，找到 @Schema 并替换整行
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains("@Schema")) {
                    hasSchema = true;
                    if (!lines.get(i).equals(schema)) {//只有remarks 的内容改了，才会更新scheam
                        lines.set(i, schema);
                        break; // 假设只有一个 @Schema 注解   
                    }
                }
            }
            
            // 如果没有 @Schema，在类定义前添加
            if (!hasSchema) {
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).contains("public class ")) {
                        lines.add(i, schema);
                        break;
                    }
                }
            }
            
            // 写回文件
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update @Schema line", e);
        }
    }
    
    private void updateOrAddSchema(File file, String newRemarks) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            int classDefLineIndex = -1;

            // 查找类定义行和类级别 @Schema
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                
                // 标记类定义行（如 "public class User"）
                if (line.startsWith("public class ") || line.startsWith("class ")) {
                    classDefLineIndex = i;
                    break; // 找到类定义后退出循环
                }
            }
            
            String schema = String.format("@Schema(title = \"%s\", description = \"%s\")", newRemarks, newRemarks);
            boolean hasClassSchema = false;
            // 遍历每一行，找到 @Schema 并替换整行
            for (int i = 0; i < classDefLineIndex; i++) {
                if (lines.get(i).contains("@Schema")) {
                    hasClassSchema = true;
                    if (!lines.get(i).equals(schema)) {//只有remarks 的内容改了，才会更新scheam
                        lines.set(i, schema);
                        break; // 假设只有一个 @Schema 注解   
                    }
                }
            }
            
            // 如果没有类级别 @Schema，在类定义上方添加
            if (!hasClassSchema && classDefLineIndex != -1) {
                lines.add(classDefLineIndex, schema);
            }
            
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update class-level @Schema", e);
        }
    }
}
