package com.avermak.vkube.balance;

import java.util.HashMap;

public class ChartNodeColors {
    private static final String[] BAR_COLORS = {"FF4444", "00AF05", "0076CF", "CF6300", "A600CF", "009ACF"};
    private static final HashMap<String, String> barColors = new HashMap<>();
    /**
     * @return #rrggbb
     */
    public static synchronized String getBarColor(String nodeName) {
        String color = barColors.get(nodeName);
        if (color == null) {
            color = BAR_COLORS[barColors.size() % BAR_COLORS.length];
            barColors.put(nodeName, color);
        }
        return color;
    }
}
