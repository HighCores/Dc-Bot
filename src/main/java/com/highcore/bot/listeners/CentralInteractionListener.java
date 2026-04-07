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
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.util.List;

public class CentralInteractionListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("menu_main")) { PanelService.sendMainMenu(event); return; }
        if (id.equals("menu_tickets") || id.equals("btn_ticket")) { PanelService.sendTicketPanel(event); return; }
        if (id.equals("menu_services") || id.equals("view_services") || id.equals("btn_services")) { PanelService.sendServicesPanel(event); return; }
        if (id.equals("menu_points")) { PanelService.sendPointsPanel(event); return; }
        if (id.equals("menu_stats") || id.equals("startup_stats")) { PanelService.sendStatsPanel(event); return; }
        if (id.equals("view_team") || id.equals("btn_team")) { PanelService.sendTeamPanel(event); return; }
        
        if (id.equals("startup_map")) {
            PanelService.replyEphemeral(event, EmbedUtil.serverMap());
            return;
        }
        if (id.equals("startup_prices")) {
            PanelService.replyEphemeral(event, EmbedUtil.info("PRICES", "### \uD83D\uDCB0 Agency Price List\n" +
                "> **Design:** Starts at $20\n" +
                "> **Development:** Starts at $45\n" +
                "> **Media:** Starts at $30\n" +
                "> **Minecraft:** Starts at $25\n\n" +
                "*Exact costs are calculated based on your specific project needs via Ticket.*"));
            return;
        }
        if (id.equals("startup_rules")) {
            PanelService.replyEphemeral(event, EmbedUtil.rulePanel());
            return;
        }
        if (id.equals("startup_social")) {
            ActionRow links = ActionRow.of(
                Button.link("https://highcore.agency", "Website"),
                Button.link("https://twitter.com/highcoreagency", "Twitter"),
                Button.link("https://discord.gg/highcore", "Discord")
            );
            PanelService.replyEphemeral(event, EmbedUtil.socialMedia(), links);
            return;
        }
        if (id.equals("startup_colors")) {
            ActionRow row1 = ActionRow.of(
                Button.success("color_emerald", "Emerald Green"),
                Button.primary("color_ocean", "Ocean Blue"),
                Button.secondary("color_royal", "Royal Purple")
            );
            ActionRow row2 = ActionRow.of(
                Button.primary("color_golden", "Golden Yellow"),
                Button.danger("color_rose", "Rose Pink"),
                Button.secondary("color_sunset", "Sunset Orange")
            );
            PanelService.replyEphemeral(event, EmbedUtil.containerBranded("VISUALS", "Identity Selection", 
                "Select a color to update your appearance. This will toggle existing roles.", EmbedUtil.BANNER_MAIN), row1, row2);
            return;
        }

        if (id.startsWith("color_")) {
            String roleId = switch (id) {
                case "color_emerald" -> "1490844719054454845";
                case "color_ocean" -> "1490844755050135593";
                case "color_royal" -> "1490844792052158485";
                case "color_golden" -> "1490844828056223785";
                case "color_rose" -> "1490844872054472745";
                case "color_sunset" -> "1490844917051031626";
                default -> null;
            };
            if (roleId == null) return;
            net.dv8tion.jda.api.entities.Role role = event.getGuild().getRoleById(roleId);
            if (role == null) { PanelService.replyEphemeral(event, EmbedUtil.error("SECURITY", "Role ID validation failed. Node missing.")); return; }
            
            if (event.getMember().getRoles().contains(role)) {
                event.getGuild().removeRoleFromMember(event.getMember(), role).queue(v -> 
                    PanelService.replyEphemeral(event, EmbedUtil.success("COLORS", "Removed: **" + role.getName() + "**")));
            } else {
                List<String> allColors = List.of("1490844719054454845", "1490844755050135593", "1490844792052158485", "1490844828056223785", "1490844872054472745", "1490844917051031626");
                for (String cid : allColors) {
                    net.dv8tion.jda.api.entities.Role r = event.getGuild().getRoleById(cid);
                    if (r != null && event.getMember().getRoles().contains(r)) event.getGuild().removeRoleFromMember(event.getMember(), r).queue();
                }
                event.getGuild().addRoleToMember(event.getMember(), role).queue(v -> 
                    PanelService.replyEphemeral(event, EmbedUtil.success("COLORS", "Applied: **" + role.getName() + "**")));
            }
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
            TextInput subject = TextInput.create("subject", "Problem Brief", TextInputStyle.SHORT)
                    .setPlaceholder("Describe the technical issue...").setRequired(true).build();
            Modal m = Modal.create("modal_ticket_open", "Support Request")
                    .addActionRow(subject)
                    .build();
            event.replyModal(m).queue();
            return;
        }
        if (id.equals("report_start")) {
            TextInput reason = TextInput.create("reason", "Report Context", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("What are you reporting?").setRequired(true).build();
            Modal m = Modal.create("modal_report_open", "Submit a Report")
                    .addActionRow(reason)
                    .build();
            event.replyModal(m).queue();
            return;
        }

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
                 event.replyComponents(EmbedUtil.warning("CLOSING", "The channel will be deleted in 5 seconds.")).useComponentsV2(true).queue();
                 event.getChannel().delete().queueAfter(5, TimeUnit.SECONDS);
                 return;
            }
        }

        if (id.equals("wiz_prev")) { OrderService.handleNav(event, -1); return; }
        if (id.equals("wiz_next")) { OrderService.handleNav(event, 1); return; }
        if (id.equals("wiz_to_addons")) { OrderService.handlePhaseJump(event, "ADDONS"); return; }
        if (id.equals("wiz_finish")) { OrderService.finishWizard(event); return; }

        if (id.startsWith("order_status_update_")) {
            String[] p = id.split("_");
            String status = p[4];
            TicketService.finalizeClose(event.getChannel().asTextChannel(), event.getMember(), status);
            PanelService.reply(event, EmbedUtil.containerBranded("STATUS", "Status Updated", "Status set to **" + status + "**. The channel is being closed.", EmbedUtil.BANNER_MAIN));
            return;
        }

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

        if (id.equals("points_check")) {
            int pts = SupabaseClient.getPoints(event.getUser().getId(), event.getGuild().getId());
            PanelService.reply(event, EmbedUtil.meritAudit(pts));
            return;
        }

        if (id.startsWith("rate_")) {
            String[] parts = id.split("_");
            int stars = Integer.parseInt(parts[1]);
            String tid = parts[2];
            SupabaseClient.logRating(tid, stars, event.getUser().getName());
            PanelService.reply(event, EmbedUtil.ratingThanks(stars));
            return;
        }

        if (id.equals("gw_create")) {
            boolean isStaff = event.getMember().getRoles().stream().anyMatch(r -> Config.getStaffRoles().contains(r.getId()));
            if (!isStaff) { PanelService.replyEphemeral(event, EmbedUtil.accessDenied()); return; }
            event.replyComponents(EmbedUtil.info("GIVEAWAYS", "Use `/giveaway-start` to begin a new giveaway cycle.")).useComponentsV2(true).setEphemeral(true).queue();
            return;
        }
        if (id.equals("gw_list")) {
            event.replyComponents(EmbedUtil.info("GIVEAWAYS", "Check the giveaway list for active giveaways.")).useComponentsV2(true).setEphemeral(true).queue();
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
                    Modal m = Modal.create("modal_ticket_open", "Support Request")
                        .addActionRow(TextInput.create("subject", "Problem Brief", TextInputStyle.SHORT).setPlaceholder("Describe the technical issue...").setRequired(true).build())
                        .build();
                    event.replyModal(m).queue();
                }
                case "complaint" -> {
                    Modal m = Modal.create("modal_report_open", "Submit a Report")
                        .addActionRow(TextInput.create("reason", "Report Context", TextInputStyle.PARAGRAPH).setPlaceholder("What are you reporting?").setRequired(true).build())
                        .build();
                    event.replyModal(m).queue();
                }
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
                default -> PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String id = event.getModalId();
        event.deferReply(true).queue();
        
        if (id.equals("modal_ticket_open")) {
            String subject = EmojiUtil.parse(event.getValue("subject").getAsString());
            TicketService.createTicket(event.getGuild(), event.getUser(), subject, "Medium", "tech_support");
            PanelService.reply(event, EmbedUtil.containerBranded("SUPPORT", "Ticket Initialized", "Your support channel has been created successfully.", EmbedUtil.BANNER_MAIN));
            return;
        }

        if (id.equals("modal_report_open")) {
            String reason = EmojiUtil.parse(event.getValue("reason").getAsString());
            TicketService.createTicket(event.getGuild(), event.getUser(), "Report: " + (reason.length() > 30 ? reason.substring(0, 30) + "..." : reason), "High", "complaint");
            PanelService.reply(event, EmbedUtil.containerBranded("AGENCY", "Report Logged", "Your submission has been received. Enforcement will review soon.", EmbedUtil.BANNER_MAIN));
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
