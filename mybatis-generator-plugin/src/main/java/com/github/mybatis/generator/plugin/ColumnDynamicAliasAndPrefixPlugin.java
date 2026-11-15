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
 * </ul>
 * <p>
 * 用法（扩展 xml 里 join 时）：
 * <pre>
 * &lt;include refid="Base_Column_List"&gt;
 *     &lt;property name="_table_alias" value="o"/&gt;
 * &lt;/include&gt;
 * </pre>
 * 生成 {@code o.SID, o.USERNAME, ...}（仅表限定，列名保持原名，便于自动映射实体）。
 * 不传 {@code _table_alias} 时走 otherwise 分支，输出纯列名（反引号包裹）。
 * <p>
 * <b>注意</b>：{@code <when test="'${_table_alias}' != ''">} 里的 {@code ${_table_alias}} 是
 * <b>解析期</b>占位符（被 include 的 property 或全局 properties 替换），不是运行期 OGNL 变量。
 * 因此「不传 property」时必须在 MyBatis 全局 {@code <properties>}（或 Spring Boot 的
 * {@code mybatis.configuration-properties}）里预置 {@code _table_alias=}（空串），
 * 让 {@code ${_table_alias}} 解析期被替换成空串，{@code test} 变成 {@code '' != ''} 走 otherwise。
 * 否则不传 property 时 {@code ${_table_alias}} 未被替换、落入运行期 OGNL 求值，
 * 在有 {@code @Param} 的方法下会因访问 ParamMap 不存在的 key 抛 {@code BindingException}。
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
            // 仅表限定 ${_table_alias}.COLUMN，不加列别名，保证列名仍是原始列名、可自动映射实体
            columnsWithAlias.append(String.format("%s${_table_alias}.%s"
                    , " ".repeat(8), columnName));
            columns.append(String.format("%s`%s`", " ".repeat(8), columnName));
        }

        String content = """
                <choose>
                    <when test="'${_table_alias}' != ''">
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