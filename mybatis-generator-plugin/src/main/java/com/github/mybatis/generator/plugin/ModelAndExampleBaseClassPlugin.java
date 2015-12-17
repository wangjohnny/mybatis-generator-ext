package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.InnerClass;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.XmlConstants;
import org.mybatis.generator.exception.ShellException;
import org.mybatis.generator.internal.DefaultShellCallback;

/**
 * 
 * 定制部分mybatis的插件，主要实现以下功能
 * <ol>
 * <li>生成Model和Example的Base文件</li>
 * <li>生成新的sqlmap并覆盖xml文件</li>
 * <li>生成空的sqlmap custom的xml文件，不覆盖原来的，如果没有则创建空的</li>
 * </ol>
 * <p>
 * 使用方法配置与在generatorConfig.xml中其中</br>
 * baseCLassNamePrefix 为新生成的类文件的前置关键字</br>
 * basePackage 为生成新的类文件的包名</br>
 * extXmlPackage 包名</br>
 * 
 * @author Johnny
 *
 **/
public class ModelAndExampleBaseClassPlugin extends PluginAdapter {

    private ShellCallback shellCallback = null;

    /**
     * Model的基类
     */
    private String baseModelSuperClass;

    /**
     * Example的基类
     */
    private String baseExampleSuperClass;

    /**
     * Criteria的基类
     */
    private String baseCriteriaSuperClass;

    /**
     * Model类文件的前缀名称
     */
    private String baseCLassNamePrefix;

    /**
     * Model类文件包名
     */
    private String basePackage;

    /**
     * 扩展xml文件包名
     */
    private String extXmlPackage;

    /**
     * 利用java反射获取isMergeable参数，并修改
     */
    private java.lang.reflect.Field isMergeableFid = null;

    /**
     * 两个参数用于做数据中转，否则xml文件里会用Base类，这里是为了让xml文件用标准的Model类
     */
    private String modelClassName;
    private String exampleClassName;

