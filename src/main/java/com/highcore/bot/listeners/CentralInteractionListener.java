package com.highcore.bot.listeners;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.services.BroadcastService;
import com.highcore.bot.services.TicketService;
import com.highcore.bot.services.InvoiceService;
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
        String id = event.getComponentId();
        
        boolean isModalTrigger = id.equals("ticket_init_support") || 
                                 id.equals("ticket_init_complaint") || 
                                 id.equals("order_final_meta");

        if (isModalTrigger) {
            processButton(event);
        } else {
            if (!event.isAcknowledged()) event.deferEdit().queue();
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
            if (id.equals("view_prices_cat")) { PanelService.sendPricesCategory(event); return; }
            if (id.equals("hub_rules")) { 
                PanelService.replyEphemeral(event, EmbedUtil.rulesPanel());
                return; 
            }
            if (id.equals("hub_stats")) { PanelService.sendStatsPanel(event); return; }
            
            if (id.equals("ticket_init_support")) { PanelService.handleSupportFlow(event); return; }
            if (id.equals("ticket_init_order")) { PanelService.handleOrderFlow(event); return; }
            if (id.equals("ticket_init_complaint")) { PanelService.handleComplaintFlow(event); return; }

            if (id.equals("order_initiate") || id.equals("order_start") || id.equals("hub_tickets")) { PanelService.sendTicketPanel(event); return; }
            if (id.equals("order_final_meta")) { PanelService.handleOrderMetaModal(event); return; }

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

            if (id.equals("ticket_claim")) { 
                TicketService.claimTicket(event.getChannel().asTextChannel(), member); 
            }
            else if (id.equals("ticket_close")) { 
                TicketService.closeTicket(event.getChannel().asTextChannel(), member); 
            }
            else if (id.equals("ticket_delete")) { 
                event.getChannel().delete().queue(); 
            }
            else if (id.equals("ticket_reopen")) {
                TicketService.reopenTicket(event.getChannel().asTextChannel(), member);
            }
            else if (id.startsWith("order_status_update_")) {
                String status = id.replace("order_status_update_", "");
                TicketService.finalizeClose(event.getChannel().asTextChannel(), member, status);
            }
        } catch (Exception e) {
            try { 
                if (event.isAcknowledged()) event.getHook().sendMessage("Selection failure: " + e.getMessage()).setEphemeral(true).queue();
                else event.reply("Selection failure: " + e.getMessage()).setEphemeral(true).queue();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
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
            else if (id.equals("order_sector_select")) { PanelService.handleSectorSelection(event, val); }
            else if (id.startsWith("order_service_select_")) { PanelService.handleServiceSelection(event, event.getValues()); }
            else if (id.startsWith("order_addon_select_")) { PanelService.handleAddonSelection(event, event.getValues()); }
            else if (id.equals("ticket_type_select")) {
                TicketService.createTicket(event, "General Request", "MEDIUM", val);
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
        PanelService.reply(event, EmbedUtil.containerBrandedRows("SERVICES", "Active Capabilities", body, EmbedUtil.BANNER_MAIN, ActionRow.of(Button.success("order_start", "Start Order"))));
    }

    private void handlePriceDisplay(StringSelectInteractionEvent event, String val) {
        String body = switch (val) {
            case "price_designer" -> "**Logo:** $20+\n**Full Identity:** $50+\n**Social Pack:** $25+\n**Discord Setup:** $20+";
            case "price_developer" -> "**Web App:** $30+\n**Discord Bot:** $40+";
            case "price_editor" -> "**Video Edit:** $15+\n**Shorts/Reels:** $10+\n**Youtube Branding:** $30+";
            case "price_minecraft" -> "**Server Setup:** $50+\n**Map Design:** $40+\n**Plugin Config:** $20+";
            default -> "Pricing data unavailable.";
        };
        PanelService.reply(event, EmbedUtil.containerBranded("ACCOUNTING", "Price Matrix", body, EmbedUtil.BANNER_MAIN));
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
        } else if (id.equals("modal_support_init")) {
            String issueDesc   = event.getValue("issue_desc").getAsString();
            String serviceType = event.getValue("service_type").getAsString();
            String subject  = issueDesc.length() > 80 ? issueDesc.substring(0, 77) + "..." : issueDesc;
            String details  = "**Service:** " + (serviceType.isBlank() ? "Not specified" : serviceType);
            TicketService.createTicket(event, subject, "MEDIUM", "SUPPORT", details);
        } else if (id.equals("modal_complaint_init")) {
            String compType   = event.getValue("comp_type").getAsString();
            String compPerson = event.getValue("comp_person").getAsString();
            String compDesc   = event.getValue("comp_desc").getAsString();
            String subject  = compType.length() > 80 ? compType.substring(0, 77) + "..." : compType;
            String details  = "**Regarding:** " + compPerson + "\n**Details:** " + compDesc;
            TicketService.createTicket(event, subject, "HIGH", "COMPLAINT", details);
        } else if (id.equals("modal_order_finalize")) {
            event.deferReply(true).queue();
            String pName   = event.getValue("p_name").getAsString();
            String cName   = event.getValue("p_client").getAsString();
            String contact = event.getValue("p_contact").getAsString();
            String eta     = event.getValue("p_eta").getAsString();

            com.highcore.bot.services.PanelService.OrderSession session =
                com.highcore.bot.services.PanelService.ORDER_SESSIONS.remove(event.getUser().getId());
            List<com.highcore.bot.services.InvoiceService.OrderItem> items = new ArrayList<>();
            if (session != null) {
                for (String svc : session.mainServices) {
                    com.highcore.bot.services.InvoiceService.OrderItem item =
                        com.highcore.bot.services.PanelService.getOrderItem(svc);
                    if (item != null) items.add(item);
                }
                for (String addon : session.addons) {
                    com.highcore.bot.services.InvoiceService.OrderItem item =
                        com.highcore.bot.services.PanelService.getOrderItem(addon);
                    if (item != null) items.add(item);
                }
            }
            if (items.isEmpty()) items.add(new com.highcore.bot.services.InvoiceService.OrderItem("Agency Service Package", 0.0));

            TicketService.createHighEndOrderTicket(event.getGuild(), event.getUser(), pName, cName, contact, eta, items);
            event.getHook().sendMessage("Your order has been submitted. Please check your new ticket channel.").setEphemeral(true).queue();
        }
    }
}
