package me.asofold.bpl.simplyvanish.stats;

import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;

public class Stats {
    public static final class Entry {
        public long dur = 0;
        public long n = 0;
        public long min = Long.MAX_VALUE;
        public long max = Long.MIN_VALUE;
    }

    private long tsStats = 0;
    private final long periodStats = 12345;
    private final long nVerbose = 500;
    private long nDone = 0;
    private boolean logStats = true;
    private boolean showRange = true;
    private final Map<Integer, Entry> entries = new HashMap<>();
    private final DecimalFormat f;
    private final String label;

    public Stats() {
        this("[Stats]");
    }

    public Stats(String label) {
        this.label = label;
        f = new DecimalFormat();
        f.setGroupingUsed(true);
        f.setGroupingSize(3);
        DecimalFormatSymbols s = f.getDecimalFormatSymbols();
        s.setGroupingSeparator(',');
        f.setDecimalFormatSymbols(s);
    }

    /**
     * Map id to name.
     */
    private final Map<Integer, String> idKeyMap = new HashMap<>();
    int maxId = 0;

    public final void addStats(Integer key, long dur) {
        if (dur < 0) {
            dur = 0;
        }
        Entry entry = entries.get(key);
        if (entry != null) {
            entry.n += 1;
            entry.dur += dur;
            if (dur < entry.min) {
                entry.min = dur;
            } else if (dur > entry.max) {
                entry.max = dur;
            }
        } else {
            entry = new Entry();
            entry.dur = dur;
            entry.n = 1;
            entries.put(key, entry);
            entry.min = dur;
            entry.max = dur;
        }
        if (!logStats) {
            return;
        }
        nDone++;
        if (nDone > nVerbose) {
            nDone = 0;
            long ts = System.currentTimeMillis();
            if (ts > tsStats + periodStats) {
                tsStats = ts;
                // print out stats !
                System.out.println(getStatsStr());
            }
        }
    }

    public final String getStatsStr() {
        return getStatsStr(false);
    }

    public final String getStatsStr(boolean colors) {
        StringBuilder b = new StringBuilder(400);
        b.append(label).append(" ");
        boolean first = true;
        for (Integer id : entries.keySet()) {
            if (!first) {
                b.append(" | ");
            }
            Entry entry = entries.get(id);
            String av = f.format(entry.dur / entry.n);
            String key = getKey(id);
            String n = f.format(entry.n);
            if (colors) {
                key = ChatColor.GREEN + key + ChatColor.WHITE;
                n = ChatColor.AQUA + n + ChatColor.WHITE;
                av = ChatColor.YELLOW + av + ChatColor.WHITE;
            }
            b.append(key).append(" av=").append(av).append(" n=").append(n);
            if (showRange) {
                b.append(" rg=").append(f.format(entry.min)).append("...").append(f.format(entry.max));
            }
            first = false;
        }
        return b.toString();
    }

    /**
     * @param id
     * @return
     */
    public final String getKey(Integer id) {
        return idKeyMap.computeIfAbsent(id, i -> "<no key for id: " + i + ">");

    }

    public final Integer getNewId(String key) {
        maxId++;
        while (idKeyMap.containsKey(maxId)) {
            maxId++; // probably not going to happen...
        }
        idKeyMap.put(maxId, key);
        return maxId;
    }

    /**
     * @param key not null
     * @return
     */
    public final Integer getId(String key) {
        for (Integer id : idKeyMap.keySet()) {
            if (key.equals(idKeyMap.get(id))) {
                return id;
            }
        }
        return null;
    }

    public final void clear() {
        entries.clear();
    }

    public void setLogStats(boolean log) {
        logStats = log;
    }

    public void setShowRange(boolean set) {
        showRange = set;
    }

}
