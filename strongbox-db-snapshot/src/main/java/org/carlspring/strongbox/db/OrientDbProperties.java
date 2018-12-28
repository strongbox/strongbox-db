package org.carlspring.strongbox.db;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "strongbox.orientdb")
public class OrientDbProperties
{

    private String url;

    private String username;

    private String password;

    private String protocol;

    private String host;

    private String port;

    private String database;

    private String path;

    public String getUrl()
    {
        if ("memory".equals(protocol))
        {
            return String.format("%s:%s", protocol, database);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(protocol);
        if (host != null)
        {
            sb.append(":").append(host);
        }
        if (port != null)
        {
            sb.append(":").append(port);
        }
        sb.append("/").append(database);

        return sb.toString();
    }
}
