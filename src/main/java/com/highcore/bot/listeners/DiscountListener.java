package com.highcore.bot.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.DiscountService;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

public class DiscountListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        
        if (id.startsWith("disc_prev_") || id.startsWith("disc_next_")) {
            String[] parts = id.split("_");
            int year = Integer.parseInt(parts[2]);
            int month = Integer.parseInt(parts[3]);
            
            LocalDate date = LocalDate.of(year, month, 1);
            if (id.startsWith("disc_prev_")) {
                date = date.minusMonths(1);
            } else {
                date = date.plusMonths(1);
            }
            
            event.deferEdit().queue();
            DiscountService.updateDiscountPanel(event, date.getYear(), date.getMonthValue());
            return;
        }

        if (id.equals("disc_deploy_manual")) {
            TextInput dateInput = TextInput.create("date", TextInputStyle.SHORT).setPlaceholder("Start: YYYY-MM-DD").setRequired(true).build();
            TextInput endInput = TextInput.create("end_date", TextInputStyle.SHORT).setPlaceholder("End: YYYY-MM-DD (Optional)").setRequired(false).build();
            TextInput percentInput = TextInput.create("percent", TextInputStyle.SHORT).setPlaceholder("e.g. 15").setRequired(true).build();
            TextInput nameInput = TextInput.create("name", TextInputStyle.SHORT).setPlaceholder("e.g. Summer Sale").setRequired(true).build();

            event.replyModal(Modal.create("modal_disc_save_MANUAL_NONE", "Deploy MANUAL Discount")
                    .addComponents(
                        net.dv8tion.jda.api.components.label.Label.of("Occasion Name", nameInput),
                        net.dv8tion.jda.api.components.label.Label.of("Start Event (YYYY-MM-DD)", dateInput),
                        net.dv8tion.jda.api.components.label.Label.of("End Event (YYYY-MM-DD - Opt)", endInput),
                        net.dv8tion.jda.api.components.label.Label.of("Discount (%)", percentInput)
                    )
                    .build()).queue();
        } else if (id.equals("disc_deploy_auto")) {
            net.dv8tion.jda.api.components.selections.StringSelectMenu menu = net.dv8tion.jda.api.components.selections.StringSelectMenu.create("sel_disc_interval")
                    .setPlaceholder("Select Repeat Interval...")
                    .addOption("Weekly Interval", "WEEKLY")
                    .addOption("Monthly Interval", "MONTHLY")
                    .addOption("Yearly Interval", "YEARLY")
                    .build();
            
            event.reply("Please select the **Recurrence Interval** for this automated deployment:")
                 .setComponents(ActionRow.of(menu))
                 .setEphemeral(true)
                 .queue();
        } else if (id.equals("disc_view_all")) {
            JsonArray all = SupabaseClient.getAllDiscounts();
            if (all == null || all.size() == 0) {
                event.reply("No scheduled discounts were found.").setEphemeral(true).queue();
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("### \uD83D\uDCCB Full Discount Schedule\n\n");
            for (var dev : all) {
                JsonObject d = dev.getAsJsonObject();
                String date = d.get("schedule_date").getAsString();
                String type = d.get("type").getAsString();
                String repeat = d.has("repeat_interval") ? d.get("repeat_interval").getAsString() : "NONE";
                int percent = d.has("percentage") ? d.get("percentage").getAsInt() : 15;
                String name = d.has("name") ? d.get("name").getAsString() : "Discount Event";

                String expiry = d.has("end_date") && !d.get("end_date").isJsonNull() ? d.get("end_date").getAsString() : "Permanent";

                sb.append("\u25AB\uFE0F **").append(date).append("** \u2014 **").append(percent).append("%** ")
                  .append("(_").append(name).append("_)\n")
                  .append("      (`").append(type).append("` | `").append(repeat).append("` | Ends: `").append(expiry).append("`)\n");
            }

            PanelService.replyEphemeral(event, EmbedUtil.containerBranded("DISCOUNT LIST", "All Active Events", sb.toString(), null, 
                    ActionRow.of(Button.primary("disc_edit_list", "Edit/Delete Discount"))));
        } else if (id.equals("disc_edit_list")) {
            JsonArray all = SupabaseClient.getAllDiscounts();
            if (all == null || all.size() == 0) {
                event.reply("No discounts available to edit.").setEphemeral(true).queue();
                return;
            }

            net.dv8tion.jda.api.components.selections.StringSelectMenu.Builder menu = net.dv8tion.jda.api.components.selections.StringSelectMenu.create("sel_disc_edit_pick")
                    .setPlaceholder("Choose a discount to manage...");
            
            for (var dev : all) {
                JsonObject d = dev.getAsJsonObject();
                String date = d.get("schedule_date").getAsString();
                int percent = d.has("percentage") ? d.get("percentage").getAsInt() : 15;
                long dbId = d.get("id").getAsLong();
                menu.addOption(date + " (" + percent + "%)", String.valueOf(dbId));
            }

            event.reply("Select the discount you want to **Modify** or **Delete**:")
                 .setComponents(ActionRow.of(menu.build()))
                 .setEphemeral(true)
                 .queue();
        } else if (id.startsWith("btn_disc_del_")) {
            long dbId = Long.parseLong(id.replace("btn_disc_del_", ""));
            SupabaseClient.deleteDiscount(dbId);
            event.reply("\u2705 Discount deleted successfully.").setEphemeral(true).queue();
            
            // Sync UI
            try {
                if (event.getMessage() != null) {
                    DiscountService.updateDiscountPanel(event, LocalDate.now().getYear(), LocalDate.now().getMonthValue());
                }
            } catch (Exception e) {}
        } else if (id.startsWith("btn_disc_mod_")) {
            long dbId = Long.parseLong(id.replace("btn_disc_mod_", ""));
            // Fetch current data (actually we can just ask for new data)
            TextInput dateInput = TextInput.create("date", TextInputStyle.SHORT).setPlaceholder("YYYY-MM-DD").setRequired(true).build();
            TextInput percentInput = TextInput.create("percent", TextInputStyle.SHORT).setPlaceholder("Percentage").setRequired(true).build();
            
            event.replyModal(Modal.create("modal_disc_edit_save_" + dbId, "Modify Discount")
                    .addComponents(
                        net.dv8tion.jda.api.components.label.Label.of("New Date", dateInput),
                        net.dv8tion.jda.api.components.label.Label.of("New Percentage", percentInput)
                    )
                    .build()).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (id.equals("sel_disc_interval")) {
            // ... (existing auto deploy logic)
            String interval = event.getValues().get(0);
            TextInput nameInput = TextInput.create("name", TextInputStyle.SHORT).setPlaceholder("Holiday Name").setRequired(true).build();
            
            String datePlaceholder = interval.equals("YEARLY") ? "MM-DD (e.g. 11-20)" : "YYYY-MM-DD";
            String endPlaceholder = interval.equals("YEARLY") ? "MM-DD (e.g. 11-30)" : "YYYY-MM-DD (Optional)";
            TextInput dateInput = TextInput.create("date", TextInputStyle.SHORT).setPlaceholder(datePlaceholder).setRequired(true).build();
            TextInput endInput = TextInput.create("end_date", TextInputStyle.SHORT).setPlaceholder(endPlaceholder).setRequired(false).build();
            TextInput percentInput = TextInput.create("percent", TextInputStyle.SHORT).setPlaceholder("e.g. 15").setRequired(true).build();
            
            event.replyModal(Modal.create("modal_disc_save_AUTO_" + interval, "Configure AUTO Discount")
                    .addComponents(
                        net.dv8tion.jda.api.components.label.Label.of("Occasion Name", nameInput),
                        net.dv8tion.jda.api.components.label.Label.of(interval.equals("YEARLY") ? "Start Event (MM-DD)" : "Start Event", dateInput),
                        net.dv8tion.jda.api.components.label.Label.of(interval.equals("YEARLY") ? "End Event (MM-DD)" : "End Event", endInput),
                        net.dv8tion.jda.api.components.label.Label.of("Percentage", percentInput)
                    ).build()).queue();
        } else if (id.equals("sel_disc_edit_pick")) {
            String dbIdStr = event.getValues().get(0);
            event.reply("How would you like to proceed with this discount?")
                 .setComponents(ActionRow.of(
                     Button.success("btn_disc_mod_" + dbIdStr, "Modify Details"),
                     Button.danger("btn_disc_del_" + dbIdStr, "Delete Forever")
                 ))
                 .setEphemeral(true)
                 .queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String id = event.getModalId();
        if (id.startsWith("modal_disc_save_")) {
            // ... (existing creation logic)
            String[] parts = id.split("_");
            String type = parts[3];
            String repeat = parts[4];
            String name = event.getValue("name").getAsString().trim();
            String dateRaw = event.getValue("date").getAsString().trim();
            String endRaw = event.getValue("end_date") != null ? event.getValue("end_date").getAsString().trim() : "";
            String percentStr = event.getValue("percent").getAsString();

            // Handling Yearly MM-DD format
            if (repeat.equals("YEARLY")) {
                if (dateRaw.matches("\\d{1,2}-\\d{1,2}")) dateRaw = LocalDate.now().getYear() + "-" + dateRaw;
                if (!endRaw.isEmpty() && endRaw.matches("\\d{1,2}-\\d{1,2}")) endRaw = LocalDate.now().getYear() + "-" + endRaw;
            }

            if (!dateRaw.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) { event.reply("Invalid Start Event format. Use YYYY-MM-DD (or MM-DD for Yearly).").setEphemeral(true).queue(); return; }
            
            String date; try { String[] dp = dateRaw.split("-"); date = String.format("%s-%02d-%02d", dp[0], Integer.parseInt(dp[1]), Integer.parseInt(dp[2])); } catch (Exception e) { event.reply("Error parsing Start Event.").setEphemeral(true).queue(); return; }
            
            String endDate = null;
            if (!endRaw.isEmpty()) {
                if (!endRaw.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) { event.reply("Invalid End Event format. Use YYYY-MM-DD (or MM-DD for Yearly).").setEphemeral(true).queue(); return; }
                try { String[] dp = endRaw.split("-"); endDate = String.format("%s-%02d-%02d", dp[0], Integer.parseInt(dp[1]), Integer.parseInt(dp[2])); } catch (Exception e) { event.reply("Error parsing End Event.").setEphemeral(true).queue(); return; }
            }

            int percent; try { percent = Integer.parseInt(percentStr.replace("%", "").trim()); } catch (Exception e) { event.reply("Invalid percentage.").setEphemeral(true).queue(); return; }

            SupabaseClient.createDiscount(type, date, repeat, percent, name, endDate);
            try { if (event.getMessage() != null) DiscountService.updateDiscountPanel(event, LocalDate.now().getYear(), LocalDate.now().getMonthValue()); } catch (Exception e) {}
            event.reply("### \u2705 Success\nDiscount **" + name + "** scheduled from **" + date + "** to **" + (endDate != null ? endDate : "End of Day") + "**.").setEphemeral(true).queue();
        } else if (id.startsWith("modal_disc_edit_save_")) {
            long dbId = Long.parseLong(id.replace("modal_disc_edit_save_", ""));
            String dateRaw = event.getValue("date").getAsString().trim();
            String percentStr = event.getValue("percent").getAsString();

            if (!dateRaw.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) { event.reply("Invalid date format.").setEphemeral(true).queue(); return; }
            String date; try { String[] dp = dateRaw.split("-"); date = String.format("%s-%02d-%02d", dp[0], Integer.parseInt(dp[1]), Integer.parseInt(dp[2])); } catch (Exception e) { event.reply("Error.").setEphemeral(true).queue(); return; }
            int percent; try { percent = Integer.parseInt(percentStr.replace("%", "").trim()); } catch (Exception e) { event.reply("Invalid percentage.").setEphemeral(true).queue(); return; }

            // I need a patch/update method in SupabaseClient
            JsonObject body = new JsonObject();
            body.addProperty("schedule_date", date);
            body.addProperty("percentage", percent);
            SupabaseClient.patch("dc_discounts", "id=eq." + dbId, body);

            try { if (event.getMessage() != null) DiscountService.updateDiscountPanel(event, LocalDate.now().getYear(), LocalDate.now().getMonthValue()); } catch (Exception e) {}
            event.reply("### \u2705 Updated\nDiscount details updated for **" + date + "**.").setEphemeral(true).queue();
        }
    }
}
