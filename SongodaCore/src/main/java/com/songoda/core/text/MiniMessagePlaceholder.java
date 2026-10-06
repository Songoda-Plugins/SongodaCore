package com.songoda.core.text;

import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.util.List;

public class MiniMessagePlaceholder {

    @Getter
    private final String placeholder;
    @Getter
    private final Object value;

    public MiniMessagePlaceholder(String placeholder, String value) {
        this.placeholder = placeholder;
        this.value = value != null ? AdventureUtils.replaceLegacy(value) : "";
    }

    public MiniMessagePlaceholder(String placeholder, Number value) {
        this.placeholder = placeholder;
        this.value = value != null ? String.valueOf(value) : "";
    }

    public MiniMessagePlaceholder(String placeholder, Component value) {
        this.placeholder = placeholder;
        this.value = value != null ? value : Component.empty();
    }

    public String replace(String string) {
        return string.replace("<" + this.placeholder + ">", this.value.toString());
    }

    public List<String> replace(List<String> list) {
        List<String> newList = new java.util.ArrayList<>();
        for (String line : list) {
            newList.add(replace(line));
        }
        return newList;
    }

    public boolean isComponent() {
        return value instanceof Component;
    }

    @Override
    public String toString() {
        return "MiniMessagePlaceholder{" +
                "placeholder='" + placeholder + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
