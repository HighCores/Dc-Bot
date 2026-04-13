package com.highcore.bot.listeners;

import com.highcore.bot.services.InvoiceService;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.services.BroadcastService;
import com.highcore.bot.services.TicketService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.util.List;

public class CentralInteractionListener extends ListenerAdapter {

    private static final String STAFF_ROLE_ID = "1488795130008961040";

    private boolean isStaff(Member member) {
        return member != null && member.getRoles().stream()
                .anyMatch(r -> r.getId().equals(STAFF_ROLE_ID));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        Member member = event.getMember();
        if (member == null)
            return;

        // Modal triggers — must NOT be deferred (they open a modal directly)
        boolean isModalTrigger = id.equals("ticket_init_support") ||
                id.equals("ticket_init_complaint") ||
                id.equals("order_open_ticket");

        // Staff-only channel actions
        boolean isStaffAction = id.equals("ticket_claim") || id.equals("ticket_close") ||
                id.equals("ticket_unclaim") ||
                id.equals("ticket_delete") || id.equals("ticket_reopen") ||
                id.startsWith("order_status_update_");

        // Exclude system/giveaway buttons from Central handling
        if (id.startsWith("gw_") || id.startsWith("btn_gw_") || id.startsWith("sel_gw_")) return;

        if (!event.isAcknowledged()) {
            if (isModalTrigger) {
                // no defer
                processButton(event);
            } else if (isStaffAction) {
                if (!isStaff(member)) {
                    event.reply("\u26D4 This action is restricted to staff members.").setEphemeral(true).queue();
                    return;
                }
                event.deferEdit().queue(v -> processButton(event));
            } else {
                event.deferReply(true).queue(v -> processButton(event));
            }
            return;
        }

        processButton(event);
    }

