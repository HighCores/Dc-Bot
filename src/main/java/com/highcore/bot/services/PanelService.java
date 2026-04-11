package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import java.util.*;

public class PanelService {

    // ── Core reply helpers ────────────────────────────────────────────────────
    public static void reply(Object target, Object content) { handleReply(target, content, false); }
    public static void replyEphemeral(Object target, Object content) { handleReply(target, content, true); }

    private static void handleReply(Object target, Object content, boolean ephemeral) {
        List<MessageTopLevelComponent> components = new ArrayList<>();
        if (content instanceof MessageTopLevelComponent mtc) {
            components.add(mtc);
        } else if (content instanceof List<?> list) {
            for (Object obj : list)
                if (obj instanceof MessageTopLevelComponent mtc) components.add(mtc);
        }
        if (components.isEmpty()) {
            components.add(EmbedUtil.info("High Core Agency", "No content provided."));
        }
        if (target instanceof IReplyCallback event) {
            if (event.isAcknowledged()) {
                event.getHook().editOriginalComponents(components).useComponentsV2(true).queue();
            } else {
                event.replyComponents(components).useComponentsV2(true).setEphemeral(ephemeral).queue();
            }
        } else if (target instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel) {
            channel.sendMessageComponents(components).useComponentsV2(true).queue();
        }
    }

    // ── Hub panels ────────────────────────────────────────────────────────────
    public static void sendStartupHub(IReplyCallback event) {
        ActionRow row = ActionRow.of(Button.secondary("hub_highcore", "Map"), Button.secondary("hub_about", "About"));
        reply(event, EmbedUtil.eliteContainer("High Core Agency", "Global operations hub.", null, row));
    }

    public static void sendServerMap(IReplyCallback event) {
        ActionRow row = ActionRow.of(Button.success("hub_pings", "Alerts"), Button.secondary("hub_rules", "Rules"));
        replyEphemeral(event, EmbedUtil.eliteContainer("Map", "Internal navigation active.", null, row));
    }

    public static void sendAboutUs(IReplyCallback event) {
        replyEphemeral(event, EmbedUtil.eliteContainer("About Us", "Elite creative identity.", null,
                ActionRow.of(Button.link("https://x.com/CoreHigh70331", "X"))));
    }

