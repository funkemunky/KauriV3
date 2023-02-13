package dev.brighten.ac.utils;

import java.util.HashSet;
import java.util.Set;

public class TagsBuilder {
    private final Set<String> tags = new HashSet<>();

    public TagsBuilder addTag(String string) {
        tags.add(string);

        return this;
    }

    public String build() {
        return String.join(", ", tags);
    }

    public int getSize() {
        return tags.size();
    }

    public boolean containsTag(String tag) {
        return tags.contains(tag);
    }
}