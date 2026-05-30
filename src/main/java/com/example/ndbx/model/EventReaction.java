package com.example.ndbx.model;

import com.example.ndbx.util.Constants;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table(Constants.CASSANDRA_TABLE_EVENT_REACTIONS)
public class EventReaction {

    @PrimaryKey
    private EventReactionKey key;

    @Column(Constants.CASSANDRA_COL_LIKE_VALUE)
    private byte likeValue;

    @Column(Constants.FLD_CREATED_AT)
    private Instant createdAt;

    public EventReaction() {}

    public EventReactionKey getKey() { return key; }
    public void setKey(EventReactionKey key) { this.key = key; }

    public byte getLikeValue() { return likeValue; }
    public void setLikeValue(byte likeValue) { this.likeValue = likeValue; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
