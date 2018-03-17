package net.osomahe.cc.entity;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.validation.constraints.NotNull;


/**
 * TODO write JavaDoc
 *
 * @author Antonin Stoklasek
 */
public class QueryableDateTime implements Comparable<QueryableDateTime> {

    private final ZonedDateTime zonedDateTime;

    private final Long epochSecond;

    @JsonbCreator
    public QueryableDateTime(
            @NotNull @JsonbProperty("zonedDateTime") ZonedDateTime zonedDateTime,
            @NotNull @JsonbProperty("epochSecond") Long epochSecond) {
        if (!epochSecond.equals(zonedDateTime.toEpochSecond())) {
            throw new IllegalArgumentException(String.format("Incompatible epoch %s with dateTime %s", epochSecond, zonedDateTime));
        }
        this.zonedDateTime = zonedDateTime;
        this.epochSecond = epochSecond;
    }

    public QueryableDateTime(@NotNull ZonedDateTime zonedDateTime) {
        this(zonedDateTime, zonedDateTime.toEpochSecond());
    }

    public QueryableDateTime(@NotNull Long epochSecond) {
        this(ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC), epochSecond);
    }

    public QueryableDateTime() {
        this(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public Long getEpochSecond() {
        return epochSecond;
    }

    @Override
    public String toString() {
        return "QueryableDateTime{" +
                "zonedDateTime=" + zonedDateTime +
                ", epochSecond=" + epochSecond +
                '}';
    }

    @Override
    public int compareTo(QueryableDateTime qdt) {
        return epochSecond.compareTo(qdt.epochSecond);
    }
}
