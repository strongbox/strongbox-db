package org.carlspring.strongbox.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Przemyslaw Fusik
 */
@Slf4j
@SpringBootApplication
public class Application
{

    public static void main(String[] args)
    {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
        SpringApplication.exit(ctx, () -> 0);
    }
}
