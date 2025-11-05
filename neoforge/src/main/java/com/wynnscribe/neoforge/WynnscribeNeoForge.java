package com.wynnscribe.neoforge;

import com.wynnscribe.Wynnscribe;
import net.neoforged.fml.common.Mod;

@Mod(Wynnscribe.MOD_ID)
public class WynnscribeNeoForge {
    public WynnscribeNeoForge() {
        Wynnscribe.INSTANCE.init();
    }
}
