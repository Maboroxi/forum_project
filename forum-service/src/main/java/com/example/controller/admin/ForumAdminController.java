package com.example.controller.admin;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.common.entity.PageRestBean;
import com.example.common.entity.RestBean;
import com.example.observability.AuditLogger;
import com.example.entity.vo.request.TopicTypeCreateVO;
import com.example.entity.vo.response.TopicPreviewVO;
import com.example.entity.vo.response.TopicTypeVO;
import com.example.service.TopicService;
import com.example.utils.ProhibitedUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/forum")
public class ForumAdminController {

    @Resource
    private TopicService service;

    @Resource
    private ProhibitedUtils prohibitedUtils;

    @GetMapping("/list")
    public PageRestBean<TopicPreviewVO> list(@RequestParam int page,
                                             @RequestParam int size,
                                             @RequestParam(required = false) String keyword) {
        JSONObject result = service.listAllTopicByPage(page, size, keyword);
        return PageRestBean.success(
                result.getJSONArray("list").toList(TopicPreviewVO.class),
                result.getIntValue("total"),
                page
        );
    }

    @GetMapping("/delete")
    public RestBean<Void> delete(@RequestParam int tid){
        service.deleteTopic(tid);
        AuditLogger.success("delete", "topic", tid);
        return RestBean.success();
    }

    @PostMapping("/top")
    public RestBean<Void> setTop(@RequestBody JSONObject object) {
        service.setTopicTop(
                object.getIntValue("tid"),
                object.getBooleanValue("status")
        );
        AuditLogger.success("top", "topic", object.getIntValue("tid"));
        return RestBean.success();
    }

    @PostMapping("/locked")
    public RestBean<Void> setLocked(@RequestBody JSONObject object) {
        service.setTopicLocked(
                object.getIntValue("tid"),
                object.getBooleanValue("status")
        );
        AuditLogger.success("lock", "topic", object.getIntValue("tid"));
        return RestBean.success();
    }

    @PostMapping("/invisible")
    public RestBean<Void> setInvisible(@RequestBody JSONObject object) {
        service.setTopicInvisible(
                object.getIntValue("tid"),
                object.getBooleanValue("status")
        );
        AuditLogger.success("visibility", "topic", object.getIntValue("tid"));
        return RestBean.success();
    }

    @GetMapping("/prohibited-list")
    public RestBean<List<String>> getProhibitedList() {
        return RestBean.success(prohibitedUtils.getProhibitedWords());
    }

    @PostMapping("/prohibited-save")
    public RestBean<Void> saveProhibitedList(@RequestBody JSONArray array) {
        prohibitedUtils.setProhibitedWords(array.toList(String.class));
        AuditLogger.success("update", "prohibited-words", "all");
        return RestBean.success();
    }

    @PostMapping("/update-type")
    public RestBean<Void> updateType(@RequestBody TopicTypeVO vo) {
        service.updateTopicType(vo);
        AuditLogger.success("update", "topic-type", vo.getId());
        return RestBean.success();
    }

    @GetMapping("/delete-type")
    public RestBean<Void> deleteType(@RequestParam int tid) {
        service.deleteTopicType(tid);
        AuditLogger.success("delete", "topic-type", tid);
        return RestBean.success();
    }

    @PostMapping("/create-type")
    public RestBean<Void> createType(@RequestBody TopicTypeCreateVO vo) {
        service.createTopicType(vo);
        AuditLogger.success("create", "topic-type", vo.getName());
        return RestBean.success();
    }

    @GetMapping("/change-topic-type")
    public RestBean<Void> changeTopicType(@RequestParam int tid,
                                          @RequestParam int type) {
        service.changeTopicType(tid, type);
        AuditLogger.success("change-type", "topic", tid);
        return RestBean.success();
    }

    @GetMapping("/sync-to-es")
    public RestBean<Void> syncToEs() {
        service.syncAllTopicsToEs();
        AuditLogger.success("sync", "elasticsearch-topic-index", "all");
        return RestBean.success();
    }
}
