package com.highcore.bot.listeners;

import com.highcore.bot.services.*;
import com.highcore.bot.config.Config;
import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.commands.AutoReplyCommands;
import com.highcore.bot.commands.BannedWordCommands;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
    private boolean isStaff(net.dv8tion.jda.api.entities.Member member) {
        return Config.isStaff(member);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        Member member = event.getMember();
        if (member == null) return;

        // Secure administrative actions to STAFF only
        List<String> restricted = List.of(
            "ticket_claim", "ticket_unclaim", "ticket_close", 
            "ticket_reopen", "ticket_transcript", 
            "ticket_delete_init", "ticket_delete_final", "ticket_verify"
        );

        if (restricted.contains(id) || id.startsWith("order_status_update_")) {
            if (!isStaff(member)) {
                event.reply("### ❌ ACCESS DENIED\nThis module is restricted to authorized Highcore Operatives only.").setEphemeral(true).queue();
                return;
            }
        }

        try {
            if (id.equals("ticket_claim")) {
                TicketService.claimTicket(event.getChannel().asTextChannel(), member, event);
            } else if (id.equals("ticket_unclaim")) {
                TicketService.unclaimTicket(event.getChannel().asTextChannel(), member, event);
            } else if (id.equals("ticket_verify")) {
                String tid = event.getChannel().getName().substring(event.getChannel().getName().lastIndexOf("-") + 1);
                TicketService.markAsPaid(event.getChannel().asTextChannel(), tid, member);
                event.reply("✅ Payment verification initiated.").setEphemeral(true).queue();
            } else if (id.equals("ticket_close")) {
                TicketService.requestCloseConfirmation(event);
            } else if (id.equals("ticket_close_final")) {
                TicketService.closeTicket(event, member);
            } else if (id.equals("ticket_close_cancel")) {
                event.reply("Closure cancelled.").setEphemeral(true).queue();
            } else if (id.equals("ticket_reopen")) {
                TicketService.reopenTicket(event.getChannel().asTextChannel(), member, event);
            } else if (id.equals("ticket_transcript")) {
                TicketService.transcriptTicket(event.getChannel().asTextChannel(), member, event);
            } else if (id.equals("ticket_delete_init")) {
                TicketService.requestDeleteConfirmation(event);
            } else if (id.equals("ticket_delete_final")) {
                TicketService.deleteTicket(event.getChannel().asTextChannel());
            } else if (id.equals("ticket_delete_cancel")) {
                event.reply("Deletion cancelled.").setEphemeral(true).queue();
            } else if (id.equals("wiz_finish")) {
                OrderService.finishWizard(event);
            } else if (id.equals("order_final")) {
                PanelService.handleOrderFinalModal(event, id);
            } else if (id.equals("ticket_init_order")) {
                PanelService.handleOrderFlow(event);
            } else if (id.equals("ticket_init_support")) {
                TextInput st = TextInput.create("service_type", TextInputStyle.SHORT).setPlaceholder("Which service?").setRequired(true).build();
                TextInput idesc = TextInput.create("issue_desc", TextInputStyle.PARAGRAPH).setPlaceholder("Details...").setRequired(true).build();
                event.replyModal(Modal.create("modal_support_init", "Support").addComponents(ActionRow.of(st), ActionRow.of(idesc)).build()).queue();
            } else if (id.equals("ticket_init_complaint")) {
                TextInput pr = TextInput.create("comp_person", TextInputStyle.SHORT).setPlaceholder("Against").setRequired(false).build();
                TextInput tp = TextInput.create("comp_type", TextInputStyle.SHORT).setPlaceholder("Subject").setRequired(true).build();
                TextInput ds = TextInput.create("comp_desc", TextInputStyle.PARAGRAPH).setPlaceholder("Details").setRequired(true).build();
                event.replyModal(Modal.create("modal_complaint_init", "Complaint").addComponents(ActionRow.of(pr), ActionRow.of(tp), ActionRow.of(ds)).build()).queue();
            } else if (id.startsWith("ticket_pay_")) {
                String method = id.split("_")[2];
                event.reply("✅ Method selected: **" + method.toUpperCase() + "**. Please provide proof of payment once finished.").setEphemeral(true).queue();
            } else if (id.startsWith("ticket_mark_paid_")) {
                TicketService.markAsPaid(event.getChannel().asTextChannel(), id.replace("ticket_mark_paid_", ""), member);
            } else if (id.startsWith("order_status_update_")) {
                TicketService.finalizeClose(event.getChannel().asTextChannel(), member, id.replace("order_status_update_", ""));
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
            } else if (id.equals("ar_add")) {
                TextInput kw = TextInput.create("ar_keyword", TextInputStyle.SHORT).setPlaceholder("Keyword").setRequired(true).build();
                TextInput rs = TextInput.create("ar_response", TextInputStyle.PARAGRAPH).setPlaceholder("Response").setRequired(true).build();
                event.replyModal(Modal.create("modal_ar_add", "New AR").addComponents(ActionRow.of(kw), ActionRow.of(rs)).build()).queue();
            } else if (id.startsWith("ping_")) {
                String roleId = id.replace("ping_", "");
                net.dv8tion.jda.api.entities.Role role = event.getGuild().getRoleById(roleId);
                if (role == null) {
                    event.reply("### ❌ Role Not Found\nThe requested notification layer is currently unavailable.").setEphemeral(true).queue();
                    return;
                }
                if (member.getRoles().contains(role)) {
                    event.getGuild().removeRoleFromMember(member, role).queue();
                    event.reply("### 🔔 Notifications Disabled\nYou will no longer receive alerts for: " + role.getAsMention()).setEphemeral(true).queue();
                } else {
                    event.getGuild().addRoleToMember(member, role).queue();
                    event.reply("### 🔔 Notifications Enabled\nYou have subscribed to: " + role.getAsMention()).setEphemeral(true).queue();
                }
            } else if (id.startsWith("order_cat_")) {
                String cat = id.replace("order_cat_", "");
                PanelService.handleCategorySelected(event, event.getUser().getId(), cat);
            } else if (id.startsWith("feedback_star_")) {
                int stars = Integer.parseInt(id.replace("feedback_star_", ""));
                FeedbackService.ratingCache.put(event.getUser().getId(), stars);

                TextInput fb = TextInput.create("feedback_input", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("Tell us more about your experience...")
                        .setRequired(true)
                        .setMinLength(20)
                        .build();

                event.replyModal(Modal.create("modal_feedback_submit", "Write Your Feed-Back")
                        .addComponents(ActionRow.of(fb))
                        .build()).queue();
            }
        } catch (Exception e) { log.error("Button handling error", e); }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (id.equals("order_wiz_cat")) {
            OrderService.handleCategory(event);
        } else if (id.equals("wiz_sel_services") || id.equals("wiz_sel_addons")) {
            OrderService.handleMultiSelection(event);
        } else if (id.equals("order_service_select")) {
            PanelService.handleCategorySelected(event, event.getUser().getId(), event.getValues().get(0));
        } else if (id.equals("order_main_select")) {
            PanelService.handleMainSelected(event, event.getUser().getId(), event.getValues());
        } else if (id.equals("order_addon_select")) {
            PanelService.handleAddonsSelected(event, event.getUser().getId(), event.getValues());
        } else if (id.equals("about_category_select")) {
            String val = event.getValues().get(0);
            PanelService.sendServicePriceInfo(event, val.replace("about_", ""));
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        if (id.equals("order_modal")) {
            String vc = event.getValue("o_voucher") != null ? event.getValue("o_voucher").getAsString() : "";
            if (!vc.trim().isEmpty()) {
                String cleanCode = vc.trim().toUpperCase();
                JsonObject v = com.highcore.bot.database.SupabaseClient.getVoucherByCode(cleanCode);
                log.info("[VOUCHER DEBUG] Code: {}, Found: {}, State: {}", cleanCode, (v != null), (v != null ? v.get("is_used") : "N/A"));
                String errorMsg = null;
                if (v == null) {
                    errorMsg = "Code is not registered in the database.";
                } else if (!v.get("user_id").getAsString().equals(event.getUser().getId())) {
                    errorMsg = "This code belongs to another user.";
                } else if (v.has("is_used") && !v.get("is_used").isJsonNull() && v.get("is_used").getAsBoolean()) {
                    errorMsg = "This voucher has already been used and claimed.";
                } else if (v.has("expires_at") && !v.get("expires_at").isJsonNull()) {
                    boolean expired = false;
                    try {
                        if (java.time.OffsetDateTime.parse(v.get("expires_at").getAsString()).toInstant().isBefore(java.time.Instant.now())) expired = true;
                    } catch (Exception e) {
                        try {
                            if (java.time.Instant.parse(v.get("expires_at").getAsString()).isBefore(java.time.Instant.now())) expired = true;
                        } catch (Exception e2) {}
                    }
                    if (expired) errorMsg = "This voucher is expired and cannot be used.";
                }
                
                if (errorMsg != null) {
                    event.reply("❌ **Invalid Voucher** \u2014 " + errorMsg).setEphemeral(true).queue();
                    return;
                }
            }
            event.deferReply(true).queue();
            OrderService.OrderSession s = OrderService.sessions.remove(event.getUser().getId());
            List<InvoiceService.OrderItem> main = (s != null) ? OrderService.resolveItems(s.selectedServices) : new ArrayList<>();
            List<InvoiceService.OrderItem> add = (s != null) ? OrderService.resolveItems(s.selectedAddons) : new ArrayList<>();
            String cat = (s != null) ? (s.category.substring(0,1).toUpperCase() + s.category.substring(1).toLowerCase()) : "Projects";
            TicketService.createHighEndOrderTicket(event.getGuild(), event.getUser(), event.getValue("o_project").getAsString(), event.getValue("o_name").getAsString(), event.getValue("o_contact").getAsString(), "", cat, main, add, event.getValue("o_voucher").getAsString(), event.getValue("o_eta").getAsString());
            event.getHook().sendMessage("✅ Order ticket created.").setEphemeral(true).queue();
        } else if (id.equals("modal_order_final")) {
            String vc = event.getValue("o_voucher") != null ? event.getValue("o_voucher").getAsString() : "";
            if (!vc.trim().isEmpty()) {
                JsonObject v = com.highcore.bot.database.SupabaseClient.getVoucherByCode(vc.trim().toUpperCase());
                log.info("[VOUCHER DEBUG] Code entered: '{}', Found voucher data: {}", vc, v);
                String errorMsg = null;
                if (v == null) {
                    errorMsg = "Code is not registered in the database.";
                } else if (!v.get("user_id").getAsString().equals(event.getUser().getId())) {
                    errorMsg = "This code belongs to another user.";
                } else if (v.has("is_used") && !v.get("is_used").isJsonNull() && v.get("is_used").getAsBoolean()) {
                    errorMsg = "This voucher has already been used and claimed.";
                } else if (v.has("expires_at") && !v.get("expires_at").isJsonNull()) {
                    boolean expired = false;
                    try {
                        if (java.time.OffsetDateTime.parse(v.get("expires_at").getAsString()).toInstant().isBefore(java.time.Instant.now())) expired = true;
                    } catch (Exception e) {
                        try {
                            if (java.time.Instant.parse(v.get("expires_at").getAsString()).isBefore(java.time.Instant.now())) expired = true;
                        } catch (Exception e2) {}
                    }
                    if (expired) errorMsg = "This voucher is expired and cannot be used.";
                }
                
                if (errorMsg != null) {
                    event.reply("❌ **Invalid Voucher** \u2014 " + errorMsg).setEphemeral(true).queue();
                    return;
                }
            }
            event.deferReply(true).queue();
            PanelService.OrderSession s = PanelService.SESSIONS.remove(event.getUser().getId());
            List<InvoiceService.OrderItem> main = (s != null) ? PanelService.resolveItems(s.mainIds) : new ArrayList<>();
            List<InvoiceService.OrderItem> add = (s != null) ? PanelService.resolveItems(s.addonIds) : new ArrayList<>();
            String cat = (s != null) ? (s.category.substring(0,1).toUpperCase() + s.category.substring(1).toLowerCase()) : "Projects";
            TicketService.createHighEndOrderTicket(event.getGuild(), event.getUser(), event.getValue("o_project").getAsString(), event.getValue("o_name").getAsString(), event.getValue("o_contact").getAsString(), "", cat, main, add, event.getValue("o_voucher").getAsString(), event.getValue("o_eta").getAsString());
            event.getHook().sendMessage("✅ Order ticket created.").setEphemeral(true).queue();
        } else if (id.equals("modal_support_init")) {
            TicketService.createTicket(event, event.getValue("issue_desc").getAsString(), "SUPPORT", "Service: "+event.getValue("service_type").getAsString());
        } else if (id.equals("modal_complaint_init")) {
            TicketService.createTicket(event, event.getValue("comp_desc").getAsString(), "COMPLAINT", "Target: " + event.getValue("comp_person").getAsString() + " | Subject: " + event.getValue("comp_type").getAsString());
        } else if (id.equals("modal_ar_add")) {
            AutoReplyService.addResponse(event.getValue("ar_keyword").getAsString(), event.getValue("ar_response").getAsString(), event.getUser().getId());
            event.reply("Auto-reply added.").setEphemeral(true).queue();
        } else if (id.equals("modal_feedback_submit")) {
            event.deferReply(true).queue();
            event.getHook().editOriginal("### ⏳ Processing Your Feed-Back...\nOur systems are generating your professional feedback card. Please wait a moment.").queue();
            
            Integer starsRaw = FeedbackService.ratingCache.remove(event.getUser().getId());
            final int stars = (starsRaw == null) ? 5 : starsRaw;
            final String feedback = event.getValue("feedback_input").getAsString();
            final net.dv8tion.jda.api.entities.channel.middleman.GuildChannel logCh = event.getGuild().getGuildChannelById(FeedbackService.FEEDBACK_CHANNEL_ID);
            
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                FeedbackService.submitFeedback(event.getUser(), stars, feedback, logCh);
                event.getHook().editOriginal("### ✅ Thank You!\nYour feedback has been submitted successfully. We appreciate your support!").queue();
            }).exceptionally(ex -> {
                log.error("Error submitting feedback", ex);
                event.getHook().editOriginal("### ❌ Submission Failed\nThere was an error processing your feedback. Please try again later.").queue();
                return null;
            });
        } else if (id.equals("modal_bc")) {
            event.deferReply(true).queue();
            SlashCommands.BcSession session = SlashCommands.BC_SESSIONS.remove("bc_" + event.getUser().getId());
            String message = event.getValue("message").getAsString();
            if (session != null) {
                BroadcastService.startBroadcast(event.getGuild(), message, session.roleId, session.attUrl);
                event.getHook().sendMessage("✅ Broadcast sequence initiated.").setEphemeral(true).queue();
            } else {
                event.getHook().sendMessage("❌ Session expired. Please try again.").setEphemeral(true).queue();
            }
        } else if (id.equals("modal_boter")) {
            event.deferReply(true).queue();
            SlashCommands.BoterSession session = SlashCommands.BOTER_SESSIONS.remove("boter_" + event.getUser().getId());
            String message = event.getValue("message").getAsString();
            if (session != null) {
                net.dv8tion.jda.api.entities.channel.concrete.TextChannel target = event.getGuild().getTextChannelById(session.channelId);
                if (target != null) {
                    List<com.highcore.bot.components.mediagallery.MediaGalleryItem> items = session.fileUrls.stream()
                        .map(url -> com.highcore.bot.components.mediagallery.MediaGalleryItem.fromUrl(url))
                        .toList();
                    
                    List<net.dv8tion.jda.api.components.container.ContainerChildComponent> layout = new ArrayList<>();
                    if (!items.isEmpty()) layout.add(com.highcore.bot.components.mediagallery.MediaGallery.of(items));
                    layout.add(net.dv8tion.jda.api.components.textdisplay.TextDisplay.of(com.highcore.bot.utils.EmojiUtil.parse(message)));
                    
                    target.sendMessageComponents(net.dv8tion.jda.api.components.container.Container.of(layout)).useComponentsV2(true).queue();
                    event.getHook().sendMessage("✅ Transmission deployed to " + target.getAsMention()).setEphemeral(true).queue();
                } else {
                    event.getHook().sendMessage("❌ Target channel not found.").setEphemeral(true).queue();
                }
            } else {
                event.getHook().sendMessage("❌ Session expired.").setEphemeral(true).queue();
            }
        }
    }
}
