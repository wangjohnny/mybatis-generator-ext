package com.github.mybatis.generator.plugin;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.XmlElement;

import java.util.List;
import java.util.Properties;

public class TableFilterPlugin extends PluginAdapter {
    private String includedTables;

    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);
        includedTables = properties.getProperty("includedTables", "all");
    }

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean sqlMapSelectByExampleWithBLOBsElementGenerated(XmlElement element,
                                                                  IntrospectedTable introspectedTable) {
        return shouldGenerateTable(introspectedTable);
    }

    private boolean shouldGenerateTable(IntrospectedTable table) {
        if ("all".equals(includedTables)) {
            return true;
        }
        return includedTables.contains(table.getFullyQualifiedTableNameAtRuntime());
    }
}