    private void processButton(ButtonInteractionEvent event) {
        try {
            String id     = event.getComponentId();
            Member member = event.getMember();
            if (member == null) return;

            // Hub navigation
            if (id.equals("btn_rules") || id.equals("hub_rules")) { PanelService.replyEphemeral(event, EmbedUtil.rulePanel()); return; }
            if (id.equals("btn_startup") || id.equals("menu_main")) { PanelService.sendStartupHub(event); return; }
            if (id.equals("btn_highcore") || id.equals("hub_highcore") || id.equals("hub_map")) { PanelService.sendHighcoreHub(event); return; }
            if (id.equals("btn_pings") || id.equals("hub_pings")) { PanelService.sendPingsHub(event); return; }
            if (id.equals("btn_about") || id.equals("hub_about")) { PanelService.sendAboutUsHub(event); return; }
            if (id.equals("btn_socials") || id.equals("hub_social")) { PanelService.sendSocialsHub(event); return; }
            if (id.equals("hub_partners")) { PanelService.sendPartnersPanel(event); return; }
            if (id.equals("hub_services")) { PanelService.sendServicesCategory(event); return; }
            if (id.equals("view_prices_cat")) { PanelService.sendPricesCategory(event); return; }
            if (id.equals("hub_stats")) { PanelService.sendStatsPanel(event); return; }

            // Ticket panel & flows
            if (id.equals("ticket_init_support"))   { PanelService.handleSupportFlow(event);   return; }
            if (id.equals("ticket_init_order"))     { PanelService.handleOrderFlow(event);     return; }
            if (id.equals("ticket_init_complaint")) { PanelService.handleComplaintFlow(event); return; }

            if (id.equals("order_initiate") || id.equals("order_start") || id.equals("hub_tickets")) {
                PanelService.sendTicketPanel(event);
                return;
            }

            // Order — Step 4: open final details modal
            if (id.equals("order_open_ticket")) {
                PanelService.handleOrderFinalModal(event);
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
                TicketService.claimTicket(event.getChannel().asTextChannel(), member, event);
            } else if (id.equals("ticket_unclaim")) {
                TicketService.unclaimTicket(event.getChannel().asTextChannel(), member, event);
            } else if (id.equals("ticket_close")) {
                TicketService.closeTicket(event.getChannel().asTextChannel(), member);
            } else if (id.equals("ticket_delete")) {
                event.getChannel().delete().queue();
            } else if (id.equals("ticket_reopen")) {
                TicketService.reopenTicket(event.getChannel().asTextChannel(), member);
            } else if (id.startsWith("order_status_update_")) {
                String status = id.replace("order_status_update_", "");
                TicketService.finalizeClose(event.getChannel().asTextChannel(), member, status);
            } else if (id.startsWith("ticket_mark_paid_")) {
                String tid = id.replace("ticket_mark_paid_", "");
                TicketService.markAsPaid(event.getChannel().asTextChannel(), tid, member);
            }


        if (id.startsWith("pay_")) {
            // id format: pay_<method>_<ticketId>
            String[] parts = id.split("_", 3);
            String method = parts.length > 1 ? parts[1].toUpperCase() : "UNKNOWN";
            String info = switch (method) {
                case "PAYPAL"  ->
                    "### \uD83D\uDCB3 PayPal\n" +
                    "**Email:** `billing@highcore.agency`\n" +
                    "**Note:** Send as **Friends & Family** and include your ticket ID in the note.";
                case "STRIPE"  ->
                    "### \uD83C\uDF10 Stripe\n" +
                    "Contact a staff member to receive a secure Stripe payment link.\n" +
                    "**Support:** <@&" + com.highcore.bot.config.Config.ROLE_STAFF + ">";
                case "BANK"    ->
                    "### \uD83C\uDFE6 Bank Transfer — Al-Rajhi Bank\n" +
                    "**Account Name:** `High Core Agency`\n" +
                    "**IBAN:** `SA29 8000 0000 1234 5678 1234`\n" +
                    "**Swift:** `RJHISARI`\n" +
                    "After transfer, send the receipt screenshot here.";
                case "USDT"    ->
                    "### \uD83D\uDCB0 USDT — TRC20 Network\n" +
                    "**Wallet Address:**\n```\nTHighCoreAgencyWallet9xR3mZXq\n```\n" +
                    "\u26A0\uFE0F **Use TRC20 network only.** Other networks will result in lost funds.\n" +
                    "Send the transaction hash after payment.";
                case "STCPAY"  ->
                    "### \uD83D\uDCF1 STC Pay\n" +
                    "**Number:** `+966 55 123 4567`\n" +
                    "**Name:** `High Core Agency`\n" +
                    "Screenshot the confirmation and send it here.";
                default ->
                    "Contact a staff member for payment assistance.\n<@&" + com.highcore.bot.config.Config.ROLE_STAFF + ">";
            };
            PanelService.replyEphemeral(event, EmbedUtil.containerBranded(
                "PAYMENT", "Gateway — " + method,
                info, EmbedUtil.BANNER_MAIN));
            return;
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
        String id = event.getComponentId();

        if (id.equals("ticket_type_select")) {
            String choice = event.getValues().get(0);
            switch (choice) {
                case "purchase" -> com.highcore.bot.services.OrderService.startWizard(event);
                case "tech_support" -> {
                    TextInput subjectInput = TextInput.create("subject", TextInputStyle.SHORT)
                            .setPlaceholder("Describe the technical issue...").setRequired(true).build();
                    Modal m = Modal.create("modal_ticket_open", "Support Request")
                            .addComponents(Label.of("Problem Brief", subjectInput))
                            .build();
                    event.replyModal(m).queue();
                }
                case "complaint" -> {
                    TextInput reasonInput = TextInput.create("reason", TextInputStyle.PARAGRAPH)
                            .setPlaceholder("What are you reporting?").setRequired(true).build();
                    Modal m = Modal.create("modal_report_open", "Submit a Report")
                            .addComponents(Label.of("Report Context", reasonInput))
                            .build();
                    event.replyModal(m).queue();
                }
            }
            return;
        }

        if (!event.isAcknowledged()) {
            boolean isOrderSelect = id.equals("order_service_select") ||
                    id.equals("order_main_select") ||
                    id.equals("order_addon_select");

            boolean ephemeral = id.equals("view_services_cat") || id.equals("view_prices_cat")
                    || id.equals("ticket_type_select");

            if (isOrderSelect)
                event.deferEdit().queue(v -> processSelect(event));
            else if (ephemeral)
                event.deferReply(true).queue(hook -> processSelect(event));
            else
                event.deferEdit().queue(hook -> processSelect(event));
        } else {
            processSelect(event);
        }
    }

    private void processSelect(StringSelectInteractionEvent event) {
        try {
            String id = event.getComponentId();
            String userId = event.getUser().getId();

            // ── Order Flow ────────────────────────────────────────────
            if (id.equals("order_service_select")) {
                String category = event.getValues().get(0);
                PanelService.handleCategorySelected(event, userId, category);
                return;
            }

            if (id.equals("order_main_select")) {
                PanelService.handleMainSelected(event, userId, event.getValues());
                return;
            }

            if (id.equals("order_addon_select")) {
                PanelService.handleAddonsSelected(event, userId, event.getValues());
                return;
            }

            // ── Legacy ticket type select ─────────────────────────────
            if (id.equals("about_category_select")) {
                String category = event.getValues().get(0).replace("about_", "");
                PanelService.sendServicePriceInfo(event, category);
                return;
            }

            // ── Legacy ticket type select ─────────────────────────────

        } catch (Exception e) {
            try {
                event.getHook().sendMessage("An error occurred: `" + e.getMessage() + "`").setEphemeral(true).queue();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();

        if (id.equals("modal_bc")) {
            com.highcore.bot.commands.SlashCommands.BcSession s = com.highcore.bot.commands.SlashCommands.BC_SESSIONS
                    .get("bc_" + event.getUser().getId());
            if (s != null) {
                BroadcastService.startBroadcast(event.getGuild(),
                        event.getValue("message").getAsString(), s.roleId, s.attUrl);
                event.reply("Broadcast transmission initiated.").setEphemeral(true).queue();
            }

        } else if (id.equals("modal_support_init")) {
            event.deferReply(true).queue();
            String issueDesc = event.getValue("issue_desc").getAsString();
            String serviceType = event.getValue("service_type").getAsString();
            String subject = issueDesc.length() > 80 ? issueDesc.substring(0, 77) + "..." : issueDesc;
            String details = "**Service:** " + (serviceType.isBlank() ? "Not specified" : serviceType);
            TicketService.createTicket(event, subject, "MEDIUM", "SUPPORT", details);

        } else if (id.equals("modal_complaint_init")) {
            event.deferReply(true).queue();
            String compType = event.getValue("comp_type").getAsString();
            String compPerson = event.getValue("comp_person").getAsString();
            String compDesc = event.getValue("comp_desc").getAsString();
            String subject = compType.length() > 80 ? compType.substring(0, 77) + "..." : compType;
            String details = "**Regarding:** " + compPerson + "\n**Details:** " + compDesc;
            TicketService.createTicket(event, subject, "HIGH", "COMPLAINT", details);

        } else if (id.equals("modal_order_final")) {
            // ── Order Flow — Step 5: create locked ticket + invoice ───
            event.deferReply(true).queue();

            String userId = event.getUser().getId();
            String pName = event.getValue("o_project").getAsString();
            String cName = event.getValue("o_name").getAsString();
            String contact = event.getValue("o_contact").getAsString();
            String eta = event.getValue("o_eta").getAsString();

            PanelService.OrderSession session = PanelService.SESSIONS.remove(userId);

            List<InvoiceService.OrderItem> items;
            if (session != null) {
                List<String> allIds = new java.util.ArrayList<>(session.mainIds);
                allIds.addAll(session.addonIds);
                items = PanelService.resolveItems(allIds);
            } else {
                items = new java.util.ArrayList<>();
            }

            Guild guild = event.getGuild();
            User user = event.getUser();
            if (guild != null) {
                TicketService.createHighEndOrderTicket(guild, user, pName, cName, contact, eta, items);
            }

            event.getHook()
                    .sendMessage("\u2705 Order submitted — your ticket channel has been created.")
                    .setEphemeral(true).queue();
        }

        // ── Order — final details modal ───────────────────────────────────────
        if (id.equals("order_modal")) {
            String pName = event.getValue("o_project").getAsString();
            String cName = event.getValue("o_name").getAsString();
            String contact = event.getValue("o_contact").getAsString();
            String eta = event.getValue("o_eta").getAsString();

            String userId = event.getUser().getId();
            com.highcore.bot.services.OrderService.OrderSession session = com.highcore.bot.services.OrderService.sessions
                    .remove(userId);

            List<InvoiceService.OrderItem> items = new java.util.ArrayList<>();
            if (session != null) {
                List<String> allIds = new java.util.ArrayList<>(session.selectedServices);
                allIds.addAll(session.selectedAddons);
                items = com.highcore.bot.services.OrderService.resolveItems(allIds);
            }

            net.dv8tion.jda.api.entities.Guild guild = event.getGuild();
            if (guild != null) {
                TicketService.createHighEndOrderTicket(guild, event.getUser(), pName, cName, contact, eta, items);
            }

            PanelService.reply(event, EmbedUtil.success("ORDER SUBMITTED",
                    "Your ticket has been created. It will be unlocked once payment is confirmed."));
        }
    }
}
