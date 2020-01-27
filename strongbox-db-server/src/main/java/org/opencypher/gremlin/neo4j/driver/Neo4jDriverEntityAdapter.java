package org.opencypher.gremlin.neo4j.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.internal.value.ListValue;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Entity;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
import org.neo4j.ogm.driver.TypeSystem.NoNativeTypes;

/**
 * @author sbespalov
 */
public class Neo4jDriverEntityAdapter
{

    public boolean isPath(Object value)
    {
        return value instanceof Path;
    }

    public boolean isNode(Object value)
    {
        return value instanceof Node;
    }

    public boolean isRelationship(Object value)
    {
        return value instanceof Relationship;
    }

    public long nodeId(Object node)
    {
        return ((Node) node).id();
    }

    public List<String> labels(Object value)
    {
        Node node = (Node) value;
        List<String> labels = new ArrayList<>();
        for (String label : node.labels())
        {
            labels.add(label);
        }
        return labels;
    }

    public long relationshipId(Object relationship)
    {
        return ((Relationship) relationship).id();
    }

    public String relationshipType(Object relationship)
    {
        return ((Relationship) relationship).type();
    }

    public Long startNodeId(Object relationship)
    {
        return ((Relationship) relationship).startNodeId();
    }

    public Long endNodeId(Object relationship)
    {
        return ((Relationship) relationship).endNodeId();
    }

    public Map<String, Object> properties(Object container)
    {
        return ((Entity) container).asMap(this::toMapped);
    }

    public List<Object> nodesInPath(Object pathValue)
    {
        Path path = (Path) pathValue;
        List<Object> nodes = new ArrayList<>(path.length());
        for (Node node : path.nodes())
        {
            nodes.add(node);
        }
        return nodes;
    }

    public List<Object> relsInPath(Object pathValue)
    {
        Path path = (Path) pathValue;
        List<Object> rels = new ArrayList<>(path.length());
        for (Relationship rel : path.relationships())
        {
            rels.add(rel);
        }
        return rels;
    }

    private Object toMapped(Value value)
    {

        if (value == null)
        {
            return null;
        }

        if (value instanceof ListValue)
        {
            return value.asList(this::toMapped);
        }
        
        Object object = value.asObject();
        return NoNativeTypes.INSTANCE.getNativeToMappedTypeAdapter(object.getClass())
                                     .apply(object);
    }

}
