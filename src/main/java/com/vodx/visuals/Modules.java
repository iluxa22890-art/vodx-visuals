package com.vodx.visuals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class Modules {
    public static final List<Module> ALL = new ArrayList<>();

    private static Module reg(Module m) {
        ALL.add(m);
        return m;
    }

    // ---------------------------------------------------------------- HUD
    public static final Module WATERMARK = reg(new Module("Watermark", "Watermark", Category.HUD,
            "Client badge with nickname and FPS", true, "logo", "fps", "name"));
    public static final Module MODULE_LIST = reg(new Module("ModuleList", "Module List", Category.HUD,
            "List of enabled functions on the right", true, "arraylist", "active"));
    public static final Module INFO = reg(new Module("Info", "Info", Category.HUD,
            "Coordinates and ping", true, "coords", "xyz", "ping"));
    public static final Module SPEED = reg(new Module("Speed", "Speed", Category.HUD,
            "Current movement speed in m/s", true, "bps", "velocity"));
    public static final Module KEYSTROKES = reg(new Module("Keystrokes", "Keystrokes", Category.HUD,
            "WASD keys and CPS counter", false, "wasd", "cps", "keys", "clicks"));
    public static final Module ARMOR_HUD = reg(new Module("ArmorHud", "Armor HUD", Category.HUD,
            "Armor pieces with durability", false, "armor", "durability", "items"));
    public static final Module POTION_HUD = reg(new Module("PotionHud", "Potion HUD", Category.HUD,
            "Active effects with timers", false, "potions", "effects", "status"));
    public static final Module CLOCK = reg(new Module("Clock", "Clock", Category.HUD,
            "Real time at the top of the screen", false, "time"));
    public static final Module TARGET_HUD = reg(new Module("TargetHUD", "Target HUD", Category.HUD,
            "Name and animated health bar of your target", true, "health", "enemy", "target"));
    public static final Module HEALTH_ALERT = reg(new Module("HealthAlert", "Health Alert", Category.HUD,
            "Pulsing red screen edges on low health", true, "low hp", "warning", "vignette"));
    public static final Module CROSSHAIR = reg(new Module("CustomCrosshair", "Custom Crosshair", Category.HUD,
            "Replaces the vanilla crosshair", true, "aim", "reticle"));

    public static final Setting.Num CROSSHAIR_GAP = CROSSHAIR.add(new Setting.Num("Gap", 0, 10, 1, 3));
    public static final Setting.Num CROSSHAIR_SIZE = CROSSHAIR.add(new Setting.Num("Size", 2, 14, 1, 5));
    public static final Setting.Bool CROSSHAIR_DOT = CROSSHAIR.add(new Setting.Bool("Center dot", true));
    public static final Setting.Num ALERT_HEALTH = HEALTH_ALERT.add(new Setting.Num("Health below", 2, 12, 1, 6));

    // ------------------------------------------------------------- Combat
    public static final Module AIM_ASSIST = reg(new Module("AimAssist", "Aim Assist", Category.COMBAT,
            "Smoothly turns the camera to the nearest target", false, "aimbot", "autoaim", "aim", "lock", "target"));
    public static final Setting.Num AIM_RANGE = AIM_ASSIST.add(new Setting.Num("Range", 2, 8, 0.5, 4.5));
    public static final Setting.Num AIM_FOV = AIM_ASSIST.add(new Setting.Num("FOV", 10, 180, 5, 90));
    public static final Setting.Num AIM_SPEED = AIM_ASSIST.add(new Setting.Num("Speed", 0.05, 1.0, 0.05, 0.25));
    public static final Setting.Num AIM_MAXSTEP = AIM_ASSIST.add(new Setting.Num("Max turn per tick", 1, 40, 1, 12));
    public static final Setting.Num AIM_RANDOM = AIM_ASSIST.add(new Setting.Num("Randomness", 0, 2, 0.1, 0.4));
    public static final Setting.Mode AIM_POINT = AIM_ASSIST.add(new Setting.Mode("Aim point", 1, "Head", "Chest", "Body"));
    public static final Setting.Bool AIM_HOLD = AIM_ASSIST.add(new Setting.Bool("Only while attacking", true));
    public static final Setting.Bool AIM_VISIBLE = AIM_ASSIST.add(new Setting.Bool("Visible only", true));
    public static final Setting.Bool AIM_PLAYERS = AIM_ASSIST.add(new Setting.Bool("Players", true));
    public static final Setting.Bool AIM_MOBS = AIM_ASSIST.add(new Setting.Bool("Mobs", false));

    public static final Module KILL_AURA = reg(new Module("KillAura", "Kill Aura", Category.COMBAT,
            "Automatically attacks targets in range", false, "killaura", "aura", "auto attack", "ka", "combat"));
    public static final Setting.Num KA_RANGE = KILL_AURA.add(new Setting.Num("Range", 2, 8, 0.1, 3.0));
    public static final Setting.Mode KA_MODE = KILL_AURA.add(new Setting.Mode("Mode", 0, "Legit", "Blatant", "Multi"));
    public static final Setting.Num KA_MAXTARGETS = KILL_AURA.add(new Setting.Num("Max targets", 1, 8, 1, 4));
    public static final Setting.Num KA_FOV = KILL_AURA.add(new Setting.Num("FOV", 30, 360, 10, 360));
    public static final Setting.Num KA_CPS = KILL_AURA.add(new Setting.Num("CPS", 1, 20, 1, 10));
    public static final Setting.Bool KA_COOLDOWN = KILL_AURA.add(new Setting.Bool("Respect cooldown", true));
    public static final Setting.Bool KA_ROTATE = KILL_AURA.add(new Setting.Bool("Rotate to target", true));
    public static final Setting.Num KA_TURN = KILL_AURA.add(new Setting.Num("Turn speed", 0.1, 1.0, 0.05, 0.5));
    public static final Setting.Mode KA_PRIORITY = KILL_AURA.add(new Setting.Mode("Priority", 0, "Distance", "Health", "Angle"));
    public static final Setting.Bool KA_PLAYERS = KILL_AURA.add(new Setting.Bool("Players", true));
    public static final Setting.Bool KA_MOBS = KILL_AURA.add(new Setting.Bool("Mobs", true));
    public static final Setting.Bool KA_VISIBLE = KILL_AURA.add(new Setting.Bool("Visible only", true));

    public static final Module HITBOXES = reg(new Module("Hitboxes", "Hitboxes", Category.COMBAT,
            "Enlarges enemy hitboxes for your crosshair", false, "hitbox", "expand", "reach", "box", "size"));
    public static final Setting.Num HB_EXPAND = HITBOXES.add(new Setting.Num("Expand", 0, 1.5, 0.05, 0.3));
    public static final Setting.Bool HB_PLAYERS = HITBOXES.add(new Setting.Bool("Players", true));
    public static final Setting.Bool HB_MOBS = HITBOXES.add(new Setting.Bool("Mobs", true));

    public static final Module HIT_SOUND = reg(new Module("HitSound", "Hit Sound", Category.COMBAT,
            "Plays a sound when you hit an entity", false, "sound", "ding", "hit"));
    public static final Setting.Mode HIT_SOUND_TYPE = HIT_SOUND.add(new Setting.Mode("Sound", 0, "Orb", "Arrow", "Crit"));

    // ------------------------------------------------------------ Effects
    public static final Module HIT_PARTICLES = reg(new Module("HitParticles", "Hit Particles", Category.EFFECTS,
            "Particle burst on every hit", true, "crit", "burst", "hit"));
    public static final Setting.Num HIT_AMOUNT = HIT_PARTICLES.add(new Setting.Num("Amount", 5, 60, 1, 20));
    public static final Setting.Mode HIT_STYLE = HIT_PARTICLES.add(new Setting.Mode("Style", 0, "Sparks", "Stars", "Orbs"));

    public static final Module KILL_EFFECT = reg(new Module("KillEffect", "Kill Effect", Category.EFFECTS,
            "Beam and burst when your target dies", true, "death", "beam", "kill"));
    public static final Module JUMP_CIRCLE = reg(new Module("JumpCircle", "Jump Circle", Category.EFFECTS,
            "Expanding ring where you jump", true, "ring", "jump", "circle"));
    public static final Setting.Num JUMP_SPEED = JUMP_CIRCLE.add(new Setting.Num("Expand speed", 0.05, 0.25, 0.01, 0.1));
    public static final Module TRAIL = reg(new Module("Trails", "Trails", Category.EFFECTS,
            "Colored particle trail behind you", false, "particles", "trail", "path"));
    public static final Setting.Num TRAIL_LENGTH = TRAIL.add(new Setting.Num("Length", 8, 60, 2, 28));
    public static final Module TARGET_MARKER = reg(new Module("TargetMarker", "Target Marker", Category.EFFECTS,
            "Rotating glowing ring around your target", true, "esp", "ring", "marker", "halo", "target"));
    public static final Module CHINA_HAT = reg(new Module("ChinaHat", "China Hat", Category.EFFECTS,
            "Glowing hat above your head (third person)", false, "hat", "cone", "head"));
    public static final Module AMBIENT = reg(new Module("Ambient", "Ambient", Category.EFFECTS,
            "Floating glow particles around you", false, "particles", "dust", "atmosphere"));

    // --------------------------------------------------------------- Misc
    public static final Module RAINBOW = reg(new Module("Rainbow", "Rainbow Theme", Category.MISC,
            "Animated rainbow accent color everywhere", false, "color", "theme", "rgb"));

    private Modules() { }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("vodxvisuals.properties");
    }

    public static void load() {
        Path p = file();
        if (!Files.exists(p)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(p)) {
            props.load(in);
        } catch (IOException e) {
            return;
        }
        for (Module m : ALL) {
            String v = props.getProperty(m.id);
            if (v != null) m.enabled = Boolean.parseBoolean(v);
            for (Setting s : m.settings) {
                String sv = props.getProperty(m.id + "." + s.name);
                if (sv != null) s.deserialize(sv);
            }
        }
    }

    public static void save() {
        Properties props = new Properties();
        for (Module m : ALL) {
            props.setProperty(m.id, Boolean.toString(m.enabled));
            for (Setting s : m.settings) props.setProperty(m.id + "." + s.name, s.serialize());
        }
        try (OutputStream out = Files.newOutputStream(file())) {
            props.store(out, "Vodx Visuals");
        } catch (IOException ignored) { }
    }
}
