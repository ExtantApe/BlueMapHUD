package com.extantape.bluemaphud.command;

import com.extantape.bluemaphud.BlueMapHudPlugin;
import com.extantape.bluemaphud.config.HudConfig;
import com.extantape.bluemaphud.hud.HudManager;
import com.extantape.bluemaphud.hud.HudSession;
import com.extantape.bluemaphud.hud.PlayerSettings;
import com.extantape.bluemaphud.theme.Theme;
import com.extantape.bluemaphud.theme.ThemeSet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;

/** Registers /map, /hud and /bmhud, backing off to the next name when one is already taken. */
public final class HudCommands {
    private final BlueMapHudPlugin plugin;
    /** Names chosen at startup. Data pack reloads rebuild the tree and must not back off from themselves. */
    private String claimedMap;
    private String claimedHud;

    public HudCommands(BlueMapHudPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(Commands commands, HudConfig config) {
        LiteralArgumentBuilder<CommandSourceStack> admin = Commands.literal("bmhud")
                .then(Commands.literal("help").executes(ctx -> help(ctx.getSource().getSender())))
                .then(mapTree(Commands.literal("map")))
                .then(hudTree(Commands.literal("hud")))
                .then(Commands.literal("reload").requires(HudCommands::isAdmin).executes(ctx -> {
                    plugin.reload();
                    return reply(ctx.getSource().getSender(), "BlueMapHUD reloaded. Command names change after a restart.");
                }))
                .then(Commands.literal("debug").requires(HudCommands::isAdmin)
                        .executes(ctx -> reply(ctx.getSource().getSender(), plugin.debugLine()))
                        .then(Commands.literal("dump").executes(player((p, s) -> plugin.dumpView(p))))
                        .then(Commands.literal("pattern").executes(player((p, s) -> {
                            plugin.hud().togglePattern(s);
                            reply(p, s.settings().enabled ? "Test pattern toggled." : "Test pattern toggled. Turn the HUD on to see it.");
                        }))))
                .then(Commands.literal("pack").requires(HudCommands::isAdmin)
                        .then(Commands.literal("export").executes(ctx -> exportPack(ctx.getSource().getSender()))));
        commands.register(admin.build(), "BlueMapHUD help and admin commands", List.of());

        boolean firstTime = claimedMap == null && claimedHud == null;
        claimedMap = claim(commands, claimedMap != null ? List.of(claimedMap) : config.commands().mapNames(), "Open the fullscreen map", true, firstTime);
        claimedHud = claim(commands, claimedHud != null ? List.of(claimedHud) : config.commands().hudNames(), "Control the minimap", false, firstTime);
        // Namespaced names always exist, so a taken short name never locks anyone out.
        addNamespaced(commands, "map", claimedMap, mapTree(Commands.literal("bluemaphud:map")));
        addNamespaced(commands, "hud", claimedHud, hudTree(Commands.literal("bluemaphud:hud")));
        plugin.getLogger().info("Commands: fullscreen map is " + describe(claimedMap, "/bmhud map") + ", minimap is " + describe(claimedHud, "/bmhud hud") + ".");
    }

    private String claim(Commands commands, List<String> names, String description, boolean isMap, boolean checkLegacy) {
        for (String name : names) {
            if (checkLegacy && Bukkit.getCommandMap().getCommand(name) != null) continue; // A legacy plugin command.
            if (commands.getDispatcher().getRoot().getChild(name) != null) continue; // Another plugin's Brigadier command.
            LiteralCommandNode<CommandSourceStack> node =
                    (isMap ? mapTree(Commands.literal(name)) : hudTree(Commands.literal(name))).build();
            Set<String> registered = commands.register(node, description, List.of());
            if (registered.contains(name)) return name;
        }
        return null;
    }

    /**
     * Adds the namespaced name directly. Registering "map" through {@link Commands#register} would also
     * take the plain /map, even from a legacy command that another plugin owns.
     */
    private static void addNamespaced(Commands commands, String shortName, String claimed,
                                      LiteralArgumentBuilder<CommandSourceStack> tree) {
        if (!shortName.equals(claimed)) commands.getDispatcher().getRoot().addChild(tree.build());
    }

    private static String describe(String claimed, String fallback) {
        return claimed == null ? fallback : "/" + claimed;
    }

    private static boolean isAdmin(CommandSourceStack source) {
        return source.getSender().hasPermission("bluemaphud.admin");
    }

    private LiteralArgumentBuilder<CommandSourceStack> mapTree(LiteralArgumentBuilder<CommandSourceStack> root) {
        return root.requires(source -> source.getSender().hasPermission("bluemaphud.fullscreen"))
                .executes(player((p, s) -> plugin.toggleFullscreen(p)))
                .then(Commands.literal("zoom").then(Commands.argument("level", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestZoom(builder))
                        .executes(ctx -> withPlayer(ctx, (p, s) -> {
                            Integer zoom = parseZoom(StringArgumentType.getString(ctx, "level"), s.settings().fullscreenZoom);
                            if (zoom == null) {
                                error(p, "Zoom must be in, out or one of 1, 2, 4, 8, 16, 32, 64.");
                                return;
                            }
                            s.settings().fullscreenZoom = zoom;
                            plugin.hud().saveSettings(p, s);
                            reply(p, "Fullscreen zoom is now " + zoom + " blocks per cell.");
                        }))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> hudTree(LiteralArgumentBuilder<CommandSourceStack> root) {
        return root.requires(source -> source.getSender().hasPermission("bluemaphud.use"))
                .executes(player((p, s) -> setEnabled(p, s, !s.settings().enabled)))
                .then(Commands.literal("on").executes(player((p, s) -> setEnabled(p, s, true))))
                .then(Commands.literal("off").executes(player((p, s) -> setEnabled(p, s, false))))
                .then(Commands.literal("zoom").then(Commands.argument("level", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestZoom(builder))
                        .executes(ctx -> withPlayer(ctx, (p, s) -> {
                            Integer zoom = parseZoom(StringArgumentType.getString(ctx, "level"), s.settings().zoom);
                            if (zoom == null) {
                                error(p, "Zoom must be in, out or one of 1, 2, 4, 8, 16, 32, 64.");
                                return;
                            }
                            s.settings().zoom = zoom;
                            plugin.hud().saveSettings(p, s);
                            reply(p, "Minimap zoom is now " + zoom + " blocks per cell.");
                        }))))
                .then(Commands.literal("size")
                        .then(sizeOption("small"))
                        .then(sizeOption("medium"))
                        .then(sizeOption("large")))
                .then(Commands.literal("offset").then(Commands.argument("x", IntegerArgumentType.integer(-1000, 1000))
                        .then(Commands.argument("y", IntegerArgumentType.integer(0, 1000)).executes(ctx -> withPlayer(ctx, (p, s) -> {
                            s.settings().offsetX = IntegerArgumentType.getInteger(ctx, "x");
                            s.settings().offsetY = Math.min(IntegerArgumentType.getInteger(ctx, "y"), plugin.config().hud().maxOffsetY());
                            plugin.hud().saveSettings(p, s);
                            reply(p, hudName(s) + " moved to " + s.settings().offsetX + ", " + s.settings().offsetY + ".");
                        })))))
                .then(Commands.literal("corner")
                        .then(cornerOption("left", -1))
                        .then(cornerOption("center", 0))
                        .then(cornerOption("right", 1)))
                .then(Commands.literal("mode")
                        .executes(player((p, s) -> reply(p, s.settings().coordsOnly
                                ? "You're seeing coordinates only. Use /hud mode map for the minimap."
                                : "You're seeing the minimap. Use /hud mode coords for coordinates only.")))
                        .then(Commands.literal("map").executes(player((p, s) -> setCoordsOnly(p, s, false))))
                        .then(Commands.literal("coords").executes(player((p, s) -> setCoordsOnly(p, s, true)))))
                .then(Commands.literal("theme")
                        .executes(player((p, s) -> listThemes(p, s)))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
                                    for (Theme theme : plugin.themes().themes()) if (theme.id().startsWith(typed)) builder.suggest(theme.id());
                                    if ("default".startsWith(typed)) builder.suggest("default");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> withPlayer(ctx, (p, s) -> setTheme(p, s, StringArgumentType.getString(ctx, "name"))))))
                .then(Commands.literal("hide")
                        .then(partOption("coords", false)).then(partOption("players", false)).then(partOption("markers", false))
                        .then(partOption("clock", false)))
                .then(Commands.literal("show")
                        .then(partOption("coords", true)).then(partOption("players", true)).then(partOption("markers", true))
                        .then(partOption("clock", true)));
    }

    private LiteralArgumentBuilder<CommandSourceStack> sizeOption(String name) {
        return Commands.literal(name).executes(player((p, s) -> {
            s.settings().size = HudConfig.sizeCells(name);
            plugin.hud().saveSettings(p, s);
            reply(p, "Minimap size is now " + name + ".");
        }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> cornerOption(String name, int side) {
        return Commands.literal(name).executes(player((p, s) -> {
            s.settings().offsetX = side * plugin.config().hud().cornerX();
            plugin.hud().saveSettings(p, s);
            reply(p, hudName(s) + " moved " + name + ". Fine-tune it with the offset option, because the server can't see your screen width.");
        }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> partOption(String part, boolean visible) {
        return Commands.literal(part).executes(player((p, s) -> {
            PlayerSettings settings = s.settings();
            if (part.equals("coords") && !visible && settings.coordsOnly) {
                error(p, "Coordinates are all that's showing. Use /hud off to hide them, or /hud mode map for the minimap.");
                return;
            }
            switch (part) {
                case "coords" -> settings.showCoords = visible;
                case "players" -> settings.showPlayers = visible;
                case "clock" -> settings.showClock = visible;
                default -> settings.showMarkers = visible;
            }
            plugin.hud().saveSettings(p, s);
            reply(p, (visible ? "Showing " : "Hiding ") + (part.equals("clock") ? "the clock" : part) + ".");
            if (part.equals("clock") && visible && !plugin.config().hud().clock()) {
                error(p, "The clock is turned off on this server, so it won't show.");
            }
        }));
    }

    /** Lists the themes, marking the player's own, with each name clickable to switch to it. */
    private void listThemes(Player player, HudSession session) {
        ThemeSet themes = plugin.themes();
        int current = themes.resolve(session.settings().theme);
        Component line = Component.text("Themes: ", NamedTextColor.GRAY);
        for (int i = 0; i < themes.size(); i++) {
            Theme theme = themes.get(i);
            boolean mine = i == current;
            Component name = Component.text(theme.name(), mine ? NamedTextColor.WHITE : NamedTextColor.AQUA)
                    .decoration(TextDecoration.UNDERLINED, !mine)
                    .clickEvent(ClickEvent.runCommand("/bluemaphud:hud theme " + theme.id()))
                    .hoverEvent(HoverEvent.showText(Component.text(mine ? "Your theme" : "Switch to " + theme.name())));
            line = line.append(name).append(Component.text(mine ? " (yours)" : "", NamedTextColor.GRAY))
                    .append(Component.text(i + 1 < themes.size() ? ", " : "", NamedTextColor.GRAY));
        }
        player.sendMessage(line);
        reply(player, "Click one, or use /hud theme <name>. /hud theme default goes back to the server's.");
    }

    private void setTheme(Player player, HudSession session, String name) {
        ThemeSet themes = plugin.themes();
        String wanted = name.toLowerCase(Locale.ROOT);
        if (wanted.equals("default")) {
            session.settings().theme = null;
            plugin.hud().saveSettings(player, session);
            reply(player, "Using the server's theme, " + themes.defaultTheme().name() + ".");
            return;
        }
        int index = themes.indexOf(wanted);
        if (index < 0) {
            error(player, "There's no theme called " + name + ". Try " + String.join(", ", themes.themes().stream().map(Theme::id).toList()) + ".");
            return;
        }
        session.settings().theme = themes.get(index).id();
        plugin.hud().saveSettings(player, session);
        reply(player, "Theme set to " + themes.get(index).name() + ".");
    }

    private void setEnabled(Player player, HudSession session, boolean enabled) {
        session.settings().enabled = enabled;
        plugin.hud().saveSettings(player, session);
        String off = session.settings().coordsOnly ? "Coordinates off. They stay off until you turn them on." : "Minimap off. It stays off until you turn it on.";
        reply(player, enabled ? hudName(session) + " on." : off);
        if (enabled) explainIfHidden(player, session);
    }

    /** Choosing a mode also turns the HUD on, because that's what the player is asking to see. */
    private void setCoordsOnly(Player player, HudSession session, boolean coordsOnly) {
        session.settings().coordsOnly = coordsOnly;
        session.settings().enabled = true;
        plugin.hud().saveSettings(player, session);
        reply(player, coordsOnly ? "Showing only coordinates. Use /hud mode map to bring the minimap back." : "Showing the minimap.");
        explainIfHidden(player, session);
    }

    private void explainIfHidden(Player player, HudSession session) {
        if (session.settings().coordsOnly) plugin.explainIfNoPack(player);
        else plugin.explainIfNoMap(player);
    }

    private static String hudName(HudSession session) {
        return session.settings().coordsOnly ? "Coordinates" : "Minimap";
    }

    private static Integer parseZoom(String text, int current) {
        switch (text.toLowerCase(Locale.ROOT)) {
            case "in": return HudConfig.stepZoom(current, -1);
            case "out": return HudConfig.stepZoom(current, 1);
            default:
                try {
                    int value = Integer.parseInt(text);
                    for (int level : HudConfig.ZOOM_LEVELS) {
                        if (level == value) return value;
                    }
                } catch (NumberFormatException ignored) {
                    // Reported by the caller.
                }
                return null;
        }
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestZoom(
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        builder.suggest("in").suggest("out");
        for (int level : HudConfig.ZOOM_LEVELS) builder.suggest(String.valueOf(level));
        return builder.buildFuture();
    }

    private Command<CommandSourceStack> player(BiConsumer<Player, HudSession> action) {
        return ctx -> withPlayer(ctx, action);
    }

    private int withPlayer(CommandContext<CommandSourceStack> ctx, BiConsumer<Player, HudSession> action) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            error(sender, "Only players can use this command.");
            return 0;
        }
        action.accept(player, plugin.hud().session(player));
        return Command.SINGLE_SUCCESS;
    }

    private int help(CommandSender sender) {
        HudManager hud = plugin.hud();
        sender.sendMessage(Component.text("BlueMapHUD", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Fullscreen map: toggle, or zoom <n|in|out>. Sneak + F also opens it."));
        sender.sendMessage(Component.text("Minimap: on, off, mode map or coords, zoom, size, offset, corner, theme, hide and show (coords, players, markers, clock)."));
        if (sender.hasPermission("bluemaphud.admin")) {
            sender.sendMessage(Component.text("Admin: /bmhud reload, debug, debug dump, debug pattern, pack export."));
            sender.sendMessage(Component.text(hud.debugLine()));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int exportPack(CommandSender sender) {
        var packs = plugin.packs();
        reply(sender, "The pack is at " + packs.exportPath() + ", SHA-1 " + packs.sha1() + ".");
        reply(sender, "To host it at pack.public-url, upload that zip unchanged: re-zipping it changes the hash, and clients reject a pack whose hash doesn't match.");
        String url = packs.publicUrl();
        if (url != null) reply(sender, "Players are sent " + url + " (check: " + packs.publicUrlStatus() + ").");
        return Command.SINGLE_SUCCESS;
    }

    private static int reply(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private static void error(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }
}
