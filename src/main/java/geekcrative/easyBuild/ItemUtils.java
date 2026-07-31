package geekcrative.easyBuild;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemUtils {

    public static ItemStack createToolItem(Material material, String name, String toolId, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            
            if (lore != null && lore.length > 0) {
                List<Component> loreComponents = Arrays.stream(lore)
                        .map(s -> LegacyComponentSerializer.legacySection().deserialize(s))
                        .collect(Collectors.toList());
                meta.lore(loreComponents);
            }
            
            if (toolId != null) {
                NamespacedKey key = new NamespacedKey(Leaf.getInstance(), "tool_id");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, toolId);
            }
            
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public static ItemStack createMenuItem(Material material, String name, String... lore) {
        return createToolItem(material, name, null, lore);
    }
}
