package com.highcore.bot.utils;

import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class EmbedUtil {

    public static final String BANNER_MAIN = "https://images.unsplash.com/photo-1614850523296-d8c1af93d400?q=80&w=2070&auto=format&fit=crop";
    public static final String BANNER_SUPPORT = "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=2070&auto=format&fit=crop";
    public static final String BANNER_GIVEAWAY = "https://images.unsplash.com/photo-1513151233558-d860c5398176?q=80&w=2070&auto=format&fit=crop";
    
    public static final Color SUCCESS = Color.decode("#10b981"); 
    public static final Color DANGER = Color.decode("#f43f5e");  
    public static final Color WARNING = Color.decode("#f59e0b"); 
    public static final Color INFO = Color.decode("#3b82f6");
    public static final Color GOLD = Color.decode("#fbbf24");
    public static final Color ACCENT_TEAL = Color.decode("#14b8a6");

    public static final String RULES_TEXT = """
            ## \uD83D\uDCCB قوانين وكالة هايكور | Haikore Agency
            
            ### 🛡️ أولاً : القواعد العامة (General Rules)
            
            1- الاحترام المتبادل : يمنع الإساءة، التنمر، أو استخدام الألفاظ النابية نحن مجتمع مبني على الدعم والرقي
            
            2- الهوية المهنية : يفضل استخدام أسماء واضحة (أو مستعارة محترمة) وتجنب الصور الشخصية غير اللائقة
            
            3- الخصوصية: يمنع منعاً باتاً نشر معلومات شخصية (Doxxing) لأي عضو أو عميل داخل السيرفر
            
            ### \uD83D\uDCBC ثانياً : قوانين التعاملات والعمل (Business Rules)
            
            1- الجدية في الطلبات : رومات الطلبات مخصصة للعمل فقط أي استهزاء أو طلبات وهمية قد تؤدي لتقييد وصولك
            
            2- حقوق الملكية : جميع الحقوق البرمجية والتصميمية او اي خدمه تعود لوكالة "هايكور" ما لم يتم الاتفاق على خلاف ذلك مع العميل
            
            3- التواصل الرسمي : أي اتفاق مالي أو تقني يجب أن يتم عبر "التكتات" (Tickets) لضمان حق الطرفين وتوثيق العمل
            
            ### \uD83D\uDEAB ثالثاً : المحظورات (Prohibitions)
            
            1- ممنوع الإعلانات (No Ads) : يمنع نشر روابط سيرفرات أخرى أو الترويج لخدمات خارجية دون إذن الإدارة
            
            2- السبام (No Spam) : تجنب تكرار الرسائل أو المنشن العشوائي للإدارة ؛ الجميع هنا لخدمتك وفق جدول زمني
            
            3- المحتوى الحساس : يمنع نشر أي محتوى سياسي، ديني متطرف، أو خادش للحياء
            
            ### \u26A0\uFE0F رابعاً : الإجراءات الإدارية
            
            1- قرار الإدارة قطعي : الإدارة لها الحق في اتخاذ الإجراء المناسب (تحذير/طرد/باند) في حال مخالفة الروح العامة للوكالة، حتى لو لم  ينص القانون على المخالفة نصاً
            """;

    public static Container containerBranded(String sector, String title, String body, String imageUrl) {
        return containerBranded(sector, title, body, imageUrl, null);
    }

    public static Container containerBranded(String sector, String title, String body, String imageUrl, Emoji iconEmoji, ActionRow... rows) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        }

        layout.add(TextDisplay.of("### \u25C8 " + sector.toUpperCase() + " SECTOR \u30FB " + title.toUpperCase()));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));

        String content = (iconEmoji != null ? iconEmoji.getFormatted() + " " : "") + body;
        layout.add(TextDisplay.of(content));

        if (rows != null && rows.length > 0) {
            layout.add(Separator.createDivider(Separator.Spacing.SMALL));
            layout.addAll(Arrays.asList(rows));
            layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        layout.add(TextDisplay.of("` \u2022 UNIFIED TERMINAL v2.5 \u2022 HIGHCORE AGENCY \u2022 `"));

        return Container.of(layout);
    }

    public static Container startupPanel(ActionRow... rows) {
        String body = "Welcome to Highcore Agency Operative.\n" +
                "We provide high-fidelity digital solutions in Design, Development, Editing, and Minecraft Services.\n\n" +
                "Initialize your guidance via the command modules below.";
        return containerBranded("INIT", "Onboarding Sequence", body, BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDE80"), rows);
    }

    public static Container rulesPanel() {
        return containerBranded("PROTOCOL", "Compliance System", RULES_TEXT, BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDCDC"));
    }

    public static Container accessDenied() { return error("ACCESS RESTRICTED", "Credentials insufficient for this terminal node."); }
    public static Container error(String title, String description) { return containerBranded("ERROR", title, "[\u274C] " + description, BANNER_SUPPORT); }
    public static Container success(String title, String description) { return containerBranded("SUCCESS", title, "[\u2705] " + description, BANNER_MAIN); }
    public static Container info(String title, String description) { return containerBranded("INFO", title, "[\u2139\uFE0F] " + description, BANNER_MAIN); }

    public static Container activityLog(String type, String details, Color color) { return containerBranded("activity", type, details, BANNER_MAIN); }
    public static Container giveaway(String prize, int winners, int duration) {
        String body = "### \uD83C\uDF81 **GIVEAWAY ACTIVATED**\n" +
                "**Prize:** `" + prize + "`\n" +
                "**Winners:** `" + winners + "`\n" +
                "**Duration:** `" + duration + " minutes`\n\n" +
                "Click the button below to initialize your entry into the participant pool.";
        return containerBranded("SWEEPSTAKES", "Active Reward Distribution", body, BANNER_GIVEAWAY, Emoji.fromUnicode("\uD83C\uDF81"));
    }
}
