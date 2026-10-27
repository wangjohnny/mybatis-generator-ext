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
 * @author oking
 */
public class MysqlPaginationPlugin extends PluginAdapter {

	@Override
	public boolean sqlMapSelectByExampleWithoutBLOBsElementGenerated(
	        XmlElement element, IntrospectedTable introspectedTable) {

		addPaginationElement(element);

		// 分页片段已追加，保留原 select 元素
		return true;
	}

	/**
	 * 带 BLOB 字段的表会走 selectByExampleWithBLOBs，这里同样要追加分页片段，
	 * 否则这类表的分页不生效
	 */
	@Override
	public boolean sqlMapSelectByExampleWithBLOBsElementGenerated(
	        XmlElement element, IntrospectedTable introspectedTable) {

		addPaginationElement(element);

		// 分页片段已追加，保留原 select 元素
		return true;
	}

	/**
	 * 在 select 元素末尾追加分页片段，生成 limit 语句
	 *
	 * @param element select 元素
	 */
	private void addPaginationElement(XmlElement element) {
		XmlElement pageElement = new XmlElement("if");
		pageElement.addAttribute(new Attribute("test",
		        "pagination != null and pagination.begin >= 0"));
		pageElement.addElement(new TextElement(
		        "limit #{pagination.begin}, #{pagination.pageSize}"));
		element.getElements().add(pageElement);
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