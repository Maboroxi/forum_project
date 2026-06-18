package com.example.client;

import com.example.common.constants.GatewayHeaders;
import com.example.common.entity.RestBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", path = "/internal/user")
public interface UserInternalClient {

    @PostMapping("/batch")
    RestBean<Map<Integer, Map<String, Object>>> batchQuery(
            @RequestHeader(GatewayHeaders.INTERNAL_TOKEN) String token,
            @RequestBody Map<String, List<Integer>> body);

    @GetMapping("/{id}")
    RestBean<Map<String, Object>> getUserById(
            @RequestHeader(GatewayHeaders.INTERNAL_TOKEN) String token,
            @PathVariable int id);

    @GetMapping("/{id}/status")
    RestBean<Map<String, Boolean>> getUserStatus(
            @RequestHeader(GatewayHeaders.INTERNAL_TOKEN) String token,
            @PathVariable int id);
}
