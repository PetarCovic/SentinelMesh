package com.sentinelmesh.simulator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonFieldExtractor 
{

    public static String extractStringField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            throw new IllegalStateException("Could not extract field '" + fieldName + "' from response: " + json);
        }

        return matcher.group(1);
    }
}