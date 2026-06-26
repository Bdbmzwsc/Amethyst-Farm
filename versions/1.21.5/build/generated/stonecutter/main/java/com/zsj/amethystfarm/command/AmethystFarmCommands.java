package com.zsj.amethystfarm.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zsj.amethystfarm.AmethystFarmRuntime;
import com.zsj.amethystfarm.CarpetHooks;
import com.zsj.amethystfarm.config.AmethystFarmSettings;
import com.zsj.amethystfarm.data.AmethystFarmDataManager;
import com.zsj.amethystfarm.farm.AmethystFarmProfile;
import com.zsj.amethystfarm.farm.AmethystHarvester;
import com.zsj.amethystfarm.farm.WorkMode;
import com.zsj.amethystfarm.gui.AmethystFarmMenus;
import com.zsj.amethystfarm.network.ModClientSupport;
import com.zsj.amethystfarm.network.SettingsSyncHelper;
import com.zsj.amethystfarm.util.ModCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public final class AmethystFarmCommands {
    public static final String PRIMARY_ROOT = "amethystfarm";
    public static final String SHORT_ROOT = "af";

    private static final SuggestionProvider<CommandSourceStack> RULE_SUGGESTIONS = (ctx, builder) ->
        SharedSuggestionProvider.suggest(AmethystFarmSettings.ruleNames(), builder);

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTIONS = (ctx, builder) ->
        SharedSuggestionProvider.suggest(List.of("IDLE", "SCAN", "HARVEST", "LOCK_MINE"), builder);

    private static final SuggestionProvider<CommandSourceStack> FAKE_PLAYER_SUGGESTIONS = (ctx, builder) -> {
        MinecraftServer server = ctx.getSource().getServer();
        if (server != null) {
            for (ServerPlayer player : CarpetHooks.getOnlineFakePlayers(server)) {
                builder.suggest(ModCompat.profileName(player.getGameProfile()));
            }
        }
        return builder.buildFuture();
    };

    private AmethystFarmCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildRoot(PRIMARY_ROOT));
        dispatcher.register(buildRoot(SHORT_ROOT));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot(String root) {
        return Commands.literal(root)
            .then(Commands.literal("list")
                .executes(AmethystFarmCommands::listFakes))
            .then(Commands.literal("gui")
                .then(Commands.argument("player", EntityArgument.player())
                    .suggests(FAKE_PLAYER_SUGGESTIONS)
                    .executes(ctx -> openGui(ctx, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("harvest")
                .then(Commands.argument("player", EntityArgument.player())
                    .suggests(FAKE_PLAYER_SUGGESTIONS)
                    .executes(ctx -> startHarvest(ctx, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("stop")
                .then(Commands.argument("player", EntityArgument.player())
                    .suggests(FAKE_PLAYER_SUGGESTIONS)
                    .executes(ctx -> stopFake(ctx, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("on")
                .then(Commands.argument("player", EntityArgument.player())
                    .suggests(FAKE_PLAYER_SUGGESTIONS)
                    .executes(ctx -> enableBot(ctx, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("off")
                .then(Commands.argument("player", EntityArgument.player())
                    .suggests(FAKE_PLAYER_SUGGESTIONS)
                    .executes(ctx -> disableBot(ctx, EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("mode")
                .then(Commands.argument("mode", StringArgumentType.word())
                    .suggests(MODE_SUGGESTIONS)
                    .then(Commands.argument("player", EntityArgument.player())
                        .suggests(FAKE_PLAYER_SUGGESTIONS)
                        .executes(ctx -> setMode(
                            ctx,
                            EntityArgument.getPlayer(ctx, "player"),
                            StringArgumentType.getString(ctx, "mode")
                        )))))
            .then(Commands.literal("status")
                .then(Commands.argument("player", EntityArgument.player())
                    .suggests(FAKE_PLAYER_SUGGESTIONS)
                    .executes(ctx -> showStatus(ctx, EntityArgument.getPlayer(ctx, "player")))))
            .then(buildBatchNode())
            .then(Commands.literal("rule")
                .then(Commands.literal("list")
                    .executes(AmethystFarmCommands::listRules))
                .then(Commands.literal("get")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(RULE_SUGGESTIONS)
                        .executes(ctx -> getRule(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("set")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(RULE_SUGGESTIONS)
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                            .executes(ctx -> setRule(
                                ctx,
                                StringArgumentType.getString(ctx, "name"),
                                StringArgumentType.getString(ctx, "value")
                            ))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildBatchNode() {
        return Commands.literal("batch")
            .then(Commands.literal("mode")
                .then(Commands.argument("mode", StringArgumentType.word())
                    .suggests(MODE_SUGGESTIONS)
                    .then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> batchSetMode(
                            ctx,
                            StringArgumentType.getString(ctx, "mode"),
                            EntityArgument.getPlayers(ctx, "targets")
                        )))))
            .then(Commands.literal("harvest")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> batchHarvest(ctx, EntityArgument.getPlayers(ctx, "targets")))));
    }

    private static void persistProfiles(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        if (server != null) {
            AmethystFarmDataManager.persistProfiles(server);
        }
    }

    private static int listFakes(CommandContext<CommandSourceStack> ctx) {
        if (!requireCarpet(ctx)) {
            return 0;
        }
        MinecraftServer server = ctx.getSource().getServer();
        List<ServerPlayer> fakes = CarpetHooks.getOnlineFakePlayers(server);
        if (fakes.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("当前没有在线 Carpet 假人"), false);
            return 0;
        }
        for (ServerPlayer fake : fakes) {
            boolean active = AmethystFarmDataManager.isFarmActive(fake);
            AmethystFarmProfile profile = active ? AmethystFarmDataManager.get(fake) : null;
            ctx.getSource().sendSuccess(() -> Component.literal(
                fake.getName().getString()
                    + " | " + (active ? "农场已启用" : "未纳入农场")
                    + (profile != null ? " | " + profile.getWorkMode().name() : "")
                    + (profile != null ? " | 附近可挖: " + profile.getCrystalCount() : "")
                    + (profile != null ? " | 手长内: " + profile.getReachableCrystalCount() : "")
            ), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("共 " + fakes.size() + " 个假人在线"), false);
        return fakes.size();
    }

    private static int batchSetMode(CommandContext<CommandSourceStack> ctx, String modeName, Collection<ServerPlayer> targets)
        throws CommandSyntaxException {
        if (!requireCarpet(ctx)) {
            return 0;
        }
        WorkMode mode;
        try {
            mode = WorkMode.fromString(modeName);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("未知模式，可用: IDLE, SCAN, HARVEST, LOCK_MINE"));
            return 0;
        }
        String invalid = findNonFakeName(targets);
        if (invalid != null) {
            ctx.getSource().sendFailure(Component.literal("目标必须是 Carpet 假人: " + invalid));
            return 0;
        }
        for (ServerPlayer target : targets) {
            AmethystHarvester.applyWorkMode(target, AmethystFarmDataManager.get(target), mode);
        }
        persistProfiles(ctx);
        int count = targets.size();
        WorkMode appliedMode = mode;
        ctx.getSource().sendSuccess(() -> Component.literal(
            "已将 " + count + " 个假人设为 " + appliedMode.getDisplayName()
        ), true);
        return count;
    }

    private static int batchHarvest(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets)
        throws CommandSyntaxException {
        if (!requireCarpet(ctx)) {
            return 0;
        }
        String invalid = findNonFakeName(targets);
        if (invalid != null) {
            ctx.getSource().sendFailure(Component.literal("目标必须是 Carpet 假人: " + invalid));
            return 0;
        }
        for (ServerPlayer target : targets) {
            AmethystHarvester.applyWorkMode(target, AmethystFarmDataManager.get(target), WorkMode.HARVEST);
        }
        persistProfiles(ctx);
        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.literal(
            "已启用并令 " + count + " 个假人开始挖掘附近紫水晶"
        ), true);
        return count;
    }

    private static String findNonFakeName(Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            if (!CarpetHooks.isFakePlayer(player)) {
                return player.getName().getString();
            }
        }
        return null;
    }

    private static int listRules(CommandContext<CommandSourceStack> ctx) {
        for (String name : AmethystFarmSettings.ruleNames()) {
            AmethystFarmSettings.RuleDefinition rule = AmethystFarmSettings.findRule(name).orElseThrow();
            ctx.getSource().sendSuccess(() -> Component.literal(
                name + " = " + rule.get() + " | " + rule.description() + " | " + rule.valueHint()
            ), false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
            "使用 /" + PRIMARY_ROOT + " rule set <规则> <值> 修改，简写: /" + SHORT_ROOT
        ), false);
        return AmethystFarmSettings.ruleNames().size();
    }

    private static int getRule(CommandContext<CommandSourceStack> ctx, String name) {
        return AmethystFarmSettings.findRule(name).map(rule -> {
            ctx.getSource().sendSuccess(() -> Component.literal(
                rule.id() + " = " + rule.get() + " | " + rule.description()
            ), false);
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("未知规则: " + name));
            return 0;
        });
    }

    private static int setRule(CommandContext<CommandSourceStack> ctx, String name, String value) {
        try {
            AmethystFarmSettings.setValue(name, value.trim());
            if (ctx.getSource().getServer() != null) {
                AmethystFarmDataManager.save(ctx.getSource().getServer());
                SettingsSyncHelper.broadcast(ctx.getSource().getServer());
            }
            AmethystFarmSettings.RuleDefinition rule = AmethystFarmSettings.findRule(name).orElseThrow();
            ctx.getSource().sendSuccess(() -> Component.literal(
                "规则 " + rule.id() + " 已设为 " + rule.get()
            ), true);
            return 1;
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("设置失败: " + e.getMessage()));
            return 0;
        }
    }

    private static boolean requireCarpet(CommandContext<CommandSourceStack> ctx) {
        if (AmethystFarmRuntime.isServerLogicEnabled()) {
            return true;
        }
        ctx.getSource().sendFailure(Component.literal("此功能需要服务端安装 Carpet Mod"));
        return false;
    }

    private static int openGui(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        if (!requireCarpet(ctx)) {
            return 0;
        }
        if (!CarpetHooks.isFakePlayer(target)) {
            ctx.getSource().sendFailure(Component.literal("目标必须是 Carpet 假人，请先用 /player <name> spawn 生成"));
            return 0;
        }
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer opener)) {
            ctx.getSource().sendFailure(Component.literal("必须由玩家打开 GUI"));
            return 0;
        }
        if (!ModClientSupport.hasClientMod(opener)) {
            ctx.getSource().sendFailure(Component.literal(
                "打开 GUI 需要客户端安装 amethystfarm 模组，无模组玩家请使用 /af 命令"
            ));
            return 0;
        }
        AmethystFarmMenus.open(opener, target);
        ctx.getSource().sendSuccess(() -> Component.literal("已打开假人 " + target.getName().getString() + " 的紫水晶农场控制面板"), false);
        return 1;
    }

    private static int startHarvest(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        if (!requireCarpet(ctx) || !requireFake(ctx, target)) {
            return 0;
        }
        AmethystHarvester.applyWorkMode(target, AmethystFarmDataManager.get(target), WorkMode.HARVEST);
        persistProfiles(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal(
            "已启用假人 " + target.getName().getString() + " 的紫水晶挖掘（HARVEST 模式）"
        ), true);
        return 1;
    }

    private static int stopFake(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        return disableBot(ctx, target);
    }

    private static int enableBot(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        if (!requireCarpet(ctx) || !requireFake(ctx, target)) {
            return 0;
        }
        AmethystFarmProfile profile = AmethystFarmDataManager.get(target);
        AmethystHarvester.enableBot(profile);
        persistProfiles(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal(
            "已启用假人 " + target.getName().getString() + " 的紫水晶农场控制（当前模式: "
                + profile.getWorkMode().getDisplayName() + "）"
        ), true);
        return 1;
    }

    private static int disableBot(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        if (!requireFake(ctx, target)) {
            return 0;
        }
        AmethystHarvester.disableBot(target, AmethystFarmDataManager.get(target));
        persistProfiles(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal(
            "已关闭假人 " + target.getName().getString() + " 的紫水晶农场控制，不再影响其 ActionPack"
        ), true);
        return 1;
    }

    private static boolean requireFake(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        if (CarpetHooks.isFakePlayer(target)) {
            return true;
        }
        ctx.getSource().sendFailure(Component.literal("目标必须是 Carpet 假人"));
        return false;
    }

    private static int setMode(CommandContext<CommandSourceStack> ctx, ServerPlayer target, String modeName) {
        if (!requireCarpet(ctx)) {
            return 0;
        }
        if (!CarpetHooks.isFakePlayer(target)) {
            ctx.getSource().sendFailure(Component.literal("目标必须是 Carpet 假人"));
            return 0;
        }
        WorkMode mode;
        try {
            mode = WorkMode.fromString(modeName);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.literal("未知模式，可用: IDLE, SCAN, HARVEST, LOCK_MINE"));
            return 0;
        }
        AmethystFarmProfile profile = AmethystFarmDataManager.get(target);
        AmethystHarvester.applyWorkMode(target, profile, mode);
        persistProfiles(ctx);
        String enabledHint = profile.isBotEnabled() ? "（农场已启用）" : "（农场未启用，请 /af on 或 /af harvest）";
        ctx.getSource().sendSuccess(() -> Component.literal("假人 " + target.getName().getString()
            + " 工作模式已设为: " + mode.getDisplayName() + " — " + mode.getDescription() + enabledHint), true);
        return 1;
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        if (!CarpetHooks.isFakePlayer(target)) {
            ctx.getSource().sendFailure(Component.literal("目标必须是 Carpet 假人"));
            return 0;
        }
        AmethystFarmProfile profile = AmethystFarmDataManager.get(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
            "假人: " + target.getName().getString()
                + " | 农场: " + (profile.isBotEnabled() ? "已启用" : "未启用")
                + " | 模式: " + profile.getWorkMode().getDisplayName()
                + " | 白名单: " + profile.getHarvestWhitelist().formatSummary().getString()
                + " | 扫描半径: " + AmethystFarmSettings.scanRadius
                + " | 手长: " + AmethystFarmSettings.miningReach
                + " | 母岩: " + profile.getBuddingCount()
                + " | 附近可挖: " + profile.getCrystalCount()
                + " | 手长内: " + profile.getReachableCrystalCount()
                + " | 成熟簇: " + profile.getClusterCount()
        ), false);
        return 1;
    }
}