    public ModelAndExampleBaseClassPlugin() {
        shellCallback = new DefaultShellCallback(false);

        try {
            if (isMergeableFid == null) {
                isMergeableFid = GeneratedXmlFile.class.getDeclaredField("isMergeable");
                isMergeableFid.setAccessible(true);
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        // 初始化两参数为空
        modelClassName = null;
        exampleClassName = null;

        modelClassName = introspectedTable.getBaseRecordType();
        introspectedTable.setBaseRecordType(genBaseClassName(modelClassName));

        exampleClassName = introspectedTable.getExampleType();
        introspectedTable.setExampleType(genBaseClassName(exampleClassName));
    }

    /*
     * 检查xml参数是否正确
     * 
     * @see org.mybatis.generator.api.Plugin#validate(java.util.List)
     */
    @Override
    public boolean validate(List<String> warnings) {
        System.out.println("开始：validate");

        baseCLassNamePrefix = properties.getProperty("baseCLassNamePrefix");
        boolean valid = stringHasValue(baseCLassNamePrefix);

        basePackage = properties.getProperty("basePackage");
        boolean valid2 = stringHasValue(basePackage);

        extXmlPackage = properties.getProperty("extXmlPackage");
        boolean valid3 = stringHasValue(extXmlPackage);

        baseModelSuperClass = properties.getProperty("baseModelSuperClass");
        boolean valid4 = stringHasValue(baseModelSuperClass);

        baseExampleSuperClass = properties.getProperty("baseExampleSuperClass");
        boolean valid5 = stringHasValue(baseExampleSuperClass);

        baseCriteriaSuperClass = properties.getProperty("baseCriteriaSuperClass");
        boolean valid6 = stringHasValue(baseCriteriaSuperClass);

        boolean b = valid && valid2 && valid3 && valid4 && valid5 && valid6;

        System.out.println("valid:" + valid + ",valid2:" + valid2 + "valid3:" + valid3 + "valid4:" + valid4 + ",valid5:"
                + valid5 + ",valid6:" + valid6);

        return b;
    }

    /*
     * 生成新的xml文件 ,覆盖原来存在文件
     * 
     * @see
     * org.mybatis.generator.api.PluginAdapter#contextGenerateAdditionalXmlFiles
     * (org.mybatis.generator.api.IntrospectedTable)
     */
    @Override
    public List<GeneratedXmlFile> contextGenerateAdditionalXmlFiles(IntrospectedTable introspectedTable) {
        System.out.println("===============开始：生成Mapper扩展xml文件================");

        if (modelClassName != null) {
            introspectedTable.setBaseRecordType(modelClassName);
        }

        if (exampleClassName != null) {
            introspectedTable.setExampleType(exampleClassName);
        }

        List<GeneratedXmlFile> extXmlFiles = new ArrayList<GeneratedXmlFile>(1);
        List<GeneratedXmlFile> xmlFiles = introspectedTable.getGeneratedXmlFiles();

        for (GeneratedXmlFile xmlFile : xmlFiles) {
            try {
                // 将xml的isMergeabl改为false
                isMergeableFid.set(xmlFile, false);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            Document document = new Document(XmlConstants.MYBATIS3_MAPPER_PUBLIC_ID,
                    XmlConstants.MYBATIS3_MAPPER_SYSTEM_ID);
            XmlElement root = new XmlElement("mapper");
            document.setRootElement(root);

            // 生成新的空的xml 但是不覆盖
            root.addAttribute(new Attribute("namespace", introspectedTable.getMyBatis3FallbackSqlMapNamespace()));
            root.addElement(new TextElement("<!--"));
            StringBuilder sb = new StringBuilder();
            sb.append("  文件的生成时间： ");
            sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            sb.append('.');
            root.addElement(new TextElement(sb.toString()));
            root.addElement(new TextElement("  你应该把Mapper类的扩展方法的sql语句放在这个文件里面"));

            root.addElement(new TextElement("-->"));
            root.addElement(new TextElement(""));// 添加空白行

            String fileName = xmlFile.getFileName();
            String targetProject = xmlFile.getTargetProject();

            try {
                File directory = shellCallback.getDirectory(targetProject, extXmlPackage);

                File targetFile = new File(directory, fileName);

                if (!targetFile.exists()) {// 需要判断这个xml文件是否存在，若存在则不生成
                    GeneratedXmlFile gxf = new GeneratedXmlFile(document, fileName, extXmlPackage, targetProject, true,
                            context.getXmlFormatter());
                    extXmlFiles.add(gxf);
                }
            } catch (ShellException e) {
                e.printStackTrace();
            }

            extXmlFiles.add(xmlFile);
        }

        System.out.println("===============完成：生成Mapper扩展xml文件================");

        return extXmlFiles;
    }

    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        System.out.println("===============开始：修改Model文件================");

        // 添加example基类
        topLevelClass.addImportedType(new FullyQualifiedJavaType(baseModelSuperClass));
        topLevelClass.setSuperClass(baseModelSuperClass);

        clearModelCLass(topLevelClass);

        System.out.println("===============完成：修改Model文件================");

        return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
    }

    /**
     * 清理Example的多余属性与方法，已经迁移到父类了
     * 
     * @param topLevelClass
     */
    private static void clearModelCLass(TopLevelClass topLevelClass) {

        System.out.println("开始清理Model的TopLevelCLass多余属性");

        HashSet<Field> removingFields = new HashSet<Field>();

        List<Field> fields = topLevelClass.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            if ("sid".equals(fieldName)) {// 将要删除的变量
                System.out.println("removing field:" + fieldName);
                removingFields.add(field);
            }
        }
        fields.removeAll(removingFields);

        HashSet<Method> removingMethods = new HashSet<Method>();

        List<Method> methods = topLevelClass.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();

            if ("setSid".equals(methodName) || "getSid".equals(methodName)) {// 将要删除的方法
                System.out.println("removing method:" + methodName);
                removingMethods.add(method);
            }
        }
        methods.removeAll(removingMethods);
    }

    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        System.out.println("===============开始：修改Example文件================");

        // 添加example基类
        topLevelClass.addImportedType(new FullyQualifiedJavaType(baseExampleSuperClass));
        topLevelClass.setSuperClass(baseExampleSuperClass);

