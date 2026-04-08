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

    public static final String BANNER_MAIN = "https://i.ibb.co/3ykfX5K/media-1775551414274.png";
    public static final String BANNER_SUPPORT = "https://i.ibb.co/3ykfX5K/media-1775551414274.png";
    public static final String BANNER_GIVEAWAY = "https://i.ibb.co/3ykfX5K/media-1775551414274.png";
    
    public static final Color SUCCESS = Color.decode("#D4AF37"); // Metallic Gold
    public static final Color DANGER = Color.decode("#8B0000");  // Deep Red for Danger
    public static final Color WARNING = Color.decode("#FFD700"); // Bright Gold
    public static final Color INFO = Color.decode("#C0C0C0");    // Silver for Info
    public static final Color GOLD = Color.decode("#D4AF37");
    public static final Color ACCENT_GOLD = Color.decode("#FFD700");

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

        layout.add(TextDisplay.of("### \u25C8 قسم " + sector.toUpperCase() + " \u30FB " + title.toUpperCase()));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));

        String content = (iconEmoji != null ? iconEmoji.getFormatted() + " " : "") + body;
        layout.add(TextDisplay.of(content));

        if (rows != null && rows.length > 0) {
            layout.add(Separator.createDivider(Separator.Spacing.SMALL));
            layout.addAll(Arrays.asList(rows));
            layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        layout.add(TextDisplay.of("` \u2022 نظام وكالة هايكور الموحد \u2022 GOLDEN TERMINAL 1.2.0 \u2022 `"));

        return Container.of(layout);
    }

    public static Container startupPanel(ActionRow... rows) {
        String body = "أهلاً بك في وكالة هايكور.\n" +
                "نقدم حلولاً رقمية احترافية في التصميم، البرمجة، المونتاج، وخدمات ماينكرافت.\n\n" +
                "تفضل ببدء تجربتك عبر القوائم أدناه.";
        return containerBranded("بداية", "مركز التحكم", body, BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDE80"), rows);
    }

    public static Container rulesPanel() {
        return containerBranded("PROTOCOL", "Compliance System", RULES_TEXT, BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDCDC"));
    }

    public static Container accessDenied() { return error("غير مصرح", "عذراً، لا تملك الصلاحيات الكافية للوصول لهذا القسم."); }
    public static Container error(String title, String description) { return containerBranded("خطأ", title, "[\u274C] " + description, BANNER_SUPPORT); }
    public static Container success(String title, String description) { return containerBranded("نجاح", title, "[\u2705] " + description, BANNER_MAIN); }
    public static Container info(String title, String description) { return containerBranded("معلومات", title, "[\u2139\uFE0F] " + description, BANNER_MAIN); }

    public static Container activityLog(String type, String details, Color color) { return containerBranded("سجل", type, details, BANNER_MAIN); }
    public static Container giveaway(String prize, int winners, int duration) {
        String body = "### \uD83C\uDF81 **مسابقة جديدة!**\n" +
                "**الجائزة:** `" + prize + "`\n" +
                "**الفائزون:** `" + winners + "`\n" +
                "**المدة:** `" + duration + " دقيقة`\n\n" +
                "اضغط على الزر أدناه للدخول في السحب.";
        return containerBranded("مسابقة", "توزيع المكافآت", body, BANNER_GIVEAWAY, Emoji.fromUnicode("\uD83C\uDF81"));
    }
}
