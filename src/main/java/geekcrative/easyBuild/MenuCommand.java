package geekcrative.easyBuild;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class MenuCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用此命令。");
            return true;
        }

        Player player = (Player) sender;
        openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, net.kyori.adventure.text.Component.text("菜单：快捷建筑工具(1)"));

        // Add items as per the first image
        // 1. Iron Shovel: Quick Copy Paste
        inv.setItem(0, ItemUtils.createToolItem(Material.IRON_SHOVEL, "§b快捷复制粘贴", "quick_copy",
                "§f右键： //copy -e",
                "§f左键： //paste -?", 
                "§fQ： 设置-?的部分", 
                "§fShift+右键： //rotate 90", 
                "§fShift+左键： //flip", 
                "§fF： //undo"));

        // 2. Diamond Shovel: Quick Fill Switch
        inv.setItem(1, ItemUtils.createToolItem(Material.DIAMOND_SHOVEL, "§b快捷填充开关", "quick_fill",
                "§f快捷填充模式开启时，", 
                "§f会自动用放置的方块", 
                "§f填充小木斧的选区。", 
                "§e右键点地开/关快捷填充模式", 
                "§a当前： 关闭", 
                "§eF： //undo"));

        // 3. Bone Meal: Quick Tree Planting
        inv.setItem(2, ItemUtils.createToolItem(Material.BONE_MEAL, "§b快捷种树", "quick_tree",
                "§c种树无法撤销！", 
                "§f右键点击方块来种树", 
                "§f左键点地来切换树的种类"));

        // 4. Golden Shovel: Quick Curve Tool
        inv.setItem(3, ItemUtils.createToolItem(Material.GOLDEN_SHOVEL, "§b快捷曲线工具", "quick_curve",
                "§f右键： 保存下一个点", 
                "§fShift+右键： 保存下一个点(nb)", 
                "§fShift+左键： 保存下一个点(nf)", 
                "§f左键： //bc(用选区内容曲线)", 
                "§fU： //undo(撤回上一次动作)", 
                "§fShift+U： 删除上一个点", 
                "§fQ： 删除所有点"));

        // 5. Blue Arrow (Tipped Arrow? Or Lapis? Looks like Arrow). Let's use ARROW.
        // Image 5 says "Three-point Curve Tool"
        inv.setItem(4, ItemUtils.createToolItem(Material.ARROW, "§b三点曲线工具", "three_point_curve",
                "§7无效果", 
                "§f1.右键方块设置点1", 
                "§f2.右键方块设置点3", 
                "§f3.右键方块设置点2", 
                "§f4.此时n=2，左键切换n", 
                "§f5.潜行右键设置点nb", 
                "§f6.潜行左键设置点nf", 
                "§f7.右键方块删除所有点"));

        // 6. Stick: Random Paste Tool
        inv.setItem(5, ItemUtils.createToolItem(Material.STICK, "§d随机粘贴工具", "random_paste",
                "§fQ： 管理标签", 
                "§fShift+Q： 管理模板", 
                "§f右键： 复制", 
                "§fShift+右键： 保存到已选标签", 
                "§f左键： 从已选标签随机//paste -ae", 
                "§fShift+左键： 从已选标签随机//paste -n", 
                "§fF： //undo"));

        player.openInventory(inv);
    }
}
