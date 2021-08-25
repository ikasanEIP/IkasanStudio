package com.ikasan.sample.spring.boot;

import org.ikasan.builder.BuilderFactory;
import org.ikasan.builder.ModuleBuilder;
import org.ikasan.flow.visitorPattern.invoker.FilterInvokerConfiguration;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.module.Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

class MyFilter implements Filter<List<Person>>, ConfiguredResource<MyFilterConfiguration>
{
    private final Logger logger = LoggerFactory.getLogger(MyFilter.class);

    String configuredResourceId;
    MyFilterConfiguration configuration;

    @Override
    public String getConfiguredResourceId() {
        return configuredResourceId;
    }

    @Override
    public void setConfiguredResourceId(String configuredResourceId) {
        this.configuredResourceId = configuredResourceId;
    }

    @Override
    public MyFilterConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(MyFilterConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<Person> filter(List<Person> people) throws FilterException
    {
        List<Person> peopleOldEnough = new ArrayList<Person>();

        for(Person person:people)
        {
            try
            {
                if(configuration.ageRestriction < person.getAge())
                {
                    peopleOldEnough.add(person);
                }
            }
            catch(Exception e)
            {
                logger.info("Unable to calculate age for " + person.getName(), e);
            }
        }

        if(peopleOldEnough.size() > 0)
        {
            return peopleOldEnough;
        }

        return null;
    }
}