package com.highcore.bot.commands;

import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.services.PanelService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

public class ModerationCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName();
        switch (name) {
            case "setnick" -> handleSetNick(event);
            case "ban" -> handleBan(event);
            case "unban" -> handleUnban(event);
            case "unban-all" -> handleUnbanAll(event);
            case "kick" -> handleKick(event);
            case "vkick" -> handleVoiceKick(event);
            case "mute-text" -> handleMuteText(event);
            case "unmute-text" -> handleUnmuteText(event);
            case "mute-check" -> handleMuteCheck(event);
            case "mute-voice" -> handleMuteVoice(event);
            case "unmute-voice" -> handleUnmuteVoice(event);
            case "timeout" -> handleTimeout(event);
            case "untimeout" -> handleUntimeout(event);
            case "clear" -> handleClear(event);
            case "move" -> handleMove(event);
            case "role" -> handleRole(event);
            case "role-multiple" -> handleRoleMultiple(event);
            case "temprole" -> handleTempRole(event);
            case "rar" -> handleRemoveAllRoles(event);
            case "inrole" -> handleInRole(event);
            case "warn-add" -> handleWarnAdd(event);
            case "warn-remove" -> handleWarnRemove(event);
            case "warnings" -> handleWarnings(event);
            case "violations" -> handleViolations(event);
            case "violations-clear" -> handleViolationsClear(event);
            case "lock" -> handleLock(event, false);
            case "unlock" -> handleLock(event, true);
            case "hide" -> handleVisibility(event, false);
            case "show" -> handleVisibility(event, true);
            case "slowmode" -> handleSlowmode(event);
            case "add-emoji" -> handleAddEmoji(event);
        }
    }

    private void handleSetNick(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.NICKNAME_MANAGE)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        String nick = event.getOption("nick", OptionMapping::getAsString);
        if (m == null) return;
        m.modifyNickname(nick).queue(v -> PanelService.reply(event, EmbedUtil.success("تحديث اللقب", "تم تغيير لقب العضو " + m.getAsMention() + " إِلى: `" + (nick == null ? "الأساسي" : nick) + "`")));
    }

    private void handleBan(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.BAN_MEMBERS)) return;
        User u = event.getOption("user", OptionMapping::getAsUser);
        String reason = getReason(event);
        if (u == null) return;
        event.getGuild().ban(u, 7, TimeUnit.DAYS).reason(reason).queue(v -> PanelService.reply(event, EmbedUtil.success("نظام الحظر", "تم استبعاد " + u.getName() + " من السيرفر.\nالسبب: " + reason)));
    }

    private void handleUnban(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.BAN_MEMBERS)) return;
        String userId = event.getOption("user_id", OptionMapping::getAsString);
        event.getGuild().unban(User.fromId(userId)).queue(v -> PanelService.reply(event, EmbedUtil.success("إلغاء الحظر", "تم فك القيود عن المعرف الرقمي: `" + userId + "`")));
    }

    private void handleUnbanAll(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.ADMINISTRATOR)) return;
        event.getGuild().retrieveBanList().queue(bans -> {
            bans.forEach(ban -> event.getGuild().unban(ban.getUser()).queue());
            PanelService.reply(event, EmbedUtil.success("تطهير قائمة الحظر", "تم فك الحظر عن **" + bans.size() + "** عضو بنجاح."));
        });
    }

    private void handleKick(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.KICK_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        String reason = getReason(event);
        if (m == null) return;
        m.kick().reason(reason).queue(v -> PanelService.reply(event, EmbedUtil.success("نظام الطرد", "تم طرد " + m.getUser().getName() + " من السيرفر.\nالسبب: " + reason)));
    }

    private void handleVoiceKick(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.KICK_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m != null && m.getVoiceState().inAudioChannel()) {
            event.getGuild().kickVoiceMember(m).queue(v -> PanelService.reply(event, EmbedUtil.success("طرد صوتي", "تم إخراج " + m.getUser().getName() + " من الاتصال الحالي.")));
        }
    }

    private void handleMuteText(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.timeoutFor(24, TimeUnit.HOURS).reason("تقييد كتابي").queue(v -> PanelService.reply(event, EmbedUtil.success("نظام الكتم", "تم تقييد " + m.getUser().getName() + " لمدة 24 ساعة.")));
    }

    private void handleUnmuteText(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.removeTimeout().queue(v -> PanelService.reply(event, EmbedUtil.success("فك التقييد", "تم استعادة صلاحيات الكتابة لـ " + m.getUser().getName())));
    }

    private void handleMuteCheck(SlashCommandInteractionEvent event) {
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        boolean muted = m.isTimedOut();
        PanelService.reply(event, EmbedUtil.containerBranded("الفحص", "حالة القيود", "العضو: " + m.getUser().getName() + "\nحالة التقييد: " + (muted ? "`نشط`" : "`سليم`"), EmbedUtil.BANNER_MAIN));
    }

    private void handleMuteVoice(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.VOICE_MUTE_OTHERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.mute(true).queue(v -> PanelService.reply(event, EmbedUtil.success("كتم صوتي", "تم كتم ميكروفون العضو " + m.getUser().getName())));
    }

    private void handleUnmuteVoice(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.VOICE_MUTE_OTHERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.mute(false).queue(v -> PanelService.reply(event, EmbedUtil.success("فك الكتم الصوتي", "تم تفعيل ميكروفون العضو " + m.getUser().getName())));
    }

    private void handleTimeout(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        int dur = event.getOption("duration", OptionMapping::getAsInt);
        if (m == null) return;
        m.timeoutFor(dur, TimeUnit.MINUTES).queue(v -> PanelService.reply(event, EmbedUtil.success("تقييد مؤقت", "تم تقييد " + m.getUser().getName() + " لمدة **" + dur + "** دقيقة.")));
    }

    private void handleUntimeout(SlashCommandInteractionEvent event) {
        handleUnmuteText(event);
    }

    private void handleClear(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MESSAGE_MANAGE)) return;
        int amt = event.getOption("amount", OptionMapping::getAsInt);
        event.getChannel().getIterableHistory().takeAsync(amt).thenAccept(msgs -> {
            event.getGuildChannel().deleteMessages(msgs).queue(v -> PanelService.replyEphemeral(event, EmbedUtil.success("تطهير القناة", "تم مسح **" + msgs.size() + "** رسالة من السجلات.")));
        });
    }

    private void handleMove(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.VOICE_MOVE_OTHERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        var chMapping = event.getOption("channel");
        if (m != null && chMapping != null) {
            net.dv8tion.jda.api.entities.channel.middleman.AudioChannel ch = chMapping.getAsChannel().asAudioChannel();
            event.getGuild().moveVoiceMember(m, ch).queue(v -> PanelService.reply(event, EmbedUtil.success("نقل العضو", "تم نقل " + m.getUser().getName() + " إلى قناة " + ch.getName())));
        }
    }

    private void handleRole(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_ROLES)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        Role r = event.getOption("role", OptionMapping::getAsRole);
        if (m == null || r == null) return;
        if (m.getRoles().contains(r)) {
            event.getGuild().removeRoleFromMember(m, r).queue(v -> PanelService.reply(event, EmbedUtil.success("نظام الرتب", "تم سحب رتبة " + r.getAsMention() + " من " + m.getUser().getName())));
        } else {
            event.getGuild().addRoleToMember(m, r).queue(v -> PanelService.reply(event, EmbedUtil.success("نظام الرتب", "تم منح رتبة " + r.getAsMention() + " لـ " + m.getUser().getName())));
        }
    }

    private void handleTempRole(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_ROLES)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        Role r = event.getOption("role", OptionMapping::getAsRole);
        int hours = event.getOption("duration", OptionMapping::getAsInt);
        if (m == null || r == null) return;
        
        String expiry = Instant.now().plus(hours, ChronoUnit.HOURS).toString();
        com.highcore.bot.database.SupabaseClient.saveTempRole(m.getId(), event.getGuild().getId(), r.getId(), expiry);
        event.getGuild().addRoleToMember(m, r).queue(v -> PanelService.reply(event, EmbedUtil.success("رتبة مؤقتة", "تم منح " + r.getAsMention() + " لـ " + m.getUser().getName() + " لمدة " + hours + " ساعة.")));
    }

    private void handleInRole(SlashCommandInteractionEvent event) {
        Role r = event.getOption("role", OptionMapping::getAsRole);
        if (r == null) return;
        event.getGuild().loadMembers().onSuccess(members -> {
            String list = members.stream().filter(m -> m.getRoles().contains(r)).map(Member::getEffectiveName).limit(20).collect(Collectors.joining(", "));
            PanelService.reply(event, EmbedUtil.containerBranded("القائمة", "أعضاء الرتبة", "رتبة: " + r.getAsMention() + "\nالأعضاء: " + (list.isEmpty() ? "لا يوجد" : list), EmbedUtil.BANNER_MAIN));
        });
    }

    private void handleWarnAdd(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        User u = event.getOption("user", OptionMapping::getAsUser);
        String reason = getReason(event);
        if (u == null) return;
        com.highcore.bot.database.SupabaseClient.addWarning(u.getId(), u.getName(), event.getUser().getId(), event.getUser().getName(), reason, event.getGuild().getId());
        PanelService.reply(event, EmbedUtil.success("تحذير جديد", "تم تسجيل تحذير لـ " + u.getName() + ".\nالسبب: " + reason));
    }

    private void handleWarnRemove(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        User u = event.getOption("user", OptionMapping::getAsUser);
        if (u == null) return;
        com.highcore.bot.database.SupabaseClient.clearUserWarnings(u.getId(), event.getGuild().getId());
        PanelService.reply(event, EmbedUtil.success("تصفية السجل", "تم حذف كافة تحذيرات " + u.getName()));
    }

    private void handleWarnings(SlashCommandInteractionEvent event) {
        User u = event.getOption("user", OptionMapping::getAsUser);
        if (u == null) return;
        com.google.gson.JsonArray warns = com.highcore.bot.database.SupabaseClient.getUserWarnings(u.getId(), event.getGuild().getId());
        int count = warns != null ? warns.size() : 0;
        PanelService.reply(event, EmbedUtil.containerBranded("السجل القضائي", "تحذيرات العضو", "العضو: " + u.getName() + "\nإجمالي التحذيرات: **" + count + "**", EmbedUtil.BANNER_MAIN));
    }

    private void handleViolations(SlashCommandInteractionEvent event) {
        User u = event.getOption("user", OptionMapping::getAsUser);
        if (u == null) return;
        com.google.gson.JsonArray vits = com.highcore.bot.database.SupabaseClient.getUserViolations(u.getId(), event.getGuild().getId());
        int count = vits != null ? vits.size() : 0;
        PanelService.reply(event, EmbedUtil.containerBranded("نظام الرقابة", "مخالفات المصطلحات", "العضو: " + u.getName() + "\nالمخالفات المسجلة: **" + count + "**", EmbedUtil.BANNER_MAIN));
    }

    private void handleViolationsClear(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.ADMINISTRATOR)) return;
        User u = event.getOption("user", OptionMapping::getAsUser);
        if (u == null) return;
        com.highcore.bot.database.SupabaseClient.clearUserViolations(u.getId(), event.getGuild().getId());
        PanelService.reply(event, EmbedUtil.success("تطهير الرقابة", "تم مسح كافة مخالفات العضو " + u.getName()));
    }

    private void handleRoleMultiple(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.ADMINISTRATOR)) return;
        Role r = event.getOption("role", OptionMapping::getAsRole);
        String action = event.getOption("action", OptionMapping::getAsString);
        if (r == null || action == null) return;
        
        event.getGuild().loadMembers().onSuccess(members -> {
            int count = 0;
            for (Member m : members) {
                if (action.equalsIgnoreCase("إضافة") && !m.getRoles().contains(r)) {
                    event.getGuild().addRoleToMember(m, r).queue();
                    count++;
                } else if (action.equalsIgnoreCase("سحب") && m.getRoles().contains(r)) {
                    event.getGuild().removeRoleFromMember(m, r).queue();
                    count++;
                }
            }
            PanelService.reply(event, EmbedUtil.success("إدارة جماعية", "الرتبة: " + r.getAsMention() + "\nالعملية: " + action + "\nالأعضاء المتأثرين: **" + count + "**"));
        });
    }

    private void handleRemoveAllRoles(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_ROLES)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.getRoles().forEach(r -> {
            if (!r.isManaged()) event.getGuild().removeRoleFromMember(m, r).queue();
        });
        PanelService.reply(event, EmbedUtil.success("تجريد الرتب", "تم سحب كافة الرتب القابلة للإزالة من " + m.getUser().getName()));
    }

    private void handleLock(SlashCommandInteractionEvent event, boolean unlock) {
        if (!hasPerm(event, Permission.MANAGE_CHANNEL)) return;
        TextChannel tc = event.getChannel().asTextChannel();
        tc.upsertPermissionOverride(event.getGuild().getPublicRole()).setAllowed(unlock ? Permission.MESSAGE_SEND : null).setDenied(unlock ? null : Permission.MESSAGE_SEND).queue(v -> 
            PanelService.reply(event, EmbedUtil.success("إدارة الأقسام", "تم " + (unlock ? "فتح" : "قفل") + " القناة بنجاح.")));
    }

    private void handleVisibility(SlashCommandInteractionEvent event, boolean show) {
        if (!hasPerm(event, Permission.MANAGE_CHANNEL)) return;
        TextChannel tc = event.getChannel().asTextChannel();
        tc.upsertPermissionOverride(event.getGuild().getPublicRole()).setAllowed(show ? Permission.VIEW_CHANNEL : null).setDenied(show ? null : Permission.VIEW_CHANNEL).queue(v -> 
            PanelService.reply(event, EmbedUtil.success("نظام الظهور", "القناة الآن " + (show ? "مرئية للجميع" : "مخفية عن الجميع"))));
    }

    private void handleSlowmode(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_CHANNEL)) return;
        int sec = event.getOption("seconds", OptionMapping::getAsInt);
        event.getChannel().asTextChannel().getManager().setSlowmode(sec).queue(v -> 
            PanelService.reply(event, EmbedUtil.success("وضع التباطؤ", "تم تحديد المهلة بـ `" + sec + " ثانية`")));
    }

    private void handleAddEmoji(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_GUILD_EXPRESSIONS)) return;
        String name = event.getOption("name", OptionMapping::getAsString);
        String url = event.getOption("url", OptionMapping::getAsString);
        try {
            net.dv8tion.jda.api.entities.Icon icon = net.dv8tion.jda.api.entities.Icon.from(new java.net.URL(url).openStream());
            event.getGuild().createEmoji(name, icon).queue(v -> PanelService.reply(event, EmbedUtil.success("تحديث الموارد", "تمت إضافة الإيموجي `" + name + "` بنجاح.")));
        } catch (Exception e) { PanelService.reply(event, EmbedUtil.error("فشل العملية", "تأكد من صحة الرابط أو صيغة الملف.")); }
    }

    private boolean hasPerm(SlashCommandInteractionEvent e, Permission p) {
        if (!e.getMember().hasPermission(p)) {
            PanelService.replyEphemeral(e, EmbedUtil.accessDenied());
            return false;
        }
        return true;
    }

    private String getReason(SlashCommandInteractionEvent e) {
        return e.getOption("reason") != null ? e.getOption("reason").getAsString() : "No reason specified";
    }
}
