package org.opencypher.gremlin.neo4j.ogm.request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.StatementRunner;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.neo4j.ogm.exception.CypherException;
import org.neo4j.ogm.model.GraphModel;
import org.neo4j.ogm.model.GraphRowListModel;
import org.neo4j.ogm.model.RestModel;
import org.neo4j.ogm.model.RowModel;
import org.neo4j.ogm.request.DefaultRequest;
import org.neo4j.ogm.request.GraphModelRequest;
import org.neo4j.ogm.request.GraphRowListModelRequest;
import org.neo4j.ogm.request.Request;
import org.neo4j.ogm.request.RestModelRequest;
import org.neo4j.ogm.request.RowModelRequest;
import org.neo4j.ogm.request.Statement;
import org.neo4j.ogm.response.EmptyResponse;
import org.neo4j.ogm.response.Response;
import org.opencypher.gremlin.neo4j.driver.Neo4jDriverEntityAdapter;
import org.opencypher.gremlin.neo4j.ogm.CypherQueryUtils;
import org.opencypher.gremlin.neo4j.ogm.response.GremlinGraphRowModelResponse;
import org.opencypher.gremlin.neo4j.ogm.response.GremlinModelResponse;
import org.opencypher.gremlin.neo4j.ogm.response.GremlinRestModelResponse;
import org.opencypher.gremlin.neo4j.ogm.response.GremlinRowModelResponse;
import org.opencypher.gremlin.translation.CypherAst;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GremlinRequest implements Request
{

    private static final Logger logger = LoggerFactory.getLogger(GremlinRequest.class);

    private final StatementRunner statementRunner;
    private final Neo4jDriverEntityAdapter entityAdapter = new Neo4jDriverEntityAdapter();

    public GremlinRequest(StatementRunner statementRunner)
    {
        this.statementRunner = statementRunner;
    }

    @Override
    public Response<GraphModel> execute(GraphModelRequest query)
    {
        if (query.getStatement().length() == 0)
        {
            return new EmptyResponse<>();
        }
        return new GremlinModelResponse(executeRequest(query), entityAdapter);
    }

    @Override
    public Response<RowModel> execute(RowModelRequest query)
    {
        if (query.getStatement().length() == 0)
        {
            return new EmptyResponse();
        }
        return new GremlinRowModelResponse(executeRequest(query), entityAdapter);
    }

    @Override
    public Response<RowModel> execute(DefaultRequest query)
    {
        final List<RowModel> rowModels = new ArrayList<>();
        String[] columns = null;
        for (Statement statement : query.getStatements())
        {

            StatementResult result = executeRequest(statement);

            if (columns == null)
            {
                try
                {
                    List<String> columnSet = result.keys();
                    columns = columnSet.toArray(new String[columnSet.size()]);
                }
                catch (ClientException e)
                {
                    throw new CypherException(e.code(), e.getMessage(), e);
                }
            }
            try (GremlinRowModelResponse rowModelResponse = new GremlinRowModelResponse(result, entityAdapter))
            {
                RowModel model;
                while ((model = rowModelResponse.next()) != null)
                {
                    rowModels.add(model);
                }
                result.consume();
            }
        }

        return new MultiStatementBasedResponse(columns, rowModels);
    }

    @Override
    public Response<GraphRowListModel> execute(GraphRowListModelRequest query)
    {
        if (query.getStatement().length() == 0) {
            return new EmptyResponse();
        }
        return new GremlinGraphRowModelResponse(executeRequest(query), entityAdapter);
    }

    @Override
    public Response<RestModel> execute(RestModelRequest query)
    {
        if (query.getStatement().length() == 0) {
            return new EmptyResponse();
        }
        return new GremlinRestModelResponse(executeRequest(query), entityAdapter);
    }

    private org.neo4j.driver.v1.StatementResult executeRequest(Statement query)
    {
        Map<String, Object> parameterMap = query.getParameters();
        String cypherStatement = query.getStatement();
        logger.debug("Cypher: {} with params {}", cypherStatement, parameterMap);

        Pair<String, Map<String, Object>> cypherWithParams = Pair.of(cypherStatement, parameterMap);
        cypherStatement = Optional.of(cypherWithParams)
                                  .map(this::inlineParameters)
                                  .map(this::normalizeMergeByIdWithParams)
                                  .map(this::normalizeMatchByIdWithRelationResult)
                                  .map(Pair::getLeft)
                                  .get();
        
        logger.debug("Cypher(normalized): {}", cypherStatement);
        
        CypherAst ast = CypherAst.parse(cypherStatement, parameterMap);
        Translator<String, GroovyPredicate> translator = Translator.builder()
                                                                   .gremlinGroovy()
                                                                   .enableCypherExtensions()
                                                                   .build();
        logger.debug("Gremlin: {}", ast.buildTranslation(translator));

        return statementRunner.run(cypherStatement, parameterMap);
    }

    protected Pair<String, Map<String, Object>> normalizeMatchByIdWithRelationResult(Pair<String, Map<String, Object>> cyphterWithParams)
    {
        return Optional.of(cyphterWithParams)
                       .map(p -> p.getRight())
                       .map(p -> p.get("id"))
                       .map(id -> id.toString())
                       .map(id -> CypherQueryUtils.normalizeMatchByIdWithRelationResult(cyphterWithParams.getLeft(),
                                                                                        id))
                       .map(s -> Pair.of(s, cyphterWithParams.getRight()))
                       .orElse(cyphterWithParams);
    }

    protected Pair<String, Map<String, Object>> normalizeMergeByIdWithParams(Pair<String, Map<String, Object>> cyphterWithParams)
    {
        return Optional.of(cyphterWithParams)
                       .map(p -> p.getRight())
                       .map(p -> p.get("rows"))
                       .filter(r -> r instanceof Collection)
                       .map(r -> (Collection<Object>) r)
                       .map(r -> r.iterator().next())
                       .map(r -> (Map<String, Object>) r)
                       .map(r -> r.get("props"))
                       .filter(p -> p instanceof Map)
                       .map(p -> (Map<String, Object>) p)
                       .map(p -> CypherQueryUtils.normalizeMergeByIdWithParams(cyphterWithParams.getLeft(), p))
                       .map(s -> Pair.of(s, cyphterWithParams.getRight()))
                       .orElse(cyphterWithParams);
    }

    protected Pair<String, Map<String, Object>> inlineParameters(Pair<String, Map<String, Object>> cyphterWithParams)
    {
        return Optional.of(cyphterWithParams.getRight())
                       .filter(p -> !p.isEmpty())
                       .map(p -> CypherQueryUtils.inlineParameters(cyphterWithParams.getLeft(), p))
                       .map(s -> Pair.of(s, cyphterWithParams.getRight()))
                       .orElse(cyphterWithParams);
    }
    
    private static class MultiStatementBasedResponse implements Response<RowModel>
    {
        // This implementation is not good, but it preserved the current
        // behaviour while fixing another bug.
        // While the statements executed in
        // org.neo4j.ogm.drivers.bolt.request.BoltRequest.execute(org.neo4j.ogm.request.DefaultRequest)
        // might return different columns, only the ones of the first result are
        // used. :(
        private final String[] columns;
        private final List<RowModel> rowModels;

        private int currentRow = 0;

        MultiStatementBasedResponse(String[] columns,
                                    List<RowModel> rowModels)
        {
            this.columns = columns;
            this.rowModels = rowModels;
        }

        @Override
        public RowModel next()
        {
            if (currentRow < rowModels.size())
            {
                return rowModels.get(currentRow++);
            }
            return null;
        }

        @Override
        public void close()
        {
        }

        @Override
        public String[] columns()
        {
            return this.columns;
        }
    }

}
