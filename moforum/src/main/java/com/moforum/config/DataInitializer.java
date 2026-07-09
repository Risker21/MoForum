package com.moforum.config;

import com.moforum.entity.*;
import com.moforum.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final long MO_NO_BASE = 1_000_000_000L;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BoardMapper boardMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private FriendRequestMapper friendRequestMapper;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Override
    @Transactional
    public void run(String... args) {
        if (userMapper.selectByUsername("alice") != null
                || userMapper.selectByUsername("admin") != null) {
            return;
        }

        long t = System.currentTimeMillis();

        User alice = createUser("alice", "123456");
        User bob = createUser("bob", "123456");
        User admin = createUser("admin", "123456");

        Post p1 = createPost(alice.getId(), 1L, "新人报到", "大家好，我是 alice，刚来 MoForum，请多多关照！");
        Post p2 = createPost(bob.getId(), 1L, "大家平时都逛哪些吧", "综合吧的各位，除了这个吧你们还常去哪些板块？推荐一下～");
        Post p3 = createPost(alice.getId(), 2L, "推荐一款好玩的独立游戏", "最近在玩《Hollow Knight》，画面精美手感好，强烈推荐！");
        Post p4 = createPost(admin.getId(), 2L, "2026 热门网游排行榜", "整理了一下今年比较火的网游，大家看看有没有补充");
        Post p5 = createPost(bob.getId(), 3L, "Java 学习路线分享", "从零基础到 Spring Boot，这是我整理的学习路线图");
        Post p6 = createPost(admin.getId(), 3L, "今天开始准备考研", "立个 Flag，每天打卡学习进度，欢迎监督");
        Post p7 = createPost(alice.getId(), 4L, "周末做了一道红烧肉", "按照网上教程做的，味道还不错，分享给大家");
        Post p8 = createPost(bob.getId(), 4L, "推荐一个周末好去处", "市郊新开的森林公园，适合徒步和露营");

        createReply(p1.getId(), bob.getId(), "欢迎欢迎 👋");
        createReply(p1.getId(), admin.getId(), "新人好，有什么问题尽管问");
        createReply(p2.getId(), alice.getId(), "我喜欢去游戏吧和学习吧～");
        createReply(p3.getId(), bob.getId(), "这个游戏确实不错，我也通关了！");
        createReply(p3.getId(), admin.getId(), "马上去试试");
        createReply(p3.getId(), alice.getId(), "还有《Celeste》也很好玩");
        createReply(p3.getId(), bob.getId(), "收藏了，慢慢玩");
        createReply(p3.getId(), admin.getId(), "画面党表示很满意");
        createReply(p4.getId(), alice.getId(), "今年原神还有更新吗");
        createReply(p4.getId(), bob.getId(), "期待黑神话悟空");
        createReply(p4.getId(), admin.getId(), "加一个永劫无间");
        createReply(p5.getId(), alice.getId(), "很实用，已收藏");
        createReply(p5.getId(), admin.getId(), "Spring Boot 那块可以再详细点");
        createReply(p5.getId(), bob.getId(), "大佬带带我");
        createReply(p5.getId(), alice.getId(), "建议加上微服务部分");
        createReply(p5.getId(), bob.getId(), "跟着学完了，收获很大");
        createReply(p5.getId(), admin.getId(), "催更催更");
        createReply(p5.getId(), alice.getId(), "已三连");
        createReply(p5.getId(), bob.getId(), "好人一生平安");
        createReply(p6.getId(), alice.getId(), "加油加油 💪");
        createReply(p6.getId(), bob.getId(), "同考研，一起努力");
        createReply(p6.getId(), admin.getId(), "坚持就是胜利");
        createReply(p6.getId(), alice.getId(), "今天打卡了吗");
        createReply(p7.getId(), bob.getId(), "看起来很好吃！求教程");
        createReply(p7.getId(), admin.getId(), "色泽很棒");
        createReply(p8.getId(), alice.getId(), "求具体地址");

        followMapper.insert(follow(alice.getId(), bob.getId()));
        followMapper.insert(follow(alice.getId(), admin.getId()));
        followMapper.insert(follow(bob.getId(), alice.getId()));

        FriendRequest req = new FriendRequest();
        req.setFromId(alice.getId());
        req.setToId(bob.getId());
        friendRequestMapper.insert(req);
        friendRequestMapper.updateStatus(req.getId(), 1);
        Friend f = new Friend();
        f.setUserId1(alice.getId());
        f.setUserId2(bob.getId());
        friendMapper.insert(f);

        messageMapper.insert(message(alice.getId(), bob.getId(), "你好 bob，很高兴认识你！"));
        messageMapper.insert(message(bob.getId(), alice.getId(), "你好 alice，我也很高兴！"));
        messageMapper.insert(message(alice.getId(), bob.getId(), "周末一起打游戏吗？"));

        long elapsed = System.currentTimeMillis() - t;
        System.out.println("✓ 示例数据已初始化（" + elapsed + "ms）");
    }

    private User createUser(String username, String password) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        userMapper.insert(u);
        long mo = MO_NO_BASE + u.getId();
        userMapper.updateUserNo(u.getId(), mo);
        return u;
    }

    private Post createPost(Long userId, Long boardId, String title, String content) {
        Post p = new Post();
        p.setUserId(userId);
        p.setBoardId(boardId);
        p.setTitle(title);
        p.setContent(content);
        p.setViewCount(0);
        p.setReplyCount(0);
        postMapper.insert(p);
        boardMapper.incrementPostCount(boardId);
        return p;
    }

    private void createReply(Long postId, Long userId, String content) {
        Reply r = new Reply();
        r.setPostId(postId);
        r.setUserId(userId);
        r.setContent(content);
        replyMapper.insert(r);
        postMapper.incrementReplyCount(postId);
    }

    private Follow follow(Long followerId, Long followedId) {
        Follow f = new Follow();
        f.setFollowerId(followerId);
        f.setFollowedId(followedId);
        return f;
    }

    private Message message(Long fromId, Long toId, String content) {
        Message m = new Message();
        m.setFromId(fromId);
        m.setToId(toId);
        m.setContent(content);
        return m;
    }
}
