package com.zsj.amethystfarm.gui;

import com.zsj.amethystfarm.CarpetHooks;
import com.zsj.amethystfarm.data.AmethystFarmDataManager;
import com.zsj.amethystfarm.farm.AmethystFarmProfile;
import com.zsj.amethystfarm.farm.AmethystHarvester;
import com.zsj.amethystfarm.farm.HarvestWhitelist;
import com.zsj.amethystfarm.farm.WorkMode;
import com.zsj.amethystfarm.util.ModCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.UUID;

public class AmethystFarmMenu extends AbstractContainerMenu {
    public static final int SLOT_COUNT = 14;

    public static final int BUTTON_IDLE = 0;
    public static final int BUTTON_SCAN = 1;
    public static final int BUTTON_HARVEST = 2;
    public static final int BUTTON_LOCK = 3;
    public static final int BUTTON_STOP = 4;
    public static final int BUTTON_STATUS = 5;
    public static final int BUTTON_DECOR = 6;
    public static final int BUTTON_DECOR2 = 7;
    public static final int BUTTON_DECOR3 = 8;

    public static final int WHITELIST_SMALL = 9;
    public static final int WHITELIST_MEDIUM = 10;
    public static final int WHITELIST_LARGE = 11;
    public static final int WHITELIST_CLUSTER = 12;
    public static final int WHITELIST_SIDES = 13;

    private final SimpleContainer buttons = new SimpleContainer(SLOT_COUNT);
    private final UUID fakePlayerId;

    public AmethystFarmMenu(int syncId, Inventory playerInventory, UUID fakePlayerId) {
        super(AmethystFarmMenuTypes.AMETHYST_FARM_MENU, syncId);
        this.fakePlayerId = fakePlayerId;
        refreshButtons(null);

        for (int i = 0; i < 9; i++) {
            addSlot(new AmethystFarmButtonSlot(buttons, i, 8 + i * 18, 27));
        }
        int whitelistX = 26;
        for (int i = 0; i < 5; i++) {
            addSlot(new AmethystFarmButtonSlot(buttons, WHITELIST_SMALL + i, whitelistX + i * 18, 57));
        }

        addPlayerInventory(playerInventory);
    }

    public void syncFromProfile(ServerPlayer operator) {
        ServerPlayer fake = findFakePlayer(ModCompat.serverOf(operator));
        if (fake != null) {
            refreshButtons(AmethystFarmDataManager.get(fake));
            broadcastFullState();
        }
    }

    private void refreshButtons(AmethystFarmProfile profile) {
        HarvestWhitelist whitelist = profile != null ? profile.getHarvestWhitelist() : new HarvestWhitelist();

        buttons.setItem(BUTTON_IDLE, named(Items.GRAY_STAINED_GLASS_PANE, "空闲模式", WorkMode.IDLE));
        buttons.setItem(BUTTON_SCAN, named(Items.AMETHYST_SHARD, "扫描紫水晶", WorkMode.SCAN));
        buttons.setItem(BUTTON_HARVEST, named(Items.IRON_PICKAXE, "挖掘附近紫水晶", WorkMode.HARVEST));
        buttons.setItem(BUTTON_LOCK, named(Items.SPYGLASS, "单点锁定挖掘", WorkMode.LOCK_MINE));
        buttons.setItem(BUTTON_STOP, named(Items.BARRIER, "停止", WorkMode.IDLE));
        buttons.setItem(BUTTON_STATUS, named(Items.BOOK, "刷新状态", null));
        buttons.setItem(BUTTON_DECOR, named(Items.AMETHYST_BLOCK, "紫水晶农场控制", null));
        boolean botEnabled = profile != null && profile.isBotEnabled();
        buttons.setItem(BUTTON_DECOR2, toggleItem(
            Items.REDSTONE_TORCH,
            "农场总开关",
            "关闭后不再接管此假人 ActionPack",
            botEnabled
        ));
        buttons.setItem(BUTTON_DECOR3, named(Items.PURPLE_STAINED_GLASS_PANE, "—", null));

        buttons.setItem(WHITELIST_SMALL, toggleItem(
            Blocks.SMALL_AMETHYST_BUD.asItem(),
            "小芽",
            "小型紫水晶芽",
            whitelist.isSmallBud()
        ));
        buttons.setItem(WHITELIST_MEDIUM, toggleItem(
            Blocks.MEDIUM_AMETHYST_BUD.asItem(),
            "中芽",
            "中型紫水晶芽",
            whitelist.isMediumBud()
        ));
        buttons.setItem(WHITELIST_LARGE, toggleItem(
            Blocks.LARGE_AMETHYST_BUD.asItem(),
            "大芽",
            "大型紫水晶芽",
            whitelist.isLargeBud()
        ));
        buttons.setItem(WHITELIST_CLUSTER, toggleItem(
            Blocks.AMETHYST_CLUSTER.asItem(),
            "成熟簇",
            "成熟紫水晶簇",
            whitelist.isCluster()
        ));
        buttons.setItem(WHITELIST_SIDES, toggleItem(
            Items.IRON_BARS,
            "仅母岩侧面",
            "只挖母岩四侧（不含顶/底）",
            whitelist.isSidesOnly()
        ));
    }

