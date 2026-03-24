package com.example.ndbx.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "events")
@CompoundIndex(def = "{'title': 1, 'created_by': 1}")
public class Event {

    @Id
    private String id;

    @Indexed(unique = true)
    private String title;

    private String description;

    private Location location;

    @Field("created_at")
    private String createdAt;

    @Field("created_by")
    @Indexed
    private String createdBy;

    @Field("started_at")
    private String startedAt;

    @Field("finished_at")
    private String finishedAt;

    public static class Location {
        private String address;

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

    public String getFinishedAt() { return finishedAt; }
    public void setFinishedAt(String finishedAt) { this.finishedAt = finishedAt; }
}
