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
 * 通用方法插件：为每张表补充一组「按主键批量 / 计数 / 存在性 / 按条件取单条」的通用查询方法，
 * 补全 {@link com.github.mybatis.mapper.CrudMapper} 与 {@link com.github.mybatis.mapper.GenericMapper}
 * 的方法对称性。
 * <p>
 * 生成的方法（全部对「所有表」生成，软删除过滤按表是否含软删列自动开关）：
 * <ul>
 * <li>{@code selectByPrimaryKeys(Collection<PK> ids)} —— 按主键集合批量查询（含软删列时过滤已删除）</li>
 * <li>{@code deleteByPrimaryKeys(Collection<PK> ids)} —— 按主键集合物理删除（不做软删过滤）</li>
 * <li>{@code existsByPrimaryKey(PK id)} —— 判断主键是否存在（含软删列时只判未删除）</li>
 * <li>{@code countAll()} —— 统计全表行数（含软删列时只统计未删除）</li>
 * <li>{@code countRaw()} —— 统计全表行数（含已软删除，不做过滤）</li>
 * <li>{@code selectOneByExample(TE example)} —— 按条件取单条（复用 Example_Where_Clause，自动带软删过滤）</li>
 * <li>{@code existsByExample(TE example)} —— 按条件判断是否存在（复用 Example_Where_Clause）</li>
 * </ul>
 * <p>
 * 软删除标记列名（{@code softDeleteColumn}）的三层优先级与 {@link SoftDeletePlugin} 完全一致：
 * table 级 → plugin 级 → 默认 {@code is_deleted}/{@code is_delete}；显式配置的字段名在表中不存在时报错中断，
 * 默认候选不存在时退化为普通查询（不加过滤）。
 *
 * @author oking
 */
public class GenericMethodPlugin extends PluginAdapter {

    /** 软删除标记列名配置项（table 级与 plugin 级共用此 key） */
    private static final String PROP_SOFT_DELETE_COLUMN = "softDeleteColumn";

    /** 默认兼容的软删除标记列名 */
    private static final String[] DEFAULT_SOFT_DELETE_COLUMNS = { "is_deleted", "is_delete" };

    /** 未删除标记值（查询过滤用） */
    private static final String NOT_DELETED_VALUE = "0";

    /** 插件级软删除标记列候选名，作为 table 级配置的兜底 */
    private String[] pluginLevelCandidates;

    @Override
    public boolean validate(List<String> warnings) {
        String configured = properties.getProperty(PROP_SOFT_DELETE_COLUMN);
        if (stringHasValue(configured)) {
            pluginLevelCandidates = splitCandidates(configured);
        } else {
            pluginLevelCandidates = null;
        }
        return true;
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        XmlElement root = document.getRootElement();

        // 软删除列（无则 null，用于过滤开关）
        String softDeleteColumn = resolveSoftDeleteColumnName(introspectedTable);

        root.addElement(buildSelectByPrimaryKeysElement(introspectedTable, softDeleteColumn));
        root.addElement(buildDeleteByPrimaryKeysElement(introspectedTable));
        root.addElement(buildExistsByPrimaryKeyElement(introspectedTable, softDeleteColumn));
        root.addElement(buildCountAllElement(introspectedTable, softDeleteColumn));
        root.addElement(buildCountRawElement(introspectedTable));
        root.addElement(buildSelectOneByExampleElement(introspectedTable));
        root.addElement(buildExistsByExampleElement(introspectedTable));

        return true;
    }

    // ==================== 软删除列解析（与 SoftDeletePlugin 三层优先级一致） ====================