    private static ItemStack named(Item item, String name, WorkMode mode) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (mode != null) {
            stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(mode.getDescription()))));
        }
        return stack;
    }

    private static ItemStack toggleItem(Item enabledItem, String name, String hint, boolean enabled) {
        ItemStack stack = new ItemStack(enabled ? enabledItem : Items.GRAY_STAINED_GLASS_PANE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name + (enabled ? " §a[开]" : " §7[关]")));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal(hint),
            Component.literal("点击切换")
        )));
        return stack;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 148));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId >= 0 && buttonId < SLOT_COUNT
            && !player.level().isClientSide()
            && player instanceof ServerPlayer serverPlayer) {
            handleButton(serverPlayer, buttonId);
            return true;
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < SLOT_COUNT
            && !player.level().isClientSide()
            && player instanceof ServerPlayer serverPlayer) {
            handleButton(serverPlayer, slotId);
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    private void handleButton(ServerPlayer operator, int buttonId) {
        ServerPlayer fake = findFakePlayer(ModCompat.serverOf(operator));
        if (fake == null) {
            operator.sendSystemMessage(Component.literal("找不到绑定的假人"));
            return;
        }
        if (!CarpetHooks.isFakePlayer(fake)) {
            operator.sendSystemMessage(Component.literal("目标必须是 Carpet 假人"));
            return;
        }

        AmethystFarmProfile profile = AmethystFarmDataManager.get(fake);
        HarvestWhitelist whitelist = profile.getHarvestWhitelist();

        switch (buttonId) {
            case BUTTON_IDLE -> AmethystHarvester.applyWorkMode(fake, profile, WorkMode.IDLE);
            case BUTTON_SCAN -> AmethystHarvester.applyWorkMode(fake, profile, WorkMode.SCAN);
            case BUTTON_HARVEST -> AmethystHarvester.applyWorkMode(fake, profile, WorkMode.HARVEST);
            case BUTTON_LOCK -> AmethystHarvester.applyWorkMode(fake, profile, WorkMode.LOCK_MINE);
            case BUTTON_STOP -> AmethystHarvester.disableBot(fake, profile);
            case BUTTON_DECOR2 -> {
                if (profile.isBotEnabled()) {
                    AmethystHarvester.disableBot(fake, profile);
                } else {
                    AmethystHarvester.enableBot(profile);
                }
            }
            case BUTTON_STATUS -> sendStatus(operator, fake, profile);
            case WHITELIST_SMALL -> whitelist.toggleSmallBud();
            case WHITELIST_MEDIUM -> whitelist.toggleMediumBud();
            case WHITELIST_LARGE -> whitelist.toggleLargeBud();
            case WHITELIST_CLUSTER -> whitelist.toggleCluster();
            case WHITELIST_SIDES -> whitelist.toggleSidesOnly();
        }

        refreshButtons(profile);
        broadcastFullState();

        if (buttonId != BUTTON_STATUS && buttonId != BUTTON_DECOR && buttonId != BUTTON_DECOR3) {
            AmethystFarmDataManager.persistProfiles(ModCompat.serverOf(operator));
        }

        if (buttonId == BUTTON_DECOR2 || buttonId == BUTTON_STOP) {
            operator.sendSystemMessage(Component.literal(
                "假人农场: " + (profile.isBotEnabled() ? "已启用" : "已关闭（不影响其他 Carpet 动作）")
            ));
        } else if (buttonId <= BUTTON_LOCK) {
            operator.sendSystemMessage(Component.literal(
                "假人模式: " + profile.getWorkMode().getDisplayName()
                    + (profile.isBotEnabled() ? "" : "（农场未启用）")
            ));
        } else if (buttonId >= WHITELIST_SMALL && buttonId <= WHITELIST_SIDES) {
            operator.sendSystemMessage(Component.literal("挖掘白名单: " + whitelist.formatSummary().getString()));
        }
    }

    private void sendStatus(ServerPlayer operator, ServerPlayer fake, AmethystFarmProfile profile) {
        operator.sendSystemMessage(Component.literal(
            "假人 " + fake.getName().getString()
                + " | 农场: " + (profile.isBotEnabled() ? "已启用" : "未启用")
                + " | 模式: " + profile.getWorkMode().getDisplayName()
                + " | 白名单: " + profile.getHarvestWhitelist().formatSummary().getString()
                + " | 附近可挖: " + profile.getCrystalCount()
                + " | 手长内: " + profile.getReachableCrystalCount()
                + " | 成熟簇: " + profile.getClusterCount()
        ));
    }

    private ServerPlayer findFakePlayer(net.minecraft.server.MinecraftServer server) {
        return server.getPlayerList().getPlayer(fakePlayerId);
    }
}
