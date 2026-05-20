package com.greate.community.service;

import com.alibaba.fastjson.JSONObject;
import com.greate.community.entity.BehaviorEvent;
import com.greate.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    @PostConstruct
    public void init() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);

        this.clickHouseJdbcTemplate = new JdbcTemplate(dataSource);

        logger.info("ClickHouse JdbcTemplate 初始化完成，url={}", url);
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
}
