package org.github.mybatis.generator.plugin;

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
 * @author Johnny
 *
 */
public class ModelAndExampleSubClassPlugin extends PluginAdapter {

    private ShellCallback shellCallback = null;

    public ModelAndExampleSubClassPlugin() {
        shellCallback = new DefaultShellCallback(false);
    }

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        System.out.println("===============开始：生成Model子类文件================");

        JavaFormatter javaFormatter = context.getJavaFormatter();

        List<GeneratedJavaFile> subClassJavaFiles = new ArrayList<GeneratedJavaFile>();
        for (GeneratedJavaFile javaFile : introspectedTable.getGeneratedJavaFiles()) {

            CompilationUnit unit = javaFile.getCompilationUnit();
            FullyQualifiedJavaType baseModelJavaType = unit.getType();

            TopLevelClass subModelClass = new TopLevelClass(getSubModelType(baseModelJavaType));

            subModelClass.setVisibility(JavaVisibility.PUBLIC);
            subModelClass.addImportedType(baseModelJavaType);
            subModelClass.setSuperClass(baseModelJavaType);

            Field field = new Field("serialVersionUID", new FullyQualifiedJavaType("long"));
            field.setStatic(true);
            field.setFinal(true);
            field.setVisibility(JavaVisibility.PRIVATE);
            field.setInitializationString("1L");
            subModelClass.addField(field);

            String targetProject = javaFile.getTargetProject();
            FullyQualifiedJavaType subModelJavaType = subModelClass.getType();
            String subModelPackageName = subModelJavaType.getPackageName();

            try {
                GeneratedJavaFile subCLassJavafile = new GeneratedJavaFile(subModelClass, targetProject, javaFormatter);

                File subModelDir = shellCallback.getDirectory(targetProject, subModelPackageName);

                File subModelFile = new File(subModelDir, subCLassJavafile.getFileName());

                // 文件不存在
                if (!subModelFile.exists()) {

                    subClassJavaFiles.add(subCLassJavafile);
                }
            } catch (ShellException e) {
                e.printStackTrace();
            }

        }

        System.out.println("===============结束：生成Model子类文件================");

        return subClassJavaFiles;
    }

    private String getSubModelType(FullyQualifiedJavaType fullyQualifiedJavaType) {
        String type = fullyQualifiedJavaType.getFullyQualifiedName();
        String temp = "base.Base";
        String newType = type.replace(temp, "");
        return newType;
    }
}
