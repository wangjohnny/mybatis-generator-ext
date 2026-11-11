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
     * 按主键软删除：将 {@code is_deleted} 置为 1。
     * <p>
     * 仅当表存在软删除标记列（默认 {@code is_deleted}，可配置）时，生成的 XML 中才会提供对应 SQL；
     * 否则调用此方法会抛出 MyBatis 的 {@code Invalid bound statement} 异常。
     */
    int softDeleteByPrimaryKey(PK id);

    /**
     * 按条件软删除：将 {@code is_deleted} 置为 1。
     * <p>
     * 与 {@link #softDeleteByPrimaryKey(Object)} 相同，仅当表存在软删除标记列时才生成对应 SQL。
     */
    int softDeleteByExample(TE example);

}
