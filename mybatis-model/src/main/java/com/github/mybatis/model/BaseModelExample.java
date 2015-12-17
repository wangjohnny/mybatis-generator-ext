package com.github.mybatis.model;

import com.github.mybatis.pagination.Pagination;

/**
 * 所有Example的基类，包括分页属性
 * 
 * @author Johnny
 *
 */
public class BaseModelExample {
	protected Pagination<?> pagination;

	protected String orderByClause;

    protected boolean distinct;

	public void setPagination(Pagination<?> pagination) {
		this.pagination = pagination;
	}

	public Pagination<?> getPagination() {
		return pagination;
	}

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }
    
    public void clear() {
        orderByClause = null;
        distinct = false;
    }

}
