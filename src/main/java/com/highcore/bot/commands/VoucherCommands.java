package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.VoucherService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.time.Duration;

public class VoucherCommands extends ListenerAdapter {
    private static final String SUPER_ROLE_ID = "1488795130034000036";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase("voucher")) return;

        if (!Config.isStaff(event.getMember())) {
            event.reply("### \u26A0\uFE0F ACCESS DENIED\nYou do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) return;

        if (subcommand.equals("create")) {
            boolean hasSuperRole = event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(SUPER_ROLE_ID));
            if (!hasSuperRole) {
                event.reply("### \u26A0\uFE0F PERMISSION RESTRICTED\nOnly specific administration roles can generate new vouchers.").setEphemeral(true).queue();
                return;
            }

            User target = event.getOption("user").getAsUser();
            String type = event.getOption("type").getAsString(); // VOUCHER or DISCOUNT
            int amount = event.getOption("amount").getAsInt();
            int expiryDays = event.getOption("expiry").getAsInt();

            if (type.equalsIgnoreCase("DISCOUNT")) {
                if (amount > 60) {
                    event.reply("### \u26A0\uFE0F VALIDATION ERROR\nMaximum discount allowed is **60%**.").setEphemeral(true).queue();
                    return;
                }
            } else if (type.equalsIgnoreCase("VOUCHER")) {
                if (amount != 50 && amount != 100) {
                    event.reply("### \u26A0\uFE0F VALIDATION ERROR\nVoucher amount must be either **50** or **100**.").setEphemeral(true).queue();
                    return;
                }
            }

            event.deferReply(true).queue();

            String expiresAt = Instant.now().plus(Duration.ofDays(expiryDays)).toString();
            String prizeDetails = (type.equalsIgnoreCase("DISCOUNT") ? amount + "% Discount" : "$" + amount + " Voucher");
            
            VoucherService.issueVoucherToChannel(target, amount, type, expiresAt, prizeDetails, event.getChannel().asTextChannel());
            
            event.getHook().sendMessage("### \u2705 SUCCESS\nSuccessfully issued " + prizeDetails + " to <@" + target.getId() + ">.").setEphemeral(true).queue();
        } else if (subcommand.equals("view")) {
            OptionMapping userOpt = event.getOption("user");
            User target = (userOpt != null) ? userOpt.getAsUser() : null;

            event.deferReply(true).queue();

            JsonArray vouchers;
            if (target != null) {
                vouchers = SupabaseClient.getUserVouchers(target.getId());
            } else {
                vouchers = SupabaseClient.getAllActiveVouchers();
            }

            if (vouchers == null || vouchers.size() == 0) {
                event.getHook().sendMessage("### \uD83D\uDCC3 REGISTRY EMPTY\nNo active vouchers found for the specified criteria.").queue();
                return;
            }

            StringBuilder sb = new StringBuilder("### \uD83C\uDFAB Active Vouchers\n");
            for (var el : vouchers) {
                JsonObject v = el.getAsJsonObject();
                if (v.has("is_used") && v.get("is_used").getAsBoolean()) continue;
                
                String code = v.get("code").getAsString();
                String userId = v.get("user_id").getAsString();
                int val = v.get("percentage").getAsInt(); // Column is percentage in DB
                String vType = v.get("type").getAsString();
                
                sb.append("▫️ `").append(code).append("` | <@").append(userId).append("> | ")
                  .append(val).append(vType.equalsIgnoreCase("PERCENT") || vType.equalsIgnoreCase("DISCOUNT") ? "%" : "$").append("\n");
            }

            event.getHook().sendMessage(sb.toString()).queue();
        }
    }
}
