package com.example.ai.client;

import com.example.common.entity.RestBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "oss-service", contextId = "ossInternalClient")
public interface OssInternalClient {

    @GetMapping("/internal/file/text")
    RestBean<TextContent> readText(@RequestParam String key, @RequestParam int userId);

    @GetMapping("/internal/image/content")
    RestBean<ImageContent> readImage(@RequestParam String key, @RequestParam int userId);

    record TextContent(String key, String filename, String content, Integer size) {
    }

    record ImageContent(String key, String mediaType, String base64, Long size) {
    }
}
