package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.util.List;
import java.util.Map;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.VisitableElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

/**
 * 软删除插件：为「含有软删除标记列」的表生成软删除相关 SQL。
 * <p>
 * 功能一 —— 软删除写操作：生成 {@code softDeleteByPrimaryKey} / {@code softDeleteByExample} 两个 update SQL，
 * 软删除时同时更新三列：
 * <ul>
 * <li>{@code is_deleted} 置 1（软删除标记）</li>
 * <li>{@code deleted_by} = 删除人 id（方法参数传入）</li>
 * <li>{@code deleted_at} = 删除时间（方法参数传入，为 null 时用数据库 {@code CURRENT_TIMESTAMP}）</li>
 * </ul>
 * 对应 {@link com.github.mybatis.mapper.CrudMapper#softDeleteByPrimaryKey} 与
 * {@link com.github.mybatis.mapper.GenericMapper#softDeleteByExample} 两个方法签名（更新值在前、条件在后）：
 * <ul>
 * <li>{@code int softDeleteByPrimaryKey(Long deletedBy, LocalDateTime deletedAt, PK id)} —— 按主键软删</li>
 * <li>{@code int softDeleteByExample(Long deletedBy, LocalDateTime deletedAt, TE example)} —— 按条件批量软删</li>
 * </ul>
 * <p>
 * 功能二 —— 查询自动过滤软删除：为含软删除标记列的表，在查询类 statement 里追加 {@code is_deleted = 0} 条件，
 * 过滤掉已软删除的记录：
 * <ul>
 * <li>{@code selectByPrimaryKey}：追加 {@code and is_deleted = 0}，只查未删除记录；</li>
 * <li>所有 ByExample 方法（{@code selectByExample} / {@code countByExample} / {@code selectByExampleWithBLOBs} /
 * {@code updateByExample} / {@code updateByExampleSelective} / {@code deleteByExample}）：通过往共享的
 * {@code Example_Where_Clause} 的 {@code <where>} 标签内追加 {@code and is_deleted = 0}，一并过滤；</li>
 * <li>{@code selectByPrimaryKeyRaw}（新增，对应 {@link com.github.mybatis.mapper.CrudMapper#selectByPrimaryKeyRaw}）：
 * 克隆 {@code selectByPrimaryKey} 生成，但<b>不加</b> {@code is_deleted} 条件，用于查全部记录（含已删除）。</li>
 * </ul>
 * <p>
 * 判断逻辑：仅当表含软删除标记列时，上述过滤才生效；无软删除标记列（或配置的列名写错、不存在）时，
 * 所有查询都退化为普通查询，{@code selectByPrimaryKeyRaw} 与 {@code selectByPrimaryKey} 行为一致。
 * 软删除写操作（{@code softDeleteByPrimaryKey} / {@code softDeleteByExample}）未生成 SQL 时，
 * 调用会抛 MyBatis {@code Invalid bound statement} 异常，即「该表没有此字段」的运行时错误。
 * <p>
 * 约定：含软删除标记列的表必定同时含 {@code deleted_by} 与 {@code deleted_at} 列，
 * 因此软删除写操作固定更新这三列，不做逐列存在性判断。
 * <p>
 * 软删除标记列名可配置（{@code softDeleteColumn}），配置优先级从高到低：
 * <ol>
 * <li>表级别（table 元素上 {@code <property name="softDeleteColumn" .../>}）</li>
 * <li>插件级别（SoftDeletePlugin 上 {@code <property name="softDeleteColumn" .../>}）</li>
 * <li>默认 {@code is_deleted} / {@code is_delete}（任一张表命中其一即可）</li>
 * </ol>
 * 均支持逗号分隔多个候选列名；无论哪一级，只要表里实际没有匹配的软删除标记列，
 * 就不会往 SQL 里加任何软删除条件（整体退化为普通查询）。
 * <p>
 * 用法（generatorConfig.xml）：
 * <pre>
 * &lt;!-- 表级别：仅对 user 表生效，优先级最高 --&gt;
 * &lt;table tableName="user"&gt;
 *     &lt;property name="softDeleteColumn" value="is_deleted"/&gt;
 * &lt;/table&gt;
 * &lt;!-- 插件级别：对未单独配置的所有表生效 --&gt;
 * &lt;plugin type="com.github.mybatis.generator.plugin.SoftDeletePlugin"&gt;
 *     &lt;property name="softDeleteColumn" value="is_deleted"/&gt;
 * &lt;/plugin&gt;
 * </pre>
 * 两者都不配置时，默认按 {@code is_deleted} / {@code is_delete} 两个候选列名匹配。
 *
 * @author oking
 */
