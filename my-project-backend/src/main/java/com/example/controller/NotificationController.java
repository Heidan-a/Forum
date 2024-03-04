package com.example.controller;

import com.example.entity.RestBean;
import com.example.entity.vo.response.NotificationVO;
import com.example.service.NotificationService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.security.PublicKey;
import java.util.List;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    @Resource
    NotificationService service;

    /**
     * 返回用户的通知列表
     *
     * @param id 用户id
     * @return 用户通知列表
     */
    @GetMapping("/list")
    public RestBean<List<NotificationVO>> notificationList(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(service.findUserNotification(id));
    }

    /**
     * 删除已读的消息
     * @param id 通知id
     * @param uid 用户id
     * @return 处理消息
     */
    @GetMapping("/delete")
    public RestBean<List<NotificationVO>> deleteNotification(@RequestParam @Min(0) int id,
                                                             @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        service.deleteUserNotification(id, uid);
        return RestBean.success();
    }


    /**
     * 一键清除所有通知
     * @param uid 用户id
     * @return
     */
    @GetMapping("/delete-all")
    public RestBean<List<NotificationVO>> deleteAllNotification(@RequestAttribute(Const.ATTR_USER_ID) int uid) {
        service.deleteUserALLNotification(uid);
        return RestBean.success();
    }




}
