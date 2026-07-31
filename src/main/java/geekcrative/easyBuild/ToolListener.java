package geekcrative.easyBuild;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import org.bukkit.event.player.PlayerDropItemEvent;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.EditSession;

import java.io.File;

public class ToolListener implements Listener {
    
    private final java.util.Map<java.util.UUID, String> inputMode = new java.util.HashMap<>();
    
    private String getToolId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        NamespacedKey key = new NamespacedKey(Leaf.getInstance(), "tool_id");
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(key, PersistentDataType.STRING, "");
    }
    
    private String getPlainName(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return "";
        return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
    }

    private void pasteClipboard(Player player, ClipboardHolder holder, Location target, boolean ignoreAir, boolean copyEntities, boolean selectAfter) {
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            Operation operation = holder
                    .createPaste(editSession)
                    .to(BlockVector3.at(target.getX(), target.getY(), target.getZ()))
                    .ignoreAirBlocks(ignoreAir)
                    .copyEntities(copyEntities)
                    .build();
            Operations.complete(operation);
            player.sendMessage("§a粘贴成功！");
        } catch (Exception e) {
            player.sendMessage("§c粘贴失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;
        
        String toolId = getToolId(item);
        PlayerState state = StateManager.getState(player);

        // 1. Iron Shovel: Quick Copy Paste
        if ("quick_copy".equals(toolId)) {
            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (player.isSneaking()) {
                    player.performCommand("rotate 90");
                } else {
                    String cmd = "/copy -e";
                    player.performCommand(cmd.startsWith("/") ? cmd.substring(1) : cmd);
                }
            } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (player.isSneaking()) {
                    player.performCommand("flip");
                } else {
                    if (state.pasteAtClickedBlock) {
                        Block clicked = player.getTargetBlockExact(10);
                        if (event.getClickedBlock() != null) clicked = event.getClickedBlock();
                        
                        if (clicked != null) {
                            try {
                                LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
                                ClipboardHolder holder = session.getClipboard();
                                pasteClipboard(player, holder, clicked.getLocation(), state.pasteIgnoreAir, state.pasteEntities, state.pasteSelect);
                            } catch (EmptyClipboardException e) {
                                player.sendMessage("§c剪贴板为空！");
                            }
                        } else {
                             player.sendMessage("§c请对准一个方块粘贴 (或关闭参考点模式)");
                        }
                    } else {
                        StringBuilder cmd = new StringBuilder("/paste");
                        if (state.pasteIgnoreAir) cmd.append(" -a");
                        if (state.pasteEntities) cmd.append(" -e");
                        if (state.pasteSelect) cmd.append(" -s");
                        String s = cmd.toString();
                        player.performCommand(s.startsWith("/") ? s.substring(1) : s);
                    }
                }
            }
        }
        
        // 2. Bone Meal: Quick Tree Planting
        else if ("quick_tree".equals(toolId)) {
            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Block block = event.getClickedBlock();
                if (block != null) {
                    block.getWorld().generateTree(block.getLocation().add(0, 1, 0), state.currentTreeType);
                }
            } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                openTreeMenu(player);
            }
        }

        // 3. Diamond Shovel: Quick Fill Switch
        else if ("quick_fill".equals(toolId)) {
            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                state.quickFillEnabled = !state.quickFillEnabled;
                player.sendMessage("§e快捷填充模式: " + (state.quickFillEnabled ? "§a开启" : "§c关闭"));
            } 
        }

        // 4. Golden Shovel: Quick Curve Tool
        else if ("quick_curve".equals(toolId)) {
            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Block clicked = event.getClickedBlock();
                if (clicked == null) {
                    clicked = player.getTargetBlockExact(10);
                }
                if (clicked == null) {
                    player.sendMessage("§c请对准一个方块设置点！");
                    return;
                }
                Location loc = clicked.getLocation();
                state.bezierPoints.add(loc);
                int index = state.bezierPoints.size();
                player.sendMessage("§a第" + index + "个点(" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + ") 已保存，右键点击下一个点");
            } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (player.isSneaking()) {
                    Block clicked = event.getClickedBlock();
                    if (clicked == null) clicked = player.getTargetBlockExact(10);
                    if (clicked == null) {
                        player.sendMessage("§c请对准一个方块设置点！");
                        return;
                    }
                    Location loc = clicked.getLocation();
                    state.bezierPoints.add(loc);
                    int index = state.bezierPoints.size();
                    player.sendMessage("§a第" + index + "个点(" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + ") 已保存，右键点击下一个点");
                } else {
                    if (!hasWorldEditSelection(player)) {
                        player.sendMessage("§c请先用木斧完成选区！");
                        return;
                    }
                    if (state.bezierPoints.size() < 2) {
                        player.sendMessage("§c需要至少2个点，右键继续添加点。");
                        return;
                    }
                    player.performCommand("bc");
                }
            }
        }
        
        // 5. Arrow: Three-point Curve Tool
        else if ("three_point_curve".equals(toolId)) {
             event.setCancelled(true);
             if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                 if (player.isSneaking()) {
                     state.nb = event.getClickedBlock().getLocation();
                     player.sendMessage("§a已设置控制点 nb");
                 } else {
                     if (state.step == 1) {
                         state.p1 = event.getClickedBlock().getLocation();
                         state.step = 2;
                         player.sendMessage("§a已设置点 1 (P1)");
                     } else if (state.step == 2) {
                         state.p3 = event.getClickedBlock().getLocation();
                         state.step = 3;
                         player.sendMessage("§a已设置点 3 (P3)");
                     } else if (state.step == 3) {
                         state.p2 = event.getClickedBlock().getLocation();
                         state.step = 4;
                         player.sendMessage("§a已设置点 2 (P2)");
                     } else {
                         state.p1 = null;
                         state.p2 = null;
                         state.p3 = null;
                         state.nb = null;
                         state.nf = null;
                         state.step = 1;
                         player.sendMessage("§a已清除所有点");
                     }
                 }
             } else if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
                 if (player.isSneaking()) {
                     Block block = player.getTargetBlock(null, 5);
                     if (block != null && block.getType() != Material.AIR) {
                         state.nf = block.getLocation();
                         player.sendMessage("§a已设置控制点 nf");
                     } else {
                         player.sendMessage("§c请对准方块设置 nf");
                     }
                 } else {
                     state.n = (state.n == 2) ? 3 : 2;
                     player.sendMessage("§a已切换 n = " + state.n);
                 }
             }
        }
        
        // 6. Stick: Random Paste Tool
        else if ("random_paste".equals(toolId)) {
            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (player.isSneaking()) {
                    player.sendMessage("§e请在聊天栏输入模板名称 (保存到 '" + state.currentTag + "'):");
                    inputMode.put(player.getUniqueId(), "save_template");
                } else {
                    player.performCommand("copy");
                }
            } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                File templateFile = Leaf.getTemplateManager().getRandomTemplateFile(state.currentTag);
                if (templateFile != null) {
                    Clipboard clipboard = Leaf.getTemplateManager().loadTemplate(templateFile);
                    if (clipboard != null) {
                        ClipboardHolder holder = new ClipboardHolder(clipboard);
                        Location target = (event.getClickedBlock() != null) ? event.getClickedBlock().getLocation() : player.getLocation();
                        
                        boolean ignoreAir = true; 
                        if (player.isSneaking()) {
                            ignoreAir = false; 
                        }
                        
                        pasteClipboard(player, holder, target, ignoreAir, true, false);
                        player.sendMessage("§a已随机粘贴: " + templateFile.getName());
                    } else {
                        player.sendMessage("§c加载模板失败！");
                    }
                } else {
                    player.sendMessage("§c当前标签 '" + state.currentTag + "' 下没有模板！");
                }
            }
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = event.getOffHandItem(); 
        String toolId = getToolId(tool);
        
        if (toolId.isEmpty()) return;

        if ("quick_copy".equals(toolId) || "quick_fill".equals(toolId) || "random_paste".equals(toolId)) {
            event.setCancelled(true);
            player.performCommand("undo");
        } else if ("three_point_curve".equals(toolId)) {
            event.setCancelled(true);
        } else if ("quick_curve".equals(toolId)) {
            event.setCancelled(true);
            if (player.isSneaking()) {
                PlayerState state = StateManager.getState(player);
                if (!state.bezierPoints.isEmpty()) {
                    Location removed = state.bezierPoints.remove(state.bezierPoints.size() - 1);
                    player.sendMessage("§a已删除上一个点(" + removed.getBlockX() + " " + removed.getBlockY() + " " + removed.getBlockZ() + ")");
                } else {
                    player.sendMessage("§c没有可以删除的点");
                }
            } else {
                player.performCommand("undo");
            }
        }
    }
    
    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        String toolId = getToolId(item);
        
        if ("quick_copy".equals(toolId)) {
            event.setCancelled(true);
            openPasteSettingsMenu(player);
        } else if ("random_paste".equals(toolId)) {
             event.setCancelled(true);
             openTemplateMainMenu(player);
        } else if ("quick_curve".equals(toolId)) {
            event.setCancelled(true);
            PlayerState state = StateManager.getState(player);
            state.bezierPoints.clear();
            player.sendMessage("§a已清除所有曲线点");
        }
    }

    private boolean hasWorldEditSelection(Player player) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
            session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void openPasteSettingsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, net.kyori.adventure.text.Component.text("菜单：粘贴设置"));
        PlayerState state = StateManager.getState(player);
        
        inv.setItem(0, ItemUtils.createMenuItem(Material.TARGET, "§b点击方块粘贴", 
                "§7当前: " + (state.pasteAtClickedBlock ? "§a开启" : "§c关闭"),
                "§7点击切换"));
        inv.setItem(1, ItemUtils.createMenuItem(Material.FEATHER, "§b忽略空气 (-a)", 
                "§7当前: " + (state.pasteIgnoreAir ? "§a开启" : "§c关闭"),
                "§7点击切换"));
        inv.setItem(2, ItemUtils.createMenuItem(Material.ARMOR_STAND, "§b粘贴实体 (-e)", 
                "§7当前: " + (state.pasteEntities ? "§a开启" : "§c关闭"),
                "§7点击切换"));
        inv.setItem(3, ItemUtils.createMenuItem(Material.STRUCTURE_VOID, "§b粘贴后选中 (-s)", 
                "§7当前: " + (state.pasteSelect ? "§a开启" : "§c关闭"),
                "§7点击切换"));
                
        inv.setItem(8, ItemUtils.createMenuItem(Material.NAME_TAG, "§e上传为模板", 
                "§7将当前选区保存为模板",
                "§7点击后在聊天栏输入名称"));
                
        player.openInventory(inv);
    }

    private void openTemplateMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, net.kyori.adventure.text.Component.text("菜单：随机粘贴管理"));
        PlayerState state = StateManager.getState(player);
        inv.setItem(4, ItemUtils.createMenuItem(Material.NAME_TAG, "§e当前标签: " + state.currentTag, "§7点击切换标签"));
        inv.setItem(11, ItemUtils.createMenuItem(Material.BEACON, "§b上传选区为模板", 
                "§7将当前WorldEdit选区", 
                "§7保存到当前标签: " + state.currentTag,
                "§e点击开始上传"));
        inv.setItem(15, ItemUtils.createMenuItem(Material.WRITABLE_BOOK, "§b新建标签", 
                "§7创建一个新的分类标签",
                "§e点击创建"));
        player.openInventory(inv);
    }
    
    private void openTagSelectionMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, net.kyori.adventure.text.Component.text("菜单：选择标签"));
        TemplateManager tm = Leaf.getTemplateManager();
        int i = 0;
        for (String tag : tm.getTags()) {
            if (i >= 54) break;
            int count = tm.getTemplates(tag).size();
            inv.setItem(i++, ItemUtils.createMenuItem(Material.PAPER, "§a" + tag, "§7包含 " + count + " 个模板", "§e点击选择"));
        }
        player.openInventory(inv);
    }
    
    private Material getTreeIcon(TreeType type) {
        switch (type) {
            case TREE: return Material.OAK_SAPLING;
            case BIG_TREE: return Material.OAK_SAPLING;
            case REDWOOD: return Material.SPRUCE_SAPLING;
            case TALL_REDWOOD: return Material.SPRUCE_SAPLING;
            case BIRCH: return Material.BIRCH_SAPLING;
            case TALL_BIRCH: return Material.BIRCH_SAPLING;
            case JUNGLE: return Material.JUNGLE_SAPLING;
            case SMALL_JUNGLE: return Material.JUNGLE_SAPLING;
            case JUNGLE_BUSH: return Material.JUNGLE_LEAVES;
            case COCOA_TREE: return Material.COCOA_BEANS;
            case ACACIA: return Material.ACACIA_SAPLING;
            case DARK_OAK: return Material.DARK_OAK_SAPLING;
            case MEGA_REDWOOD: return Material.SPRUCE_SAPLING;
            case SWAMP: return Material.OAK_SAPLING; // Swamp tree uses oak logs/leaves but is distinct. No swamp sapling.
            case CHORUS_PLANT: return Material.CHORUS_FLOWER;
            case CRIMSON_FUNGUS: return Material.CRIMSON_FUNGUS;
            case WARPED_FUNGUS: return Material.WARPED_FUNGUS;
            case AZALEA: return Material.AZALEA;
            case MANGROVE: return Material.MANGROVE_PROPAGULE;
            case TALL_MANGROVE: return Material.MANGROVE_PROPAGULE;
            case CHERRY: return Material.CHERRY_SAPLING;
            case RED_MUSHROOM: return Material.RED_MUSHROOM;
            case BROWN_MUSHROOM: return Material.BROWN_MUSHROOM;
            default: return Material.OAK_SAPLING;
        }
    }

    private void openTreeMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, net.kyori.adventure.text.Component.text("菜单：选择树种"));
        int i = 0;
        for (TreeType type : TreeType.values()) {
            if (i >= 54) break;
            inv.setItem(i++, ItemUtils.createMenuItem(getTreeIcon(type), "§a" + getChineseTreeName(type), "§7点击切换"));
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (inputMode.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String action = inputMode.remove(player.getUniqueId());
            String input = event.getMessage();
            
            Bukkit.getScheduler().runTask(Leaf.getInstance(), () -> {
                if ("create_tag".equals(action)) {
                    Leaf.getTemplateManager().createTag(input);
                    player.sendMessage("§a标签 '" + input + "' 创建成功！");
                    StateManager.getState(player).currentTag = input;
                    openTemplateMainMenu(player);
                } else if ("save_template".equals(action)) {
                    saveSelectionAsTemplate(player, input);
                }
            });
        }
    }

    private void saveSelectionAsTemplate(Player player, String name) {
        try {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
            ClipboardHolder holder = session.getClipboard();
            Clipboard clipboard = holder.getClipboard();
            
            String tag = StateManager.getState(player).currentTag;
            boolean success = Leaf.getTemplateManager().saveTemplate(tag, name, clipboard);
            
            if (success) {
                player.sendMessage("§a模板 '" + name + "' 已保存到标签 '" + tag + "'！");
            } else {
                player.sendMessage("§c保存失败，请检查后台报错。");
            }
        } catch (EmptyClipboardException e) {
            player.sendMessage("§c你的剪贴板为空！请先复制选区 (//copy)。");
        } catch (Exception e) {
            player.sendMessage("§c保存出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Use PlainText for title comparison, robust against colors
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        
        if (title.contains("菜单：粘贴设置")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            PlayerState state = StateManager.getState(player);
            
            int slot = event.getSlot();
            if (slot == 0) {
                state.pasteAtClickedBlock = !state.pasteAtClickedBlock;
            } else if (slot == 1) {
                state.pasteIgnoreAir = !state.pasteIgnoreAir;
            } else if (slot == 2) {
                state.pasteEntities = !state.pasteEntities;
            } else if (slot == 3) {
                state.pasteSelect = !state.pasteSelect;
            }
            openPasteSettingsMenu(player);
        } else if (title.contains("菜单：随机粘贴管理")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            
            if (event.getSlot() == 4) { // Change Tag
                openTagSelectionMenu(player);
            } else if (event.getSlot() == 11) { // Upload
                player.closeInventory();
                player.sendMessage("§e请在聊天栏输入模板名称:");
                inputMode.put(player.getUniqueId(), "save_template");
            } else if (event.getSlot() == 15) { // Create Tag
                player.closeInventory();
                player.sendMessage("§e请在聊天栏输入新标签名称:");
                inputMode.put(player.getUniqueId(), "create_tag");
            }
        } else if (title.contains("菜单：选择标签")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            Player player = (Player) event.getWhoClicked();
            
            String tagName = PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().getItemMeta().displayName()).replace("§a", "").trim();
            // Remove any prefix/suffix if present
            // But plain text serialization should handle color codes removal.
            
            StateManager.getState(player).currentTag = tagName;
            player.sendMessage("§a已选择标签: " + tagName);
            openTemplateMainMenu(player);
        } else if (title.contains("菜单：选择树种")) {
             event.setCancelled(true);
             if (event.getCurrentItem() == null) return;
             Player player = (Player) event.getWhoClicked();
             PlayerState state = StateManager.getState(player);
             
             String name = PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().getItemMeta().displayName());
             for (TreeType type : TreeType.values()) {
                 // getChineseTreeName returns plain string
                 // name from item might have formatting stripped by serializer
                 // Let's match carefully
                 if (name.contains(getChineseTreeName(type))) {
                     state.currentTreeType = type;
                     player.sendMessage("§a已切换树种: " + getChineseTreeName(type));
                     player.closeInventory();
                     break;
                 }
             }
        }
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerState state = StateManager.getState(player);
        
        if (state.quickFillEnabled) {
            event.setCancelled(true);
            ItemStack hand = event.getItemInHand();
            if (!hand.getType().isBlock()) return;
            String blockName = hand.getType().name().toLowerCase();
            player.performCommand("set " + blockName);
            player.sendActionBar(net.kyori.adventure.text.Component.text("§a已填充: " + blockName));
        }
    }

    private String getChineseTreeName(TreeType type) {
        switch (type) {
            case TREE: return "橡木";
            case BIG_TREE: return "大型橡木";
            case REDWOOD: return "云杉";
            case TALL_REDWOOD: return "大型云杉";
            case BIRCH: return "白桦";
            case JUNGLE: return "丛林木";
            case SMALL_JUNGLE: return "小型丛林木";
            case COCOA_TREE: return "可可树";
            case JUNGLE_BUSH: return "丛林灌木";
            case RED_MUSHROOM: return "红蘑菇";
            case BROWN_MUSHROOM: return "棕蘑菇";
            case SWAMP: return "沼泽橡木";
            case ACACIA: return "金合欢";
            case DARK_OAK: return "深色橡木";
            case MEGA_REDWOOD: return "巨型云杉";
            case TALL_BIRCH: return "高白桦";
            case CHORUS_PLANT: return "紫颂植物";
            case CRIMSON_FUNGUS: return "绯红菌";
            case WARPED_FUNGUS: return "诡异菌";
            case AZALEA: return "杜鹃";
            case MANGROVE: return "红树";
            case TALL_MANGROVE: return "高红树";
            case CHERRY: return "樱花";
            default: return type.name();
        }
    }
}
