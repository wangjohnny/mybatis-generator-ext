package com.github.mybatis.model;

import java.io.Serializable;

/**
 * 所有Model的基类，包括分页属性
 * 
 * @author Johnny
 *
 */
public class BaseModel<PK extends Serializable> implements Serializable {

    private static final long serialVersionUID = -6590882888801386323L;

    protected PK id;

	public PK getId() {
		return id;
	}

	public void setId(PK id) {
		this.id = id;
	}

}
