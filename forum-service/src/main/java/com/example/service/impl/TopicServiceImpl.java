package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.client.UserInternalClient;
import com.example.entity.dto.*;
import com.example.entity.es.TopicDocument;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicTypeCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.*;
import com.example.mapper.*;
import com.example.observability.RabbitRequestContext;
import com.example.repository.TopicRepository;
import com.example.service.TopicService;
import com.example.utils.CacheUtils;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import com.example.utils.ProhibitedUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    @Resource
    TopicTypeMapper mapper;

    @Resource
    FlowUtils flowUtils;

    @Resource
    CacheUtils cacheUtils;

    @Resource
    ProhibitedUtils prohibitedUtils;

    @Resource
    UserInternalClient userInternalClient;

    @Resource
    TopicCommentMapper commentMapper;

    @Resource
    StringRedisTemplate template;

    @Resource
    AmqpTemplate amqpTemplate;

    @Resource
    TopicRepository topicRepository;

    @Value("${internal.service.token}")
    private String internalToken;

    private static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    private static final String NOTIFICATION_ROUTING_KEY = "notification.event";

    private Set<Integer> types = null;
    @PostConstruct
    private void initTypes() {
        types = this.listTypes()
                .stream()
                .map(TopicType::getId)
                .collect(Collectors.toSet());
        try {
            syncAllTopicsToEs();
        } catch (Exception ignored) {
            // ES may not be ready; individual sync covers it
        }
    }

    @Override
    public List<TopicType> listTypes() {
        return mapper.selectList(null);
    }

    @Override
    public void updateTopicType(TopicTypeVO vo) {
        TopicType topicType = mapper.selectById(vo.getId());
        BeanUtils.copyProperties(vo, topicType);
        mapper.updateById(topicType);
    }

    @Override
    public void deleteTopicType(int id) {
        TopicType type = mapper.selectById(id);
        if(mapper.deleteById(id) > 0) {
            List<Topic> list = baseMapper.selectList(Wrappers.<Topic>query().eq("type", type.getId()));
            list.forEach(topic -> deleteTopic(topic.getId()));
        }
    }

    @Override
    public void createTopicType(TopicTypeCreateVO vo) {
        TopicType type = new TopicType();
        BeanUtils.copyProperties(vo, type);
        mapper.insert(type);
    }

    @Override
    public void changeTopicType(int tid, int type) {
        if(baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("type", type)
        ) > 1) {
            cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        }
    }

    @Override
    public String createTopic(int uid, TopicCreateVO vo) {
        if(!textLimitCheck(vo.getContent(), 20000))
            return "文章内容太多，发文失败！";
        if(!types.contains(vo.getType()))
            return "文章类型非法！";
        String key = Const.FORUM_TOPIC_CREATE_COUNTER + uid;
        if(!flowUtils.limitPeriodCounterCheck(key, 3, 3600))
            return "发文频繁，请稍后再试！";
        if(prohibitedUtils.containsProhibitedWord(vo.getContent()))
            return "包含违禁词，发文失败！";
        Topic topic = new Topic();
        BeanUtils.copyProperties(vo, topic);
        topic.setContent(vo.getContent().toJSONString());
        topic.setUid(uid);
        topic.setTime(new Date());
        topic.createIntro();
        if(this.save(topic)) {
            cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
            syncTopicToEs(topic);
            return null;
        } else {
            return "内部错误，请联系管理员！";
        }
    }

    @Override
    public String updateTopic(int uid, TopicUpdateVO vo) {
        if(!textLimitCheck(vo.getContent(), 20000))
            return "文章内容太多，发文失败！";
        if(!types.contains(vo.getType()))
            return "文章类型非法！";
        if(prohibitedUtils.containsProhibitedWord(vo.getContent()))
            return "包含违禁词，发文失败！";
        int result = baseMapper.update(null, Wrappers.<Topic>update()
                .eq("uid", uid)
                .eq("id", vo.getId())
                .eq("locked", 0)
                .set("title", vo.getTitle())
                .set("content", vo.getContent().toString())
                .set("type", vo.getType())
                .set("intro", Topic.recreateIntro(vo.getContent()))
        );
        if (result > 0) {
            Topic updated = baseMapper.selectById(vo.getId());
            syncTopicToEs(updated);
            return null;
        }
        return "文章被锁定，无法进行修改";
    }

    @Override
    public String createComment(int uid, AddCommentVO vo) {
        if(!textLimitCheck(JSONObject.parseObject(vo.getContent()), 2000))
            return "评论内容太多，发表失败！";
        String key = Const.FORUM_TOPIC_COMMENT_COUNTER + uid;
        if(!flowUtils.limitPeriodCounterCheck(key, 2, 60))
            return "发表评论频繁，请稍后再试！";
        if(prohibitedUtils.containsProhibitedWord(vo.getContent()))
            return "包含违禁词，发文失败！";
        TopicComment comment = new TopicComment();
        comment.setUid(uid);
        BeanUtils.copyProperties(vo, comment);
        comment.setTime(new Date());
        commentMapper.insert(comment);
        Topic topic = baseMapper.selectById(vo.getTid());
        String username = this.resolveUsername(uid);
        if(vo.getQuote() > 0) {
            TopicComment com = commentMapper.selectById(vo.getQuote());
            if(!Objects.equals(uid, com.getUid())) {
                Map<String, Object> msg = Map.of(
                        "recipientUid", com.getUid(),
                        "title", "您有新的帖子评论回复",
                        "content", username+" 回复了你发表的评论，快去看看吧！",
                        "type", "success",
                        "url", "/index/topic-detail/"+com.getTid()
                );
                amqpTemplate.convertAndSend(
                        NOTIFICATION_EXCHANGE, NOTIFICATION_ROUTING_KEY, msg,
                        RabbitRequestContext.outbound());
            }
        } else if (!Objects.equals(uid, topic.getUid())) {
            Map<String, Object> msg = Map.of(
                    "recipientUid", topic.getUid(),
                    "title", "您有新的帖子回复",
                    "content", username+" 回复了你发表主题: "+topic.getTitle()+"，快去看看吧！",
                    "type", "success",
                    "url", "/index/topic-detail/"+topic.getId()
            );
            amqpTemplate.convertAndSend(
                    NOTIFICATION_EXCHANGE, NOTIFICATION_ROUTING_KEY, msg,
                    RabbitRequestContext.outbound());
        }
        return null;
    }

    @Override
    public List<CommentVO> comments(int tid, int pageNumber) {
        Page<TopicComment> page = Page.of(pageNumber, 10);
        commentMapper.selectPage(page, Wrappers.<TopicComment>query().eq("tid", tid));
        List<Integer> userIds = page.getRecords().stream()
                .map(TopicComment::getUid)
                .distinct()
                .toList();
        Map<Integer, Map<String, Object>> userDataMap = this.batchFetchUserData(userIds);
        return page.getRecords().stream().map(dto -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(dto, vo);
            if(dto.getQuote() > 0) {
                TopicComment comment = commentMapper.selectOne(Wrappers.<TopicComment>query()
                        .eq("id", dto.getQuote()).orderByAsc("time"));
                if(comment != null) {
                    JSONObject object = JSONObject.parseObject(comment.getContent());
                    StringBuilder builder = new StringBuilder();
                    this.shortContent(object.getJSONArray("ops"), builder, ignore -> {});
                    vo.setQuote(builder.toString());
                } else {
                    vo.setQuote("此评论已被删除");
                }
            }
            CommentVO.User user = new CommentVO.User();
            this.fillUserDetailsFromMap(user, dto.getUid(), userDataMap);
            vo.setUser(user);
            return vo;
        }).toList();
    }

    @Override
    public void deleteComment(int id, int uid) {
        commentMapper.delete(Wrappers.<TopicComment>query().eq("id", id).eq("uid", uid));
    }

    @Override
    public void deleteTopic(int id) {
        baseMapper.deleteById(id);
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
        baseMapper.deleteTopicCollect(id);
        deleteTopicFromEs(id);
    }

    @Override
    public void deleteTopic(int tid, int uid) {
        int result = baseMapper.delete(Wrappers.<Topic>query()
                .eq("id", tid)
                .eq("uid", uid)
        );
        if(result > 0) {
            cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
            baseMapper.deleteTopicCollect(tid);
            deleteTopicFromEs(tid);
        }
    }

    @Override
    public void setTopicTop(int tid, boolean top) {
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("top", top)
        );
    }

    @Override
    public void setTopicLocked(int tid, boolean locked) {
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("locked", locked)
        );
    }

    @Override
    public void setTopicInvisible(int tid, boolean invisible) {
        baseMapper.update(null, Wrappers.<Topic>update()
                .eq("id", tid)
                .set("invisible", invisible)
        );
        cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
    }

    @Override
    public List<TopicPreviewVO> listTopicCollects(int uid) {
        return baseMapper.collectTopics(uid)
                .stream()
                .map(topic -> {
                    TopicPreviewVO vo = new TopicPreviewVO();
                    BeanUtils.copyProperties(topic, vo);
                    return vo;
                })
                .toList();
    }

    @Override
    public JSONObject listAllTopicByPage(int page, int size, String keyword) {
        Page<Topic> topicPage = baseMapper.selectPage(Page.of(page, size), Wrappers.<Topic>query()
                .select("id", "title", "uid", "type", "time", "top", "locked", "invisible")
                .like(keyword != null, "title", "%" + keyword + "%")
                .orderByDesc("time"));
        List<Integer> userIds = topicPage.getRecords().stream()
                .map(Topic::getUid)
                .distinct()
                .toList();
        Map<Integer, Map<String, Object>> userDataMap = this.batchFetchUserData(userIds);
        List<TopicPreviewVO> list = topicPage.getRecords().stream()
                .map(topic -> resolveToPreview(topic, userDataMap))
                .toList();
        JSONObject object = new JSONObject();
        object.put("total", topicPage.getTotal());
        object.put("list", list);
        return object;
    }

    @Override
    public List<TopicPreviewVO> listTopicByPage(int pageNumber, int type) {
        String key = Const.FORUM_TOPIC_PREVIEW_CACHE + pageNumber + ":" + type;
        List<TopicPreviewVO> list = cacheUtils.takeListFromCache(key, TopicPreviewVO.class);
        if(list != null)
            return list;
        Page<Topic> page = Page.of(pageNumber, 10);
        if(type == 0)
            baseMapper.selectPage(page, Wrappers.<Topic>query()
                    .eq("invisible", 0)
                    .orderByDesc("time"));
        else
            baseMapper.selectPage(page, Wrappers.<Topic>query().eq("type", type)
                    .eq("invisible", 0)
                    .orderByDesc("time"));
        List<Topic> topics = page.getRecords();
        if(topics.isEmpty()) return null;
        List<Integer> userIds = topics.stream()
                .map(Topic::getUid)
                .distinct()
                .toList();
        Map<Integer, Map<String, Object>> userDataMap = this.batchFetchUserData(userIds);
        list = topics.stream()
                .map(topic -> resolveToPreview(topic, userDataMap))
                .toList();
        cacheUtils.saveListToCache(key, list, 60);
        return list;
    }

    @Override
    public List<TopicTopVO> listTopTopics() {
        List<Topic> topics = baseMapper.selectList(Wrappers.<Topic>query()
                .select("id", "title", "time")
                .eq("top", 1));
        return topics.stream().map(topic -> {
            TopicTopVO vo = new TopicTopVO();
            BeanUtils.copyProperties(topic, vo);
            return vo;
        }).toList();
    }

    @Override
    public TopicDetailVO getTopic(int tid, int uid) {
        TopicDetailVO vo = new TopicDetailVO();
        Topic topic = baseMapper.selectById(tid);
        if(topic.getInvisible() == 1 && topic.getUid() != uid) {
            return null;
        }
        BeanUtils.copyProperties(topic, vo);
        TopicDetailVO.Interact interact = new TopicDetailVO.Interact(
                hasInteract(tid, uid, "like"),
                hasInteract(tid, uid, "collect")
        );
        vo.setInteract(interact);
        TopicDetailVO.User user = new TopicDetailVO.User();
        Map<Integer, Map<String, Object>> userDataMap = this.batchFetchUserData(List.of(topic.getUid()));
        vo.setUser(this.fillUserDetailsFromMap(user, topic.getUid(), userDataMap));
        vo.setComments(commentMapper.selectCount(Wrappers.<TopicComment>query().eq("tid", tid)));
        return vo;
    }

    @Override
    public void interact(Interact interact, boolean state) {
        String type = interact.getType();
        synchronized (type.intern()) {
            template.opsForHash().put(type, interact.toKey(), Boolean.toString(state));
            this.saveInteractSchedule(type);
        }
    }

    @Override
    public List<Topic> listTopicByUser(int uid) {
        return baseMapper.selectList(Wrappers.<Topic>query().eq("uid", uid));
    }

    @Override
    public List<TopicSearchVO> searchTopic(String keyword) {
        List<SearchHit<TopicDocument>> list = topicRepository.findByTitleOrIntro(keyword);
        return list.stream().map(item -> {
            TopicSearchVO vo = new TopicSearchVO();
            BeanUtils.copyProperties(item.getContent(), vo);
            vo.setHighlight(item.getHighlightFields());
            return vo;
        }).toList();
    }

    private boolean hasInteract(int tid, int uid, String type) {
        String key = tid + ":" + uid;
        if (template.opsForHash().hasKey(type, key))
            return Boolean.parseBoolean(template.opsForHash().entries(type).get(key).toString());
        return baseMapper.userInteractCount(tid, uid, type) > 0;
    }

    private final Map<String, Boolean> state = new HashMap<>();
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
    private void saveInteractSchedule(String type) {
        if(!state.getOrDefault(type, false)) {
            state.put(type, true);
            service.schedule(() -> {
                this.saveInteract(type);
                state.put(type, false);
            }, 3, TimeUnit.SECONDS);
        }
    }

    private void saveInteract(String type) {
        synchronized (type.intern()) {
            List<Interact> check = new LinkedList<>();
            List<Interact> uncheck = new LinkedList<>();
            template.opsForHash().entries(type).forEach((k, v) -> {
                if(Boolean.parseBoolean(v.toString()))
                    check.add(Interact.parseInteract(k.toString(), type));
                else
                    uncheck.add(Interact.parseInteract(k.toString(), type));
            });
            if(!check.isEmpty())
                baseMapper.addInteract(check, type);
            if(!uncheck.isEmpty())
                baseMapper.deleteInteract(uncheck, type);
            template.delete(type);
        }
    }

    /**
     * Resolve username via user-service Feign call.
     */
    private String resolveUsername(int uid) {
        try {
            var resp = userInternalClient.getUserById(internalToken, uid);
            if (resp.code() == 200 && resp.data() != null) {
                Object username = resp.data().get("username");
                if (username != null) {
                    return username.toString();
                }
            }
        } catch (Exception ignored) {
            // Fallback: return uid as string if user-service unavailable
        }
        return "用户" + uid;
    }

    /**
     * Batch-fetch user data from user-service via Feign.
     */
    private Map<Integer, Map<String, Object>> batchFetchUserData(List<Integer> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        try {
            var resp = userInternalClient.batchQuery(internalToken, Map.of("ids", userIds));
            if (resp.code() == 200 && resp.data() != null) {
                return resp.data();
            }
        } catch (Exception ignored) {
            // Fallback: empty map, user fields will be null
        }
        return Map.of();
    }

    /**
     * Fill user details from a pre-fetched user data map.
     */
    private <T> T fillUserDetailsFromMap(T target, int uid, Map<Integer, Map<String, Object>> userDataMap) {
        Map<String, Object> userData = userDataMap.get(uid);
        if (userData != null) {
            BeanUtils.copyProperties(userData, target);
        }
        return target;
    }

    private TopicPreviewVO resolveToPreview(Topic topic, Map<Integer, Map<String, Object>> userDataMap) {
        TopicPreviewVO vo = new TopicPreviewVO();
        Map<String, Object> userData = userDataMap.get(topic.getUid());
        if (userData != null) {
            Object username = userData.get("username");
            if (username != null) vo.setUsername(username.toString());
            Object avatar = userData.get("avatar");
            if (avatar != null) vo.setAvatar(avatar.toString());
        }
        BeanUtils.copyProperties(topic, vo);
        vo.setLike(baseMapper.interactCount(topic.getId(), "like"));
        vo.setCollect(baseMapper.interactCount(topic.getId(), "collect"));
        List<String> images = new ArrayList<>();
        StringBuilder previewText = new StringBuilder();
        if (topic.getContent() != null) {
            JSONArray ops = JSONObject.parseObject(topic.getContent()).getJSONArray("ops");
            this.shortContent(ops, previewText, obj -> images.add(obj.toString()));
        }
        vo.setText(previewText.length() > 300 ? previewText.substring(0, 300) : previewText.toString());
        vo.setImages(images);
        return vo;
    }

    private void shortContent(JSONArray ops, StringBuilder previewText, Consumer<Object> imageHandler){
        for (Object op : ops) {
            Object insert = JSONObject.from(op).get("insert");
            if(insert instanceof String text) {
                if(previewText.length() >= 300) continue;
                previewText.append(text);
            } else if(insert instanceof Map<?, ?> map) {
                Optional.ofNullable(map.get("image")).ifPresent(imageHandler);
            }
        }
    }

    private boolean textLimitCheck(JSONObject object, int max) {
        if(object == null) return false;
        long length = 0;
        for (Object op : object.getJSONArray("ops")) {
            length += JSONObject.from(op).getString("insert").length();
            if(length > max) return false;
        }
        return true;
    }

    // ==================== ES sync ====================

    @Override
    public void syncTopicToEs(Topic topic) {
        TopicDocument doc = new TopicDocument();
        BeanUtils.copyProperties(topic, doc);
        doc.setTop(topic.getTop() != null && topic.getTop() == 1);
        doc.setLocked(topic.getLocked() != null && topic.getLocked() == 1);
        doc.setInvisible(topic.getInvisible() != null && topic.getInvisible() == 1);
        topicRepository.save(doc);
    }

    @Override
    public void deleteTopicFromEs(int tid) {
        topicRepository.deleteById(tid);
    }

    @Override
    public void syncAllTopicsToEs() {
        List<Topic> topics = baseMapper.selectList(null);
        for (Topic topic : topics) {
            if (topic.getInvisible() == null || topic.getInvisible() == 0) {
                syncTopicToEs(topic);
            }
        }
    }
}
