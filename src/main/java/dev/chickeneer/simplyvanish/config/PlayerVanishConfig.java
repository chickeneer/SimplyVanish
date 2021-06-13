package dev.chickeneer.simplyvanish.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

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

    public static PlayerVanishConfig load(JSONObject jsonObject) {
        String name = (String) jsonObject.get("name");
        UUID uuid = (UUID) jsonObject.get("uuid");
        PlayerVanishConfig config = new PlayerVanishConfig(name, uuid);

        JSONArray list = (JSONArray) jsonObject.get("flags");
        for (Object o : list) {
            String s = ((String) o).trim().toLowerCase();
            if (s.isEmpty() || s.length() < 2) {
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

    public @Nullable UUID getUuid() {
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
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("uuid", uuid);

        JSONArray list = new JSONArray();
        for (Flag flag : flags.values()) {
            if (flag.state == flag.preset) {
                continue;
            }
            list.put(flag.toLine());
        }
        obj.put("flags", list);

        return obj.toString();
    }
}
