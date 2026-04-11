package com.example.ndbx.model;

import com.example.ndbx.util.Constants;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.Objects;

@PrimaryKeyClass
public class EventReactionKey implements Serializable {

    @PrimaryKeyColumn(name = Constants.PV_EVENT_ID, ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String eventId;

    @PrimaryKeyColumn(name = Constants.FLD_CREATED_BY, ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private String createdBy;

    public EventReactionKey() {}

    public EventReactionKey(String eventId, String createdBy) {
        this.eventId = eventId;
        this.createdBy = createdBy;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventReactionKey that)) return false;
        return Objects.equals(eventId, that.eventId) && Objects.equals(createdBy, that.createdBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, createdBy);
    }
}
