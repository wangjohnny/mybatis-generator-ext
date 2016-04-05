package com.github.mybatis.generator.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
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