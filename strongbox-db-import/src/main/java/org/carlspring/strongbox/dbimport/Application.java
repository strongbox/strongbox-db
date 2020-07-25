package org.carlspring.strongbox.dbimport;

import org.janusgraph.core.JanusGraph;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Przemyslaw Fusik
 */
@SpringBootApplication(exclude = ValidationAutoConfiguration.class)
public class Application
{

    public static void main(String[] args)
    {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
        ctx.getBean(JanusGraph.class);
        
        SpringApplication.exit(ctx, () -> 0);
    }
}