    /**
     * 取当前表的软删除标记列实际列名；无匹配列时，显式配置则报错中断，默认候选则返回 null（退化）。
     */
    private String resolveSoftDeleteColumnName(IntrospectedTable introspectedTable) {
        String tableConfigured = introspectedTable.getTableConfigurationProperty(PROP_SOFT_DELETE_COLUMN);
        boolean explicitlyConfigured;
        String[] candidates;
        if (stringHasValue(tableConfigured)) {
            candidates = splitCandidates(tableConfigured);
            explicitlyConfigured = true;
        } else if (pluginLevelCandidates != null) {
            candidates = pluginLevelCandidates;
            explicitlyConfigured = true;
        } else {
            candidates = DEFAULT_SOFT_DELETE_COLUMNS;
            explicitlyConfigured = false;
        }

        for (IntrospectedColumn column : introspectedTable.getAllColumns()) {
            String actualColumnName = column.getActualColumnName();
            if (actualColumnName == null) {
                continue;
            }
            String lower = actualColumnName.toLowerCase();
            for (String candidate : candidates) {
                if (candidate.equals(lower)) {
                    return actualColumnName;
                }
            }
        }

        if (explicitlyConfigured) {
            throw new RuntimeException(String.format(
                    "表 %s 的软删除字段配置错误：配置的 softDeleteColumn=[%s] 在表中不存在，"
                            + "请检查 table 元素或 SoftDeletePlugin 的 softDeleteColumn 配置。",
                    introspectedTable.getFullyQualifiedTable().getIntrospectedTableName(),
                    String.join(", ", candidates)));
        }
        return null;
    }

