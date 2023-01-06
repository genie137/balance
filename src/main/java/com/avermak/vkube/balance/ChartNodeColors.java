package com.avermak.vkube.balance;

import java.util.HashMap;

public class ChartNodeColors {
    private static final String[] BAR_COLORS = {"FF4444", "00CF05", "0076CF", "CF6300", "A600CF", "009ACF"};
    private static HashMap<String, String> barColors = new HashMap<>();
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
