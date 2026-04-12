package com.highcore.bot.listeners;

import com.highcore.bot.services.InvoiceService;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.services.BroadcastService;
import com.highcore.bot.services.TicketService;
import com.highcore.bot.services.AIService;
import com.highcore.bot.services.OrderService;
import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.config.Config;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

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

        if (id.equals("menu_main")) { PanelService.sendStartupHub(event); return; }
        if (id.equals("menu_tickets") || id.equals("btn_ticket")) { PanelService.sendTicketPanel(event); return; }
        if (id.equals("menu_services") || id.equals("view_services") || id.equals("btn_services")) { PanelService.sendServicesCategory(event); return; }
        if (id.equals("menu_stats") || id.equals("startup_stats")) { PanelService.sendStatsPanel(event); return; }
        
        if (id.equals("menu_points")) {
            PanelService.replyEphemeral(event, EmbedUtil.eliteContainer("Telemetry", "Points registry operational.", "User points: Classified. Use /points for report."));
            return;
        }
        if (id.equals("view_team") || id.equals("btn_team")) {
            PanelService.replyEphemeral(event, EmbedUtil.eliteContainer("Personnel", "High Core Agency Staff", "Active personnel roster loaded."));
            return;
        }
        
        if (id.equals("view_rules")) {
            PanelService.replyEphemeral(event, EmbedUtil.rulePanel());
            return;
        }
        
        if (id.equals("quick_query") || id.equals("ai_query")) {
            AIService.enableAI(event.getChannel().getId());
            event.replyComponents(EmbedUtil.assistantResponse("Assistant activated. Looking for information... Ready for your message."))
                    .useComponentsV2(true).setEphemeral(true).queue();
            return;
        }
        
        if (id.equals("order_start")) { OrderService.startWizard(event); return; }
        if (id.equals("support_start")) {
            TextInput subject = TextInput.create("subject", TextInputStyle.SHORT)
                    .setPlaceholder("Describe the technical issue...").setRequired(true).build();
            Modal m = Modal.create("modal_ticket_open", "Support Request")
                    .addComponents(Label.of("Problem Brief", subject))
                    .build();
            event.replyModal(m).queue();
            return;
        }
        if (id.equals("report_start")) {
            TextInput reason = TextInput.create("reason", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("What are you reporting?").setRequired(true).build();
            Modal m = Modal.create("modal_report_open", "Submit a Report")
                    .addComponents(Label.of("Report Context", reason))
                    .build();
            event.replyModal(m).queue();
            return;
        }

        if (id.startsWith("ticket_") && !id.startsWith("ticket_type_") && !id.startsWith("ticket_modal_")) {
            if (!isStaff(member)) {
                PanelService.reply(event, EmbedUtil.accessDenied());
                return;
            }
            if (id.equals("ticket_claim")) { TicketService.claimTicket(event.getChannel().asTextChannel(), member); event.deferEdit().queue(); return; }
            if (id.equals("ticket_close")) { TicketService.closeTicket(event.getChannel().asTextChannel(), member); event.deferEdit().queue(); return; }
            if (id.equals("ticket_reopen")) { TicketService.reopenTicket(event.getChannel().asTextChannel(), member); event.deferEdit().queue(); return; }
            if (id.equals("ticket_delete")) {
                 event.replyComponents(EmbedUtil.warning("CLOSING", "The channel will be deleted in 5 seconds.")).useComponentsV2(true).queue();
                 event.getChannel().delete().queueAfter(5, TimeUnit.SECONDS);
                 return;
            }
        }

        if (id.startsWith("pay_")) {
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
                    "**Support:** <@&" + Config.ROLE_STAFF + ">";
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
                    "Contact a staff member for payment assistance.\n<@&" + Config.ROLE_STAFF + ">";
            };
            PanelService.replyEphemeral(event, EmbedUtil.containerBranded("PAYMENT", "Gateway — " + method, info, EmbedUtil.BANNER_MAIN));
            return;
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (id.equals("ticket_type_select")) {
            String choice = event.getValues().get(0);
            switch (choice) {
                case "purchase" -> OrderService.startWizard(event);
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
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        
        if (id.equals("order_modal")) {
            String pName   = event.getValue("o_project").getAsString();
            String cName   = event.getValue("o_name").getAsString();
            String contact = event.getValue("o_contact").getAsString();
            String eta     = event.getValue("o_eta").getAsString();

            String userId = event.getUser().getId();
            OrderService.OrderSession session = OrderService.sessions.remove(userId);

            List<InvoiceService.OrderItem> items = new java.util.ArrayList<>();
            if (session != null) {
                List<String> allIds = new java.util.ArrayList<>(session.selectedServices);
                allIds.addAll(session.selectedAddons);
                items = OrderService.resolveItems(allIds);
            }

            net.dv8tion.jda.api.entities.Guild guild = event.getGuild();
            if (guild != null) {
                TicketService.createHighEndOrderTicket(guild, event.getUser(), pName, cName, contact, eta, items);
            }

            PanelService.reply(event, EmbedUtil.success("ORDER SUBMITTED", "Your ticket has been created. It will be unlocked once payment is confirmed."));
        }
    }
}
