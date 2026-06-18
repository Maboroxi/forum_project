package com.example.notification.controller;

import com.example.common.constants.RequestAttributes;
import com.example.common.entity.RestBean;
import com.example.notification.entity.vo.response.NotificationVO;
import com.example.notification.service.NotificationService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    @Resource
    NotificationService service;

    @GetMapping("/list")
    public RestBean<List<NotificationVO>> listNotification(@RequestAttribute(RequestAttributes.USER_ID) int id) {
        return RestBean.success(service.findUserNotification(id));
    }

    @GetMapping("/delete")
    public RestBean<Void> deleteNotification(@RequestParam @Min(0) int id,
                                              @RequestAttribute(RequestAttributes.USER_ID) int uid) {
        service.deleteUserNotification(id, uid);
        return RestBean.success();
    }

    @GetMapping("/delete-all")
    public RestBean<Void> deleteAllNotification(@RequestAttribute(RequestAttributes.USER_ID) int uid) {
        service.deleteUserAllNotification(uid);
        return RestBean.success();
    }
}
