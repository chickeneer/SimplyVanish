package me.asofold.bpl.simplyvanish.api.hooks.impl;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import me.asofold.bpl.simplyvanish.api.hooks.AbstractHook;
import me.asofold.bpl.simplyvanish.api.hooks.HookPurpose;
import me.asofold.bpl.simplyvanish.api.hooks.util.PluginGetter;

public class Essentials2Hook extends AbstractHook {

    private final PluginGetter<Essentials> getter;

    public Essentials2Hook() throws SecurityException, NoSuchMethodException {
        getter = new PluginGetter<Essentials>("Essentials");
        User.class.getDeclaredMethod("setHidden", boolean.class); // hmmm
    }

    @Override
    public String getHookName() {
        return "Essentials2";
    }

    @Override
    public HookPurpose[] getSupportedMethods() {
        return new HookPurpose[]{HookPurpose.BEFORE_VANISH, HookPurpose.AFTER_REAPPEAR};
    }

    @Override
    public final void beforeVanish(final String playerName) {
        setHidden(playerName, true);
    }

    @Override
    public final void afterReappear(final String playerName) {
        setHidden(playerName, false);
    }

    private final void setHidden(final String playerName, final boolean hidden) {
        try {
            final User user = getter.getPlugin().getUser(playerName);
            if (user != null) {
                user.setHidden(hidden);
            }
        } catch (Throwable t) {
            // TODO: organize the source code by grace of devs.
        }
    }

}
