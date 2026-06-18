package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.client.UserInternalClient;
import com.example.entity.dto.Announcement;
import com.example.entity.vo.request.AnnouncementCreateVO;
import com.example.entity.vo.request.AnnouncementUpdateVO;
import com.example.entity.vo.response.AnnouncementAdminVO;
import com.example.entity.vo.response.AnnouncementDetailVO;
import com.example.entity.vo.response.AnnouncementPreviewVO;
import com.example.mapper.AnnouncementMapper;
import com.example.service.AnnouncementService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    @Resource
    UserInternalClient userInternalClient;

    @Value("${internal.service.token}")
    private String internalToken;

    @Override
    public List<AnnouncementPreviewVO> latest(int limit) {
        int size = Math.max(1, Math.min(limit, 10));
        Page<Announcement> page = Page.of(1, size);
        baseMapper.selectPage(page, publishedQuery());
        return page.getRecords().stream().map(this::preview).toList();
    }

    @Override
    public Page<AnnouncementPreviewVO> listPublished(int page, int size) {
        Page<Announcement> result = baseMapper.selectPage(Page.of(page, size), publishedQuery());
        return convertPage(result, this::preview);
    }

    @Override
    public AnnouncementDetailVO detail(int id) {
        Announcement announcement = baseMapper.selectOne(Wrappers.<Announcement>query()
                .eq("id", id)
                .eq("published", 1));
        return announcement == null ? null : announcement.asViewObject(AnnouncementDetailVO.class);
    }

    @Override
    public Page<AnnouncementAdminVO> listAll(int page, int size, String keyword, Boolean published) {
        QueryWrapper<Announcement> query = Wrappers.<Announcement>query()
                .eq(published != null, "published", published)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like("title", keyword)
                        .or()
                        .like("summary", keyword))
                .orderByDesc("top")
                .orderByDesc("create_time")
                .orderByDesc("id");
        Page<Announcement> result = baseMapper.selectPage(Page.of(page, size), query);
        // Batch query user names from user-service
        List<Integer> userIds = result.getRecords().stream()
                .map(Announcement::getUid)
                .distinct()
                .toList();
        Map<Integer, String> usernameMap = resolveUsernames(userIds);
        return convertPage(result, announcement -> admin(announcement, usernameMap));
    }

    private Map<Integer, String> resolveUsernames(List<Integer> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        try {
            var resp = userInternalClient.batchQuery(internalToken, Map.of("ids", userIds));
            if (resp.code() == 200 && resp.data() != null) {
                Map<Integer, String> result = new HashMap<>();
                resp.data().forEach((id, userData) -> {
                    Object username = userData.get("username");
                    if (username != null) {
                        result.put(id, username.toString());
                    }
                });
                return result;
            }
        } catch (Exception e) {
            // Fallback: return empty map if user-service is unavailable
        }
        return Map.of();
    }

    @Override
    public int create(int uid, AnnouncementCreateVO vo) {
        Announcement announcement = new Announcement();
        BeanUtils.copyProperties(vo, announcement);
        announcement.setContent(vo.getContent().toJSONString());
        announcement.setUid(uid);
        announcement.setPublished(false);
        announcement.setTop(false);
        Date now = new Date();
        announcement.setCreateTime(now);
        announcement.setUpdateTime(now);
        baseMapper.insert(announcement);
        return announcement.getId();
    }

    @Override
    public String update(AnnouncementUpdateVO vo) {
        Announcement announcement = baseMapper.selectById(vo.getId());
        if(announcement == null)
            return "公告不存在";
        announcement.setTitle(vo.getTitle());
        announcement.setSummary(vo.getSummary());
        announcement.setContent(vo.getContent().toJSONString());
        announcement.setUpdateTime(new Date());
        baseMapper.updateById(announcement);
        return null;
    }

    @Override
    public String publish(int id, boolean published) {
        Announcement announcement = baseMapper.selectById(id);
        if(announcement == null)
            return "公告不存在";
        if(!Boolean.TRUE.equals(announcement.getPublished()) && published)
            announcement.setPublishTime(new Date());
        announcement.setPublished(published);
        announcement.setUpdateTime(new Date());
        baseMapper.updateById(announcement);
        return null;
    }

    @Override
    public String top(int id, boolean top) {
        Announcement announcement = baseMapper.selectById(id);
        if(announcement == null)
            return "公告不存在";
        announcement.setTop(top);
        announcement.setUpdateTime(new Date());
        baseMapper.updateById(announcement);
        return null;
    }

    @Override
    public void delete(int id) {
        baseMapper.deleteById(id);
    }

    private QueryWrapper<Announcement> publishedQuery() {
        return Wrappers.<Announcement>query()
                .eq("published", 1)
                .orderByDesc("top")
                .orderByDesc("publish_time")
                .orderByDesc("id");
    }

    private AnnouncementPreviewVO preview(Announcement announcement) {
        return announcement.asViewObject(AnnouncementPreviewVO.class);
    }

    private AnnouncementAdminVO admin(Announcement announcement, Map<Integer, String> usernameMap) {
        AnnouncementAdminVO vo = announcement.asViewObject(AnnouncementAdminVO.class);
        String username = usernameMap.get(announcement.getUid());
        if (username != null) {
            vo.setUsername(username);
        }
        return vo;
    }

    private <T> Page<T> convertPage(Page<Announcement> source, Function<Announcement, T> converter) {
        Page<T> target = Page.of(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(source.getRecords().stream().map(converter).toList());
        return target;
    }
}
