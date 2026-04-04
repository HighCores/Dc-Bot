package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;

public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    // Overload for backward compat
    public static TextChannel createTicket(Guild guild, User user, String subject, String priority) {
        return createTicket(guild, user, subject, priority, "general");
    }

    public static TextChannel createTicket(Guild guild, User user, String subject, String priority, String type) {
        int num = SupabaseClient.getNextTicketNumber();
        String ticketId = String.format("HC-%03d", num);
        Category cat = guild.getCategoryById(Config.TICKET_CATEGORY_ID);
        if (cat == null) return null;
        Member member = guild.getMember(user);
        if (member == null) return null;

        TextChannel channel = cat.createTextChannel("ticket-" + ticketId.toLowerCase())
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_HISTORY), null)
                .setTopic("\uD83C\uDFAB " + ticketId + " | " + getTypeEmoji(type) + " " + getTypeName(type) + " | \uD83D\uDC64 " + user.getName())
                .complete();

        for (String roleId : Config.getStaffRoles()) {
            if (roleId != null) try { var r = guild.getRoleById(roleId); if (r != null)
                channel.upsertPermissionOverride(r).grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ATTACH_FILES).queue();
            } catch (Exception e) {}
        }

        SupabaseClient.createTicket(ticketId, user.getId(), user.getName(), channel.getId(), subject, priority);
        SupabaseClient.logStat("ticket_created", user.getId(), ticketId);

        channel.sendMessageEmbeds(getWelcomeEmbed(ticketId, user.getName(), type))
                .setComponents(net.dv8tion.jda.api.interactions.components.ActionRow.of(getTicketButtons("open"))).queue();
        channel.sendMessage(user.getAsMention() + " <@&" + Config.ROLE_STAFF + ">").queue();
        return channel;
    }

    private static String getTypeEmoji(String type) {
        return switch (type) { case "tech_support" -> "\uD83D\uDD27"; case "inquiry" -> "\u2753"; case "purchase" -> "\uD83D\uDED2";
            case "order_status" -> "\uD83D\uDCE6"; case "apply_management" -> "\uD83D\uDC54"; case "apply_team" -> "\uD83D\uDC65"; default -> "\uD83C\uDFAB"; };
    }

    private static String getTypeName(String type) {
        return switch (type) { case "tech_support" -> "Tech Support"; case "inquiry" -> "Inquiry"; case "purchase" -> "Purchase";
            case "order_status" -> "Order Status"; case "apply_management" -> "Management App"; case "apply_team" -> "Team App"; default -> "General"; };
    }

    private static net.dv8tion.jda.api.entities.MessageEmbed getWelcomeEmbed(String ticketId, String userName, String type) {
        String body = switch (type) {
            case "tech_support" -> """
                    > Welcome **%s**! \uD83D\uDC4B
                    > You've opened a **Technical Support** ticket.

                    \uD83D\uDD27 Please provide:
                    > \u25B8 What issue are you experiencing?
                    > \u25B8 When did it start?
                    > \u25B8 Any screenshots or error messages?

                    Our tech team will assist you shortly!
                    """.formatted(userName);
            case "inquiry" -> """
                    > Welcome **%s**! \uD83D\uDC4B
                    > You've opened a **General Inquiry** ticket.

                    \u2753 Feel free to ask any question about:
                    > \u25B8 Our services and capabilities
                    > \u25B8 Pricing and packages
                    > \u25B8 Timelines and availability

                    We'll get back to you ASAP!
                    """.formatted(userName);
            case "purchase" -> """
                    > Welcome **%s**! \uD83D\uDC4B
                    > You've opened a **Purchase Service** ticket.

                    \uD83D\uDED2 Please let us know:
                    > \u25B8 Which service are you interested in?
                    > \u25B8 Any specific requirements?
                    > \u25B8 Your budget range (optional)

                    Our sales team will prepare a quote for you!
                    """.formatted(userName);
            case "order_status" -> """
                    > Welcome **%s**! \uD83D\uDC4B
                    > You've opened an **Order Status** ticket.

                    \uD83D\uDCE6 Please provide:
                    > \u25B8 Your order number (if you have one)
                    > \u25B8 What service did you order?
                    > \u25B8 Approximate date of order

                    We'll check the status for you right away!
                    """.formatted(userName);
            case "apply_management" -> """
                    > Welcome **%s**! \uD83D\uDC4B
                    > You've opened a **Management Application** ticket.

                    \uD83D\uDC54 Please provide:
                    > \u25B8 Your name and age
                    > \u25B8 Previous experience in management
                    > \u25B8 Why you want to join Highcore management
                    > \u25B8 Your availability (hours/week)

                    Our leadership team will review your application!
                    """.formatted(userName);
            case "apply_team" -> """
                    > Welcome **%s**! \uD83D\uDC4B
                    > You've opened a **Highcore Team Application** ticket.

                    \uD83D\uDC65 Please provide:
                    > \u25B8 Your skills (development, design, marketing, etc.)
                    > \u25B8 Portfolio or examples of your work
                    > \u25B8 Why you want to join Highcore
                    > \u25B8 Your availability

                    We're excited to hear from you!
                    """.formatted(userName);
            default -> """
                    > Welcome **%s**! \uD83D\uDC4B
                    > Thank you for reaching out to **Highcore Agency**

                    \uD83D\uDCCC Please describe your request in detail.
                    """.formatted(userName);
        };

        return EmbedUtil.branded().setColor(EmbedUtil.PRIMARY)
                .setDescription("## " + getTypeEmoji(type) + " Ticket #" + ticketId + " \u2014 " + getTypeName(type) + "\n\n" + body).build();
    }

    public static void claimTicket(TextChannel channel, Member claimer) {
        JsonObject t = SupabaseClient.getTicketByChannel(channel.getId()); if (t == null) return;
        String id = t.get("ticket_id").getAsString();
        SupabaseClient.claimTicket(id, claimer.getUser().getName());
        SupabaseClient.logStat("ticket_claimed", claimer.getId(), id);
        // Auto-points for team members
        if (PointsService.isTeamMember(claimer)) {
            PointsService.awardTicketPoints(claimer.getId(), channel.getGuild().getId(), 5, "Ticket claimed: " + id);
        }
        channel.sendMessageEmbeds(EmbedUtil.ticketClaimed(id, claimer.getUser().getName()))
                .setComponents(net.dv8tion.jda.api.interactions.components.ActionRow.of(getTicketButtons("claimed"))).queue();
        logTicketAction(channel.getGuild(), id, "Claimed", claimer.getUser());
    }

    public static void closeTicket(TextChannel channel, Member closer) {
        JsonObject t = SupabaseClient.getTicketByChannel(channel.getId()); if (t == null) return;
        String id = t.get("ticket_id").getAsString();
        SupabaseClient.updateTicketStatus(id, "closed", closer.getUser().getName());
        SupabaseClient.logStat("ticket_closed", closer.getId(), id);
        // Auto-points for team members
        if (PointsService.isTeamMember(closer)) {
            PointsService.awardTicketPoints(closer.getId(), channel.getGuild().getId(), 10, "Ticket closed: " + id);
        }
        channel.sendMessageEmbeds(EmbedUtil.ticketClosed(id, closer.getUser().getName()))
                .setComponents(net.dv8tion.jda.api.interactions.components.ActionRow.of(getTicketButtons("closed"))).queue();
        String userId = t.get("user_id").getAsString();
        Member owner = channel.getGuild().getMemberById(userId);
        if (owner != null) {
            channel.upsertPermissionOverride(owner).deny(Permission.VIEW_CHANNEL).queue();
            owner.getUser().openPrivateChannel().queue(dm -> dm.sendMessageEmbeds(EmbedUtil.ratingRequest(id))
                    .setComponents(net.dv8tion.jda.api.interactions.components.ActionRow.of(
                            Button.success("rate_5_"+id,"\u2B50\u2B50\u2B50\u2B50\u2B50"), Button.primary("rate_4_"+id,"\u2B50\u2B50\u2B50\u2B50"),
                            Button.primary("rate_3_"+id,"\u2B50\u2B50\u2B50"), Button.secondary("rate_2_"+id,"\u2B50\u2B50"), Button.danger("rate_1_"+id,"\u2B50")
                    )).queue(null, e->{}), e->{});
        }
        logTicketAction(channel.getGuild(), id, "Closed", closer.getUser());
    }

    public static void reopenTicket(TextChannel channel, Member reopener) {
        JsonObject t = SupabaseClient.getTicketByChannel(channel.getId()); if (t == null) return;
        String id = t.get("ticket_id").getAsString();
        SupabaseClient.updateTicketStatus(id, "reopened", null);
        SupabaseClient.logStat("ticket_reopened", reopener.getId(), id);
        channel.sendMessageEmbeds(EmbedUtil.ticketReopened(id, reopener.getUser().getName()))
                .setComponents(net.dv8tion.jda.api.interactions.components.ActionRow.of(getTicketButtons("open"))).queue();
        String userId = t.get("user_id").getAsString();
        Member owner = channel.getGuild().getMemberById(userId);
        if (owner != null) channel.upsertPermissionOverride(owner)
                .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ATTACH_FILES).queue();
        logTicketAction(channel.getGuild(), id, "Reopened", reopener.getUser());
    }

    private static void logTicketAction(Guild guild, String ticketId, String action, User by) {
        TextChannel logCh = LogManager.get(guild, Config.LOG_TICKETS);
        if (logCh == null) return;
        logCh.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setAuthor(by.getName(), null, by.getEffectiveAvatarUrl())
                .setDescription("### \uD83C\uDFAB Ticket " + action)
                .addField("Ticket", "`" + ticketId + "`", true)
                .addField("By", by.getAsMention(), true)
                .addField("\uD83D\uDD52 Time", DateTimeFormatter.ofPattern("EEE, MMM dd yyyy \u2022 hh:mm:ss a")
                        .withZone(ZoneId.of("Asia/Riyadh")).format(Instant.now()), false).build()).queue();
    }

    public static void sendTranscript(TextChannel ticketChannel) {
        JsonObject t = SupabaseClient.getTicketByChannel(ticketChannel.getId());
        if (t == null) { ticketChannel.sendMessage("Ticket not found.").queue(); return; }
        String id = t.get("ticket_id").getAsString();
        String un = t.has("user_name") ? t.get("user_name").getAsString() : "Unknown";
        String sub = t.has("subject") ? t.get("subject").getAsString() : "N/A";
        String st = t.has("status") ? t.get("status").getAsString() : "N/A";
        String ca = t.has("created_at") ? t.get("created_at").getAsString() : "";
        JsonArray msgs = SupabaseClient.getTicketMessages(id);
        int mc = msgs != null ? msgs.size() : 0;
        String html = genHtml(id, un, sub, st, ca, msgs);
        TextChannel tc = ticketChannel.getGuild().getTextChannelById(Config.TRANSCRIPT_CHANNEL_ID);
        if (tc == null) { ticketChannel.sendMessage("Transcript channel not found.").queue(); return; }
        tc.sendMessageEmbeds(EmbedUtil.transcriptLog(id, un, sub, st, mc))
                .addFiles(FileUpload.fromData(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)), "transcript-"+id.toLowerCase()+".html"))
                .queue(m -> ticketChannel.sendMessageEmbeds(EmbedUtil.success("Transcript", "Sent to "+tc.getAsMention())).queue(), e -> ticketChannel.sendMessage("Failed.").queue());

        logTicketAction(ticketChannel.getGuild(), id, "Transcript Generated", ticketChannel.getJDA().getSelfUser());
    }

    private static String genHtml(String id, String un, String sub, String st, String ca, JsonArray msgs) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Riyadh"));
        String cd = ""; if (ca != null && !ca.isEmpty()) try { cd = f.format(Instant.parse(ca)); } catch (Exception e) { cd = ca; }
        String sc = switch(st){case "open"->"badge-open";case "claimed"->"badge-claimed";case "closed"->"badge-closed";default->"badge-open";};
        String sn = switch(st){case "open"->"Open";case "claimed"->"Claimed";case "closed"->"Closed";case "reopened"->"Reopened";default->"Unknown";};
        StringBuilder mb = new StringBuilder();
        if (msgs != null && msgs.size() > 0) { for (var e : msgs) { JsonObject m = e.getAsJsonObject();
            String u=esc(m.has("user_name")?m.get("user_name").getAsString():"?"), c=esc(m.has("content")?m.get("content").getAsString():"");
            String ts=m.has("created_at")?m.get("created_at").getAsString():""; String t=""; if(!ts.isEmpty()) try{t=f.format(Instant.parse(ts));}catch(Exception x){t=ts;}
            mb.append("<div class=message><div class=msg-header><span class=msg-author>").append(u).append("</span><span class=msg-time>").append(t).append("</span></div><div class=msg-content>").append(c).append("</div></div>");
        }} else mb.append("<div class=no-messages>No messages.</div>");
        return "<!DOCTYPE html><html><head><meta charset=UTF-8><title>Transcript "+esc(id)+"</title><style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:Segoe UI,sans-serif;background:#1a1a2e;color:#e0e0e0}.header{background:linear-gradient(135deg,#16213e,#0f3460,#533483);padding:40px 30px;text-align:center;border-bottom:3px solid #e94560}.header h1{font-size:28px;color:#fff;margin-bottom:8px}.header .brand{color:#e94560}.info-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:16px;padding:30px;max-width:900px;margin:0 auto}.info-card{background:#16213e;border-radius:12px;padding:16px 20px}.info-card .label{font-size:11px;color:#888;text-transform:uppercase;letter-spacing:1px;margin-bottom:6px}.info-card .value{font-size:16px;font-weight:600;color:#fff}.messages-container{max-width:900px;margin:0 auto;padding:0 30px 40px}.messages-title{font-size:18px;color:#e94560;margin-bottom:20px;padding-bottom:10px;border-bottom:1px solid #2a2a4a}.message{background:#16213e;border-radius:10px;padding:14px 18px;margin-bottom:10px;border-left:3px solid #533483}.message:hover{background:#1a2744}.msg-header{display:flex;justify-content:space-between;margin-bottom:8px}.msg-author{font-weight:700;color:#e94560;font-size:14px}.msg-time{font-size:11px;color:#666}.msg-content{color:#d0d0d8;font-size:14px;line-height:1.7;white-space:pre-wrap}.footer{text-align:center;padding:24px;color:#555;font-size:12px;border-top:1px solid #2a2a4a}.no-messages{text-align:center;padding:40px;color:#666}.badge{display:inline-block;padding:4px 12px;border-radius:20px;font-size:12px;font-weight:600}.badge-open{background:#1a4731;color:#57F287}.badge-claimed{background:#4a3f1a;color:#FEE75C}.badge-closed{background:#4a1a1a;color:#ED4245}</style></head><body><div class=header><h1>\uD83D\uDCDC Transcript #"+esc(id)+"</h1><div>Generated by <span class=brand>Highcore Agency Bot</span></div></div><div class=info-grid><div class=info-card><div class=label>Ticket</div><div class=value>"+esc(id)+"</div></div><div class=info-card><div class=label>Client</div><div class=value>"+esc(un)+"</div></div><div class=info-card><div class=label>Subject</div><div class=value>"+esc(sub)+"</div></div><div class=info-card><div class=label>Status</div><div class=value><span class='badge "+sc+"'>"+sn+"</span></div></div><div class=info-card><div class=label>Created</div><div class=value>"+esc(cd)+"</div></div><div class=info-card><div class=label>Messages</div><div class=value>"+(msgs!=null?msgs.size():0)+"</div></div></div><div class=messages-container><div class=messages-title>\uD83D\uDCAC Messages</div>"+mb+"</div><div class=footer>Highcore Agency \u00A9 2025 \u2014 <span class=brand>IMY</span></div></body></html>";
    }
    private static String esc(String t){return t==null?"":t.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}

    public static List<Button> getTicketButtons(String s) {
        return switch(s) {
            case "open" -> List.of(Button.success("ticket_claim","\u270B Claim"),Button.danger("ticket_close","\uD83D\uDD12 Close"),Button.secondary("ticket_transcript","\uD83D\uDCDC Transcript"));
            case "claimed" -> List.of(Button.danger("ticket_close","\uD83D\uDD12 Close"),Button.secondary("ticket_transcript","\uD83D\uDCDC Transcript"));
            case "closed" -> List.of(Button.success("ticket_reopen","\uD83D\uDD13 Reopen"),Button.secondary("ticket_transcript","\uD83D\uDCDC Transcript"),Button.danger("ticket_delete","\uD83D\uDDD1 Delete"));
            default -> List.of(Button.success("ticket_claim","\u270B Claim"),Button.danger("ticket_close","\uD83D\uDD12 Close"));
        };
    }
}
