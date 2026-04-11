package com.example.ndbx.util;

public class Constants {
    public static final String FLD_ID = "id";
    public static final String FLD_TITLE = "title";
    public static final String FLD_DESCRIPTION = "description";
    public static final String FLD_CATEGORY = "category";
    public static final String FLD_PRICE = "price";
    public static final String FLD_CITY = "city";
    public static final String FLD_ADDRESS = "address";
    public static final String FLD_LOCATION = "location";
    public static final String FLD_CREATED_AT = "created_at";
    public static final String FLD_CREATED_BY = "created_by";
    public static final String FLD_STARTED_AT = "started_at";
    public static final String FLD_FINISHED_AT = "finished_at";
    
    public static final String FLD_FULL_NAME = "full_name";
    public static final String FLD_USERNAME = "username";
    public static final String FLD_PASSWORD = "password";
    
    public static final String FLD_MESSAGE = "message";
    public static final String FLD_EVENTS = "events";
    public static final String FLD_USERS = "users";
    public static final String FLD_COUNT = "count";

    public static final String COOKIE_NAME = "X-Session-Id";
    public static final String REDIS_KEY_PREFIX = "sid:";
    public static final String REDIS_FLD_USER_ID = "user_id";
    public static final String REDIS_FLD_CREATED_AT = "created_at";
    public static final String REDIS_FLD_UPDATED_AT = "updated_at";

    public static final String MSG_INVALID_FIELD = "invalid \"%s\" field";
    public static final String MSG_NOT_FOUND = "Not found";
    public static final String MSG_EVENT_NOT_FOUND_OR_NOT_ORGANIZER = "Not found. Be sure that event exists and you are the organizer";
    public static final String MSG_USER_NOT_FOUND = "User not found";
    public static final String MSG_EVENT_ALREADY_EXISTS = "event already exists";
    public static final String MSG_USER_ALREADY_EXISTS = "user already exists";
    public static final String MSG_INVALID_CREDENTIALS = "invalid credentials";
}
