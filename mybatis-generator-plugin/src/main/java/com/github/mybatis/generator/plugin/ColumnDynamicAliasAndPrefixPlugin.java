package com.github.mybatis.generator.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

/**
 * 给 xml 文件的 Base_Column_List 添加别名机制，便于多表 join 时复用列清单。
 * <p>
 * 调用方通过 {@code <include>} 传入表别名 {@code _table_alias}（纯别名，不含点号），
 * 插件自动推导出两种形态：
 * <ul>
 * <li>表限定：{@code ${_table_alias}.COLUMN}，如 {@code o.SID}</li>
 * <li>列别名（防止 join 表同名字段冲突）：{@code ${_table_alias}_COLUMN}，如 {@code o_SID}</li>
 * </ul>
 * <p>
 * 用法（扩展 xml 里 join 时）：
 * <pre>
 * &lt;include refid="Base_Column_List"&gt;
 *     &lt;property name="_table_alias" value="o"/&gt;
 * &lt;/include&gt;
 * </pre>
 * 生成 {@code o.SID as o_SID, o.USERNAME as o_USERNAME, ...}。
 * 不传 {@code _table_alias} 时走 otherwise 分支，输出纯列名（反引号包裹）。
 *
 * @author lao king
 */
public class ColumnDynamicAliasAndPrefixPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean sqlMapBaseColumnListElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        StringBuilder columnsWithAlias = new StringBuilder();
        StringBuilder columns = new StringBuilder();
        List<IntrospectedColumn> allColumns = introspectedTable.getAllColumns();

        for (int i = 0; i < allColumns.size(); i++) {
            IntrospectedColumn column = allColumns.get(i);
            if (columnsWithAlias.length() > 0) {
                columnsWithAlias.append(", ");
                columnsWithAlias.append(System.lineSeparator());
                columns.append(", ");
                columns.append(System.lineSeparator());
            }
            String columnName = column.getActualColumnName();
            // 表限定 ${_table_alias}.COLUMN，列别名 ${_table_alias}_COLUMN（防止同名字段冲突）
            columnsWithAlias.append(String.format("%s${_table_alias}.%s as ${_table_alias}_%s"
                    , " ".repeat(8), columnName, columnName));
            columns.append(String.format("%s`%s`", " ".repeat(8), columnName));
        }

        String content = """
                <choose>
                    <when test="_table_alias != null and _table_alias != ''">
                ${columnsWithAlias}
                    </when>
                    <otherwise>
                ${columns}
                    </otherwise>
                </choose>
                """.replace("${columnsWithAlias}", columnsWithAlias)
                   .replace("${columns}", columns);

        element.getElements().clear();
        element.addElement(new TextElement(content));
        return true;
    }
}