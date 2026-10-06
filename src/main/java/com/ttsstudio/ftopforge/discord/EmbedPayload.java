package com.ttsstudio.ftopforge.discord;

import java.util.ArrayList;
import java.util.List;

public final class EmbedPayload {
    public String title;
    public int color;
    public String thumbnailUrl;
    public String footer;
    public String timestampIso;
    public List<Field> fields = new ArrayList<>();
    public String content;  // mention text (e.g., "<@&123>"), null if none

    public static final class Field {
        public String name;
        public String value;
        public boolean inline;
    }
}
