package com.jaba.awr_3.core.globalvar;

import java.util.concurrent.atomic.AtomicInteger;

public class GlobalRight {

    private static boolean RightAddOpWagon_1;
    private static boolean RightAddOpWagon_2;
    private static boolean RightAddOpWagon_3;
    private static boolean RightAddOpWagon_4;
    private static boolean RightAddOpWagon_5;
    private static boolean RightAddOpWagon_6;

    private static boolean itWorks_1 = false;
    private static boolean itWorks_2 = false;
    private static boolean itWorks_3 = false;
    private static boolean itWorks_4 = false;
    private static boolean itWorks_5 = false;
    private static boolean itWorks_6 = false;

    // თითოეული პორტისთვის ცალ-ცალკე sequenceId და suffix (ჰექსადეციმალური
    // კოუნტერები)
    private static final AtomicInteger sequenceId_0 = new AtomicInteger(0x02);
    private static final AtomicInteger sequenceId_1 = new AtomicInteger(0x02);
    private static final AtomicInteger sequenceId_2 = new AtomicInteger(0x02);
    private static final AtomicInteger sequenceId_3 = new AtomicInteger(0x02);
    private static final AtomicInteger sequenceId_4 = new AtomicInteger(0x02);
    private static final AtomicInteger sequenceId_5 = new AtomicInteger(0x02);
    private static final AtomicInteger sequenceId_6 = new AtomicInteger(0x02);
    private static final AtomicInteger sequenceId_7 = new AtomicInteger(0x02);
    private static final AtomicInteger sequenceId_8 = new AtomicInteger(0x02);
    private static final AtomicInteger sequenceId_9 = new AtomicInteger(0x02);

    private static final AtomicInteger suffix_0 = new AtomicInteger(0x2A);
    private static final AtomicInteger suffix_1 = new AtomicInteger(0x2A);
    private static final AtomicInteger suffix_2 = new AtomicInteger(0x2A);
    private static final AtomicInteger suffix_3 = new AtomicInteger(0x2A);
    private static final AtomicInteger suffix_4 = new AtomicInteger(0x2A);
    private static final AtomicInteger suffix_5 = new AtomicInteger(0x2A);
    private static final AtomicInteger suffix_6 = new AtomicInteger(0x2A);
    private static final AtomicInteger suffix_7 = new AtomicInteger(0x2A);
    private static final AtomicInteger suffix_8 = new AtomicInteger(0x2A);
    private static final AtomicInteger suffix_9 = new AtomicInteger(0x2A);



    private static final int MIN = 0x00;
    private static final int MAX = 0xFF;

    // ==================== Sequence ID getters ====================

    public static String getSequenceIdHex_0() {
        return getNextSequenceId(sequenceId_0);
    }

    public static String getSequenceIdHex_1() {
        return getNextSequenceId(sequenceId_1);
    }

    public static String getSequenceIdHex_2() {
        return getNextSequenceId(sequenceId_2);
    }

    public static String getSequenceIdHex_3() {
        return getNextSequenceId(sequenceId_3);
    }

    public static String getSequenceIdHex_4() {
        return getNextSequenceId(sequenceId_4);
    }

    public static String getSequenceIdHex_5() {
        return getNextSequenceId(sequenceId_5);
    }

    public static String getSequenceIdHex_6() {
        return getNextSequenceId(sequenceId_6);
    }
    public static String getSequenceIdHex_7() {
        return getNextSequenceId(sequenceId_7);
    }
    public static String getSequenceIdHex_8() {
        return getNextSequenceId(sequenceId_8);
    }
    public static String getSequenceIdHex_9() {
        return getNextSequenceId(sequenceId_9);
    }



    // ==================== Suffix getters ====================

    public static String getSuffixHex_0() {
        return getNextSuffix(suffix_0);
    }

    public static String getSuffixHex_1() {
        return getNextSuffix(suffix_1);
    }

    public static String getSuffixHex_2() {
        return getNextSuffix(suffix_2);
    }

    public static String getSuffixHex_3() {
        return getNextSuffix(suffix_3);
    }

    public static String getSuffixHex_4() {
        return getNextSuffix(suffix_4);
    }

    public static String getSuffixHex_5() {
        return getNextSuffix(suffix_5);
    }

    public static String getSuffixHex_6() {
        return getNextSuffix(suffix_6);
    }
    public static String getSuffixHex_7() {
        return getNextSuffix(suffix_7);
    }
    public static String getSuffixHex_8() {
        return getNextSuffix(suffix_8);
    }
    public static String getSuffixHex_9() {
        return getNextSuffix(suffix_9);
    }

    // ==================== Helper methods ====================

    private static String getNextSequenceId(AtomicInteger counter) {
        int next = counter.incrementAndGet(); // +1 და აბრუნებს ახალ მნიშვნელობას
        if (next > MAX) {
            counter.set(MIN);
            next = MIN;
        }
        return String.format("%02X", next);
    }

    private static String getNextSuffix(AtomicInteger counter) {
        int next = counter.decrementAndGet(); // -1 და აბრუნებს ახალ მნიშვნელობას
        if (next < MIN) {
            counter.set(MAX);
            next = MAX;
        }
        return String.format("%02X", next);
    }

    // ==================== Existing methods (unchanged) ====================

    public static void setRightAddOpWagon(String conId, boolean right) {
        switch (conId) {
            case "ttyUSB0" -> RightAddOpWagon_1 = right;
            case "ttyUSB1" -> RightAddOpWagon_2 = right;
            case "TCP_1" -> RightAddOpWagon_3 = right;
            case "TCP_2" -> RightAddOpWagon_4 = right;
            case "TCP_3" -> RightAddOpWagon_5 = right;
            case "TCP_4" -> RightAddOpWagon_6 = right;
        }
    }

    public static boolean getRightAddOpWagon(String conId) {
        return switch (conId) {
            case "ttyUSB0" -> RightAddOpWagon_1;
            case "ttyUSB1" -> RightAddOpWagon_2;
            case "TCP_1" -> RightAddOpWagon_3;
            case "TCP_2" -> RightAddOpWagon_4;
            case "TCP_3" -> RightAddOpWagon_5;
            case "TCP_4" -> RightAddOpWagon_6;
            default -> false;
        };
    }

    public static void setWorks(String conId, boolean right) {
        switch (conId) {
            case "ttyUSB0" -> itWorks_1 = right;
            case "ttyUSB1" -> itWorks_2 = right;
            case "TCP_1" -> itWorks_3 = right;
            case "TCP_2" -> itWorks_4 = right;
            case "TCP_3" -> itWorks_5 = right;
            case "TCP_4" -> itWorks_6 = right;
        }
    }

    public static boolean getWorks(String conId) {
        return switch (conId) {
            case "ttyUSB0" -> itWorks_1;
            case "ttyUSB1" -> itWorks_2;
            case "TCP_1" -> itWorks_3;
            case "TCP_2" -> itWorks_4;
            case "TCP_3" -> itWorks_5;
            case "TCP_4" -> itWorks_6;
            default -> false;
        };
    }
}