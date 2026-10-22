package com.yaya.framework.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTException;
import com.yaya.framework.config.YaYaConfig;
import com.yaya.framework.enums.KeyConstant;
import com.yaya.framework.utils.JwtUtils;
import com.yaya.framework.utils.RedisClient;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * 校验和解析token的过滤器（统一token处理的过滤器）
 * 1. 认证: 把请求携带的有效 token 翻译成 Spring Security 的已认证状态
 * 2. 会话绑定: token 的 jti 必须与 Redis 登录会话中的会话ID一致, 不一致按未登录处理
 * 3. 拒绝统一交给框架: 校验失败只跳过认证(不写安全上下文), 由后续授权检查 + 401 处理器统一拒绝
 */
@Slf4j
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Resource
    private RedisClient redisClient;

    @Resource
    private YaYaConfig yaYaConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 第一步: 从请求头提取令牌
        String token = resolveToken(request);
        // 第二步: 校验令牌并写入安全上下文(无令牌或已有认证时跳过, 直接放行)
        if (StrUtil.isNotBlank(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 2.1 校验令牌是否有效(签名、是否过期)
                JWT jwt = JwtUtils.validateAndGet(token);
                        // 2.2 校验 redis 中的登录数据是否有效(已注销/会话过期则查不到)
                        String username = JwtUtils.getClaimAsString(jwt,"username");
                        //会话绑定声明: 登录时生成, 必须与Redis会话中保存的会话ID一致
                        String tokenSessionId = JwtUtils.getClaimAsString(jwt, KeyConstant.AUTH_SESSION_CLAIM.getKey());
                if(StringUtils.isNotEmpty(username)){
                    String loginKey = KeyConstant.LOGIN_USER_KEY.getKey() + username; //使用前缀+账号作为唯一的key
                    String loginJson = redisClient.get(loginKey);
                    if (StrUtil.isNotBlank(loginJson)) {
                        // 2.3 还原登录用户, 构建认证信息并写入安全上下文
                        LoginUserDetails loginUserDetails = JSONUtil.toBean(loginJson, LoginUserDetails.class);
                        if(Objects.nonNull(loginUserDetails)){
                            if (!loginUserDetails.isAccountNonExpired()) {
                                // 账号会话期间自然过期: 立即清理会话, 不再续期, 后续请求按未登录处理
                                clearExpiredSession(loginKey, username);
                            } else if (StrUtil.isBlank(tokenSessionId)
                                    || !tokenSessionId.equals(loginUserDetails.getSessionId())) {
                                /*
                                 * 会话绑定校验失败: token的jti与当前登录会话的会话ID不一致
                                 * 1. 注销/管理员清理会话后, 用户重新登录生成新会话ID, 旧token到此被拒绝, 不会复活
                                 * 2. 没有私钥之外凭据的伪造token不知道会话ID, 无法冒用他人在线会话
                                 */
                                log.error("Token校验失败: jti与登录会话不一致(旧token或伪造token), username={}", username);
                            } else {
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUserDetails, null, loginUserDetails.getAuthorities());
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                try {
                                    // redis TLL续期
                                    redisClient.expire(loginKey, yaYaConfig.getTokenTimeout());
                                } catch (Exception e) {
                                    log.error("登录数据滑动续期失败, key={}", loginKey, e);
                                }
                                /*
                                 * token续期
                                 * 1. token不是长期有效的
                                 * 2. 系统在使用过程中不能让token直接失效
                                 * 3. 剩余有效期小于 20 分钟时进行续期
                                 */
                                try {
                                    // 复用前面已经验签过的 jwt, 避免再次调用 getExpirationDate(token) 重复验签
                                    Long expireSeconds = jwt.getPayload().getClaimsJson().getLong(JWT.EXPIRES_AT);
                                    if (expireSeconds != null) {
                                        long remainingSeconds = expireSeconds - System.currentTimeMillis() / 1000;
                                        // 剩余有效期小于配置的续签阈值时触发续期
                                        Integer thresholdMinutes = yaYaConfig.getTokenRenewThreshold();
                                        long renewThresholdSeconds = (thresholdMinutes == null ? 20L : thresholdMinutes.longValue()) * 60L;
                                        if (remainingSeconds <= renewThresholdSeconds) {
                                            long remainingMinutes = Math.max(0, remainingSeconds / 60);
                                            log.info("用户 [{}] 的 Token 即将在{}分钟内过期，触发自动续期机制...", username, remainingMinutes);
                                        // 重新组装 Claims 载荷（这里包含你的业务需要，比如 username; jti必须原样携带, 续签不能破坏令牌与会话的绑定）
                                        Map<String, Object> claims = Map.of(
                                                "username", username,
                                                KeyConstant.AUTH_SESSION_CLAIM.getKey(), tokenSessionId);
                                            // 重新生成新 Token
                                            String newToken = JwtUtils.createToken(claims, yaYaConfig.getTokenTimeout());
                                            // 将新 Token 塞入特定的 Response Header
                                            response.setHeader(KeyConstant.AUTH_RENEW_TOKEN_HEADER.getKey(), newToken);
                                            // 关键：暴露自定义请求头，允许前端跨域读取
                                            response.setHeader("Access-Control-Expose-Headers", KeyConstant.AUTH_RENEW_TOKEN_HEADER.getKey());
                                        }
                                    }
                                }catch (Exception e){
                                    // 续期失败不应该阻断用户的本次正常请求，打印警告日志即可
                                    log.warn("Token自动续期失败，但不影响本次请求: {}", e.getMessage());
                                }
                            }
                        }
                    } else {
                        log.error("Token校验失败: redis 中的登录数据不存在(已注销或会话过期), key={}", loginKey);
                    }
                }
            } catch (JWTException e) {
                log.error("Token无效, 跳过认证: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Token认证异常, 跳过认证", e);
            }
        }

        //放行,不管token对或者错，都交给Spring Security框架去处理
        filterChain.doFilter(request, response);
    }

    /**
     * 清理已自然过期账号的登录会话; 清理失败也不能继续认证
     */
    private void clearExpiredSession(String loginKey, String username) {
        try {
            redisClient.deleteKey(loginKey);
            log.info("用户 [{}] 账号已过期, 已清理登录会话", username);
        } catch (Exception e) {
            log.error("账号已过期但清理登录会话失败, key={}", loginKey, e);
        }
    }

    /**
     * 从请求头中提取令牌(兼容两种格式: "Bearer 令牌" 或 裸令牌)
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. 取头 —— 可能为 null
        String header = request.getHeader(KeyConstant.AUTH_TOKEN_HEADER.getKey());
        // 2. 非空即可: 带 Bearer 前缀则去掉前缀, 不带前缀(裸令牌)也兼容
        if (StrUtil.isNotBlank(header)) {
            return header.startsWith(KeyConstant.AUTH_TOKEN_PREFIX.getKey()) ? header.substring(KeyConstant.AUTH_TOKEN_PREFIX.getKey().length()) : header;
        }
        // 未携带 → 返回 null, 视为"未登录"继续走流程
        return null;
    }
}
