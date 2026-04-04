package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.AIService;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.services.TicketService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ButtonListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ButtonListener.class);

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        switch (id) {
            case "view_services" -> event.replyEmbeds(EmbedUtil.services()).setEphemeral(true).queue();
            case "view_team" -> event.replyEmbeds(EmbedUtil.team()).setEphemeral(true).queue();
            case "quick_query" -> { AIService.enableAI(event.getChannel().getId()); event.reply("\uD83D\uDCAC AI enabled! Type your question. `!stop` to disable.").setEphemeral(true).queue(); }
            case "ticket_claim" -> { if (!isStaff(event.getMember())) { event.reply("Staff only.").setEphemeral(true).queue(); return; } event.deferEdit().queue(); TicketService.claimTicket((TextChannel) event.getChannel(), event.getMember()); }
            case "ticket_close" -> { if (!isStaff(event.getMember())) { event.reply("Staff only.").setEphemeral(true).queue(); return; } event.deferEdit().queue(); TicketService.closeTicket((TextChannel) event.getChannel(), event.getMember()); }
            case "ticket_reopen" -> { event.deferEdit().queue(); TicketService.reopenTicket((TextChannel) event.getChannel(), event.getMember()); }
            case "ticket_delete" -> { if (!isStaff(event.getMember())) { event.reply("Staff only.").setEphemeral(true).queue(); return; } event.reply("\uD83D\uDDD1\uFE0F Deleting in 5s...").queue(); event.getChannel().delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS); }
            case "ticket_transcript" -> { event.deferReply(true).queue(); event.getHook().editOriginal("\uD83D\uDCDC Generating...").queue(); TicketService.sendTranscript((TextChannel) event.getChannel()); }
            default -> {
                if (id.startsWith("rate_")) handleRating(event);
                else event.reply("Unknown button.").setEphemeral(true).queue();
            }
        }
    }

    // ===== DROPDOWN SELECTION → MODAL =====
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("ticket_type_select")) return;
        String type = event.getValues().get(0);

        TextInput details = TextInput.create("ticket_details", "Details", TextInputStyle.PARAGRAPH)
                .setPlaceholder(getPlaceholder(type)).setRequired(true).setMinLength(10).setMaxLength(1000).build();
        TextInput priority = TextInput.create("ticket_priority", "Priority (low/medium/high)", TextInputStyle.SHORT)
                .setPlaceholder("low").setRequired(false).setMaxLength(10).build();

        Modal modal = Modal.create("ticket_modal_" + type, getModalTitle(type))
                .addComponents(ActionRow.of(details), ActionRow.of(priority)).build();
        event.replyModal(modal).queue();
    }

    private String getPlaceholder(String type) {
        return switch (type) {
            case "tech_support" -> "Describe your technical issue...";
            case "inquiry" -> "What would you like to know?";
            case "purchase" -> "Which service are you interested in?";
            case "order_status" -> "Provide your order number or details...";
            case "apply_management" -> "Tell us about yourself and why you want to join...";
            case "apply_team" -> "What skills do you have? Why Highcore?";
            default -> "Describe your request...";
        };
    }

    private String getModalTitle(String type) {
        return switch (type) {
            case "tech_support" -> "\uD83D\uDD27 Technical Support";
            case "inquiry" -> "\u2753 General Inquiry";
            case "purchase" -> "\uD83D\uDED2 Purchase Service";
            case "order_status" -> "\uD83D\uDCE6 Order Status";
            case "apply_management" -> "\uD83D\uDC54 Management Application";
            case "apply_team" -> "\uD83D\uDC65 Team Application";
            default -> "\uD83D\uDCE9 New Ticket";
        };
    }

    // ===== MODAL SUBMIT =====
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().startsWith("ticket_modal_")) return;
        event.deferReply(true).queue();

        String type = event.getModalId().replace("ticket_modal_", "");
        String details = event.getValue("ticket_details").getAsString();
        String priority = "normal";
        var pv = event.getValue("ticket_priority");
        if (pv != null && !pv.getAsString().trim().isEmpty()) {
            String p = pv.getAsString().trim().toLowerCase();
            if (p.equals("high") || p.equals("medium") || p.equals("low")) priority = p;
        }

        String subject = getModalTitle(type) + " | " + details;
        TextChannel tc = TicketService.createTicket(event.getGuild(), event.getUser(), subject, priority, type);
        if (tc != null) event.getHook().editOriginal("\u2705 Ticket created! " + tc.getAsMention()).queue();
        else event.getHook().editOriginal("Error creating ticket. Try again.").queue();

        // Log to ticket logs
        TextChannel logCh = LogManager.get(event.getGuild(), Config.LOG_TICKETS);
        if (logCh != null) {
            logCh.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.SUCCESS)
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .setDescription("### \uD83C\uDFAB Ticket Created")
                    .addField("\uD83D\uDC64 User", event.getUser().getAsMention(), true)
                    .addField("\uD83D\uDCCB Type", getModalTitle(type), true)
                    .addField("\u26A1 Priority", priority, true)
                    .addField("\uD83D\uDD52 Time", java.time.format.DateTimeFormatter.ofPattern("EEE, MMM dd yyyy \u2022 hh:mm:ss a")
                            .withZone(java.time.ZoneId.of("Asia/Riyadh")).format(java.time.Instant.now()), false)
                    .build()).queue();
        }
    }

    private void handleRating(ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split("_", 3);
        if (parts.length < 3) return;
        int stars; try { stars = Integer.parseInt(parts[1]); } catch (Exception e) { return; }
        com.highcore.bot.database.SupabaseClient.logStat("ticket_rating", event.getUser().getId(), parts[2] + ":" + stars);
        event.editMessageEmbeds(EmbedUtil.ratingThanks(stars)).setComponents().queue();
    }

    private boolean isStaff(Member m) {
        return m != null && m.getRoles().stream().anyMatch(r -> Config.getStaffRoles().contains(r.getId()));
    }
}
