package me.asofold.bpl.simplyvanish.config.compatlayer;

import org.bukkit.Bukkit;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

public class ConfigUtil {

    public static final int canaryInt = Integer.MIN_VALUE + 7;
    public static final long canaryLong = Long.MIN_VALUE + 7L;
    public static final double canaryDouble = -Double.MAX_VALUE + 1.125798;

    public static String stringPath(String path) {
        return stringPath(path, '.');
    }

    public static String stringPath(String path, char sep) {
        String useSep = (sep == '.') ? "\\." : "" + sep;
        String[] split = path.split(useSep);
        StringBuilder builder = new StringBuilder();
        builder.append(stringPart(split[0]));
        for (int i = 1; i < split.length; i++) {
            builder.append(sep).append(stringPart(split[i]));
        }
        return builder.toString();
    }

    /**
     * Aimed at numbers in paths.
     *
     * @param cfg
     * @param path
     * @return
     */
    public static String bestPath(CompatConfig cfg, String path) {
        return bestPath(cfg, path, '.');
    }


    /**
     * Aimed at numbers in paths.
     *
     * @param cfg
     * @param path
     * @param sep
     * @return
     */
    public static String bestPath(CompatConfig cfg, String path, char sep) {
        String useSep = (sep == '.') ? "\\." : "" + sep;
        String[] split = path.split(useSep);
        String res;
        if (cfg.hasEntry(split[0])) {
            res = split[0];
        } else {
            res = stringPart(split[0]);
            if (!cfg.hasEntry(res)) {
                return path;
            }
        }
        for (int i = 1; i < split.length; i++) {
            if (cfg.hasEntry(res + sep + split[i])) {
                res += sep + split[i];
            } else {
                res += sep + stringPart(split[i]);
                if (!cfg.hasEntry(res)) {
                    return path;
                }
            }
        }
        return res;
    }

    public static String stringPart(String input) {
        try {
            Double.parseDouble(input);
            return "'" + input + "'";
        } catch (NumberFormatException e) {
        }
        try {
            Long.parseLong(input);
            return "'" + input + "'";
        } catch (NumberFormatException e) {
        }
        try {
            Integer.parseInt(input);
            return "'" + input + "'";
        } catch (NumberFormatException e) {
        }
        return input;
    }

    public static boolean forceDefaults(CompatConfig defaults, CompatConfig config) {
        Map<String, Object> all = defaults.getValuesDeep();
        boolean changed = false;
        for (String path : all.keySet()) {
            if (!config.hasEntry(path)) {
                config.setProperty(path, defaults.getProperty(path, null));
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Add StringList entries to a set.
     *
     * @param cfg
     * @param path
     * @param set
     * @param clear     If to clear the set.
     * @param trim
     * @param lowerCase
     */
    public static void readStringSetFromList(CompatConfig cfg, String path, Set<String> set, boolean clear, boolean trim, boolean lowerCase) {
        if (clear) {
            set.clear();
        }
        List<String> tempList = cfg.getStringList(path, null);
        if (tempList != null) {
            for (String entry : tempList) {
                if (trim) {
                    entry = entry.trim();
                }
                if (lowerCase) {
                    entry = entry.toLowerCase();
                }
                set.add(entry);
            }
        }
    }

    /**
     * Read keys and return map sorted by inheritance, safe for reading.<br>
     * Entries without inheritance have their own key set in the map (!).
     * <hr>
     * Quadratic time algorithm :)
     *
     * @param cfg
     * @param path
     * @param inheritanceKey
     * @return
     */
    public static LinkedHashMap<String, String> getInheritanceOrder(CompatConfig cfg, String path, String inheritanceKey) {
        // TODO: detach this to ConfigUtil from compatlayer !
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();

        List<String> keys = cfg.getStringKeys(path); // node names
        if (keys.isEmpty()) {
            return ordered;
        }

        Set<String> done = new HashSet<>();
        Map<String, String> inheritance = new HashMap<>();
        // First sorting in:
        for (String key : keys) {
            String parent = cfg.getString(path + "." + key + "." + inheritanceKey, null);
            if (parent == null) {
                done.add(key);
                ordered.put(key, key);
            } else {
                inheritance.put(key, parent);
            }
        }
        // Now attempt to resolve parents:
        List<String> rem = new LinkedList<>();
        while (!inheritance.isEmpty()) {
            rem.clear();
            int found = 0;
            for (Entry<String, String> entry : inheritance.entrySet()) {
                String key = entry.getKey();
                String parent = entry.getValue();
                if (done.contains(parent)) {
                    rem.add(key);
                    done.add(key);
                    ordered.put(key, parent);
                    found++;
                }
            }
            for (String key : rem) {
                inheritance.remove(key);
            }
            if (found == 0) {
                break;
            }
        }
        if (!inheritance.isEmpty()) {
            StringBuilder b = new StringBuilder();
            b.append("[ConfigUtil] Inheritance entries could not be resolved(").append(path).append("):");
            for (Entry<String, String> entry : inheritance.entrySet()) {
                b.append(" ").append(entry.getKey()).append("->").append(entry.getValue());
            }
            Bukkit.getLogger().warning(b.toString());
        }
        return ordered;
    }

    /**
     * Might have a newline at the end.
     *
     * @param clazz
     * @param path
     * @return
     */
    public static String fetchResource(Class<?> clazz, String path) {
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (!classPath.startsWith("jar")) {
            return null;
        }
        String absPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/" + path;
        try {
            URL url = new URL(absPath);
            try {
                Object obj = url.getContent();
                if (obj instanceof InputStream) {
                    BufferedReader r = new BufferedReader(new InputStreamReader((InputStream) obj));
                    StringBuilder builder = new StringBuilder();
                    String last = r.readLine();
                    while (last != null) {
                        builder.append(last);
                        builder.append("\n"); // does not hurt if one too many.
                        last = r.readLine();
                    }
                    return builder.toString();
                } else {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        } catch (MalformedURLException e) {
        }
        return null;
    }

    public static boolean writeFile(File file, String content) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                return false;
            }
        }
        FileWriter w = null;
        try {
            w = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(w);
            bw.write(content);
            bw.flush();
            w.flush();
            w.close();
            return true;
        } catch (IOException e) {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException e2) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }

}
