package org.carlspring.strongbox.db.schema.util;

import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.janusgraph.core.EdgeLabel;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.VertexLabel;
import org.janusgraph.core.schema.ConsistencyModifier;
import org.janusgraph.core.schema.JanusGraphIndex;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.JanusGraphSchemaType;
import org.janusgraph.core.schema.JanusGraphManagement.IndexBuilder;

public class SchemaUtils
{

    public static void addVertexPropertyConstraints(JanusGraphManagement jgm,
                                                    String vertex,
                                                    String... propertykeys)
    {
        VertexLabel vertexLabel = jgm.getVertexLabel(vertex);
        for (String propertyKey : propertykeys)
        {
            jgm.addProperties(vertexLabel, jgm.getPropertyKey(propertyKey));
        }
    }

    public static void addEdgePropertyConstraints(JanusGraphManagement jgm,
                                                  String label,
                                                  String... propertykeys)
    {
        EdgeLabel edge = jgm.getEdgeLabel(label);
        for (String propertyKey : propertykeys)
        {
            jgm.addProperties(edge, jgm.getPropertyKey(propertyKey));
        }
    }

    public static String buildIndex(final JanusGraphManagement jgm,
                                    final Class<? extends Element> elementType,
                                    final JanusGraphSchemaType schemaType,
                                    final boolean unique,
                                    final PropertyKey... properties)
    {
        return buildIndex(jgm, elementType, schemaType, unique, true, properties);
    }

    public static String buildIndex(final JanusGraphManagement jgm,
                                    final Class<? extends Element> elementType,
                                    final JanusGraphSchemaType schemaType,
                                    final boolean unique,
                                    final boolean consistent,
                                    final PropertyKey... properties)
    {
        final String name = schemaType.name() + "By" + Arrays.stream(properties)
                                                             .map(PropertyKey::name)
                                                             .map(StringUtils::capitalize)
                                                             .reduce((p1,
                                                                      p2) -> p1 + "And" + p2)
                                                             .get();

        IndexBuilder indexBuilder = jgm.buildIndex(name, elementType)
                                       .indexOnly(schemaType);
        Arrays.stream(properties).forEach(indexBuilder::addKey);

        if (unique)
        {
            indexBuilder = indexBuilder.unique();
        }
        JanusGraphIndex intex = indexBuilder.buildCompositeIndex();
        if (consistent)
        {
            jgm.setConsistency(intex, ConsistencyModifier.LOCK);
        }

        return intex.name();
    }
}
