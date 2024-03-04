package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.*;
import com.example.entity.vo.response.JudgeTopicPreviewVO;
import com.example.entity.vo.response.TopicDetailsVO;
import com.example.mapper.*;
import com.example.service.JudgeTopicService;
import com.example.service.NotificationService;
import com.example.utils.CacheUtils;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JudgeTopicServiceImpl extends ServiceImpl<JudgeTopicMapper, JudgeTopic> implements JudgeTopicService {
    @Resource
    TopicTypeMapper mapper;
    @Resource
    TopicMapper topicMapper;
    @Resource
    JudgeTopicMapper judgeTopicMapper;

    @Resource
    AccountMapper accountMapper;

    @Resource
    AccountDetailsMapper accountDetailsMapper;
    @Resource
    AccountPrivacyMapper accountPrivacyMapper;

    @Resource
    CacheUtils cacheUtils;

    @Resource
    StringRedisTemplate template;

    private Set<Integer> types = null;

    /**
     * 初始化列表，因为types在方法里用到的时候还未生成，所以用postConstruct在impl创建后提前给types赋值
     */
    @PostConstruct
    private void initTypes() {
        types = this.listTypes()
                .stream()
                .map(TopicType::getId)
                .collect(Collectors.toSet());
    }

    /**
     * 直接查找所有类型
     *
     * @return 返回list
     */
    public List<TopicType> listTypes() {
        return mapper.selectList(null);
    }


    /**
     * 按页数查找预览帖子
     * @param pageNumber 帖子页码
     * @param type 帖子类型
     * @return 预览帖子列表
     */
    @Override
    public List<JudgeTopicPreviewVO> listTopicByPage(int pageNumber, int type) {
        String key = Const.JUDGE_TOPIC_PREVIEW_CACHE + pageNumber + ":" + type;
        //先从缓存里找找帖子预览
        List<JudgeTopicPreviewVO> list = cacheUtils.takeListFromCache(key, JudgeTopicPreviewVO.class);
        if (list != null)
            return list;
        //按页查找
        Page<JudgeTopic> page = Page.of(pageNumber, 10);
        if (type == 0)
            baseMapper.selectPage(page, Wrappers.<JudgeTopic>query().orderByDesc("time"));
        else
            baseMapper.selectPage(page, Wrappers.<JudgeTopic>query().eq("type", type).orderByDesc("time"));
        List<JudgeTopic> topics;
//        if(page.getRecords().size() >= 10){
//             topics = page.getRecords().subList((pageNumber-1)*10,(page.getRecords().size()-pageNumber*10>10) ? pageNumber*10 : page.getRecords().size());
//        }
//        else {
//
//        }
        //取得查找到的帖子
        topics = page.getRecords();
        if (topics.isEmpty()) return null;
        //将帖子搞成预览列表
        list = topics.stream().map(this::resolveToPreview).toList();
        //预览时间60秒
        cacheUtils.saveListToCache(key, list, 10);
        return list;
    }

    /**
     *
     * 审核不通过删除帖子
     * @param tid
     * @return
     */
    @Override
    public Void deleteTopic(int tid) {
        judgeTopicMapper.deleteById(tid);
        return null;
    }

    /**
     * 审核通过加入帖子
     * @param tid
     * @return
     */
    @Override
    public Void agreeTopic(int tid) {
        JudgeTopic judgeTopic = judgeTopicMapper.selectById(tid);
        Topic topic = new Topic();
        BeanUtils.copyProperties(judgeTopic,topic);
        if(judgeTopic != null){
            topicMapper.insert(topic);
            deleteTopic(tid);
        }
        return null;
    }

    /**
     * 查找帖子
     * @param tid 帖子id
     * @param uid 用户id
     * @return 帖子列表
     */
    @Override
    public TopicDetailsVO getTopic(int tid, int uid) {
        TopicDetailsVO vo = new TopicDetailsVO();
        //先查topic
        JudgeTopic topic = baseMapper.selectById(tid);
        BeanUtils.copyProperties(topic, vo);
        vo.setInteract(null);
        //用户信息需要根据隐私设置查
        TopicDetailsVO.User user = new TopicDetailsVO.User();
        vo.setUser(this.fillUserDetailsByPrivacy(user, topic.getUid()));
        vo.setComments(null);
        return vo;
    }

    /**
     * 将帖子转换成预览帖子类型
     * @param topic 帖子
     * @return 预览的帖子
     */
    private JudgeTopicPreviewVO resolveToPreview(JudgeTopic topic) {
        JudgeTopicPreviewVO vo = new JudgeTopicPreviewVO();
        BeanUtils.copyProperties(accountMapper.selectById(topic.getUid()), vo);
        BeanUtils.copyProperties(topic, vo);
        vo.setLike(baseMapper.interactCount(topic.getId(), "like"));
        vo.setCollect(baseMapper.interactCount(topic.getId(), "collect"));
//        vo.setAvatar(baseMapper.selectAvatarByUid(topic.getUid()));
//        vo.setUsername(baseMapper.selectUsernameByUid(topic.getUid()));
        List<String> images = new ArrayList<>();
        StringBuilder textPreview = new StringBuilder();
        //取出topic内容转为JSON，然后取出里面的ops
        JSONArray ops = JSONObject.parseObject(topic.getContent()).getJSONArray("ops");
        this.shortComment(ops, textPreview, obj -> images.add(obj.toString()));
        vo.setText(textPreview.length() > 300 ? textPreview.substring(0, 300) : textPreview.toString());
        vo.setImages(images);
        return vo;
    }

    /**
     * 创建短评论，如帖子预览的内容，评论引用的评论内容预览
     * @param ops 富文本的ops数组
     * @param textPreview 文字部分的sb
     * @param imageHandler 图片处理
     */
    private void shortComment(JSONArray ops, StringBuilder textPreview, Consumer<Object> imageHandler) {
        for (Object op : ops) {
            Object insert = JSONObject.from(op).get("insert");
            if (insert instanceof String text) {
                if (textPreview.length() > 300) continue;
                textPreview.append(text);
            } else if (insert instanceof Map<?, ?> map) {
                //这里取出map里的image，因为是jsonObject所以要转化为String
                Optional.ofNullable(map.get("image")).ifPresent(imageHandler);
            }
        }
    }


    //创建状态的Map
    private final Map<String, Boolean> state = new HashMap<>();
    //执行线程池
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);


    /**
     * 通过隐私设置来填充用户细节
     * @param target 目标对象
     * @param uid 用户id
     * @return 经过封装的用户对象
     * @param <T>
     */
    private <T> T fillUserDetailsByPrivacy(T target, int uid) {
        //查出信息和用户
        AccountDetails details = accountDetailsMapper.selectById(uid);
        Account account = accountMapper.selectById(uid);
        //查出隐私设置
        AccountPrivacy privacy = accountPrivacyMapper.selectById(uid);
        //根据隐私设置找出隐藏字段
        String[] ignores = privacy.hiddenFields();
        BeanUtils.copyProperties(account, target, ignores);
        BeanUtils.copyProperties(details, target, ignores);
        return target;
    }


    /**
     * 检查文本字数
     * @param object 富文本JSON对象
     * @param max 文本最大字数限制
     * @return 是否满足字数
     */
    private boolean textLimitCheck(JSONObject object, int max) {
        if (object == null) return false;
        long length = 0;
        for (Object op : object.getJSONArray("ops")) {
            length += JSONObject.from(op).getString("insert").length();
        }
        return length > max ? false : true;
    }
}
