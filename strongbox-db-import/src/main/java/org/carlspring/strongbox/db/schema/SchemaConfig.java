package org.carlspring.strongbox.db.schema;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchemaConfig
{

    @Bean
    StrongboxSchema strongboxSchema()
    {
        return new StrongboxSchema();
    }

}
