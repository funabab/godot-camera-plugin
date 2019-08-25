package org.godotengine.godot.funabab.camera;

import android.graphics.Rect;

import java.util.HashMap;

public class ParameterSerializer {
    public static HashMap<String, Object> unSerialize(String config) {
        final HashMap<String, Object> result = new HashMap<>();
        final String split[] = config.split(";");

        for (String data : split) {
            final String[] dataSplit = data.split("=");
            if (dataSplit.length != 2)
                continue;
            final String key = dataSplit[0].trim();
            final String value = dataSplit[1].toLowerCase().trim();

            if (value.equals("on") || value.equals("off")) {
                result.put(key, value.equals("on"));
            } else if (value.startsWith("'")) {
                result.put(key, value.replace("'", ""));
            } else if (value.split(",").length == 4) {
                final String valueSplit[] = value.split(",");
                try {
                    final int x = Integer.valueOf(valueSplit[0]);
                    final int y = Integer.valueOf(valueSplit[1]);
                    final int w = Integer.valueOf(valueSplit[2]);
                    final int h = Integer.valueOf(valueSplit[3]);

                    result.put(key, new Rect(x, y, x+w, y+h));
                } catch (Exception e) {}
            } else {
                try {
                    result.put(key, Integer.valueOf(value));
                } catch (Exception e) {
                }
            }
        }
        return result;
    }

    public static String serialize(HashMap<String, Object> config) {
        final StringBuilder stringBuilder = new StringBuilder();
        int count = 0;
        for (String key : config.keySet()) {
            final Object value = config.get(key);
            String data;
            if (value instanceof String) {
                data = "'" + value + "'";
            } else if (value instanceof Boolean) {
                data = (boolean) value ? "on" : "off";
            } else if (value instanceof Rect) {
                final Rect rect = (Rect) value;
                data = rect.left + "," + rect.top + "," + rect.width() + "," + rect.height();
            } else if (value instanceof Integer) {
                data = String.valueOf(value);
            } else {
                continue;
            }
            stringBuilder.append((count > 0 ? ";" : "") + key + "=" + data);
            count++;
        }
        return stringBuilder.toString();
    }
}
