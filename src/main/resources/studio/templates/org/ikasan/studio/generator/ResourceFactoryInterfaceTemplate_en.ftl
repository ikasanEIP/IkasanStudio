package ${studioPackageTag};

/**
* The Resource Factory allows the user to provide the definitions of more complex properties.
*
* The ResourceFactory interface allows the IDE to signla to the user that new properties are required without
* updating and potentially interfering with other complex properties that have already been setup.
*
*/

public interface ResourceFactory
{
<#compress>
    <#list flow.flowComponentList![] as ikasanFlowComponent>
        <#list ikasanFlowComponent.getStandardConfiguredProperties() as propName, propValue>
            <#if propValue.meta.userDefineResource>
                ${propValue.meta.usageDataType} get${StudioUtils.toJavaIdentifier(propValue.valueString)};
            </#if>
        </#list>
    </#list>
</#compress>
}