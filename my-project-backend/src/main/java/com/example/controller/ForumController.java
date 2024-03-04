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
@RequestMapping("/api/forum")
public class ForumController {

    @Resource
    WeatherService service;
    @Resource
    TopicService topicService;
    @Resource
    JudgeTopicService judgeTopicService;

    @Resource
    ControllerUtils controllerUtils;

    /**
     * 请求天气信息
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 天气信息vo
     */
    @GetMapping("/weather")
    public RestBean<WeatherVO> weather(double longitude, double latitude) {
        WeatherVO vo = service.fetchWeather(longitude, latitude);
        return vo == null ? RestBean.failure(400, "获取天气信息失败") : RestBean.success(vo);
    }

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
     * 创建帖子
     * @param vo 创建帖子vo
     * @param id 用户id
     * @return 处理信息
     */
    @PostMapping("/create-topic")
    public RestBean<Void> createTopic(@Valid @RequestBody TopicCreateVO vo,
                                      @RequestAttribute(Const.ATTR_USER_ID) int id) {
        return controllerUtils.messageHandle(() -> topicService.createTopic(id, vo));
    }

    /**
     * 得到主页的帖子列表
     * @param page 当前页数
     * @param type 帖子类型
     * @return 符合条件的帖子列表
     */
    @GetMapping("/list-topic")
    public RestBean<List<TopicPreviewVO>> topicList(@RequestParam @Min(0) @Max(200) int page,
                                                    @RequestParam @Min(0) int type) {
        return RestBean.success(topicService.listTopicByPage(page + 1, type));
    }



    /**
     * 得到top的帖子
     * @return top的帖子列表
     */
    @GetMapping("/top-topic")
    public RestBean<List<TopicTopVO>> topicTop() {
        return RestBean.success(topicService.topTopic());
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
        return RestBean.success(topicService.getTopic(tid, uid));
    }

    /**
     * 用户收藏和点赞操作
     * @param tid 帖子id
     * @param type 交互类型
     * @param state 想到达到的交互状态
     * @param uid 用户id
     * @return 状态码
     */
    @GetMapping("/interact")
    public RestBean<Void> interact(@RequestParam @Min(0) int tid,
                                   @RequestParam @Pattern(regexp = "(like|collect)") String type,
                                   @RequestParam boolean state,
                                   @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        topicService.interact(new Interact(tid, uid, new Date(), type), state);
        return RestBean.success();
    }

    /**
     * 请求我的收藏列表
     * @param uid 用户id
     * @return 用户的收藏列表
     */
    @GetMapping("/collects")
    public RestBean<List<TopicPreviewVO>> collects(@RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return RestBean.success(topicService.collectsTopic(uid));
    }

    /**
     * 修改帖子内容
     * @param vo 新的帖子内容vo
     * @param id 用户id
     * @return 处理信息
     */
    @PostMapping("/update-topic")
    public RestBean<Void> updateTopic(@Valid @RequestBody TopicUpdateVO vo,
                                      @RequestAttribute(Const.ATTR_USER_ID) int id) {

        return controllerUtils.messageHandle(() -> topicService.updateTopic(id, vo));
    }

    /**
     * 添加评论
     * @param vo 评论vo
     * @param id 用户id
     * @return 处理信息
     */
    @PostMapping("/add-comment")
    public RestBean<Void> addComment(@Valid @RequestBody AddCommentVO vo,
                                     @RequestAttribute(Const.ATTR_USER_ID) int id) {
        return controllerUtils.messageHandle(() -> topicService.createComment(id, vo));
    }

    /**
     * 拉取帖子评论内容
     * @param tid 帖子id
     * @param page 评论页码
     * @return 大小为10的评论列表
     */
    @GetMapping("/comments")
    public RestBean<List<CommentVO>> getComments(@RequestParam @Min(0) int tid,
                                                 @RequestParam @Min(0) int page) {
        return RestBean.success(topicService.getComments(tid, page + 1));
    }

    /**
     * 判断用户并删除帖子
     *
     * @param id  评论id
     * @param uid 用户id
     * @return
     */
    @GetMapping("/delete-comment")
    public RestBean<Void> deleteComment(@RequestParam @Min(0) int id,
                                        @RequestAttribute(Const.ATTR_USER_ID) int uid) {
        topicService.deleteComment(id, uid);
        return RestBean.success();
    }
}
