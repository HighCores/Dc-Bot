package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CommandService {
    private static final Logger log = LoggerFactory.getLogger(CommandService.class);

    public static boolean execute(Member member, MessageChannel channel, String command) {
        String trigger = command.trim().toLowerCase();
        if (trigger.isEmpty()) return false;

        JsonObject menu = SupabaseClient.getMenuByTrigger(trigger);
        if (menu != null) {
            sendMenu(channel, menu);
            return true;
        }

        JsonObject cmd = SupabaseClient.getCommand(trigger);
        if (cmd != null) {
            String perm = cmd.has("permission") && !cmd.get("permission").isJsonNull() ? cmd.get("permission").getAsString() : "everyone";
            if (!hasPermission(member, perm)) return true;
            String response = cmd.get("response_text").getAsString();
            channel.sendMessage(response).queue();
            return true;
        }
        return false;
    }

    public static void executeSlash(SlashCommandInteractionEvent event) {
        String trigger = event.getName().toLowerCase();
        JsonObject menu = SupabaseClient.getMenuByTrigger(trigger);
        if (menu != null) {
            event.replyComponents(buildMenuContainer(menu)).useComponentsV2(true).queue();
            return;
        }

        JsonObject cmd = SupabaseClient.getCommand(trigger);
        if (cmd != null) {
            String perm = cmd.has("permission") && !cmd.get("permission").isJsonNull() ? cmd.get("permission").getAsString() : "everyone";
            if (!hasPermission(event.getMember(), perm)) { event.reply("Access denied.").setEphemeral(true).queue(); return; }
            String response = cmd.get("response_text").getAsString();
            event.reply(response).queue();
            return;
        }
    }

    public static void sendMenu(MessageChannel channel, JsonObject menu) {
        channel.sendMessageComponents(buildMenuContainer(menu)).useComponentsV2(true).queue();
    }

    private static Container buildMenuContainer(JsonObject menu) {
        String title = menu.get("title").getAsString();
        String desc = menu.get("description").getAsString().replace("\\n", "\n");
        String imageUrl = menu.has("image_url") && !menu.get("image_url").isJsonNull() ? menu.get("image_url").getAsString() : null;
        String menuId = menu.get("menu_id").getAsString();

        List<ActionRow> rows = new ArrayList<>();
        JsonArray buttonsArr = SupabaseClient.getButtons(menuId);
        if (buttonsArr != null && buttonsArr.size() > 0) {
            List<Button> buttons = new ArrayList<>();
            for (var el : buttonsArr) {
                JsonObject btnObj = el.getAsJsonObject();
                String label = btnObj.get("label").getAsString();
                String actionId = btnObj.get("action_id").getAsString();
                String styleStr = btnObj.has("button_style") ? btnObj.get("button_style").getAsString().toUpperCase() : "PRIMARY";
                String emoji = btnObj.has("emoji") && !btnObj.get("emoji").isJsonNull() ? btnObj.get("emoji").getAsString() : null;
                
                Button btn = switch (styleStr) {
                    case "SUCCESS" -> Button.success(actionId, label);
                    case "DANGER" -> Button.danger(actionId, label);
                    case "SECONDARY" -> Button.secondary(actionId, label);
                    case "LINK" -> Button.link(actionId, label);
                    default -> Button.primary(actionId, label);
                };

                if (emoji != null && !emoji.isEmpty()) {
                    try { btn = btn.withEmoji(Emoji.fromFormatted(emoji)); } 
                    catch (Exception ignored) {}
                }
                buttons.add(btn);
            }
            if (!buttons.isEmpty()) rows.add(ActionRow.of(buttons));
        }

        return EmbedUtil.containerBranded("OPERATIONS", title, desc, imageUrl, null, rows.toArray(new ActionRow[0]));
    }

    private static boolean hasPermission(Member member, String perm) {
        if (perm.equalsIgnoreCase("everyone")) return true;
        List<String> staff = com.highcore.bot.config.Config.getStaffRoles();
        List<String> admins = com.highcore.bot.config.Config.getAdminRoles();
        boolean isStaff = member != null && member.getRoles().stream().anyMatch(r -> staff.contains(r.getId()));
        boolean isAdmin = member != null && member.getRoles().stream().anyMatch(r -> admins.contains(r.getId()));
        if (perm.equalsIgnoreCase("admin")) return isAdmin;
        if (perm.equalsIgnoreCase("staff")) return isStaff;
        return true;
    }
}
