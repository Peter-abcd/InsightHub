package com.greate.community.event;

import com.alibaba.fastjson.JSONObject;
import com.greate.community.entity.BehaviorEvent;
import com.greate.community.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Profile("develop")
@Component
public class BehaviorEventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(BehaviorEventConsumer.class);

    @KafkaListener(topics = {TOPIC_BEHAVIOR_LOG})
    public void handleBehaviorEvent(ConsumerRecord<String, String> record) {
        if (record == null || record.value() == null) {
            logger.warn("收到空的用户行为事件");
            return;
        }

        BehaviorEvent event = JSONObject.parseObject(record.value(), BehaviorEvent.class);

        logger.info("收到用户行为事件: eventType={}, userId={}, entityType={}, entityId={}, postId={}, keyword={}",
                event.getEventType(),
                event.getUserId(),
                event.getEntityType(),
                event.getEntityId(),
                event.getPostId(),
                event.getKeyword());
    }
}
