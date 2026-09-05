package ${studioPackageTag};

/**
* The Resource Factory allows the user to provide the definitions of more complex properties.
*
* The ResourceFactory interface allows the IDE to signal to the user that new properties are required without
* updating and potentially interfering with other complex properties that have already been setup.
*/

public interface ResourceFactory
{
<#compress>
    <#list flow.getFlowRoute().ftlGetConsumerAndFlowElements()![] as flowElements>
        <#list flowElements.getStandardComponentProperties() as propKey, propValue>
            <#if propValue.meta.userDefineResource>
                ${propValue.meta.usageDataType} get${StudioBuildUtils.toJavaIdentifier(propValue.valueString)};
            </#if>
        </#list>
    </#list>
</#compress>
}