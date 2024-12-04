package com.vertmix.lib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RandomSkinPlugin extends JavaPlugin implements Listener {

   private List<String> skinTextures;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Save the default config if it doesn't exist
        FileConfiguration config = getConfig();
        skinTextures = config.getStringList("skin-textures");

        if (skinTextures.isEmpty()) {
            getLogger().warning("No skin textures found in config! Add them under 'skin-textures' in the config.yml.");
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("ProtocolLib is required to run this plugin. Disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (skinTextures.isEmpty()) return;

        Player player = event.getPlayer();
        String randomSkinTexture = skinTextures.get(new Random().nextInt(skinTextures.size()));

        // Apply the skin using ProtocolLib
        applySkin(player, randomSkinTexture);
    }

    private void applySkin(Player player, String skinTexture) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        WrappedGameProfile profileWithSkin = createProfileWithSkin(uuid, name, skinTexture);

        // Create a PacketContainer for PLAYER_INFO
        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);

        // Set the action to REMOVE_PLAYER followed by ADD_PLAYER for updating the skin
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

        // Create a PlayerInfoData list
        List<PlayerInfoData> playerInfoDataList = new ArrayList<>();
        playerInfoDataList.add(new PlayerInfoData(profileWithSkin, 0,
                EnumWrappers.NativeGameMode.SURVIVAL, null));
        packet.getPlayerInfoDataLists().write(0, playerInfoDataList);

        // Remove the player
        for (Player online : Bukkit.getOnlinePlayers()) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(online, packet);
        }

        // Add the player back with the updated profile
        packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        for (Player online : Bukkit.getOnlinePlayers()) {
            ProtocolLibrary.getProtocolManager().sendServerPacket(online, packet);
        }

        player.sendMessage("Your skin has been updated to a random texture!");

    }

    private WrappedGameProfile createProfileWithSkin(UUID uuid, String name, String skinTexture) {
        // Generate a new GameProfile with the skin texture
        WrappedGameProfile profile = new WrappedGameProfile(uuid, name);
        profile.getProperties().put("textures", new com.comphenix.protocol.wrappers.WrappedSignedProperty("textures", skinTexture, ""));
        return profile;
    }
}