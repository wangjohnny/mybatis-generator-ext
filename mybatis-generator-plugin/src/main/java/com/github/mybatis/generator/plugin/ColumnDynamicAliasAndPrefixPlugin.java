package com.github.mybatis.generator.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

/**
 * 给 xml 文件的BaseColumnList添加可配置的前缀
 * 
 * @author lao king
 */
public class ColumnDynamicAliasAndPrefixPlugin extends PluginAdapter {
//    private String defaultPrefix = "";

    @Override
    public boolean validate(List<String> warnings) {
//        tableAlias = properties.getProperty("_table_alias", "");
        return true;
    }

    @Override
    public boolean sqlMapBaseColumnListElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        StringBuilder columnsWithPrefix = new StringBuilder();
        StringBuilder columns = new StringBuilder();
        List<IntrospectedColumn> allColumns = introspectedTable.getAllColumns();

        for (int i = 0; i < allColumns.size(); i++) {
            IntrospectedColumn column = allColumns.get(i);
            if (columnsWithPrefix.length() > 0) {
                columnsWithPrefix.append(", ");
                columnsWithPrefix.append(System.lineSeparator());
                columns.append(", ");
                columns.append(System.lineSeparator());
            }
            String columnName = column.getActualColumnName();
            columnsWithPrefix.append(String.format("%s${_table_alias}%s as ${_table_prefix}%s"
                    , " ".repeat(8), columnName, columnName));
//          columnsWithPrefix.append(String.format("${prefix}%s", columnName));
            columns.append(String.format("%s`%s`", " ".repeat(8), columnName));
        }

        String content = """
                <choose>
                    <when test="'${_table_alias}' != ''">
                ${columnsWithPrefix}
                    </when>
                    <otherwise>
                ${columns}
                    </otherwise>
                </choose>
                """.replace("${columnsWithPrefix}", columnsWithPrefix)
                   .replace("${columns}", columns);

        element.getElements().clear();
        element.addElement(new TextElement(content));
        return true;
    }
}