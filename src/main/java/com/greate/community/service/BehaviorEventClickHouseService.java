package com.greate.community.service;

import com.alibaba.fastjson.JSONObject;
import com.greate.community.entity.BehaviorEvent;
import com.greate.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Service
public class BehaviorEventClickHouseService {

    @Value("${clickhouse.datasource.url}")
    private String url;

    @Value("${clickhouse.datasource.username}")
    private String username;

    @Value("${clickhouse.datasource.password}")
    private String password;

    @Value("${clickhouse.datasource.driver-class-name}")
    private String driverClassName;

    private JdbcTemplate clickHouseJdbcTemplate;

    private static final Logger logger = LoggerFactory.getLogger(BehaviorEventClickHouseService.class);

    @Value("${clickhouse.sink.batch-size:200}")
    private int batchSize;

    @Value("${clickhouse.sink.flush-interval-ms:3000}")
    private long flushIntervalMs;

    @Value("${clickhouse.sink.buffer-capacity:10000}")
    private int bufferCapacity;

    private LinkedBlockingQueue<BehaviorEvent> buffer;

    private ScheduledExecutorService flushExecutor;

    private final Object flushLock = new Object();

    @PostConstruct
    public void init() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);

        this.clickHouseJdbcTemplate = new JdbcTemplate(dataSource);

        this.buffer = new LinkedBlockingQueue<>(bufferCapacity);

        this.flushExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("clickhouse-behavior-flush-thread");
            thread.setDaemon(true);
            return thread;
        });

        this.flushExecutor.scheduleWithFixedDelay(
                this::flushSafely,
                flushIntervalMs,
                flushIntervalMs,
                TimeUnit.MILLISECONDS
        );

        logger.info("ClickHouse 批量写入器初始化完成，url={}, batchSize={}, flushIntervalMs={}, bufferCapacity={}",
                url, batchSize, flushIntervalMs, bufferCapacity);
    }

    @PreDestroy
    public void destroy() {
        logger.info("应用关闭前刷新 ClickHouse 行为事件缓冲队列");

        flushSafely();

        if (flushExecutor != null) {
            flushExecutor.shutdown();
        }
    }




    public void save(BehaviorEvent event) {
        if (event == null) {
            return;
        }

        if (event.getEventId() == null) {
            event.setEventId(CommunityUtil.generateUUID());
        }

        long eventTime = event.getTimestamp() == 0
                ? System.currentTimeMillis()
                : event.getTimestamp();

        String sql = "insert into behavior_event " +
                "(event_id, user_id, event_type, entity_type, entity_id, entity_user_id, " +
                "target_id, post_id, keyword, ip, event_time, data) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        clickHouseJdbcTemplate.update(sql,
                event.getEventId(),
                Math.max(event.getUserId(), 0),
                emptyToDefault(event.getEventType()),
                event.getEntityType(),
                Math.max(event.getEntityId(), 0),
                Math.max(event.getEntityUserId(), 0),
                Math.max(event.getTargetId(), 0),
                Math.max(event.getPostId(), 0),
                emptyToDefault(event.getKeyword()),
                emptyToDefault(event.getIp()),
                new Timestamp(eventTime),
                event.getData() == null ? "{}" : JSONObject.toJSONString(event.getData())
        );
    }

    private String emptyToDefault(String value) {
        return value == null ? "" : value;
    }

    public void buffer(BehaviorEvent event) {
        if (event == null) {
            return;
        }

        boolean success = buffer.offer(event);

        if (!success) {
            logger.warn("ClickHouse 行为事件缓冲队列已满，触发同步刷新");
            flush();
            success = buffer.offer(event);
        }

        if (!success) {
            logger.error("ClickHouse 行为事件缓冲队列写入失败，丢弃事件: eventType={}, userId={}",
                    event.getEventType(), event.getUserId());
            return;
        }

        if (buffer.size() >= batchSize) {
            flush();
        }
    }

    private void flushSafely() {
        try {
            flush();
        } catch (Exception e) {
            logger.error("ClickHouse 定时批量写入失败", e);
        }
    }

    public void flush() {
        synchronized (flushLock) {
            if (buffer == null || buffer.isEmpty()) {
                return;
            }

            List<BehaviorEvent> events = new ArrayList<>(batchSize);
            buffer.drainTo(events, batchSize);

            if (events.isEmpty()) {
                return;
            }

            batchSave(events);
        }
    }


    private void batchSave(List<BehaviorEvent> events) {
        String sql = "insert into behavior_event " +
                "(event_id, user_id, event_type, entity_type, entity_id, entity_user_id, " +
                "target_id, post_id, keyword, ip, event_time, data) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        clickHouseJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                BehaviorEvent event = events.get(i);

                if (event.getEventId() == null) {
                    event.setEventId(CommunityUtil.generateUUID());
                }

                long eventTime = event.getTimestamp() == 0
                        ? System.currentTimeMillis()
                        : event.getTimestamp();

                ps.setString(1, event.getEventId());
                ps.setInt(2, Math.max(event.getUserId(), 0));
                ps.setString(3, emptyToDefault(event.getEventType()));
                ps.setInt(4, event.getEntityType());
                ps.setInt(5, Math.max(event.getEntityId(), 0));
                ps.setInt(6, Math.max(event.getEntityUserId(), 0));
                ps.setInt(7, Math.max(event.getTargetId(), 0));
                ps.setInt(8, Math.max(event.getPostId(), 0));
                ps.setString(9, emptyToDefault(event.getKeyword()));
                ps.setString(10, emptyToDefault(event.getIp()));
                ps.setTimestamp(11, new Timestamp(eventTime));
                ps.setString(12, event.getData() == null ? "{}" : JSONObject.toJSONString(event.getData()));
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        });

        logger.info("ClickHouse 批量写入行为事件成功，count={}", events.size());
    }


    /**
     * 行为类型统计
     */
    public List<Map<String, Object>> queryEventTypeStats() {
        String sql = "select event_type, count(*) as cnt " +
                "from behavior_event " +
                "group by event_type " +
                "order by cnt desc";

        return normalizeRows(clickHouseJdbcTemplate.queryForList(sql));
    }

    /**
     * 热门帖子排行：按浏览量统计
     */
    public List<Map<String, Object>> queryHotPosts(int limit) {
        String sql = "select post_id, count(*) as views " +
                "from behavior_event " +
                "where event_type = 'VIEW_POST' and post_id > 0 " +
                "group by post_id " +
                "order by views desc " +
                "limit ?";

        return normalizeRows(clickHouseJdbcTemplate.queryForList(sql, limit));
    }

    /**
     * 搜索关键词排行
     */
    public List<Map<String, Object>> querySearchKeywords(int limit) {
        String sql = "select keyword, count(*) as cnt " +
                "from behavior_event " +
                "where event_type = 'SEARCH_KEYWORD' and keyword != '' " +
                "group by keyword " +
                "order by cnt desc " +
                "limit ?";

        return normalizeRows(clickHouseJdbcTemplate.queryForList(sql, limit));
    }

    /**
     * DAU：按日期统计活跃用户数
     */
    public List<Map<String, Object>> queryDau() {
        String sql = "select event_date, uniqExact(user_id) as dau " +
                "from behavior_event " +
                "where user_id > 0 " +
                "group by event_date " +
                "order by event_date desc";

        return normalizeRows(clickHouseJdbcTemplate.queryForList(sql));
    }

    /**
     * 每日行为趋势
     */
    public List<Map<String, Object>> queryDailyEventTrend() {
        String sql = "select event_date, event_type, count(*) as cnt " +
                "from behavior_event " +
                "group by event_date, event_type " +
                "order by event_date desc, cnt desc";

        return normalizeRows(clickHouseJdbcTemplate.queryForList(sql));
    }

    private List<Map<String, Object>> normalizeRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Map<String, Object> newRow = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : row.entrySet()) {
                newRow.put(entry.getKey(), normalizeValue(entry.getValue()));
            }

            result.add(newRow);
        }

        return result;
    }

    private Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }

//        System.out.println("ClickHouse value class = " + value.getClass() + ", value = " + value);

        if (value instanceof String || value instanceof Boolean) {
            return value;
        }

        if (value instanceof Number) {
            Number number = (Number) value;

            if (value instanceof Float || value instanceof Double) {
                return number.doubleValue();
            }

            return number.longValue();
        }

        if (value instanceof java.sql.Date
                || value instanceof java.sql.Timestamp
                || value instanceof java.util.Date) {
            return value.toString();
        }

        return value.toString();
    }



}
