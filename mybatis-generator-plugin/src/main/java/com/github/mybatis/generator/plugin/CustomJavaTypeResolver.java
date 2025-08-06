package com.github.mybatis.generator.plugin;

import java.math.BigDecimal;
import java.sql.Types;

import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.internal.types.JavaTypeResolverDefaultImpl;

public class CustomJavaTypeResolver extends JavaTypeResolverDefaultImpl {
    public CustomJavaTypeResolver() {
        super();
        // 将 DECIMAL 和 NUMERIC 默认映射为 BigDecimal
        typeMap.put(Types.DECIMAL,
                new JdbcTypeInformation("DECIMAL", new FullyQualifiedJavaType(BigDecimal.class.getName())));
        typeMap.put(Types.NUMERIC,
                new JdbcTypeInformation("NUMERIC", new FullyQualifiedJavaType(BigDecimal.class.getName())));
    }
}