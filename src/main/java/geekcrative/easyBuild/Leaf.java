package geekcrative.easyBuild;

import org.bukkit.plugin.java.JavaPlugin;

public final class Leaf extends JavaPlugin {

    private static Leaf instance;
    private static TemplateManager templateManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Leaf 插件已加载！");
        
        templateManager = new TemplateManager(getDataFolder());
        
        // Register Command
        getCommand("kjjzgj").setExecutor(new MenuCommand());
        getCommand("bc").setExecutor(new BCCommand());
        getCommand("bcpos").setExecutor(new BCCommand());
        
        // Register Listeners
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new ToolListener(), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Leaf 插件已卸载！");
    }

    public static Leaf getInstance() {
        return instance;
    }

    public static TemplateManager getTemplateManager() {
        return templateManager;
    }
}
