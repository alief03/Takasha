package net.takasha.rpg.dungeon.arena;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Snapshot blok arena untuk rollback 100% (Zero World Griefing).
 * Merekam kondisi blok asli sebelum kubah arena dibangun dan mengembalikannya secara utuh,
 * dengan proteksi posisi peti hadiah agar tidak terhapus.
 * Mendukung serialisasi JSON untuk pemulihan darurat setelah server crash/force-close.
 */
public class ArenaBlockSnapshot {

    private final Map<BlockPos, BlockState> originalBlocks = new HashMap<>();

    public void recordAndSet(ServerLevel level, BlockPos pos, BlockState newState) {
        BlockPos immutablePos = pos.immutable();
        if (!originalBlocks.containsKey(immutablePos)) {
            originalBlocks.put(immutablePos, level.getBlockState(immutablePos));
        }
        level.setBlock(immutablePos, newState, 3);
    }

    public void restoreAllExcept(ServerLevel level, BlockPos preservedPos) {
        if (level == null) return;
        for (Map.Entry<BlockPos, BlockState> entry : originalBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (preservedPos != null && pos.equals(preservedPos)) {
                continue; // Jangan timpa peti hadiah di titik tengah
            }
            level.setBlock(pos, entry.getValue(), 3);
        }
        originalBlocks.clear();
    }

    public void restoreAll(ServerLevel level) {
        restoreAllExcept(level, null);
    }

    public void clear() {
        originalBlocks.clear();
    }

    public int getRecordedBlockCount() {
        return originalBlocks.size();
    }

    public JsonArray toJsonArray() {
        JsonArray array = new JsonArray();
        for (Map.Entry<BlockPos, BlockState> entry : originalBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            JsonObject obj = new JsonObject();
            obj.addProperty("x", pos.getX());
            obj.addProperty("y", pos.getY());
            obj.addProperty("z", pos.getZ());
            obj.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            array.add(obj);
        }
        return array;
    }

    public static ArenaBlockSnapshot fromJsonArray(JsonArray array) {
        ArenaBlockSnapshot snapshot = new ArenaBlockSnapshot();
        if (array == null) return snapshot;

        for (JsonElement el : array) {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("x") && obj.has("y") && obj.has("z") && obj.has("block")) {
                    int x = obj.get("x").getAsInt();
                    int y = obj.get("y").getAsInt();
                    int z = obj.get("z").getAsInt();
                    String blockId = obj.get("block").getAsString();

                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = null;
                    try {
                        block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId));
                    } catch (Exception ignored) {}
                    BlockState state = (block != null) ? block.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    snapshot.originalBlocks.put(pos, state);
                }
            }
        }
        return snapshot;
    }
}
