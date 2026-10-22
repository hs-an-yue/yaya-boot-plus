package com.yaya.framework.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

/**
 *  加密工具类 <br/>
 *      HuTool：5.8.47 <br/>
 *  1. RSA密钥对生成: 注意:每生成一次密钥对,不能将公钥和私钥丢失,否则新生成的私钥,不能解密旧公钥加密的数据,这个东西是一对一的关系 <br/>
 *  2. 公钥加密 <br/>
 *  3. 私钥解密 <br/>
 *  4. AES对称加密: 密钥生成、加密、解密 <br/>
 *  5. Base64编码和解码 <br/>
 */
public class CryptoUtils {

    /**
     * RSA非对称加密，密钥对生成
     * 生成的密钥对是经过base64编码后的
     * 注意:生成2048位密钥,1024位密钥长度已不安全(不符合现行安全规范)
     */
    public static Map<String,String> createRsaKey(){
        Map<String,String> map = new HashMap<>();
        KeyPair keyPair = KeyUtil.generateKeyPair("RSA", 2048);
        String privateKeyBase64 = Base64.encode(keyPair.getPrivate().getEncoded());
        String publicKeyBase64 = Base64.encode(keyPair.getPublic().getEncoded());
        map.put("privateKey",privateKeyBase64);
        map.put("publicKey",publicKeyBase64);
        return  map;
    }

    /**
     * 公钥加密
     *
     * @param data      待加密的明文
     * @param publicKey Base64 编码的公钥
     * @return 加密后的密文（Base64 编码）
     */
    public static String encryptByPublicKey(String data, String publicKey) {
        RSA rsa = new RSA(null, publicKey);
        return rsa.encryptBase64(data, KeyType.PublicKey);
    }

    /**
     * 私钥解密
     *
     * @param encryptData Base64 编码的密文
     * @param privateKey  Base64 编码的私钥
     * @return 解密后的明文
     */
    public static String decryptByPrivateKey(String encryptData, String privateKey) {
        RSA rsa = new RSA(privateKey, null);
        return rsa.decryptStr(encryptData, KeyType.PrivateKey);
    }

    /**
     * AES对称加密，密钥生成
     * 生成的密钥是经过base64编码后的
     * 注意:密钥必须妥善保存,密钥丢失后将无法解密已加密的数据
     */
    public static String createAesKey() {
        byte[] keyBytes = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
        return Base64.encode(keyBytes);
    }

    /**
     * AES对称加密
     * 加密模式: CBC + PKCS5Padding, 每次加密随机生成IV并拼接在密文头部, 解密时无需单独传递IV, 相同明文每次加密出的密文都不同
     *
     * @param data   待加密的明文
     * @param aesKey Base64 编码的AES密钥
     * @return 加密后的密文（Base64 编码, 格式: IV+密文）
     */
    public static String encryptByAes(String data, String aesKey) {
        byte[] keyBytes = Base64.decode(aesKey);
        //随机生成16位IV(初始向量)
        byte[] ivBytes = RandomUtil.randomBytes(16);
        AES aes = new AES(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);
        byte[] encryptBytes = aes.encrypt(data, StandardCharsets.UTF_8);
        //IV + 密文 拼接后进行Base64编码
        return Base64.encode(ArrayUtil.addAll(ivBytes, encryptBytes));
    }

    /**
     * AES对称解密
     *
     * @param encryptData Base64 编码的密文（格式: IV+密文）
     * @param aesKey      Base64 编码的AES密钥
     * @return 解密后的明文
     */
    public static String decryptByAes(String encryptData, String aesKey) {
        byte[] keyBytes = Base64.decode(aesKey);
        byte[] ivAndEncryptBytes = Base64.decode(encryptData);
        //密文头部16位是IV, 其余部分是真正的密文
        byte[] ivBytes = ArrayUtil.sub(ivAndEncryptBytes, 0, 16);
        byte[] encryptBytes = ArrayUtil.sub(ivAndEncryptBytes, 16, ivAndEncryptBytes.length);
        AES aes = new AES(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);
        return aes.decryptStr(encryptBytes, StandardCharsets.UTF_8);
    }


    /**
     * Base64编码
     * @param text 要编码的数据
     * @return 编码后的数据
     */
    public static String base64Encode(String text) {
        return Base64.encode(text);
    }

    /**
     * Base64解码
     * @param text 要解码的数据
     * @return 解码后的数据
     */
    public static String base64Decode(String text) {
        return Base64.decodeStr(text);
    }


    static void main() {
        Map<String, String> keys = createRsaKey();
        //公钥
        String publicKey = keys.get("publicKey");
        //私钥
        String privateKey = keys.get("privateKey");
        System.out.println("==============公钥+私钥===============");
        System.out.println("公钥: "+publicKey);
        System.out.println("私钥: "+privateKey);
        System.out.println("==============公钥+私钥===============");

        //待加密的数据
        String data="123456";
        System.out.println("待加密的数据:"+data);
        //公钥加密
        String encryptData = encryptByPublicKey(data, publicKey);
        System.out.println("加密后的数据:"+encryptData);

        //私钥解密
        String decryptData = decryptByPrivateKey(encryptData, privateKey);
        System.out.println("解密后的数据:"+decryptData);

        //AES对称加密
        String aesKey = createAesKey();
        System.out.println("==============AES对称加密===============");
        System.out.println("AES密钥: "+aesKey);
        String encryptDataByAes = encryptByAes(data, aesKey);
        System.out.println("AES加密后的数据:"+encryptDataByAes);
        System.out.println("AES解密后的数据:"+decryptByAes(encryptDataByAes, aesKey));
        System.out.println("==============AES对称加密===============");
    }
}
