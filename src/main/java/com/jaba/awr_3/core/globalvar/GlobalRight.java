package com.jaba.awr_3.core.globalvar;

import java.util.concurrent.atomic.AtomicInteger;

public class GlobalRight {

    // თითოეული პორტისთვის ცალ-ცალკე sequence counter
    private static final AtomicInteger sequenceId_0 = new AtomicInteger(0x01);
    private static final AtomicInteger sequenceId_1 = new AtomicInteger(0x01);
    private static final AtomicInteger sequenceId_2 = new AtomicInteger(0x01);
    private static final AtomicInteger sequenceId_3 = new AtomicInteger(0x01);
    private static final AtomicInteger sequenceId_4 = new AtomicInteger(0x01);
    private static final AtomicInteger sequenceId_5 = new AtomicInteger(0x01);
    private static final AtomicInteger sequenceId_6 = new AtomicInteger(0x01);
    private static final AtomicInteger sequenceId_7 = new AtomicInteger(0x01);
    private static final AtomicInteger sequenceId_8 = new AtomicInteger(0x01);
    private static final AtomicInteger sequenceId_9 = new AtomicInteger(0x01);

    private static final int START = 0x02;
    private static final int RESET_VALUE = 0x01;
    private static final int MAX = 0xFF;

    private static final int[] CSTART_BASE = {
            0x6C, 0x6B, 0x6A, 0x69,
            0x68, 0x67, 0x66, 0x65,
            0x64, 0x63, 0x5B, 0x5A,
            0x59, 0x58, 0x57, 0x56
    };

    private static final int[] CWSTATE_BASE = {
            0x2C, 0x2B, 0x2A, 0x29,
            0x28, 0x27, 0x26, 0x25,
            0x24, 0x23, 0x1B, 0x1A,
            0x19, 0x18, 0x17, 0x16
    };

    private static final int[] CABORT_BASE = {
            0x81, 0x80, 0x7F, 0x7E,
            0x7D, 0x7C, 0x7B, 0x7A,
            0x79, 0x78, 0x70, 0x6F,
            0x6E, 0x6D, 0x6C, 0x6B
    };

    // ==================== Sequence ID getters ====================
    public static String getSequenceIdHex_0() { return getNextSequenceId(sequenceId_0); }
    public static String getSequenceIdHex_1() { return getNextSequenceId(sequenceId_1); }
    public static String getSequenceIdHex_2() { return getNextSequenceId(sequenceId_2); }
    public static String getSequenceIdHex_3() { return getNextSequenceId(sequenceId_3); }
    public static String getSequenceIdHex_4() { return getNextSequenceId(sequenceId_4); }
    public static String getSequenceIdHex_5() { return getNextSequenceId(sequenceId_5); }
    public static String getSequenceIdHex_6() { return getNextSequenceId(sequenceId_6); }
    public static String getSequenceIdHex_7() { return getNextSequenceId(sequenceId_7); }
    public static String getSequenceIdHex_8() { return getNextSequenceId(sequenceId_8); }
    public static String getSequenceIdHex_9() { return getNextSequenceId(sequenceId_9); }

    // ==================== ბრძანებების getters ====================
    public static String getCSTART_0()  { return buildCSTART(sequenceId_0); }
    public static String getCSTART_1()  { return buildCSTART(sequenceId_1); }
    public static String getCSTART_2()  { return buildCSTART(sequenceId_2); }
    public static String getCSTART_3()  { return buildCSTART(sequenceId_3); }
    public static String getCSTART_4()  { return buildCSTART(sequenceId_4); }
    public static String getCSTART_5()  { return buildCSTART(sequenceId_5); }
    public static String getCSTART_6()  { return buildCSTART(sequenceId_6); }
    public static String getCSTART_7()  { return buildCSTART(sequenceId_7); }
    public static String getCSTART_8()  { return buildCSTART(sequenceId_8); }
    public static String getCSTART_9()  { return buildCSTART(sequenceId_9); }

