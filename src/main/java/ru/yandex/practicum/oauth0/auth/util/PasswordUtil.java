package ru.yandex.practicum.oauth0.auth.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    public static String hashPassword(String plainPassword) {
        String salt = BCrypt.gensalt(12);
        return BCrypt.hashpw(plainPassword, salt);
    }

    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    public static void main(String[] args) {
        String[] passwords = {"pass", "secret", "svc-secret"};
        for (String pwd : passwords) {
            System.out.println(pwd + " -> " + hashPassword(pwd));
        }
    }
}