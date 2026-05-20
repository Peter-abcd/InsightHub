package com.greate.community.event;

import com.alibaba.fastjson.JSONObject;
import com.greate.community.entity.BehaviorEvent;
import com.greate.community.service.BehaviorEventClickHouseService;
import com.greate.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class BehaviorEventClickHouseConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(BehaviorEventClickHouseConsumer.class);

    @Value("${clickhouse.sink.enabled:false}")
    private boolean enabled;

    @Autowired
    private BehaviorEventClickHouseService clickHouseService;

    @PostConstruct
    public void init() {
        logger.info("ClickHouse 行为事件落库消费者初始化完成，enabled={}", enabled);
    }

    @KafkaListener(
            topics = {TOPIC_BEHAVIOR_LOG},
            groupId = "behavior-clickhouse-sink"
    )
    public void handleBehaviorEvent(ConsumerRecord<String, String> record) {
        if (!enabled) {
            logger.debug("ClickHouse sink 未开启，跳过行为事件落库");
            return;
        }

        if (record == null || record.value() == null) {
            logger.warn("收到空的行为事件消息，跳过");
            return;
        }

        try {
            BehaviorEvent event = JSONObject.parseObject(record.value(), BehaviorEvent.class);
//            clickHouseService.save(event);
            clickHouseService.buffer(event);

            logger.debug("行为事件已加入 ClickHouse 缓冲队列: eventType={}, userId={}, postId={}",
                    event.getEventType(), event.getUserId(), event.getPostId());
        } catch (Exception e) {
            logger.error("行为事件写入 ClickHouse 失败，message={}", record.value(), e);
        }
    }
}
