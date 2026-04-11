package com.example.ndbx.model;

import com.example.ndbx.util.Constants;
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

    @Indexed
    private String title;

    private String description;

    private String category;

    private Integer price;

    private Location location;

    @Field(Constants.FLD_CREATED_AT)
    private String createdAt;

    @Field(Constants.FLD_CREATED_BY)
    @Indexed
    private String createdBy;

    @Field(Constants.FLD_STARTED_AT)
    private String startedAt;

    @Field(Constants.FLD_FINISHED_AT)
    private String finishedAt;

    public static class Location {
        private String city;
        private String address;

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

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
