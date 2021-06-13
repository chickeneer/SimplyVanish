package dev.chickeneer.simplyvanish.api.hooks.impl;

import dev.chickeneer.simplyvanish.SimplyVanish;
import dev.chickeneer.simplyvanish.api.hooks.AbstractHook;
import dev.chickeneer.simplyvanish.api.hooks.HookListener;
import dev.chickeneer.simplyvanish.api.hooks.HookPurpose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ProtocolLibHook extends AbstractHook {

    private static final HookPurpose[] purpose = new HookPurpose[]{
            //		HookPurpose.LISTENER,
            //		HookPurpose.AFTER_VANISH, HookPurpose.AFTER_REAPPEAR,
            //		HookPurpose.AFTER_SETFLAGS,
    };

    //	private final ProtocolManager protocolManager;

    public ProtocolLibHook(@NotNull SimplyVanish plugin) {
        //		protocolManager = ProtocolLibrary.getProtocolManager();
        throw new RuntimeException("not intended for use :)");
        //		protocolManager.addPacketListener(new PacketAdapter(plugin, ConnectionSide.SERVER_SIDE, ListenerPriority.NORMAL, 0x1F, 0x22) {
        //			@Override
        //			public void onPacketSending(PacketEvent event) {
        //				// Item packets
        //				switch (event.getPacketID()) {
        //				case 0x1F:
        //					try {
        //						System.out.println("RELMOVE: " + event.getPacket().getModifier().readSafely(0));
        //					} catch (FieldAccessException e) {
        //						// TODO Auto-generated catch block
        //						e.printStackTrace();
        //					}
        ////					event.setCancelled(true);
        //					break;
        //				case 0x22:
        //					try {
        //						System.out.println("TELEPORT: " + event.getPacket().getModifier().readSafely(0));
        //					} catch (FieldAccessException e) {
        //						// TODO Auto-generated catch block
        //						e.printStackTrace();
        //					}
        ////					event.setCancelled(true);
        //					break;
        //				}
        //			}
        //		});
    }

    @Override
    public @NotNull String getHookName() {
        return "ProtocolLib";
    }

    @Override
    public @NotNull HookPurpose[] getSupportedMethods() {
        return purpose;
    }

    @Override
    public HookListener getListener() {
        // TODO
        return () -> false;
    }

    @Override
    public void afterVanish(@NotNull String name, @Nullable UUID uuid) {
        // TODO
    }

    @Override
    public void afterReappear(@NotNull String name, @Nullable UUID uuid) {
        // TODO
    }

    @Override
    public void afterSetFlags(@NotNull String name, @Nullable UUID uuid) {
        // TODO
    }
}
