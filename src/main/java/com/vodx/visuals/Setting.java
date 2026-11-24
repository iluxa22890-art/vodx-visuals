package com.vodx.visuals;

import java.util.Locale;
import net.minecraft.util.math.MathHelper;

public abstract class Setting {
    public final String name;

    protected Setting(String name) {
        this.name = name;
    }

    public abstract String serialize();

    public abstract void deserialize(String s);

    public static class Bool extends Setting {
        public boolean value;

        public Bool(String name, boolean def) {
            super(name);
            this.value = def;
        }

        @Override public String serialize() { return Boolean.toString(value); }
        @Override public void deserialize(String s) { value = Boolean.parseBoolean(s); }
    }

    public static class Num extends Setting {
        public final double min, max, step;
        public double value;

        public Num(String name, double min, double max, double step, double def) {
            super(name);
            this.min = min;
            this.max = max;
            this.step = step;
            this.value = def;
        }

        public void set(double v) {
            v = Math.round((v - min) / step) * step + min;
            value = MathHelper.clamp(v, min, max);
        }

        public float f() { return (float) value; }
        public int i() { return (int) Math.round(value); }

        public String display() {
            return step >= 1 ? Integer.toString(i()) : String.format(Locale.ROOT, "%.2f", value);
        }

        @Override public String serialize() { return Double.toString(value); }
        @Override public void deserialize(String s) {
            try { set(Double.parseDouble(s)); } catch (NumberFormatException ignored) { }
        }
    }

    public static class Mode extends Setting {
        public final String[] modes;
        public int index;

        public Mode(String name, int def, String... modes) {
            super(name);
            this.modes = modes;
            this.index = def;
        }

        public String get() { return modes[index]; }
        public void next() { index = (index + 1) % modes.length; }
        public void prev() { index = (index - 1 + modes.length) % modes.length; }

        @Override public String serialize() { return get(); }
        @Override public void deserialize(String s) {
            for (int i = 0; i < modes.length; i++) {
                if (modes[i].equalsIgnoreCase(s)) { index = i; return; }
            }
        }
    }
}
