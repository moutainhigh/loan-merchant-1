package com.mod.loan.util;

/**
 * String字符串的替换，以到达保密效果
 *
 * Created by lijy on 2017/7/31.
 */
public class StringReplaceUtil {
    /**
     * 根据用户名的不同长度，来进行替换 ，达到保密效果
     *
     * @param userName 用户名
     * @return 替换后的用户名
     */
    public static String userNameReplaceWithStar(String userName) {
        String userNameAfterReplaced = "";

        if (userName == null){
            userName = "";
        }

        int nameLength = userName.length();

        if (nameLength <= 1) {
            userNameAfterReplaced = "*";
        } else if (nameLength == 2) {
            userNameAfterReplaced = replaceAction(userName, "(?<=\\S{0})\\S(?=\\S{1})");
        } else if (nameLength >= 3 && nameLength <= 6) {
            userNameAfterReplaced = replaceAction(userName, "(?<=\\S{1})\\S(?=\\S{1})");
        } else if (nameLength == 7) {
            userNameAfterReplaced = replaceAction(userName, "(?<=\\S{1})\\S(?=\\S{2})");
        } else if (nameLength == 8) {
            userNameAfterReplaced = replaceAction(userName, "(?<=\\S{2})\\S(?=\\S{2})");
        } else if (nameLength == 9) {
            userNameAfterReplaced = replaceAction(userName, "(?<=\\S{2})\\S(?=\\S{3})");
        } else if (nameLength == 10) {
            userNameAfterReplaced = replaceAction(userName, "(?<=\\S{3})\\S(?=\\S{3})");
        } else if (nameLength >= 11) {
            userNameAfterReplaced = replaceAction(userName, "(?<=\\S{3})\\S(?=\\S{4})");
        }

        return userNameAfterReplaced;

    }

    /**
     * 实际替换动作
     *
     * @param username username
     * @param regular  正则
     * @return
     */
    private static String replaceAction(String username, String regular) {
        return username.replaceAll(regular, "*");
    }

    /**
     * 身份证号替换，保留前四位和后四位
     *
     * 如果身份证号为空 或者 null ,返回null ；否则，返回替换后的字符串；
     *
     * @param idCard 身份证号
     * @return
     */
    public static String idCardReplaceWithStar(String idCard) {

        if (idCard.isEmpty() || idCard == null) {
            return null;
        } else {
            return replaceAction(idCard, "(?<=\\d{4})\\d(?=\\d{4})");
        }
    }

    /**
     * 银行卡替换，保留后四位
     *
     * 如果银行卡号为空 或者 null ,返回null ；否则，返回替换后的字符串；
     *
     * @param bankCard 银行卡号
     * @return
     */
    public static String bankCardReplaceWithStar(String bankCard) {

        if (bankCard.isEmpty() || bankCard == null) {
            return null;
        } else {
            return replaceAction(bankCard, "(?<=\\d{0})\\d(?=\\d{4})");
        }
    }

    /**
     *
     *
     * @param phone
     * @return
     */
    public static String phoneReplaceWithStar(String phone) {
        if (phone.isEmpty() || phone == null) {
            return null;
        } else {
            return replaceAction(phone, "(?<=\\d{3})\\d(?=\\d{4})");
        }
    }
}
