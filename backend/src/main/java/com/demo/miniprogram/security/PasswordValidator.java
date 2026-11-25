package com.demo.miniprogram.security;

/**
 * 密码强度校验器
 *
 * 规则：
 *   1. 长度至少8位
 *   2. 必须包含大写字母、小写字母、数字中至少两种
 *   3. 不能是常见弱密码
 *
 * 在创建/修改管理员密码时调用：
 *   if (!PasswordValidator.isStrong(password)) {
 *       throw new BusinessException("密码强度不足: " + PasswordValidator.getRequirement());
 *   }
 *
 * @author demo
 */
public class PasswordValidator {

    /** 常见弱密码黑名单 */
    private static final String[] WEAK_PASSWORDS = {
            "12345678", "password", "admin123", "11111111",
            "00000000", "123456789", "qwertyui", "aaaaaaaa"
    };

    /**
     * 密码强度要求说明（给前端展示）
     */
    public static String getRequirement() {
        return "密码至少8位，需包含大写字母、小写字母、数字中至少两种，不能使用常见弱密码";
    }

    /**
     * 校验密码是否足够强
     *
     * @param password 明文密码
     * @return true=合格，false=太弱
     */
    public static boolean isStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        // 检查是否在弱密码黑名单中
        for (String weak : WEAK_PASSWORDS) {
            if (weak.equalsIgnoreCase(password)) {
                return false;
            }
        }

        // 统计字符类型数量
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }

        // 至少包含两种字符类型
        int types = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0);
        return types >= 2;
    }
}
