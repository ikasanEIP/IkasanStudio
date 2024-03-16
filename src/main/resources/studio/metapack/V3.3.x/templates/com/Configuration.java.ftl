package com;

/**
* NOTE: Generated code !! DO NOT EDIT !!
* Reference: http://freemarker.org
* See template: ${pp.sourceFile}
*/
public class Configuration implements Cloneable {
<#list properties as property>
    private final ${property.type} ${property.name} = 0;
</#list>
}