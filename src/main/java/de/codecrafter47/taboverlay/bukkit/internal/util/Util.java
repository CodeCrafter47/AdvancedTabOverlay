package de.codecrafter47.taboverlay.bukkit.internal.util;

import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.utility.MinecraftFields;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import io.netty.channel.Channel;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.bukkit.entity.Player;

@UtilityClass
public class Util {

    @SneakyThrows
    public Channel getChannel(Player player) {
        val manager = MinecraftFields.getNetworkManager(player);
        return ((Channel) FuzzyReflection.fromClass(MinecraftReflection.getNetworkManagerClass()).getFieldByType("channel", Channel.class).get(manager));
    }

    public PlayerInfoData getPlayerInfoData(Player player) {
        return new PlayerInfoData(
                WrappedGameProfile.fromPlayer(player),
                getLatency(player),
                EnumWrappers.NativeGameMode.fromBukkit(player.getGameMode()),
                player.getPlayerListName() != null ? WrappedChatComponent.fromText(player.getPlayerListName()) : null);
    }

    @SneakyThrows
    public int getLatency(Player player) {
        val nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
        return nmsPlayer.getClass().getField("ping").getInt(nmsPlayer);
    }
}
