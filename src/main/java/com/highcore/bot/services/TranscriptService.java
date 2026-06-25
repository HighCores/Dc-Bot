package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TranscriptService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("MMM dd, yyyy · hh:mm a", Locale.ENGLISH).withZone(ZoneId.of("Asia/Riyadh"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
            .withZone(ZoneId.of("Asia/Riyadh"));

    public static byte[] buildHtml(String id, String channelName, String type, String status, String openedAt,
            String openerName, String claimedBy, String closedBy, JsonArray messages) {
        StringBuilder sb = new StringBuilder();

        sb.append(
                "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>");
        sb.append("<title>Transcript · #").append(channelName).append("</title>");
        sb.append(
                "<link rel='preconnect' href='https://fonts.googleapis.com'><link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap' rel='stylesheet'>");
        sb.append("<style>");
        sb.append(
                "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; } :root { --bg:#0d0e10; --surface:#141517; --surface2:#1a1b1e; --surface3:#202225; --border:#2a2c30; --gold:#C5A059; --gold2:#FFD700; --text:#e3e5e8; --muted:#72767d; --subtle:#4f545c; }");
        sb.append(
                "body { background:var(--bg); color:var(--text); font-family:'Inter',system-ui,sans-serif; font-size:14px; line-height:1.5; min-height:100vh; }");
        sb.append(
                ".hero { background:linear-gradient(180deg,#0a0a0c,#111215); border-bottom:1px solid var(--border); padding:36px 48px 28px; position:relative; overflow:hidden; }");
        sb.append(
                ".hero::before { content:''; position:absolute; inset:0; background:radial-gradient(ellipse 60% 80% at 50% -20%,rgba(197,160,89,.12),transparent 70%); pointer-events:none; }");
        sb.append(".hero-top { display:flex; align-items:center; gap:16px; margin-bottom:24px; }");
        sb.append(
                ".logo-mark { width:44px; height:44px; background:linear-gradient(135deg,#C5A059,#FFD700); border-radius:10px; display:flex; align-items:center; justify-content:center; font-weight:800; font-size:18px; color:#000; box-shadow:0 4px 16px rgba(197,160,89,.35); }");
        sb.append(
                ".logo-text { font-size:20px; font-weight:800; letter-spacing:.5px; background:linear-gradient(90deg,#C5A059,#FFD700,#C5A059); background-size:200%; -webkit-background-clip:text; -webkit-text-fill-color:transparent; animation:shimmer 3s linear infinite; }");
        sb.append("@keyframes shimmer { to { background-position:200% center; } }");
        sb.append(
                ".logo-sub { font-size:12px; color:var(--muted); letter-spacing:1.5px; text-transform:uppercase; margin-top:1px; }");
        sb.append(
                ".hero-channel { margin-left:auto; display:flex; align-items:center; gap:6px; background:var(--surface3); border:1px solid var(--border); border-radius:8px; padding:6px 14px; font-size:13px; font-weight:600; }");
        sb.append(".hero-channel span { color:var(--muted); }");
        sb.append(".stats-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(140px,1fr)); gap:12px; }");
        sb.append(
                ".stat-card { background:var(--surface2); border:1px solid var(--border); border-radius:10px; padding:12px 16px; position:relative; overflow:hidden; }");
        sb.append(
                ".stat-label { font-size:10px; font-weight:700; text-transform:uppercase; letter-spacing:1px; color:var(--muted); margin-bottom:6px; }");
        sb.append(".stat-value { font-size:14px; font-weight:600; color:var(--text); }");
        sb.append(
                ".badge { display:inline-flex; align-items:center; gap:5px; padding:3px 10px; border-radius:20px; font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:.5px; }");
        sb.append(
                ".badge-support { background:rgba(88,101,242,.2); color:#848df9; border:1px solid rgba(88,101,242,.3); }");
        sb.append(
                ".badge-order { background:rgba(59,165,92,.2); color:#57c97e; border:1px solid rgba(59,165,92,.3); }");
        sb.append(
                ".badge-closed { background:rgba(116,127,141,.2); color:#9da5b0; border:1px solid rgba(116,127,141,.3); }");
        sb.append(
                ".section-label { display:flex; align-items:center; gap:12px; padding:28px 48px 16px; font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:1.2px; color:var(--muted); }");
        sb.append(".section-label::after { content:''; flex:1; height:1px; background:var(--border); }");
        sb.append(".messages-wrap { padding:0 48px 60px; }");
        sb.append(".bubble-group { display:flex; gap:14px; margin-bottom:20px; }");
        sb.append(
                ".av { width:38px; height:38px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-weight:700; font-size:15px; color:#fff; flex-shrink:0; margin-top:2px; box-shadow:0 2px 8px rgba(0,0,0,.4); }");
        sb.append(".bubble-col { flex:1; min-width:0; }");
        sb.append(".bubble-meta { display:flex; align-items:baseline; gap:8px; margin-bottom:4px; }");
        sb.append(".uname { font-size:14px; font-weight:700; color:#fff; }");
        sb.append(
                ".bot-badge { background:#5865f2; color:#fff; font-size:9px; font-weight:700; padding:1px 5px; border-radius:3px; margin-left:5px; vertical-align:middle; }");
        sb.append(".ts { font-size:11px; color:var(--subtle); }");
        sb.append(
                ".bubble { background:var(--surface2); border:1px solid var(--border); border-radius:0 12px 12px 12px; padding:10px 14px; color:var(--text); font-size:14px; line-height:1.55; word-break:break-word; max-width:720px; margin-bottom:4px; }");
        sb.append(".bubble-bot { background:rgba(88,101,242,.08); border-color:rgba(88,101,242,.2); }");
        sb.append(
                ".footer { background:var(--surface); border-top:1px solid var(--border); padding:20px 48px; display:flex; align-items:center; justify-content:space-between; font-size:11px; color:var(--subtle); }");
        sb.append(
                ".footer-brand { font-size:13px; font-weight:700; background:linear-gradient(90deg,#C5A059,#FFD700); -webkit-background-clip:text; -webkit-text-fill-color:transparent; }");
        sb.append(".att-wrap { margin:8px 0; display:flex; flex-wrap:wrap; gap:8px; }");
        sb.append(
                ".att-img { max-width:400px; max-height:400px; border-radius:8px; border:1px solid var(--border); box-shadow:0 2px 8px rgba(0,0,0,.2); }");
        sb.append("</style></head><body>");

        sb.append("<div class='hero'><div class='hero-top'><div style='display:flex;align-items:center;gap:12px'>");
        sb.append(
                "<div class='logo-mark'>HC</div><div><div class='logo-text'>HIGH CORE AGENCY</div><div class='logo-sub'>Ticket Transcript</div></div>");
        sb.append("</div><div class='hero-channel'><span>#</span>").append(channelName).append("</div></div>");
        sb.append("<div class='stats-grid'>");
        sb.append("<div class='stat-card'><div class='stat-label'>Ticket ID</div><div class='stat-value'>#").append(id)
                .append("</div></div>");
        sb.append(
                "<div class='stat-card'><div class='stat-label'>Type</div><div class='stat-value'><span class='badge badge-support'>")
                .append(type).append("</span></div></div>");
        sb.append(
                "<div class='stat-card'><div class='stat-label'>Status</div><div class='stat-value'><span class='badge badge-closed'>")
                .append(status.toUpperCase()).append("</span></div></div>");
        sb.append("<div class='stat-card'><div class='stat-label'>Opened By</div><div class='stat-value'>")
                .append(openerName).append("</div></div>");
        sb.append("<div class='stat-card'><div class='stat-label'>Opened At</div><div class='stat-value'>")
                .append(formatDate(openedAt)).append("</div></div>");
        sb.append("<div class='stat-card'><div class='stat-label'>Messages</div><div class='stat-value'>")
                .append(messages.size()).append("</div></div>");
        sb.append("</div></div>");

        sb.append("<div class='section-label'>Conversation History</div>");
        sb.append("<div class='messages-wrap'>");

        String lastUser = "";
        for (int i = 0; i < messages.size(); i++) {
            JsonObject m = messages.get(i).getAsJsonObject();
            String uId = safe(m, "user_id");
            String uName = safe(m, "user_name");
            String content = safe(m, "content");
            String time = formatTime(safe(m, "created_at"));

            boolean isBot = uName.toLowerCase().contains("bot") || uName.toLowerCase().contains("agency")
                    || uName.equals("Highcore");

            if (!uId.equals(lastUser)) {
                if (!lastUser.isEmpty())
                    sb.append("</div></div>");
                sb.append("<div class='bubble-group'>");
                sb.append("<div class='av' style='background:").append(getAvatarColor(uName)).append("'>")
                        .append(uName.substring(0, 1).toUpperCase()).append("</div>");
                sb.append("<div class='bubble-col'>");
                sb.append("<div class='bubble-meta'><span class='uname").append(isBot ? " bot-tag" : "").append("'>")
                        .append(uName);
                if (isBot)
                    sb.append("<span class='bot-badge'>BOT</span>");
                sb.append("</span><span class='ts'>").append(time).append("</span></div>");
            }

            sb.append("<div class='bubble").append(isBot ? " bubble-bot" : "").append("'>")
                    .append(processContent(content)).append("</div>");
            lastUser = uId;
        }
        if (messages.size() > 0)
            sb.append("</div></div>");

        sb.append("</div>");

        sb.append("<div class='footer'><span class='footer-brand'>Highcore Agency</span>");
        sb.append("<span>Generated ").append(DATE_FORMAT.format(Instant.now())).append(" (Asia/Riyadh)</span></div>");
        sb.append("</body></html>");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String formatDate(String iso) {
        try {
            return DATE_FORMAT.format(Instant.parse(iso));
        } catch (Exception e) {
            return iso;
        }
    }

    private static String formatTime(String iso) {
        try {
            return TIME_FORMAT.format(Instant.parse(iso));
        } catch (Exception e) {
            return "";
        }
    }

    private static String safe(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    private static String getAvatarColor(String name) {
        int hash = name.hashCode();
        String[] colors = { "#5865f2", "#3ba55c", "#fac418", "#ed4245", "#eb459e", "#57c97e" };
        return colors[Math.abs(hash) % colors.length];
    }

    private static String processContent(String content) {
        String html = content.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");

        if (html.contains("[ATTACHMENT: ")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[ATTACHMENT: (.*?)\\]").matcher(html);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String url = m.group(1);
                String lowerUrl = url.toLowerCase();
                if (lowerUrl.matches(".*\\.(png|jpg|jpeg|gif|webp|bmp)(?:\\?.*)?$")) {
                    m.appendReplacement(sb,
                            "<div class='att-wrap'><a href='" + url + "' target='_blank'><img class='att-img' src='"
                                    + url + "' alt='Image Attachment'></a></div>");
                } else {
                    String fileName = "Download File/Video";
                    try {
                        String[] parts = url.split("\\?")[0].split("/");
                        fileName = parts[parts.length - 1];
                    } catch (Exception ignored) {
                    }
                    m.appendReplacement(sb, "<div style='margin-top:8px'><a href='" + url
                            + "' target='_blank' style='display:inline-flex;align-items:center;gap:6px;background:var(--surface);border:1px solid var(--border);padding:8px 12px;border-radius:6px;color:var(--gold);text-decoration:none;font-size:12px;font-weight:600'>📎 "
                            + fileName + "</a></div>");
                }
            }
            m.appendTail(sb);
            html = sb.toString();
        }
        return html;
    }
}
