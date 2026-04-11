package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import net.dv8tion.jda.api.entities.Message;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TranscriptService {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("MMM dd, yyyy 'at' hh:mm a")
            .withZone(ZoneId.of("Asia/Riyadh"));

    // Generate from Discord Message objects (fetched directly from channel history)
    public static byte[] generateFromMessages(String ticketId, String channelName, String type, String status,
                                              String openedAt, String closedBy, List<Message> messages) {
        JsonArray arr = new JsonArray();
        for (Message m : messages) {
            JsonObject obj = new JsonObject();
            obj.addProperty("user_id", m.getAuthor().getId());
            String authorName = m.getMember() != null
                    ? m.getMember().getEffectiveName()
                    : m.getAuthor().getName();
            obj.addProperty("user_name", authorName);
            String content = m.getContentDisplay();
            if (content.isBlank() && !m.getAttachments().isEmpty()) {
                content = "[" + m.getAttachments().size() + " attachment(s)]";
            }
            obj.addProperty("content", content);
            obj.addProperty("created_at", m.getTimeCreated().toInstant().toString());
            arr.add(obj);
        }
        return buildHtml(ticketId, channelName, type, status, openedAt, closedBy, arr);
    }

    private static byte[] buildHtml(String ticketId, String channelName, String type, String status,
                                    String openedAt, String closedBy, JsonArray messages) {

        StringBuilder msgs = new StringBuilder();
        int count = 0;
        String prevUser = null;

        if (messages != null) {
            for (var el : messages) {
                JsonObject m = el.getAsJsonObject();
                String userId   = m.has("user_id")   ? m.get("user_id").getAsString()   : "0";
                String userName = m.has("user_name")  ? m.get("user_name").getAsString() : "Unknown";
                String content  = m.has("content")    ? htmlEscape(m.get("content").getAsString()) : "";
                String ts       = "—";
                if (m.has("created_at") && !m.get("created_at").isJsonNull()) {
                    try { ts = FMT.format(Instant.parse(m.get("created_at").getAsString())); }
                    catch (Exception ignored) {}
                }
                content = content.replace("\n", "<br>");

                boolean newGroup = !userId.equals(prevUser);
                prevUser = userId;
                count++;

                String avatarLetter = userName.isEmpty() ? "?" : String.valueOf(userName.charAt(0)).toUpperCase();
                String avatarColor  = pickColor(userId);

                if (newGroup) {
                    msgs.append("<div class=\"msg-group\">")
                        .append("<div class=\"avatar\" style=\"background:").append(avatarColor).append("\">")
                        .append(avatarLetter).append("</div>")
                        .append("<div class=\"msg-body\">")
                        .append("<div class=\"msg-header\">")
                        .append("<span class=\"username\">").append(htmlEscape(userName)).append("</span>")
                        .append("<span class=\"ts\">").append(ts).append("</span>")
                        .append("</div>");
                } else {
                    // continuation message — no header
                    msgs.append("<div class=\"msg-group msg-cont\">");
                    msgs.append("<div class=\"avatar-spacer\"></div>");
                    msgs.append("<div class=\"msg-body\">");
                }

                msgs.append("<div class=\"msg-text\">").append(content).append("</div>");
                msgs.append("</div></div>"); // msg-body + msg-group
            }
        }

        if (count == 0) {
            msgs.append("<div class=\"empty\">No messages recorded in this ticket.</div>");
        }

        // Format openedAt nicely if it looks like ISO
        String openedFmt = openedAt;
        try { openedFmt = FMT.format(Instant.parse(openedAt)); } catch (Exception ignored) {}

        String html = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Transcript — #""" + htmlEscape(channelName) + """
</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: #313338;
      color: #dcddde;
      font-family: "gg sans", "Noto Sans", Whitney, "Helvetica Neue", Helvetica, Arial, sans-serif;
      font-size: 15px;
      line-height: 1.375;
    }

    /* ── Top bar ── */
    .topbar {
      background: #1e1f22;
      border-bottom: 1px solid #1a1b1e;
      padding: 14px 28px;
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .topbar-logo {
      font-size: 22px;
      font-weight: 800;
      letter-spacing: -0.5px;
      background: linear-gradient(135deg, #C5A059, #FFD700);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .topbar-title { font-size: 15px; color: #b5bac1; }
    .channel-name {
      font-size: 16px;
      font-weight: 700;
      color: #fff;
      margin-left: auto;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .channel-name::before { content: "#"; color: #72767d; }

    /* ── Info cards ── */
    .info-bar {
      background: #2b2d31;
      border-bottom: 1px solid #232428;
      padding: 12px 28px;
      display: flex;
      flex-wrap: wrap;
      gap: 24px;
    }
    .info-card { display: flex; flex-direction: column; gap: 2px; }
    .info-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.8px; color: #72767d; }
    .info-value { font-size: 13px; color: #dbdee1; font-weight: 500; }
    .badge {
      display: inline-block;
      padding: 1px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
    }
    .badge-support  { background: #5865f2; color: #fff; }
    .badge-order    { background: #3ba55c; color: #fff; }
    .badge-complaint{ background: #ed4245; color: #fff; }
    .badge-open     { background: #3ba55c; color: #fff; }
    .badge-closed   { background: #747f8d; color: #fff; }
    .badge-claimed  { background: #faa81a; color: #000; }

    /* ── Messages ── */
    .messages {
      padding: 20px 0 40px;
      max-width: 900px;
      margin: 0 auto;
    }
    .date-divider {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 28px 8px;
      font-size: 12px;
      font-weight: 600;
      color: #72767d;
    }
    .date-divider::before, .date-divider::after {
      content: "";
      flex: 1;
      height: 1px;
      background: #3f4147;
    }

    .msg-group {
      display: flex;
      gap: 16px;
      padding: 2px 28px;
      transition: background 0.1s;
    }
    .msg-group:hover { background: #2e3035; }
    .msg-group.msg-cont { padding-top: 1px; padding-bottom: 1px; }
    .msg-group:not(.msg-cont) { padding-top: 10px; }

    .avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      font-weight: 700;
      font-size: 17px;
      color: #fff;
      margin-top: 2px;
    }
    .avatar-spacer { width: 40px; flex-shrink: 0; }

    .msg-body { flex: 1; min-width: 0; }
    .msg-header { display: flex; align-items: baseline; gap: 8px; margin-bottom: 2px; }
    .username { color: #f2f3f5; font-weight: 600; font-size: 15px; cursor: default; }
    .username:hover { text-decoration: underline; }
    .ts { color: #72767d; font-size: 11px; font-weight: 400; }
    .msg-text { color: #dcddde; word-break: break-word; }

    .empty { padding: 60px 28px; text-align: center; color: #72767d; font-size: 15px; }

    /* ── Footer ── */
    .footer {
      background: #1e1f22;
      border-top: 1px solid #1a1b1e;
      padding: 14px 28px;
      text-align: center;
      font-size: 12px;
      color: #4f5660;
    }
    .footer a { color: #C5A059; text-decoration: none; }
    .footer span { color: #72767d; }
  </style>
</head>
<body>

  <div class="topbar">
    <span class="topbar-logo">HIGH CORE</span>
    <span class="topbar-title">Support Transcript</span>
    <span class="channel-name">""" + htmlEscape(channelName) + """
</span>
  </div>

  <div class="info-bar">
    <div class="info-card">
      <span class="info-label">Ticket ID</span>
      <span class="info-value">""" + htmlEscape(ticketId) + """
</span>
    </div>
    <div class="info-card">
      <span class="info-label">Type</span>
      <span class="info-value"><span class="badge """ + typeBadgeClass(type) + """
">""" + htmlEscape(type) + """
</span></span>
    </div>
    <div class="info-card">
      <span class="info-label">Status</span>
      <span class="info-value"><span class="badge """ + statusBadgeClass(status) + """
">""" + htmlEscape(status) + """
</span></span>
    </div>
    <div class="info-card">
      <span class="info-label">Opened</span>
      <span class="info-value">""" + htmlEscape(openedFmt) + """
</span>
    </div>
    <div class="info-card">
      <span class="info-label">Closed By</span>
      <span class="info-value">""" + htmlEscape(closedBy) + """
</span>
    </div>
    <div class="info-card">
      <span class="info-label">Messages</span>
      <span class="info-value">""" + count + """
</span>
    </div>
  </div>

  <div class="messages">
    <div class="date-divider">Conversation History</div>
""" + msgs + """
  </div>

  <div class="footer">
    Generated by <strong>High Core Agency</strong> &nbsp;·&nbsp;
    <span>""" + FMT.format(Instant.now()) + """
 (Asia/Riyadh)</span>
  </div>

</body>
</html>
""";
        return html.getBytes(StandardCharsets.UTF_8);
    }

    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String typeBadgeClass(String type) {
        if (type == null) return "";
        return switch (type.toUpperCase()) {
            case "SUPPORT"   -> "badge-support";
            case "ORDER"     -> "badge-order";
            case "COMPLAINT" -> "badge-complaint";
            default          -> "badge-support";
        };
    }

    private static String statusBadgeClass(String status) {
        if (status == null) return "";
        return switch (status.toLowerCase()) {
            case "open"    -> "badge-open";
            case "claimed" -> "badge-claimed";
            default        -> "badge-closed";
        };
    }

    // Deterministic color per userId
    private static final String[] COLORS = {
        "#5865f2", "#3ba55c", "#faa81a", "#ed4245",
        "#eb459e", "#00b0f4", "#f47fff", "#43b581"
    };

    private static String pickColor(String userId) {
        int hash = 0;
        for (char c : userId.toCharArray()) hash = (hash * 31 + c) & 0xFFFFFF;
        return COLORS[Math.abs(hash) % COLORS.length];
    }
}