public class SoftDeletePlugin extends PluginAdapter {

    /** 软删除标记列名配置项（table 级与 plugin 级共用此 key，可自定义，逗号分隔可配多个候选列名） */
    private static final String PROP_SOFT_DELETE_COLUMN = "softDeleteColumn";

    /** 默认兼容的软删除标记列名 */
    private static final String[] DEFAULT_SOFT_DELETE_COLUMNS = { "is_deleted", "is_delete" };

    /** 软删除标记置位的目标值（已删除） */
    private static final String DELETED_VALUE = "1";

    /** 软删除标记的未删除值（查询过滤用） */
    private static final String NOT_DELETED_VALUE = "0";

    /** 软删除审计列：删除人 */
    private static final String DELETED_BY_COLUMN = "deleted_by";

    /** 软删除审计列：删除时间 */
    private static final String DELETED_AT_COLUMN = "deleted_at";

    /** selectByPrimaryKey 的克隆方法名：查全部记录（含已删除），不加软删除过滤 */
    private static final String SELECT_BY_PRIMARY_KEY_RAW = "selectByPrimaryKeyRaw";

    /**
     * 暂存每张表的 selectByPrimaryKey 未加软删除过滤前的克隆元素，供生成 selectByPrimaryKeyRaw 使用。
     * <p>
     * 为什么用 Map 而非单实例字段：MBG 对多张表「先逐表生成 XML 元素（触发
     * {@link #sqlMapSelectByPrimaryKeyElementGenerated}）再逐表生成文档（触发
     * {@link #sqlMapDocumentGenerated}）」，PluginAggregator 跨表复用同一插件实例，
     * 单实例字段会在多表场景串表读错（与项目里 EntityInheritancePlugin 的既有教训一致）。
     */
    private final Map<IntrospectedTable, XmlElement> selectByPrimaryKeyRawRegistry = new java.util.HashMap<>();

    /** 插件级（SoftDeletePlugin property）软删除标记列候选名，作为表级配置的兜底 */
    private String[] pluginLevelCandidates;

    @Override
    public boolean validate(List<String> warnings) {
        // 插件级配置：仅在 table 级未配置时作为兜底使用
        String configured = properties.getProperty(PROP_SOFT_DELETE_COLUMN);
        if (stringHasValue(configured)) {
            pluginLevelCandidates = splitCandidates(configured);
        } else {
            pluginLevelCandidates = null;
        }
        return true;
    }

    /**
     * 取当前表的软删除标记列候选名（去空格后的小写形式用于比对）。
     * 优先级从高到低：
     * <ol>
     * <li>表级 property {@code softDeleteColumn}（table 元素上配置）</li>
     * <li>插件级 property {@code softDeleteColumn}（SoftDeletePlugin 上配置）</li>
     * <li>默认 {@code is_deleted} / {@code is_delete}</li>
     * </ol>
     *
     * @return 候选列名解析结果，含「是否来自显式配置」标记（用于区分配置错误与默认不匹配两种处理）
     */
    private SoftDeleteColumnCandidates resolveCandidates(IntrospectedTable introspectedTable) {
        String tableConfigured = introspectedTable.getTableConfigurationProperty(PROP_SOFT_DELETE_COLUMN);
        if (stringHasValue(tableConfigured)) {
            return new SoftDeleteColumnCandidates(splitCandidates(tableConfigured), true);
        }
        if (pluginLevelCandidates != null) {
            return new SoftDeleteColumnCandidates(pluginLevelCandidates, true);
        }
        return new SoftDeleteColumnCandidates(DEFAULT_SOFT_DELETE_COLUMNS, false);
    }

