package geekcrative.easyBuild;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BCCommand implements CommandExecutor {


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        PlayerState state = StateManager.getState(player);

        if (label.equalsIgnoreCase("bcpos")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("clearall")) {
                    state.bezierPoints.clear();
                    player.sendMessage("§a已清除所有曲线点");
                    return true;
                }
                if (args[0].equalsIgnoreCase("undo")) {
                    if (!state.bezierPoints.isEmpty()) {
                        Location removed = state.bezierPoints.remove(state.bezierPoints.size() - 1);
                        player.sendMessage("§a已移除最后一个点 @ " + removed.getBlockX() + "," + removed.getBlockY() + "," + removed.getBlockZ());
                    } else {
                        player.sendMessage("§c没有可以移除的点");
                    }
                    return true;
                }
                if (args[0].equalsIgnoreCase("set") && args.length > 1) {
                    // Add target block location
                    Block block = player.getTargetBlockExact(10);
                    if (block == null) {
                        player.sendMessage("§c请对准一个方块！");
                        return true;
                    }
                    Location loc = block.getLocation();
                    state.bezierPoints.add(loc);
                    String type = args[1];
                    int index = state.bezierPoints.size();
                    player.sendMessage("§a第" + index + "个点(" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + ") 已保存，右键点击下一个点");
                    return true;
                }
            }
        } else if (label.equalsIgnoreCase("bc")) {
            if (state.bezierPoints.size() < 2) {
                player.sendMessage("§c需要至少2个点");
                return true;
            }
            
            try {
                SelectionTemplate template = readTemplateFromSelection(player, state.bezierPoints);
                if (template.blocks.isEmpty()) {
                    player.sendMessage("§c你的选区里没有可用方块(全是空气)。");
                    return true;
                }
                buildCurveFromTemplate(player, state.bezierPoints, template);
                player.sendMessage("§a曲线生成完成：模板方块数 " + template.blocks.size());
            } catch (Exception e) {
                player.sendMessage("§c生成曲线失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return true;
    }

    private static final class TemplateBlock {
        private final int forward;
        private final int up;
        private final double lateral;
        private final BlockState blockState;

        private TemplateBlock(int forward, int up, double lateral, BlockState blockState) {
            this.forward = forward;
            this.up = up;
            this.lateral = lateral;
            this.blockState = blockState;
        }
    }

    private static final class SelectionTemplate {
        private final int forwardSize;
        private final List<TemplateBlock> blocks;

        private SelectionTemplate(int forwardSize, List<TemplateBlock> blocks) {
            this.forwardSize = forwardSize;
            this.blocks = blocks;
        }
    }

    private SelectionTemplate readTemplateFromSelection(Player player, List<Location> curvePoints) throws Exception {
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
        Region region;
        try {
            region = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (Exception e) {
            throw new Exception("请先用木斧完成选区！");
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int sizeX = max.x() - min.x() + 1;
        int sizeZ = max.z() - min.z() + 1;

        Location c0 = curvePoints.get(0);
        Location c1 = curvePoints.get(1);
        double cdx = c1.getX() - c0.getX();
        double cdz = c1.getZ() - c0.getZ();
        boolean curveForwardIsX = Math.abs(cdx) >= Math.abs(cdz);

        boolean forwardIsX = curveForwardIsX;
        if (forwardIsX && sizeX <= 1 && sizeZ > 1) forwardIsX = false;
        if (!forwardIsX && sizeZ <= 1 && sizeX > 1) forwardIsX = true;

        boolean reverseForward = forwardIsX ? (cdx < 0) : (cdz < 0);

        int forwardSize = forwardIsX ? sizeX : sizeZ;
        int widthSize = forwardIsX ? sizeZ : sizeX;
        double widthCenter = (widthSize - 1) / 2.0;

        int maxBlocks = 300000;
        List<TemplateBlock> blocks = new ArrayList<>(Math.min(maxBlocks, 8192));

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            for (BlockVector3 v : region) {
                BlockState bs = editSession.getBlock(v);
                if (bs.getBlockType() == BlockTypes.AIR) continue;

                int dx = v.x() - min.x();
                int dy = v.y() - min.y();
                int dz = v.z() - min.z();

                int forward = forwardIsX ? dx : dz;
                int lateralRaw = forwardIsX ? dz : dx;
                double lateral = lateralRaw - widthCenter;
                if (reverseForward) {
                    forward = (forwardSize - 1) - forward;
                    lateral = -lateral;
                }

                blocks.add(new TemplateBlock(forward, dy, lateral, bs));
                if (blocks.size() >= maxBlocks) break;
            }
        }

        return new SelectionTemplate(forwardSize, blocks);
    }

    private static final class Vec3d {
        private final double x;
        private final double y;
        private final double z;

        private Vec3d(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static Vec3d lerp(Vec3d a, Vec3d b, double t) {
        return new Vec3d(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t
        );
    }

    private static Vec3d normalize2d(double x, double z) {
        double len = Math.sqrt(x * x + z * z);
        if (len <= 1e-9) return new Vec3d(1, 0, 0);
        return new Vec3d(x / len, 0, z / len);
    }

    private void buildCurveFromTemplate(Player player, List<Location> controlPoints, SelectionTemplate template) {
        List<Vec3d> path = sampleCatmullRom(controlPoints);
        if (path.size() < 2) {
            player.sendMessage("§c曲线采样失败：点数不足。");
            return;
        }

        double[] cum = new double[path.size()];
        cum[0] = 0;
        for (int i = 1; i < path.size(); i++) {
            Vec3d a = path.get(i - 1);
            Vec3d b = path.get(i);
            double d = Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y) + (a.z - b.z) * (a.z - b.z));
            cum[i] = cum[i - 1] + d;
        }
        double totalLen = cum[cum.length - 1];
        if (totalLen <= 1e-6) {
            player.sendMessage("§c曲线长度太短。");
            return;
        }

        Set<BlockVector3> placed = new HashSet<>();
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(player.getWorld()))) {
            for (TemplateBlock tb : template.blocks) {
                double alpha = (template.forwardSize <= 1) ? 0 : (tb.forward / (double) (template.forwardSize - 1));
                double targetDist = alpha * totalLen;

                int idx = findSegmentIndex(cum, targetDist);
                double segStart = cum[idx];
                double segEnd = cum[idx + 1];
                double localT = (segEnd - segStart) <= 1e-9 ? 0 : (targetDist - segStart) / (segEnd - segStart);

                Vec3d p = lerp(path.get(idx), path.get(idx + 1), localT);
                Vec3d d = new Vec3d(path.get(idx + 1).x - path.get(idx).x, 0, path.get(idx + 1).z - path.get(idx).z);
                Vec3d dir = normalize2d(d.x, d.z);
                Vec3d right = new Vec3d(-dir.z, 0, dir.x);

                double x = p.x + right.x * tb.lateral;
                double y = p.y + tb.up;
                double z = p.z + right.z * tb.lateral;

                BlockVector3 bv = BlockVector3.at((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
                if (placed.add(bv)) {
                    editSession.setBlock(bv, tb.blockState);
                }
            }
            editSession.flushQueue();
        }
    }

    private int findSegmentIndex(double[] cum, double targetDist) {
        int low = 0;
        int high = cum.length - 2;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (cum[mid] <= targetDist && targetDist <= cum[mid + 1]) return mid;
            if (targetDist < cum[mid]) high = mid - 1;
            else low = mid + 1;
        }
        return Math.max(0, Math.min(cum.length - 2, low));
    }

    private List<Vec3d> sampleCatmullRom(List<Location> points) {
        List<Vec3d> out = new ArrayList<>();
        if (points.size() < 2) return out;

        for (int i = 0; i < points.size() - 1; i++) {
            Location p0 = (i == 0) ? points.get(i) : points.get(i - 1);
            Location p1 = points.get(i);
            Location p2 = points.get(i + 1);
            Location p3 = (i + 2 < points.size()) ? points.get(i + 2) : points.get(i + 1);

            double segDist = p1.distance(p2);
            int samples = Math.max(32, (int) (segDist * 16));
            for (int s = 0; s <= samples; s++) {
                double t = s / (double) samples;
                double x = 0.5 * ((2 * p1.getX())
                        + (-p0.getX() + p2.getX()) * t
                        + (2 * p0.getX() - 5 * p1.getX() + 4 * p2.getX() - p3.getX()) * t * t
                        + (-p0.getX() + 3 * p1.getX() - 3 * p2.getX() + p3.getX()) * t * t * t);
                double y = 0.5 * ((2 * p1.getY())
                        + (-p0.getY() + p2.getY()) * t
                        + (2 * p0.getY() - 5 * p1.getY() + 4 * p2.getY() - p3.getY()) * t * t
                        + (-p0.getY() + 3 * p1.getY() - 3 * p2.getY() + p3.getY()) * t * t * t);
                double z = 0.5 * ((2 * p1.getZ())
                        + (-p0.getZ() + p2.getZ()) * t
                        + (2 * p0.getZ() - 5 * p1.getZ() + 4 * p2.getZ() - p3.getZ()) * t * t
                        + (-p0.getZ() + 3 * p1.getZ() - 3 * p2.getZ() + p3.getZ()) * t * t * t);

                if (out.isEmpty() || (Math.abs(out.get(out.size() - 1).x - x) + Math.abs(out.get(out.size() - 1).y - y) + Math.abs(out.get(out.size() - 1).z - z)) > 1e-6) {
                    out.add(new Vec3d(x, y, z));
                }
            }
        }
        return out;
    }
}
