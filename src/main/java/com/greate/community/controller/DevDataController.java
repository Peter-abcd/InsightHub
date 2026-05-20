package com.greate.community.controller;

import com.greate.community.dao.UserMapper;
import com.greate.community.entity.*;
import com.greate.community.event.BehaviorEventProducer;
import com.greate.community.event.EventProducer;
import com.greate.community.service.CommentService;
import com.greate.community.service.DiscussPostService;
import com.greate.community.service.FollowService;
import com.greate.community.service.LikeService;
import com.greate.community.util.CommunityConstant;
import com.greate.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Profile("develop")
@RestController
@RequestMapping("/dev")
public class DevDataController implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BehaviorEventProducer behaviorEventProducer;


    private final Random random = new Random();

    @PostMapping("/mock-data")
    public String mockData(
            @RequestParam(defaultValue = "20") int userCount,
            @RequestParam(defaultValue = "100") int postCount,
            @RequestParam(defaultValue = "300") int commentCount
    ) {
        List<User> users = createUsers(userCount);
        List<DiscussPost> posts = createPosts(users, postCount);
        createComments(users, posts, commentCount);
        createLikes(users, posts);
        createFollows(users);

        return "生成测试数据完成：用户 " + users.size()
                + " 个，帖子 " + posts.size()
                + " 篇，评论 " + commentCount + " 条";
    }

    private List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            User user = new User();
            user.setUsername("mock_user_" + System.currentTimeMillis() + "_" + i);

            String salt = CommunityUtil.generateUUID().substring(0, 5);
            user.setSalt(salt);
            user.setPassword(CommunityUtil.md5("123456" + salt));

            user.setEmail("mock" + i + "@test.com");
            user.setType(0);
            user.setStatus(1);
            user.setActivationCode(CommunityUtil.generateUUID());
            user.setHeaderUrl("http://images.nowcoder.com/head/" + random.nextInt(1000) + "t.png");
            user.setCreateTime(new Date());

            userMapper.insertUser(user);
            users.add(user);
        }

        return users;
    }

    private List<DiscussPost> createPosts(List<User> users, int count) {
        List<DiscussPost> posts = new ArrayList<>();

        String[] titles = {
                "Kafka 消息队列在社区通知系统中的应用",
                "Elasticsearch 全文检索实战记录",
                "Redis 点赞和关注模型设计",
                "Spring Security 权限控制总结",
                "Flink 实时计算用户行为数据",
                "ClickHouse 分析社区行为日志",
                "如何设计内容社区热榜系统",
                "MySQL 索引优化实践",
                "高并发场景下缓存一致性问题",
                "用户行为事件流设计方案"
        };

        for (int i = 1; i <= count; i++) {
            User user = users.get(random.nextInt(users.size()));

            DiscussPost post = new DiscussPost();
            post.setUserId(user.getId());
            post.setTitle(titles[random.nextInt(titles.length)] + " #" + i);
            post.setContent("这是一篇用于 InsightHub 测试的帖子，包含 Redis、Kafka、Elasticsearch、Flink、ClickHouse 等技术关键词。当前编号：" + i);
            post.setType(0);
            post.setStatus(0);
            post.setCreateTime(new Date());
            post.setCommentCount(0);
            post.setScore(random.nextDouble() * 100);

            discussPostService.addDiscussPost(post);
            posts.add(post);

            // 触发原项目 Kafka -> Elasticsearch 同步链路
            Event event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(user.getId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(post.getId());
            eventProducer.fireEvent(event);
        }

        return posts;
    }

    private void createComments(List<User> users, List<DiscussPost> posts, int count) {
        String[] contents = {
                "这个设计思路不错。",
                "可以考虑增加 Kafka 异步处理。",
                "Redis 这里需要注意数据一致性。",
                "Elasticsearch 索引同步要考虑失败补偿。",
                "这个场景很适合用 ClickHouse 做分析。",
                "后续可以接入 Flink 做实时统计。",
                "建议补充一下接口压测数据。",
                "这个模块适合写到简历里。"
        };

        for (int i = 1; i <= count; i++) {
            User user = users.get(random.nextInt(users.size()));
            DiscussPost post = posts.get(random.nextInt(posts.size()));

            Comment comment = new Comment();
            comment.setUserId(user.getId());
            comment.setEntityType(ENTITY_TYPE_POST);
            comment.setEntityId(post.getId());
            comment.setTargetId(0);
            comment.setContent(contents[random.nextInt(contents.length)]);
            comment.setStatus(0);
            comment.setCreateTime(new Date());

            commentService.addComment(comment);
        }
    }

    private void createLikes(List<User> users, List<DiscussPost> posts) {
        for (DiscussPost post : posts) {
            int likeTimes = random.nextInt(Math.min(users.size(), 10));

            for (int i = 0; i < likeTimes; i++) {
                User user = users.get(random.nextInt(users.size()));
                likeService.like(user.getId(), ENTITY_TYPE_POST, post.getId(), post.getUserId());
            }
        }
    }

    private void createFollows(List<User> users) {
        for (User user : users) {
            int followTimes = random.nextInt(Math.min(users.size(), 8));

            for (int i = 0; i < followTimes; i++) {
                User target = users.get(random.nextInt(users.size()));

                if (user.getId() != target.getId()) {
                    followService.follow(user.getId(), ENTITY_TYPE_USER, target.getId());
                }
            }
        }
    }

    @PostMapping("/mock-behavior")
    public String mockBehavior(
            @RequestParam(defaultValue = "500") int viewCount,
            @RequestParam(defaultValue = "100") int searchCount,
            @RequestParam(defaultValue = "200") int likeCount,
            @RequestParam(defaultValue = "100") int commentCount,
            @RequestParam(defaultValue = "50") int followCount
    ) {
        List<Map<String, Object>> users = loadMockUsers();
        List<Map<String, Object>> posts = loadPosts();

        if (users.isEmpty() || posts.isEmpty()) {
            return "生成失败：请先调用 /dev/mock-data 生成用户和帖子";
        }

        mockViewBehavior(users, posts, viewCount);
        mockSearchBehavior(users, searchCount);
        mockLikeBehavior(users, posts, likeCount);
        mockCommentBehavior(users, posts, commentCount);
        mockFollowBehavior(users, followCount);

        return "生成行为数据完成：浏览 " + viewCount
                + " 次，搜索 " + searchCount
                + " 次，点赞/取消点赞 " + likeCount
                + " 次，评论 " + commentCount
                + " 条，关注 " + followCount + " 次";
    }

    //3. 增加查询用户和帖子的工具方法
    private List<Map<String, Object>> loadMockUsers() {
        return jdbcTemplate.queryForList(
                "select id, username from user where status = 1 order by id desc limit 200"
        );
    }

    private List<Map<String, Object>> loadPosts() {
        return jdbcTemplate.queryForList(
                "select id, user_id, title from discuss_post where status = 0 order by id desc limit 500"
        );
    }

    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value.toString());
    }

    //4. 增加浏览行为模拟
    private void mockViewBehavior(List<Map<String, Object>> users,
                                  List<Map<String, Object>> posts,
                                  int count) {
        for (int i = 0; i < count; i++) {
            Map<String, Object> user = users.get(random.nextInt(users.size()));
            Map<String, Object> post = posts.get(random.nextInt(posts.size()));

            int userId = getInt(user, "id");
            int postId = getInt(post, "id");
            int postUserId = getInt(post, "user_id");

            behaviorEventProducer.fireEvent(new BehaviorEvent()
                    .setUserId(userId)
                    .setEventType(BEHAVIOR_VIEW_POST)
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(postId)
                    .setEntityUserId(postUserId)
                    .setPostId(postId));
        }
    }

    //5. 增加搜索行为模拟
    private void mockSearchBehavior(List<Map<String, Object>> users, int count) {
        String[] keywords = {
                "Kafka", "Redis", "Elasticsearch", "Spring Security",
                "Flink", "ClickHouse", "MySQL", "用户行为分析",
                "内容社区", "实时计算", "热榜", "点赞系统"
        };

        for (int i = 0; i < count; i++) {
            Map<String, Object> user = users.get(random.nextInt(users.size()));
            int userId = getInt(user, "id");
            String keyword = keywords[random.nextInt(keywords.length)];

            behaviorEventProducer.fireEvent(new BehaviorEvent()
                    .setUserId(userId)
                    .setEventType(BEHAVIOR_SEARCH_KEYWORD)
                    .setKeyword(keyword));
        }
    }

    //6. 增加点赞 / 取消点赞行为模拟
    private void mockLikeBehavior(List<Map<String, Object>> users,
                                  List<Map<String, Object>> posts,
                                  int count) {
        for (int i = 0; i < count; i++) {
            Map<String, Object> user = users.get(random.nextInt(users.size()));
            Map<String, Object> post = posts.get(random.nextInt(posts.size()));

            int userId = getInt(user, "id");
            int postId = getInt(post, "id");
            int postUserId = getInt(post, "user_id");

            likeService.like(userId, ENTITY_TYPE_POST, postId, postUserId);

            int likeStatus = likeService.findEntityLikeStatus(userId, ENTITY_TYPE_POST, postId);

            behaviorEventProducer.fireEvent(new BehaviorEvent()
                    .setUserId(userId)
                    .setEventType(likeStatus == 1 ? BEHAVIOR_LIKE_POST : BEHAVIOR_UNLIKE_POST)
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(postId)
                    .setEntityUserId(postUserId)
                    .setPostId(postId));
        }
    }

    //7. 增加评论行为模拟
    private void mockCommentBehavior(List<Map<String, Object>> users,
                                     List<Map<String, Object>> posts,
                                     int count) {
        String[] comments = {
                "这个帖子很有参考价值。",
                "Kafka 行为事件流这个设计不错。",
                "后续可以接入 Flink 做实时统计。",
                "ClickHouse 很适合做运营分析看板。",
                "这个模块可以写进简历。",
                "Redis 点赞模型需要注意幂等性。",
                "搜索索引同步可以考虑失败补偿。",
                "内容社区和实时分析结合得很好。"
        };

        for (int i = 0; i < count; i++) {
            Map<String, Object> user = users.get(random.nextInt(users.size()));
            Map<String, Object> post = posts.get(random.nextInt(posts.size()));

            int userId = getInt(user, "id");
            int postId = getInt(post, "id");

            Comment comment = new Comment();
            comment.setUserId(userId);
            comment.setEntityType(ENTITY_TYPE_POST);
            comment.setEntityId(postId);
            comment.setTargetId(0);
            comment.setContent(comments[random.nextInt(comments.length)]);
            comment.setStatus(0);
            comment.setCreateTime(new Date());

            commentService.addComment(comment);

            behaviorEventProducer.fireEvent(new BehaviorEvent()
                    .setUserId(userId)
                    .setEventType(BEHAVIOR_COMMENT_POST)
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(postId)
                    .setTargetId(0)
                    .setPostId(postId));
        }
    }

    // 8. 增加关注行为模拟
    private void mockFollowBehavior(List<Map<String, Object>> users, int count) {
        int success = 0;
        int maxRetry = count * 3;
        int retry = 0;

        while (success < count && retry < maxRetry) {
            retry++;

            Map<String, Object> user = users.get(random.nextInt(users.size()));
            Map<String, Object> target = users.get(random.nextInt(users.size()));

            int userId = getInt(user, "id");
            int targetUserId = getInt(target, "id");

            if (userId == targetUserId) {
                continue;
            }

            followService.follow(userId, ENTITY_TYPE_USER, targetUserId);

            behaviorEventProducer.fireEvent(new BehaviorEvent()
                    .setUserId(userId)
                    .setEventType(BEHAVIOR_FOLLOW_USER)
                    .setEntityType(ENTITY_TYPE_USER)
                    .setEntityId(targetUserId)
                    .setEntityUserId(targetUserId));

            success++;
        }
    }

}
