package org.ikasan.studio.generator;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class BasicPropertiesTemplate extends Generator {
    private static String MODULE_PROPERTIES = "application";
    private static String MODULE_PROPERTIES_IMPL =
            "# Logging levels across packages (optional)\n" +
                    "logging.level.com.arjuna=INFO\n" +
                    "logging.level.org.springframework=INFO\n" +
                    "\n" +
                    "# Blue console servlet settings (optional)\n" +
                    "server.error.whitelabel.enabled=false\n" +
                    "\n" +
                    "# Web Bindings\n" +
                    "server.port=8090\n" +
                    "server.address=localhost\n" +
                    "server.servlet.context-path=/example-im\n" +
                    "server.tomcat.additional-tld-skip-patterns=xercesImpl.jar,xml-apis.jar,serializer.jar\n" +
                    "\n" +
                    "# Spring config\n" +
                    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration\n" +
                    "spring.liquibase.change-log=classpath:db-changelog.xml\n" +
                    "spring.liquibase.enabled=true\n" +
                    "\n" +
                    "# health probs and remote management (optional)\n" +
                    "management.endpoints.web.expose=*\n" +
                    "management.server.servlet.context-path=/manage\n" +
                    "management.endpoint.shutdown.enabled=true\n" +
                    "\n" +
                    "# Ikasan persistence store\n" +
                    "datasource.username=sa\n" +
                    "datasource.password=sa\n" +
                    "datasource.driver-class-name=org.h2.Driver\n" +
                    "datasource.xadriver-class-name=org.h2.jdbcx.JdbcDataSource\n" +
                    "datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1\n" +
                    "datasource.dialect=org.hibernate.dialect.H2Dialect\n" +
                    "datasource.show-sql=false\n" +
                    "datasource.hbm2ddl.auto=none\n" +
                    "datasource.validationQuery=select 1";
    public static void createBasicPropertes(final AnActionEvent ae, final Project project) {
        createResourceFile(ae, project, null, MODULE_PROPERTIES, MODULE_PROPERTIES_IMPL, false);
    }
}
