package com.highcore.bot.listeners;

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
            TextInput dateInput = TextInput.create("date", TextInputStyle.SHORT)
                    .setPlaceholder("e.g. 2026-04-25")
                    .setRequired(true)
                    .build();

            TextInput percentInput = TextInput.create("percent", TextInputStyle.SHORT)
                    .setPlaceholder("e.g. 15")
                    .setRequired(true)
                    .build();

            event.replyModal(Modal.create("modal_disc_save_MANUAL_NONE", "Deploy MANUAL Discount")
                    .addComponents(
                        net.dv8tion.jda.api.components.label.Label.of("Date (YYYY-MM-DD)", dateInput),
                        net.dv8tion.jda.api.components.label.Label.of("Discount Percentage (%)", percentInput)
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
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (id.equals("sel_disc_interval")) {
            String interval = event.getValues().get(0);
            
            TextInput dateInput = TextInput.create("date", TextInputStyle.SHORT)
                    .setPlaceholder("e.g. 2026-04-25")
                    .setRequired(true)
                    .build();

            TextInput percentInput = TextInput.create("percent", TextInputStyle.SHORT)
                    .setPlaceholder("e.g. 15")
                    .setRequired(true)
                    .build();

            event.replyModal(Modal.create("modal_disc_save_AUTO_" + interval, "Configure AUTO Discount")
                    .addComponents(
                        net.dv8tion.jda.api.components.label.Label.of("Start Date (YYYY-MM-DD)", dateInput),
                        net.dv8tion.jda.api.components.label.Label.of("Discount Percentage (%)", percentInput)
                    )
                    .build()).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String id = event.getModalId();
        if (id.startsWith("modal_disc_save_")) {
            String[] parts = id.split("_");
            String type = parts[3];
            String repeat = parts[4];
            String dateRaw = event.getValue("date").getAsString().trim();
            String percentStr = event.getValue("percent").getAsString();

            // Flexible validation: accepts 2026-4-2 or 2026-04-02
            if (!dateRaw.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                event.reply("Invalid date format. Use YYYY-MM-DD (e.g. 2026-04-25).").setEphemeral(true).queue();
                return;
            }

            // Standardize format to YYYY-MM-DD (auto-pad zeros)
            String date;
            try {
                String[] dateParts = dateRaw.split("-");
                date = String.format("%s-%02d-%02d", dateParts[0], Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[2]));
            } catch (Exception e) {
                event.reply("Internal parsing error. Check date format.").setEphemeral(true).queue();
                return;
            }

            int percent;
            try {
                percent = Integer.parseInt(percentStr.replace("%", "").trim());
            } catch (Exception e) {
                event.reply("Invalid percentage value. Use a number (e.g. 15).").setEphemeral(true).queue();
                return;
            }

            SupabaseClient.createDiscount(type, date, repeat, percent);
            
            event.reply("### \u2705 Deployment Successful\n" + type + " Discount of **" + percent + "%** scheduled for **" + date + "**.")
                 .setEphemeral(true)
                 .queue();
        }
    }
}
