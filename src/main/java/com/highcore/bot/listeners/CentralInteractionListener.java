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
import java.util.List;
import java.util.ArrayList;

public class CentralInteractionListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        // SILENT COORDINATION: deferEdit acknowledges instantly WITHOUT showing 'thinking...'
        if (!event.isAcknowledged()) {
            event.deferEdit().queue(hook -> processButton(event));
        } else {
            processButton(event);
        }
    }

    private void processButton(ButtonInteractionEvent event) {
        try {
            String id = event.getComponentId();
            Member member = event.getMember();
            if (member == null) return;
            
            if (id.equals("menu_main")) { PanelService.sendStartupHub(event); return; }
            if (id.equals("hub_highcore") || id.equals("hub_map")) { PanelService.sendServerMap(event); return; }
            if (id.equals("hub_about") || id.equals("hub_social")) { PanelService.sendAboutUs(event); return; }
            if (id.equals("hub_partners")) { PanelService.sendPartnersPanel(event); return; }
            if (id.equals("hub_pings")) { PanelService.sendPingsPanel(event); return; }
            if (id.equals("hub_services")) { PanelService.sendServicesCategory(event); return; }
            if (id.equals("hub_prices")) { PanelService.sendPricesCategory(event); return; }
            if (id.equals("hub_rules")) { 
                PanelService.replyEphemeral(event, EmbedUtil.rulesPanel());
                return; 
            }
            if (id.equals("hub_stats")) { PanelService.sendStatsPanel(event); return; }
            if (id.equals("order_initiate") || id.equals("order_start") || id.equals("hub_tickets")) { PanelService.sendTicketPanel(event); return; }
            if (id.equals("hub_support_jump")) {
                event.getHook().editOriginal("Access established. Point of entry: <#1488798547947159612>").queue();
                return;
            }


            // PING ROLE HANDLING
            if (id.startsWith("ping_")) {
                String roleId = id.replace("ping_", "");
                Role role = event.getGuild().getRoleById(roleId);
                if (role != null) {
                    if (member.getRoles().contains(role)) {
                        event.getGuild().removeRoleFromMember(member, role).queue(v -> 
                            event.getHook().editOriginal("Notification frequency disabled: **" + role.getName() + "**").queue());
                    } else {
                        event.getGuild().addRoleToMember(member, role).queue(v -> 
                            event.getHook().editOriginal("Notification frequency enabled: **" + role.getName() + "**").queue());
                    }
                }
                return;
            }

            // TICKET OPS
            if (id.equals("ticket_claim")) { TicketService.claimTicket(event.getChannel().asTextChannel(), member); event.getHook().editOriginal("Ticket claimed.").queue(); }
            else if (id.equals("ticket_close")) { TicketService.closeTicket(event.getChannel().asTextChannel(), member); event.getHook().editOriginal("Ticket closing process initiated.").queue(); }
            else if (id.equals("ticket_delete")) { event.getChannel().delete().queue(); }
        } catch (Exception e) {
            try { event.getHook().editOriginal("### \u26A0 INTERACTION FAILURE\n`" + e.getMessage() + "`").queue(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        // SILENT COORDINATION for menus too: deferEdit to keep UI clean
        if (!event.isAcknowledged()) {
            event.deferEdit().queue(hook -> processSelect(event));
        } else {
            processSelect(event);
        }
    }

    private void processSelect(StringSelectInteractionEvent event) {
        try {
            String id = event.getComponentId();
            String val = event.getValues().get(0);

            if (id.equals("view_services_cat")) { handleServiceDisplay(event, val); }
            else if (id.equals("view_prices_cat")) { handlePriceDisplay(event, val); }
            else if (id.equals("ticket_type_select")) {
                event.getHook().sendMessage("Initializing terminal session...").setEphemeral(true).queue();
                TicketService.createTicket(event.getGuild(), event.getUser(), "General Request", "MEDIUM", val).queue();
            }
        } catch (Exception e) {
            try { event.getHook().sendMessage("### \u26A0 SELECTION FAILURE\n`" + e.getMessage() + "`").setEphemeral(true).queue(); } catch (Exception ignored) {}
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
        net.dv8tion.jda.api.utils.messages.MessageEditBuilder meb = new net.dv8tion.jda.api.utils.messages.MessageEditBuilder();
        meb.setComponents(List.of(
             EmbedUtil.containerBranded("SERVICES", "Active Capabilities", body, EmbedUtil.BANNER_MAIN),
             ActionRow.of(Button.success("order_start", "Start Order"))
        ));
        meb.useComponentsV2(true);
        
        event.getHook().editOriginal(meb.build()).queue();
    }

    private void handlePriceDisplay(StringSelectInteractionEvent event, String val) {
        String body = switch (val) {
            case "price_designer" -> "**Logo:** $20+\n**Full Identity:** $50+\n**Social Pack:** $25+\n**Discord Setup:** $20+";
            case "price_developer" -> "**Web App:** $30+\n**Discord Bot:** $40+";
            case "price_editor" -> "**Video Edit:** $15+\n**Shorts/Reels:** $10+\n**Youtube Branding:** $30+";
            case "price_minecraft" -> "**Server Setup:** $50+\n**Map Design:** $40+\n**Plugin Config:** $20+";
            default -> "Pricing data unavailable.";
        };
        net.dv8tion.jda.api.utils.messages.MessageEditBuilder meb = new net.dv8tion.jda.api.utils.messages.MessageEditBuilder();
        meb.setComponents(EmbedUtil.containerBranded("ACCOUNTING", "Price Matrix", body, EmbedUtil.BANNER_MAIN));
        meb.useComponentsV2(true);
        
        event.getHook().editOriginal(meb.build()).queue();
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
