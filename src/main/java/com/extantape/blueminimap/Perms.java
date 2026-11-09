package com.extantape.blueminimap;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Permission checks that still respect the names from before the rename to BlueMinimap (2026-11-09), for one
 * release (0.2.x): {@code bluemaphud.use} alongside {@code blueminimap.use}, and so on. The old nodes are declared
 * with their old defaults, so on a server that never touched them both names agree.
 *
 * <p>Most nodes are on by default, so the old names matter most where a server turned one <em>off</em>, for example
 * {@code bluemaphud.share.send: false} for a children's group. That must stay off after the upgrade. So for nodes
 * that are on by default, a "no" on either name wins. For {@code admin}, which is for operators only by default, a
 * "yes" on either name wins, so admins keep their tools. Whenever the old name is what decided the answer, that's
 * logged once per player and node, saying what to change.
 */
public final class Perms {
    /** Nodes that are on by default, where a deny on either name wins. Everything else: a grant on either wins. */
    static final Set<String> ON_BY_DEFAULT = Set.of("blueminimap.use", "blueminimap.fullscreen", "blueminimap.seeplayers",
            "blueminimap.share.send", "blueminimap.share.receive");
    private static final Set<String> warned = ConcurrentHashMap.newKeySet();
    private static volatile Consumer<String> warn = line -> {};

    private Perms() {}

    public static void logTo(Consumer<String> sink) { warn = sink; }

    public static String oldName(String node) {
        return "bluemaphud." + node.substring("blueminimap.".length());
    }

    public static boolean has(Permissible who, String node) {
        boolean now = who.hasPermission(node);
        if (!node.startsWith("blueminimap.")) return now;
        String old = oldName(node);
        boolean then = who.hasPermission(old);
        if (now == then) return now;
        boolean result = ON_BY_DEFAULT.contains(node) ? now && then : now || then;
        if (result == now) return result; // the old name changed nothing, so there's nothing to tell anyone
        String name = who instanceof CommandSender cs ? cs.getName() : String.valueOf(who);
        if (warned.add(name + " " + node))
            warn.accept(name + " has " + node + " " + now + " but the old " + old + " " + then + ", so it's " + result
                    + ". The old name is still respected in 0.2.x but goes in the next release: set " + node + " instead.");
        return result;
    }
}
