package com.example.ndbx.repository;

import com.example.ndbx.model.EventReaction;
import com.example.ndbx.model.EventReactionKey;
import com.example.ndbx.util.Constants;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventReactionRepository extends CassandraRepository<EventReaction, EventReactionKey> {
    @Query("SELECT * FROM " + Constants.CASSANDRA_TABLE_EVENT_REACTIONS
            + " WHERE " + Constants.PV_EVENT_ID + " = ?0")
    List<EventReaction> findByEventId(String eventId);
}
