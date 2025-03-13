package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedColumn;
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
 * baseModelNamePrefix 为新生成的类文件的前置关键字</br>
 * baseModelPackage 为生成新的类文件的包名</br>
 * extXmlPackage 包名</br>
 * 
 * @author Johnny
 *
 **/
public class ModelAndExampleBaseClassPlugin extends PluginAdapter {

	public final static String DEFAULT_BASE_MODEL_PACKAGE = "base";
	public final static String DEFAULT_BASE_MODEL_NAME_PREFIX = "Base";

	private final static String DEFAULT_EXT_XML_PACKAGE = "ext";

	private ShellCallback shellCallback = null;

	/**
	 * Model的基类
	 */
    private String baseModelSuperClass = "com.github.mybatis.model.BaseModel";
//	private String baseModelSuperInterface = "com.github.mybatis.model.IModel";

	/**
	 * Example的基类
	 */
	private String baseExampleSuperClass = "com.github.mybatis.model.BaseModelExample";

	/**
	 * Criteria的基类
	 */
	private String baseCriteriaSuperClass = "com.github.mybatis.model.BaseCriteria";

	/**
	 * Model类的前缀名称
	 */
	private String baseModelNamePrefix;

	/**
	 * Model类文件包名
	 */
	private String fullModelPackage;

	/**
	 * 类的主键字段名, 默认为sid
	 */
	private String modelPKColumnName = "sid";

	/**
	 * 扩展xml文件包名
	 */
	private String fullExtXmlPackage;

	/**
	 * 利用java反射获取isMergeable参数，并修改
	 */
	private java.lang.reflect.Field isMergeableFid = null;

	/**
	 * 三个参数用于做数据中转，否则xml文件里Model类、Example类 PrimaryKey 类的package 的路径不对，这里是为了让xml文件用正确的Model类
	 */
	private String modelClassName;
	private String exampleClassName;
	private String primaryKeyClassName;

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
		primaryKeyClassName = null;

		modelClassName = introspectedTable.getBaseRecordType();
		introspectedTable.setBaseRecordType(genBaseClassName(modelClassName));

		exampleClassName = introspectedTable.getExampleType();
		introspectedTable.setExampleType(genBaseClassName(exampleClassName));

