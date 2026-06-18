package com.example.user.controller.admin;

import com.example.common.entity.PageRestBean;
import com.example.common.entity.RestBean;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.user.entity.dto.EmailRecord;
import com.example.user.service.EmailService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/email")
public class EmailAdminController {

    @Resource
    EmailService service;

    @GetMapping("/list")
    public PageRestBean<EmailRecord> listEmailRecord(@RequestParam int page,
                                                     @RequestParam int size) {
        Page<EmailRecord> result = service.listEmailRecord(page, size);
        return PageRestBean.success(result.getRecords(), result.getTotal(), result.getCurrent());
    }

    @GetMapping("/resend")
    public RestBean<Void> resendEmailRecord(@RequestParam int id) {
        if (service.resendEmailRecord(id)) {
            return RestBean.success();
        } else {
            return RestBean.failure(400, "邮件重发失败");
        }
    }
}
