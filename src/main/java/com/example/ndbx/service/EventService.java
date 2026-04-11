package com.example.ndbx.service;

import com.example.ndbx.model.Event;
import com.example.ndbx.repository.EventRepository;
import com.example.ndbx.util.Constants;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class EventService {
    private static final Set<String> VALID_CATEGORIES = Set.of("meetup", "concert", "exhibition", "party", "other");

    private final EventRepository eventRepository;
    private final MongoTemplate mongoTemplate;

    public EventService(EventRepository eventRepository, MongoTemplate mongoTemplate) {
        this.eventRepository = eventRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public boolean existsByTitle(String title) {
        return eventRepository.existsByTitle(title);
    }

    public Event createEvent(String title, String description, String address, String startedAt, String finishedAt, String userId) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        Event.Location location = new Event.Location();
        location.setAddress(address);
        event.setLocation(location);
        event.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        event.setCreatedBy(userId);
        event.setStartedAt(startedAt);
        event.setFinishedAt(finishedAt);
        return eventRepository.save(event);
    }

    public boolean isValidCategory(String category) {
        return VALID_CATEGORIES.contains(category);
    }

    public boolean updateEvent(String id, String userId, Map<String, Object> body) {
        Query query = new Query(Criteria.where("_id").is(id).and(Constants.FLD_CREATED_BY).is(userId));
        Event event = mongoTemplate.findOne(query, Event.class);
        if (event == null) {
            return false;
        }

        Update update = new Update();
        if (body.containsKey(Constants.FLD_CATEGORY)) {
            update.set(Constants.FLD_CATEGORY, body.get(Constants.FLD_CATEGORY));
        }
        if (body.containsKey(Constants.FLD_PRICE)) {
            update.set(Constants.FLD_PRICE, ((Number) body.get(Constants.FLD_PRICE)).intValue());
        }
        if (body.containsKey(Constants.FLD_CITY)) {
            String city = (String) body.get(Constants.FLD_CITY);
            if (city == null || city.isEmpty()) {
                update.unset(Constants.FLD_LOCATION + "." + Constants.FLD_CITY);
            } else {
                update.set(Constants.FLD_LOCATION + "." + Constants.FLD_CITY, city);
            }
        }

        mongoTemplate.updateFirst(query, update, Event.class);
        return true;
    }

    public Optional<Event> getEventById(String id) {
        return eventRepository.findById(id);
    }

    public List<Event> getEventsByUserId(String userId) {
        return eventRepository.findByCreatedBy(userId);
    }

    public List<Event> searchEvents(String title, String id, String category, String city, String user,
                                    Integer priceFrom, Integer priceTo, LocalDate dateFrom, LocalDate dateTo,
                                    int limit, int offset) {
        List<Criteria> criteriaList = new ArrayList<>();

        Optional.ofNullable(id)
                .filter(StringUtils::hasText)
                .ifPresent(val -> criteriaList.add(Criteria.where("_id").is(val)));

        Optional.ofNullable(title)
                .filter(StringUtils::hasText)
                .ifPresent(val -> criteriaList.add(Criteria.where(Constants.FLD_TITLE).regex(val, "i")));

        Optional.ofNullable(category)
                .filter(StringUtils::hasText)
                .ifPresent(val -> criteriaList.add(Criteria.where(Constants.FLD_CATEGORY).is(val)));

        Optional.ofNullable(city)
                .filter(StringUtils::hasText)
                .ifPresent(val -> criteriaList.add(Criteria.where(Constants.FLD_LOCATION + "." + Constants.FLD_CITY).is(val)));

        Optional.ofNullable(user)
                .filter(StringUtils::hasText)
                .map(this::resolveUserIdByUsername)
                .filter(StringUtils::hasText)
                .ifPresent(val -> criteriaList.add(Criteria.where(Constants.FLD_CREATED_BY).is(val)));

        if (priceFrom != null && priceTo != null) {
            criteriaList.add(Criteria.where(Constants.FLD_PRICE).gte(priceFrom).lte(priceTo));
        } else {
            Optional.ofNullable(priceFrom).ifPresent(val -> criteriaList.add(Criteria.where(Constants.FLD_PRICE).gte(val)));
            Optional.ofNullable(priceTo).ifPresent(val -> criteriaList.add(Criteria.where(Constants.FLD_PRICE).lte(val)));
        }

        Optional.ofNullable(dateFrom)
                .map(d -> d.atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .ifPresent(val -> criteriaList.add(Criteria.where(Constants.FLD_STARTED_AT).gte(val)));

        Optional.ofNullable(dateTo)
                .map(d -> d.plusDays(1).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .ifPresent(val -> criteriaList.add(Criteria.where(Constants.FLD_STARTED_AT).lt(val)));

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        query.skip(offset).limit(limit);

        return mongoTemplate.find(query, Event.class);
    }

    private String resolveUserIdByUsername(String username) {
        Query q = new Query(Criteria.where(Constants.FLD_USERNAME).is(username));
        var user = mongoTemplate.findOne(q, com.example.ndbx.model.User.class);
        return user != null ? user.getId() : "";
    }

    public Map<String, Object> eventToMap(Event e) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(Constants.FLD_ID, e.getId());
        map.put(Constants.FLD_TITLE, e.getTitle());
        if (e.getCategory() != null) {
            map.put(Constants.FLD_CATEGORY, e.getCategory());
        }
        if (e.getPrice() != null) {
            map.put(Constants.FLD_PRICE, e.getPrice());
        }
        map.put(Constants.FLD_DESCRIPTION, e.getDescription() != null ? e.getDescription() : "");

        Map<String, Object> locationMap = new LinkedHashMap<>();
        if (e.getLocation() != null) {
            if (e.getLocation().getCity() != null) {
                locationMap.put(Constants.FLD_CITY, e.getLocation().getCity());
            }
            if (e.getLocation().getAddress() != null) {
                locationMap.put(Constants.FLD_ADDRESS, e.getLocation().getAddress());
            }
        }
        map.put(Constants.FLD_LOCATION, locationMap);
        map.put(Constants.FLD_CREATED_AT, e.getCreatedAt());
        map.put(Constants.FLD_CREATED_BY, e.getCreatedBy());
        map.put(Constants.FLD_STARTED_AT, e.getStartedAt());
        map.put(Constants.FLD_FINISHED_AT, e.getFinishedAt());
        return map;
    }
}
