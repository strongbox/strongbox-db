package org.carlspring.strongbox.db.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.TreeSet;

import org.carlspring.strongbox.db.schema.migration.Changeset;
import org.carlspring.strongbox.db.schema.migration.ChangesetVersionValue;
import org.carlspring.strongbox.db.schema.migration.JanusgraphChangelogStorage;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.strongbox.db.server.InMemoryJanusGraphServer;
import org.strongbox.db.server.JanusGraphConfiguration;
import org.strongbox.db.server.JanusGraphProperties;
import org.strongbox.db.server.JanusGraphServer;
import org.strongbox.util.ConfigurationUtils;

public class SchemaMigrationTest
{

    private static final Changeset V1 = new Changeset()
    {

        @Override
        public String getVersion()
        {
            return "1.0.0.1";
        }

        @Override
        public String getAuthor()
        {
            return "sbespalov";
        }

    };

    private static final Changeset V2 = new Changeset()
    {

        @Override
        public String getVersion()
        {
            return "1.0.0.2";
        }

        @Override
        public String getAuthor()
        {
            return "sbespalov";
        }

    };

    private static final Changeset V3 = new Changeset()
    {

        @Override
        public String getVersion()
        {
            return "1.0.0.3";
        }

        @Override
        public void applySchemaChanges(JanusGraphManagement jgm)
        {
            throw new RuntimeException(getVersion());
        }

        @Override
        public String getAuthor()
        {
            return "sbespalov";
        }

    };

    private JanusGraphServer janusGraphServer;

    @BeforeAll
    public static void init()
        throws IOException
    {
        ConfigurationUtils.extractConfigurationFile("./target", "janusgraph-inmemory.properties");
    }

    @BeforeEach
    public void setUp()
        throws Exception
    {
        JanusGraphConfiguration janusGraphConfiguration = new JanusGraphProperties(
                "file:./target/etc/conf/janusgraph-inmemory.properties");
        janusGraphServer = new InMemoryJanusGraphServer(janusGraphConfiguration, () -> null);
        janusGraphServer.start();
    }

    @Test
    public void testUpdateSchemaVersion()
        throws InterruptedException
    {
        JanusGraph jg = janusGraphServer.getJanusGraph();
        JanusgraphChangelogStorage changelogStorage = new JanusgraphChangelogStorage(jg);
        TreeSet<Changeset> changeSets = new TreeSet<>();
        StrongboxSchema strongboxSchema = new StrongboxSchema(changeSets);

        strongboxSchema.createSchema(jg);
        assertThat(changelogStorage.getSchemaVersion().getVersion()).isEqualTo(ChangesetVersionValue.ZERO.getVersion());

        changeSets.add(V2);
        strongboxSchema.createSchema(jg);
        assertThat(changelogStorage.getSchemaVersion().getVersion()).isEqualTo(V2.getVersion());

        changeSets.add(V3);
        assertThatThrownBy(() -> strongboxSchema.createSchema(jg)).withFailMessage("Failed to apply changeset [1.0.0.3]-[]");
        assertThat(changelogStorage.getSchemaVersion().getVersion()).isEqualTo(V2.getVersion());
    }

    @AfterEach
    public void tearDwon()
        throws Exception
    {
        janusGraphServer.stop();
    }

}
