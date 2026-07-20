package com.ae2utilix.network;

import com.ae2utilix.AE2Utilix;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {

    public static final SimpleNetworkWrapper CHANNEL = new SimpleNetworkWrapper(AE2Utilix.MODID);

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(PacketHighlightBlock.Handler.class, PacketHighlightBlock.class, nextId++, Side.CLIENT);
        CHANNEL.registerMessage(PacketSwitchDecomposerMode.Handler.class, PacketSwitchDecomposerMode.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketToggleCGCButton.Handler.class, PacketToggleCGCButton.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketCommonInterfaceFluidMark.Handler.class, PacketCommonInterfaceFluidMark.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketCommonInterfaceSetAmount.Handler.class, PacketCommonInterfaceSetAmount.class, nextId++, Side.SERVER);
        CHANNEL.registerMessage(PacketSwitchCpuAccessMode.Handler.class, PacketSwitchCpuAccessMode.class, nextId++, Side.SERVER);
    }
}
