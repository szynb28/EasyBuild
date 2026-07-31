package geekcrative.easyBuild;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class TemplateManager {
    private final File templateDir;
    // Map<TagName, List<FileName>>
    private final Map<String, List<String>> tags = new HashMap<>();

    public TemplateManager(File dataFolder) {
        this.templateDir = new File(dataFolder, "templates");
        if (!this.templateDir.exists()) {
            this.templateDir.mkdirs();
        }
        loadTags();
    }

    private void loadTags() {
        // Simple directory scanning: Tag = Subfolder
        // OR Tag = Prefix?
        // Let's use Subfolders as Tags for simplicity.
        // Root folder files are "default" tag.
        
        tags.clear();
        tags.put("default", new ArrayList<>());
        
        File[] files = templateDir.listFiles();
        if (files == null) return;
        
        for (File f : files) {
            if (f.isDirectory()) {
                String tagName = f.getName();
                List<String> templates = new ArrayList<>();
                File[] subFiles = f.listFiles((dir, name) -> name.endsWith(".schem"));
                if (subFiles != null) {
                    for (File sub : subFiles) {
                        templates.add(sub.getName());
                    }
                }
                tags.put(tagName, templates);
            } else if (f.isFile() && f.getName().endsWith(".schem")) {
                tags.get("default").add(f.getName());
            }
        }
    }

    public Set<String> getTags() {
        return tags.keySet();
    }

    public List<String> getTemplates(String tag) {
        return tags.getOrDefault(tag, Collections.emptyList());
    }
    
    public void createTag(String tagName) {
        File tagDir = new File(templateDir, tagName);
        if (!tagDir.exists()) {
            tagDir.mkdirs();
        }
        tags.putIfAbsent(tagName, new ArrayList<>());
    }

    public boolean saveTemplate(String tag, String name, Clipboard clipboard) {
        File folder = "default".equals(tag) ? templateDir : new File(templateDir, tag);
        if (!folder.exists()) folder.mkdirs();
        
        File file = new File(folder, name + ".schem");
        try (ClipboardWriter writer = ClipboardFormats.findByAlias("schem").getWriter(new FileOutputStream(file))) {
            writer.write(clipboard);
            
            // Update cache
            tags.computeIfAbsent(tag, k -> new ArrayList<>()).add(name + ".schem");
            return true;
        } catch (IOException e) {
            Leaf.getInstance().getLogger().log(Level.SEVERE, "Failed to save template", e);
            return false;
        }
    }

    public File getRandomTemplateFile(String tag) {
        List<String> templates = getTemplates(tag);
        if (templates.isEmpty()) return null;
        
        String randomName = templates.get(new Random().nextInt(templates.size()));
        File folder = "default".equals(tag) ? templateDir : new File(templateDir, tag);
        return new File(folder, randomName);
    }

    public Clipboard loadTemplate(File file) {
        if (file == null || !file.exists()) return null;
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) return null;
        
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        } catch (IOException e) {
            Leaf.getInstance().getLogger().log(Level.SEVERE, "Failed to load template", e);
            return null;
        }
    }

    public Clipboard loadTemplate(String tag, String filename) {
        File folder = "default".equals(tag) ? templateDir : new File(templateDir, tag);
        File file = new File(folder, filename);
        return loadTemplate(file);
    }
}
