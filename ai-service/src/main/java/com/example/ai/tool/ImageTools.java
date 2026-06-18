package com.example.ai.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.ai.client.OssInternalClient;
import com.example.ai.support.AiRequestContext;
import com.example.common.entity.RestBean;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ImageTools {

    @Value("${spring.ai.siliconflow.api-key:}")
    private String apiKey;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private OssInternalClient ossClient;

    @Tool(description = "识别用户已经上传到校园系统的图片。imageKey 必须是系统图片对象键，不能是URL。")
    public String recognize_image(
            @ToolParam(description = "图片对象键，例如 /cache/20260618/abc") String imageKey,
            @ToolParam(description = "用户针对图片提出的问题") String question) {
        if (apiKey == null || apiKey.isBlank()) {
            return "图片识别功能未配置。";
        }
        try {
            RestBean<OssInternalClient.ImageContent> result =
                    ossClient.readImage(imageKey, AiRequestContext.requireUserId());
            if (result.code() != 200 || result.data() == null) {
                return "无法读取该图片。";
            }
            OssInternalClient.ImageContent image = result.data();
            JSONObject body = new JSONObject();
            body.put("model", "Qwen/Qwen3-VL-8B-Thinking");

            JSONArray content = new JSONArray();
            content.add(JSONObject.of(
                    "type", "text",
                    "text", question == null || question.isBlank() ? "请详细描述图片内容" : question));
            content.add(JSONObject.of(
                    "type", "image_url",
                    "image_url", JSONObject.of(
                            "url", "data:" + image.mediaType() + ";base64," + image.base64())));
            body.put("messages", JSONArray.of(JSONObject.of("role", "user", "content", content)));

            JSONObject response = restTemplate.postForObject(
                    "https://api.siliconflow.cn/v1/chat/completions",
                    request(body),
                    JSONObject.class);
            return response.getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content");
        } catch (Exception e) {
            return "图片识别暂时不可用。";
        }
    }

    @Tool(description = "根据文字描述生成图片。")
    public String generate_image(
            @ToolParam(description = "图片的详细文字描述") String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "图片生成功能未配置。";
        }
        try {
            JSONObject body = JSONObject.of(
                    "model", "Tongyi-MAI/Z-Image-Turbo",
                    "prompt", prompt,
                    "image_size", "1024x1024");
            JSONObject response = restTemplate.postForObject(
                    "https://api.siliconflow.cn/v1/images/generations",
                    request(body),
                    JSONObject.class);
            String url = response.getJSONArray("images").getJSONObject(0).getString("url");
            return "![生成的图片](" + url + ")\n\n图片地址：" + url;
        } catch (Exception e) {
            return "图片生成暂时不可用。";
        }
    }

    private HttpEntity<String> request(JSONObject body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return new HttpEntity<>(body.toJSONString(), headers);
    }
}
