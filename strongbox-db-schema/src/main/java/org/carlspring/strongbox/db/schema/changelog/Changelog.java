package org.carlspring.strongbox.db.schema.changelog;

import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.carlspring.strongbox.db.schema.migration.Changeset;

public interface Changelog
{

    SortedSet<Changeset> changeSets = Collections.unmodifiableSortedSet(new TreeSet<>(
            Arrays.asList(new Changeset[] { new V1_0_0_1_InitialSchema(),
                                            new V1_0_0_2_InitialIndexes()
            })));

}
