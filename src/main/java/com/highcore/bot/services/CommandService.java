package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CommandService {
    private static final Logger log = LoggerFactory.getLogger(CommandService.class);

    public static boolean execute(Member member, MessageChannel channel, String command) {
        String trigger = command.trim().toLowerCase();
        if (trigger.isEmpty()) return false;

        // 1. Try Menu/Panel Trigger
        JsonObject menu = SupabaseClient.getMenuByTrigger(trigger);
        if (menu != null) {
            sendMenu(channel, menu);
            return true;
        }

        // 2. Try Standard Command Trigger
        JsonObject cmd = SupabaseClient.getCommand(trigger);
        if (cmd != null) {
            String perm = cmd.has("permission") && !cmd.get("permission").isJsonNull() ? cmd.get("permission").getAsString() : "everyone";
            if (!hasPermission(member, perm)) {
                channel.sendMessage("\u274C **Unauthorized:** This node requires `" + perm.toUpperCase() + "` authority.").queue();
                return true;
            }

            String response = cmd.get("response_text").getAsString();
            channel.sendMessage(response).queue();
            return true;
        }

        return false;
    }

    public static void executeSlash(net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event) {
        String trigger = event.getName().toLowerCase();
        
        // 1. Check Standard Commands with Intelligent Matching
        com.google.gson.JsonArray allCmds = SupabaseClient.getAllCommands();
        if (allCmds != null) {
            for (com.google.gson.JsonElement el : allCmds) {
                com.google.gson.JsonObject obj = el.getAsJsonObject();
                String dbName = obj.get("name").getAsString().toLowerCase().replaceAll("[^a-z0-9_-]", "");
                if (dbName.equals(trigger)) {
                    String perm = obj.has("permission") && !obj.get("permission").isJsonNull() ? obj.get("permission").getAsString() : "everyone";
                    if (!hasPermission(event.getMember(), perm)) {
                        event.reply("\u274C **Unauthorized:** This node requires `" + perm.toUpperCase() + "` authority.").setEphemeral(true).queue();
                        return;
                    }
                    String response = obj.get("response_text").getAsString();
                    event.reply(response).queue();
                    return;
                }
            }
        }

        // 2. Check Panels/Menus with Intelligent Matching
        com.google.gson.JsonArray allMenus = SupabaseClient.getAllMenus();
        if (allMenus != null) {
            for (com.google.gson.JsonElement el : allMenus) {
                com.google.gson.JsonObject obj = el.getAsJsonObject();
                if (obj.has("trigger_command") && !obj.get("trigger_command").isJsonNull()) {
                    String dbTrigger = obj.get("trigger_command").getAsString().toLowerCase().replaceAll("[^a-z0-9_-]", "");
                    if (dbTrigger.equals(trigger)) {
                        sendMenuSlash(event, obj);
                        return;
                    }
                }
            }
        }

        event.reply("\u26A0\uFE0F **Neural link lost:** Command not found in registry.").setEphemeral(true).queue();
    }

    private static void sendMenuSlash(net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event, JsonObject menu) {
        String title = menu.get("title").getAsString();
        String desc = menu.get("description").getAsString().replace("\\n", "\n");
        String imageUrl = menu.has("image_url") && !menu.get("image_url").isJsonNull() ? menu.get("image_url").getAsString() : null;
        String colorHex = menu.has("color_hex") && !menu.get("color_hex").isJsonNull() ? menu.get("color_hex").getAsString() : "BRAND";
        String menuId = menu.get("menu_id").getAsString();

        EmbedBuilder eb = EmbedUtil.branded()
                .setColor(EmbedUtil.parseColor(colorHex))
                .setTitle(title)
                .setDescription(desc);

        if (imageUrl != null && !imageUrl.isEmpty()) eb.setImage(imageUrl);

        JsonArray buttonsArr = SupabaseClient.getButtons(menuId);
        if (buttonsArr != null && buttonsArr.size() > 0) {
            List<Button> buttons = new ArrayList<>();
            for (var el : buttonsArr) {
                JsonObject btnObj = el.getAsJsonObject();
                String label = btnObj.get("label").getAsString();
                String actionId = btnObj.get("action_id").getAsString();
                String styleStr = btnObj.has("button_style") ? btnObj.get("button_style").getAsString() : "PRIMARY";
                ButtonStyle style = switch (styleStr.toUpperCase()) {
                    case "SUCCESS" -> ButtonStyle.SUCCESS;
                    case "DANGER" -> ButtonStyle.DANGER;
                    case "SECONDARY" -> ButtonStyle.SECONDARY;
                    case "LINK" -> ButtonStyle.LINK;
                    default -> ButtonStyle.PRIMARY;
                };
                if (style == ButtonStyle.LINK) buttons.add(Button.link(actionId, label));
                else buttons.add(Button.of(style, actionId, label));
            }
            event.replyEmbeds(eb.build()).addComponents(ActionRow.of(buttons)).queue();
        } else {
            event.replyEmbeds(eb.build()).queue();
        }
    }

    private static boolean hasPermission(Member member, String perm) {
        if (perm.equalsIgnoreCase("everyone")) return true;
        List<String> staff = com.highcore.bot.config.Config.getStaffRoles();
        List<String> admins = com.highcore.bot.config.Config.getAdminRoles();
        
        boolean isStaff = member.getRoles().stream().anyMatch(r -> staff.contains(r.getId()));
        boolean isAdmin = member.getRoles().stream().anyMatch(r -> admins.contains(r.getId()));

        if (perm.equalsIgnoreCase("admin")) return isAdmin;
        if (perm.equalsIgnoreCase("staff")) return isStaff;
        return true;
    }

    private static void sendMenu(MessageChannel channel, JsonObject menu) {
        String title = menu.get("title").getAsString();
        String desc = menu.get("description").getAsString().replace("\\n", "\n");
        String imageUrl = menu.has("image_url") && !menu.get("image_url").isJsonNull() ? menu.get("image_url").getAsString() : null;
        String colorHex = menu.has("color_hex") && !menu.get("color_hex").isJsonNull() ? menu.get("color_hex").getAsString() : "BRAND";
        String menuId = menu.get("menu_id").getAsString();

        EmbedBuilder eb = EmbedUtil.branded()
                .setColor(EmbedUtil.parseColor(colorHex))
                .setTitle(title)
                .setDescription(desc);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            eb.setImage(imageUrl);
        }

        // Fetch Buttons
        JsonArray buttonsArr = SupabaseClient.getButtons(menuId);
        if (buttonsArr != null && buttonsArr.size() > 0) {
            List<Button> buttons = new ArrayList<>();
            for (var el : buttonsArr) {
                JsonObject btnObj = el.getAsJsonObject();
                String label = btnObj.get("label").getAsString();
                String actionId = btnObj.get("action_id").getAsString();
                String styleStr = btnObj.has("button_style") ? btnObj.get("button_style").getAsString() : "PRIMARY";
                
                ButtonStyle style = switch (styleStr.toUpperCase()) {
                    case "SUCCESS" -> ButtonStyle.SUCCESS;
                    case "DANGER" -> ButtonStyle.DANGER;
                    case "SECONDARY" -> ButtonStyle.SECONDARY;
                    case "LINK" -> ButtonStyle.LINK;
                    default -> ButtonStyle.PRIMARY;
                };

                if (style == ButtonStyle.LINK) {
                    buttons.add(Button.link(actionId, label));
                } else {
                    buttons.add(Button.of(style, actionId, label));
                }
            }
            channel.sendMessageEmbeds(eb.build()).addComponents(ActionRow.of(buttons)).queue();
        } else {
            channel.sendMessageEmbeds(eb.build()).queue();
        }
    }
}
