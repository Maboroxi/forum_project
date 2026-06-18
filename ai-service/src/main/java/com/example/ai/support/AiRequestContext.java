package com.example.ai.support;

public final class AiRequestContext {

    private static final ThreadLocal<Integer> USER_ID = new ThreadLocal<>();

    private AiRequestContext() {
    }

    public static void setUserId(int userId) {
        USER_ID.set(userId);
    }

    public static int requireUserId() {
        Integer userId = USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("缺少 AI 用户上下文");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
