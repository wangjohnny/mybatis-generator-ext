package com.github.mybatis.mapper;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Param;

import com.github.mybatis.model.BaseModelExample;

/**
 * @author oking
 *
 * @param <T>
 * @param <TE>
 * @param <PK>
 */
public interface GenericMapper<T extends Serializable, TE extends BaseModelExample, PK extends Serializable> {

    long countByExample(TE example);

    int deleteByExample(TE example);

    int deleteByPrimaryKey(PK id);

    int insert(T record);

    int insertSelective(T record);

    List<T> selectByExample(TE example);

    Optional<T> selectByPrimaryKey(PK id);

    int updateByExampleSelective(@Param("record") T record, @Param("example") TE example);

    int updateByExample(@Param("record") T record, @Param("example") TE example);

    int updateByPrimaryKeySelective(T record);

    int updateByPrimaryKey(T record);

    /**
     * 按主键软删除：将 {@code is_deleted} 置为 1，同时更新 {@code deleted_by} 与 {@code deleted_at}。
     * <p>
     * 仅当表存在软删除标记列（默认 {@code is_deleted}，可配置）时，生成的 XML 中才会提供对应 SQL；
     * 否则调用此方法会抛出 MyBatis 的 {@code Invalid bound statement} 异常。
     * <p>
     * {@code deletedAt} 传入 null 时使用数据库当前时间（{@code CURRENT_TIMESTAMP}）。
     *
     * @param deletedBy 删除人 id（更新值）
     * @param deletedAt 删除时间，可为 null（更新值，此时用数据库当前时间）
     * @param id 主键（删除条件）
     */
    int softDeleteByPrimaryKey(@Param("deletedBy") Long deletedBy,
                               @Param("deletedAt") java.time.LocalDateTime deletedAt,
                               @Param("id") PK id);

    /**
     * 按条件软删除：将 {@code is_deleted} 置为 1，同时更新 {@code deleted_by} 与 {@code deleted_at}。
     * <p>
     * 与 {@link #softDeleteByPrimaryKey(Long, java.time.LocalDateTime, Object)} 相同，
     * 仅当表存在软删除标记列时才生成对应 SQL；{@code deletedAt} 为 null 时用数据库当前时间。
     *
     * @param deletedBy 删除人 id（更新值）
     * @param deletedAt 删除时间，可为 null（更新值，此时用数据库当前时间）
     * @param example 删除条件
     */
    int softDeleteByExample(@Param("deletedBy") Long deletedBy,
                            @Param("deletedAt") java.time.LocalDateTime deletedAt,
                            @Param("example") TE example);

}
