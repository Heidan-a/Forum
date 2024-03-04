package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.*;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.CommentVO;
import com.example.entity.vo.response.TopicDetailsVO;
import com.example.entity.vo.response.TopicPreviewVO;
import com.example.entity.vo.response.TopicTopVO;
import com.example.mapper.*;
import com.example.service.NotificationService;
import com.example.service.TopicService;
import com.example.utils.CacheUtils;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {
    @Resource
    TopicTypeMapper mapper;

    @Resource
    AccountMapper accountMapper;

    @Resource
    AccountDetailsMapper accountDetailsMapper;
    @Resource
    AccountPrivacyMapper accountPrivacyMapper;
    @Resource
    TopicCommentMapper commentMapper;

    @Resource
    NotificationService notificationService;

    @Resource
    FlowUtils flowUtils;

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
    @Override
    public List<TopicType> listTypes() {
        return mapper.selectList(null);
    }

    /**
     * 获取收藏的帖子列表
     * @param uid
     * @return 收藏帖子列表
     */
    @Override
    public List<TopicPreviewVO> collectsTopic(int uid) {
        return baseMapper.collectTopics(uid)
                .stream()
                .map(topic -> {
                    TopicPreviewVO vo = new TopicPreviewVO();
                    BeanUtils.copyProperties(topic, vo);
                    return vo;
                }).toList();
    }

    /**
     * 按页数查找预览帖子
     * @param pageNumber 帖子页码
     * @param type 帖子类型
     * @return 预览帖子列表
     */
    @Override
    public List<TopicPreviewVO> listTopicByPage(int pageNumber, int type) {
        String key = Const.FORUM_TOPIC_PREVIEW_CACHE + pageNumber + ":" + type;
        //先从缓存里找找帖子预览
        List<TopicPreviewVO> list = cacheUtils.takeListFromCache(key, TopicPreviewVO.class);
        if (list != null)
            return list;
        //按页查找
        Page<Topic> page = Page.of(pageNumber, 10);
        if (type == 0)
            baseMapper.selectPage(page, Wrappers.<Topic>query().orderByDesc("time"));
        else
            baseMapper.selectPage(page, Wrappers.<Topic>query().eq("type", type).orderByDesc("time"));
        List<Topic> topics;
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
        cacheUtils.saveListToCache(key, list, 60);
        return list;
    }

    /**
     * 将帖子转换成预览帖子类型
     * @param topic 帖子
     * @return 预览的帖子
     */
    private TopicPreviewVO resolveToPreview(Topic topic) {
        TopicPreviewVO vo = new TopicPreviewVO();
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

    /**
     * 创建帖子
     * @param uid 用户id
     * @param vo 帖子创建vo
     * @return 处理结果
     */
    @Override
    public String createTopic(int uid, TopicCreateVO vo) {
        if (!textLimitCheck(vo.getContent(), 20000)) return "文章超出字数限制";
        if (!types.contains(vo.getType()))
            return "文章类型非法";
        String key = Const.FORUM_TOPIC_CREATE_COUNTER + uid;
        if (!flowUtils.limitPeriodCounterCheck(key, 5, 3600))
            return "一小时内只能发5次文章";
        Topic topic = new Topic();
        BeanUtils.copyProperties(vo, topic);
        topic.setContent(vo.getContent().toJSONString());
        topic.setUid(uid);
        topic.setTime(new Date());
        if (this.save(topic)) {
            //创建后删除当前redis里的预览，因为要重新获取最新的帖子列表
            cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
            return null;
        } else {
            return "内部错误";
        }
    }

    /**
     * 更新帖子
     * @param id 帖子id
     * @param vo 帖子更新vo
     * @return 处理结果
     */
    @Override
    public String updateTopic(int id, TopicUpdateVO vo) {
        if (!textLimitCheck(vo.getContent(), 20000)) return "文章超出字数限制";
        if (!types.contains(vo.getType()))
            return "文章类型非法";
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("uid", id)
                .eq("id", vo.getId())
                .set("content", vo.getContent().toString())
                .set("type", vo.getType())
                .set("title", vo.getTitle()
                ));
        return null;
    }

    /**
     * 创建评论
     * @param uid 用户id
     * @param vo 评论创建vo
     * @return 处理结果
     */
    @Override
    public String createComment(int uid, AddCommentVO vo) {
        if (!textLimitCheck(JSONObject.parseObject(vo.getContent()), 2000)) return "评论超出字数限制";
        String key = Const.FORUM_COMMENT_CREATE_COUNTER + uid;
        if (!flowUtils.limitPeriodCounterCheck(key, 2, 60))
            return "发表评论频繁";
        TopicComment comment = new TopicComment();
        comment.setUid(uid);
        BeanUtils.copyProperties(vo, comment);
        comment.setTime(new Date());
        commentMapper.insert(comment);
        //拿到帖子和当前评论的用户
        Topic topic = baseMapper.selectById(vo.getTid());
        Account account = accountMapper.selectById(uid);
        //quote>0说明回复的是评论
        if (vo.getQuote() > 0) {
            TopicComment com = commentMapper.selectById(vo.getQuote());
            //如果回复的是自己的评论就不需要提醒
            if (!Objects.equals(com.getUid(), account.getId())) {
                notificationService.addNotification(
                        com.getUid(),
                        "有人回复了你的评论",
                        account.getUsername() + "回复了你的评论，快去看看吧",
                        "success", "/index/topic-detail/" + com.getTid()
                );
            }
        }//判断一下是不是自己回复自己的帖子
        else if (!Objects.equals(topic.getUid(), account.getId())) {
            notificationService.addNotification(
                    topic.getUid(),
                    "你有新的帖子回复",
                    account.getUsername() + "回复了你发表的主题" + topic.getTitle() + "，快去看看吧",
                    "success", "/index/topic-detail/" + topic.getId()
            );
        }
        return null;
    }

    /**
     * 获得帖子评论
     * @param tid 帖子的评论
     * @param pageNumber 评论页码
     * @return 评论列表
     */
    @Override
    public List<CommentVO> getComments(int tid, int pageNumber) {
        Page<TopicComment> pages = Page.of(pageNumber, 10);
        commentMapper.selectPage(pages, Wrappers.<TopicComment>query().eq("tid", tid));
        return pages.getRecords().stream().map(dto -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(dto, vo);
            if (dto.getQuote() > 0) {
                TopicComment comment = commentMapper.selectOne(
                        Wrappers.<TopicComment>query().eq("id", dto.getQuote()).orderByAsc("time"));
                if (comment != null) {
                    JSONObject object = JSONObject.parseObject((comment.getContent()));
                    StringBuilder builder = new StringBuilder();
                    this.shortComment(object.getJSONArray("ops"), builder, ignore -> {
                    });
                    vo.setQuote(builder.toString());
                } else vo.setQuote("此评论已被删除");

            }
            CommentVO.User user = new CommentVO.User();
            this.fillUserDetailsByPrivacy(user, dto.getUid());
            vo.setUser(user);
            return vo;
        }).toList();
    }

    /**
     * 删除评论
     * @param id 评论id
     * @param uid 用户id
     */
    @Override
    public void deleteComment(int id, int uid) {
        commentMapper.delete(Wrappers.<TopicComment>query().eq("id", id).eq("uid", uid));
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
        Topic topic = baseMapper.selectById(tid);
        BeanUtils.copyProperties(topic, vo);
        TopicDetailsVO.Interact interact = new TopicDetailsVO.Interact(
                hasInteract(tid, uid, "like"),
                hasInteract(tid, uid, "collect")
        );
        vo.setInteract(interact);
        //用户信息需要根据隐私设置查
        TopicDetailsVO.User user = new TopicDetailsVO.User();
        vo.setUser(this.fillUserDetailsByPrivacy(user, topic.getUid()));
        vo.setComments(commentMapper.selectCount(Wrappers.<TopicComment>query().eq("tid", tid)));
        return vo;
    }

    /**
     * 从redis判断某个交互类型的状态
     * @param tid
     * @param uid
     * @param type
     * @return
     */
    private boolean hasInteract(int tid, int uid, String type) {
        String key = tid + ":" + uid;
        if (template.opsForHash().hasKey(type, key))
            return Boolean.parseBoolean(template.opsForHash().entries(type).get(key).toString());
        return baseMapper.userInteractCount(tid, uid, type) > 0;
    }

    /**
     * 将交互结果分时间段存入redis
     * @param interact 交互
     * @param state 状态
     */
    @Override
    public void interact(Interact interact, boolean state) {
        String type = interact.getType();
        synchronized (type.intern()) {
            template.opsForHash().put(type, interact.toKey(), Boolean.toString(state));
            this.saveInteractSchedule(type);
        }
    }

    //创建状态的Map
    private final Map<String, Boolean> state = new HashMap<>();
    //执行线程池
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

    //按时段保存交互信息，以三秒为界
    private void saveInteractSchedule(String type) {
        //取出type的状态，没有默认就是false，所有如果本来没有或者是false就执行
        if (!state.getOrDefault(type, false)) {
            state.put(type, true);
            service.schedule(() -> {
                //存完数据库置为false开始下一次
                this.saveInteract(type);
                state.put(type, false);
            }, 3, TimeUnit.SECONDS);
        }
    }

    /**
     * 通过缓存在redis，然后根据type一次性插入减少入库次数
     *
     * @param type like或者collect
     */
    private void saveInteract(String type) {
        synchronized (type.intern()) {
            List<Interact> check = new LinkedList<>();
            List<Interact> uncheck = new LinkedList<>();
            template.opsForHash().entries(type).forEach((k, v) -> {
                if (Boolean.parseBoolean(v.toString())) {
                    check.add(Interact.parseInteract(k.toString(), type));
                } else
                    uncheck.add(Interact.parseInteract(k.toString(), type));
            });
            if (!check.isEmpty()) {
                baseMapper.addInteract(check, type);
            }
            if (!uncheck.isEmpty())
                baseMapper.deleteInteract(uncheck, type);
            template.delete(type);
        }
    }

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
     * 查找top的帖子
     * @return 热门帖子列表
     */
    @Override
    public List<TopicTopVO> topTopic() {
        List<Topic> list = baseMapper.selectList(Wrappers.<Topic>query()
                .select("id", "title", "time")
                .eq("top", 1)
        );
        return list.stream().map(topic -> {
            TopicTopVO vo = new TopicTopVO();
            BeanUtils.copyProperties(topic, vo);
            return vo;
        }).toList();
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
