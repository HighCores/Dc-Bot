package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TranscriptService {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("MMM dd, yyyy · hh:mm a")
            .withZone(ZoneId.of("Asia/Riyadh"));
    private static final DateTimeFormatter TIME_ONLY = DateTimeFormatter
            .ofPattern("hh:mm a")
            .withZone(ZoneId.of("Asia/Riyadh"));

    public static byte[] generateFromMessages(String ticketId, String channelName, String type, String status,
                                              String openedAt, String openerName, String closedBy,
                                              List<Message> messages) {
        JsonArray arr = new JsonArray();
        for (Message m : messages) {
            String content = m.getContentDisplay();
            if (content == null || content.isBlank()) content = extractComponentText(m);
            if (content == null || content.isBlank()) {
                if (!m.getAttachments().isEmpty())
                    content = "\uD83D\uDCCE " + m.getAttachments().size() + " attachment(s)";
                else continue;
            }
            JsonObject obj = new JsonObject();
            obj.addProperty("user_id",   m.getAuthor().getId());
            obj.addProperty("is_bot",    m.getAuthor().isBot());
            String name = m.getMember() != null ? m.getMember().getEffectiveName() : m.getAuthor().getName();
            obj.addProperty("user_name", name);
            obj.addProperty("content",   content);
            obj.addProperty("created_at", m.getTimeCreated().toInstant().toString());
            arr.add(obj);
        }
        return buildHtml(ticketId, channelName, type, status, openedAt, openerName, "None", closedBy, arr);
    }

    private static String extractComponentText(Message m) {
        List<String> parts = new ArrayList<>();
        for (MessageTopLevelComponent top : m.getComponents()) collectText(top, parts);
        return String.join("\n", parts);
    }
    private static void collectText(Object c, List<String> out) {
        if (c instanceof TextDisplay td) { String t = td.getContent(); if (t != null && !t.isBlank()) out.add(t); }
        else if (c instanceof Container ct) { for (var ch : ct.getComponents()) collectText(ch, out); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    public static byte[] buildHtml(String ticketId, String channelName, String type, String status,
                                    String openedAt, String openerName, String claimedBy, String closedBy, JsonArray messages) {

        // ── Format timestamps ────────────────────────────────────────────────
        String openedFmt = openedAt;
        try { openedFmt = FMT.format(Instant.parse(openedAt)); } catch (Exception ignored) {}
        String nowFmt = FMT.format(Instant.now());

        // ── Build message bubbles ────────────────────────────────────────────
        StringBuilder msgs = new StringBuilder();
        int count = 0;
        String prevUser = null;
        boolean groupOpen = false;

        if (messages != null) {
            for (var el : messages) {
                JsonObject m   = el.getAsJsonObject();
                String userId  = safe(m, "user_id");
                String uName   = safe(m, "user_name");
                boolean isBot  = m.has("is_bot") && m.get("is_bot").getAsBoolean();
                String raw     = safe(m, "content");
                String content = e(raw).replace("\n", "<br>");
                String ts      = "";
                try { ts = TIME_ONLY.format(Instant.parse(safe(m, "created_at"))); } catch (Exception ignored) {}

                if (content.isBlank()) continue;
                count++;

                boolean newGroup = !userId.equals(prevUser);
                prevUser = userId;

                if (newGroup) {
                    if (groupOpen) msgs.append("</div></div>\n"); // close bubble-col + bubble-group
                    groupOpen = true;
                    String letter = uName.isEmpty() ? "?" : uName.substring(0, 1).toUpperCase();
                    String grad   = isBot
                        ? "linear-gradient(135deg,#5865f2,#4752c4)"
                        : "linear-gradient(135deg," + pickColor(userId) + "," + pickColorDark(userId) + ")";

                    msgs.append("<div class=\"bubble-group\">\n");
                    msgs.append("  <div class=\"av\" style=\"background:").append(grad).append("\">").append(e(letter)).append("</div>\n");
                    msgs.append("  <div class=\"bubble-col\">\n");
                    msgs.append("    <div class=\"bubble-meta\">");
                    msgs.append("<span class=\"uname").append(isBot ? " bot-tag" : "").append("\">").append(e(uName));
                    if (isBot) msgs.append("<span class=\"bot-badge\">BOT</span>");
                    msgs.append("</span>");
                    msgs.append("<span class=\"ts\">").append(ts).append("</span>");
                    msgs.append("</div>\n");
                    msgs.append("    <div class=\"bubble").append(isBot ? " bubble-bot" : "").append("\">").append(content).append("</div>\n");
                } else {
                    msgs.append("    <div class=\"bubble-cont\">\n");
                    msgs.append("      <span class=\"ts ts-hover\">").append(ts).append("</span>\n");
                    msgs.append("      <div class=\"bubble").append(isBot ? " bubble-bot" : "").append("\">").append(content).append("</div>\n");
                    msgs.append("    </div>\n");
                }
            }
            if (groupOpen) msgs.append("  </div>\n</div>\n"); // close last bubble-col + bubble-group
        }

        if (count == 0) {
            msgs.append("<div class=\"empty-state\">")
                .append("<div class=\"empty-icon\">\uD83D\uDCAC</div>")
                .append("<div class=\"empty-title\">No messages recorded</div>")
                .append("<div class=\"empty-sub\">This ticket had no conversation.</div>")
                .append("</div>");
        }

        // ── Type icon + badge class ──────────────────────────────────────────
        String typeIcon  = switch (type.toUpperCase()) {
            case "SUPPORT"   -> "🔵";
            case "ORDER"     -> "🟢";
            case "COMPLAINT" -> "🔴";
            default          -> "⚪";
        };
        String typeCls   = typeBadgeClass(type);
        String statusCls = statusBadgeClass(status);

        // ── Assemble HTML ────────────────────────────────────────────────────
        StringBuilder h = new StringBuilder(65536);
        h.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        h.append("  <meta charset=\"UTF-8\">\n");
        h.append("  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n");
        h.append("  <title>Transcript · #").append(e(channelName)).append("</title>\n");
        h.append("  <link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n");
        h.append("  <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap\" rel=\"stylesheet\">\n");
        h.append("  <style>\n");
        h.append(CSS);
        h.append("  </style>\n</head>\n<body>\n\n");

        // HERO
        h.append("<div class=\"hero\">\n");
        h.append("  <div class=\"hero-top\">\n");
        h.append("    <div style=\"display:flex;align-items:center;gap:12px\">\n");
        h.append("      <div class=\"logo-mark\">HC</div>\n");
        h.append("      <div><div class=\"logo-text\">HIGH CORE AGENCY</div><div class=\"logo-sub\">Ticket Transcript</div></div>\n");
        h.append("    </div>\n");
        h.append("    <div class=\"hero-channel\"><span>#</span>").append(e(channelName)).append("</div>\n");
        h.append("  </div>\n");
        h.append("  <div class=\"hero-divider\"></div>\n");
        h.append("  <div class=\"stats-grid\">\n");
        statCard(h, "Ticket ID",   "#" + e(ticketId));
        h.append("    <div class=\"stat-card\"><div class=\"stat-label\">Type</div>");
        h.append("<div class=\"stat-value\"><span class=\"badge ").append(typeCls).append("\">")
         .append(typeIcon).append(" ").append(e(type)).append("</span></div></div>\n");
        h.append("    <div class=\"stat-card\"><div class=\"stat-label\">Status</div>");
        h.append("<div class=\"stat-value\"><span class=\"badge ").append(statusCls).append("\">")
         .append(e(status.toUpperCase())).append("</span></div></div>\n");
        statCard(h, "Opened By",   e(openerName));
        statCard(h, "Opened At",   e(openedFmt));
        statCard(h, "Handled By",  e(claimedBy));
        statCard(h, "Closed By",   e(closedBy));
        statCard(h, "Messages",    String.valueOf(count));
        h.append("  </div>\n</div>\n\n");

        // MESSAGES
        h.append("<div class=\"section-label\">Conversation History</div>\n");
        h.append("<div class=\"messages-wrap\">\n");
        h.append(msgs);
        h.append("</div>\n\n");

        // FOOTER
        h.append("<div class=\"footer\">\n");
        h.append("  <span class=\"footer-brand\">HIGH CORE AGENCY</span>\n");
        h.append("  <span class=\"footer-info\">Generated ").append(nowFmt)
         .append(" (Asia/Riyadh) &nbsp;·&nbsp; Confidential — Internal Use Only</span>\n");
        h.append("</div>\n\n</body>\n</html>\n");

        return h.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void statCard(StringBuilder h, String label, String value) {
        h.append("    <div class=\"stat-card\">")
         .append("<div class=\"stat-label\">").append(label).append("</div>")
         .append("<div class=\"stat-value\">").append(value).append("</div>")
         .append("</div>\n");
    }

    private static String safe(JsonObject m, String key) {
        return m.has(key) && !m.get(key).isJsonNull() ? m.get(key).getAsString() : "";
    }
    private static String e(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    private static String typeBadgeClass(String t) {
        if (t == null) return "";
        return switch (t.toUpperCase()) {
            case "SUPPORT"   -> "badge-support";
            case "ORDER"     -> "badge-order";
            case "COMPLAINT" -> "badge-complaint";
            default          -> "badge-support";
        };
    }
    private static String statusBadgeClass(String s) {
        if (s == null) return "";
        return switch (s.toLowerCase()) {
            case "open"    -> "badge-open";
            case "claimed" -> "badge-claimed";
            default        -> "badge-closed";
        };
    }
    private static final String[] COLORS      = {"#5865f2","#3ba55c","#e67e22","#ed4245","#eb459e","#00b0f4","#9b59b6","#1abc9c"};
    private static final String[] COLORS_DARK = {"#4752c4","#2d8a47","#ca6f1e","#c0392b","#c0387e","#0090d4","#7d3c98","#16a085"};
    private static String pickColor(String id)     { int h=0; for(char c:id.toCharArray()) h=(h*31+c)&0xFFFFFF; return COLORS[Math.abs(h)%COLORS.length]; }
    private static String pickColorDark(String id) { int h=0; for(char c:id.toCharArray()) h=(h*31+c)&0xFFFFFF; return COLORS_DARK[Math.abs(h)%COLORS_DARK.length]; }

    // ── CSS constant ─────────────────────────────────────────────────────────
    private static final String CSS = """
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    :root {
      --bg:#0d0e10; --surface:#141517; --surface2:#1a1b1e; --surface3:#202225;
      --border:#2a2c30; --gold:#C5A059; --gold2:#FFD700;
      --text:#e3e5e8; --muted:#72767d; --subtle:#4f545c;
    }
    body { background:var(--bg); color:var(--text); font-family:'Inter',system-ui,sans-serif; font-size:14px; line-height:1.5; min-height:100vh; }

    .hero { background:linear-gradient(180deg,#0a0a0c,#111215); border-bottom:1px solid var(--border); padding:36px 48px 28px; position:relative; overflow:hidden; }
    .hero::before { content:""; position:absolute; inset:0; background:radial-gradient(ellipse 60% 80% at 50% -20%,rgba(197,160,89,.12),transparent 70%); pointer-events:none; }
    .hero-top { display:flex; align-items:center; gap:16px; margin-bottom:24px; }
    .logo-mark { width:44px; height:44px; background:linear-gradient(135deg,#C5A059,#FFD700); border-radius:10px; display:flex; align-items:center; justify-content:center; font-weight:800; font-size:18px; color:#000; box-shadow:0 4px 16px rgba(197,160,89,.35); }
    .logo-text { font-size:20px; font-weight:800; letter-spacing:.5px; background:linear-gradient(90deg,#C5A059,#FFD700,#C5A059); background-size:200%; -webkit-background-clip:text; -webkit-text-fill-color:transparent; animation:shimmer 3s linear infinite; }
    @keyframes shimmer { to { background-position:200% center; } }
    .logo-sub { font-size:12px; color:var(--muted); letter-spacing:1.5px; text-transform:uppercase; margin-top:1px; }
    .hero-channel { margin-left:auto; display:flex; align-items:center; gap:6px; background:var(--surface3); border:1px solid var(--border); border-radius:8px; padding:6px 14px; font-size:13px; font-weight:600; }
    .hero-channel span { color:var(--muted); }
    .hero-divider { height:1px; background:linear-gradient(90deg,transparent,var(--gold),transparent); opacity:.4; margin-bottom:24px; }

    .stats-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(140px,1fr)); gap:12px; }
    .stat-card { background:var(--surface2); border:1px solid var(--border); border-radius:10px; padding:12px 16px; position:relative; overflow:hidden; }
    .stat-card::before { content:""; position:absolute; top:0; left:0; right:0; height:2px; background:linear-gradient(90deg,var(--gold),var(--gold2)); opacity:0; transition:opacity .2s; }
    .stat-card:hover::before { opacity:1; }
    .stat-label { font-size:10px; font-weight:700; text-transform:uppercase; letter-spacing:1px; color:var(--muted); margin-bottom:6px; }
    .stat-value { font-size:14px; font-weight:600; color:var(--text); }

    .badge { display:inline-flex; align-items:center; gap:5px; padding:3px 10px; border-radius:20px; font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:.5px; }
    .badge-support   { background:rgba(88,101,242,.2);  color:#848df9; border:1px solid rgba(88,101,242,.3); }
    .badge-order     { background:rgba(59,165,92,.2);   color:#57c97e; border:1px solid rgba(59,165,92,.3); }
    .badge-complaint { background:rgba(237,66,69,.2);   color:#f57476; border:1px solid rgba(237,66,69,.3); }
    .badge-closed    { background:rgba(116,127,141,.2); color:#9da5b0; border:1px solid rgba(116,127,141,.3); }
    .badge-open      { background:rgba(59,165,92,.2);   color:#57c97e; border:1px solid rgba(59,165,92,.3); }
    .badge-claimed   { background:rgba(250,168,26,.2);  color:#fbb73a; border:1px solid rgba(250,168,26,.3); }

    .section-label { display:flex; align-items:center; gap:12px; padding:28px 48px 16px; font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:1.2px; color:var(--muted); }
    .section-label::after { content:""; flex:1; height:1px; background:var(--border); }

    .messages-wrap { padding:0 48px 60px; }

    .bubble-group { display:flex; gap:14px; margin-bottom:20px; }
    .av { width:38px; height:38px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-weight:700; font-size:15px; color:#fff; flex-shrink:0; margin-top:2px; box-shadow:0 2px 8px rgba(0,0,0,.4); }
    .bubble-col { flex:1; min-width:0; }
    .bubble-meta { display:flex; align-items:baseline; gap:8px; margin-bottom:4px; }
    .uname { font-size:14px; font-weight:700; color:#fff; }
    .bot-tag { color:#848df9; }
    .bot-badge { background:#5865f2; color:#fff; font-size:9px; font-weight:700; padding:1px 5px; border-radius:3px; margin-left:5px; vertical-align:middle; }
    .ts { font-size:11px; color:var(--subtle); }

    .bubble { background:var(--surface2); border:1px solid var(--border); border-radius:0 12px 12px 12px; padding:10px 14px; color:var(--text); font-size:14px; line-height:1.55; word-break:break-word; max-width:720px; margin-bottom:4px; transition:border-color .15s; }
    .bubble:hover { border-color:#3a3c41; }
    .bubble-bot { background:rgba(88,101,242,.08); border-color:rgba(88,101,242,.2); }
    .bubble-cont { display:flex; align-items:flex-start; gap:8px; margin-bottom:4px; }
    .bubble-cont .bubble { border-radius:12px; flex:1; }
    .ts-hover { opacity:0; transition:opacity .15s; min-width:52px; text-align:right; padding-top:11px; font-size:11px; color:var(--subtle); }
    .bubble-cont:hover .ts-hover { opacity:1; }

    .empty-state { text-align:center; padding:80px 0; color:var(--muted); }
    .empty-icon { font-size:40px; margin-bottom:12px; }
    .empty-title { font-size:18px; font-weight:700; color:var(--subtle); margin-bottom:6px; }
    .empty-sub { font-size:13px; }

    .footer { background:var(--surface); border-top:1px solid var(--border); padding:20px 48px; display:flex; align-items:center; justify-content:space-between; gap:16px; flex-wrap:wrap; }
    .footer-brand { font-size:13px; font-weight:700; background:linear-gradient(90deg,#C5A059,#FFD700); -webkit-background-clip:text; -webkit-text-fill-color:transparent; }
    .footer-info { font-size:11px; color:var(--subtle); }
""";
}
