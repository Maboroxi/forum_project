package com.example.oss.controller;

import com.example.common.entity.RestBean;
import com.example.oss.utils.Const;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    private static final long MAX_FILE_SIZE = 1024 * 1024;
    private static final int MAX_CONTENT_LENGTH = 50000;

    @PostMapping("/text")
    public RestBean<Map<String, Object>> uploadTextFile(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(Const.ATTR_USER_ID) int userId) throws IOException {

        if (file.getSize() > MAX_FILE_SIZE) {
            return RestBean.failure(400, "文本文件不能超过1MB");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return RestBean.failure(400, "不支持的文件格式: ." + ext);
        }

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.length() > MAX_CONTENT_LENGTH) {
            content = content.substring(0, MAX_CONTENT_LENGTH)
                    + "\n\n... (文件过长，仅显示前 " + MAX_CONTENT_LENGTH + " 字符)";
        }

        String objectName = "/chat/" + userId + "/" + format.format(new Date()) + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + "." + ext;
        boolean stored = true;
        try {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket("study")
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .object(objectName)
                    .build();
            client.putObject(args);
        } catch (Exception e) {
            log.error("文本文件存储到 MinIO 失败: " + e.getMessage(), e);
            stored = false;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("filename", originalFilename);
        result.put("size", content.length());
        result.put("fileKey", stored ? objectName : null);

        return RestBean.success(result);
    }
}