    /**
     * 软删除标记列候选名的解析结果：候选列名数组 + 是否来自显式配置。
     * <p>
     * {@code explicitlyConfigured=true} 表示候选来自 table 级或 plugin 级的 {@code softDeleteColumn} 配置；
     * 此时若表里无任何匹配列，属于「配置错误」，应报错中断。{@code false} 表示来自默认值
     * {@code is_deleted}/{@code is_delete}，表里无匹配列属正常情况（如纯关系表），仅提示、不中断。
     */
    private static final class SoftDeleteColumnCandidates {
        final String[] candidates;
        final boolean explicitlyConfigured;

        SoftDeleteColumnCandidates(String[] candidates, boolean explicitlyConfigured) {
            this.candidates = candidates;
            this.explicitlyConfigured = explicitlyConfigured;
        }
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
     * 替换元素的同名属性值；属性不存在则追加。{@link Attribute} 为不可变对象（name/value 均 final），
     * 无法直接 setValue，故采用「删除旧属性 + 新增同名属性」的方式实现替换。
     */
    private void replaceAttribute(XmlElement element, String name, String value) {
        element.getAttributes().removeIf(attribute -> name.equals(attribute.getName()));
        element.addAttribute(new Attribute(name, value));
    }

    /**
     * 判断当前表是否含软删除标记列。命中任意一个候选列名（忽略大小写）即认为有。
     */
    private boolean hasSoftDeleteColumn(IntrospectedTable introspectedTable) {
        return resolveSoftDeleteColumnName(introspectedTable) != null;
    }

