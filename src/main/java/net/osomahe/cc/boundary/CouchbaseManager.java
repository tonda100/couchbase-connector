package net.osomahe.cc.boundary;

import static com.couchbase.client.java.query.dsl.Expression.x;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.validation.constraints.NotNull;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.couchbase.client.java.query.ParameterizedN1qlQuery;
import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.Statement;
import com.couchbase.client.java.query.dsl.Expression;
import net.osomahe.cc.entity.CBAggregate;
import net.osomahe.cc.entity.CBType;


/**
 * TODO write JavaDoc
 *
 * @author Antonin Stoklasek
 */
public class CouchbaseManager {

    protected static final String DOT = ".";

    protected static final String ID_KEY = "id";

    protected static final String TYPE_KEY = "type";

    protected static final String CONTENT_KEY = "content";

    protected final String bucketName;

    protected final Bucket bucket;

    protected final Jsonb jsonb;

    public CouchbaseManager(CouchbaseCluster cluster, String bucketName) {
        this.bucketName = bucketName;
        this.jsonb = JsonbBuilder.create();
        this.bucket = cluster.openBucket(bucketName);
    }

    private void getBigDecimalSerializer(Object o, JsonGenerator jsonGenerator, SerializationContext serializationContext) {


    }


    public <T extends CBAggregate> void save(T aggregate) {
        Map aggregateMap = createMap(aggregate);
        Optional<String> oType = getType(aggregate);
        JsonObject data;
        if (oType.isPresent()) {
            data = JsonObject.create()
                    .put(TYPE_KEY, oType.get())
                    .put(CONTENT_KEY, aggregateMap);
        } else {
            data = JsonObject.from(aggregateMap);
        }
        save(aggregate.getId(), data);
    }

    protected void save(String id, JsonObject data) {
        save(JsonDocument.create(id, data));
    }

    protected void save(JsonDocument document) {
        bucket.upsert(document);
    }

    private <T extends CBAggregate> Map<String, ?> createMap(T aggregate) {
        Map<String, ?> map = jsonb.fromJson(jsonb.toJson(aggregate), Map.class);
        map.remove(ID_KEY);
        return map;
    }

    public <T extends CBAggregate> T find(String aggregateId, Class<T> aClass) {
        JsonDocument doc = this.bucket.get(aggregateId);
        Map<String, Object> content = doc.content().toMap();
        Optional<String> oType = getType(aClass);
        String jsonString;
        if (oType.isPresent()) {
            jsonString = jsonb.toJson(content.get(CONTENT_KEY));
        } else {
            jsonString = jsonb.toJson(content);
        }
        T aggregate = jsonb.fromJson(jsonString, aClass);
        aggregate.setId(doc.id());
        return aggregate;
    }

    private Optional<String> getType(@NotNull CBAggregate aggregate) {
        return getType(aggregate.getClass());
    }

    protected <T extends CBAggregate> Optional<String> getType(@NotNull Class<T> aClass) {
        CBType type = aClass.getDeclaredAnnotation(CBType.class);
        if (type != null) {
            return Optional.of(type.value());
        }
        return Optional.empty();
    }

    public <T extends CBAggregate> List<T> findAllByCriteria(final Class<T> aClass, final Map<String, ?> criteriaMap) {
        String type = getType(aClass).get();
        JsonObject placeholderValues = JsonObject.create()
                .put(TYPE_KEY, type);
        Expression exprWhere = x(TYPE_KEY).eq(x("$" + TYPE_KEY));

        for (Map.Entry<String, ?> entry : criteriaMap.entrySet()) {
            if (entry.getValue() != null) {
                placeholderValues.put(entry.getKey(), entry.getValue());
                exprWhere = exprWhere.and(x(CONTENT_KEY + DOT + entry.getKey()).eq(x("$" + entry.getKey())));
            } else {
                exprWhere = exprWhere.and(x(CONTENT_KEY + DOT + entry.getKey()).isMissing());
            }
        }
        Statement stat = Select.select("meta() as meta", CONTENT_KEY).from(bucketName)
                .where(exprWhere);

        ParameterizedN1qlQuery query = N1qlQuery.parameterized(stat, placeholderValues);
        N1qlQueryResult queryResult = bucket.query(query);
        List<N1qlQueryRow> rows = queryResult.allRows();
        List<T> results = new ArrayList<T>(rows.size());
        for (N1qlQueryRow row : rows) {
            JsonObject jsonObjectContent = row.value().getObject(CONTENT_KEY);
            String jsonString = jsonb.toJson(jsonObjectContent.toMap());
            T aggregate = jsonb.fromJson(jsonString, aClass);
            aggregate.setId(row.value().getObject("meta").getString(ID_KEY));
            results.add(aggregate);
        }

        return results;
    }

    public <T extends CBAggregate> void delete(String id) {
        this.bucket.remove(id);
    }

    @PreDestroy
    public void destroy() {
        this.bucket.close(10, TimeUnit.SECONDS);
    }
}
