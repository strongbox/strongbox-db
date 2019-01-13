package org.carlspring.strongbox.db.orient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.LinkedList;
import java.util.List;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component("orientDbServer")
public class EmbeddedOrientDbServer
{

    private OServer server;

    private OServerConfiguration serverConfiguration;

    @Autowired
    private OrientDbProperties orientDbProperties;

    @PostConstruct
    void start()
    {
        try
        {
            init();
            activate();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to start the embedded OrientDb server!", e);
        }
    }

    private void init()
            throws Exception
    {
        String database = orientDbProperties.getDatabase();

        log.info(String.format("Initialized Embedded OrientDB server for [%s]", database));

        server = OServerMain.create();
        serverConfiguration = new OServerConfiguration();

        OServerNetworkListenerConfiguration binaryListener = new OServerNetworkListenerConfiguration();
        binaryListener.ipAddress = orientDbProperties.getHost();
        binaryListener.portRange = orientDbProperties.getPort();
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
        users.add(buildUser(orientDbProperties.getUsername(), orientDbProperties.getPassword(), "*"));

        System.setProperty("ORIENTDB_ROOT_PASSWORD", orientDbProperties.getUsername());

        // add other properties
        List<OServerEntryConfiguration> properties = new LinkedList<>();
        properties.add(buildProperty("server.database.path", orientDbProperties.getPath()));
        properties.add(buildProperty("plugin.dynamic", "false"));
        properties.add(buildProperty("log.console.level", "info"));
        properties.add(buildProperty(OGlobalConfiguration.NETWORK_BINARY_MAX_CONTENT_LENGTH.getKey(), "64000"));

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
    void stop()
    {
        server.shutdown();
    }

}
