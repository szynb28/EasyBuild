package geekcrative.easyBuild;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (title.contains("菜单：快捷建筑工具(1)")) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null) return;
            
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            
            // Give the item to the player
            player.getInventory().addItem(clickedItem);
            player.sendMessage("§a已获取工具: " + PlainTextComponentSerializer.plainText().serialize(clickedItem.displayName()));
            player.closeInventory();
        }
    }
}
