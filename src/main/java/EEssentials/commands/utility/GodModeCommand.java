package EEssentials.commands.utility;

import EEssentials.lang.LangManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Provides command to toggle god mode for a player.
 */
public class GodModeCommand {

    // Permission node for the god mode command.
    public static final String GOD_SELF_PERMISSION_NODE = "eessentials.godmode.self";
    public static final String GOD_OTHER_PERMISSION_NODE = "eessentials.godmode.other";
    private static final Set<UUID> godModePlayers = new HashSet<>();

    /**
     * Registers the god mode command.
     *
     * @param dispatcher The command dispatcher to register commands on.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("god")
                        .requires(Permissions.require(GOD_SELF_PERMISSION_NODE, 2))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            boolean enabled = toggleGod(null, player);
                            if (enabled) LangManager.send(context.getSource(), "God-Mode-Enabled");
                            else LangManager.send(context.getSource(), "God-Mode-Disabled");
                            return Command.SINGLE_SUCCESS;
                        }).then(argument("target", EntityArgumentType.players())
                                .requires(Permissions.require(GOD_OTHER_PERMISSION_NODE, 4))
                                .executes(context -> {
                                    Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "target");
                                    players.forEach(player -> {
                                        boolean enabled = toggleGod(null, player);
                                        if (enabled) LangManager.send(player, "God-Mode-Enabled");
                                        else LangManager.send(player, "God-Mode-Disabled");
                                    });
                                    if (players.size() == 1) {
                                        ServerPlayerEntity first = players.iterator().next();
                                        if (first.getAbilities().invulnerable) LangManager.send(context.getSource(), "God-Mode-Other-Enabled", Map.of("{player}", first.getName().getString()));
                                        else LangManager.send(context.getSource(), "God-Mode-Other-Disabled", Map.of("{player}", first.getName().getString()));
                                    } else LangManager.send(context.getSource(), "God-Mode-All", Map.of("{amount}", String.valueOf(players.size())));

                                    return Command.SINGLE_SUCCESS;
                                }).then(argument("state", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean state = BoolArgumentType.getBool(context, "state");
                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

                                            boolean enabled = toggleGod(state, player);
                                            if (enabled) LangManager.send(context.getSource(), "God-Mode-Enabled");
                                            else LangManager.send(context.getSource(), "God-Mode-Disabled");

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        ));
    }

    /**
     * Toggles God mode for the target player.
     *
     * @param targets the target players.
     * @return true if god mode is enabled after toggling, false otherwise.
     */
    // ToDo: Move toggles to player data (this is for Seam, sorry EEssentials)
    private static boolean toggleGod(Boolean toggleState, ServerPlayerEntity... targets) {
        for (ServerPlayerEntity target : targets) {
            boolean currentState = target.getAbilities().invulnerable;
            boolean updatedState = (toggleState == null) ? !currentState : toggleState;

            target.getAbilities().invulnerable = updatedState;
            target.sendAbilitiesUpdate();

            return updatedState;
        }
        return false;
    }
}
