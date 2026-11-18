package com.github.mybatis.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import com.github.mybatis.model.BaseModelExample;

/**
 * 通用 Mapper 接口：在 {@link CrudMapper} 的基础上，追加「按 example 条件批量操作」的能力。
 * <p>
 * 继承链：{@code GenericMapper} 继承 {@link CrudMapper}，因此自动获得按主键的 CRUD 与
 * {@code softDeleteByPrimaryKey}，本接口只声明与 example 相关的操作。
 *
 * @param <T>  实体类型
 * @param <TE> example 条件类型（继承自 {@link BaseModelExample}）
 * @param <PK> 主键类型
 * @author oking
 */
public interface GenericMapper<T extends Serializable, TE extends BaseModelExample, PK extends Serializable>
        extends CrudMapper<T, PK> {

    /**
     * 按 example 条件统计行数。
     *
     * @param example 查询条件
     * @return 行数
     */
    long countByExample(TE example);

    /**
     * 按 example 条件删除（物理删除）。
     *
     * @param example 删除条件
     * @return 受影响行数
     */
    int deleteByExample(TE example);

    /**
     * 按 example 条件查询，返回实体列表。
     *
     * @param example 查询条件
     * @return 实体列表
     */
    List<T> selectByExample(TE example);

    /**
     * 按 example 条件查询单条记录，返回 {@link Optional} 包装的实体（不存在或命中多条时语义见实现）。
     * <p>
     * SQL 层追加 {@code limit 1}，仅取第一条匹配记录；含软删除标记列的表自动过滤已软删除记录。
     *
     * @param example 查询条件
     * @return 实体，可能为空
     */
    Optional<T> selectOneByExample(TE example);

    /**
     * 按 example 条件判断是否存在匹配记录。含软删除标记列的表只判断「未删除」的记录。
     *
     * @param example 查询条件
     * @return 存在返回 {@code true}，否则 {@code false}
     */
    boolean existsByExample(TE example);

    /**
     * 按 example 条件选择性更新：只更新 {@code record} 中非 null 字段。
     *
     * @param record  更新值
     * @param example 更新条件
     * @return 受影响行数
     */
    int updateByExampleSelective(@Param("record") T record, @Param("example") TE example);

    /**
     * 按 example 条件全量更新：{@code record} 中所有字段（含 null）都会写入。
     *
     * @param record  更新值
     * @param example 更新条件
     * @return 受影响行数
     */
    int updateByExample(@Param("record") T record, @Param("example") TE example);

    /**
     * 按 example 条件软删除：将软删除标记列（默认 {@code is_deleted}）置为 1，同时更新 {@code deleted_by} 与 {@code deleted_at}。
     * <p>
     * 与 {@link CrudMapper#softDeleteByPrimaryKey} 相同，仅当表存在软删除标记列时才生成对应 SQL；
     * {@code deletedAt} 为 null 时用数据库当前时间。
     *
     * @param deletedBy 删除人 id（更新值）
     * @param deletedAt 删除时间，可为 null（更新值，此时用数据库当前时间）
     * @param example   删除条件
     * @return 受影响行数
     */
    int softDeleteByExample(@Param("deletedBy") Long deletedBy,
                            @Param("deletedAt") java.time.LocalDateTime deletedAt,
                            @Param("example") TE example);

}
