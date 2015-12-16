package org.github.mybatis.generator.plugin;

import java.util.List;

import org.mybatis.generator.api.CommentGenerator;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

/**
 * Mysql 分页插件
 * 
 * @author Johnny
 */
public class MysqlPaginationPlugin extends PluginAdapter {

	@Override
	public boolean modelExampleClassGenerated(TopLevelClass topLevelClass,
	        IntrospectedTable introspectedTable) {
		// add field, getter, setter for limit clause
		// 把分页字段放到基类
		// addPagination(topLevelClass, introspectedTable, "pagination");

		return super.modelExampleClassGenerated(topLevelClass,
		        introspectedTable);
	}

	@Override
	public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(
	        XmlElement element, IntrospectedTable introspectedTable) {

		XmlElement pageElement = new XmlElement("if");
		pageElement.addAttribute(new Attribute("test",
		        "pagination != null and pagination.begin >= 0"));
		pageElement.addElement(new TextElement(
		        "limit #{pagination.begin}, #{pagination.pageSize}"));
		element.getElements().add(pageElement);

		return super.sqlMapUpdateByExampleWithoutBLOBsElementGenerated(element,
		        introspectedTable);
	}

	/**
	 * 现在分页代码被放到基类里面了
	 * 
	 * @param topLevelClass
	 * @param introspectedTable
	 * @param name
	 */
	private void addPagination(TopLevelClass topLevelClass,
	        IntrospectedTable introspectedTable, String name) {

		topLevelClass.addImportedType(new FullyQualifiedJavaType(
		        "com.company.core.Pagination"));
		CommentGenerator commentGenerator = context.getCommentGenerator();
		Field field = new Field();
		field.setVisibility(JavaVisibility.PROTECTED);
		field.setType(new FullyQualifiedJavaType("com.company.core.Pagination"));
		field.setName(name);
		commentGenerator.addFieldComment(field, introspectedTable);
		topLevelClass.addField(field);
		char c = name.charAt(0);
		String camel = Character.toUpperCase(c) + name.substring(1);

		Method method = new Method();
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setName("set" + camel);
		method.addParameter(new Parameter(new FullyQualifiedJavaType(
		        "com.company.core.Pagination"), name));
		method.addBodyLine("this." + name + "=" + name + ";");
		commentGenerator.addGeneralMethodComment(method, introspectedTable);
		topLevelClass.addMethod(method);

		method = new Method();
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setReturnType(new FullyQualifiedJavaType(
		        "com.company.core.Pagination"));
		method.setName("get" + camel);
		method.addBodyLine("return " + name + ";");
		commentGenerator.addGeneralMethodComment(method, introspectedTable);
		topLevelClass.addMethod(method);
	}

	/*
	 * 检查xml参数是否正确
	 * 
	 * @see org.mybatis.generator.api.Plugin#validate(java.util.List)
	 */
	@Override
	public boolean validate(List<String> warnings) {

		return true;
	}

}