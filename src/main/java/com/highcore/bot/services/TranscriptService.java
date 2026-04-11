package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class TranscriptService {

    private static final DateTimeFormatter FMT = DateTimeFormatter
            .ofPattern("MMM dd, yyyy · hh:mm a")
            .withZone(ZoneId.of("Asia/Riyadh"));

    private static final DateTimeFormatter TIME_ONLY = DateTimeFormatter
            .ofPattern("hh:mm a")
            .withZone(ZoneId.of("Asia/Riyadh"));

    // Generate from Discord Message objects (fetched directly from channel history)
    public static byte[] generateFromMessages(String ticketId, String channelName, String type, String status,
                                              String openedAt, String openerName, String closedBy, List<Message> messages) {
        JsonArray arr = new JsonArray();
        for (Message m : messages) {
            JsonObject obj = new JsonObject();
            obj.addProperty("user_id", m.getAuthor().getId());
            obj.addProperty("is_bot", m.getAuthor().isBot());
            String authorName = m.getMember() != null
                    ? m.getMember().getEffectiveName()
                    : m.getAuthor().getName();
            obj.addProperty("user_name", authorName);

            // Try plain text content first
            String content = m.getContentDisplay();

            // For bot Component V2 messages, extract text from containers
            if ((content == null || content.isBlank()) && m.getAuthor().isBot()) {
                content = extractComponentText(m);
            }

            // Attachments
            if ((content == null || content.isBlank()) && !m.getAttachments().isEmpty()) {
                content = "\uD83D\uDCCE " + m.getAttachments().size() + " attachment(s)";
            }

            // Skip completely empty messages (e.g. pure file uploads already counted above)
            if (content == null || content.isBlank()) continue;

            obj.addProperty("content", content);
            obj.addProperty("created_at", m.getTimeCreated().toInstant().toString());
            arr.add(obj);
        }
        return buildHtml(ticketId, channelName, type, status, openedAt, openerName, closedBy, arr);
    }

    /** Walks Component V2 containers and concatenates TextDisplay text */
    private static String extractComponentText(Message m) {
        List<String> parts = new ArrayList<>();
        for (MessageTopLevelComponent top : m.getComponents()) {
            extractFromComponent(top, parts);
        }
        return String.join("\n", parts);
    }

    private static void extractFromComponent(Object component, List<String> out) {
        if (component instanceof TextDisplay td) {
            String t = td.getContent();
            if (t != null && !t.isBlank()) out.add(t);
        } else if (component instanceof Container c) {
            for (var child : c.getComponents()) extractFromComponent(child, out);
        }
        // ActionRow, MediaGallery, Separator — skip
    }

    private static byte[] buildHtml(String ticketId, String channelName, String type, String status,
                                    String openedAt, String openerName, String closedBy, JsonArray messages) {

        StringBuilder msgs = new StringBuilder();
        int count = 0;
        String prevUser = null;

        if (messages != null) {
            for (var el : messages) {
                JsonObject m   = el.getAsJsonObject();
                String userId  = m.has("user_id")  ? m.get("user_id").getAsString()  : "0";
                String uName   = m.has("user_name") ? m.get("user_name").getAsString(): "Unknown";
                boolean isBot  = m.has("is_bot") && m.get("is_bot").getAsBoolean();
                String content = m.has("content")   ? htmlEscape(m.get("content").getAsString()) : "";
                String ts      = "—";
                if (m.has("created_at") && !m.get("created_at").isJsonNull()) {
                    try { ts = TIME_ONLY.format(Instant.parse(m.get("created_at").getAsString())); }
                    catch (Exception ignored) {}
                }
                content = content.replace("\n", "<br>");
                if (content.isBlank()) continue;

                boolean newGroup = !userId.equals(prevUser);
                prevUser = userId;
                count++;

                String letter = uName.isEmpty() ? "?" : String.valueOf(uName.charAt(0)).toUpperCase();
                String color  = isBot ? "linear-gradient(135deg,#5865f2,#4752c4)"
                                      : "linear-gradient(135deg," + pickColor(userId) + "," + pickColorDark(userId) + ")";

                if (newGroup) {
                    if (count > 1) msgs.append("</div>"); // close prev bubble-group
                    msgs.append("<div class=\"bubble-group\">")
                        .append("<div class=\"av\" style=\"background:").append(color).append("\">")
                        .append(letter).append("</div>")
                        .append("<div class=\"bubble-col\">")
                        .append("<div class=\"bubble-meta\">")
                        .append("<span class=\"uname").append(isBot ? " bot-tag" : "").append("\">")
                        .append(htmlEscape(uName)).append(isBot ? "<span class=\"bot-badge\">BOT</span>" : "").append("</span>")
                        .append("<span class=\"ts\">").append(ts).append("</span>")
                        .append("</div>");
                } else {
                    msgs.append("<div class=\"bubble-cont\">");
                    msgs.append("<span class=\"ts ts-hover\">").append(ts).append("</span>");
                }

                msgs.append("<div class=\"bubble").append(isBot ? " bubble-bot" : "").append("\">")
                    .append(content).append("</div>");

                if (!newGroup) msgs.append("</div>");
            }
            if (count > 0) msgs.append("</div></div>"); // close last bubble-col + bubble-group
        }

        if (count == 0) {
            msgs.append("""
                <div class="empty-state">
                  <div class="empty-icon">💬</div>
                  <div class="empty-title">No messages recorded</div>
                  <div class="empty-sub">This ticket had no conversation to transcript.</div>
                </div>""");
        }

        String openedFmt = openedAt;
        try { openedFmt = FMT.format(Instant.parse(openedAt)); } catch (Exception ignored) {}
        String nowFmt = FMT.format(Instant.now());

        String typeIcon = switch (type.toUpperCase()) {
            case "SUPPORT"   -> "🔵";
            case "ORDER"     -> "🟢";
            case "COMPLAINT" -> "🔴";
            default          -> "⚪";
        };

        String html = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Transcript · #""" + htmlEscape(channelName) + """
</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

    :root {
      --bg:        #0d0e10;
      --surface:   #141517;
      --surface2:  #1a1b1e;
      --surface3:  #202225;
      --border:    #2a2c30;
      --gold:      #C5A059;
      --gold2:     #FFD700;
      --text:      #e3e5e8;
      --muted:     #72767d;
      --subtle:    #4f545c;
      --white:     #ffffff;
    }

    body {
      background: var(--bg);
      color: var(--text);
      font-family: 'Inter', system-ui, sans-serif;
      font-size: 14px;
      line-height: 1.5;
      min-height: 100vh;
    }

    /* ═══════════════════════════════════════════
       HERO HEADER
    ═══════════════════════════════════════════ */
    .hero {
      background: linear-gradient(180deg, #0a0a0c 0%, #111215 100%);
      border-bottom: 1px solid var(--border);
      padding: 36px 48px 28px;
      position: relative;
      overflow: hidden;
    }
    .hero::before {
      content: "";
      position: absolute;
      inset: 0;
      background: radial-gradient(ellipse 60% 80% at 50% -20%, rgba(197,160,89,.12) 0%, transparent 70%);
      pointer-events: none;
    }
    .hero-top {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 24px;
    }
    .logo-mark {
      width: 44px; height: 44px;
      background: linear-gradient(135deg, #C5A059, #FFD700);
      border-radius: 10px;
      display: flex; align-items: center; justify-content: center;
      font-weight: 800; font-size: 18px; color: #000;
      flex-shrink: 0;
      box-shadow: 0 4px 16px rgba(197,160,89,.35);
    }
    .logo-text {
      font-size: 20px; font-weight: 800; letter-spacing: .5px;
      background: linear-gradient(90deg, #C5A059, #FFD700, #C5A059);
      background-size: 200%;
      -webkit-background-clip: text; -webkit-text-fill-color: transparent;
      animation: shimmer 3s linear infinite;
    }
    @keyframes shimmer { to { background-position: 200% center; } }
    .logo-sub { font-size: 12px; color: var(--muted); letter-spacing: 1.5px; text-transform: uppercase; margin-top: 1px; }

    .hero-channel {
      margin-left: auto;
      display: flex; align-items: center; gap: 8px;
      background: var(--surface3);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 6px 14px;
      font-size: 13px; font-weight: 600; color: var(--text);
    }
    .hero-channel span { color: var(--muted); }

    .hero-divider {
      height: 1px;
      background: linear-gradient(90deg, transparent, var(--gold), transparent);
      opacity: .4;
      margin-bottom: 24px;
    }

    /* ═══════════════════════════════════════════
       STAT CARDS
    ═══════════════════════════════════════════ */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
      gap: 12px;
    }
    .stat-card {
      background: var(--surface2);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 12px 16px;
      position: relative;
      overflow: hidden;
    }
    .stat-card::before {
      content: "";
      position: absolute;
      top: 0; left: 0; right: 0;
      height: 2px;
      background: linear-gradient(90deg, var(--gold), var(--gold2));
      opacity: 0;
      transition: opacity .2s;
    }
    .stat-card:hover::before { opacity: 1; }
    .stat-label {
      font-size: 10px; font-weight: 700; text-transform: uppercase;
      letter-spacing: 1px; color: var(--muted); margin-bottom: 6px;
    }
    .stat-value { font-size: 14px; font-weight: 600; color: var(--text); }

    .badge {
      display: inline-flex; align-items: center; gap: 5px;
      padding: 3px 10px;
      border-radius: 20px;
      font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .5px;
    }
    .badge-support  { background: rgba(88,101,242,.2);  color: #848df9; border: 1px solid rgba(88,101,242,.3); }
    .badge-order    { background: rgba(59,165,92,.2);   color: #57c97e; border: 1px solid rgba(59,165,92,.3); }
    .badge-complaint{ background: rgba(237,66,69,.2);   color: #f57476; border: 1px solid rgba(237,66,69,.3); }
    .badge-closed   { background: rgba(116,127,141,.2); color: #9da5b0; border: 1px solid rgba(116,127,141,.3); }
    .badge-open     { background: rgba(59,165,92,.2);   color: #57c97e; border: 1px solid rgba(59,165,92,.3); }
    .badge-claimed  { background: rgba(250,168,26,.2);  color: #fbb73a; border: 1px solid rgba(250,168,26,.3); }

    /* ═══════════════════════════════════════════
       MESSAGES SECTION
    ═══════════════════════════════════════════ */
    .section-label {
      display: flex; align-items: center; gap: 12px;
      padding: 28px 48px 16px;
      font-size: 11px; font-weight: 700; text-transform: uppercase;
      letter-spacing: 1.2px; color: var(--muted);
    }
    .section-label::after {
      content: ""; flex: 1; height: 1px; background: var(--border);
    }

    .messages-wrap {
      padding: 0 48px 60px;
      max-width: 1000px;
      margin: 0 auto;
    }

    .bubble-group {
      display: flex;
      gap: 14px;
      margin-bottom: 20px;
    }
    .av {
      width: 38px; height: 38px;
      border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 15px; color: #fff;
      flex-shrink: 0;
      margin-top: 2px;
      box-shadow: 0 2px 8px rgba(0,0,0,.4);
    }
    .bubble-col { flex: 1; min-width: 0; }
    .bubble-meta {
      display: flex; align-items: baseline; gap: 8px;
      margin-bottom: 4px;
    }
    .uname {
      font-size: 14px; font-weight: 700; color: var(--white);
    }
    .bot-tag { color: #848df9; }
    .bot-badge {
      display: inline-block;
      background: #5865f2; color: #fff;
      font-size: 9px; font-weight: 700;
      padding: 1px 5px; border-radius: 3px;
      margin-left: 5px; vertical-align: middle;
    }
    .ts {
      font-size: 11px; color: var(--subtle);
      font-weight: 400;
    }

    .bubble {
      background: var(--surface2);
      border: 1px solid var(--border);
      border-radius: 0 12px 12px 12px;
      padding: 10px 14px;
      color: var(--text);
      font-size: 14px; line-height: 1.55;
      word-break: break-word;
      max-width: 720px;
      margin-bottom: 4px;
      transition: border-color .15s;
    }
    .bubble:hover { border-color: #3a3c41; }
    .bubble-bot {
      background: rgba(88,101,242,.08);
      border-color: rgba(88,101,242,.2);
    }

    .bubble-cont {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      margin-bottom: 4px;
    }
    .bubble-cont .bubble {
      border-radius: 12px;
      flex: 1;
    }
    .ts-hover {
      opacity: 0;
      transition: opacity .15s;
      min-width: 52px;
      text-align: right;
      padding-top: 11px;
    }
    .bubble-cont:hover .ts-hover { opacity: 1; }

    .empty-state {
      text-align: center;
      padding: 80px 0;
      color: var(--muted);
    }
    .empty-icon { font-size: 40px; margin-bottom: 12px; }
    .empty-title { font-size: 18px; font-weight: 700; color: var(--subtle); margin-bottom: 6px; }
    .empty-sub { font-size: 13px; }

    /* ═══════════════════════════════════════════
       FOOTER
    ═══════════════════════════════════════════ */
    .footer {
      background: var(--surface);
      border-top: 1px solid var(--border);
      padding: 20px 48px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      flex-wrap: wrap;
    }
    .footer-brand {
      font-size: 13px; font-weight: 700;
      background: linear-gradient(90deg, #C5A059, #FFD700);
      -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    }
    .footer-info { font-size: 11px; color: var(--subtle); }
    .footer-dot { color: var(--border); margin: 0 6px; }
  </style>
</head>
<body>

  <!-- HERO -->
  <div class="hero">
    <div class="hero-top">
      <div>
        <div style="display:flex;align-items:center;gap:12px">
          <div class="logo-mark">HC</div>
          <div>
            <div class="logo-text">HIGH CORE AGENCY</div>
            <div class="logo-sub">Ticket Transcript</div>
          </div>
        </div>
      </div>
      <div class="hero-channel">
        <span>#</span>""" + htmlEscape(channelName) + """
      </div>
    </div>
    <div class="hero-divider"></div>
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">Ticket ID</div>
        <div class="stat-value">#""" + htmlEscape(ticketId) + """
</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Type</div>
        <div class="stat-value"><span class="badge """ + typeBadgeClass(type) + "\">\" + typeIcon + \" \" + htmlEscape(type) + \"</span></div>\n" +
        "      </div>\n" +
        "      <div class=\"stat-card\">\n" +
        "        <div class=\"stat-label\">Status</div>\n" +
        "        <div class=\"stat-value\"><span class=\"badge " + statusBadgeClass(status) + "\">" + htmlEscape(status.toUpperCase()) + "</span></div>\n" +
        "      </div>\n" +
        "      <div class=\"stat-card\">\n" +
        "        <div class=\"stat-label\">Opened By</div>\n" +
        "        <div class=\"stat-value\">" + htmlEscape(openerName) + "</div>\n" +
        "      </div>\n" +
        "      <div class=\"stat-card\">\n" +
        "        <div class=\"stat-label\">Opened At</div>\n" +
        "        <div class=\"stat-value\">" + htmlEscape(openedFmt) + "</div>\n" +
        "      </div>\n" +
        "      <div class=\"stat-card\">\n" +
        "        <div class=\"stat-label\">Closed By</div>\n" +
        "        <div class=\"stat-value\">" + htmlEscape(closedBy) + "</div>\n" +
        "      </div>\n" +
        "      <div class=\"stat-card\">\n" +
        "        <div class=\"stat-label\">Messages</div>\n" +
        "        <div class=\"stat-value\">" + count + """
</div>
      </div>
    </div>
  </div>

  <!-- MESSAGES -->
  <div class="section-label" style="padding-left:48px">Conversation History</div>
  <div class="messages-wrap">
""" + msgs + """
  </div>

  <!-- FOOTER -->
  <div class="footer">
    <span class="footer-brand">HIGH CORE AGENCY</span>
    <span class="footer-info">
      Generated """ + nowFmt + """
 (Asia/Riyadh)
      <span class="footer-dot">·</span>
      Confidential — Internal Use Only
    </span>
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

    private static final String[] COLORS = {
        "#5865f2","#3ba55c","#e67e22","#ed4245",
        "#eb459e","#00b0f4","#9b59b6","#1abc9c"
    };
    private static final String[] COLORS_DARK = {
        "#4752c4","#2d8a47","#ca6f1e","#c0392b",
        "#c0387e","#0090d4","#7d3c98","#16a085"
    };

    private static String pickColor(String userId) {
        int h = 0; for (char c : userId.toCharArray()) h = (h * 31 + c) & 0xFFFFFF;
        return COLORS[Math.abs(h) % COLORS.length];
    }
    private static String pickColorDark(String userId) {
        int h = 0; for (char c : userId.toCharArray()) h = (h * 31 + c) & 0xFFFFFF;
        return COLORS_DARK[Math.abs(h) % COLORS_DARK.length];
    }
}
