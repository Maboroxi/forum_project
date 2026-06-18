package com.example.ai.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WebSearchTools {

    @Value("${spring.ai.tavily.api-key:}")
    private String apiKey;

    @Resource
    private RestTemplate restTemplate;

    @Tool(description = "搜索互联网上的最新信息，最多返回5条结果。")
    public String web_search(
            @ToolParam(description = "搜索关键词，可以是单个词或短语") String keyword) {
        if (apiKey == null || apiKey.isBlank()) {
            return "网络搜索功能未配置。";
        }
        try {
            JSONObject request = new JSONObject();
            request.put("api_key", apiKey);
            request.put("query", keyword);
            request.put("max_results", 5);
            request.put("include_answer", false);
            JSONObject response = restTemplate.postForObject(
                    "https://api.tavily.com/search", request, JSONObject.class);
            JSONArray results = response == null ? null : response.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                return "未找到相关网络搜索结果。";
            }
            StringBuilder text = new StringBuilder("以下是「").append(keyword).append("」的搜索结果：\n\n");
            for (int i = 0; i < Math.min(results.size(), 5); i++) {
                JSONObject item = results.getJSONObject(i);
                text.append(i + 1).append(". ").append(item.getString("title")).append("\n")
                        .append("   ").append(item.getString("content")).append("\n")
                        .append("   来源：").append(item.getString("url")).append("\n\n");
            }
            return limit(text.toString(), 12000);
        } catch (Exception e) {
            return "网络搜索暂时不可用。";
        }
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
