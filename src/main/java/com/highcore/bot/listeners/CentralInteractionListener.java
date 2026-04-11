package com.highcore.bot.listeners;

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
import java.util.List;

public class CentralInteractionListener extends ListenerAdapter {

    private static final String STAFF_ROLE_ID = "1488795130008961040";

    private boolean isStaff(Member member) {
        return member != null && member.getRoles().stream()
            .anyMatch(r -> r.getId().equals(STAFF_ROLE_ID));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id     = event.getComponentId();
        Member member = event.getMember();
        if (member == null) return;

        // Modal triggers — must NOT be deferred (they open a modal directly)
        boolean isModalTrigger = id.equals("ticket_init_support")   ||
                                 id.equals("ticket_init_complaint")  ||
                                 id.startsWith("order_meta_");

        // Staff-only channel actions
        boolean isStaffAction  = id.equals("ticket_claim")   || id.equals("ticket_close")  ||
                                 id.equals("ticket_delete")   || id.equals("ticket_reopen") ||
                                 id.startsWith("order_status_update_");

        // Category view — edit existing ephemeral in-place
        boolean isCategorySwitch = id.startsWith("order_cat_");

        if (!event.isAcknowledged()) {
            if (isModalTrigger) {
                // no defer
            } else if (isStaffAction) {
                if (!isStaff(member)) {
                    event.reply("\u26D4 This action is restricted to staff members.").setEphemeral(true).queue();
                    return;
                }
                event.deferEdit().queue();
            } else if (isCategorySwitch) {
                event.deferEdit().queue();
            } else {
                event.deferReply(true).queue();
            }
        }

        processButton(event);
    }

