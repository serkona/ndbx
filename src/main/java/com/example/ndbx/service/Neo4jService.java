package com.example.ndbx.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class Neo4jService {

    private final Driver driver;

    public Neo4jService(Driver driver) {
        this.driver = driver;
    }

    public void mergeUser(String userId) {
        try (Session session = driver.session()) {
            session.run("MERGE (:User {id: $id})", Map.of("id", userId));
        }
    }

    public void createLiked(String userId, String eventId, String eventTitle) {
        try (Session session = driver.session()) {
            session.run(
                "MERGE (u:User {id: $userId}) " +
                "MERGE (e:Event {id: $eventId}) SET e.title = $title " +
                "MERGE (u)-[:LIKED]->(e)",
                Map.of("userId", userId, "eventId", eventId, "title", eventTitle)
            );
        }
    }

    public List<String> getRecommendedEventIds(String userId) {
        try (Session session = driver.session()) {
            var result = session.run(
                "MATCH (me:User {id: $userId})-[:LIKED]->(liked:Event)" +
                "<-[:LIKED]-(other:User)-[:LIKED]->(rec:Event) " +
                "WHERE NOT (me)-[:LIKED]->(rec) " +
                "RETURN rec.id AS eventId, count(*) AS score " +
                "ORDER BY score DESC",
                Map.of("userId", userId)
            );
            List<String> ids = new ArrayList<>();
            while (result.hasNext()) {
                ids.add(result.next().get("eventId").asString());
            }
            return ids;
        }
    }
}
