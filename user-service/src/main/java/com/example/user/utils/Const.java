package com.example.user.utils;

import com.example.common.constants.RedisKeys;
import com.example.common.constants.RequestAttributes;

/**
 * Constants for user-service.
 */
public final class Const {
    // JWT tokens
    public static final String JWT_BLACK_LIST = RedisKeys.JWT_BLACK_LIST;
    public static final String JWT_FREQUENCY = "jwt:frequency:";
    // Rate limiting
    public static final String FLOW_LIMIT_COUNTER = "flow:counter:";
    public static final String FLOW_LIMIT_BLOCK = "flow:block:";
    public static final String BANNED_BLOCK = RedisKeys.BANNED_BLOCK;
    // Email verification
    public static final String VERIFY_EMAIL_LIMIT = "verify:email:limit:";
    public static final String VERIFY_EMAIL_DATA = "verify:email:data:";
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
}
