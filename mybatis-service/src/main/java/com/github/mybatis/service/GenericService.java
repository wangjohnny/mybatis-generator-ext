package com.github.mybatis.service;

import java.io.Serializable;

import com.github.mybatis.model.BaseModelExample;
import com.github.mybatis.pagination.Pagination;

public interface GenericService<T extends Serializable, TE extends BaseModelExample, PK extends Serializable> {

    public Pagination<T> selectByPage(Integer begin, Integer pageSize, TE te);
}
