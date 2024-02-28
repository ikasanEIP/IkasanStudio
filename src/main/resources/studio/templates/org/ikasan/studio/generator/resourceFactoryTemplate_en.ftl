package ${studioPackageTag};

/**
* The Resource Factory allows the user to provide the definitions of more complex properties.
*
* Once created, the factory will not generally overwritten, new resources will be added to the interface, allowing the
* user to non-destructively accommodate new properties.
*
* Once created, this class is the users responsibility to maintain.
*
* Each new resource should be annotated with the @Resource tag e.g.
* @Resource
* public myComplexClass  getMyProperty() { return new myComplexClass(); }
*
* @TODO - verify whether jms.provider.url is part of the ikasan domain or the solution domain.
* The users is encouraged to access any properties that might be exposed from the ikasan properties e.g.
* @Value("${jms.provider.url}")
* private String brokerUrl;
*/

@org.springframework.stereotype.Component
public class ResourceFactoryImpl implements ResourceFactory
{
<#compress>
    <#list flow.ftlGetConsumerAndFlowElements()![] as flowElements>
            <#list flowElements.getStandardConfiguredProperties() as propKey, propValue>
                <#if propValue.meta.userDefineResource>
                    ${propValue.meta.usageDataType} get${StudioUtils.toJavaIdentifier(propValue.valueString)}();
                </#if>
            </#list>
    </#list>
</#compress>
}