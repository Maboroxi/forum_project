package com.example.oss.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.entity.RestBean;
import com.example.oss.client.UserInternalClient;
import com.example.oss.entity.dto.StoreImage;
import com.example.oss.mapper.ImageStoreMapper;
import com.example.oss.service.ImageService;
import com.example.oss.utils.Const;
import com.example.oss.utils.FlowUtils;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ImageServiceImpl extends ServiceImpl<ImageStoreMapper, StoreImage> implements ImageService {

    @Resource
    MinioClient client;

    @Resource
    UserInternalClient userInternalClient;

    @Resource
    FlowUtils flowUtils;

    @Value("${internal.service.token}")
    private String internalToken;

    private final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

    @Override
    public void fetchImageFromMinio(OutputStream stream, String image) throws Exception {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket("study")
                .object(image)
                .build();
        GetObjectResponse response = client.getObject(args);
        IOUtils.copy(response, stream);
    }

    @Override
    public String uploadImage(MultipartFile file, int id) throws IOException {
        String key = Const.FORUM_IMAGE_COUNTER + id;
        if (!flowUtils.limitPeriodCounterCheck(key, 20, 3600)) {
            return null;
        }
        String imageName = UUID.randomUUID().toString().replace("-", "");
        Date date = new Date();
        imageName = "/cache/" + format.format(date) + "/" + imageName;
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket("study")
                .stream(file.getInputStream(), file.getSize(), -1)
                .object(imageName)
                .build();
        try {
            client.putObject(args);
            if (this.save(new StoreImage(id, imageName, date))) {
                return imageName;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("图片上传出现问题: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String uploadAvatar(MultipartFile file, int id) throws IOException {
        String imageName = UUID.randomUUID().toString().replace("-", "");
        imageName = "/avatar/" + imageName;
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket("study")
                .stream(file.getInputStream(), file.getSize(), -1)
                .object(imageName)
                .build();
        try {
            client.putObject(args);
            RestBean<Map<String, String>> resp = userInternalClient.updateAvatar(
                    internalToken, id, Map.of("avatar", imageName));
            if (resp.code() != 200 || resp.data() == null) {
                log.warn("更新用户头像失败: userId={}, message={}", id, resp.message());
                return null;
            }
            // Delete old avatar from MinIO
            String oldAvatar = resp.data().get("oldAvatar");
            if (oldAvatar != null && !oldAvatar.isEmpty()) {
                this.deleteOldAvatar(oldAvatar);
            }
            return imageName;
        } catch (Exception e) {
            log.error("头像上传出现问题: " + e.getMessage(), e);
            return null;
        }
    }

    private void deleteOldAvatar(String avatar) throws Exception {
        if (avatar == null || avatar.isEmpty()) {
            return;
        }
        RemoveObjectArgs remove = RemoveObjectArgs.builder()
                .bucket("study")
                .object(avatar)
                .build();
        client.removeObject(remove);
    }
}
