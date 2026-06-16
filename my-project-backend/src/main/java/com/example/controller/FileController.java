package com.example.controller;

import com.example.entity.RestBean;
import com.example.utils.Const;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Resource
    MinioClient client;

    private final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "txt", "md", "csv", "json", "xml", "yaml", "yml",
            "log", "properties", "cfg", "conf", "ini",
            "py", "java", "js", "ts", "vue", "html", "css", "scss", "less",
            "sh", "bat", "ps1", "sql", "r", "go", "rs", "kt");

    private static final long MAX_FILE_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_CONTENT_LENGTH = 50000;    // 最多取前50000字符

    @PostMapping("/text")
    public RestBean<Map<String, Object>> uploadTextFile(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(Const.ATTR_USER_ID) int userId) throws IOException {

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return RestBean.failure(400, "文本文件不能超过1MB");
        }

        // 校验扩展名
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return RestBean.failure(400, "不支持的文件格式: ." + ext);
        }

        // 读取文本内容
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.length() > MAX_CONTENT_LENGTH) {
            content = content.substring(0, MAX_CONTENT_LENGTH)
                    + "\n\n... (文件过长，仅显示前 " + MAX_CONTENT_LENGTH + " 字符)";
        }

        // 存入 MinIO 备份
        String objectName = "/chat/" + format.format(new Date()) + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + "." + ext;
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket("study")
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .object(objectName)
                    .build();
            client.putObject(args);
        } catch (Exception e) {
            log.error("文本文件存储到 MinIO 失败: " + e.getMessage(), e);
            // 存储失败不阻塞，仍返回内容
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("filename", originalFilename);
        result.put("size", content.length());

        return RestBean.success(result);
    }
}
