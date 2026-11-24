package com.vodx.visuals;

import com.vodx.visuals.gui.ClickGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class VodxVisualsClient implements ClientModInitializer {
    public static KeyBinding MENU_KEY;

    @Override
    public void onInitializeClient() {
        Modules.load();
        MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vodxvisuals.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.vodxvisuals"));
        HudRenderer.init();
        Effects.init();
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (MENU_KEY.wasPressed()) {
                mc.setScreen(new ClickGuiScreen());
            }
            KillAura.tick(mc);
            AimAssist.tick(mc);
        });
    }
}
