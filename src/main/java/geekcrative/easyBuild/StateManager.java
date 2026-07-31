package geekcrative.easyBuild;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StateManager {
    private static final Map<UUID, PlayerState> states = new HashMap<>();

    public static PlayerState getState(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), k -> new PlayerState());
    }
}
