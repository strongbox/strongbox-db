package org.strongbox.db.server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.graph.server.command.OServerCommandGetGephi;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.OServerCommandConfiguration;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerEntryConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkListenerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkProtocolConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetStaticContent;

/**
 * An embedded configuration of OrientDb server.
 *
 * @author Alex Oreshkevich
 */
public class EmbeddedOrientDbServer
        implements OrientDbServer
{

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedOrientDbServer.class);

    public static final String ORIENTDB_STUDIO_VERSION = "2.2.0";

    private OServer server;
    private OServerConfiguration serverConfiguration;

    private OrientDbStudioConfiguration studioProperties;
    private OrientDbServerConfiguration serverProperties;

    public EmbeddedOrientDbServer(OrientDbStudioConfiguration studioProperties,
                                  OrientDbServerConfiguration serverProperties)
    {
        super();
        
        this.studioProperties = studioProperties;
        this.serverProperties = serverProperties;
    }

    @PostConstruct
    public void start()
    {
        try
        {
            init();
            prepareStudio();
            activate();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to start the embedded OrientDb server!", e);
        }
    }

    public JarFile getStudioClasspathLocation()
        throws IOException
    {
        URL systemResource = OServer.class.getResource(String.format("/META-INF/resources/webjars/orientdb-studio/%s",
                                                                     ORIENTDB_STUDIO_VERSION));
        JarURLConnection connection = (JarURLConnection) systemResource.openConnection();

        return connection.getJarFile();
    }

    private void prepareStudio()
        throws IOException
    {
        if (!studioProperties.isEnabled())
        {
            logger.info("OrientDB Studio disabled with, skip initialization.");

            return;
        }

        OServerNetworkListenerConfiguration httpListener = new OServerNetworkListenerConfiguration();
        httpListener.ipAddress = studioProperties.getIpAddress();
        httpListener.portRange = String.valueOf(studioProperties.getPort());
        httpListener.protocol = "http";
        httpListener.socket = "default";

        OServerCommandConfiguration httpCommandConfiguration1 = new OServerCommandConfiguration();
        httpCommandConfiguration1.implementation = OServerCommandGetStaticContent.class.getCanonicalName();
        httpCommandConfiguration1.pattern = "GET|www GET|studio/ GET| GET|*.htm GET|*.html GET|*.xml GET|*.jpeg GET|*.jpg GET|*.png GET|*.gif GET|*.js GET|*.css GET|*.swf GET|*.ico GET|*.txt GET|*.otf GET|*.pjs GET|*.svg GET|*.json GET|*.woff GET|*.ttf GET|*.svgz";
        httpCommandConfiguration1.stateful = false;
        httpCommandConfiguration1.parameters = new OServerEntryConfiguration[] { new OServerEntryConfiguration(
                "http.cache:*.htm *.html",
                "Cache-Control: no-cache, no-store, max-age=0, must-revalidate\\r\\nPragma: no-cache"),
                                                                                 new OServerEntryConfiguration(
                                                                                         "http.cache:default",
                                                                                         "Cache-Control: max-age=120") };

        OServerCommandConfiguration httpCommandConfiguration2 = new OServerCommandConfiguration();
        httpCommandConfiguration2.implementation = OServerCommandGetGephi.class.getCanonicalName();

        httpListener.commands = new OServerCommandConfiguration[] { httpCommandConfiguration1,
                                                                    httpCommandConfiguration2 };
        httpListener.parameters = new OServerParameterConfiguration[] { new OServerParameterConfiguration("utf-8",
                "network.http.charset") };

        serverConfiguration.network.listeners.add(httpListener);

        OServerNetworkProtocolConfiguration httpProtocol = new OServerNetworkProtocolConfiguration();
        httpProtocol.name = "http";
        httpProtocol.implementation = "com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb";

        serverConfiguration.network.protocols.add(httpProtocol);

        Path studioPath = Paths.get(studioProperties.getPath()).resolve("studio");
        if (Files.exists(studioPath))
        {
            logger.info(String.format("OrientDB Studio is already available at [%s], skipping initialization. %n" +
                    "If you want to force the initialization of OrientDB Studio, please remove it's " +
                    "folder shown above.",
                                      studioPath.toAbsolutePath().toString()));
            return;
        }

        logger.info(String.format("Initialized OrientDB Studio at [%s].", studioPath.toAbsolutePath().toString()));

        Files.createDirectories(studioPath);

        String root = String.format("META-INF/resources/webjars/orientdb-studio/%s/", ORIENTDB_STUDIO_VERSION);

        try (JarFile jar = getStudioClasspathLocation())
        {
            Enumeration<JarEntry> enumEntries = jar.entries();
            while (enumEntries.hasMoreElements())
            {
                JarEntry file = enumEntries.nextElement();
                if (!file.getName().startsWith(root))
                {
                    continue;
                }

                Path filePath = studioPath.resolve(file.getName().replace(root, ""));
                if (file.isDirectory())
                {
                    Files.createDirectories(filePath);
                    continue;
                }

                try (InputStream is = jar.getInputStream(file))
                {
                    try (FileOutputStream fos = new java.io.FileOutputStream(filePath.toFile()))
                    {
                        while (is.available() > 0)
                        {
                            fos.write(is.read());
                        }
                    }
                }
            }
        }
    }

    private void init()
        throws Exception
    {
        logger.info(String.format("Initialized Embedded OrientDB server."));

        // Don't touch below line. Don't move it down the code. It needs to be
        // called before OServerMain.create()
        System.setProperty("network.binary.maxLength", "64000");

        server = OServerMain.create();
        serverConfiguration = new OServerConfiguration();

        // OServerHookConfiguration hookConfiguration = new
        // OServerHookConfiguration();
        // serverConfiguration.hooks = Arrays.asList(new
        // OServerHookConfiguration[] { hookConfiguration });
        // hookConfiguration.clazz = GenericEntityHook.class.getName();

        OServerNetworkListenerConfiguration binaryListener = new OServerNetworkListenerConfiguration();
        binaryListener.ipAddress = serverProperties.getHost();
        binaryListener.portRange = serverProperties.getPort();
        binaryListener.protocol = "binary";
        binaryListener.socket = "default";

        OServerNetworkProtocolConfiguration binaryProtocol = new OServerNetworkProtocolConfiguration();
        binaryProtocol.name = "binary";
        binaryProtocol.implementation = "com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary";

        // prepare network configuration
        OServerNetworkConfiguration networkConfiguration = new OServerNetworkConfiguration();

        networkConfiguration.protocols = new LinkedList<>();
        networkConfiguration.protocols.add(binaryProtocol);

        networkConfiguration.listeners = new LinkedList<>();
        networkConfiguration.listeners.add(binaryListener);

        // add users (incl system-level root user)
        List<OServerUserConfiguration> users = new LinkedList<>();
        users.add(buildUser(serverProperties.getUsername(), serverProperties.getPassword(), "*"));

        System.setProperty("ORIENTDB_ROOT_PASSWORD", serverProperties.getPassword());

        // add other properties
        List<OServerEntryConfiguration> properties = new LinkedList<>();
        properties.add(buildProperty("server.database.path", serverProperties.getPath()));
        properties.add(buildProperty("plugin.dynamic", "false"));
        properties.add(buildProperty("log.console.level", "info"));
        //properties.add(buildProperty("orientdb.www.path", studioProperties.getPath()));

        serverConfiguration.network = networkConfiguration;
        serverConfiguration.users = users.toArray(new OServerUserConfiguration[users.size()]);
        serverConfiguration.properties = properties.toArray(new OServerEntryConfiguration[properties.size()]);
    }

    private void activate()
        throws Exception
    {
        if (!server.isActive())
        {
            server.startup(serverConfiguration);
            server.activate();
        }
    }

    private OServerUserConfiguration buildUser(String name,
                                               String password,
                                               String resources)
    {
        OServerUserConfiguration user = new OServerUserConfiguration();
        user.name = name;
        user.password = password;
        user.resources = resources;

        return user;
    }

    private OServerEntryConfiguration buildProperty(String name,
                                                    String value)
    {
        OServerEntryConfiguration property = new OServerEntryConfiguration();
        property.name = name;
        property.value = value;

        return property;
    }

    @PreDestroy
    @Override
    public void stop()
    {
        server.shutdown();
    }

}
