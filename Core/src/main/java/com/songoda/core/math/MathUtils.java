package com.songoda.core.math;

import com.songoda.core.CraftaroCoreConstants;

import java.util.HashMap;
import java.util.Map;

public class MathUtils {
    private static final Map<String, Double> CACHE = new HashMap<>();

    public static double eval(String toParse) {
        return eval(toParse, CraftaroCoreConstants.getProjectName() + " Eval Engine");
    }

    public static double eval(String toParse, String warningMessage) {
        return CACHE.computeIfAbsent(toParse, t -> new Eval(toParse, warningMessage).parse());
    }
}
