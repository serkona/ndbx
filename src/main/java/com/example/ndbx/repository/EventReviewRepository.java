package com.example.ndbx.repository;

import com.example.ndbx.model.EventReview;
import com.example.ndbx.model.EventReviewKey;
import com.example.ndbx.util.Constants;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventReviewRepository extends CassandraRepository<EventReview, EventReviewKey> {

    @Query("SELECT * FROM " + Constants.CASSANDRA_TABLE_EVENT_REVIEWS
            + " WHERE " + Constants.PV_EVENT_ID + " = ?0")
    List<EventReview> findByEventId(String eventId);
}