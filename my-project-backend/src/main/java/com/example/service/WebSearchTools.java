package com.example.service;

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
    String tavilyApiKey;

    @Resource
    RestTemplate restTemplate;

    @Tool(description = "搜索互联网上的最新信息。当用户询问实时信息、新闻、或你不确定的内容时使用。每次搜索返回最多5条结果。")
    String web_search(@ToolParam(description = "搜索关键词，可以是单个词或短语") String keyword) {
        if (tavilyApiKey.isEmpty() || "your-tavily-api-key-placeholder".equals(tavilyApiKey)) {
            return "网络搜索功能未配置，请联系管理员设置 Tavily API Key。";
        }
        try {
            JSONObject request = new JSONObject();
            request.put("api_key", tavilyApiKey);
            request.put("query", keyword);
            request.put("max_results", 5);
            request.put("include_answer", false);

            JSONObject response = restTemplate.postForObject(
                    "https://api.tavily.com/search",
                    request,
                    JSONObject.class);

            if (response == null || response.getJSONArray("results") == null) {
                return "未找到相关网络搜索结果。";
            }

            JSONArray results = response.getJSONArray("results");
            StringBuilder sb = new StringBuilder();
            sb.append("以下是「").append(keyword).append("」的网络搜索结果：\n\n");
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                sb.append(i + 1).append(". ").append(item.getString("title")).append("\n");
                sb.append("   ").append(item.getString("content")).append("\n");
                sb.append("   来源：").append(item.getString("url")).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "网络搜索时发生错误：" + e.getMessage();
        }
    }
}
