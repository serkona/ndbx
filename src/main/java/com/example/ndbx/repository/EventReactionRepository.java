package com.example.ndbx.repository;

import com.example.ndbx.model.EventReaction;
import com.example.ndbx.model.EventReactionKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventReactionRepository extends CassandraRepository<EventReaction, EventReactionKey> {
    List<EventReaction> findByKeyEventId(String eventId);
}
