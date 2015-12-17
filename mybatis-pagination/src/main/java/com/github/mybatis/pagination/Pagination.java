package com.github.mybatis.pagination;

import java.util.List;

/**
 * 
 * @author Johnny
 *
 * @param <T>
 */
public class Pagination<T> {

	public static final Integer DEFAULT_PAGE_SIZE = 10;

	/**
	 * 每页显示记录数
	 */
	private Integer pageSize;

	/**
	 * 查询结果总记录数
	 */
	private Integer totalRowNumber;

	/**
	 * 起始行号,默认第0行开始
	 */
	private Integer begin;

	private List<T> dataList;

	public Pagination(Integer begin) {
		this(begin, DEFAULT_PAGE_SIZE);
	}

	public Pagination(Integer begin, Integer pageSize) {
		this.begin = begin;
		this.pageSize = pageSize;
	}

	public void setTotalRowNumber(Integer totalRowNumber) {
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

	public Integer getTotalRowNumber() {
		return this.totalRowNumber;
	}

	public List<T> getDataList() {
		return dataList;
	}

}