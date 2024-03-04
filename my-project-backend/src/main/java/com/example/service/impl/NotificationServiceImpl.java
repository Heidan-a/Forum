package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Notification;
import com.example.entity.vo.response.NotificationVO;
import com.example.mapper.NotificationMapper;
import com.example.service.NotificationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {
    /**
     * 查找用户通知
     *
     * @param uid 用户id
     * @return 用户通知列表
     */
    @Override
    public List<NotificationVO> findUserNotification(int uid) {
        return this.list(Wrappers.<Notification>query().eq("uid", uid))
                .stream()
                .map(notification -> notification.asViewObject(NotificationVO.class))
                .toList();
    }

    /**
     * 删除用户通知
     * @param id 通知id
     * @param uid 用户id
     */
    @Override
    public void deleteUserNotification(int id, int uid) {
        this.remove(Wrappers.<Notification>query().eq("id", id).eq("uid", uid));
    }

    /**
     * 一键删除所有通知
     * @param uid 用户id
     */
    @Override
    public void deleteUserALLNotification(int uid) {
        this.remove(Wrappers.<Notification>query().eq("uid", uid));
    }

    /**
     * 添加通知
     * @param uid 用户id
     * @param title 标题
     * @param content 内容
     * @param type 类中
     * @param url 地址
     */
    @Override
    public void addNotification(int uid, String title, String content, String type, String url) {
        Notification notification = new Notification();
        notification.setUid(uid);
        notification.setType(type);
        notification.setContent(content);
        notification.setTitle(title);
        notification.setUrl(url);
        this.save(notification);
    }
}