    public static void sendPartnersPanel(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("Partners", "Strategic collaborations.", null)); }
    public static void sendPingsPanel(IReplyCallback event)    { replyEphemeral(event, EmbedUtil.eliteContainer("Pings", "Select notification layers.", null)); }
    public static void sendStatsPanel(IReplyCallback event)    { replyEphemeral(event, EmbedUtil.eliteContainer("Telemetry", "Systems Operational.", null)); }
    public static void sendPricesCategory(IReplyCallback event){ replyEphemeral(event, EmbedUtil.eliteContainer("Pricing", "Service modules processing.", null)); }
    public static void sendServicesCategory(IReplyCallback event){ replyEphemeral(event, EmbedUtil.eliteContainer("Services", "Agency assets online.", null)); }

    // ── /tickets command ──────────────────────────────────────────────────────
    public static void sendTicketPanel(IReplyCallback event) {
        String imageUrl = "https://cdn.discordapp.com/attachments/1488900668042510568/1492305839736750230/IMG_20260411_023024.png" +
                          "?ex=69dad99d&is=69d9881d&hm=9df0283d5f26dc60385980e7f3d713966c15e2505d78aa2b9da35f9359901046&";
        String rules =
            "\uD83D\uDCDC **RULES & GUIDELINES**\n\n" +
            "**Mutual Respect** — Please respect all staff members. Any form of offensive behavior or harassment will not be tolerated.\n\n" +
            "**One Ticket** — Open only one ticket per issue. Do not open multiple tickets for the same problem.\n\n" +
            "**Clarity** — Please fully describe your issue or request before a staff member responds.\n\n" +
            "**Content** — Spam and external links are strictly prohibited without staff authorization.\n\n" +
            "**Mentions** — Pinging or mentioning the staff member inside the ticket is strictly forbidden.";

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        children.add(TextDisplay.of("## TICKET SUPPORT | High Core Agency"));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(TextDisplay.of(rules));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(
            Button.primary("ticket_init_support",  "\uD83D\uDCA1 Technical Support"),
            Button.success("ticket_init_order",    "\uD83D\uDED2 New Order"),
            Button.danger("ticket_init_complaint", "\u26A0\uFE0F File Complaint")
        ));
        reply(event, Container.of(children));
    }

    // ── Support flow ──────────────────────────────────────────────────────────
    public static void handleSupportFlow(IReplyCallback event) {
        if (event instanceof IModalCallback modal) {
            TextInput issueInput   = TextInput.create("issue_desc",   TextInputStyle.PARAGRAPH).build();
            TextInput serviceInput = TextInput.create("service_type", TextInputStyle.SHORT).build();
            modal.replyModal(Modal.create("modal_support_init", "Technical Support")
                .addComponents(Label.of("Describe your issue", issueInput))
                .addComponents(Label.of("Service (Designer/Dev/Editor/MC)", serviceInput))
                .build()).queue();
        }
    }

    // ── Complaint flow ────────────────────────────────────────────────────────
    public static void handleComplaintFlow(IReplyCallback event) {
        if (event instanceof IModalCallback modal) {
            TextInput issueTypeInput = TextInput.create("comp_type",   TextInputStyle.SHORT).build();
            TextInput personInput    = TextInput.create("comp_person", TextInputStyle.SHORT).build();
            TextInput descInput      = TextInput.create("comp_desc",   TextInputStyle.PARAGRAPH).build();
            modal.replyModal(Modal.create("modal_complaint_init", "File a Complaint")
                .addComponents(Label.of("Delivery issue or staff member?", issueTypeInput))
                .addComponents(Label.of("Staff / person involved (or N/A)", personInput))
                .addComponents(Label.of("Describe the issue in full detail", descInput))
                .build()).queue();
        }
    }

    // ── Order flow — Step 1: category buttons ─────────────────────────────────
    public static void handleOrderFlow(IReplyCallback event) {
        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(
            "## \uD83D\uDED2 New Order\n" +
            "Select your service category to view available services and pricing."));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(
            Button.secondary("order_cat_designer",  "\uD83C\uDFA8 Designer"),
            Button.secondary("order_cat_developer", "\uD83D\uDCBB Developer"),
            Button.secondary("order_cat_editor",    "\uD83C\uDFAC Editor & Animation"),
            Button.secondary("order_cat_minecraft", "\u26CF\uFE0F Minecraft Dev")
        ));
        replyEphemeral(event, Container.of(children));
    }

    // ── Order flow — Step 2: show service list for category ───────────────────
    public static void handleCategoryView(IReplyCallback event, String cat) {
        String title = switch (cat) {
            case "designer"  -> "\uD83C\uDFA8 Designer — Services & Pricing";
            case "developer" -> "\uD83D\uDCBB Developer — Services & Pricing";
            case "editor"    -> "\uD83C\uDFAC Editor & Animation — Services & Pricing";
            case "minecraft" -> "\u26CF\uFE0F Minecraft Developer — Services & Pricing";
            default          -> "Services & Pricing";
        };
        String list = switch (cat) {
            case "designer"  -> DESIGNER_LIST;
            case "developer" -> DEVELOPER_LIST;
            case "editor"    -> EDITOR_LIST;
            case "minecraft" -> MINECRAFT_LIST;
            default          -> "";
        };

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of("## " + title + "\n\n" + list));
        children.add(Separator.createDivider(Spacing.SMALL));
        // Category switcher + open ticket
        children.add(ActionRow.of(
            Button.secondary("order_cat_designer",  "\uD83C\uDFA8"),
            Button.secondary("order_cat_developer", "\uD83D\uDCBB"),
            Button.secondary("order_cat_editor",    "\uD83C\uDFAC"),
            Button.secondary("order_cat_minecraft", "\u26CF\uFE0F"),
            Button.success("order_meta_" + cat, "Open Order Ticket \u2192")
        ));
        reply(event, Container.of(children));
    }

    // ── Order flow — Step 3: modal with order details ─────────────────────────
    public static void handleOrderMetaModal(IReplyCallback event, String cat) {
        if (event instanceof IModalCallback modal) {
            String catLabel = switch (cat) {
                case "designer"  -> "Designer \uD83C\uDFA8";
                case "developer" -> "Developer \uD83D\uDCBB";
                case "editor"    -> "Editor & Animation \uD83C\uDFAC";
                case "minecraft" -> "Minecraft Dev \u26CF\uFE0F";
                default          -> "New Order";
            };
            TextInput nameInput     = TextInput.create("o_name",     TextInputStyle.SHORT).build();
            TextInput servicesInput = TextInput.create("o_services", TextInputStyle.PARAGRAPH).build();
            TextInput addonsInput   = TextInput.create("o_addons",   TextInputStyle.SHORT).build();
            TextInput contactInput  = TextInput.create("o_contact",  TextInputStyle.SHORT).build();
            TextInput etaInput      = TextInput.create("o_eta",      TextInputStyle.SHORT).build();
            modal.replyModal(Modal.create("modal_order_" + cat, "New Order — " + catLabel)
                .addComponents(Label.of("\u0627\u0633\u0645\u0643 \u0627\u0644\u0643\u0627\u0645\u0644 / Full Name",          nameInput))
                .addComponents(Label.of("\u0627\u0644\u062e\u062f\u0645\u0627\u062a \u0627\u0644\u0645\u0637\u0644\u0648\u0628\u0629 \u0645\u0646 \u0627\u0644\u0642\u0627\u0626\u0645\u0629", servicesInput))
                .addComponents(Label.of("\u0627\u0644\u0625\u0636\u0627\u0641\u0627\u062a \u0627\u0644\u0645\u0637\u0644\u0648\u0628\u0629 (\u0627\u062e\u062a\u064a\u0627\u0631\u064a)", addonsInput))
                .addComponents(Label.of("\u0627\u0644\u062a\u0648\u0627\u0635\u0644 (\u062c\u0648\u0627\u0644/\u0625\u064a\u0645\u064a\u0644)",                 contactInput))
                .addComponents(Label.of("\u0627\u0644\u0645\u062f\u0629 \u0627\u0644\u0645\u062a\u0648\u0642\u0639\u0629 \u0644\u0644\u062a\u0633\u0644\u064a\u0645",              etaInput))
                .build()).queue();
        }
    }

    // ── Service & pricing lists ───────────────────────────────────────────────
    private static final String DESIGNER_LIST =
        "**\u2726 \u0627\u0644\u062e\u062f\u0645\u0627\u062a \u0627\u0644\u0631\u0626\u064a\u0633\u064a\u0629**\n" +
        "`$30 `  \u0634\u0639\u0627\u0631\u0627\u062a \u00b7 Logo Design\n" +
        "`$60 `  \u0647\u0648\u064a\u0629 \u0628\u0635\u0631\u064a\u0629 \u0643\u0627\u0645\u0644\u0629 \u00b7 Full Visual Identity\n" +
        "`$90 `  \u0628\u0648\u0633\u062a\u0631\u0627\u062a \u0648\u0625\u0639\u0644\u0627\u0646\u0627\u062a \u00b7 Posters & Ads\n" +
        "`$20 `  \u062a\u0635\u0645\u064a\u0645 \u0633\u0648\u0634\u064a\u0627\u0644 \u0645\u064a\u062f\u064a\u0627 \u00b7 Social Media Design\n" +
        "`$20 `  \u0628\u0627\u0642\u0627\u062a \u062a\u0631\u062d\u064a\u0628 \u002f \u062f\u064a\u0633\u0643\u0648\u0631\u062f \u00b7 Discord Welcome Pack\n" +
        "`$30 `  \u0623\u063a\u0644\u0641\u0629 \u0648\u0628\u0627\u0646\u0631\u0627\u062a \u00b7 Covers & Banners\n" +
        "`$25 `  \u0645\u0637\u0628\u0648\u0639\u0627\u062a (\u0643\u0631\u0648\u062a / \u0628\u0631\u0648\u0634\u0648\u0631) \u00b7 Print\n" +
        "`$90 `  \u0645\u0648\u0634\u0646 \u062c\u0631\u0627\u0641\u064a\u0643 \u00b7 Motion Graphic\n" +
        "`$120`  \u062a\u0635\u0645\u064a\u0645 UI/UX\n" +
        "`$40 `  \u0625\u0646\u0641\u0648\u062c\u0631\u0627\u0641\u064a\u0643 \u00b7 Infographic\n" +
        "`$30 `  \u0625\u064a\u0645\u0648\u062c\u064a / \u0627\u0633\u062a\u064a\u0643\u0631\u0632 \u00b7 Emoji & Stickers\n\n" +
        "**\u2726 \u0627\u0644\u0625\u0636\u0627\u0641\u0627\u062a**\n" +
        "`+$45 `  \u062a\u0633\u0644\u064a\u0645 \u0633\u0631\u064a\u0639 \u00b7 Rush Delivery\n" +
        "`+$250`  \u0645\u0644\u0641\u0627\u062a \u0627\u0644\u0645\u0635\u062f\u0631 (AI/PSD) \u00b7 Source Files\n" +
        "`+$35 `  \u0646\u0633\u062e \u0645\u062a\u0639\u062f\u062f\u0629 \u0627\u0644\u0623\u0644\u0648\u0627\u0646 \u00b7 Color Variants\n" +
        "`+$200`  \u0625\u0636\u0627\u0641\u0629 \u0623\u0646\u064a\u0645\u064a\u0634\u0646 \u00b7 Add Animation\n" +
        "`+$35 `  \u062a\u0639\u062f\u064a\u0644\u0627\u0646 \u0628\u0639\u062f \u0627\u0644\u062a\u0633\u0644\u064a\u0645 \u00b7 2 Revisions After Delivery\n" +
        "`+$10 `  \u062d\u062c\u0645 \u0634\u0639\u0627\u0631 \u0625\u0636\u0627\u0641\u064a \u00b7 Additional Logo Size\n" +
        "`+$25 `  \u0635\u064a\u0627\u063a\u0629 \u0646\u0635\u0648\u0635 \u00b7 Copywriting";

    private static final String DEVELOPER_LIST =
        "**\u2726 \u0627\u0644\u062e\u062f\u0645\u0627\u062a \u0627\u0644\u0631\u0626\u064a\u0633\u064a\u0629**\n" +
        "`$50 `  \u0645\u0637\u0648\u0631 \u0645\u0648\u0627\u0642\u0639 \u00b7 Web Developer\n" +
        "`$50 `  \u0645\u0637\u0648\u0631 \u0628\u0648\u062a\u0627\u062a \u00b7 Bots Developer\n" +
        "`$100`  \u0645\u0637\u0648\u0631 \u0634\u0627\u0645\u0644 \u00b7 Full-Stack Developer\n" +
        "`$30 `  \u0648\u0627\u062c\u0647\u0627\u062a \u0623\u0645\u0627\u0645\u064a\u0629 \u00b7 Front-End\n" +
        "`$40 `  \u0623\u0646\u0638\u0645\u0629 \u062e\u0644\u0641\u064a\u0629 \u00b7 Back-End\n" +
        "`$100`  \u0630\u0643\u0627\u0621 \u0627\u0635\u0637\u0646\u0627\u0639\u064a \u0648\u0623\u062a\u0645\u062a\u0629 \u00b7 AI & Automation\n" +
        "`$30 `  \u0642\u0648\u0627\u0639\u062f \u0628\u064a\u0627\u0646\u0627\u062a \u00b7 Database Administrator\n\n" +
        "**\u2726 \u0627\u0644\u0625\u0636\u0627\u0641\u0627\u062a**\n" +
        "`+$70 `  \u062a\u0633\u0644\u064a\u0645 \u0633\u0631\u064a\u0639 \u00b7 Rush Delivery\n" +
        "`+$150`  \u0645\u0644\u0641\u0627\u062a \u0627\u0644\u0645\u0635\u062f\u0631 \u00b7 Source Files\n" +
        "`+$180`  \u062a\u0639\u062f\u064a\u0644\u0627\u0646 \u0628\u0639\u062f \u0627\u0644\u062a\u0633\u0644\u064a\u0645 \u00b7 2 Revisions After Delivery";

    private static final String EDITOR_LIST =
        "**\u2726 \u0627\u0644\u062e\u062f\u0645\u0627\u062a \u0627\u0644\u0631\u0626\u064a\u0633\u064a\u0629**\n" +
        "`$60 `  Reels / Shorts Editor\n" +
        "`$120`  Long-form Video Editor\n" +
        "`$150`  Animation Editor\n" +
        "`$150`  Gaming Editor\n\n" +
        "**\u2726 \u0627\u0644\u0625\u0636\u0627\u0641\u0627\u062a**\n" +
        "`+$45 `  \u062a\u0633\u0644\u064a\u0645 \u0633\u0631\u064a\u0639 \u00b7 Rush Delivery\n" +
        "`+$250`  \u0645\u0644\u0641\u0627\u062a \u0627\u0644\u0645\u0635\u062f\u0631 (AI/PSD) \u00b7 Source Files\n" +
        "`+$35 `  \u0646\u0633\u062e \u0645\u062a\u0639\u062f\u062f\u0629 \u0627\u0644\u0623\u0644\u0648\u0627\u0646 \u00b7 Color Variants\n" +
        "`+$200`  \u0625\u0636\u0627\u0641\u0629 \u0623\u0646\u064a\u0645\u064a\u0634\u0646 \u00b7 Add Animation\n" +
        "`+$35 `  \u062a\u0639\u062f\u064a\u0644\u0627\u0646 \u0628\u0639\u062f \u0627\u0644\u062a\u0633\u0644\u064a\u0645 \u00b7 2 Revisions After Delivery\n" +
        "`+$10 `  \u062d\u062c\u0645 \u0625\u0636\u0627\u0641\u064a \u00b7 Additional Size\n" +
        "`+$25 `  \u0635\u064a\u0627\u063a\u0629 \u0646\u0635\u0648\u0635 \u00b7 Copywriting";

    private static final String MINECRAFT_LIST =
        "**\u2726 \u0627\u0644\u062e\u062f\u0645\u0627\u062a \u0627\u0644\u0631\u0626\u064a\u0633\u064a\u0629**\n" +
        "`$50 `  Plugin Developer\n" +
        "`$80 `  Configuration Specialist\n" +
        "`$30 `  Map Maker / Builder\n" +
        "`$130`  Pixel Artist / Texture Creator\n" +
        "`$65 `  3D Modeler (Blockbench)\n" +
        "`$55 `  Technical Admin / SysAdmin\n\n" +
        "**\u2726 \u0627\u0644\u0625\u0636\u0627\u0641\u0627\u062a**\n" +
        "`+$45 `  \u062a\u0633\u0644\u064a\u0645 \u0633\u0631\u064a\u0639 \u00b7 Rush Delivery\n" +
        "`+$250`  \u0645\u0644\u0641\u0627\u062a \u0627\u0644\u0645\u0635\u062f\u0631 (AI/PSD) \u00b7 Source Files\n" +
        "`+$35 `  \u0646\u0633\u062e \u0645\u062a\u0639\u062f\u062f\u0629 \u0627\u0644\u0623\u0644\u0648\u0627\u0646 \u00b7 Color Variants\n" +
        "`+$200`  \u0625\u0636\u0627\u0641\u0629 \u0623\u0646\u064a\u0645\u064a\u0634\u0646 \u00b7 Add Animation\n" +
        "`+$35 `  \u062a\u0639\u062f\u064a\u0644\u0627\u0646 \u0628\u0639\u062f \u0627\u0644\u062a\u0633\u0644\u064a\u0645 \u00b7 2 Revisions After Delivery\n" +
        "`+$10 `  \u062a\u0639\u062f\u064a\u0644 \u0625\u0636\u0627\u0641\u064a \u00b7 Additional Modification\n" +
        "`+$25 `  \u0635\u064a\u0627\u063a\u0629 \u0646\u0635\u0648\u0635 \u00b7 Copywriting";
}
