package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.util.List;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

/**
 * 软删除插件：为「含有软删除标记列」的表生成 {@code softDelete} / {@code softDeleteByExample}
 * 两个 update SQL（将标记列置为 1）。
 * <p>
 * 对应 {@link com.github.mybatis.mapper.GenericMapper} 上新增的两个方法签名：
 * <ul>
 * <li>{@code int softDelete(PK id)} —— 按主键软删</li>
 * <li>{@code int softDeleteByExample(TE example)} —— 按条件批量软删</li>
 * </ul>
 * <p>
 * 判断逻辑：仅当表含软删除标记列时，才往生成的 XML（{@code generated/XxxMapper.xml}）里追加
 * 这两个 statement；否则不生成。未生成 SQL 的表调用 {@code softDelete} 时，MyBatis 会抛出
 * {@code Invalid bound statement} 异常，即「该表没有此字段」的运行时错误。
 * <p>
 * 软删除标记列名可配置（{@code softDeleteColumn}），默认兼容两种常见命名：
 * {@code is_deleted} 与 {@code is_delete}，任一张表命中其一即可。
 * <p>
 * 用法（generatorConfig.xml）：
 * <pre>
 * &lt;plugin type="com.github.mybatis.generator.plugin.SoftDeletePlugin"&gt;
 *     &lt;property name="softDeleteColumn" value="is_deleted"/&gt;
 * &lt;/plugin&gt;
 * </pre>
 *
 * @author oking
 */
public class SoftDeletePlugin extends PluginAdapter {

    /** 软删除标记列名配置项（可自定义，逗号分隔可配多个候选列名） */
    private static final String PROP_SOFT_DELETE_COLUMN = "softDeleteColumn";

    /** 默认兼容的软删除标记列名 */
    private static final String[] DEFAULT_SOFT_DELETE_COLUMNS = { "is_deleted", "is_delete" };

    /** 软删除标记置位的目标值 */
    private static final String DELETED_VALUE = "1";

    /** 软删除标记列名候选（去空格后的小写形式用于比对） */
    private String[] softDeleteColumnCandidates;

    @Override
    public boolean validate(List<String> warnings) {
        String configured = properties.getProperty(PROP_SOFT_DELETE_COLUMN);
        if (stringHasValue(configured)) {
            softDeleteColumnCandidates = splitCandidates(configured);
        } else {
            softDeleteColumnCandidates = DEFAULT_SOFT_DELETE_COLUMNS;
        }
        return true;
    }

    /**
     * 把逗号分隔的配置拆成小写去空格后的候选列名数组。
     */
    private String[] splitCandidates(String configured) {
        String[] parts = configured.split(",");
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].trim().toLowerCase();
        }
        return result;
    }

    /**
     * 判断当前表是否含软删除标记列。命中任意一个候选列名（忽略大小写）即认为有。
     */
    private boolean hasSoftDeleteColumn(IntrospectedTable introspectedTable) {
        for (IntrospectedColumn column : introspectedTable.getAllColumns()) {
            String actualColumnName = column.getActualColumnName();
            if (actualColumnName == null) {
                continue;
            }
            String lower = actualColumnName.toLowerCase();
            for (String candidate : softDeleteColumnCandidates) {
                if (candidate.equals(lower)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 取软删除标记列的实际列名（保留数据库里的原始大小写，保证 SQL 正确）。
     */
    private String resolveSoftDeleteColumnName(IntrospectedTable introspectedTable) {
        for (IntrospectedColumn column : introspectedTable.getAllColumns()) {
            String actualColumnName = column.getActualColumnName();
            if (actualColumnName == null) {
                continue;
            }
            String lower = actualColumnName.toLowerCase();
            for (String candidate : softDeleteColumnCandidates) {
                if (candidate.equals(lower)) {
                    return actualColumnName;
                }
            }
        }
        return null;
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        // 无软删除标记列的表不生成 softDelete SQL，调用时抛 Invalid bound statement
        if (!hasSoftDeleteColumn(introspectedTable)) {
            return true;
        }

        String softDeleteColumn = resolveSoftDeleteColumnName(introspectedTable);
        if (!stringHasValue(softDeleteColumn)) {
            return true;
        }

        XmlElement root = document.getRootElement();

        // 按主键软删
        root.addElement(buildSoftDeleteByPrimaryKeyElement(introspectedTable, softDeleteColumn));

        // 按条件软删
        root.addElement(buildSoftDeleteByExampleElement(introspectedTable, softDeleteColumn));

        System.out.printf("表 %s 含软删除列 %s，已生成 softDelete / softDeleteByExample 语句%n",
                introspectedTable.getFullyQualifiedTable().getIntrospectedTableName(),
                softDeleteColumn);

        return true;
    }

    /**
     * 生成 {@code softDelete} 的 update 元素：update 表名 set 软删除列 = 1 where 主键 = #{id}。
     */
    private XmlElement buildSoftDeleteByPrimaryKeyElement(IntrospectedTable introspectedTable,
            String softDeleteColumn) {
        XmlElement update = new XmlElement("update");
        update.addAttribute(new Attribute("id", "softDelete"));

        List<IntrospectedColumn> pkColumns = introspectedTable.getPrimaryKeyColumns();
        if (!pkColumns.isEmpty()) {
            IntrospectedColumn pkColumn = pkColumns.get(0);
            update.addAttribute(new Attribute("parameterType", pkColumn.getFullyQualifiedJavaType().getFullyQualifiedName()));
        }

        String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(tableName).append(" set ").append(softDeleteColumn)
                .append(" = ").append(DELETED_VALUE).append(" where ");

        if (pkColumns.isEmpty()) {
            // 理论上不会有主键为空的表，这里兜底防止生成不完整 SQL
            sb.append("1 = 0");
        } else {
            IntrospectedColumn pkColumn = pkColumns.get(0);
            sb.append(pkColumn.getActualColumnName()).append(" = #{id,jdbcType=")
                    .append(pkColumn.getJdbcTypeName()).append("}");
        }

        update.addElement(new TextElement(sb.toString()));
        return update;
    }

    /**
     * 生成 {@code softDeleteByExample} 的 update 元素：
     * update 表名 set 软删除列 = 1，where 条件复用 Example_Where_Clause。
     */
    private XmlElement buildSoftDeleteByExampleElement(IntrospectedTable introspectedTable,
            String softDeleteColumn) {
        XmlElement update = new XmlElement("update");
        update.addAttribute(new Attribute("id", "softDeleteByExample"));
        update.addAttribute(new Attribute("parameterType", "map"));

        String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
        update.addElement(new TextElement(
                "update " + tableName + " set " + softDeleteColumn + " = " + DELETED_VALUE));

        XmlElement ifElement = new XmlElement("if");
        ifElement.addAttribute(new Attribute("test", "example != null"));
        XmlElement include = new XmlElement("include");
        include.addAttribute(new Attribute("refid", "Update_By_Example_Where_Clause"));
        ifElement.addElement(include);
        update.addElement(ifElement);

        return update;
    }

}