    /**
     * 取软删除标记列的实际列名（保留数据库里的原始大小写，保证 SQL 正确）。
     * <p>
     * 无匹配列时按候选来源分别处理：
     * <ul>
     * <li>候选来自显式配置（table 级 / plugin 级的 {@code softDeleteColumn}）→ 视为<b>配置错误</b>，
     * 抛出 {@link RuntimeException} 中断生成，明确提示用户检查字段名；</li>
     * <li>候选来自默认值（{@code is_deleted}/{@code is_delete}）→ 属正常情况（如纯关系表无软删字段），
     * 静默返回 null，不中断，整体退化为普通查询（提示由调用方统一打印）。</li>
     * </ul>
     */
    private String resolveSoftDeleteColumnName(IntrospectedTable introspectedTable) {
        SoftDeleteColumnCandidates resolved = resolveCandidates(introspectedTable);
        String[] candidates = resolved.candidates;
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

        if (resolved.explicitlyConfigured) {
            // 显式配置了字段名但表里不存在：配置错误，报错中断
            throw new RuntimeException(String.format(
                    "表 %s 的软删除字段配置错误：配置的 softDeleteColumn=[%s] 在表中不存在，"
                            + "请检查 table 元素或 SoftDeletePlugin 的 softDeleteColumn 配置。",
                    introspectedTable.getFullyQualifiedTable().getIntrospectedTableName(),
                    String.join(", ", candidates)));
        }

        // 默认候选 is_deleted/is_delete 都不存在：正常情况，静默返回 null（退化）
        return null;
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        // 无软删除标记列的表不生成 softDelete 相关 SQL，调用时抛 Invalid bound statement
        if (!hasSoftDeleteColumn(introspectedTable)) {
            // 仅当候选来自默认值（非显式配置）时才走到这里；显式配置不匹配已在 resolveSoftDeleteColumnName 抛异常。
            // 这里统一打印一次「无软删除字段」的提示（纯关系表等正常场景，不中断）。
            System.out.printf("表 %s 不含软删除标记列（默认候选 is_deleted/is_delete 均未命中），"
                    + "软删除过滤与 softDelete 相关语句将不生成。%n",
                    introspectedTable.getFullyQualifiedTable().getIntrospectedTableName());
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

        // 生成 selectByPrimaryKeyRaw：克隆的未加过滤版本（id 已在克隆时改为 selectByPrimaryKeyRaw），直接追加到文档
        XmlElement rawElement = selectByPrimaryKeyRawRegistry.remove(introspectedTable);
        if (rawElement != null) {
            root.addElement(rawElement);
        }

        System.out.printf("表 %s 含软删除列 %s，已生成 softDelete / 查询过滤 / selectByPrimaryKeyRaw 相关语句%n",
                introspectedTable.getFullyQualifiedTable().getIntrospectedTableName(),
                softDeleteColumn);

        return true;
    }

    /**
     * selectByPrimaryKey 元素构建完成、加入父级之前触发：
     * 有软删除列时，先克隆一份未加过滤的原始元素（用于生成 selectByPrimaryKeyRaw），
     * 再往原元素末尾追加 {@code and is_deleted = 0}，过滤已软删除的记录。
     */
    @Override
    public boolean sqlMapSelectByPrimaryKeyElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        if (!hasSoftDeleteColumn(introspectedTable)) {
            return true;
        }
        String softDeleteColumn = resolveSoftDeleteColumnName(introspectedTable);
        if (!stringHasValue(softDeleteColumn)) {
            return true;
        }

        // 先克隆「未加过滤」的原始元素，改 id 为 selectByPrimaryKeyRaw，暂存给 sqlMapDocumentGenerated 使用
        // 注：XmlElement(XmlElement) 拷贝构造器会新建独立的 attributes / elements List（浅拷贝元素引用），
        //     因此克隆体不会受到后续往原元素追加过滤条件的影响。
        XmlElement rawElement = new XmlElement(element);
        replaceAttribute(rawElement, "id", SELECT_BY_PRIMARY_KEY_RAW);
        selectByPrimaryKeyRawRegistry.put(introspectedTable, rawElement);

        // 再往原元素末尾追加软删除过滤条件
        element.addElement(new TextElement("and " + softDeleteColumn + " = " + NOT_DELETED_VALUE));

        return true;
    }

    /**
     * Example_Where_Clause 片段生成完成时触发：往 {@code <where>} 标签内追加 {@code and is_deleted = 0}，
     * 使所有复用该片段的方法（selectByExample / countByExample / selectByExampleWithBLOBs /
     * updateByExample / updateByExampleSelective / deleteByExample）都自动过滤已软删除的记录。
     * <p>
     * {@code Example_Where_Clause} 结构为 {@code <sql><where>...动态条件...</where></sql>}，
     * 其中 {@code <where>} 是 MyBatis 动态标签，条件为空时自动省略 {@code where} 关键字、
     * 并去除首条 {@code and}，因此把静态过滤条件放进 {@code <where>} 内即可在 example 为空时
     * 仍生成合法的 {@code where is_deleted = 0}。
     */
    @Override
    public boolean sqlMapExampleWhereClauseElementGenerated(XmlElement element,
            IntrospectedTable introspectedTable) {
        if (!hasSoftDeleteColumn(introspectedTable)) {
            return true;
        }
        String softDeleteColumn = resolveSoftDeleteColumnName(introspectedTable);
        if (!stringHasValue(softDeleteColumn)) {
            return true;
        }

        // element 是 <sql id="Example_Where_Clause">，定位其子 <where> 元素
        for (VisitableElement child : element.getElements()) {
            if (child instanceof XmlElement && "where".equals(((XmlElement) child).getName())) {
                ((XmlElement) child).addElement(
                        new TextElement("and " + softDeleteColumn + " = " + NOT_DELETED_VALUE));
                break;
            }
        }

        return true;
    }