    private String[] splitCandidates(String configured) {
        String[] parts = configured.split(",");
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].trim().toLowerCase();
        }
        return result;
    }

    // ==================== 六个方法的 XML 元素构造 ====================

    /**
     * {@code selectByPrimaryKeys}：select 全字段，where 主键 in (foreach ids)，含软删列时追加 is_deleted=0。
     */
    private XmlElement buildSelectByPrimaryKeysElement(IntrospectedTable introspectedTable,
            String softDeleteColumn) {
        XmlElement select = new XmlElement("select");
        select.addAttribute(new Attribute("id", "selectByPrimaryKeys"));
        select.addAttribute(new Attribute("resultMap", introspectedTable.getBaseResultMapId()));

        select.addElement(new TextElement("select"));
        select.addElement(buildBaseColumnListIncludeElement());
        select.addElement(new TextElement("from "
                + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()));

        IntrospectedColumn pkColumn = introspectedTable.getPrimaryKeyColumns().get(0);
        select.addElement(new TextElement("where " + pkColumn.getActualColumnName() + " in"));

        select.addElement(buildIdsForeachElement(pkColumn));

        if (stringHasValue(softDeleteColumn)) {
            select.addElement(new TextElement("and " + softDeleteColumn + " = " + NOT_DELETED_VALUE));
        }

        return select;
    }

    /**
     * {@code deleteByPrimaryKeys}：物理删除，where 主键 in (foreach ids)，不做软删过滤。
     */
    private XmlElement buildDeleteByPrimaryKeysElement(IntrospectedTable introspectedTable) {
        XmlElement delete = new XmlElement("delete");
        delete.addAttribute(new Attribute("id", "deleteByPrimaryKeys"));

        IntrospectedColumn pkColumn = introspectedTable.getPrimaryKeyColumns().get(0);
        delete.addElement(new TextElement("delete from "
                + introspectedTable.getFullyQualifiedTableNameAtRuntime()
                + " where " + pkColumn.getActualColumnName() + " in"));

        delete.addElement(buildIdsForeachElement(pkColumn));

        return delete;
    }

    /**
     * {@code existsByPrimaryKey}：select count(1)，where 主键=#{id}，含软删列时追加 is_deleted=0。
     */
    private XmlElement buildExistsByPrimaryKeyElement(IntrospectedTable introspectedTable,
            String softDeleteColumn) {
        XmlElement select = new XmlElement("select");
        select.addAttribute(new Attribute("id", "existsByPrimaryKey"));
        select.addAttribute(new Attribute("resultType", "boolean"));

        IntrospectedColumn pkColumn = introspectedTable.getPrimaryKeyColumns().get(0);

        StringBuilder sb = new StringBuilder("select count(1) from ")
                .append(introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime())
                .append(" where ").append(pkColumn.getActualColumnName())
                .append(" = #{id,jdbcType=").append(pkColumn.getJdbcTypeName()).append("}");
        if (stringHasValue(softDeleteColumn)) {
            sb.append(" and ").append(softDeleteColumn).append(" = ").append(NOT_DELETED_VALUE);
        }
        select.addElement(new TextElement(sb.toString()));

        return select;
    }

    /**
     * {@code countAll}：select count(*)，含软删列时加 where is_deleted=0（只统计未删除）。
     */
    private XmlElement buildCountAllElement(IntrospectedTable introspectedTable, String softDeleteColumn) {
        XmlElement select = new XmlElement("select");
        select.addAttribute(new Attribute("id", "countAll"));
        select.addAttribute(new Attribute("resultType", "java.lang.Long"));

        StringBuilder sb = new StringBuilder("select count(*) from ")
                .append(introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime());
        if (stringHasValue(softDeleteColumn)) {
            sb.append(" where ").append(softDeleteColumn).append(" = ").append(NOT_DELETED_VALUE);
        }
        select.addElement(new TextElement(sb.toString()));

        return select;
    }

    /**
     * {@code countRaw}：select count(*)，不加软删除过滤（统计全部，含已删除）。
     */
    private XmlElement buildCountRawElement(IntrospectedTable introspectedTable) {
        XmlElement select = new XmlElement("select");
        select.addAttribute(new Attribute("id", "countRaw"));
        select.addAttribute(new Attribute("resultType", "java.lang.Long"));

        select.addElement(new TextElement("select count(*) from "
                + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()));

        return select;
    }

    /**
     * {@code selectOneByExample}：select 全字段，复用 Example_Where_Clause（自动带软删过滤），limit 1。
     */
    private XmlElement buildSelectOneByExampleElement(IntrospectedTable introspectedTable) {
        XmlElement select = new XmlElement("select");
        select.addAttribute(new Attribute("id", "selectOneByExample"));
        select.addAttribute(new Attribute("parameterType", introspectedTable.getExampleType()));
        select.addAttribute(new Attribute("resultMap", introspectedTable.getBaseResultMapId()));

        select.addElement(new TextElement("select"));
        select.addElement(buildBaseColumnListIncludeElement());
        select.addElement(new TextElement("from "
                + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()));

        select.addElement(buildExampleWhereClauseIncludeElement());

        select.addElement(new TextElement("limit 1"));

        return select;
    }

    /**
     * {@code existsByExample}：select count(1)，复用 Example_Where_Clause（自动带软删过滤）。
     */
    private XmlElement buildExistsByExampleElement(IntrospectedTable introspectedTable) {
        XmlElement select = new XmlElement("select");
        select.addAttribute(new Attribute("id", "existsByExample"));
        select.addAttribute(new Attribute("parameterType", introspectedTable.getExampleType()));
        select.addAttribute(new Attribute("resultType", "boolean"));

        select.addElement(new TextElement("select count(1) from "
                + introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime()));

        select.addElement(buildExampleWhereClauseIncludeElement());

        return select;
    }

    // ==================== 公共 XML 片段 ====================

    /** {@code <include refid="Base_Column_List" />} */
    private XmlElement buildBaseColumnListIncludeElement() {
        XmlElement include = new XmlElement("include");
        include.addAttribute(new Attribute("refid", "Base_Column_List"));
        return include;
    }

    /** {@code <include refid="Example_Where_Clause" />}（包在 _parameter != null 判断里） */
    private XmlElement buildExampleWhereClauseIncludeElement() {
        XmlElement ifElement = new XmlElement("if");
        ifElement.addAttribute(new Attribute("test", "_parameter != null"));
        XmlElement include = new XmlElement("include");
        include.addAttribute(new Attribute("refid", "Example_Where_Clause"));
        ifElement.addElement(include);
        return ifElement;
    }

    /** {@code <foreach collection="ids" item="item" open="(" separator="," close=")">#{item}</foreach>} */
    private XmlElement buildIdsForeachElement(IntrospectedColumn pkColumn) {
        XmlElement foreach = new XmlElement("foreach");
        foreach.addAttribute(new Attribute("collection", "ids"));
        foreach.addAttribute(new Attribute("item", "item"));
        foreach.addAttribute(new Attribute("open", "("));
        foreach.addAttribute(new Attribute("separator", ","));
        foreach.addAttribute(new Attribute("close", ")"));
        foreach.addElement(new TextElement("#{item,jdbcType=" + pkColumn.getJdbcTypeName() + "}"));
        return foreach;
    }

}
