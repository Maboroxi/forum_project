package com.example.oss.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.entity.RestBean;
import com.example.oss.entity.dto.StoreImage;
import com.example.oss.mapper.ImageStoreMapper;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class InternalObjectController {

    private static final long MAX_TEXT_BYTES = 1024 * 1024;
    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_TEXT_LENGTH = 50000;

    @Resource
    private MinioClient minioClient;

    @Resource
    private ImageStoreMapper imageStoreMapper;

    @GetMapping("/file/text")
    public RestBean<Map<String, Object>> readText(
            @RequestParam String key,
            @RequestParam int userId) {
        String requiredPrefix = "/chat/" + userId + "/";
        if (!validKey(key, requiredPrefix)) {
            return RestBean.forbidden("无权读取该附件");
        }
        try {
            byte[] bytes = readObject(key, MAX_TEXT_BYTES);
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (content.length() > MAX_TEXT_LENGTH) {
                content = content.substring(0, MAX_TEXT_LENGTH);
            }
            String filename = key.substring(key.lastIndexOf('/') + 1);
            return RestBean.success(Map.of(
                    "key", key,
                    "filename", filename,
                    "content", content,
                    "size", content.length()));
        } catch (IllegalArgumentException e) {
            return RestBean.failure(400, e.getMessage());
        } catch (Exception e) {
            return RestBean.failure(404, "附件不存在");
        }
    }

    @GetMapping("/image/content")
    public RestBean<Map<String, Object>> readImage(
            @RequestParam String key,
            @RequestParam int userId) {
        if (!validKey(key, "/cache/") || !ownsImage(userId, key)) {
            return RestBean.forbidden("无权读取该图片");
        }
        try {
            byte[] bytes = readObject(key, MAX_IMAGE_BYTES);
            String mediaType = detectImageType(bytes);
            if (mediaType == null) {
                return RestBean.failure(400, "不支持的图片格式");
            }
            return RestBean.success(Map.of(
                    "key", key,
                    "mediaType", mediaType,
                    "base64", Base64.getEncoder().encodeToString(bytes),
                    "size", bytes.length));
        } catch (IllegalArgumentException e) {
            return RestBean.failure(400, e.getMessage());
        } catch (Exception e) {
            return RestBean.failure(404, "图片不存在");
        }
    }

    private boolean ownsImage(int userId, String key) {
        return imageStoreMapper.selectCount(new LambdaQueryWrapper<StoreImage>()
                .eq(StoreImage::getUid, userId)
                .eq(StoreImage::getName, key)) > 0;
    }

    private byte[] readObject(String key, long maxBytes) throws Exception {
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder().bucket("study").object(key).build());
        if (stat.size() > maxBytes) {
            throw new IllegalArgumentException("对象大小超过限制");
        }
        try (GetObjectResponse input = minioClient.getObject(
                GetObjectArgs.builder().bucket("study").object(key).build());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            if (output.size() > maxBytes) {
                throw new IllegalArgumentException("对象大小超过限制");
            }
            return output.toByteArray();
        }
    }

    private boolean validKey(String key, String prefix) {
        return key != null && key.startsWith(prefix)
                && !key.contains("..") && !key.contains("\\")
                && !key.contains("?") && !key.contains("#");
    }

    private String detectImageType(byte[] bytes) {
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes.length >= 3
                && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 6) {
            String header = new String(bytes, 0, 6, StandardCharsets.US_ASCII);
            if ("GIF87a".equals(header) || "GIF89a".equals(header)) {
                return "image/gif";
            }
        }
        if (bytes.length >= 12
                && "RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII))) {
            return "image/webp";
        }
        return null;
    }
}
