package com.example.controller.internal;

import com.example.common.constants.GatewayHeaders;
import com.example.common.entity.RestBean;
import com.example.entity.es.TopicDocument;
import com.example.repository.TopicRepository;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/internal/forum")
public class ForumInternalController {

    @Resource
    private TopicRepository repository;

    @Value("${internal.service.token}")
    private String internalToken;

    @GetMapping("/search")
    public RestBean<List<ForumPostSummary>> search(
            @RequestHeader(value = GatewayHeaders.INTERNAL_TOKEN, required = false) String token,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") int limit) {
        if (!authorized(token)) {
            return RestBean.unauthorized("无效的内部服务凭证");
        }
        if (keyword == null || keyword.isBlank()) {
            return RestBean.failure(400, "搜索关键词不能为空");
        }
        int safeLimit = Math.max(1, Math.min(limit, 10));
        List<ForumPostSummary> result = repository.findByTitleOrIntro(keyword.trim()).stream()
                .map(SearchHit::getContent)
                .filter(this::visible)
                .limit(safeLimit)
                .map(this::summary)
                .toList();
        return RestBean.success(result);
    }

    @GetMapping("/recent")
    public RestBean<List<ForumPostSummary>> recent(
            @RequestHeader(value = GatewayHeaders.INTERNAL_TOKEN, required = false) String token,
            @RequestParam(defaultValue = "5") int limit) {
        if (!authorized(token)) {
            return RestBean.unauthorized("无效的内部服务凭证");
        }
        int safeLimit = Math.max(1, Math.min(limit, 10));
        List<ForumPostSummary> result = repository
                .findByOrderByTimeDesc(PageRequest.of(0, Math.min(safeLimit * 2, 20))).stream()
                .filter(this::visible)
                .limit(safeLimit)
                .map(this::summary)
                .toList();
        return RestBean.success(result);
    }

    private boolean authorized(String token) {
        return internalToken != null && internalToken.equals(token);
    }

    private boolean visible(TopicDocument topic) {
        return !Boolean.TRUE.equals(topic.getInvisible());
    }

    private ForumPostSummary summary(TopicDocument topic) {
        String time = topic.getTime() == null ? null
                : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                        topic.getTime().toInstant().atOffset(ZoneOffset.ofHours(8)));
        return new ForumPostSummary(topic.getId(), topic.getTitle(), topic.getIntro(), time);
    }

    public record ForumPostSummary(Integer id, String title, String intro, String time) {
    }
}
