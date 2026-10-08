package net.sakura.weapons.entity.npc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.sakura.weapons.entity.TakashaNpcEntity;

public enum NpcPushPermission {
    OWNER_ONLY("gui.takasha.permission.owner", 0),
    TEAM_WHITELIST("gui.takasha.permission.team", 1),
    EVERYONE("gui.takasha.permission.everyone", 2),
    OP_ONLY("gui.takasha.permission.op", 3);

    private final String translationKey;
    private final int id;

    NpcPushPermission(String translationKey, int id) {
        this.translationKey = translationKey;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    public static NpcPushPermission fromId(int id) {
        for (NpcPushPermission perm : values()) {
            if (perm.id == id) {
                return perm;
            }
        }
        return OWNER_ONLY;
    }

    public boolean canPush(Player player, TakashaNpcEntity npc) {
        if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        // Unclaimed NPC (tidak memiliki owner UUID) dapat didorong oleh siapa saja
        if (npc.getOwnerUUID().isEmpty()) {
            return true;
        }
        return switch (this) {
            case OWNER_ONLY -> npc.isOwner(player);
            case TEAM_WHITELIST -> npc.isOwner(player) || npc.isWhitelisted(player);
            case EVERYONE -> true;
            case OP_ONLY -> player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        };
    }
}
