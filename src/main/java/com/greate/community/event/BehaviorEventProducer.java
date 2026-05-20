package com.greate.community.event;

import com.alibaba.fastjson.JSONObject;
import com.greate.community.entity.BehaviorEvent;
import com.greate.community.util.CommunityConstant;
import com.greate.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class BehaviorEventProducer implements CommunityConstant {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void fireEvent(BehaviorEvent event) {
        if (event == null) {
            return;
        }

        if (event.getEventId() == null) {
            event.setEventId(CommunityUtil.generateUUID());
        }

        if (event.getTimestamp() == 0) {
            event.setTimestamp(System.currentTimeMillis());
        }

        kafkaTemplate.send(TOPIC_BEHAVIOR_LOG, JSONObject.toJSONString(event));
    }
}
