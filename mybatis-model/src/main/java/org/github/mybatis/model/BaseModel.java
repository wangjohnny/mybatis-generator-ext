package org.github.mybatis.model;

import java.io.Serializable;

/**
 * 所有Example的基类，包括分页属性
 * 
 * @author Johnny
 *
 */
public class BaseModel implements Serializable {

    private static final long serialVersionUID = -6590882888801386323L;

    protected String sid;

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }
	
}
