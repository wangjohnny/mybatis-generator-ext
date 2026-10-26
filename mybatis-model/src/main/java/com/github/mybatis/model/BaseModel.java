package com.github.mybatis.model;

import java.io.Serializable;

/**
 * 所有Model的基类，包括分页属性
 * 
 * @author oking
 *
 */
public class BaseModel<PK extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;

    protected PK id;

	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

}
