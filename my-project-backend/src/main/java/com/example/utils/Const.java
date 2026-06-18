package com.example.utils;

import com.example.common.constants.RedisKeys;
import com.example.common.constants.RequestAttributes;

/**
 * Constants for forum-monolith-service.
 * User/auth constants moved to user-service.
 */
public final class Const {
    // Rate limiting
    public static final String FLOW_LIMIT_COUNTER = "flow:counter:";
    public static final String FLOW_LIMIT_BLOCK = "flow:block:";
    public static final String BANNED_BLOCK = RedisKeys.BANNED_BLOCK;
    // Filter order
    public static final int ORDER_FLOW_LIMIT = -101;
    public static final int ORDER_CORS = -102;
    // Request attributes
    public static final String ATTR_USER_ID = RequestAttributes.USER_ID;
    // Message queues
    public static final String MQ_MAIL = "mail";
    public static final String MQ_ERROR = "error";
    // User roles
    public static final String ROLE_DEFAULT = "user";
    public static final String ROLE_ADMIN = "admin";
    // Forum-related
    public static final String FORUM_WEATHER_CACHE = "weather:cache:";
    public static final String FORUM_TOPIC_CREATE_COUNTER = "forum:topic:create:";
    public static final String FORUM_TOPIC_COMMENT_COUNTER = "forum:topic:comment:";
    public static final String FORUM_TOPIC_PREVIEW_CACHE = "topic:preview:";
}
