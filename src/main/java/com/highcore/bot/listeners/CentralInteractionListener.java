package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.services.InvoiceService;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.services.BroadcastService;
import com.highcore.bot.services.TicketService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
import net.dv8tion.jda.api.modals.Modal;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;

public class CentralInteractionListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(CentralInteractionListener.class);

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

        boolean isModalTrigger = id.equals("ticket_init_support") || id.equals("ticket_init_complaint")
                || id.equals("order_open_ticket") || id.equals("ar_add") || id.equals("bw_add");
        boolean isStaffAction = id.equals("ticket_claim") || id.equals("ticket_close") || id.equals("ticket_unclaim") ||
                id.equals("ticket_delete") || id.equals("ticket_reopen") || id.startsWith("order_status_update_");

        if (id.startsWith("gw_") || id.startsWith("btn_gw_") || id.startsWith("sel_gw_"))
            return;

        if (!event.isAcknowledged()) {
            if (isModalTrigger) {
                processButton(event);
            } else if (isStaffAction) {
                if (!isStaff(member)) {
                    event.reply("⛔ This action is restricted to staff members.").setEphemeral(true).queue();
                    return;
                }
                event.deferEdit().queue(v -> processButton(event));
            } else {
                event.deferReply(false).queue(v -> processButton(event));
            }
            return;
        }

        processButton(event);
    }

    private void processButton(ButtonInteractionEvent event) {
        try {
            String id = event.getComponentId();
            Member member = event.getMember();
            if (member == null)
                return;

            if (id.equals("btn_rules") || id.equals("hub_rules")) {
                PanelService.replyEphemeral(event, EmbedUtil.rulePanel());
                return;
            }
            if (id.equals("btn_startup") || id.equals("menu_main")) {
                PanelService.sendStartupHub(event);
                return;
            }
            if (id.equals("btn_highcore") || id.equals("hub_highcore")) {
                PanelService.sendHighcoreHub(event);
                return;
            }
            if (id.equals("btn_pings") || id.equals("hub_pings")) {
                PanelService.sendPingsHub(event);
                return;
            }
            if (id.equals("btn_about") || id.equals("hub_about")) {
                PanelService.sendAboutUsHub(event);
                return;
            }
            if (id.equals("btn_socials") || id.equals("hub_social")) {
                PanelService.sendSocialsHub(event);
                return;
            }
            if (id.equals("hub_partners")) {
                PanelService.sendPartnersPanel(event);
                return;
            }
            if (id.equals("hub_services")) {
                PanelService.sendServicesCategory(event, "all");
                return;
            }
            if (id.equals("view_prices_cat")) {
                PanelService.sendPricesCategory(event);
                return;
            }

            if (id.equals("ticket_init_support")) {
                PanelService.handleSupportFlow(event, "support_tech");
                return;
            }
            if (id.equals("ticket_init_order")) {
                PanelService.handleOrderFlow(event);
                return;
            }
            if (id.equals("ticket_init_complaint")) {
                PanelService.handleComplaintFlow(event);
                return;
            }

            if (id.equals("order_initiate") || id.equals("order_start") || id.equals("hub_tickets")) {
                PanelService.sendTicketPanel(event);
                return;
            }

            if (id.equals("order_open_ticket")) {
                PanelService.handleOrderFinalModal(event, "order_final");
                return;
            }

            if (id.startsWith("ping_")) {
                String roleId = id.replace("ping_", "");
                Role role = event.getGuild().getRoleById(roleId);
                if (role != null) {
                    if (member.getRoles().contains(role)) {
                        event.getGuild().removeRoleFromMember(member, role).queue(v -> event.getHook()
                                .editOriginal("Notification disabled: **" + role.getName() + "**").queue());
                    } else {
                        event.getGuild().addRoleToMember(member, role).queue(v -> event.getHook()
                                .editOriginal("Notification enabled: **" + role.getName() + "**").queue());
                    }
                }
                return;
            }

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
            }

            if (id.startsWith("pay_")) {
                String[] parts = id.split("_", 3);
                String method = parts.length > 1 ? parts[1].toUpperCase() : "UNKNOWN";
                String info = switch (method) {
                    case "PAYPAL" -> "### 💳 PayPal\n**Email:** `billing@highcore.agency`\n**Note:** Send as **Friends & Family**.";
                    case "STRIPE" -> "### 🌐 Stripe\nContact a staff member to receive a secure Stripe payment link.";
                    case "BANK" -> "### 🏦 Bank Transfer — Al-Rajhi Bank\n**Account Name:** `High Core Agency`\n**IBAN:** `SA29 8000 0000 1234 5678 1234`\n**Swift:** `RJHISARI`\nAfter transfer, send the receipt screenshot here.";
                    case "USDT" -> "### 💰 USDT — TRC20 Network\n**Wallet Address:**\n```\nTHighCoreAgencyWallet9xR3mZXq\n```\n⚠️ **Use TRC20 network only.**";
                    case "STCPAY" -> "### 📱 STC Pay\n**Number:** `+966 55 123 4567`\n**Name:** `High Core Agency`\nScreenshot the confirmation and send it here.";
                    default -> "Contact a staff member for payment assistance.";
                };
                PanelService.replyEphemeral(event, EmbedUtil.containerBranded("PAYMENT", "Gateway — " + method, info, EmbedUtil.BANNER_MAIN));
                return;
            }

            if (id.equals("ar_add")) {
                TextInput kw = TextInput.create("ar_keyword", TextInputStyle.SHORT).setPlaceholder("Trigger word...").setRequired(true).build();
                TextInput resp = TextInput.create("ar_response", TextInputStyle.PARAGRAPH).setPlaceholder("What should I reply?").setRequired(true).build();
                Modal m = Modal.create("modal_ar_add", "Create Response Protocol")
                    .addComponents(
                        net.dv8tion.jda.api.components.label.Label.of("Auto-Reply Keyword", kw),
                        net.dv8tion.jda.api.components.label.Label.of("Matrix Response", resp)
                    ).build();
                event.replyModal(m).queue();
            } else if (id.equals("ar_manage")) {
                JsonArray arr = com.highcore.bot.database.SupabaseClient.getAutoResponses();
                if (arr == null || arr.size() == 0) { event.reply("No protocols found.").setEphemeral(true).queue(); return; }
                StringSelectMenu.Builder menu = StringSelectMenu.create("ar_delete_select").setPlaceholder("Select a protocol to decommission...");
                for (int i=0; i<Math.min(arr.size(), 25); i++) {
                    String kw = arr.get(i).getAsJsonObject().get("keyword").getAsString();
                    menu.addOption(kw, kw);
                }
                event.reply("### ⚠️ DECOMMISSION SECTOR\nSelect a response protocol to permanently delete from the matrix.")
                    .setComponents(ActionRow.of(menu.build())).setEphemeral(true).queue();
            } else if (id.equals("bw_add")) {
                TextInput word = TextInput.create("bw_word", TextInputStyle.SHORT).setPlaceholder("Enter word to block...").setRequired(true).build();
                Modal m = Modal.create("modal_bw_add", "Update Security Lexicon")
                    .addComponents(net.dv8tion.jda.api.components.label.Label.of("Restricted Term", word))
                    .build();
                event.replyModal(m).queue();
            } else if (id.equals("bw_remove")) {
                JsonArray arr = com.highcore.bot.database.SupabaseClient.getWordFilter();
                if (arr == null || arr.size() == 0) { event.reply("Security Lexicon is clear.").setEphemeral(true).queue(); return; }
                StringSelectMenu.Builder menu = StringSelectMenu.create("bw_delete_select").setPlaceholder("Select a term to whitelist...");
                for (int i=0; i<Math.min(arr.size(), 25); i++) {
                    String w = arr.get(i).getAsJsonObject().get("word").getAsString();
                    menu.addOption(w, w);
                }
                event.reply("### 🛠️ SECURITY OVERRIDE\nSelect a forbidden term to remove from the firewall blacklist.")
                    .setComponents(ActionRow.of(menu.build())).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            try {
                if (event.isAcknowledged())
                    event.getHook().sendMessage("An error occurred: " + e.getMessage()).setEphemeral(true).queue();
                else
                    event.reply("An error occurred: " + e.getMessage()).setEphemeral(true).queue();
            } catch (Exception ignored) {
            }
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
                            .addComponents(net.dv8tion.jda.api.components.label.Label.of("Subject", subjectInput))
                            .build();
                    event.replyModal(m).queue();
                }
                case "complaint" -> {
                    TextInput reasonInput = TextInput.create("reason", TextInputStyle.PARAGRAPH)
                            .setPlaceholder("What are you reporting?").setRequired(true).build();
                    Modal m = Modal.create("modal_report_open", "Submit a Report")
                            .addComponents(net.dv8tion.jda.api.components.label.Label.of("Reason", reasonInput))
                            .build();
                    event.replyModal(m).queue();
                }
            }
            return;
        }

        if (!event.isAcknowledged()) {
            boolean isOrderSelect = id.equals("order_service_select") || id.equals("order_main_select")
                    || id.equals("order_addon_select");
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
            if (id.equals("order_service_select")) {
                PanelService.handleCategorySelected(event, userId, event.getValues().get(0));
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
            if (id.equals("about_category_select")) {
                PanelService.sendServicePriceInfo(event, event.getValues().get(0).replace("about_", ""));
                return;
            }
            if (id.equals("ar_delete_select")) {
                String kw = event.getValues().get(0);
                com.highcore.bot.database.SupabaseClient.deleteAutoResponse(kw);
                event.reply("✅ Response protocol `" + kw + "` has been wiped from the matrix.").setEphemeral(true).queue();
            }
            if (id.equals("bw_delete_select")) {
                String w = event.getValues().get(0);
                com.highcore.bot.database.SupabaseClient.removeForbiddenWord(w);
                event.reply("✅ Term `" + w + "` has been whitelisted and removed from firewall.").setEphemeral(true).queue();
            }
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
                    .remove("bc_" + event.getUser().getId());
            if (s != null) {
                if (BroadcastService.startBroadcast(event.getGuild(), event.getValue("message").getAsString(), s.roleId,
                        s.attUrl)) {
                    PanelService.reply(event, EmbedUtil.containerBranded("SYSTEM", "Broadcast Active",
                            "### 📡 Global Signal Locked\nThe broadcast sequence has been successfully initiated. Transmission to all designated nodes is now in progress.",
                            EmbedUtil.BANNER_MAIN));
                    LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, EmbedUtil.createOldLogEmbed("Global Broadcast", "Message: " + event.getValue("message").getAsString(), event.getMember(), null, null, EmbedUtil.GOLD));
                } else {
                    PanelService.reply(event, EmbedUtil.error("Broadcast Conflict",
                            "A broadcast sequence is already active in the mainframe."));
                }
            }
        } else if (id.equals("modal_boter")) {
            com.highcore.bot.commands.SlashCommands.BoterSession s = com.highcore.bot.commands.SlashCommands.BOTER_SESSIONS
                    .remove("boter_" + event.getUser().getId());
            if (s != null) {
                String msg = event.getValue("message").getAsString();
                var tc = event.getGuild().getTextChannelById(s.channelId);
                if (tc != null) {
                    var send = tc.sendMessage(msg);
                    if (!s.fileUrls.isEmpty()) {
                        List<net.dv8tion.jda.api.utils.FileUpload> uploads = new ArrayList<>();
                        for (String url : s.fileUrls) {
                            try {
                                java.net.URL urlObj = new java.net.URI(url).toURL();
                                String fileName = url.substring(url.lastIndexOf('/') + 1);
                                if (fileName.contains("?")) fileName = fileName.substring(0, fileName.indexOf('?'));
                                uploads.add(net.dv8tion.jda.api.utils.FileUpload.fromData(urlObj.openStream(), fileName));
                            } catch (Exception e) {
                                log.error("Error attaching file from URL: " + url, e);
                            }
                        }
                        if (!uploads.isEmpty()) send = send.addFiles(uploads);
                    }
                    send.queue(v -> {
                        LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, EmbedUtil.createOldLogEmbed("Boter Message", "Channel: " + tc.getAsMention() + "\nContent: " + msg, event.getMember(), null, null, EmbedUtil.INFO));
                        // Acknowledge silently without a visible message after deployment
                        if (!event.isAcknowledged()) {
                            event.deferReply(true).queue(hook -> hook.deleteOriginal().queue());
                        }
                    });
                }
            }
        } else if (id.equals("modal_support_init")) {
            event.deferReply(true).queue();
            String issueDesc = event.getValue("issue_desc").getAsString();
            String subject = issueDesc.length() > 80 ? issueDesc.substring(0, 77) + "..." : issueDesc;
            TicketService.createTicket(event, subject, "MEDIUM", "SUPPORT",
                    "**Service:** " + event.getValue("service_type").getAsString());
        } else if (id.equals("modal_complaint_init")) {
            event.deferReply(true).queue();
            String compType = event.getValue("comp_type").getAsString();
            String subject = compType.length() > 80 ? compType.substring(0, 77) + "..." : compType;
            TicketService.createTicket(event, subject, "HIGH", "COMPLAINT",
                    "**Regarding:** " + event.getValue("comp_person").getAsString() + "\n**Details:** "
                            + event.getValue("comp_desc").getAsString());
        } else if (id.equals("modal_order_final")) {
            event.deferReply(true).queue();
            String userId = event.getUser().getId();
            PanelService.OrderSession session = PanelService.SESSIONS.remove(userId);
            List<InvoiceService.OrderItem> items = (session != null)
                    ? PanelService.resolveItems(new ArrayList<String>() {
                        {
                            addAll(session.mainIds);
                            addAll(session.addonIds);
                        }
                    })
                    : new ArrayList<>();
            TicketService.createHighEndOrderTicket(event.getGuild(), event.getUser(),
                    event.getValue("o_project").getAsString(), event.getValue("o_name").getAsString(),
                    event.getValue("o_contact").getAsString(), event.getValue("o_eta").getAsString(), items);
            event.getHook().sendMessage("✅ Order submitted.").setEphemeral(true).queue();
        } else if (id.equals("modal_ar_add")) {
            String kw = event.getValue("ar_keyword").getAsString();
            String resp = event.getValue("ar_response").getAsString();
            com.highcore.bot.database.SupabaseClient.createAutoResponse(kw, resp, event.getUser().getName());
            event.reply("✅ **Response Protocol Initialized**\nKeyword: `" + kw + "`").setEphemeral(true).queue();
        } else if (id.equals("modal_bw_add")) {
            String w = event.getValue("bw_word").getAsString().toLowerCase();
            com.highcore.bot.database.SupabaseClient.addForbiddenWord(w);
            event.reply("✅ **Security Firewall Updated**\nForbidden Term: `" + w + "`").setEphemeral(true).queue();
        }
    }

}
