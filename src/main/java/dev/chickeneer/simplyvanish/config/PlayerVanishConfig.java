package dev.chickeneer.simplyvanish.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerVanishConfig extends VanishConfig {
    private final String name;
    private final String lowerCaseName;
    private final UUID uuid;

    public PlayerVanishConfig(@NotNull String name, @Nullable UUID uuid) {
        super();
        this.name = name;
        this.lowerCaseName = name.toLowerCase();
        this.uuid = uuid;
    }

    public static PlayerVanishConfig load(JsonObject jsonObject) {
        String name = jsonObject.get("name").getAsString();
        UUID uuid = jsonObject.has("uuid") ? UUID.fromString(jsonObject.get("uuid").getAsString()) : null;
        PlayerVanishConfig config = new PlayerVanishConfig(name, uuid);

        JsonArray list = (JsonArray) jsonObject.get("flags");
        for (JsonElement e : list) {
            String s = e.getAsString().toLowerCase();
            if (s.length() < 2) {
                continue;
            }
            boolean state;
            if (s.startsWith("+")) {
                state = true;
                s = s.substring(1);
            } else if (s.startsWith("-")) {
                state = false;
                s = s.substring(1);
            } else {
                continue;
            }

            Flag flag = config.flags.get(getMappedFlagName(s));
            if (flag == null) {
                continue; // should not happen by contract.
            }
            if (state != flag.state) {
                flag.state = state;
                config.changed = true;
            }
        }
        return config;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getLowerCaseName() {
        return lowerCaseName;
    }

    public @Nullable UUID getUniqueId() {
        return uuid;
    }

    /**
     * Clones but sets changed to true.
     */
    @Override
    public PlayerVanishConfig clone() {
        PlayerVanishConfig cfg = new PlayerVanishConfig(name, uuid);
        cfg.setAll(this);
        cfg.changed = true;
        cfg.preventInventoryAction = preventInventoryAction;
        return cfg;
    }


    public String toJsonString() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        if (uuid != null) {
            obj.addProperty("uuid", uuid.toString());
        }

        JsonArray list = new JsonArray();
        for (Flag flag : flags.values()) {
            if (flag.state == flag.preset) {
                continue;
            }
            list.add(flag.toLine());
        }
        obj.add("flags", list);

        return obj.toString();
    }
}
