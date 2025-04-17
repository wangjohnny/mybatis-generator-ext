package com.github.mybatis.pagination;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * @author Johnny
 *
 * @param <T>
 */
public class Pagination<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Integer DEFAULT_PAGE_SIZE = 10;

	/**
	 * 每页显示记录数
	 */
	private Integer pageSize;

	/**
	 * 查询结果总记录数
	 */
	private Long totalRowNumber;

	/**
	 * 起始行号,默认第0行开始
	 */
	private Integer begin;

	private List<T> dataList;

    public Pagination() {
        this(0);//默认第0行开始
    }

	public Pagination(Integer begin) {
		this(begin, DEFAULT_PAGE_SIZE);
	}

	public Pagination(Integer begin, Integer pageSize) {
		this.begin = begin;
		this.pageSize = pageSize;
	}

	public void setTotalRowNumber(Long totalRowNumber) {
		this.totalRowNumber = (totalRowNumber == null ? 0 : totalRowNumber);
	}

	public void setDataList(List<T> dataList) {
		this.dataList = dataList;
	}

	public Integer getBegin() {
		return begin;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public Long getTotalRowNumber() {
		return this.totalRowNumber;
	}

	public List<T> getDataList() {
		return dataList;
	}

}