package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.JavaFormatter;
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
 * baseModelPackage 为生成新的类文件的包名，缺省为generated，表明这些类是生成的</br>
 * exampleTargetPackage 为 Example 类的独立包名（可选），配置后 Example 子类（XxxExample）落在该独立包，
 * Base Example（BaseXxxExample）落在它的 generated 子包（exampleTargetPackage.generated），与 Model 对称；
 * 不配置时 Example 与 Model 同包，BaseXxxExample 与 BaseXxx 同在 modelTargetPackage.generated（向后兼容）</br>
 * exampleTargetDir 为 Example 类的独立输出目录（可选，targetProject 完整路径），配置后 Example 类
 * 彻底输出到该目录（Model 目录不残留 Example）；未配置时退回 modelTargetDir</br>
 * extXmlPackage 已废弃，改为generatedXmlPackage：生成xml文件所在的子包名，缺省为generated</br>
 * xmlTargetPackage 为xml文件存放的包名，扩展xml文件直接放在这个目录下，
 * 生成的xml文件放在它的generated子目录下，两者是父子目录的关系</br>
 * xml文件里mapper元素的namespace跟随mapperTargetPackage(Mapper类的包名)，与xml文件的存放路径无关，
 * 未配置mapperTargetPackage时退回xmlTargetPackage</br>
 * 
 * @author oking
 *
 **/
public class ModelAndExampleBaseClassPlugin extends PluginAdapter {

	/**
	 * 生成的Model类所在的子包，缺省为generated，用包名表明这些类是工具生成的
	 */
	public final static String DEFAULT_BASE_MODEL_PACKAGE = "generated";
	public final static String DEFAULT_BASE_MODEL_NAME_PREFIX = "Base";

	private final static String DEFAULT_GENERATED_XML_PACKAGE = "generated";

	private ShellCallback shellCallback = null;

	/**
	 * Model的基类
	 */
    private String baseModelSuperClass = "com.github.mybatis.model.BaseModel";

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
	 * Base Example 类文件包名。始终是「子类 Example 包」的 generated 子包，与 Base Model 对称：
	 * 配置 exampleTargetPackage 时 = exampleTargetPackage.generated；未配置时 = fullModelPackage（与 Base Model 同包）。
	 */
	private String fullExamplePackage;

	/**
	 * Example 类的独立包名（原始配置值，未配置时为 null）
	 */
	private String exampleTargetPackage;

	/**
	 * Example 类的独立目录（targetProject，完整路径）。未配置时为 null，退回 modelTargetDir
	 */
	private String exampleTargetDir;

	/**
	 * 跨表保存「被阻止写盘」的 Base Example 类（modelExampleClassGenerated 返回 false 时暂存，
	 * 供 contextGenerateAdditionalJavaFiles 重新定向到 exampleTargetDir）。
	 * 键为 IntrospectedTable 实例，避免单实例字段跨表串表。
	 */
	private final java.util.Map<IntrospectedTable, TopLevelClass> exampleClassRegistry = new java.util.HashMap<>();

	/**
	 * 类的主键字段名, 默认为id
	 */
	private String modelPKColumnName = "id";

	/**
	 * 生成的xml文件(每次覆盖)存放的包名，是xmlTargetPackage下面的子包。
	 * 扩展xml文件放在xmlTargetPackage本身，两者是父子目录的关系
	 */
	private String fullGeneratedXmlPackage;

	/**
	 * xml文件存放的包名，只决定xml文件在磁盘上的路径，与namespace无关
	 */
	private String xmlTargetPackage;

	/**
	 * xml文件里namespace的包名，只决定mapper元素的namespace值，与xml文件的存放路径无关。
	 * 必须跟随Mapper类的包名：MyBatis要求namespace等于Mapper接口的全限定名，否则接口与xml绑定不上。
	 * 取值优先级：mapperTargetPackage(Mapper类的包名) > xmlTargetPackage
	 */
	private String xmlNamespacePackage;

	public ModelAndExampleBaseClassPlugin() {
		shellCallback = new DefaultShellCallback(false);
	}