        clearExampleCLass(topLevelClass);

        HashSet<InnerClass> removingInnerClasses = new HashSet<InnerClass>();

        // 处理example的所有内部类
        List<InnerClass> innerClasses = topLevelClass.getInnerClasses();
        for (InnerClass innerClass : innerClasses) {

            FullyQualifiedJavaType type = innerClass.getType();
            String innerClassName = type.getFullyQualifiedName();
            System.out.println("fullyQualifiedName:" + innerClassName);

            if ("Criterion".equals(innerClassName)) {// 删除example的Criterion静态类
                removingInnerClasses.add(innerClass);

            } else if ("GeneratedCriteria".equals(innerClassName)) {// 改造GeneratedCriteria类，添加一个基类
                // innerClass.setAbstract(false);

                // 添加Criteria基类
                topLevelClass.addImportedType(new FullyQualifiedJavaType(baseCriteriaSuperClass));
                innerClass.setSuperClass(baseCriteriaSuperClass);

                clearGeneratedCriteriaClass(innerClass);
            }
        }

        innerClasses.removeAll(removingInnerClasses);

        System.out.println("===============完成：修改Example文件================");

        return super.modelExampleClassGenerated(topLevelClass, introspectedTable);
    }

    /**
     * 清理Example的多余属性与方法，已经迁移到父类了
     * 
     * @param topLevelClass
     */
    private static void clearExampleCLass(TopLevelClass topLevelClass) {

        System.out.println("开始清理Example的TopLevelCLass多余属性");

        HashSet<Field> removingFields = new HashSet<Field>();

        List<Field> fields = topLevelClass.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            if ("orderByClause".equals(fieldName) || "distinct".equals(fieldName)) {// 将要删除的变量
                System.out.println("removing field:" + fieldName);
                removingFields.add(field);
            }
        }
        fields.removeAll(removingFields);

        HashSet<Method> removingMethods = new HashSet<Method>();

        List<Method> methods = topLevelClass.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();

            if ("setOrderByClause".equals(methodName) || "getOrderByClause".equals(methodName)
                    || "setDistinct".equals(methodName) || "isDistinct".equals(methodName)) {// 将要删除的方法
                System.out.println("removing method:" + methodName);
                removingMethods.add(method);
            } else if ("clear".equals(methodName)) {// 重新实现clear方法，部分实现重构到父类
                List<String> bodyLines = method.getBodyLines();
                bodyLines.clear();

                bodyLines.add("super.clear();");
                bodyLines.add("oredCriteria.clear();");
            }
        }
        methods.removeAll(removingMethods);
    }

    /**
     * 清理GeneratedCriteria的多余属性与方法，已经迁移到父类了
     * 
     * @param innerClass
     */
    private static void clearGeneratedCriteriaClass(InnerClass innerClass) {

        System.out.println("开始清理GeneratedCriteriaClass的多余属性");

        HashSet<Field> removingFields = new HashSet<Field>();

        List<Field> fields = innerClass.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            if ("criteria".equals(fieldName)) {// 将要删除的变量
                System.out.println("removing field:" + fieldName);
                removingFields.add(field);
            }
        }
        fields.removeAll(removingFields);

        HashSet<Method> removingMethods = new HashSet<Method>();

        List<Method> methods = innerClass.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();

            if ("GeneratedCriteria".equals(methodName) || "isValid".equals(methodName)
                    || "getAllCriteria".equals(methodName) || "getCriteria".equals(methodName)
                    || "addCriterion".equals(methodName)) {// 将要删除的方法
                System.out.println("removing method:" + methodName);
                removingMethods.add(method);
            }
        }
        methods.removeAll(removingMethods);
    }

    /**
     * 根据Model类的全路径名称，生成Base Model类的全路径名(包括类名)。
     * 比如根据com.company.model.User，生成com.company.model.base.BaseUser
     * 
     * @param oldModelType
     * @return 新的名称
     */
    private String genBaseClassName(String oldModelType) {
        int indexOfLastDot = oldModelType.lastIndexOf('.');
        return basePackage + "." + baseCLassNamePrefix + oldModelType.substring(indexOfLastDot + 1);
    }

}
