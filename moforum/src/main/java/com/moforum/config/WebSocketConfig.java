package com.moforum.config;

import com.moforum.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final UserMapper userMapper;

    public WebSocketConfig(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && accessor.getNativeHeader("token") != null) {
                    List<String> tokens = accessor.getNativeHeader("token");
                    if (!tokens.isEmpty()) {
                        String token = tokens.get(0);
                        try {
                            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
                            if (keyBytes.length < 32) {
                                keyBytes = MessageDigest.getInstance("SHA-256").digest(keyBytes);
                            }
                            SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");
                            Jws<Claims> claims = Jwts.parser().verifyWith(new SecretKeySpec(key.getEncoded(), "HmacSHA256"))
                                    .build().parseSignedClaims(token);
                            Long userId = Long.valueOf(claims.getPayload().getSubject());
                            String username = claims.getPayload().get("username", String.class);
                            if (userMapper.selectById(userId) != null) {
                                accessor.setUser(new UserPrincipal(userId, username));
                            }
                        } catch (Exception ignored) {}
                    }
                }
                return message;
            }
        });
    }
}
