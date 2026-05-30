package com.example.ndbx.model;

import com.example.ndbx.util.Constants;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table(Constants.CASSANDRA_TABLE_EVENT_REVIEWS)
public class EventReview {

    @PrimaryKey
    private EventReviewKey key;

    @Column(Constants.FLD_ID)
    private UUID id;

    @Column(Constants.FLD_RATING)
    private byte rating;

    @Column(Constants.FLD_COMMENT)
    private String comment;

    @Column(Constants.FLD_CREATED_AT)
    private Instant createdAt;

    @Column(Constants.FLD_UPDATED_AT)
    private Instant updatedAt;

    public EventReview() {}

    public EventReviewKey getKey() { return key; }
    public void setKey(EventReviewKey key) { this.key = key; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public byte getRating() { return rating; }
    public void setRating(byte rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
