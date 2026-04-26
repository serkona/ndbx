package com.example.ndbx.repository;

import com.example.ndbx.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);

    @Query("{ 'full_name' : { $regex: ?0, $options: 'i' } }")
    Page<User> findByFullNameLike(String name, Pageable pageable);
}
