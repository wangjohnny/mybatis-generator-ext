package com.github.mybatis.mapper;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

/**
 * 通用 Mapper 基座：只含「按主键操作」的 CRUD，不涉及 {@code example} 条件对象。
 * <p>
 * 与 {@link GenericMapper} 的分工：本接口承载「以主键为准」的最基础操作；
 * {@link GenericMapper} 在本接口之上追加「按 example 条件批量操作」的能力。
 * 对于不需要 example（如简单关系表）的场景，可让 Mapper 直接继承本接口。
 *
 * @param <T>  实体类型
 * @param <PK> 主键类型
 * @author oking
 */
public interface CrudMapper<T extends Serializable, PK extends Serializable> {

    /**
     * 按主键删除（物理删除）。
     *
     * @param id 主键
     * @return 受影响行数
     */
    int deleteByPrimaryKey(PK id);

    /**
     * 按主键集合批量删除（物理删除）。
     *
     * @param ids 主键集合
     * @return 受影响行数
     */
    int deleteByPrimaryKeys(@Param("ids") Collection<PK> ids);

    /**
     * 全量插入：所有字段（含 null）都会写入 insert 语句。
     *
     * @param record 实体
     * @return 受影响行数
     */
    int insert(T record);

    /**
     * 选择性插入：只写入非 null 字段。
     *
     * @param record 实体
     * @return 受影响行数
     */
    int insertSelective(T record);

    /**
     * 批量插入：一次性插入多条记录，所有字段（含 null）都会写入 insert 语句。
     * <p>
     * SQL 采用 MySQL 多值 values 语法（单条 SQL，性能最优），与 {@link #insert} 使用相同的列集合。
     * 注意：多值 values 语法并非所有数据库都支持，使用前需确认目标数据库兼容。
     *
     * @param list 实体集合
     * @return 受影响行数
     */
    int batchInsert(@Param("list") Collection<T> list);

    /**
     * 按主键查询，返回 {@link Optional} 包装的实体（不存在时为 {@link Optional#empty()}）。
     *
     * @param id 主键
     * @return 实体，可能为空
     */
    Optional<T> selectByPrimaryKey(PK id);

    /**
     * 按主键查询「全部记录（含已软删除）」，返回 {@link Optional} 包装的实体（不存在时为 {@link Optional#empty()}）。
     * <p>
     * 与 {@link #selectByPrimaryKey} 的区别：本方法<b>不做软删除过滤</b>，即使用于软删除的表也能查到
     * 已被软删除（{@code is_deleted = 1}）的记录；而 {@link #selectByPrimaryKey} 对含软删除标记列的表
     * 会自动追加 {@code is_deleted = 0} 条件。对于不含软删除标记列的表，两者行为完全一致。
     *
     * @param id 主键
     * @return 实体（含已删除），可能为空
     */
    Optional<T> selectByPrimaryKeyRaw(PK id);

    /**
     * 按主键集合批量查询。含软删除标记列的表会自动追加 {@code is_deleted = 0} 过滤已软删除的记录。
     *
     * @param ids 主键集合
     * @return 实体列表（仅含未删除记录）
     */
    List<T> selectByPrimaryKeys(@Param("ids") Collection<PK> ids);

    /**
     * 判断主键对应的记录是否存在。含软删除标记列的表只判断「未删除」的记录。
     *
     * @param id 主键
     * @return 存在返回 {@code true}，否则 {@code false}
     */
    boolean existsByPrimaryKey(PK id);

    /**
     * 统计全表行数。含软删除标记列的表只统计「未删除」的记录。
     *
     * @return 行数
     */
    long count();

    /**
     * 按主键选择性更新：只更新非 null 字段。
     *
     * @param record 实体
     * @return 受影响行数
     */
    int updateByPrimaryKeySelective(T record);

    /**
     * 按主键全量更新：所有字段都会写入 update 语句（含 null）。
     *
     * @param record 实体
     * @return 受影响行数
     */
    int updateByPrimaryKey(T record);

    /**
     * 按主键软删除：将软删除标记列（默认 {@code is_deleted}）置为 1，同时更新 {@code deleted_by} 与 {@code deleted_at}。
     * <p>
     * 仅当表存在软删除标记列时，生成的 XML 才会提供对应 SQL；
     * 否则调用此方法会抛出 MyBatis 的 {@code Invalid bound statement} 异常。
     * {@code deletedAt} 传入 null 时使用数据库当前时间（{@code CURRENT_TIMESTAMP}）。
     *
     * @param deletedBy 删除人 id（更新值）
     * @param deletedAt 删除时间，可为 null（更新值，此时用数据库当前时间）
     * @param id        主键（删除条件）
     * @return 受影响行数
     */
    int softDeleteByPrimaryKey(@Param("deletedBy") Long deletedBy,
                               @Param("deletedAt") LocalDateTime deletedAt,
                               @Param("id") PK id);

    /**
     * 按主键集合批量软删除：将软删除标记列（默认 {@code is_deleted}）置为 1，同时更新 {@code deleted_by} 与 {@code deleted_at}。
     * <p>
     * 与 {@link #softDeleteByPrimaryKey} 相同，仅当表存在软删除标记列时才生成对应 SQL；
     * {@code deletedAt} 传入 null 时使用数据库当前时间（{@code CURRENT_TIMESTAMP}）。
     *
     * @param deletedBy 删除人 id（更新值）
     * @param deletedAt 删除时间，可为 null（更新值，此时用数据库当前时间）
     * @param ids       主键集合（删除条件，{@code where 主键 in (...)}）
     * @return 受影响行数
     */
    int batchSoftDeleteByIds(@Param("deletedBy") Long deletedBy,
                             @Param("deletedAt") LocalDateTime deletedAt,
                             @Param("ids") Collection<PK> ids);

}
