package com.polarnick.vrp.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.StringTokenizer;

/**
 * @author Polyarnyi Nickolay, PolarNick239
 */
public class Commons {

    public static int parseSeconds(String hhmmss) {
        StringTokenizer tok = new StringTokenizer(hhmmss, ":");
        int hh = Integer.parseInt(tok.nextToken());
        int mm = Integer.parseInt(tok.nextToken());
        int ss = Integer.parseInt(tok.nextToken());
        return hh * 60 * 60 + mm * 60 + ss;
    }

    public static <T> T getMaximum(Collection<T> collection, Comparator<T> comparator) {
        T max = null;
        for (T x: collection) {
            if (max == null || comparator.compare(x, max) >= 0) {
                max = x;
            }
        }
        return max;
    }

}
