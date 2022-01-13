/**
 *    Copyright 2006-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package me.wbean.mybatis.plugin;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.VisitableElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 特殊规则
 * @author mushan
 * 2019-05-29 17:30
 */
public class DeletedAtPlugin extends PluginAdapter {
    private static final String KEY = "markDeleteKey";
    private static final String DEFAULT = "deletedAt";


    private static final Pattern timePattern = Pattern.compile("\\bgmt_create(,?)|\\bgmt_modified(,?)|#\\{gmtCreate,jdbcType=TIMESTAMP\\}(,?)|#\\{gmtModified,jdbcType=TIMESTAMP\\}(,?)");


    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    // 已存在Mapper接口, 则不生成新的进行覆盖
    @Override
    public boolean clientGenerated(Interface interfaze, IntrospectedTable introspectedTable) {
        try{
            Class.forName(interfaze.getType().getFullyQualifiedName());
        }catch (Exception e){
            return true;
        }
        return false;
    }

    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        topLevelClass.addImportedType("lombok.Data");
        topLevelClass.addAnnotation("@Data");
        return true;
    }

    public boolean modelGetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        return false;
    }

    public boolean modelSetterMethodGenerated(Method method, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        return false;
    }

//    // 查询条件添加is_delete is NULL
//    @Override
//    public boolean sqlMapExampleWhereClauseElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
//        String column = this.getKey(introspectedTable);
//        for (VisitableElement child : element.getElements()) {
//            if (child instanceof XmlElement && ((XmlElement) child).getName().equals("where")) {
//                TextElement element1 = new TextElement("and "+column+" is NULL");
//                ((XmlElement) child).getElements().add(element1);
//                break;
//            }
//        }
//        return true;
//    }
//
//    // 查询条件添加is_delete is NULL
//    @Override
//    public boolean sqlMapSelectByPrimaryKeyElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
//        String column = this.getKey(introspectedTable);
//        TextElement element1 = new TextElement("and "+column+" is NULL");
//        element.getElements().add(element1);
//        return true;
//    }
//
//    // 标记删除
//    @Override
//    public boolean sqlMapDeleteByPrimaryKeyElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
//        String column = this.getKey(introspectedTable);
//        element.setName("update");
//        List<VisitableElement> elements = element.getElements();
//        for (int i = 0; i < elements.size(); i++) {
//            if (elements.get(i) instanceof TextElement) {
//                TextElement e = (TextElement) elements.get(i);
//                if (e.getContent().startsWith("delete from")) {
//                    elements.set(i, new TextElement("update " + introspectedTable.getTableConfiguration().getTableName() + " set " + column + " = now()"));
//                }
//            }
//        }
//        return true;
//    }

    // 去掉insert方法中的created_at, updated_at字段, 采用数据库生成
    public boolean sqlMapInsertElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        List<VisitableElement> elements = element.getElements();
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) instanceof TextElement) {
                TextElement e = (TextElement) elements.get(i);
                Matcher matcher = timePattern.matcher(e.getContent());
                if(matcher.find()){
                    String newContent = matcher.replaceAll("");
                    newContent = newContent.replaceAll(",\\s*\\)", ")");
                    elements.set(i, new TextElement(newContent));
                }
            }
        }
        return true;
    }
    // 去掉insertSelective中的created_at updated_at
    public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        List<VisitableElement> elements = element.getElements();
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) instanceof XmlElement) {
                XmlElement e = (XmlElement) elements.get(i);
                deepSearchAndRemove(e);
            }
        }
        return true;
    }

    public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        List<VisitableElement> elements = element.getElements();
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) instanceof XmlElement) {
                XmlElement e = (XmlElement) elements.get(i);
                deepSearchAndRemove(e);
            }
        }
        return true;
    }

//    // 去掉updateByPrimaryKey的实现
//    public boolean sqlMapUpdateByPrimaryKeyWithBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
//        return false;
//    }
//
//    // 去掉updateByPrimaryKey的实现
//    public boolean sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
//        return false;
//    }

    private String getKey(IntrospectedTable introspectedTable){
        String markDeleteKey = introspectedTable.getTableConfigurationProperty(KEY);
        if(markDeleteKey != null && markDeleteKey.length() > 0){
            return markDeleteKey;
        }
        markDeleteKey = this.properties.getProperty(KEY);
        if(markDeleteKey != null && markDeleteKey.length() > 0){
            return markDeleteKey;
        }
        return DEFAULT;
    }

    private void deepSearchAndRemove(XmlElement xmlElement){
        Iterator<VisitableElement> iterator = xmlElement.getElements().iterator();
        while (iterator.hasNext()){
            VisitableElement next = iterator.next();

            if(next instanceof XmlElement){
                List<Attribute> attributes = ((XmlElement) next).getAttributes();
                if(attributes.size() > 0){
                    String value = attributes.get(0).getValue();
                    if(value != null && (value.startsWith("gmtCreate") || value.startsWith("gmtModified"))){
                        iterator.remove();
                    }else {
                        deepSearchAndRemove((XmlElement) next);
                    }
                }
            }
        }
    }
}
