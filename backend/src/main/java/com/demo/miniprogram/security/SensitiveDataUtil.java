package com.demo.miniprogram.security;

/**
 * 敏感数据脱敏工具
 *
 * 20w用户的隐私保护底线：
 *   - 手机号：日志中绝不出现完整号码
 *   - 密码：绝不在任何地方打印
 *   - OpenID：生产环境日志中脱敏（开发环境保留方便调试）
 *   - 姓名：非必要不记录
 *
 * 使用示例：
 *   log.info("用户登录: phone={}", SensitiveDataUtil.maskPhone("13812341234"));
 *   // 输出: 用户登录: phone=138****1234
 *
 * @author demo
 */
public class SensitiveDataUtil {

    /**
     * 手机号脱敏：保留前3位和后4位，中间用****替代
     *
     * 示例：13812341234 → 138****1234
     *      1381234      → 138****（不足7位全打码）
     *
     * @param phone 原始手机号（可能为null）
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "***";
        }
        if (phone.length() < 7) {
            return phone.charAt(0) + "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 姓名脱敏：只保留姓氏，其余用**替代
     *
     * 示例：张三 → 张*
     *      欧阳锋 → 欧**
     *      A → *
     *
     * @param name 原始姓名
     * @return 脱敏后的姓名
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "***";
        }
        if (name.length() == 1) {
            return "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 微信号脱敏：只保留首尾字符
     *
     * @param wechatId 微信号
     * @return 脱敏后的微信号
     */
    public static String maskWechatId(String wechatId) {
        if (wechatId == null || wechatId.isEmpty()) {
            return "***";
        }
        if (wechatId.length() <= 3) {
            return wechatId.charAt(0) + "***";
        }
        return wechatId.substring(0, 2) + "****" + wechatId.substring(wechatId.length() - 2);
    }

    /**
     * Token脱敏（用于日志中打印Token时）
     * 只保留前10个字符
     */
    public static String maskToken(String token) {
        if (token == null || token.length() <= 10) {
            return "***";
        }
        return token.substring(0, 10) + "...";
    }
}