		primaryKeyClassName = introspectedTable.getPrimaryKeyType();
		int indexOfLastDot = primaryKeyClassName.lastIndexOf('.');
		String newPrimaryKeyClassName = fullModelPackage + "." + primaryKeyClassName.substring(indexOfLastDot + 1);
		introspectedTable.setPrimaryKeyType(newPrimaryKeyClassName);
	}

	/*
	 * 检查xml参数是否正确
	 * 
	 * @see org.mybatis.generator.api.Plugin#validate(java.util.List)
	 */
	@Override
	public boolean validate(List<String> warnings) {
		System.out.println("开始：validate");

		baseModelNamePrefix = properties.getProperty("baseModelNamePrefix");
		if (!stringHasValue(baseModelNamePrefix)) {
			baseModelNamePrefix = DEFAULT_BASE_MODEL_NAME_PREFIX;
		}

		String modelTargetPackage = properties.getProperty("modelTargetPackage");
		if (!stringHasValue(modelTargetPackage)) {
			return false;
		}

		String baseModelPackage = properties.getProperty("baseModelPackage");
		if (stringHasValue(baseModelPackage)) {
			fullModelPackage = modelTargetPackage + "." + baseModelPackage;
		} else {
			fullModelPackage = modelTargetPackage + "." + DEFAULT_BASE_MODEL_PACKAGE;
		}

		String xmlTargetPackage = properties.getProperty("xmlTargetPackage");
		if (!stringHasValue(xmlTargetPackage)) {
			return false;
		}

		String extXmlPackage = properties.getProperty("extXmlPackage");
		if (stringHasValue(extXmlPackage)) {
			fullExtXmlPackage = xmlTargetPackage + "." + extXmlPackage;
		} else {
			fullExtXmlPackage = xmlTargetPackage + "." + DEFAULT_EXT_XML_PACKAGE;
		}

        String baseModelSuperClazz = properties.getProperty("baseModelSuperClass");
        if (stringHasValue(baseModelSuperClazz)) {
            baseModelSuperClass = baseModelSuperClazz;
        }

//		String baseModelSuperInterface = properties.getProperty("baseModelSuperInterface");
//		if (stringHasValue(baseModelSuperInterface)) {
//			this.baseModelSuperInterface = baseModelSuperInterface;
//		}
		
		String pkColumnName = properties.getProperty("pkColumnName");
		if (stringHasValue(pkColumnName)) {
			modelPKColumnName = pkColumnName;
		}

		String baseExampleSuperClazz = properties.getProperty("baseExampleSuperClass");
		if (stringHasValue(baseExampleSuperClazz)) {
			baseExampleSuperClass = baseExampleSuperClazz;
		}

		String baseCriteriaSuperClazz = properties.getProperty("baseCriteriaSuperClass");
		if (stringHasValue(baseCriteriaSuperClazz)) {
			baseCriteriaSuperClass = baseCriteriaSuperClazz;
		}

		return true;
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

//		if (primaryKeyClassName != null) {
//			introspectedTable.setPrimaryKeyType(primaryKeyClassName);
//		}

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
				File directory = shellCallback.getDirectory(targetProject, fullExtXmlPackage);

				File targetFile = new File(directory, fileName);

				if (!targetFile.exists()) {// 需要判断这个xml文件是否存在，若存在则不生成
					GeneratedXmlFile gxf = new GeneratedXmlFile(document, fileName, fullExtXmlPackage, targetProject,
							true, context.getXmlFormatter());
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

//		FullyQualifiedJavaType superInterfaceType = new FullyQualifiedJavaType(this.baseModelSuperInterface);
//		topLevelClass.addImportedType(superInterfaceType);
		
		FullyQualifiedJavaType pkType = null;
		List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
		if (primaryKeyColumns.isEmpty()) {
			pkType = new FullyQualifiedJavaType("java.lang.String");// 没有主键的表，默认使用字符串作为主键类型
		} else {
			if (primaryKeyColumns.size() == 1) {// 单主键
				IntrospectedColumn pkColumn = primaryKeyColumns.get(0);
				modelPKColumnName = pkColumn.getActualColumnName();
				pkType = pkColumn.getFullyQualifiedJavaType();
			} else {// 联合主键
				String primaryKeyType = introspectedTable.getPrimaryKeyType();
				pkType = new FullyQualifiedJavaType(primaryKeyType);
			}

			System.out.println("主键类型:" + pkType);

			// 添加基类
	        FullyQualifiedJavaType superClazzType = new FullyQualifiedJavaType(baseModelSuperClass);
//			System.out.println("Model的基类：" + superClazzType.toString());
			// TODO: 暂时去掉集成 BaseModel 的代码，联合主键的Model 会有基类，以后可能通过接口类的形式来定义一些通用接口
//	        topLevelClass.addImportedType(superClazzType);
//			superClazzType.addTypeArgument(pkType);
//            topLevelClass.setSuperClass(superClazzType);
            
//			superInterfaceType.addTypeArgument(pkType);
//			topLevelClass.addSuperInterface(superInterfaceType);
		}
		
		topLevelClass.setAbstract(true);

//		clearModelCLass(topLevelClass);

		System.out.println("===============完成：修改Model文件================");

		return super.modelBaseRecordClassGenerated(topLevelClass, introspectedTable);
	}

	/**
	 * 清理Example的多余属性与方法，已经迁移到父类了
	 * 
	 * @param topLevelClass
	 */
	private void clearModelCLass(TopLevelClass topLevelClass) {

		System.out.println("开始清理Model的TopLevelCLass多余属性");

		HashSet<Field> removingFields = new HashSet<Field>();

		List<Field> fields = topLevelClass.getFields();
		for (Field field : fields) {
			String fieldName = field.getName();
			if (modelPKColumnName.equals(fieldName)) {// 将要删除的变量
				System.out.println("removing field:" + fieldName);
				removingFields.add(field);
			}
		}
		fields.removeAll(removingFields);

		HashSet<Method> removingMethods = new HashSet<Method>();

		String pkSetter = "set" + capitalize(modelPKColumnName);
		String pkGetter = "get" + capitalize(modelPKColumnName);

		List<Method> methods = topLevelClass.getMethods();
		for (Method method : methods) {
			String methodName = method.getName();

			if (pkSetter.equals(methodName) || pkGetter.equals(methodName)) {// 将要删除的方法
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
	
    public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
    	
//    	FullyQualifiedJavaType qualifiedJavaType = topLevelClass.getType();
//		String pkName = fullModelPackage + "." + qualifiedJavaType.getShortName();
//		
//		topLevelClass.getFields();
//		
//		System.out.println("PK type: " + pkName);
//		
//    	introspectedTable.setPrimaryKeyType(pkName);
    	
        return super.modelPrimaryKeyClassGenerated(topLevelClass, introspectedTable);
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
		return fullModelPackage + "." + baseModelNamePrefix + oldModelType.substring(indexOfLastDot + 1);
	}

	public static String capitalize(final String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0) {
			return str;
		}

		char firstChar = str.charAt(0);
		if (Character.isTitleCase(firstChar)) {
			// already capitalized
			return str;
		}

		return new StringBuilder(strLen).append(Character.toTitleCase(firstChar)).append(str.substring(1)).toString();
	}

}
