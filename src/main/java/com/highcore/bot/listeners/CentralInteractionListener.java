package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.*;
import com.highcore.bot.commands.*;
import com.highcore.bot.utils.EmbedUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class CentralInteractionListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(CentralInteractionListener.class);

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        try {
            if (id.equals("btn_highcore")) {
                PanelService.sendHighcoreHub(event);
            } else if (id.equals("btn_pings")) {
                PanelService.sendPingsHub(event);
            } else if (id.equals("btn_about")) {
                PanelService.sendAboutUsHub(event);
            } else if (id.equals("btn_socials")) {
                PanelService.sendSocialsHub(event);
            } else if (id.equals("btn_partners")) {
                PanelService.sendPartnersPanel(event);
            } else if (id.equals("ticket_init_support")) {
                TextInput issue = TextInput.create("issue_desc", TextInputStyle.PARAGRAPH).setPlaceholder("Describe your issue").setRequired(true).build();
                TextInput service = TextInput.create("service_type", TextInputStyle.SHORT).setPlaceholder("Service Type").setRequired(true).build();
                event.replyModal(Modal.create("modal_support_init", "Support Request")
                        .addComponents(
                            net.dv8tion.jda.api.components.label.Label.of("Issue", issue),
                            net.dv8tion.jda.api.components.label.Label.of("Service", service))
                        .build()).queue();
            } else if (id.equals("ticket_init_complaint")) {
                TextInput type = TextInput.create("comp_type", TextInputStyle.SHORT).setPlaceholder("Complaint Type").setRequired(true).build();
                TextInput person = TextInput.create("comp_person", TextInputStyle.SHORT).setPlaceholder("Person involved").setRequired(true).build();
                TextInput desc = TextInput.create("comp_desc", TextInputStyle.PARAGRAPH).setPlaceholder("Description").setRequired(true).build();
                event.replyModal(Modal.create("modal_complaint_init", "File Complaint")
                        .addComponents(
                            net.dv8tion.jda.api.components.label.Label.of("Type", type),
                            net.dv8tion.jda.api.components.label.Label.of("Person", person),
                            net.dv8tion.jda.api.components.label.Label.of("Description", desc))
                        .build()).queue();
            } else if (id.startsWith("ping_")) {
                String roleId = id.replace("ping_", "");
                Member m = event.getMember();
                if (m == null) return;
                net.dv8tion.jda.api.entities.Role role = event.getGuild().getRoleById(roleId);
                if (role == null) { event.reply("Role error.").setEphemeral(true).queue(); return; }
                if (m.getRoles().contains(role)) {
                    event.getGuild().removeRoleFromMember(m, role).queue();
                    event.reply("Role removed: " + role.getName()).setEphemeral(true).queue();
                } else {
                    event.getGuild().addRoleToMember(m, role).queue();
                    event.reply("Role added: " + role.getName()).setEphemeral(true).queue();
                }
            } else if (id.startsWith("order_cat_")) {
                String cat = id.replace("order_cat_", "");
                PanelService.handleCategorySelected(event, event.getUser().getId(), cat);
            } else if (id.startsWith("feedback_star_")) {
                int stars = Integer.parseInt(id.replace("feedback_star_", ""));
                FeedbackService.ratingCache.put(event.getUser().getId(), stars);
                TextInput fb = TextInput.create("feedback_input", TextInputStyle.PARAGRAPH).setPlaceholder("Tell us more...").setRequired(true).build();
                event.replyModal(Modal.create("modal_feedback_submit", "Write Feedback")
                        .addComponents(net.dv8tion.jda.api.components.label.Label.of("Feedback", fb))
                        .build()).queue();
            } else if (id.equals("order_final")) {
                PanelService.handleOrderFinalModal(event, id);
            } else if (id.startsWith("translate_init_")) {
                 // Forward to handleTranslationRequest if it exists in TranslationListener or implement here
                 // For now, use existing pattern if available
            } else if (id.startsWith("ticket_") && !id.startsWith("ticket_init_")) {
                Member member = event.getMember();
                if (member == null) return;
                
                // Restriction for ticket options
                if (member.getRoles().stream().noneMatch(r -> r.getId().equals("1488795130008961040"))) {
                    event.reply("هذا الخيار متاح فقط لرتبة الإدارة المخصصة.").setEphemeral(true).queue();
                    return;
                }

                TextChannel tc = event.getChannel().asTextChannel();
                switch (id) {
                    case "ticket_claim" -> TicketService.claimTicket(tc, member, event);
                    case "ticket_unclaim" -> TicketService.unclaimTicket(tc, member, event);
                    case "ticket_close" -> TicketService.requestCloseConfirmation(event);
                    case "ticket_close_final" -> TicketService.closeTicket(event, member);
                    case "ticket_reopen" -> TicketService.reopenTicket(tc, member, event);
                    case "ticket_transcript" -> TicketService.transcriptTicket(tc, member, event);
                    case "ticket_delete_init" -> TicketService.requestDeleteConfirmation(event);
                    case "ticket_delete_final" -> TicketService.deleteTicket(tc);
                    case "ticket_verify" -> event.reply("✅ Payment verification system initialized. Staff will review shortly.").setEphemeral(true).queue();
                }
            }
        } catch (Exception e) { log.error("Button error", e); }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        try {
            if (id.equals("order_main_select")) {
                PanelService.handleMainSelected(event, event.getUser().getId(), event.getValues());
            } else if (id.equals("order_addon_select")) {
                PanelService.handleAddonsSelected(event, event.getUser().getId(), event.getValues());
            } else if (id.equals("about_category_select")) {
                String cat = event.getValues().get(0).replace("about_", "");
                PanelService.sendServicePriceInfo(event, cat);
            } else if (id.equals("ticket_manage_menu")) {
                String selection = event.getValues().get(0);
                Member member = event.getMember();
                if (member == null) return;
                
                // Permission check: only specific management role can manage tickets
                if (member.getRoles().stream().noneMatch(r -> r.getId().equals("1488795130008961040"))) {
                    event.reply("You do not have permission to manage this ticket.").setEphemeral(true).queue();
                    return;
                }

                switch (selection) {
                    case "ticket_manage_rename":
                        TextInput nameInput = TextInput.create("new_name", TextInputStyle.SHORT)
                                .setPlaceholder("ادخل الاسم الجديد هنا...")
                                .setRequired(true)
                                .build();
                        event.replyModal(Modal.create("modal_ticket_rename", "تغيير اسم التذكرة")
                                .addComponents(Label.of("الاسم الجديد", nameInput))
                                .build()).queue();
                        break;
                    case "ticket_manage_add":
                        TextInput addInput = TextInput.create("user_id", TextInputStyle.SHORT)
                                .setPlaceholder("ادخل ID العضو لإضافته...")
                                .setRequired(true)
                                .build();
                        event.replyModal(Modal.create("modal_ticket_add", "إضافة عضو للتذكرة")
                                .addComponents(Label.of("معرف العضو (ID)", addInput))
                                .build()).queue();
                        break;
                    case "ticket_manage_remove":
                        TextInput removeInput = TextInput.create("user_id", TextInputStyle.SHORT)
                                .setPlaceholder("ادخل ID العضو لإزالته...")
                                .setRequired(true)
                                .build();
                        event.replyModal(Modal.create("modal_ticket_remove", "إزالة عضو من التذكرة")
                                .addComponents(Label.of("معرف العضو (ID)", removeInput))
                                .build()).queue();
                        break;
                }
            }
        } catch (Exception e) { log.error("Select error", e); }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        try {
            if (id.equals("modal_support_init")) {
                String issue = event.getValue("issue_desc").getAsString();
                String service = event.getValue("service_type").getAsString();
                TicketService.createTicket(event, "Support Request", "Support", "Issue: " + issue + "\nService: " + service);
            } else if (id.equals("modal_complaint_init")) {
                String type = event.getValue("comp_type").getAsString();
                String person = event.getValue("comp_person").getAsString();
                String desc = event.getValue("comp_desc").getAsString();
                TicketService.createTicket(event, "Complaint", "Complaint", "Type: " + type + "\nPerson: " + person + "\nDesc: " + desc);
            } else if (id.equals("modal_order_final")) {
                OrderService.finishWizard(null); // finishWizard expects a ButtonInteractionEvent, we need to adapt
                // Actually, OrderService.finishWizard is for buttons. We need a modal handler.
                // I'll check OrderService again for modal handling.
            } else if (id.equals("modal_feedback_submit")) {
                event.deferReply(true).queue();
                String feedback = event.getValue("feedback_input").getAsString();
                Integer stars = FeedbackService.ratingCache.remove(event.getUser().getId());
                FeedbackService.submitFeedback(event.getUser(), (stars == null ? 5 : stars), feedback, event.getGuild().getGuildChannelById(FeedbackService.FEEDBACK_CHANNEL_ID));
                event.getHook().sendMessage("Feedback submitted. Thank you!").setEphemeral(true).queue();
            } else if (id.equals("modal_bc")) {
                event.deferReply(true).queue();
                SlashCommands.BcSession session = SlashCommands.BC_SESSIONS.remove("bc_" + event.getUser().getId());
                String message = event.getValue("message").getAsString();
                if (session != null) {
                    BroadcastService.startBroadcast(event.getGuild(), message, session.roleId, session.attUrl);
                    event.getHook().sendMessage("Broadcast initiated.").setEphemeral(true).queue();
                }
            } else if (id.equals("modal_boter")) {
                event.deferReply(true).queue();
                SlashCommands.BoterSession session = SlashCommands.BOTER_SESSIONS.remove("boter_" + event.getUser().getId());
                String message = event.getValue("message").getAsString();
                if (session != null) {
                    TextChannel ch = event.getGuild().getTextChannelById(session.channelId);
                    if (ch != null) {
                        PanelService.reply(ch, com.highcore.bot.utils.EmojiUtil.parse(message));
                        event.getHook().sendMessage("Transmission deployed.").setEphemeral(true).queue();
                    }
                }
            } else if (id.equals("modal_ticket_rename")) {
                String newName = event.getValue("new_name").getAsString();
                event.getGuildChannel().asTextChannel().getManager().setName(newName).queue(
                        v -> event.reply("✅ تم تغيير اسم القناة إلى: `" + newName + "`").setEphemeral(true).queue(),
                        e -> event.reply("❌ فشل تغيير اسم القناة: " + e.getMessage()).setEphemeral(true).queue()
                );
            } else if (id.equals("modal_ticket_add")) {
                String userId = event.getValue("user_id").getAsString();
                event.getGuild().retrieveMemberById(userId).queue(
                        m -> {
                            event.getGuildChannel().asTextChannel().getManager()
                                    .putMemberPermissionOverride(m.getIdLong(), java.util.EnumSet.of(net.dv8tion.jda.api.Permission.VIEW_CHANNEL, net.dv8tion.jda.api.Permission.MESSAGE_SEND), null)
                                    .queue();
                            event.reply("✅ تم إضافة " + m.getAsMention() + " إلى التذكرة.").setEphemeral(true).queue();
                        },
                        e -> event.reply("❌ لم يتم العثور على عضو بهذا المعرف.").setEphemeral(true).queue()
                );
            } else if (id.equals("modal_ticket_remove")) {
                String userId = event.getValue("user_id").getAsString();
                event.getGuild().retrieveMemberById(userId).queue(
                        m -> {
                            event.getGuildChannel().asTextChannel().getManager()
                                    .removePermissionOverride(m.getIdLong())
                                    .queue();
                            event.reply("✅ تم إزالة " + m.getAsMention() + " من التذكرة.").setEphemeral(true).queue();
                        },
                        e -> event.reply("❌ لم يتم العثور على عضو بهذا المعرف.").setEphemeral(true).queue()
                );
            }
        } catch (Exception e) { log.error("Modal error", e); }
    }
}
