package com.example.user.controller.internal;

import com.example.common.constants.GatewayHeaders;
import com.example.common.entity.RestBean;
import com.example.user.entity.dto.Account;
import com.example.user.entity.dto.AccountDetails;
import com.example.user.entity.dto.AccountPrivacy;
import com.example.user.entity.vo.response.AccountVO;
import com.example.user.service.AccountDetailsService;
import com.example.user.service.AccountPrivacyService;
import com.example.user.service.AccountService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/user")
public class UserInternalController {

    @Resource
    private AccountService accountService;

    @Resource
    private AccountDetailsService detailsService;

    @Resource
    private AccountPrivacyService privacyService;

    @Value("${internal.service.token}")
    private String internalToken;

    private boolean authorized(String token) {
        return internalToken != null && internalToken.equals(token);
    }

    /**
     * Batch query user summaries for forum posts/comments.
     */
    @PostMapping("/batch")
    public RestBean<Map<Integer, Map<String, Object>>> batchQuery(
            @RequestHeader(value = GatewayHeaders.INTERNAL_TOKEN, required = false) String token,
            @RequestBody Map<String, List<Integer>> body) {
        if (!authorized(token)) {
            return RestBean.unauthorized("无效的内部服务凭证");
        }
        List<Integer> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return RestBean.success(Map.of());
        }
        Map<Integer, Map<String, Object>> result = new HashMap<>();
        for (Integer id : ids) {
            Account account = accountService.findAccountById(id);
            if (account == null) continue;
            AccountDetails details = detailsService.findAccountDetailsById(id);
            AccountPrivacy privacy = privacyService.accountPrivacy(id);
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", account.getId());
            userData.put("username", account.getUsername());
            userData.put("avatar", account.getAvatar());
            userData.put("role", account.getRole());
            userData.put("mute", account.isMute());
            userData.put("banned", account.isBanned());
            if (details != null) {
                userData.put("gender", details.getGender());
                userData.put("phone", details.getPhone());
                userData.put("qq", details.getQq());
                userData.put("wx", details.getWx());
                userData.put("desc", details.getDesc());
            }
            if (privacy != null) {
                userData.put("hiddenFields", privacy.hiddenFields());
            }
            result.put(id, userData);
        }
        return RestBean.success(result);
    }

    /**
     * Get single user summary.
     */
    @GetMapping("/{id}")
    public RestBean<AccountVO> getUserById(
            @RequestHeader(value = GatewayHeaders.INTERNAL_TOKEN, required = false) String token,
            @PathVariable int id) {
        if (!authorized(token)) {
            return RestBean.unauthorized("无效的内部服务凭证");
        }
        Account account = accountService.findAccountById(id);
        if (account == null) {
            return RestBean.failure(404, "用户不存在");
        }
        return RestBean.success(account.asViewObject(AccountVO.class));
    }

    /**
     * Get user status (mute, banned).
     */
    @GetMapping("/{id}/status")
    public RestBean<Map<String, Boolean>> getUserStatus(
            @RequestHeader(value = GatewayHeaders.INTERNAL_TOKEN, required = false) String token,
            @PathVariable int id) {
        if (!authorized(token)) {
            return RestBean.unauthorized("无效的内部服务凭证");
        }
        Account account = accountService.findAccountById(id);
        if (account == null) {
            return RestBean.failure(404, "用户不存在");
        }
        return RestBean.success(Map.of("mute", account.isMute(), "banned", account.isBanned()));
    }

    /**
     * Update user avatar (called by OSS service).
     * Returns the old avatar path so OSS can delete it from MinIO.
     */
    @PutMapping("/{id}/avatar")
    public RestBean<Map<String, String>> updateAvatar(
            @RequestHeader(value = GatewayHeaders.INTERNAL_TOKEN, required = false) String token,
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        if (!authorized(token)) {
            return RestBean.unauthorized("无效的内部服务凭证");
        }
        String avatar = body.get("avatar");
        if (avatar == null || avatar.isBlank()) {
            return RestBean.failure(400, "头像路径不能为空");
        }
        Account account = accountService.findAccountById(id);
        if (account == null) {
            return RestBean.failure(404, "用户不存在");
        }
        String oldAvatar = account.getAvatar();
        accountService.update()
                .eq("id", id)
                .set("avatar", avatar)
                .update();
        return RestBean.success(Map.of("oldAvatar", oldAvatar != null ? oldAvatar : ""));
    }
}
