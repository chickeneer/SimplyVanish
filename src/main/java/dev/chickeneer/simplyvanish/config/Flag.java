package dev.chickeneer.simplyvanish.config;

import org.jetbrains.annotations.NotNull;

public class Flag implements Cloneable {
    public final String name;
    public final boolean preset;
    public boolean state;

    public Flag(@NotNull String name, boolean preset) {
        this.name = name;
        this.preset = preset;
        state = preset;
    }

    /**
     * Always returns "+/-name".
     *
     * @return
     */
    public @NotNull String toLine() {
        return fs(state) + name;
    }

    /**
     * Flag state prefix.
     *
     * @param state
     * @return
     */
    public static @NotNull String fs(boolean state) {
        return state ? "+" : "-";
    }

    public Flag clone() {
        Flag flag = new Flag(name, preset);
        flag.state = state;
        return flag;
    }
}
