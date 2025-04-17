package com.github.mybatis.service;

import java.io.Serializable;
import java.util.List;

import com.github.mybatis.mapper.GenericMapper;
import com.github.mybatis.model.BaseModelExample;
import com.github.mybatis.pagination.Pagination;

public abstract class GenericServiceImpl<T extends Serializable, TE extends BaseModelExample, PK extends Serializable>
        implements GenericService<T, TE, PK> {

    protected abstract GenericMapper<T, TE, PK> getGenericMapper();

    public Pagination<T> selectByPage(Integer begin, Integer pageSize, TE te) {

        Pagination<T> pagination = new Pagination<>(begin, pageSize);
        te.setPagination(pagination);

        List<T> list = getGenericMapper().selectByExample(te);
        pagination.setDataList(list);

        long total = getGenericMapper().countByExample(te);
        pagination.setTotalRowNumber(total);

        return pagination;
    }
}
