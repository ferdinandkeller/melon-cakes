package dev.ferdinandkeller.meloncakes;

import net.minecraft.state.property.IntProperty;

public class Properties {
    /**
     * A property that specifies the bites taken out of a melon.
     */
    public static final IntProperty BITES_MELON = IntProperty.of("bites", 0, MelonCakeBlock.MAX_BITES);
}
