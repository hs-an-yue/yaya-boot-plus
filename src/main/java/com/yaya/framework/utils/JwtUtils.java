package com.yaya.framework.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import cn.hutool.json.JSONException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTException;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

/**
 * JWT工具类
 * 基于hutool工具类生成
 * 基于非对称加密RS256(SHA256withRSA)方式实现
 * 1. 生成token
 * 2. 验证token
 * 3. 获取参数
 */
@Slf4j
public class JwtUtils {
    //公钥
    private static final String PUBLIC_KEY_STR = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3TNmPUYfClvFkmcAFvjEE8b/lAFNafHgmPuU1CDosmhbvJoMSoDtvJsOivpoecRB96sPRXzxbpMyaJFlkZiflN4njT01LX64B7pkrYKMPxgUbLJnQksaPQcJD2dPfTq47KKRRnGCcbJ19pU54vxs5aYzMOLirb2BzGd9oFexM/qd5dVA8MWB+nwmnZ1YOm81rAnqMEQieMbvHo3jvOe1XYTRU901F2EfbUfywL2Q0rP51V+IFqdXvJFNLR0RvCPp7NKrg1UXNt9ZMA1H5A1Zu4f6gfB5VmratQB2/Q2QSx/VAg7g9WDbM0UqGSDMCKfnsq44d7cUDd0rVyCPTb5itQIDAQAB";
    //私钥
    private static final String PRIVATE_KEY_STR = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDdM2Y9Rh8KW8WSZwAW+MQTxv+UAU1p8eCY+5TUIOiyaFu8mgxKgO28mw6K+mh5xEH3qw9FfPFukzJokWWRmJ+U3ieNPTUtfrgHumStgow/GBRssmdCSxo9BwkPZ099OrjsopFGcYJxsnX2lTni/GzlpjMw4uKtvYHMZ32gV7Ez+p3l1UDwxYH6fCadnVg6bzWsCeowRCJ4xu8ejeO857VdhNFT3TUXYR9tR/LAvZDSs/nVX4gWp1e8kU0tHRG8I+ns0quDVRc231kwDUfkDVm7h/qB8HlWatq1AHb9DZBLH9UCDuD1YNszRSoZIMwIp+eyrjh3txQN3StXII9NvmK1AgMBAAECggEAKfXmHxl+KejewbboPXNTPq0xLlJQ7RgFEwAma1cSzUUeodhXHIr4jh6b06FM0Ex6dxDsBLJ+2gryNQPf0I6zNvotZo2F2OuA3XZuQrvQ/ucCeNwe2mSExa7f+RwP6CGLktgD1ukRZaAE4JHOOVDJF4c/jrZ7aZnZVX0MR65a3xEBOB9hGi2hrvpRMv0yZE+tkdeyW/nQUW8t+n8keW6anaTBgf/zGhhJeOn26anqq3tCdrkjibI4fpRQ0nq9I0uJvGREZj22YhHLTjuOQO1CAzeOqr8q7KoP2Xq5HhwO5SbwTiopS1sgMEqcze1iDdd5LqQWPKz8NOrQ/eThHNvAwQKBgQDxb0oOYXIkZc/lTgmfAV+BQ7lk2qRyOUEuHY15+vb++ZpHs1/YJDTedObAx1kjKaDAxbgoVtxw0efKK4Q1gmea4X6B+al45yAu5CNYAPaM0faCK/mS6ivPrHxmqfmWaNPgrHGrSnQCJoucynnlRmmr8wD7xWMP65L42MAR++/RuwKBgQDqi54d747Bew5vKbiDfOPYnj1TLdYgsjBuxBvHz2KgZJaKJZEGwDMZaKixvBfmy4wuein2ltHGnKSU/L8WQM4pUjTYmcVjAs+xYTAv4Iuz3NnqSSjc9Dc1If87ogt4tLWaSIyVGd3Iq6M1KYqRMbzvg8Tb0OcbfMAG0u4u3o5eTwKBgD/eWe/t9V0Nle6qMz9eBJ7UhKHaeTBCVyzU4o1HR7nx289l80/hXB8RO835rmnv5De61lnNiKlLzg6cbnAdXD0LtAS8QAnc0pW1AmiQcKiWDqkm6vHjgs2AarRZ+M3CU3KXC8btfjznZ32FzrNJakDzmun/sJIQlqO/6yeWxz1rAoGBAIHAEQ2ge8m/KKIIY4Gh8m3gRC6iucVGrhJovLGRoY73dmwgEhW498vDoScJ1fMggYEopdQ7ZPtJpqahF5jY6vPi/Uch8hcu8/S9vimOP9qVf5vjMw7grzXeg9LadxwkZB5gqhzFvXKIuPXs8DIYYNvjemDcNS14msZro8Yb6ohzAoGBAMt4bUaB1BDG/x3v9mM8aMOlOEm0cUAuYAu4g4lGEkm2kvdiPpLeBA4jwXzOh0Sq+d3gwtL3gXM49G8PlhTfVPoRTVaPfiC5dN0xkfkTQRYFFHqFyxInyKJXDm+7fQT4FQGo4OCYnu/LFyL2R/bnuF1AoewLHr/Mh5pNqwEan+lf";
    //公钥签名器(用来验签)
    private static final JWTSigner PUBLIC_SIGNER;
    //私钥签名器(用来签名)
    private static final JWTSigner PRIVATE_SIGNER;
    // 允许服务器之间存在 5 秒钟的时钟漂移
    private static final long ALLOW_SKEW_SECONDS = 5L;
    // 标准时间字段(签发时间/过期时间/生效时间): 由本工具类统一控制, 不允许业务参数携带
    private static final List<String> RESERVED_TIME_KEYS = List.of(JWT.ISSUED_AT, JWT.EXPIRES_AT, JWT.NOT_BEFORE);

