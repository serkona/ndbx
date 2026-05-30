package com.example.ndbx.util;

public class Constants {
    // events
    public static final String FLD_ID = "id";
    public static final String FLD_TITLE = "title";
    public static final String FLD_DESCRIPTION = "description";
    public static final String FLD_CATEGORY = "category";
    public static final String FLD_PRICE = "price";
    public static final String FLD_LOCATION = "location";
    public static final String FLD_CITY = "city";
    public static final String FLD_ADDRESS = "address";
    public static final String FLD_CREATED_AT = "created_at";
    public static final String FLD_CREATED_BY = "created_by";
    public static final String FLD_STARTED_AT = "started_at";
    public static final String FLD_FINISHED_AT = "finished_at";

    // users
    public static final String FLD_FULL_NAME = "full_name";
    public static final String FLD_USERNAME = "username";
    public static final String FLD_PASSWORD = "password";
    public static final String FLD_PASSWORD_HASH = "password_hash";

    // other
    public static final String FLD_MESSAGE = "message";
    public static final String FLD_EVENTS = "events";
    public static final String FLD_USERS = "users";
    public static final String FLD_COUNT = "count";
    public static final String FLD_REACTIONS = "reactions";

    public static final String PARAM_LIMIT = "limit";
    public static final String PARAM_OFFSET = "offset";
    public static final String PARAM_PRICE_FROM = "price_from";
    public static final String PARAM_PRICE_TO = "price_to";
    public static final String PARAM_DATE_FROM = "date_from";
    public static final String PARAM_DATE_TO = "date_to";
    public static final String PARAM_DEFAULT_LIMIT = "10";
    public static final String PARAM_DEFAULT_OFFSET = "0";

    public static final String PV_EVENT_ID = "event_id";
    public static final String CASSANDRA_TABLE_EVENT_REACTIONS = "event_reactions";
    public static final String CASSANDRA_COL_LIKE_VALUE = "like_value";
    public static final String MSG_NOT_FOUND = "Not found";
}
