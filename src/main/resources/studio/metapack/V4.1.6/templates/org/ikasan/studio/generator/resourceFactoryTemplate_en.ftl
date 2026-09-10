package ${studioPackageTag};

/**
* The Resource Factory allows the user to provide the definitions of more complex properties.
*
* Once created, the factory will not generally overwritten, new resources will be added to the interface, allowing the
* user to non-destructively accommodate new properties.
*
* Once created, this class is the users responsibility to maintain.
*
* Provide resource beans using the appropriate Spring annotations, for example {@code @Bean}.
* Inject configuration with Spring's {@code @Value} annotation on a field or constructor parameter.
* Use an application property placeholder for the broker URL rather than embedding it in code.
*/

@org.springframework.stereotype.Component("${studioPackageTag}.ResourceFactoryImpl")
public class ResourceFactoryImpl implements ResourceFactory
{
<#compress>
    <#list flow.getFlowRoute().ftlGetConsumerAndFlowElements()![] as flowElements>
            <#list flowElements.getStandardComponentProperties() as propKey, propValue>
                <#if propValue.meta.userDefineResource>
                    ${propValue.meta.usageDataType} get${StudioBuildUtils.toJavaIdentifier(propValue.valueString)}();
                </#if>
            </#list>
    </#list>
</#compress>
}