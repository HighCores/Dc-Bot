package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.LayoutComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
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

    public static void executeSlash(SlashCommandInteractionEvent event) {
        String trigger = event.getName().toLowerCase();
        
        JsonArray allCmds = SupabaseClient.getAllCommands();
        if (allCmds != null) {
            for (com.google.gson.JsonElement el : allCmds) {
                JsonObject obj = el.getAsJsonObject();
                String dbName = obj.get("name").getAsString().toLowerCase().replaceAll("[^a-z0-9_-]", "");
                if (dbName.equals(trigger)) {
                    String perm = obj.has("permission") && !obj.get("permission").isJsonNull() ? obj.get("permission").getAsString() : "everyone";
                    if (!hasPermission(event.getMember(), perm)) {
                        event.reply("\u274C **Unauthorized:** This node requires `" + perm.toUpperCase() + "` authority.").setEphemeral(true).queue();
                        return;
                    }
                    String response = obj.has("response_text") && !obj.get("response_text").isJsonNull() ? obj.get("response_text").getAsString() : "";
                    String actionType = obj.has("action_type") && !obj.get("action_type").isJsonNull() ? obj.get("action_type").getAsString() : "text";
                    String actionValue = obj.has("action_value") && !obj.get("action_value").isJsonNull() ? obj.get("action_value").getAsString() : "";

                    switch (actionType.toLowerCase()) {
                        case "panel" -> {
                            if (!actionValue.isEmpty()) {
                                JsonObject menuData = SupabaseClient.getMenu(actionValue);
                                if (menuData != null) sendMenuSlash(event, menuData);
                                else event.reply("\u26A0\uFE0F **Neural error:** Targeted panel `" + actionValue + "` not found.").setEphemeral(true).queue();
                            } else event.reply("\u26A0\uFE0F **Neural error:** No panel ID linked to this node.").setEphemeral(true).queue();
                        }
                        case "ticket" -> {
                            JsonObject ticketMenu = SupabaseClient.getMenu("ticket_panel");
                            if (ticketMenu != null) sendMenuSlash(event, ticketMenu);
                            else event.reply("\u26A0\uFE0F **Neural error:** Ticket panel not found.").setEphemeral(true).queue();
                        }
                        case "colors" -> com.highcore.bot.commands.GeneralCommands.displayColors(event);
                        default -> {
                            if (!response.isEmpty()) event.reply(response).queue();
                            else event.reply("\u26A0\uFE0F **Neural error:** No payload found for this node.").setEphemeral(true).queue();
                        }
                    }
                    return;
                }
            }
        }

        JsonArray allMenus = SupabaseClient.getAllMenus();
        if (allMenus != null) {
            for (com.google.gson.JsonElement el : allMenus) {
                JsonObject obj = el.getAsJsonObject();
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

    private static void sendMenuSlash(SlashCommandInteractionEvent event, JsonObject menu) {
        Container container = buildMenuContainer(menu);
        event.reply(container).queue();
    }

    private static void sendMenu(MessageChannel channel, JsonObject menu) {
        Container container = buildMenuContainer(menu);
        channel.sendMessage(container).queue();
    }

    private static Container buildMenuContainer(JsonObject menu) {
        String title = menu.get("title").getAsString();
        String desc = menu.get("description").getAsString().replace("\\n", "\n");
        String imageUrl = menu.has("image_url") && !menu.get("image_url").isJsonNull() ? menu.get("image_url").getAsString() : null;
        String colorHex = menu.has("color_hex") && !menu.get("color_hex").isJsonNull() ? menu.get("color_hex").getAsString() : "BRAND";
        String menuId = menu.get("menu_id").getAsString();

        List<ContainerChildComponent> layout = new ArrayList<>();
        
        // 🎞️ Banner Image (if present)
        if (imageUrl != null && !imageUrl.isEmpty()) {
            layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        }

        // 🏷️ Header
        layout.add(EmbedUtil.v2Header("DATABASE", title));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));

        // 📝 Description
        layout.add(TextDisplay.of(desc));

        // 🔘 Buttons
        JsonArray buttonsArr = SupabaseClient.getButtons(menuId);
        if (buttonsArr != null && buttonsArr.size() > 0) {
            List<Button> buttons = new ArrayList<>();
            for (var el : buttonsArr) {
                JsonObject btnObj = el.getAsJsonObject();
                String label = btnObj.get("label").getAsString();
                String actionId = btnObj.get("action_id").getAsString();
                String styleStr = btnObj.has("button_style") ? btnObj.get("button_style").getAsString() : "PRIMARY";
                ButtonStyle style = ButtonStyle.valueOf(styleStr.toUpperCase());
                String emoji = btnObj.has("emoji") && !btnObj.get("emoji").isJsonNull() ? btnObj.get("emoji").getAsString() : null;
                
                Button btn = style == ButtonStyle.LINK ? Button.link(actionId, label) : Button.of(style, actionId, label);
                if (emoji != null && !emoji.isEmpty()) {
                    try { btn = btn.withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromFormatted(emoji)); } 
                    catch (Exception ignored) {}
                }
                buttons.add(btn);
            }
            layout.add(ActionRow.of(buttons));
        }

        // 📜 Footer
        layout.add(EmbedUtil.v2Footer());

        return Container.of(layout).withAccentColor(EmbedUtil.parseColor(colorHex).getRGB() & 0xFFFFFF);
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
}