    //初始化签名器
    static {
        try {
            //私钥对象
            PrivateKey privateKey = SecureUtil.generatePrivateKey(SignAlgorithm.SHA256withRSA.getValue(), Base64.decode(PRIVATE_KEY_STR));
            //创建私钥签名器
            PRIVATE_SIGNER = JWTSignerUtil.rs256(privateKey);
            //公钥对象
            PublicKey publicKey = SecureUtil.generatePublicKey(SignAlgorithm.SHA256withRSA.getValue(), Base64.decode(PUBLIC_KEY_STR));
            //创建公钥签名器
            PUBLIC_SIGNER = JWTSignerUtil.rs256(publicKey);
        } catch (Exception e) {
            // 密钥字符串损坏时给出明确原因, 否则调用方只会连锁收到 ExceptionInInitializerError/NoClassDefFoundError, 极难定位
            log.error("JwtUtils 签名器初始化失败:", e);
            throw new IllegalStateException("JwtUtils 签名器初始化失败, 请检查公私钥字符串是否完整", e);
        }
    }

    /**
     * 创建 Token (使用私钥签名)
     *
     * @param claims        自定义业务参数（如 userId, username, roles 等）, 不允许携带 iat/exp/nbf 标准时间字段
     * @param expireMinutes 过期时间（单位：分钟）
     * @return String JWT 字符串
     * @throws IllegalArgumentException claims 携带了 iat/exp/nbf 标准时间字段
     */
    public static String createToken(Map<String, Object> claims, int expireMinutes) {
        // 防御: 业务参数不允许携带标准时间字段(iat/exp/nbf)，这句话的意思是key不可以用jwt自带的字段，否则会出问题
        // (实测: claims 中带 exp 会把"1 分钟过期"的 token 顶成"约 260 年后过期")
        if (claims != null) {
            for (String reservedKey : RESERVED_TIME_KEYS) {
                if (claims.containsKey(reservedKey)) {
                    throw new IllegalArgumentException("业务参数 claims 不允许包含标准时间字段: " + reservedKey);
                }
            }
        }
        DateTime now = DateTime.now(); //开始时间
        DateTime expTime = now.offsetNew(DateField.MINUTE, expireMinutes);           //过期时间
        return JWT.create()
                .addHeaders(Map.of("alg", "RS256", "typ", "JWT"))     // 明确声明使用的是 RS256 算法
                .setPayload(JWT.ISSUED_AT, now.getTime() / 1000)                // 签发时间 (秒)
                .setPayload(JWT.EXPIRES_AT, expTime.getTime() / 1000)           // 过期时间 (秒)
                .addPayloads(claims)                                                  // 业务参数
                .setSigner(PRIVATE_SIGNER)                                            // 注入刚刚创建的 RS256 签名器
                .sign();
    }


