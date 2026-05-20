package com.greate.community.entity;

import java.util.HashMap;
import java.util.Map;

public class BehaviorEvent {

    private String eventId;
    private int userId;
    private String eventType;

    private int entityType;
    private int entityId;
    private int entityUserId;

    private int postId;
    private String keyword;
    private String ip;
    private long timestamp;

    private Map<String, Object> data = new HashMap<>();

    public String getEventId() {
        return eventId;
    }

    public BehaviorEvent setEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public BehaviorEvent setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public String getEventType() {
        return eventType;
    }

    public BehaviorEvent setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public BehaviorEvent setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public BehaviorEvent setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public BehaviorEvent setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public int getPostId() {
        return postId;
    }

    public BehaviorEvent setPostId(int postId) {
        this.postId = postId;
        return this;
    }

    public String getKeyword() {
        return keyword;
    }

    public BehaviorEvent setKeyword(String keyword) {
        this.keyword = keyword;
        return this;
    }

    public String getIp() {
        return ip;
    }

    public BehaviorEvent setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public BehaviorEvent setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public BehaviorEvent setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
