package com.example.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * AI 图片理解与生成工具，全部通过 SiliconFlow API 完成。
 * - 图片理解：Qwen/Qwen3-VL-8B-Thinking（chat completions 接口）
 * - 图片生成：Tongyi-MAI/Z-Image-Turbo（images generations 接口）
 * 以 @Tool 方式注册给 AI 调用。
 */
@Component
public class ImageTools {

    @Value("${spring.ai.siliconflow.api-key:}")
    String siliconflowApiKey;

    @Resource
    RestTemplate restTemplate;

    // ==================== SiliconFlow 认证（Bearer Token） ====================

    private HttpEntity<String> buildRequest(JSONObject body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + siliconflowApiKey);
        return new HttpEntity<>(body.toJSONString(), headers);
    }

    private boolean isKeyMissing() {
        return siliconflowApiKey == null || siliconflowApiKey.isEmpty() || "placeholder".equals(siliconflowApiKey);
    }

    // ==================== 通用工具 ====================

    private String detectMimeType(String url) {
        String lower = url.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        else if (lower.endsWith(".gif")) return "image/gif";
        else if (lower.endsWith(".webp")) return "image/webp";
        else if (lower.endsWith(".bmp")) return "image/bmp";
        else return "image/png";
    }

    /**
     * 下载图片并转为 base64 data URI
     */
    private String downloadImageAsBase64(String imageUrl) {
        ResponseEntity<byte[]> imageResponse = restTemplate.exchange(
                imageUrl, HttpMethod.GET, null, byte[].class);
        if (imageResponse.getStatusCode() != HttpStatus.OK || imageResponse.getBody() == null) {
            throw new RuntimeException("无法获取图片，HTTP 状态码: " + imageResponse.getStatusCode());
        }
        String mimeType = detectMimeType(imageUrl);
        String base64 = Base64.getEncoder().encodeToString(imageResponse.getBody());
        return "data:" + mimeType + ";base64," + base64;
    }

    // ==================== @Tool 方法 ====================

    @Tool(description = "识别/理解图片内容。当用户上传了图片并询问图片内容、要求描述图片或分析图片时调用此工具。"
            + "参数 imageUrl 是图片的完整 URL 地址，question 是用户对图片的具体问题。")
    public String recognize_image(
            @ToolParam(description = "图片的完整 URL 地址，例如 http://localhost:8080/images/xxx.png") String imageUrl,
            @ToolParam(description = "对图片的具体问题，例如'这张图片里有什么''请详细描述这张图片''图片中的文字是什么'等。如果不确定用户的问题，直接使用'请详细描述这张图片'") String question) {
        if (isKeyMissing()) {
            return "图片识别功能未配置，请联系管理员设置 SiliconFlow API Key。";
        }
        try {
            // 1. 下载图片转 base64（本地 MinIO 图片外部 API 无法直接访问）
            String dataUri = downloadImageAsBase64(imageUrl);

            // 2. 构建 SiliconFlow 多模态识别请求
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "Qwen/Qwen3-VL-8B-Thinking");

            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");

            JSONArray content = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("type", "text");
            textPart.put("text", question != null && !question.isEmpty() ? question : "请详细描述这张图片的内容");
            content.add(textPart);

            JSONObject imagePart = new JSONObject();
            imagePart.put("type", "image_url");
            JSONObject imageUrlObj = new JSONObject();
            imageUrlObj.put("url", dataUri);
            imagePart.put("image_url", imageUrlObj);
            content.add(imagePart);

            userMessage.put("content", content);
            messages.add(userMessage);
            requestBody.put("messages", messages);

            // 3. 调用 SiliconFlow API
            JSONObject response = restTemplate.postForObject(
                    "https://api.siliconflow.cn/v1/chat/completions",
                    buildRequest(requestBody),
                    JSONObject.class);

            if (response != null && response.getJSONArray("choices") != null) {
                JSONArray choices = response.getJSONArray("choices");
                if (!choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    if (message != null) {
                        return message.getString("content");
                    }
                }
            }
            return "图片识别失败，模型无法分析该图片内容。";
        } catch (Exception e) {
            return "图片识别时发生错误：" + e.getMessage();
        }
    }

    @Tool(description = "根据文字描述生成图片。当用户要求'画一张图''生成图片''创作一幅画''绘制图像''给我画一个'时调用此工具。"
            + "参数 prompt 是对图片的详细文字描述。")
    public String generate_image(
            @ToolParam(description = "图片描述，尽可能详细，包括主体内容、风格（写实/卡通/油画/水彩）、色彩、构图等。"
                    + "例如：'一只橘色小猫坐在窗台上晒太阳，背后有绿色植物，水彩风格'") String prompt) {
        if (isKeyMissing()) {
            return "图片生成功能未配置，请联系管理员设置 SiliconFlow API Key。";
        }
        try {
            // 1. 构建 SiliconFlow 图片生成请求
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "Tongyi-MAI/Z-Image-Turbo");
            requestBody.put("prompt", prompt);
            requestBody.put("image_size", "1024x1024");

            // 2. 调用 SiliconFlow 图片生成 API
            JSONObject response = restTemplate.postForObject(
                    "https://api.siliconflow.cn/v1/images/generations",
                    buildRequest(requestBody),
                    JSONObject.class);

            // 3. SiliconFlow 返回格式为 { "images": [{ "url": "..." }] }
            if (response != null && response.getJSONArray("images") != null) {
                JSONArray images = response.getJSONArray("images");
                if (!images.isEmpty()) {
                    JSONObject first = images.getJSONObject(0);
                    String imageUrl = first.getString("url");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        return "![生成的图片](" + imageUrl + ")\n\n已根据描述「" + prompt + "」生成图片。\n图片地址：" + imageUrl;
                    }
                }
            }
            return "图片生成失败，请稍后重试或修改图片描述。";
        } catch (Exception e) {
            return "图片生成时发生错误：" + e.getMessage();
        }
    }
}
