<#assign StudioBuildUtils=statics['org.ikasan.studio.core.StudioBuildUtils']>
package ${studioPackageTag};

/**
 * Developer-owned scheduled payload provider. Replace the timer signal with your business payload.
 * Return null only when this invocation has no work. Studio preserves edits on regeneration.
 */
@org.springframework.stereotype.Component("${StudioBuildUtils.toJavaIdentifier(className)}")
public class ${className} implements org.ikasan.component.endpoint.quartz.consumer.MessageProvider<Object> {
    @Override
    public Object invoke(org.quartz.JobExecutionContext context) {
        return context;
    }
}
