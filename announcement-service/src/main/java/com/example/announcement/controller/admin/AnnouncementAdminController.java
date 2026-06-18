package com.example.announcement.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.announcement.entity.vo.request.AnnouncementCreateVO;
import com.example.announcement.entity.vo.request.AnnouncementPublishVO;
import com.example.announcement.entity.vo.request.AnnouncementTopVO;
import com.example.announcement.entity.vo.request.AnnouncementUpdateVO;
import com.example.announcement.entity.vo.response.AnnouncementAdminVO;
import com.example.announcement.service.AnnouncementService;
import com.example.common.constants.RequestAttributes;
import com.example.common.entity.PageRestBean;
import com.example.common.entity.RestBean;
import com.example.observability.AuditLogger;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/announcement")
public class AnnouncementAdminController {

    @Resource
    AnnouncementService service;

    @GetMapping("/list")
    public PageRestBean<AnnouncementAdminVO> list(@RequestParam @Min(1) int page,
                                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Boolean published) {
        Page<AnnouncementAdminVO> result = service.listAll(page, size, keyword, published);
        return PageRestBean.success(result.getRecords(), result.getTotal(), result.getCurrent());
    }

    @PostMapping("/create")
    public RestBean<Integer> create(@RequestBody @Valid AnnouncementCreateVO vo,
                                    @RequestAttribute(RequestAttributes.USER_ID) int uid) {
        int id = service.create(uid, vo);
        AuditLogger.success("create", "announcement", id);
        return RestBean.success(id);
    }

    @PostMapping("/update")
    public RestBean<Void> update(@RequestBody @Valid AnnouncementUpdateVO vo) {
        RestBean<Void> result = messageHandle(service.update(vo));
        if (result.code() == 200) AuditLogger.success("update", "announcement", vo.getId());
        return result;
    }

    @PostMapping("/publish")
    public RestBean<Void> publish(@RequestBody @Valid AnnouncementPublishVO vo) {
        RestBean<Void> result = messageHandle(service.publish(vo.getId(), vo.getPublished()));
        if (result.code() == 200) AuditLogger.success("publish", "announcement", vo.getId());
        return result;
    }

    @PostMapping("/top")
    public RestBean<Void> top(@RequestBody @Valid AnnouncementTopVO vo) {
        RestBean<Void> result = messageHandle(service.top(vo.getId(), vo.getTop()));
        if (result.code() == 200) AuditLogger.success("top", "announcement", vo.getId());
        return result;
    }

    @GetMapping("/delete")
    public RestBean<Void> delete(@RequestParam @Min(1) int id) {
        service.delete(id);
        AuditLogger.success("delete", "announcement", id);
        return RestBean.success();
    }

    private RestBean<Void> messageHandle(String message) {
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
}
