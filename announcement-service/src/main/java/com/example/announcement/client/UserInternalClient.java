package com.example.announcement.client;

import com.example.common.constants.GatewayHeaders;
import com.example.common.entity.RestBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", path = "/internal/user", contextId = "announcementUserInternalClient")
public interface UserInternalClient {

    @PostMapping("/batch")
    RestBean<Map<Integer, Map<String, Object>>> batchQuery(
            @RequestHeader(GatewayHeaders.INTERNAL_TOKEN) String token,
            @RequestBody Map<String, List<Integer>> body);
}
