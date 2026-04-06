package com.highcore.bot.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.*;
import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.utils.EmojiUtil;
import com.highcore.bot.commands.SlashCommands;
import com.highcore.bot.config.Config;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;

public class CentralInteractionListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();

        // --- NAVIGATION & MENUS ---
        if (id.equals("menu_main")) { PanelService.sendMainMenu(event); return; }
        if (id.equals("menu_tickets") || id.equals("btn_ticket")) { PanelService.sendTicketPanel(event); return; }
        if (id.equals("menu_services") || id.equals("view_services") || id.equals("btn_services")) { PanelService.sendServicesPanel(event); return; }
        if (id.equals("menu_points")) { PanelService.sendPointsPanel(event); return; }
        if (id.equals("view_team") || id.equals("btn_team")) { PanelService.sendTeamPanel(event); return; }
        if (id.equals("view_rules")) {
            PanelService.reply(event, EmbedUtil.containerBranded("RULES", "Sector Protocols", 
                "### \uD83D\uDCDC Agency Guidelines\n" +
                "1. **Respect Node Integrity:** Maintain professional communication.\n" +
                "2. **No Data Leaks:** Confidentiality is paramount.\n" +
                "3. **Queue Discipline:** Respect the ticket processing sequence.", 
                EmbedUtil.BANNER_MAIN), true);
            return;
        }
        
        // --- AI & QUICK QUERY ---
        if (id.equals("quick_query") || id.equals("ai_query")) {
            AIService.enableAI(event.getChannel().getId());
            event.replyComponents(EmbedUtil.neuralNode("Neural logic activated. Analytical mode: **ENABLED**.\nAccessing Highcore Database... Ready for input."))
                    .useComponentsV2(true).setEphemeral(true).queue();
            return;
        }
        
        // --- TICKET STARTERS ---
        if (id.equals("order_start")) { OrderService.startWizard(event); return; }
        if (id.equals("support_start")) {
            TextInput subject = TextInput.create("subject", TextInputStyle.SHORT).setPlaceholder("Describe the technical issue...").setRequired(true).build();
            Modal m = Modal.create("modal_ticket_open", "Technical Support Entry")
                    .addComponents(Label.of("Problem Brief", subject))
                    .build();
            event.replyModal(m).queue();
            return;
        }
        if (id.equals("report_start")) {
            TextInput reason = TextInput.create("reason", TextInputStyle.PARAGRAPH).setPlaceholder("What are you reporting?").setRequired(true).build();
            Modal m = Modal.create("modal_report_open", "Complaint Registry")
                    .addComponents(Label.of("Report Context", reason))
                    .build();
            event.replyModal(m).queue();
            return;
        }

        // --- TICKET CONTROLS (STAFF ONLY) ---
        if (id.startsWith("ticket_") && !id.startsWith("ticket_type_") && !id.startsWith("ticket_modal_")) {
            boolean isStaff = event.getMember().getRoles().stream().anyMatch(r -> Config.getStaffRoles().contains(r.getId()));
            if (!isStaff) {
                PanelService.reply(event, EmbedUtil.accessDenied());
                return;
            }
            if (id.equals("ticket_claim")) { TicketService.claimTicket(event.getChannel().asTextChannel(), event.getMember()); event.deferEdit().queue(); return; }
            if (id.equals("ticket_close")) { TicketService.closeTicket(event.getChannel().asTextChannel(), event.getMember()); event.deferEdit().queue(); return; }
            if (id.equals("ticket_reopen")) { TicketService.reopenTicket(event.getChannel().asTextChannel(), event.getMember()); event.deferEdit().queue(); return; }
            if (id.equals("ticket_transcript")) { TicketService.sendTranscript(event.getChannel().asTextChannel()); event.deferEdit().queue(); return; }
            if (id.equals("ticket_delete")) {
                 event.replyComponents(EmbedUtil.warning("TERMINATION", "Sector will be decommissioned in 5 seconds.")).useComponentsV2(true).queue();
                 event.getChannel().delete().queueAfter(5, TimeUnit.SECONDS);
                 return;
            }
        }

        // --- ORDER WIZARD ---
        if (id.equals("wiz_prev")) { OrderService.handleNav(event, -1); return; }
        if (id.equals("wiz_next")) { OrderService.handleNav(event, 1); return; }
        if (id.equals("wiz_to_addons")) { OrderService.handlePhaseJump(event, "ADDONS"); return; }
        if (id.equals("wiz_finish")) { OrderService.finishWizard(event); return; }

        // --- CLOSURE STATUS ---
        if (id.startsWith("order_status_update_")) {
            String[] p = id.split("_");
            String status = p[4];
            TicketService.finalizeClose(event.getChannel().asTextChannel(), event.getMember(), status);
            PanelService.reply(event, EmbedUtil.containerBranded("REGISTRY", "Registry Updated", "Status set to **" + status + "**. Sector decommissioning initiated.", EmbedUtil.BANNER_MAIN));
            return;
        }

        // --- PAYMENT MOCKS ---
        if (id.startsWith("pay_")) {
            String type = id.split("_")[1].toUpperCase();
            String info = switch (type) {
                case "PAYPAL" -> "**Registry Email:** `billing@highcore.agency`";
                case "STRIPE" -> "**Access Hub:** `https://pay.highcore.agency/checkout`";
                case "VISA" -> "**Direct Merchant Entry:** Use terminal code `HCA-993-8B`";
                case "LOCAL" -> "**Bank Node:** Al-Rajhi \u2022 IBAN: `SA99 8000 0000 1234 5678 9012`";
                default -> "**Support:** Contact a financial advisor <@&" + Config.ROLE_STAFF + ">";
            };
            PanelService.reply(event, EmbedUtil.paymentGateway(type, info));
            return;
        }

        // --- POINTS ---
        if (id.equals("points_check")) {
            int pts = SupabaseClient.getPoints(event.getUser().getId(), event.getGuild().getId());
            PanelService.reply(event, EmbedUtil.meritAudit(pts));
            return;
        }

        // --- RATINGS ---
        if (id.startsWith("rate_")) {
            String[] parts = id.split("_");
            int stars = Integer.parseInt(parts[1]);
            String tid = parts[2];
            SupabaseClient.logRating(tid, stars, event.getUser().getName());
            PanelService.reply(event, EmbedUtil.ratingThanks(stars));
            return;
        }

        // --- GIVEAWAYS (STAFF ONLY) ---
        if (id.equals("gw_create")) {
            boolean isStaff = event.getMember().getRoles().stream().anyMatch(r -> Config.getStaffRoles().contains(r.getId()));
            if (!isStaff) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
            event.replyComponents(EmbedUtil.info("REGISTRY", "Use `/giveaway-start` to initialize a new prize distribution cycle.")).useComponentsV2(true).setEphemeral(true).queue();
            return;
        }
        if (id.equals("gw_list")) {
            event.replyComponents(EmbedUtil.info("REGISTRY", "Consult the Highcore Central terminal or use `/giveaway-list` for active telemetry.")).useComponentsV2(true).setEphemeral(true).queue();
            return;
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("ticket_type_select")) {
            String choice = event.getValues().get(0);
            switch (choice) {
                case "purchase" -> OrderService.startWizard(event);
                case "tech_support" -> {
                    Modal m = Modal.create("modal_ticket_open", "Technical Support Entry")
                            .addComponents(Label.of("Problem Brief", TextInput.create("subject", TextInputStyle.SHORT).setPlaceholder("Describe the technical issue...").setRequired(true).build()))
                            .build();
                    event.replyModal(m).queue();
                }
                case "complaint" -> {
                    Modal m = Modal.create("modal_report_open", "Complaint Registry")
                            .addComponents(Label.of("Report Context", TextInput.create("reason", TextInputStyle.PARAGRAPH).setPlaceholder("What are you reporting?").setRequired(true).build()))
                            .build();
                    event.replyModal(m).queue();
                }
                default -> PanelService.reply(event, EmbedUtil.error("NETWORK ERROR", "Selected category is currently offline or improperly designated."));
            }
        } else if (id.equals("order_wiz_cat")) {
            OrderService.handleCategory(event);
        } else if (id.startsWith("wiz_sel_")) {
            OrderService.handleMultiSelection(event);
        } else if (id.equals("menu_select")) {
            String choice = event.getValues().get(0);
            switch (choice) {
                case "menu_services" -> PanelService.sendServicesPanel(event);
                case "menu_stats" -> PanelService.sendStatsPanel(event);
                case "menu_points" -> PanelService.sendPointsPanel(event);
                case "menu_main" -> PanelService.sendMainMenu(event);
                default -> PanelService.reply(event, EmbedUtil.accessDenied());
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String id = event.getModalId();
        
        if (id.equals("modal_ticket_open")) {
            String subject = EmojiUtil.parse(event.getValue("subject").getAsString());
            TicketService.createTicket(event.getGuild(), event.getUser(), subject, "Medium", "tech_support");
            PanelService.reply(event, EmbedUtil.containerBranded("OPERATIONAL", "Node Initialized", "Technical support sector initialized. Consult your terminal link.", EmbedUtil.BANNER_MAIN));
            return;
        }

        if (id.equals("modal_report_open")) {
            String reason = EmojiUtil.parse(event.getValue("reason").getAsString());
            TicketService.createTicket(event.getGuild(), event.getUser(), "Report: " + (reason.length() > 30 ? reason.substring(0, 30) + "..." : reason), "High", "complaint");
            PanelService.reply(event, EmbedUtil.containerBranded("REGISTRY", "Registry Entry", "Your report has been logged. Enforcement will review the logs.", EmbedUtil.BANNER_MAIN));
            return;
        }

        if (id.equals("modal_order_finish")) {
            OrderService.OrderSession session = OrderService.sessions.get(event.getUser().getId());
            if (session == null) return;
            
            JsonObject orderData = new JsonObject();
            orderData.addProperty("category", session.category);
            orderData.addProperty("name", event.getValue("proj_name").getAsString());
            orderData.addProperty("link", event.getValue("proj_link").getAsString());
            orderData.addProperty("details", event.getValue("proj_details").getAsString());
            orderData.addProperty("contact", event.getValue("proj_contact").getAsString());
            orderData.addProperty("total", OrderService.calculateTotal(session));
            
            JsonArray srvs = new JsonArray(); session.selectedServices.forEach(srvs::add);
            JsonArray adds = new JsonArray(); session.selectedAddons.forEach(adds::add);
            orderData.add("services", srvs);
            orderData.add("addons", adds);
            
            TicketService.createOrderTicket(event.getGuild(), event.getUser(), "purchase", orderData.get("total").getAsInt(), session.category, orderData);
            PanelService.reply(event, EmbedUtil.containerBranded("PROJECT", "Order Initialized", "Telemetry successfully uploaded. Your custom order ticket is active.", EmbedUtil.BANNER_MAIN));
            OrderService.sessions.remove(event.getUser().getId());
            return;
        }

        if (id.equals("modal_bc")) {
            String message = EmojiUtil.parse(event.getValue("message").getAsString());
            String sessionId = "bc_" + event.getUser().getId();
            SlashCommands.BcSession sess = SlashCommands.BC_SESSIONS.get(sessionId);
            if (sess == null) { event.reply("\u274C Session expired.").setEphemeral(true).queue(); return; }
            BroadcastService.startBroadcast(event.getGuild(), message, sess.roleId, sess.attUrl);
            PanelService.reply(event, EmbedUtil.success("Broadcast Engine", "Sequential broadcast protocol initialized."));
            SlashCommands.BC_SESSIONS.remove(sessionId);
        }
    }
}