    public static String getCABORT_0()  { return buildCABORT(sequenceId_0); }
    public static String getCABORT_1()  { return buildCABORT(sequenceId_1); }
    public static String getCABORT_2()  { return buildCABORT(sequenceId_2); }
    public static String getCABORT_3()  { return buildCABORT(sequenceId_3); }
    public static String getCABORT_4()  { return buildCABORT(sequenceId_4); }
    public static String getCABORT_5()  { return buildCABORT(sequenceId_5); }
    public static String getCABORT_6()  { return buildCABORT(sequenceId_6); }
    public static String getCABORT_7()  { return buildCABORT(sequenceId_7); }
    public static String getCABORT_8()  { return buildCABORT(sequenceId_8); }
    public static String getCABORT_9()  { return buildCABORT(sequenceId_9); }

    public static String getCWSTATE_0() { return buildCWSTATE(sequenceId_0); }
    public static String getCWSTATE_1() { return buildCWSTATE(sequenceId_1); }
    public static String getCWSTATE_2() { return buildCWSTATE(sequenceId_2); }
    public static String getCWSTATE_3() { return buildCWSTATE(sequenceId_3); }
    public static String getCWSTATE_4() { return buildCWSTATE(sequenceId_4); }
    public static String getCWSTATE_5() { return buildCWSTATE(sequenceId_5); }
    public static String getCWSTATE_6() { return buildCWSTATE(sequenceId_6); }
    public static String getCWSTATE_7() { return buildCWSTATE(sequenceId_7); }
    public static String getCWSTATE_8() { return buildCWSTATE(sequenceId_8); }
    public static String getCWSTATE_9() { return buildCWSTATE(sequenceId_9); }

    public static String getREOTD_0() { return buildREOTD1EB2(sequenceId_0); }
    public static String getREOTD_1() { return buildREOTD1EB2(sequenceId_1); }
    public static String getREOTD_2() { return buildREOTD1EB2(sequenceId_2); }
    public static String getREOTD_3() { return buildREOTD1EB2(sequenceId_3); }
    public static String getREOTD_4() { return buildREOTD1EB2(sequenceId_4); }
    public static String getREOTD_5() { return buildREOTD1EB2(sequenceId_5); }
    public static String getREOTD_6() { return buildREOTD1EB2(sequenceId_6); }
    public static String getREOTD_7() { return buildREOTD1EB2(sequenceId_7); }
    public static String getREOTD_8() { return buildREOTD1EB2(sequenceId_8); }
    public static String getREOTD_9() { return buildREOTD1EB2(sequenceId_9); }

    // ==================== Helper methods ====================

    private static String getNextSequenceId(AtomicInteger counter) {
        int next = nextSequence(counter);
        return String.format("%02X", next);
    }

    private static int nextSequence(AtomicInteger counter) {
        int current = counter.get();

        if (current >= MAX) {
            counter.set(RESET_VALUE);
            return START;
        }

        return counter.incrementAndGet();
    }

    private static int hi(int seq) {
        return (seq >> 4) & 0x0F;
    }

    private static int lo(int seq) {
        return seq & 0x0F;
    }

    private static int calcTail(int seq, int[] baseTable) {
        return (baseTable[lo(seq)] - hi(seq)) & 0xFF;
    }

    private static int calcReotdTail(int seq) {
        String body = String.format("%02XREOTD1EB2", seq);
        int sum = 0;

        for (int i = 0; i < body.length(); i++) {
            sum = (sum + body.charAt(i)) & 0xFF;
        }

        return (0xFE - sum) & 0xFF;
    }

    // ==================== Command builders ====================

    private static String buildCSTART(AtomicInteger seqCounter) {
        int seq = nextSequence(seqCounter);
        int tail = calcTail(seq, CSTART_BASE);
        return String.format("%02XCSTART7C34%02X", seq, tail);
    }

    private static String buildCABORT(AtomicInteger seqCounter) {
        int seq = nextSequence(seqCounter);
        int tail = calcTail(seq, CABORT_BASE);
        return String.format("%02XCABORT933C%02X", seq, tail);
    }

    private static String buildCWSTATE(AtomicInteger seqCounter) {
        int seq = nextSequence(seqCounter);
        int tail = calcTail(seq, CWSTATE_BASE);
        return String.format("%02XCWSTATE7619%02X", seq, tail);
    }

    private static String buildREOTD1EB2(AtomicInteger seqCounter) {
        int seq = nextSequence(seqCounter);
        int tail = calcReotdTail(seq);
        return String.format("%02XREOTD1EB2%02X", seq, tail);
    }
}
