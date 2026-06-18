package com.example.controller;

import com.example.client.UserInternalClient;
import com.example.common.entity.RestBean;
import com.example.entity.dto.Interact;
import com.example.entity.dto.Topic;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicDraftSaveVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.*;
import com.example.service.TopicDraftService;
import com.example.service.TopicService;
import com.example.service.WeatherService;
import com.example.utils.Const;
import com.example.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    @Resource
    WeatherService service;

    @Resource
    TopicService topicService;

    @Resource
    TopicDraftService topicDraftService;

    @Resource
    ControllerUtils utils;

    @Resource
    UserInternalClient userInternalClient;

    @Value("${internal.service.token}")
    private String internalToken;

    @GetMapping("/weather")
    public RestBean<WeatherVO> weather(double longitude, double latitude){
        WeatherVO vo = service.fetchWeather(longitude, latitude);
        return vo == null ?
                RestBean.failure(400, "获取地理位置信息与天气失败，请联系管理员！") : RestBean.success(vo);
    }

    @GetMapping("/types")
    public RestBean<List<TopicTypeVO>> listTypes(){
        return RestBean.success(topicService
                .listTypes()
                .stream()
                .map(type -> type.asViewObject(TopicTypeVO.class))
                .toList());
    }

    @PostMapping("/create-topic")
    public RestBean<Void> createTopic(@Valid @RequestBody TopicCreateVO vo,
                                      @RequestAttribute(Const.ATTR_USER_ID) int id) {
        RestBean<Map<String, Boolean>> statusResp = userInternalClient.getUserStatus(internalToken, id);
        if (statusResp.code() == 200 && statusResp.data() != null
                && Boolean.TRUE.equals(statusResp.data().get("mute"))) {
            return RestBean.forbidden("您已被禁言，无法创建新的主题");
        }
        return utils.messageHandle(() -> topicService.createTopic(id, vo));
    }

    @GetMapping("/topic-draft/list")
    public RestBean<List<TopicDraftVO>> listTopicDrafts(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(topicDraftService.listDrafts(id));
    }

    @GetMapping("/topic-draft/detail")
    public RestBean<TopicDraftVO> topicDraft(@RequestParam @Min(1) int id,
                                             @RequestAttribute(Const.ATTR_USER_ID) int userId) {
        TopicDraftVO draft = topicDraftService.getDraft(userId, id);
        return draft == null ? RestBean.failure(404, "草稿不存在") : RestBean.success(draft);
    }

    @PostMapping("/topic-draft/save")
    public RestBean<TopicDraftVO> saveTopicDraft(@Valid @RequestBody TopicDraftSaveVO vo,
                                                 @RequestAttribute(Const.ATTR_USER_ID) int id) {
        String message = topicDraftService.saveDraft(id, vo);
        return message == null ?
                RestBean.success(topicDraftService.getDraft(id, vo.getId())) :
                RestBean.failure(400, message);
    }

    @GetMapping("/topic-draft/delete")
    public RestBean<Void> deleteTopicDraft(@RequestParam @Min(1) int id,
                                           @RequestAttribute(Const.ATTR_USER_ID) int userId) {
        topicDraftService.deleteDraft(userId, id);
        return RestBean.success();
    }

    @GetMapping("/list-topic")
    public RestBean<List<TopicPreviewVO>> listTopic(@RequestParam @Min(0) int page,
                                                    @RequestParam @Min(0) int type) {
        return RestBean.success(topicService.listTopicByPage(page + 1, type));
    }

    @GetMapping("/top-topic")
    public RestBean<List<TopicTopVO>> topTopic(){
        return RestBean.success(topicService.listTopTopics());
    }

    @GetMapping("/topic")
    public RestBean<TopicDetailVO> topic(@RequestParam @Min(0) int tid,
                                         @RequestAttribute(Const.ATTR_USER_ID) int id){
        TopicDetailVO topic = topicService.getTopic(tid, id);
        if(topic != null) {
            return RestBean.success(topic);
        } else {
            return RestBean.failure(404, "帖子不存在或已被屏蔽");
        }
    }

    @GetMapping("/interact")
    public RestBean<Void> interact(@RequestParam @Min(0) int tid,
                                   @RequestParam @Pattern(regexp = "(like|collect)") String type,
                                   @RequestParam boolean state,
                                   @RequestAttribute(Const.ATTR_USER_ID) int id) {
        topicService.interact(new Interact(tid, id, new Date(), type), state);
        return RestBean.success();
    }

    @GetMapping("/collects")
    public RestBean<List<TopicPreviewVO>> collects(@RequestAttribute(Const.ATTR_USER_ID) int id){
        return RestBean.success(topicService.listTopicCollects(id));
    }

    @PostMapping("/update-topic")
    public RestBean<Void> updateTopic(@Valid @RequestBody TopicUpdateVO vo,
                                      @RequestAttribute(Const.ATTR_USER_ID) int id){
        return utils.messageHandle(() -> topicService.updateTopic(id, vo));
    }

    @PostMapping("/add-comment")
    public RestBean<Void> addComment(@Valid @RequestBody AddCommentVO vo,
                                     @RequestAttribute(Const.ATTR_USER_ID) int id){
        RestBean<Map<String, Boolean>> statusResp = userInternalClient.getUserStatus(internalToken, id);
        if (statusResp.code() == 200 && statusResp.data() != null
                && Boolean.TRUE.equals(statusResp.data().get("mute"))) {
            return RestBean.forbidden("您已被禁言，无法创建新的回复");
        }
        return utils.messageHandle(() -> topicService.createComment(id, vo));
    }

    @GetMapping("/comments")
    public RestBean<List<CommentVO>> comments(@RequestParam @Min(0) int tid,
                                              @RequestParam @Min(0) int page){
        return RestBean.success(topicService.comments(tid, page + 1));
    }

    @GetMapping("/delete-comment")
    public RestBean<Void> deleteComment(@RequestParam @Min(0) int id,
                                        @RequestAttribute(Const.ATTR_USER_ID) int uid){
        topicService.deleteComment(id, uid);
        return RestBean.success();
    }

    @GetMapping("/user-topic")
    public RestBean<List<Topic>> userTopic(@RequestAttribute(Const.ATTR_USER_ID) int uid) {
        return RestBean.success(topicService.listTopicByUser(uid));
    }

    @GetMapping("/delete-topic")
    public RestBean<Void> deleteTopic(@RequestParam @Min(0) int tid,
                                      @RequestAttribute(Const.ATTR_USER_ID) int uid){
        topicService.deleteTopic(tid, uid);
        return RestBean.success();
    }

    @GetMapping("/search-topic")
    public RestBean<List<TopicSearchVO>> searchTopic(@RequestParam String keyword){
        return RestBean.success(topicService.searchTopic(keyword));
    }
}
