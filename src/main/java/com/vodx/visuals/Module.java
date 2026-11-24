package com.vodx.visuals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Module {
    public final String id, name, description;
    public final Category category;
    public final String[] keywords;
    public final List<Setting> settings = new ArrayList<>();
    public boolean enabled;

    // GUI state
    public boolean expanded;
    public float anim, hover, expandAnim;

    public Module(String id, String name, Category category, String description, boolean def, String... keywords) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = def;
        this.keywords = keywords;
    }

    public <S extends Setting> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public void toggle() {
        enabled = !enabled;
    }

    /** Used by the GUI search box: matches name, description, category, keywords and setting names. */
    public boolean matches(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return true;
        for (String part : q.split("\\s+")) {
            if (!matchesPart(part)) return false;
        }
        return true;
    }

    private boolean matchesPart(String q) {
        if (name.toLowerCase(Locale.ROOT).contains(q)) return true;
        if (id.toLowerCase(Locale.ROOT).contains(q)) return true;
        if (description.toLowerCase(Locale.ROOT).contains(q)) return true;
        if (category.label.toLowerCase(Locale.ROOT).contains(q)) return true;
        for (String k : keywords) if (k.toLowerCase(Locale.ROOT).contains(q)) return true;
        for (Setting s : settings) if (s.name.toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }
}
