package com.highcore.bot.listeners;

import com.highcore.bot.services.*;
import com.highcore.bot.config.Config;
import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.commands.AutoReplyCommands;
import com.highcore.bot.commands.BannedWordCommands;
import com.google.gson.JsonArray;
import net.dv8tion.jda.api.entities.Member;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

public class CentralInteractionListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(CentralInteractionListener.class);
    private static final String STAFF_ROLE_ID = "1488795130008961040";

    private boolean isStaff(net.dv8tion.jda.api.entities.Member member) {
        return member != null && (member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR) || member.isOwner() || member.getRoles().stream()
                .anyMatch(r -> r.getId().equals(STAFF_ROLE_ID)));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        Member member = event.getMember();
        if (member == null) return;

        boolean isStaffAction = id.equals("ticket_claim") || id.equals("ticket_close") || id.equals("ticket_unclaim") ||
                id.equals("invoice_gen") || id.equals("order_manage") || id.equals("giveaway_manage") ||
                id.equals("ticket_reopen") || id.equals("ticket_delete") || id.startsWith("ticket_mark_paid_") ||
                id.startsWith("order_status_update_");

        if (isStaffAction && !isStaff(member)) {
            PanelService.replyEphemeral(event, "### ⛔ Access Denied\nThis control interface is reserved for staff members.");
            return;
        }

        try {
            if (id.equals("ticket_init_support")) {
                TextInput serviceType = TextInput.create("service_type", TextInputStyle.SHORT).setPlaceholder("Which service/order?").setRequired(true).build();
                TextInput issueDesc = TextInput.create("issue_desc", TextInputStyle.PARAGRAPH).setPlaceholder("Details of your request...").setRequired(true).build();
                Modal m = Modal.create("modal_support_init", "Technical Support")
                        .addComponents(
                            net.dv8tion.jda.api.components.label.Label.of("Service Name", serviceType),
                            net.dv8tion.jda.api.components.label.Label.of("Issue Details", issueDesc))
                        .build();
                event.replyModal(m).queue();
            } else if (id.equals("ticket_init_complaint")) {
                TextInput person = TextInput.create("comp_person", TextInputStyle.SHORT).setPlaceholder("Target persona").setRequired(false).build();
                TextInput type = TextInput.create("comp_type", TextInputStyle.SHORT).setPlaceholder("Subject").setRequired(true).build();
                TextInput desc = TextInput.create("comp_desc", TextInputStyle.PARAGRAPH).setPlaceholder("Details").setRequired(true).build();
                Modal m = Modal.create("modal_complaint_init", "File Complaint")
                        .addComponents(
                            net.dv8tion.jda.api.components.label.Label.of("Person", person),
                            net.dv8tion.jda.api.components.label.Label.of("Subject", type),
                            net.dv8tion.jda.api.components.label.Label.of("Details", desc))
                        .build();
                event.replyModal(m).queue();
            } else if (id.equals("ticket_claim")) {
                TicketService.claimTicket(event.getChannel().asTextChannel(), member, event);
            } else if (id.equals("ticket_close")) {
                TicketService.closeTicket(event.getChannel().asTextChannel(), member);
            } else if (id.equals("ticket_unclaim")) {
                TicketService.unclaimTicket(event.getChannel().asTextChannel(), member, event);
            } else if (id.equals("order_final")) {
                PanelService.handleOrderFinalModal(event, id);
            } else if (id.equals("ticket_init_order")) {
                PanelService.handleOrderFlow(event);
            } else if (id.equals("btn_highcore")) {
                PanelService.sendHighcoreHub(event);
            } else if (id.equals("btn_about")) {
                PanelService.sendAboutUsHub(event);
            } else if (id.equals("btn_partners")) {
                PanelService.sendPartnersPanel(event);
            } else if (id.equals("btn_pings")) {
                PanelService.sendPingsHub(event);
            } else if (id.equals("btn_socials")) {
                PanelService.sendSocialsHub(event);
            } else if (id.startsWith("ping_")) {
                String roleId = id.replace("ping_", "");
                net.dv8tion.jda.api.entities.Role role = event.getGuild().getRoleById(roleId);
                if (role != null) {
                    if (member.getRoles().contains(role)) {
                        event.getGuild().removeRoleFromMember(member, role).queue();
                        PanelService.replyEphemeral(event, "### 🔔 Alerts Updated\nYou will no longer receive notifications for **" + role.getName() + "**.");
                    } else {
                        event.getGuild().addRoleToMember(member, role).queue();
                        PanelService.replyEphemeral(event, "### 🔔 Alerts Updated\nYou are now subscribed to **" + role.getName() + "** notifications.");
                    }
                }
            } else if (id.equals("order_open_ticket")) {
                StringSelectMenu menu = StringSelectMenu.create("ticket_type_select")
                        .setPlaceholder("Ticket Purpose Selection")
                        .addOption("Purchase / Order", "purchase")
                        .addOption("Technical Support", "tech_support")
                        .addOption("Report / Complaint", "complaint")
                        .build();
                PanelService.replyEphemeral(event, "### 🎟️ Communication Hub\nPlease designate the nature of your request.", ActionRow.of(menu));
            } else if (id.equals("ar_add")) {
                TextInput kw = TextInput.create("ar_keyword", TextInputStyle.SHORT).setPlaceholder("Trigger phrase...").setRequired(true).build();
                TextInput resp = TextInput.create("ar_response", TextInputStyle.PARAGRAPH).setPlaceholder("System reply...").setRequired(true).build();
                Modal m = Modal.create("modal_ar_add", "New Auto-Reply")
                        .addComponents(
                            net.dv8tion.jda.api.components.label.Label.of("Keyword", kw),
                            net.dv8tion.jda.api.components.label.Label.of("Response", resp))
                        .build();
                event.replyModal(m).queue();
            } else if (id.equals("ar_edit")) {
                JsonArray arr = com.highcore.bot.database.SupabaseClient.getAutoResponses();
                if (arr == null || arr.isEmpty()) { PanelService.replyEphemeral(event, "### ❌ Error\nDatabase empty."); return; }
                StringSelectMenu.Builder menu = StringSelectMenu.create("ar_edit_select").setPlaceholder("Keyword to edit...");
                arr.forEach(el -> { String k = el.getAsJsonObject().get("keyword").getAsString(); menu.addOption(k, k); });
                PanelService.replyEphemeral(event, "### 📝 Modify Response\nSelect target keyword:", ActionRow.of(menu.build()));
            } else if (id.equals("ar_manage")) {
                JsonArray arr = com.highcore.bot.database.SupabaseClient.getAutoResponses();
                if (arr == null || arr.isEmpty()) { PanelService.replyEphemeral(event, "### ❌ Error\nDatabase empty."); return; }
                StringSelectMenu.Builder menu = StringSelectMenu.create("ar_del_select").setPlaceholder("Keyword to delete...");
                arr.forEach(el -> { String k = el.getAsJsonObject().get("keyword").getAsString(); menu.addOption(k, k); });
                PanelService.replyEphemeral(event, "### 🗑️ Purge Response\nSelect keyword to delete:", ActionRow.of(menu.build()));
            } else if (id.equals("bw_add")) {
                TextInput word = TextInput.create("bw_word", TextInputStyle.SHORT).setPlaceholder("Enter forbidden word...").setRequired(true).build();
                Modal m = Modal.create("modal_bw_add", "Security Lexicon")
                        .addComponents(net.dv8tion.jda.api.components.label.Label.of("Blacklist Term", word))
                        .build();
                event.replyModal(m).queue();
            } else if (id.equals("bw_remove")) {
                JsonArray words = com.highcore.bot.database.SupabaseClient.getWordFilter();
                if (words == null || words.isEmpty()) { PanelService.replyEphemeral(event, "### ❌ Error\nLexicon empty."); return; }
                StringSelectMenu.Builder menu = StringSelectMenu.create("bw_del_select").setPlaceholder("Term to remove...");
                words.forEach(el -> { String w = el.getAsJsonObject().get("word").getAsString(); menu.addOption(w, w); });
                PanelService.replyEphemeral(event, "### 🗑️ Firewall Update\nSelect term to purge:", ActionRow.of(menu.build()));
            } else if (id.equals("ticket_reopen")) {
                TicketService.reopenTicket(event.getChannel().asTextChannel(), member);
            } else if (id.equals("ticket_delete")) {
                event.getChannel().delete().queue();
            } else if (id.startsWith("ticket_mark_paid_")) {
                String tid = id.replace("ticket_mark_paid_", "");
                TicketService.markAsPaid(event.getChannel().asTextChannel(), tid, member);
            } else if (id.equals("wiz_finish")) {
                com.highcore.bot.services.OrderService.finishWizard(event);
            } else if (id.startsWith("order_status_update_")) {
                String status = id.replace("order_status_update_", "");
                TicketService.finalizeClose(event.getChannel().asTextChannel(), member, status);
            }
        } catch (Exception e) {
            log.error("Button error", e);
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        
        if (id.equals("ticket_type_select")) {
            String choice = event.getValues().get(0);
            if (choice.equals("purchase")) {
                com.highcore.bot.services.OrderService.startWizard(event);
            } else if (choice.equals("tech_support")) {
                TextInput input = TextInput.create("subject", TextInputStyle.SHORT).setPlaceholder("Topic").setRequired(true).build();
                Modal m = Modal.create("modal_ticket_open", "Support Request")
                        .addComponents(net.dv8tion.jda.api.components.label.Label.of("Topic", input))
                        .build();
                event.replyModal(m).queue();
            } else if (choice.equals("complaint")) {
                TextInput input = TextInput.create("reason", TextInputStyle.PARAGRAPH).setPlaceholder("Detailed reason...").setRequired(true).build();
                Modal m = Modal.create("modal_report_open", "Submit Report")
                        .addComponents(net.dv8tion.jda.api.components.label.Label.of("Reason for Complaint", input))
                        .build();
                event.replyModal(m).queue();
            }
        } else if (id.equals("about_category_select")) {
            PanelService.sendServicePriceInfo(event, event.getValues().get(0).replace("about_", ""));
            return;
        } else if (id.equals("order_wiz_cat")) {
            com.highcore.bot.services.OrderService.handleCategory(event);
            return;
        } else if (id.equals("wiz_sel_services") || id.equals("wiz_sel_addons")) {
            com.highcore.bot.services.OrderService.handleMultiSelection(event);
            return;
        }

        boolean publicHandler = id.equals("ar_del_select") || id.equals("bw_del_select") || id.equals("ar_edit_select");
        if (publicHandler) {
            processSelect(event);
        } else {
            if (!event.isAcknowledged()) event.deferEdit().queue(v -> processSelect(event));
            else processSelect(event);
        }
    }

    private void processSelect(StringSelectInteractionEvent event) {
        try {
            String id = event.getComponentId();
            String userId = event.getUser().getId();
            
            if (id.equals("order_service_select")) {
                PanelService.handleCategorySelected(event, userId, event.getValues().get(0));
            } else if (id.equals("order_main_select")) {
                PanelService.handleMainSelected(event, userId, event.getValues());
            } else if (id.equals("order_addon_select")) {
                PanelService.handleAddonsSelected(event, userId, event.getValues());
            } else if (id.equals("ar_edit_select")) {
                String val = event.getValues().get(0);
                TextInput input = TextInput.create("ar_new_response", TextInputStyle.PARAGRAPH).setPlaceholder("Update auto-reply text...").setRequired(true).build();
                Modal m = Modal.create("modal_ar_edit_submit:" + val, "Edit: " + val)
                        .addComponents(net.dv8tion.jda.api.components.label.Label.of("New Response Content", input))
                        .build();
                event.replyModal(m).queue();
            } else if (id.equals("ar_del_select")) {
                String val = event.getValues().get(0);
                AutoReplyService.removeResponse(val);
                var res = EmbedUtil.containerBranded("SUCCESS", "Auto-Reply Removed", "The keyword **" + val + "** has been deleted.", EmbedUtil.BANNER_MAIN);
                PanelService.reply(event, res);
                AutoReplyCommands.refreshChannel(event.getChannel());
            } else if (id.equals("bw_del_select")) {
                String val = event.getValues().get(0);
                WordFilterService.removeWord(val);
                var res = EmbedUtil.containerBranded("SUCCESS", "Term Purged", "The term **" + val + "** has been removed from filter.", EmbedUtil.BANNER_MAIN);
                PanelService.replyEphemeral(event, res);
                BannedWordCommands.refreshChannel(event.getChannel());
            }
        } catch (Exception e) {
            log.error("Select error", e);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        
        if (id.equals("modal_support_init")) {
            event.deferReply(true).queue();
            String desc = event.getValue("issue_desc").getAsString();
            TicketService.createTicket(event, (desc.length()>80?desc.substring(0,77)+"...":desc), "MEDIUM", "SUPPORT", "**Service:** "+event.getValue("service_type").getAsString());
        } else if (id.equals("modal_ar_add")) {
            String kw = event.getValue("ar_keyword").getAsString();
            String resp = event.getValue("ar_response").getAsString();
            AutoReplyService.addResponse(kw, resp, event.getUser().getId());
            var res = EmbedUtil.containerBranded("SUCCESS", "Auto-Reply Added", "Keyword: **" + kw + "**\nResponse: `" + resp + "`", EmbedUtil.BANNER_MAIN);
            PanelService.reply(event, res);
            AutoReplyCommands.refreshChannel(event.getChannel());
        } else if (id.startsWith("modal_ar_edit_submit:")) {
            String kw = id.split(":")[1];
            String text = event.getValue("ar_new_response").getAsString();
            AutoReplyService.addResponse(kw, text, event.getUser().getId());
            var res = EmbedUtil.containerBranded("SUCCESS", "Auto-Reply Updated", "Keyword: **" + kw + "**\nNew Response: `" + text + "`", EmbedUtil.BANNER_MAIN);
            PanelService.reply(event, res);
            AutoReplyCommands.refreshChannel(event.getChannel());
        } else if (id.equals("modal_bw_add")) {
            String word = event.getValue("bw_word").getAsString();
            WordFilterService.addWord(word);
            var res = EmbedUtil.containerBranded("SUCCESS", "Term Blacklisted", "The term **" + word + "** is now monitored.", EmbedUtil.BANNER_MAIN);
            PanelService.replyEphemeral(event, res);
            BannedWordCommands.refreshChannel(event.getChannel());
        }
 else if (id.equals("modal_complaint_init")) {
            event.deferReply(true).queue();
            String desc = event.getValue("comp_desc").getAsString();
            TicketService.createTicket(event, (desc.length()>80?desc.substring(0,77)+"...":desc), "HIGH", "COMPLAINT", "**Target:** "+event.getValue("comp_person").getAsString()+"\n**Type:** "+event.getValue("comp_type").getAsString());
        } else if (id.equals("modal_order_final")) {
            event.deferReply(true).queue();
            PanelService.OrderSession session = PanelService.SESSIONS.remove(event.getUser().getId());
            List<InvoiceService.OrderItem> items = (session != null) ? PanelService.resolveItems(new ArrayList<String>() {{ addAll(session.mainIds); addAll(session.addonIds); }}) : new ArrayList<>();
            
            String vCode = event.getValue("o_voucher") != null ? event.getValue("o_voucher").getAsString() : "";
            
            // Ephemeral alert if voucher is bad
            if (!vCode.isBlank()) {
                com.google.gson.JsonObject voucher = com.highcore.bot.database.SupabaseClient.getVoucherByCode(vCode);
                if (voucher == null || !voucher.get("user_id").getAsString().equals(event.getUser().getId()) || voucher.get("is_used").getAsBoolean()) {
                    event.getHook().sendMessage("### \u26A0\uFE0F Voucher Alert\nThe code you entered (`" + vCode + "`) is **invalid** or has already been used. The order will proceed without this discount.").setEphemeral(true).queue();
                }
            }

            TicketService.createHighEndOrderTicket(event.getGuild(), event.getUser(), 
                event.getValue("o_project").getAsString(), 
                event.getValue("o_name").getAsString(), 
                event.getValue("o_contact").getAsString(),
                event.getValue("o_phone").getAsString(),
                event.getValue("o_eta").getAsString(), 
                items, vCode);
            
            event.getHook().sendMessage("✅ Order submitted and ticket created.").setEphemeral(true).queue();
        } else if (id.equals("order_modal")) {
            event.deferReply(true).queue();
            OrderService.OrderSession session = OrderService.sessions.remove(event.getUser().getId());
            List<InvoiceService.OrderItem> items = (session != null) ? OrderService.resolveItems(new ArrayList<String>() {{ addAll(session.selectedServices); addAll(session.selectedAddons); }}) : new ArrayList<>();
            
            TicketService.createHighEndOrderTicket(event.getGuild(), event.getUser(), 
                event.getValue("o_project").getAsString(), 
                event.getValue("o_name").getAsString(), 
                event.getValue("o_contact").getAsString(),
                event.getValue("o_phone").getAsString(),
                event.getValue("o_eta").getAsString(), 
                items, ""); // No voucher field in wizard
            
            event.getHook().sendMessage("✅ Order submitted and ticket created.").setEphemeral(true).queue();
        } else if (id.equals("modal_bc")) {
            com.highcore.bot.commands.SlashCommands.BcSession s = com.highcore.bot.commands.SlashCommands.BC_SESSIONS.remove("bc_" + event.getUser().getId());
            if (s != null && BroadcastService.startBroadcast(event.getGuild(), event.getValue("message").getAsString(), s.roleId, s.attUrl)) {
                PanelService.replyEphemeral(event, "Broadcast started.");
            }
        } else if (id.equals("modal_boter")) {
            com.highcore.bot.commands.SlashCommands.BoterSession s = com.highcore.bot.commands.SlashCommands.BOTER_SESSIONS.remove("boter_" + event.getUser().getId());
            if (s != null) {
                var tc = event.getGuild().getTextChannelById(s.channelId);
                if (tc != null) tc.sendMessage(event.getValue("message").getAsString()).queue();
                event.reply("Sent.").setEphemeral(true).queue();
            }
        }
    }
}
