package me.dhamith.filebeam.helpers;

import java.security.SecureRandom;

public class Keygen {
    public static String generate() {
        SecureRandom rand = new SecureRandom();
        return generateNLengthWord(rand, 4) + "-" + generateNLengthWord(rand, 4) + "-" + generateNLengthWord(rand, 6);
    }

    private static String generateNLengthWord(SecureRandom rand, int n) {
        String word = Assets.words[rand.nextInt(Assets.words.length) - 1];
        if (word.length() == n) {
            return word;
        }
        return generateNLengthWord(rand, n);
    }
}
