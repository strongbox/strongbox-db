package org.carlspring.strongbox.db.schema.migration;

/**
 * @author sbespalov
 */
public interface ChangesetVersion extends Comparable<ChangesetVersion>
{

    String getVersion();

    @Override
    default int compareTo(ChangesetVersion other)
    {
        String version1 = this.getVersion();
        String version2 = other.getVersion();

        String[] levels1 = version1.split("\\.");
        String[] levels2 = version2.split("\\.");

        int length = Math.max(levels1.length, levels2.length);
        for (int i = 0; i < length; i++)
        {
            Integer v1 = i < levels1.length ? Integer.parseInt(levels1[i]) : 0;
            Integer v2 = i < levels2.length ? Integer.parseInt(levels2[i]) : 0;
            int compare = v1.compareTo(v2);
            if (compare != 0)
            {
                return compare;
            }
        }

        return 0;
    }

}
