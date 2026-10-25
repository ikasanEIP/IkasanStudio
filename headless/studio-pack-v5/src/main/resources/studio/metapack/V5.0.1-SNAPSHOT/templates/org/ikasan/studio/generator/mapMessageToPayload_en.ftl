<#-- Settings belong to the converter configuration bean, not fluent converter setters. -->
<#list ["contentAttributeName", "idAttributeName", "fileNameAttributeName"] as name>
    <#assign property=flowElement.getProperty(name)>
    component.getConfiguration().set${StudioBuildUtils.toPascalCase(name)}(<#if property.valueNotSet()>"${property.meta.defaultValue?j_string}"<#else>${StudioBuildUtils.substitutePlaceholderInJavaCamelCase(module, flow, flowElement, property.meta.propertyConfigFileLabel)}</#if>);
</#list>
    component.setConfiguredResourceId("${StudioBuildUtils.substitutePlaceholderInJavaCamelCase(module, flow, flowElement, (flowElement.getPropertyValue("configurationId")!"__module-__flow-__component"))?j_string}");