    /**
     * 校验并获取 JWT 对象 (使用公钥验签 + 严格时间校验)
     *
     * @param token JWT 字符串
     * @return JWT
     * @throws JWTException 校验失败时抛出具体异常原因
     */
    public static JWT validateAndGet(String token) {
        if (StrUtil.isBlank(token)) {
            throw new TokenVerifyException("Token 不能为空");
        }
        try {
            JWT jwt = JWT.of(token);
            // 1. 验证签名（拿着公钥去验证是否是由持有私钥的人签发的）
            if (!jwt.verify(PUBLIC_SIGNER)) {
                throw new TokenVerifyException("Token验证失败，可能已被篡改");
            }
            // 2. 验证时间: 自己读取 exp/nbf 字段比对, 不依赖 hutool 抛异常的提示文案(升级 hutool 不受文案变化影响)
            long nowSec = DateTime.now().getTime() / 1000;
            // 2.1 过期时间(exp): 必填, 缺失视为非法 token(createToken 总会写入 exp)
            Long expTimeSec = jwt.getPayload().getClaimsJson().getLong(JWT.EXPIRES_AT);
            if (expTimeSec == null) {
                throw new TokenVerifyException("Token 缺少过期时间字段");
            }
            if (nowSec > expTimeSec + ALLOW_SKEW_SECONDS) {
                // 过期场景携带 exp 值, 供 getExpiresIn/getExpirationDate 直接使用(免去重新解析 token)
                throw new TokenVerifyException("Token已过期", expTimeSec);
            }
            // 2.2 生效时间(nbf): 可选, 存在才校验(iat 有意不校验, 减少服务器时钟漂移误拒)
            Long nbfTimeSec = jwt.getPayload().getClaimsJson().getLong(JWT.NOT_BEFORE);
            if (nbfTimeSec != null && nowSec < nbfTimeSec - ALLOW_SKEW_SECONDS) {
                throw new TokenVerifyException("Token未到生效时间");
            }
            return jwt;
        } catch (Exception e) {
            log.error("Token校验失败:", e);
            // 自己主动抛出的异常(消息已是最终提示): 原样上抛, 不再参与下方分类改写
            // 注意: 此透传判断必须放在分类最前面, 否则会落入兜底被改写为"Token 格式非法或解析错误"
            if (e instanceof TokenVerifyException) {
                throw (TokenVerifyException) e;
            }
            // JSON 解析层失败 = 结构损坏(如 header/payload 段不是合法 JSON 文本), 按异常类型归类即可
            if (e instanceof JSONException) {
                throw new TokenVerifyException("Token结构损坏");
            }
            throw new TokenVerifyException("Token 格式非法或解析错误");
        }
    }
    /**
     * 判断 Token 是否有效
     */
    public static boolean isValid(String token) {
        try {
            validateAndGet(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Token 中所有的载荷参数 (内部包含安全校验)
     */
    public static Map<String, Object> getClaims(String token) {
        return validateAndGet(token).getPayloads();
    }

    /**
     * 获取已校验 JWT 中所有的载荷参数 (免去重复验签, 适用于 validateAndGet 之后连续取值的场景)
     */
    public static Map<String, Object> getClaims(JWT jwt) {
        return jwt.getPayloads();
    }

    /**
     * 获取 Token 中指定的某一个参数 (内部包含安全校验)
     */
    public static Object getClaim(String token, String claimKey) {
        return validateAndGet(token).getPayload().getClaim(claimKey);
    }

    /**
     * 获取已校验 JWT 中指定的某一个参数 (免去重复验签, 适用于 validateAndGet 之后连续取值的场景)
     */
    public static Object getClaim(JWT jwt, String claimKey) {
        return jwt.getPayload().getClaim(claimKey);
    }

    /**
     * 获取已校验 JWT 中指定参数的 Long 值 (免去重复验签)
     * 说明: hutool 解析 JSON 数字返回 NumberWithFormat, 此处统一转换为 Long, 调用方无需关心强转与类型
     */
    public static Long getClaimAsLong(JWT jwt, String claimKey) {
        return Convert.toLong(jwt.getPayload().getClaim(claimKey));
    }

    /**
     * 获取已校验 JWT 中指定参数的 String 值 (免去重复验签)
     */
    public static String getClaimAsString(JWT jwt, String claimKey) {
        return Convert.toStr(jwt.getPayload().getClaim(claimKey));
    }

    /**
     * 获取 Token 还有多久过期（剩余有效时间）
     * @param token JWT 字符串
     * @return long 剩余时间（单位：秒）。如果已过期，返回 <= 0 的值
     */
    public static long getExpiresIn(String token) {
        try {
            JWT jwt = validateAndGet(token);
            // exp 必填由 validateAndGet 保证, 此处不会为 null
            long expTimeSec = jwt.getPayload().getClaimsJson().getLong(JWT.EXPIRES_AT);
            long nowSec = DateTime.now().getTime() / 1000;
            return expTimeSec - nowSec;
        } catch (TokenVerifyException e) {
            // 已过期的 token: 异常中已携带过期时间, 直接算出剩余时间(负数), 无需重新解析 token
            if (e.getExpiresAtSec() != null) {
                long nowSec = DateTime.now().getTime() / 1000;
                return e.getExpiresAtSec() - nowSec; // 返回负数，代表已经过期了多少秒
            }
            throw e; // 其他失败原因(篡改/结构损坏等)直接抛出
        }
    }

    /**
     * 获取 Token 的具体过期绝对时间
     * @param token JWT 字符串
     * @return DateTime 过期时间
     */
    public static DateTime getExpirationDate(String token) {
        try {
            JWT jwt = validateAndGet(token);
            // exp 必填由 validateAndGet 保证, 此处不会为 null
            Long expTimeSec = jwt.getPayload().getClaimsJson().getLong(JWT.EXPIRES_AT);
            return DateUtil.date(expTimeSec * 1000);
        } catch (TokenVerifyException e) {
            // 同理，已过期的 token: 异常中已携带过期时间, 直接返回历史过期时间, 无需重新解析 token
            if (e.getExpiresAtSec() != null) {
                return DateUtil.date(e.getExpiresAtSec() * 1000);
            }
            throw e;
        }
    }

    /**
     * Token 校验失败异常  <br/>
     * 1. 继承 JWTException: 外部按 JWTException 捕获的代码无需任何改动  <br/>
     * 2. 用途: catch 分类时靠它最优先透传, 避免"自己主动抛出"的异常被兜底逻辑改写  <br/>
     * 3. "Token已过期"场景会携带过期时间(秒), 供 getExpiresIn/getExpirationDate 直接使用  <br/>
     */
    private static class TokenVerifyException extends JWTException {
        /** 过期时间(秒), 仅"已过期"场景有值, 其他失败场景为 null */
        private final Long expiresAtSec;

        TokenVerifyException(String message) {
            this(message, null);
        }

        TokenVerifyException(String message, Long expiresAtSec) {
            super(message);
            this.expiresAtSec = expiresAtSec;
        }

        Long getExpiresAtSec() {
            return expiresAtSec;
        }
    }

}
