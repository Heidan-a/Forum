package com.example.controller;


import com.example.entity.RestBean;
import com.example.entity.dto.Interact;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.*;
import com.example.service.JudgeTopicService;
import com.example.service.TopicService;
import com.example.service.WeatherService;
import com.example.utils.Const;
import com.example.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/judge")
public class JudgeForumController {


    @Resource
    TopicService topicService;
    @Resource
    JudgeTopicService judgeTopicService;

    @Resource
    ControllerUtils controllerUtils;

    /**
     * 请求types
     * @return 封装好的帖子类型列表
     */
    @GetMapping("/types")
    public RestBean<List<TopicTypeVO>> types() {
        return RestBean.success(topicService
                .listTypes()
                .stream()
                .map(type -> type.asViewObject(TopicTypeVO.class))
                .toList());
    }


    /**
     * 得到主页的帖子列表
     * @param page 当前页数
     * @param type 帖子类型
     * @return 符合条件的帖子列表
     */
    @GetMapping("/list-topic")
    public RestBean<List<JudgeTopicPreviewVO>> topicList(@RequestParam @Min(0) @Max(200) int page,
                                                    @RequestParam @Min(0) int type) {
        return RestBean.success(judgeTopicService.listTopicByPage(page + 1, type));
    }



    /**
     * 点击topic后的详细内容
     * @param tid 帖子id
     * @param uid 用户id
     * @return 根据当前用户得到的详细内容，包含交互
     */
    @GetMapping("/topic")
    public RestBean<TopicDetailsVO> topic(@RequestParam @Min(0) int tid,
                                          @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return RestBean.success(judgeTopicService.getTopic(tid, uid));
    }

    @GetMapping("/agree")
    public RestBean<Void> agree(@RequestParam @Min(0) int tid,
                                          @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        judgeTopicService.agreeTopic(tid);
        return RestBean.success();
    }

    @GetMapping("/delete")
    public RestBean<Void> delete(@RequestParam @Min(0) int tid,
                                          @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        judgeTopicService.deleteTopic(tid);
        return RestBean.success();
    }
}
