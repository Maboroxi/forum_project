package com.example.ai.client;

import com.example.common.entity.RestBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "forum-monolith-service", contextId = "forumInternalClient")
public interface ForumInternalClient {

    @GetMapping("/internal/forum/search")
    RestBean<List<ForumPostSummary>> search(@RequestParam String keyword,
                                            @RequestParam int limit);

    @GetMapping("/internal/forum/recent")
    RestBean<List<ForumPostSummary>> recent(@RequestParam int limit);

    record ForumPostSummary(Integer id, String title, String intro, String time) {
    }
}