	@Override
	public void initialized(IntrospectedTable introspectedTable) {
		// 这里只用局部变量做中转：先读 MBG 默认算出的子类名，再改写成 Base 名。
		// 切勿改成单实例字段承载这些值——MBG 生成是「先对所有表 initialized()，再逐表生成」两阶段，
		// 单实例字段到第二阶段只剩最后一张表的值，会导致 XML 里 Model/Example 类型串表。

		String modelClassName = introspectedTable.getBaseRecordType();
		introspectedTable.setBaseRecordType(genBaseClassName(modelClassName));

		String exampleClassName = introspectedTable.getExampleType();
		introspectedTable.setExampleType(genBaseExampleClassName(exampleClassName));

		String primaryKeyClassName = introspectedTable.getPrimaryKeyType();
		int indexOfLastDot = primaryKeyClassName.lastIndexOf('.');
		String newPrimaryKeyClassName = fullModelPackage + "." + primaryKeyClassName.substring(indexOfLastDot + 1);
		introspectedTable.setPrimaryKeyType(newPrimaryKeyClassName);

		// MBG默认把生成的xml放在sqlMapGenerator的targetPackage下，这里改到它的子包(generated)里，
		// 把外层目录让给扩展xml文件，于是生成的文件与扩展文件形成父子目录，
		// 例如 mapper/generated/UserMapper.xml 与 mapper/UserMapper.xml
		introspectedTable.setMyBatis3XmlMapperPackage(fullGeneratedXmlPackage);

		// 单独设定xml文件的namespace，让它不再受xml文件存放包名(xmlTargetPackage)的影响。
		// 文件写到哪个目录由MBG依据xmlTargetPackage自行决定，这里只改namespace的取值
		String xmlNamespace = genXmlNamespace(introspectedTable);
		introspectedTable.setMyBatis3FallbackSqlMapNamespace(xmlNamespace);

		// getMyBatis3SqlMapNamespace()会优先取javaMapperType，只有它为null时才用上面的fallback。
		// 配置了javaClientGenerator时javaMapperType不为null，namespace会被它覆盖
		String javaMapperType = introspectedTable.getMyBatis3JavaMapperType();
		if (javaMapperType != null && !javaMapperType.equals(xmlNamespace)) {
			System.out.printf("警告：namespace被Mapper类型[%s]覆盖，期望值[%s]未生效；" +
					"请让javaClientGenerator的targetPackage与mapperTargetPackage保持一致，或去掉该配置%n",
					javaMapperType, xmlNamespace);
		}
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

		// Example 子类的独立包名（可选）：配置后 Example 子类（XxxExample）与 Model 分离，
		// 输出到该独立包；不配置时 Example 子类与 Model 同包（向后兼容）。
		exampleTargetPackage = properties.getProperty("exampleTargetPackage");

		// Example 类的独立目录（可选）：完整路径，未配置时退回 modelTargetDir（不分离目录）。
		exampleTargetDir = properties.getProperty("exampleTargetDir");

		// Base Example 类（BaseXxxExample）与 Base Model 保持对称：始终放在「子类 Example 包」的 generated 子包下。
		// 配置 exampleTargetPackage 时 → exampleTargetPackage.generated（与 model 类似）；
		// 未配置时 → fullModelPackage（= modelTargetPackage.generated，与 Base Model 同包，类名不同不冲突）。
		fullExamplePackage = stringHasValue(exampleTargetPackage)
				? exampleTargetPackage + "." + DEFAULT_BASE_MODEL_PACKAGE
				: fullModelPackage;

		xmlTargetPackage = properties.getProperty("xmlTargetPackage");
		if (!stringHasValue(xmlTargetPackage)) {
			return false;
		}

		// 文件路径与namespace彻底分开：文件写到哪个目录由 xmlTargetPackage 决定，
		// namespace 则跟随Mapper类的包名。MyBatis要求namespace必须等于Mapper接口的全限定名，
		// 两者对不上会导致接口与xml绑定失败，所以这里不提供单独的namespace配置项
		xmlNamespacePackage = properties.getProperty("mapperTargetPackage");
		if (!stringHasValue(xmlNamespacePackage)) {
			xmlNamespacePackage = xmlTargetPackage;
		}
		System.out.printf("xml文件存放包名:%s, xml文件namespace包名:%s%n", xmlTargetPackage, xmlNamespacePackage);

		// 生成的xml文件放在xmlTargetPackage的子包(默认generated)下，扩展xml文件放在xmlTargetPackage
		// 本身，两者是父子目录关系：mapper/generated/UserMapper.xml 与 mapper/UserMapper.xml
		String generatedXmlPackage = properties.getProperty("generatedXmlPackage");
		if (stringHasValue(generatedXmlPackage)) {
			fullGeneratedXmlPackage = xmlTargetPackage + "." + generatedXmlPackage;
		} else {
			fullGeneratedXmlPackage = xmlTargetPackage + "." + DEFAULT_GENERATED_XML_PACKAGE;
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
		String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
        System.out.printf("===============开始：生成表 %s 的Mapper扩展xml文件================%n", tableName);

		// 这里必须从 introspectedTable 自身当前状态推导，不能用任何插件级缓存字段：
		// MBG 生成是「先对所有表 initialized()，再逐表调本方法」，缓存字段到本阶段只剩最后一张表的值，会导致串表。
		// initialized() 已把 baseRecordType 改成 model.generated.BaseXxx、exampleType 改成
		// Base Example 类名（如 example.generated.BaseXxxExample），据此反推回子类名即可，天然按表隔离。
		introspectedTable.setBaseRecordType(genModelSubClassFromBase(introspectedTable.getBaseRecordType()));
		introspectedTable.setExampleType(genExampleSubClassFromBase(introspectedTable.getExampleType()));

		List<GeneratedXmlFile> allXmlFiles = new ArrayList<GeneratedXmlFile>(1);
		List<GeneratedXmlFile> xmlFiles = introspectedTable.getGeneratedXmlFiles();

		for (GeneratedXmlFile xmlFile : xmlFiles) {
			// 将主xml的isMergeable改为false，强制每次生成都覆盖，否则残留旧内容
			xmlFile.setMergeable(false);

			Document document = new Document(XmlConstants.MYBATIS3_MAPPER_PUBLIC_ID,
					XmlConstants.MYBATIS3_MAPPER_SYSTEM_ID);
			XmlElement root = new XmlElement("mapper");
			document.setRootElement(root);

			// 生成新的空的xml 但是不覆盖
			// 与主xml文件使用同一个取值口径，保证两个xml文件的namespace完全一致，
			// 否则MyBatis会认为是两个不相干的命名空间，ext里的方法绑不到Mapper接口上
			root.addAttribute(new Attribute("namespace", introspectedTable.getMyBatis3SqlMapNamespace()));
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
				// 扩展xml文件放在xmlTargetPackage下，与存放生成文件的generated目录同级
				File directory = shellCallback.getDirectory(targetProject, xmlTargetPackage);

				File targetFile = new File(directory, fileName);

				if (!targetFile.exists()) {// 需要判断这个xml文件是否存在，若存在则不生成
					GeneratedXmlFile gxf = new GeneratedXmlFile(document, fileName, xmlTargetPackage, targetProject,
							true, context.getXmlFormatter());
					allXmlFiles.add(gxf);
				}
			} catch (ShellException e) {
				e.printStackTrace();
			}

			allXmlFiles.add(xmlFile);
		}

		System.out.printf("===============完成：生成表 %s 的Mapper扩展xml文件================%n", tableName);

		return allXmlFiles;
	}

