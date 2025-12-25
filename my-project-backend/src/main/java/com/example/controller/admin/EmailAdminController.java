package com.example.controller.admin;

import com.example.entity.RestBean;
import com.example.entity.dto.EmailRecord;
import com.example.service.EmailService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/email")
public class EmailAdminController {

    @Resource
    EmailService service;


    @GetMapping("/list")
    public RestBean<List<EmailRecord>> listEmailRecord(){
        return RestBean.success(service.listEmailRecord());
    }
}
