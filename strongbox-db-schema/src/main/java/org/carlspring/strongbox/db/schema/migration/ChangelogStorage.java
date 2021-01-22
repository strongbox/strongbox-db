package org.carlspring.strongbox.db.schema.migration;

/**
 * @author sbespalov
 */
public interface ChangelogStorage
{

    String EDGE_CHANGESET = "Changeset";
    String VERTEX_SCHEMA_VERSION = "SchemaVersion";
    String PROPERTY_VERSION_VALUE = "versionValue";
    String PROPERTY_APPLY_DATE = "applyDate";
    String PROPERTY_CHANGESET_NAME = "changesetName";
    String PROPERTY_AUTHOR = "changesetAuthor";

    void init();

    ChangesetVersion getSchemaVersion();

    void upgrade(Changeset version);
    
}
