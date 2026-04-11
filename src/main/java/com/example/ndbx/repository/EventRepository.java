package com.example.ndbx.repository;

import com.example.ndbx.model.Event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {
    
    @Query("{ 'title' : { $regex: ?0, $options: 'i' } }")
    Page<Event> findByTitleLike(String title, Pageable pageable);
    
    boolean existsByTitle(String title);

    List<Event> findByCreatedBy(String createdBy);
}
