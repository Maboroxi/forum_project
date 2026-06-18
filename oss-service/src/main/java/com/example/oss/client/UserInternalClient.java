package com.example.oss.client;

import com.example.common.constants.GatewayHeaders;
import com.example.common.entity.RestBean;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "user-service", path = "/internal/user")
public interface UserInternalClient {

    @PutMapping("/{id}/avatar")
    RestBean<Map<String, String>> updateAvatar(
            @RequestHeader(GatewayHeaders.INTERNAL_TOKEN) String token,
            @PathVariable int id,
            @RequestBody Map<String, String> body);
}
