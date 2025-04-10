package com.github.mybatis.generator.plugin;

import static org.mybatis.generator.internal.util.JavaBeansUtil.getGetterMethodName;

import java.util.Iterator;
import java.util.List;

import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.dom.OutputUtilities;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * 参考MBG的内置EqualsHashCodePlugin改造的类，可以生成只用ID来生成equals与hashCode的方式
 * 
 * @author Johnny
 *
 */
public class EqualsHashCodePlugin extends PluginAdapter {

    public EqualsHashCodePlugin() {
    }

    /**
     * This plugin is always valid - no properties are required
     */
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass,
            IntrospectedTable introspectedTable) {
        List<IntrospectedColumn> columns;
        if (introspectedTable.getRules().generateRecordWithBLOBsClass()) {
            columns = introspectedTable.getNonBLOBColumns();
        } else {
            columns = introspectedTable.getPrimaryKeyColumns();
        }

        generateEquals(topLevelClass, columns, introspectedTable);
        generateHashCode(topLevelClass, columns, introspectedTable);

        return true;
    }

    /**
     * Generates an <tt>equals</tt> method that does a comparison of all fields.
     * <p>
     * The generated <tt>equals</tt> method will be correct unless:
     * <ul>
     * <li>Other fields have been added to the generated classes</li>
     * <li>A <tt>rootClass</tt> is specified that holds state</li>
     * </ul>
     * 
     * @param topLevelClass
     *            the class to which the method will be added
     * @param introspectedColumns
     *            column definitions of this class and any superclass of this
     *            class
     * @param introspectedTable
     *            the table corresponding to this class
     */
    protected void generateEquals(TopLevelClass topLevelClass,
            List<IntrospectedColumn> introspectedColumns,
            IntrospectedTable introspectedTable) {
        Method method = new Method("equals");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(FullyQualifiedJavaType
                .getBooleanPrimitiveInstance());
        method.addParameter(new Parameter(FullyQualifiedJavaType
                .getObjectInstance(), "that")); //$NON-NLS-1$
        method.addAnnotation("@Override"); //$NON-NLS-1$
        
        context.getCommentGenerator().addGeneralMethodComment(method,
                introspectedTable);

        method.addBodyLine("if (this == that) {"); //$NON-NLS-1$
        method.addBodyLine("return true;"); //$NON-NLS-1$
        method.addBodyLine("}"); //$NON-NLS-1$

        method.addBodyLine("if (that == null) {"); //$NON-NLS-1$
        method.addBodyLine("return false;"); //$NON-NLS-1$
        method.addBodyLine("}"); //$NON-NLS-1$

        method.addBodyLine("if (getClass() != that.getClass()) {"); //$NON-NLS-1$
        method.addBodyLine("return false;"); //$NON-NLS-1$
        method.addBodyLine("}"); //$NON-NLS-1$

        StringBuilder sb = new StringBuilder();
        sb.append(topLevelClass.getType().getShortName());
        sb.append(" other = ("); //$NON-NLS-1$
        sb.append(topLevelClass.getType().getShortName());
        sb.append(") that;"); //$NON-NLS-1$
        method.addBodyLine(sb.toString());

        boolean first = true;
        Iterator<IntrospectedColumn> iter = introspectedColumns.iterator();
        while (iter.hasNext()) {
            IntrospectedColumn introspectedColumn = iter.next();

            sb.setLength(0);

            if (first) {
                sb.append("return ("); //$NON-NLS-1$
                first = false;
            } else {
                OutputUtilities.javaIndent(sb, 1);
                sb.append("&& ("); //$NON-NLS-1$
            }

            String getterMethod = getGetterMethodName(
                    introspectedColumn.getJavaProperty(), introspectedColumn
                            .getFullyQualifiedJavaType());

            if (introspectedColumn.getFullyQualifiedJavaType().isPrimitive()) {
                sb.append("this."); //$NON-NLS-1$
                sb.append(getterMethod);
                sb.append("() == "); //$NON-NLS-1$
                sb.append("other."); //$NON-NLS-1$
                sb.append(getterMethod);
                sb.append("())"); //$NON-NLS-1$
            } else {
                sb.append("this."); //$NON-NLS-1$
                sb.append(getterMethod);
                sb.append("() == null ? other."); //$NON-NLS-1$
                sb.append(getterMethod);
                sb.append("() == null : this."); //$NON-NLS-1$
                sb.append(getterMethod);
                sb.append("().equals(other."); //$NON-NLS-1$
                sb.append(getterMethod);
                sb.append("()))"); //$NON-NLS-1$
            }

            if (!iter.hasNext()) {
                sb.append(';');
            }

            method.addBodyLine(sb.toString());
        }

        topLevelClass.addMethod(method);
    }

    /**
     * Generates a <tt>hashCode</tt> method that includes all fields.
     * <p>
     * Note that this implementation is based on the eclipse foundation hashCode
     * generator.
     * 
     * @param topLevelClass
     *            the class to which the method will be added
     * @param introspectedColumns
     *            column definitions of this class and any superclass of this
     *            class
     * @param introspectedTable
     *            the table corresponding to this class
     */
    protected void generateHashCode(TopLevelClass topLevelClass,
            List<IntrospectedColumn> introspectedColumns,
            IntrospectedTable introspectedTable) {
        Method method = new Method("hashCode");
        method.setVisibility(JavaVisibility.PUBLIC);
        method.setReturnType(FullyQualifiedJavaType.getIntInstance());
        method.addAnnotation("@Override"); //$NON-NLS-1$
        context.getCommentGenerator().addGeneralMethodComment(method,
                introspectedTable);

        method.addBodyLine("final int prime = 31;"); //$NON-NLS-1$
        method.addBodyLine("int result = 1;"); //$NON-NLS-1$

        StringBuilder sb = new StringBuilder();
        boolean hasTemp = false;
        Iterator<IntrospectedColumn> iter = introspectedColumns.iterator();
        while (iter.hasNext()) {
            IntrospectedColumn introspectedColumn = iter.next();

            FullyQualifiedJavaType fqjt = introspectedColumn
                    .getFullyQualifiedJavaType();

            String getterMethod = getGetterMethodName(
                    introspectedColumn.getJavaProperty(), fqjt);

            sb.setLength(0);
            if (fqjt.isPrimitive()) {
                if ("boolean".equals(fqjt.getFullyQualifiedName())) { //$NON-NLS-1$
                    sb.append("result = prime * result + ("); //$NON-NLS-1$
                    sb.append(getterMethod);
                    sb.append("() ? 1231 : 1237);"); //$NON-NLS-1$
                    method.addBodyLine(sb.toString());
                } else if ("byte".equals(fqjt.getFullyQualifiedName())) { //$NON-NLS-1$
                    sb.append("result = prime * result + "); //$NON-NLS-1$
                    sb.append(getterMethod);
                    sb.append("();"); //$NON-NLS-1$
                    method.addBodyLine(sb.toString());
                } else if ("char".equals(fqjt.getFullyQualifiedName())) { //$NON-NLS-1$
                    sb.append("result = prime * result + "); //$NON-NLS-1$
                    sb.append(getterMethod);
                    sb.append("();"); //$NON-NLS-1$
                    method.addBodyLine(sb.toString());
                } else if ("double".equals(fqjt.getFullyQualifiedName())) { //$NON-NLS-1$
                    if (!hasTemp) {
                        method.addBodyLine("long temp;"); //$NON-NLS-1$
                        hasTemp = true;
                    }
                    sb.append("temp = Double.doubleToLongBits("); //$NON-NLS-1$
                    sb.append(getterMethod);
                    sb.append("());"); //$NON-NLS-1$
                    method.addBodyLine(sb.toString());
                    method
                            .addBodyLine("result = prime * result + (int) (temp ^ (temp >>> 32));"); //$NON-NLS-1$
                } else if ("float".equals(fqjt.getFullyQualifiedName())) { //$NON-NLS-1$
                    sb
                            .append("result = prime * result + Float.floatToIntBits("); //$NON-NLS-1$
                    sb.append(getterMethod);
                    sb.append("());"); //$NON-NLS-1$
                    method.addBodyLine(sb.toString());
                } else if ("int".equals(fqjt.getFullyQualifiedName())) { //$NON-NLS-1$
                    sb.append("result = prime * result + "); //$NON-NLS-1$
                    sb.append(getterMethod);
                    sb.append("();"); //$NON-NLS-1$
                    method.addBodyLine(sb.toString());
                } else if ("long".equals(fqjt.getFullyQualifiedName())) { //$NON-NLS-1$
                    sb.append("result = prime * result + (int) ("); //$NON-NLS-1$
                    sb.append(getterMethod);
                    sb.append("() ^ ("); //$NON-NLS-1$
                    sb.append(getterMethod);
                    sb.append("() >>> 32));"); //$NON-NLS-1$
                    method.addBodyLine(sb.toString());
                } else if ("short".equals(fqjt.getFullyQualifiedName())) { //$NON-NLS-1$
                    sb.append("result = prime * result + "); //$NON-NLS-1$
                    sb.append(getterMethod);
                    sb.append("();"); //$NON-NLS-1$
                    method.addBodyLine(sb.toString());
                } else {
                    // should never happen
                    continue;
                }
            } else {
                sb.append("result = prime * result + (("); //$NON-NLS-1$
                sb.append(getterMethod);
                sb.append("() == null) ? 0 : "); //$NON-NLS-1$
                sb.append(getterMethod);
                sb.append("().hashCode());"); //$NON-NLS-1$
                method.addBodyLine(sb.toString());
            }
        }

        method.addBodyLine("return result;"); //$NON-NLS-1$

        topLevelClass.addMethod(method);
    }
}
