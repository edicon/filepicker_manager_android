package io.filepicker.manager.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by maciejwitowski on 12/12/14.
 */
public enum Operation {
    SAVE, EXPORT, SHARE;

    private static final Map<String, Operation> stringToEnum = new HashMap<>();

    static {
        for(Operation op : values())
            stringToEnum.put(op.toString(), op);
    }

    public static Operation fromString(String name) {
        return stringToEnum.get(name);
    }
}