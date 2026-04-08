package com.highcore.bot.listeners;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.services.BroadcastService;
import com.highcore.bot.services.TicketService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class CentralInteractionListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        Member member = event.getMember();
        if (member == null) return;

        // HUB NAVIGATION
        if (id.equals("menu_main")) { PanelService.sendStartupHub(event); return; }
        if (id.equals("hub_map")) { PanelService.sendServerMap(event); return; }
        if (id.equals("hub_social")) { PanelService.sendSocialPanel(event); return; }
        if (id.equals("hub_colors")) { PanelService.sendColorsPanel(event); return; }
        if (id.equals("hub_pings")) { PanelService.sendPingsPanel(event); return; }
        if (id.equals("hub_services")) { PanelService.sendServicesCategory(event); return; }
        if (id.equals("hub_prices")) { PanelService.sendPricesCategory(event); return; }
        if (id.equals("hub_rules")) { 
            event.replyComponents(EmbedUtil.rulesPanel(ActionRow.of(Button.danger("menu_main", " \u21DC  RETURN TO HUB ").withEmoji(Emoji.fromUnicode("\u23EA"))))).setEphemeral(true).queue();
            return; 
        }
        if (id.equals("hub_stats")) { PanelService.sendStatsPanel(event); return; }
        if (id.equals("order_start") || id.equals("hub_tickets")) { PanelService.sendTicketPanel(event); return; }

        // COLOR ROLE HANDLING (Auto-Remove previous)
        if (id.startsWith("color_")) {
            String roleId = id.replace("color_", "");
            Role role = event.getGuild().getRoleById(roleId);
            if (role != null) {
                java.util.List<String> colorRoles = java.util.Arrays.asList(
                    "1489744978719543408", "1489744984092442704", "1489744981835911238",
                    "1489744986424479927", "1489744990962716732", "1489744988936867880"
                );
                
                for (Role r : member.getRoles()) {
                    if (colorRoles.contains(r.getId())) event.getGuild().removeRoleFromMember(member, r).queue();
                }

                event.getGuild().addRoleToMember(member, role).queue(v -> {
                    event.reply("Identity color calibrated: **" + role.getName() + "**").setEphemeral(true).queue();
                }, e -> event.reply("Failed to adjust color. Permission error.").setEphemeral(true).queue());
            } else {
                event.reply("Selected color role ID is invalid.").setEphemeral(true).queue();
            }
            return;
        }

        // PING ROLE HANDLING (Toggle)
        if (id.startsWith("ping_")) {
            String roleId = id.replace("ping_", "");
            Role role = event.getGuild().getRoleById(roleId);
            if (role != null) {
                if (member.getRoles().contains(role)) {
                    event.getGuild().removeRoleFromMember(member, role).queue(v -> 
                        event.reply("Notification frequency disabled: **" + role.getName() + "**").setEphemeral(true).queue());
                } else {
                    event.getGuild().addRoleToMember(member, role).queue(v -> 
                        event.reply("Notification frequency enabled: **" + role.getName() + "**").setEphemeral(true).queue());
                }
            }
            return;
        }

        // TICKET OPS
        if (id.equals("ticket_claim")) { TicketService.claimTicket(event.getChannel().asTextChannel(), member); event.deferEdit().queue(); }
        else if (id.equals("ticket_close")) { TicketService.closeTicket(event.getChannel().asTextChannel(), member); event.deferEdit().queue(); }
        else if (id.equals("ticket_delete")) { event.getChannel().delete().queue(); }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        String val = event.getValues().get(0);

        if (id.equals("view_services_cat")) { handleServiceDisplay(event, val); }
        else if (id.equals("view_prices_cat")) { handlePriceDisplay(event, val); }
        else if (id.equals("ticket_type_select")) {
            event.reply("Initializing terminal session...").setEphemeral(true).queue();
            TicketService.createTicket(event.getGuild(), event.getUser(), "General Request", "MEDIUM", val).queue();
        }
    }

    private void handleServiceDisplay(StringSelectInteractionEvent event, String val) {
        String body = switch (val) {
            case "cat_designer" -> "Logo, Identity, Social Media, Discord Packs, Banners, Prints, Motion, UI/UX, Info, Emoji.";
            case "cat_developer" -> "Modern Web Apps, Professional Discord Bots, Process Automation Engine.";
            case "cat_editor" -> "Professional Video Editing, Viral Shorts/Reels, Content Strategy Branding.";
            case "cat_minecraft" -> "Full Server Setup, Custom Map Architecture, Plugin Logic Design.";
            default -> "Sector data unavailable.";
        };
        event.replyComponents(EmbedUtil.containerBranded("SERVICES", "Active Capabilities", body, EmbedUtil.BANNER_MAIN), 
                ActionRow.of(Button.success("order_start", "Start Order"))).setEphemeral(true).queue();
    }

    private void handlePriceDisplay(StringSelectInteractionEvent event, String val) {
        String body = switch (val) {
            case "price_designer" -> "**Logo:** $20+\n**Full Identity:** $50+\n**Social Pack:** $25+\n**Discord Setup:** $20+";
            case "price_developer" -> "**Web App:** $30+\n**Discord Bot:** $40+";
            case "price_editor" -> "**Video Edit:** $15+\n**Shorts/Reels:** $10+\n**Youtube Branding:** $30+";
            case "price_minecraft" -> "**Server Setup:** $50+\n**Map Design:** $40+\n**Plugin Config:** $20+";
            default -> "Pricing data unavailable.";
        };
        event.replyComponents(EmbedUtil.containerBranded("ACCOUNTING", "Price Matrix", body, EmbedUtil.BANNER_MAIN)).setEphemeral(true).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        if (id.equals("modal_bc")) {
            com.highcore.bot.commands.SlashCommands.BcSession s = com.highcore.bot.commands.SlashCommands.BC_SESSIONS.get("bc_"+event.getUser().getId());
            if (s != null) {
                BroadcastService.startBroadcast(event.getGuild(), event.getValue("message").getAsString(), s.roleId, s.attUrl);
                event.reply("Broadcast transmission initiated.").setEphemeral(true).queue();
            }
        }
    }
}