	@Override
	public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
	    String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
		System.out.printf("===============开始：修改表 %s 的Model文件================%n", tableName);

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

			// 添加父类
	        FullyQualifiedJavaType superClazzType = new FullyQualifiedJavaType(baseModelSuperClass);
//			System.out.println("Model的基类：" + superClazzType.toString());
			// TODO: 暂时去掉集成 BaseModel 的代码，联合主键的Model 会有基类，以后可能通过接口类的形式来定义一些通用接口
//	        topLevelClass.addImportedType(superClazzType);
//			superClazzType.addTypeArgument(pkType);
//            topLevelClass.setSuperClass(superClazzType);
            
//			superInterfaceType.addTypeArgument(pkType);
//			topLevelClass.addSuperInterface(superInterfaceType);
		}
		
//		topLevelClass.setAbstract(true);

//		clearModelCLass(topLevelClass);

		System.out.printf("===============完成：修改表 %s 的Model文件================%n", tableName);

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

	@Override
	public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        String tableName = introspectedTable.getFullyQualifiedTable().getIntrospectedTableName();
		System.out.printf("===============开始：修改表 %s 的Example文件================%n", tableName);

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

		System.out.printf("===============完成：修改表 %s 的Example文件================%n", tableName);

		// 配置了独立目录 exampleTargetDir 时，阻止 Base Example 写 modelTargetDir，
		// 暂存到 registry，改由 contextGenerateAdditionalJavaFiles 重新定向到 exampleTargetDir。
		if (stringHasValue(exampleTargetDir)) {
			exampleClassRegistry.put(introspectedTable, topLevelClass);
			return false;
		}

		return super.modelExampleClassGenerated(topLevelClass, introspectedTable);
	}
	
    @Override
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
	 * 配置了独立目录 exampleTargetDir 时，把「被 modelExampleClassGenerated 阻止写盘」的
	 * Base Example 类重新定向到 exampleTargetDir 生成（彻底分离目录，modelTargetDir 不残留），
	 * 同时生成 Example 子类（XxxExample）到 exampleTargetDir。
	 * <p>
	 * 由于 Base Example 已从 getGeneratedJavaFiles 移除，Example 子类不能再由
	 * ModelAndExampleSubClassPlugin 生成，改由本方法一并生成。
	 */
	@Override
	public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
		List<GeneratedJavaFile> answer = new ArrayList<>();
		if (!stringHasValue(exampleTargetDir)) {
			return answer;
		}

		TopLevelClass exampleClass = exampleClassRegistry.get(introspectedTable);
		if (exampleClass == null) {
			return answer;
		}

		JavaFormatter javaFormatter = context.getJavaFormatter();

		// Base Example 类 → exampleTargetDir
		answer.add(new GeneratedJavaFile(exampleClass, exampleTargetDir, javaFormatter));

		// Example 子类 → exampleTargetDir（去掉 Base 前缀、落到 exampleTargetPackage 包）
		String subExampleType = genExampleSubClassFromBase(exampleClass.getType().getFullyQualifiedName());
		TopLevelClass subExampleClass = new TopLevelClass(subExampleType);
		subExampleClass.setVisibility(org.mybatis.generator.api.dom.java.JavaVisibility.PUBLIC);
		subExampleClass.addImportedType(exampleClass.getType());
		subExampleClass.setSuperClass(exampleClass.getType());
		answer.add(new GeneratedJavaFile(subExampleClass, exampleTargetDir, javaFormatter));

		return answer;
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
	 * 比如根据com.company.model.User，生成com.company.model.generated.BaseUser
	 * 
	 * @param oldModelType
	 * @return 新的名称
	 */
	private String genBaseClassName(String oldModelType) {
		int indexOfLastDot = oldModelType.lastIndexOf('.');
		return fullModelPackage + "." + baseModelNamePrefix + oldModelType.substring(indexOfLastDot + 1);
	}

	/**
	 * 根据 Base Model 类的全路径名，生成 Model 子类的全路径名（恢复时使用）。
	 * 去掉 generated 包与 Base 前缀，落到 modelTargetPackage 包。
	 * <p>
	 * 比如 com.company.model.generated.BaseUser 返回 com.company.model.User。
	 */
	private String genModelSubClassFromBase(String baseModelType) {
		int indexOfLastDot = baseModelType.lastIndexOf('.');
		String shortName = baseModelType.substring(indexOfLastDot + 1);
		if (shortName.startsWith(baseModelNamePrefix)) {
			shortName = shortName.substring(baseModelNamePrefix.length());
		}
		// modelTargetPackage = fullModelPackage 的父包（fullModelPackage = modelTargetPackage.generated）
		int modelPackageEnd = fullModelPackage.lastIndexOf('.');
		String modelTargetPackage = fullModelPackage.substring(0, modelPackageEnd);
		return modelTargetPackage + "." + shortName;
	}

	/**
	 * 根据 Example 类的全路径名称，生成 Base Example 类的全路径名(包括类名)。
	 * Base Example 固定放在 {@link #fullExamplePackage}（= 子类 Example 包的 generated 子包，
	 * 与 Base Model 对称）。
	 * <p>
	 * 比如 com.company.example.UserExample 在 exampleTargetPackage=com.company.example 时，
	 * 生成 com.company.example.generated.BaseUserExample。
	 */
	private String genBaseExampleClassName(String oldExampleType) {
		int indexOfLastDot = oldExampleType.lastIndexOf('.');
		return fullExamplePackage + "." + baseModelNamePrefix + oldExampleType.substring(indexOfLastDot + 1);
	}

	/**
	 * 根据 Example 子类的全路径名称，生成（恢复时使用的）Example 子类全路径名。
	 * 配置了独立 exampleTargetPackage 时，子类落在该独立包；否则保持原名（与 Model 同包）。
	 * <p>
	 * 比如 model.UserExample 在 exampleTargetPackage=com.company.example 时，
	 * 返回 com.company.example.UserExample。
	 */
	private String genExampleSubClassName(String oldExampleType) {
		int indexOfLastDot = oldExampleType.lastIndexOf('.');
		String shortName = oldExampleType.substring(indexOfLastDot + 1);
		if (stringHasValue(exampleTargetPackage)) {
			return exampleTargetPackage + "." + shortName;
		}
		return oldExampleType;
	}

	/**
	 * 根据 Base Example 类的全路径名，生成 Example 子类的全路径名。
	 * 去掉 Base 前缀，并落到 exampleTargetPackage 包（未配置则落到 Model 子类同包）。
	 * <p>
	 * 比如 com.company.example.generated.BaseUserExample 在 exampleTargetPackage=com.company.example 时，
	 * 返回 com.company.example.UserExample。
	 */
	private String genExampleSubClassFromBase(String baseExampleType) {
		int indexOfLastDot = baseExampleType.lastIndexOf('.');
		String shortName = baseExampleType.substring(indexOfLastDot + 1);
		if (shortName.startsWith(baseModelNamePrefix)) {
			shortName = shortName.substring(baseModelNamePrefix.length());
		}
		if (stringHasValue(exampleTargetPackage)) {
			return exampleTargetPackage + "." + shortName;
		}
		// 未配置独立包时，子类与 Model 子类同包（fullModelPackage 的父包，即 modelTargetPackage）
		int modelPackageEnd = fullModelPackage.lastIndexOf('.');
		String modelTargetPackage = fullModelPackage.substring(0, modelPackageEnd);
		return modelTargetPackage + "." + shortName;
	}

	/**
	 * 生成xml文件的namespace。只替换MBG默认namespace的包名部分，保留后面的Mapper短名。
	 * 比如MBG默认得到com.company.mapper.xml.UserMapper，而Mapper类的包名为com.company.mapper，
	 * 则最终得到com.company.mapper.UserMapper，保证namespace与Mapper接口的全限定名一致
	 * 
	 * @param introspectedTable
	 * @return 新的namespace
	 */
	private String genXmlNamespace(IntrospectedTable introspectedTable) {
		String defaultNamespace = introspectedTable.getMyBatis3FallbackSqlMapNamespace();
		int indexOfLastDot = defaultNamespace.lastIndexOf('.');
		String mapperShortName = defaultNamespace.substring(indexOfLastDot + 1);
		return xmlNamespacePackage + "." + mapperShortName;
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
