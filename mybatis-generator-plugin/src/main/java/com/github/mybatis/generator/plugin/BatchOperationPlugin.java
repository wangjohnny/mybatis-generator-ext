package com.github.mybatis.generator.plugin;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.ListUtilities;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

/**
 * 批量操作插件：为每张表生成 {@code batchInsert} 语句，支持一次性批量插入多条记录。
 * <p>
 * 对应 {@link com.github.mybatis.mapper.CrudMapper#batchInsert} 方法签名：
 * {@code int batchInsert(@Param("list") Collection<T> list)}。
 * <p>
 * SQL 采用 MySQL 多值 values 语法（单条 SQL，性能最优）：
 * <pre>
 * insert into 表名 (列1, 列2, ...) values
 *   (#{item.列1}, #{item.列2}, ...),
 *   (#{item.列1}, #{item.列2}, ...)
 * </pre>
 * 列集合与单条 {@code insert} 一致（{@code getAllColumns()} 排除自增/生成列，含 BLOB 列），
 * 列名经 {@link MyBatis3FormattingUtilities#getEscapedColumnName} 处理，自动给保留字加定界符。
 * <p>
 * 注意：多值 values 语法并非所有数据库都支持（MySQL / PostgreSQL 支持，HSQLDB / SQL Server 等不支持），
 * 使用前需确认目标数据库兼容。
 *
 * @author oking
 */
public class BatchOperationPlugin extends PluginAdapter {

    /** 批量插入方法名（与 CrudMapper.batchInsert 一致） */
    private static final String BATCH_INSERT = "batchInsert";

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        XmlElement root = document.getRootElement();
        root.addElement(buildInsertBatchElement(introspectedTable));
        return true;
    }

    /**
     * 生成 {@code batchInsert} 的 insert 元素（MySQL 多值 values 语法）。
     */
    private XmlElement buildInsertBatchElement(IntrospectedTable introspectedTable) {
        // 与单条 insert 一致：排除自增/生成列
        List<IntrospectedColumn> columns = ListUtilities
                .removeIdentityAndGeneratedAlwaysColumns(introspectedTable.getAllColumns());

        XmlElement insert = new XmlElement("insert");
        insert.addAttribute(new Attribute("id", BATCH_INSERT));
        insert.addAttribute(new Attribute("parameterType", "map"));

        String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();

        // 列名列表：insert into 表名 (列1, 列2, ...)，列名用 getEscapedColumnName 处理定界符
        StringBuilder columnList = new StringBuilder("insert into ").append(tableName).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                columnList.append(", ");
            }
            columnList.append(MyBatis3FormattingUtilities.getEscapedColumnName(columns.get(i)));
        }
        columnList.append(") values");
        insert.addElement(new TextElement(columnList.toString()));

        // foreach 多值：(#{item.col1}, #{item.col2}), (...)
        XmlElement foreach = new XmlElement("foreach");
        foreach.addAttribute(new Attribute("collection", "list"));
        foreach.addAttribute(new Attribute("item", "item"));
        foreach.addAttribute(new Attribute("separator", ","));

        StringBuilder values = new StringBuilder("(");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                values.append(", ");
            }
            IntrospectedColumn column = columns.get(i);
            values.append("#{item.").append(column.getJavaProperty())
                    .append(",jdbcType=").append(column.getJdbcTypeName()).append("}");
        }
        values.append(")");
        foreach.addElement(new TextElement(values.toString()));

        insert.addElement(foreach);

        return insert;
    }

}
