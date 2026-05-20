package com.greate.community.controller;

import com.greate.community.service.BehaviorEventClickHouseService;
import com.greate.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户行为分析接口
 *
 * 当前用于开发环境验证 ClickHouse 分析查询能力。
 * 后续可以接入后台运营看板。
 */
@Profile("develop")
@RestController
@RequestMapping("/dev/analytics")
public class BehaviorAnalyticsController {

    @Autowired
    private BehaviorEventClickHouseService clickHouseService;

    /**
     * 行为类型统计
     */
    @GetMapping("/event-types")
    public String eventTypes() {
        List<Map<String, Object>> data = clickHouseService.queryEventTypeStats();
        return CommunityUtil.getJSONString(0, "查询成功", data);
    }

    /**
     * 热门帖子排行
     */
    @GetMapping("/hot-posts")
    public String hotPosts(@RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> data = clickHouseService.queryHotPosts(limit);
        return CommunityUtil.getJSONString(0, "查询成功", data);
    }

    /**
     * 搜索关键词排行
     */
    @GetMapping("/search-keywords")
    public String searchKeywords(@RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> data = clickHouseService.querySearchKeywords(limit);
        return CommunityUtil.getJSONString(0, "查询成功", data);
    }

    /**
     * DAU 统计
     */
    @GetMapping("/dau")
    public String dau() {
        List<Map<String, Object>> data = clickHouseService.queryDau();
        return CommunityUtil.getJSONString(0, "查询成功", data);
    }

    /**
     * 每日行为趋势
     */
    @GetMapping("/daily-trend")
    public String dailyTrend() {
        List<Map<String, Object>> data = clickHouseService.queryDailyEventTrend();
        return CommunityUtil.getJSONString(0, "查询成功", data);
    }

    @PostMapping("/clear")
    public String clear() {
        clickHouseService.clearBehaviorEvents();
        return CommunityUtil.getJSONString(0, "行为数据已清空");
    }

}