    private void processButton(ButtonInteractionEvent event) {
        try {
            String id     = event.getComponentId();
            Member member = event.getMember();
            if (member == null) return;

            // Hub navigation
            if (id.equals("menu_main"))                              { PanelService.sendStartupHub(event);       return; }
            if (id.equals("hub_highcore") || id.equals("hub_map"))  { PanelService.sendServerMap(event);        return; }
            if (id.equals("hub_about")   || id.equals("hub_social")){ PanelService.sendAboutUs(event);          return; }
            if (id.equals("hub_partners"))                           { PanelService.sendPartnersPanel(event);    return; }
            if (id.equals("hub_pings"))                              { PanelService.sendPingsPanel(event);       return; }
            if (id.equals("hub_services"))                           { PanelService.sendServicesCategory(event); return; }
            if (id.equals("view_prices_cat"))                        { PanelService.sendPricesCategory(event);   return; }
            if (id.equals("hub_rules")) { PanelService.replyEphemeral(event, EmbedUtil.rulesPanel()); return; }
            if (id.equals("hub_stats"))                              { PanelService.sendStatsPanel(event);       return; }

            // Ticket panel & flows
            if (id.equals("ticket_init_support"))   { PanelService.handleSupportFlow(event);   return; }
            if (id.equals("ticket_init_order"))     { PanelService.handleOrderFlow(event);     return; }
            if (id.equals("ticket_init_complaint")) { PanelService.handleComplaintFlow(event); return; }

            if (id.equals("order_initiate") || id.equals("order_start") || id.equals("hub_tickets")) {
                PanelService.sendTicketPanel(event);
                return;
            }

            // Order — category view (edit in-place, no new ephemeral)
            if (id.startsWith("order_cat_")) {
                String cat = id.replace("order_cat_", "");
                PanelService.handleCategoryView(event, cat);
                return;
            }

            // Order — open ticket modal for selected category
            if (id.startsWith("order_meta_")) {
                String cat = id.replace("order_meta_", "");
                PanelService.handleOrderMetaModal(event, cat);
                return;
            }

            // Ping role toggles
            if (id.startsWith("ping_")) {
                String roleId = id.replace("ping_", "");
                Role role = event.getGuild().getRoleById(roleId);
                if (role != null) {
                    if (member.getRoles().contains(role)) {
                        event.getGuild().removeRoleFromMember(member, role).queue(v ->
                            event.getHook().editOriginal("Notification disabled: **" + role.getName() + "**").queue());
                    } else {
                        event.getGuild().addRoleToMember(member, role).queue(v ->
                            event.getHook().editOriginal("Notification enabled: **" + role.getName() + "**").queue());
                    }
                }
                return;
            }

            // Ticket channel actions (staff only — already checked in onButtonInteraction)
            if (id.equals("ticket_claim")) {
                TicketService.claimTicket(event.getChannel().asTextChannel(), member);
            } else if (id.equals("ticket_close")) {
                TicketService.closeTicket(event.getChannel().asTextChannel(), member);
            } else if (id.equals("ticket_delete")) {
                event.getChannel().delete().queue();
            } else if (id.equals("ticket_reopen")) {
                TicketService.reopenTicket(event.getChannel().asTextChannel(), member);
            } else if (id.startsWith("order_status_update_")) {
                String status = id.replace("order_status_update_", "");
                TicketService.finalizeClose(event.getChannel().asTextChannel(), member, status);
            }

        } catch (Exception e) {
            try {
                if (event.isAcknowledged())
                    event.getHook().sendMessage("An error occurred: " + e.getMessage()).setEphemeral(true).queue();
                else
                    event.reply("An error occurred: " + e.getMessage()).setEphemeral(true).queue();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.isAcknowledged()) {
            String id = event.getComponentId();
            boolean ephemeral = id.equals("view_services_cat") || id.equals("view_prices_cat")
                             || id.equals("ticket_type_select");
            if (ephemeral) event.deferReply(true).queue(hook -> processSelect(event));
            else           event.deferEdit().queue(hook -> processSelect(event));
        } else {
            processSelect(event);
        }
    }

    private void processSelect(StringSelectInteractionEvent event) {
        try {
            String id  = event.getComponentId();
            String val = event.getValues().get(0);
            if (id.equals("ticket_type_select")) {
                TicketService.createTicket(event, "General Request", "MEDIUM", val);
            }
        } catch (Exception e) {
            try { event.getHook().sendMessage("An error occurred: `" + e.getMessage() + "`").setEphemeral(true).queue(); }
            catch (Exception ignored) {}
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();

        if (id.equals("modal_bc")) {
            com.highcore.bot.commands.SlashCommands.BcSession s =
                    com.highcore.bot.commands.SlashCommands.BC_SESSIONS.get("bc_" + event.getUser().getId());
            if (s != null) {
                BroadcastService.startBroadcast(event.getGuild(),
                        event.getValue("message").getAsString(), s.roleId, s.attUrl);
                event.reply("Broadcast transmission initiated.").setEphemeral(true).queue();
            }

        } else if (id.equals("modal_support_init")) {
            event.deferReply(true).queue();
            String issueDesc   = event.getValue("issue_desc").getAsString();
            String serviceType = event.getValue("service_type").getAsString();
            String subject = issueDesc.length() > 80 ? issueDesc.substring(0, 77) + "..." : issueDesc;
            String details = "**Service:** " + (serviceType.isBlank() ? "Not specified" : serviceType);
            TicketService.createTicket(event, subject, "MEDIUM", "SUPPORT", details);

        } else if (id.equals("modal_complaint_init")) {
            event.deferReply(true).queue();
            String compType   = event.getValue("comp_type").getAsString();
            String compPerson = event.getValue("comp_person").getAsString();
            String compDesc   = event.getValue("comp_desc").getAsString();
            String subject = compType.length() > 80 ? compType.substring(0, 77) + "..." : compType;
            String details = "**Regarding:** " + compPerson + "\n**Details:** " + compDesc;
            TicketService.createTicket(event, subject, "HIGH", "COMPLAINT", details);

        } else if (id.startsWith("modal_order_")) {
            event.deferReply(true).queue();
            String cat      = id.replace("modal_order_", "");
            String name     = event.getValue("o_name").getAsString();
            String services = event.getValue("o_services").getAsString();
            String addons   = event.getValue("o_addons").getAsString();
            String contact  = event.getValue("o_contact").getAsString();
            String eta      = event.getValue("o_eta").getAsString();

            String catDisplay = switch (cat) {
                case "designer"  -> "Designer";
                case "developer" -> "Developer";
                case "editor"    -> "Editor & Animation";
                case "minecraft" -> "Minecraft Developer";
                default          -> "Order";
            };
            String subject = catDisplay + " \u2014 " +
                (services.length() > 55 ? services.substring(0, 52) + "..." : services);
            String details =
                "**\uD83C\uDFA8 Category:** " + catDisplay + "\n" +
                "**\uD83D\uDCCB Services:** " + services + "\n" +
                (addons.isBlank() ? "" : "**\u2795 Add-ons:** " + addons + "\n") +
                "**\uD83D\uDCDE Contact:** " + contact + "\n" +
                "**\u23F1\uFE0F Timeline:** " + eta;
            TicketService.createTicket(event, subject, "MEDIUM", "ORDER", details);
            event.getHook().sendMessage(
                "\u2705 Order submitted — check your new ticket channel.")
                .setEphemeral(true).queue();
        }
    }
}
