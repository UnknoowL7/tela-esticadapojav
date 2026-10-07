package com.telaesticada;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(
        modid = StretchedScreenMod.MODID,
        name = StretchedScreenMod.NAME,
        version = StretchedScreenMod.VERSION
)
public class StretchedScreenMod {

    public static final String MODID = "stretchedscreen";
    public static final String NAME = "Stretched Screen";
    public static final String VERSION = "1.0";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        System.out.println("[StretchedScreen] Mod carregado!");
    }
}
