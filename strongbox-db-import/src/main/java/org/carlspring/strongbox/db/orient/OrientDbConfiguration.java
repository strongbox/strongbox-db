package org.carlspring.strongbox.db.orient;

import java.lang.reflect.Field;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.util.ReflectionUtils;
import org.strongbox.db.server.EmbeddedOrientDbServer;
import org.strongbox.db.server.OrientDbServer;
import org.strongbox.db.server.OrientDbServerConfiguration;
import org.strongbox.db.server.OrientDbServerProperties;
import org.strongbox.db.server.OrientDbStudioConfiguration;
import org.strongbox.db.server.OrientDbStudioProperties;

import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.jdbc.OrientDataSource;

/**
 * @author Przemyslaw Fusik
 */
@Configuration
class OrientDbConfiguration
{
    
    private static final Logger logger = LoggerFactory.getLogger(OrientDbConfiguration.class);
    
    @Bean
    OrientDbServer orientDbServer(OrientDbServerConfiguration serverProperties, OrientDbStudioConfiguration studioProperties) {
        return new EmbeddedOrientDbServer(studioProperties, serverProperties);
    }

    @Bean
    @ConfigurationProperties(prefix = "strongbox.orientdb.studio")
    OrientDbStudioConfiguration orientDbStudioProperties() {
        return new OrientDbStudioProperties();
    }
    
    @Bean
    @ConfigurationProperties(prefix = "strongbox.orientdb.server")
    OrientDbServerConfiguration orientDbServerProperties() {
        return new OrientDbServerProperties();
    }

    @Bean(destroyMethod = "close")
    @DependsOn("orientDbServer")
    OrientDB orientDB(OrientDbServerConfiguration orientDbServerProperties)
    {
        OrientDB orientDB = new OrientDB(StringUtils.substringBeforeLast(orientDbServerProperties.getUrl(), "/"),
                                         orientDbServerProperties.getUsername(),
                                         orientDbServerProperties.getPassword(),
                                         OrientDBConfig.defaultConfig());
        String database = orientDbServerProperties.getDatabase();

        if (!orientDB.exists(database))
        {
            logger.info(String.format("Creating database [%s]...", database));

            orientDB.create(database, ODatabaseType.PLOCAL);
        }
        else
        {
            logger.info("Re-using existing database " + database + ".");
        }
        return orientDB;
    }

    @Bean
    @LiquibaseDataSource
    DataSource dataSource(ODatabasePool pool,
                          OrientDB orientDB)
    {
        OrientDataSource ds = new OrientDataSource(orientDB);

        ds.setInfo(new Properties());

        // DEV note:
        // NPEx hotfix for OrientDataSource.java:134 :)
        Field poolField = ReflectionUtils.findField(OrientDataSource.class, "pool");
        ReflectionUtils.makeAccessible(poolField);
        ReflectionUtils.setField(poolField, ds, pool);

        return ds;
    }

    @Bean(destroyMethod = "close")
    ODatabasePool databasePool(OrientDB orientDB, OrientDbServerConfiguration orientDbServerProperties)
    {
        return new ODatabasePool(orientDB,
                                 orientDbServerProperties.getDatabase(),
                                 orientDbServerProperties.getUsername(),
                                 orientDbServerProperties.getPassword());
    }

}
