package org.carlspring.strongbox.db.schema.migration;

/**
 * @author sbespalov
 */
public class ChangesetVersionValue implements ChangesetVersion
{

    public static ChangesetVersion ZERO = new ChangesetVersionValue("0");

    private final String version;

    public ChangesetVersionValue(String version)
    {
        this.version = version;
    }

    @Override
    public String getVersion()
    {
        return version;
    }

}
