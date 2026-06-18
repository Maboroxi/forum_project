package com.example.ai.tool;

import com.example.ai.client.ForumInternalClient;
import com.example.common.entity.RestBean;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ForumTools {

    @Resource
    private ForumInternalClient client;

    @Tool(name = "search_forum_posts",
            description = "根据关键词搜索校园论坛帖子，最多返回5条相关结果。")
    public String searchForumPosts(
            @ToolParam(description = "搜索关键词，可以是单个词或短语") String keyword) {
        RestBean<List<ForumInternalClient.ForumPostSummary>> response = client.search(keyword, 5);
        if (response.code() != 200 || response.data() == null || response.data().isEmpty()) {
            return "未找到与「" + keyword + "」相关的帖子。";
        }
        return format("搜索「" + keyword + "」找到以下帖子：", response.data());
    }

    @Tool(name = "get_recent_posts",
            description = "获取校园论坛最新帖子。当用户想了解论坛最近讨论内容时调用。")
    public String getRecentPosts(
            @ToolParam(description = "获取数量，范围1-10，默认5") int count) {
        int limit = count < 1 ? 5 : Math.min(count, 10);
        RestBean<List<ForumInternalClient.ForumPostSummary>> response = client.recent(limit);
        if (response.code() != 200 || response.data() == null || response.data().isEmpty()) {
            return "论坛暂无帖子。";
        }
        return format("论坛最新帖子：", response.data());
    }

    private String format(String heading, List<ForumInternalClient.ForumPostSummary> posts) {
        StringBuilder result = new StringBuilder(heading).append("\n\n");
        for (int i = 0; i < posts.size(); i++) {
            ForumInternalClient.ForumPostSummary post = posts.get(i);
            result.append(i + 1).append(". 【").append(post.title()).append("】\n")
                    .append("   ").append(post.intro() == null ? "" : post.intro()).append("\n\n");
        }
        return result.toString();
    }
}
