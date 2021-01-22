package org.strongbox.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.strongbox.db.server.JanusGraphServer;

/**
 * @author sbespalov
 *
 */
public class ConfigurationUtils
{

    public static void extractConfigurationFile(String rootFolder,
                                                String configFileName)
        throws IOException
    {
        try (InputStream in = JanusGraphServer.class.getResourceAsStream(String.format("/etc/conf/%s", configFileName)))
        {
            Path configRoot = Paths.get(rootFolder).resolve("etc").resolve("conf");
            Files.createDirectories(configRoot);
            Files.copy(in,
                       configRoot.resolve(configFileName),
                       StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
