package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Interact;
import com.example.entity.dto.JudgeTopic;
import com.example.entity.dto.Topic;
import com.example.entity.dto.TopicType;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.*;

import java.util.List;

public interface JudgeTopicService extends IService<JudgeTopic> {

    List<JudgeTopicPreviewVO> listTopicByPage(int page, int type);
    Void deleteTopic(int tid);
    Void agreeTopic(int tid);
    TopicDetailsVO getTopic(int tid, int uid);

}