    /**
     * 生成 {@code softDeleteByPrimaryKey} 的 update 元素：
     * update 表名 set is_deleted = 1, deleted_by = #{deletedBy}, deleted_at = &lt;动态&gt; where 主键 = #{id}。
     */
    private XmlElement buildSoftDeleteByPrimaryKeyElement(IntrospectedTable introspectedTable,
            String softDeleteColumn) {
        XmlElement update = new XmlElement("update");
        update.addAttribute(new Attribute("id", "softDeleteByPrimaryKey"));

        List<IntrospectedColumn> pkColumns = introspectedTable.getPrimaryKeyColumns();
        if (!pkColumns.isEmpty()) {
            IntrospectedColumn pkColumn = pkColumns.get(0);
            update.addAttribute(new Attribute("parameterType", pkColumn.getFullyQualifiedJavaType().getFullyQualifiedName()));
        }

        String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();

        // update 表名 set is_deleted = 1, deleted_by = #{deletedBy},
        update.addElement(new TextElement(
                "update " + tableName + " set " + softDeleteColumn + " = " + DELETED_VALUE
                        + ", " + DELETED_BY_COLUMN + " = #{deletedBy,jdbcType=BIGINT},"));

        // deleted_at 动态列
        update.addElement(buildDeletedAtChooseElement("deletedAt"));

        // where 主键 = #{id}
        StringBuilder sb = new StringBuilder("where ");
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
     * update 表名 set is_deleted = 1, deleted_by = #{deletedBy}, deleted_at = &lt;动态&gt;，
     * where 条件复用 Update_By_Example_Where_Clause。
     */
    private XmlElement buildSoftDeleteByExampleElement(IntrospectedTable introspectedTable,
            String softDeleteColumn) {
        XmlElement update = new XmlElement("update");
        update.addAttribute(new Attribute("id", "softDeleteByExample"));
        update.addAttribute(new Attribute("parameterType", "map"));

        String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();

        // update 表名 set is_deleted = 1, deleted_by = #{deletedBy},
        update.addElement(new TextElement(
                "update " + tableName + " set " + softDeleteColumn + " = " + DELETED_VALUE
                        + ", " + DELETED_BY_COLUMN + " = #{deletedBy,jdbcType=BIGINT},"));

        // deleted_at 动态列
        update.addElement(buildDeletedAtChooseElement("deletedAt"));

        // where 条件复用
        XmlElement ifElement = new XmlElement("if");
        ifElement.addAttribute(new Attribute("test", "example != null"));
        XmlElement include = new XmlElement("include");
        include.addAttribute(new Attribute("refid", "Update_By_Example_Where_Clause"));
        ifElement.addElement(include);
        update.addElement(ifElement);

        return update;
    }

    /**
     * 生成 {@code deleted_at} 的动态赋值元素：
     * 属性路径非 null 时用参数值，否则用数据库当前时间。
     *
     * @param deletedAtPath {@code deleted_at} 取值所对应的 OGNL 属性路径（此处为 {@code deletedAt}）
     * <pre>
     * &lt;choose&gt;
     *   &lt;when test="deletedAt != null"&gt;deleted_at = #{deletedAt,jdbcType=TIMESTAMP}&lt;/when&gt;
     *   &lt;otherwise&gt;deleted_at = CURRENT_TIMESTAMP&lt;/otherwise&gt;
     * &lt;/choose&gt;
     * </pre>
     */
    private XmlElement buildDeletedAtChooseElement(String deletedAtPath) {
        XmlElement choose = new XmlElement("choose");

        XmlElement when = new XmlElement("when");
        when.addAttribute(new Attribute("test", deletedAtPath + " != null"));
        when.addElement(new TextElement(
                DELETED_AT_COLUMN + " = #{" + deletedAtPath + ",jdbcType=TIMESTAMP}"));
        choose.addElement(when);

        XmlElement otherwise = new XmlElement("otherwise");
        otherwise.addElement(new TextElement(
                DELETED_AT_COLUMN + " = CURRENT_TIMESTAMP"));
        choose.addElement(otherwise);

        return choose;
    }

}
