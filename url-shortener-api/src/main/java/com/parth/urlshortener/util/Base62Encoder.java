package com.parth.urlshortener.util;

public class Base62Encoder {

    private static final String BASE62 =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String encode(Long number) {

        StringBuilder sb = new StringBuilder();

        while (number > 0) {
            sb.append(BASE62.charAt((int) (number % 62)));
            number /= 62;
        }

        return sb.reverse().toString();
    }
}