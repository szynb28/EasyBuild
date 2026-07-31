package geekcrative.easyBuild;

import org.bukkit.Location;
import org.bukkit.TreeType;

import java.util.ArrayList;
import java.util.List;

public class PlayerState {
    // Quick Copy Paste
    public boolean pasteAtClickedBlock = false; // 1. Reference point
    public boolean pasteIgnoreAir = true;       // 2. Ignore air (-a)
    public boolean pasteEntities = true;        // 3. Paste entities (-e)
    public boolean pasteSelect = false;         // 4. Paste selection (-s)

    // Quick Tree Planting
    public TreeType currentTreeType = TreeType.TREE;

    // Quick Fill
    public boolean quickFillEnabled = false;
    // quickFillCancel removed as per request

    // Quick Curve (Golden Shovel)
    public List<Location> bezierPoints = new ArrayList<>();
    public String curveBlock = "stone"; // Default block
    
    // Three-point Curve (Arrow)
    public Location p1;
    public Location p2;
    public Location p3;
    public int n = 2; // Default n=2
    public Location nb; // Control point back?
    public Location nf; // Control point forward?
    public int step = 1; // 1=Set p1, 2=Set p3, 3=Set p2

    // Random Paste
    public String currentTag = "default";
}
