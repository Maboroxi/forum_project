package com.example.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.notification.entity.dto.Notification;
import com.example.notification.entity.vo.response.NotificationVO;

import java.util.List;

public interface NotificationService extends IService<Notification> {
    List<NotificationVO> findUserNotification(int uid);
    void deleteUserNotification(int id, int uid);
    void deleteUserAllNotification(int uid);
    void addNotification(int uid, String title, String content, String type, String url);
}
