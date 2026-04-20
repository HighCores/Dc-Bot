package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private static final String TICKET_CAT_ID = "1488795130881249404";
    private static final String TRANSCRIPT_CHANNEL_ID = "1488795131019526147";
    private static final String ADMIN_ROLE_ID = "1488795130008961040";

    private static final AtomicInteger TICKET_SEQ = new AtomicInteger(1);
    private static final Map<String, JsonObject> ticketCache = new ConcurrentHashMap<>();

    static {
        try {
            int current = SupabaseClient.getNextTicketNumber() - 1;
            TICKET_SEQ.set(current + 1);
        } catch (Exception e) {
            log.error("Failed to sync TICKET_SEQ: {}", e.getMessage());
        }
    }

    public static void createTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, String subject, String priority, String type) {
        createTicket(event, subject, priority, type, null);
    }

    public static void createTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, String subject, String priority, String type, String details) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null) {
            log.warn("Ticket category {} not found.", TICKET_CAT_ID);
            guild.createCategory("Agency Tickets").queue(c -> proceedWithTicket(event.getMember(), event.getUser(), c, subject, priority, type, details, event));
            return;
        }
        proceedWithTicket(event.getMember(), event.getUser(), cat, subject, priority, type, details, event);
    }

    private static void proceedWithTicket(Member member, User user, Category cat, String subject, String priority, String type, String details, net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event) {
        String ticketId = String.format("%04d", TICKET_SEQ.getAndIncrement());
        String name = type.toLowerCase() + "-" + ticketId;

        Role adminRole = cat.getGuild().getRoleById(ADMIN_ROLE_ID);
        var builder = cat.createTextChannel(name)
            .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES), null)
            .addPermissionOverride(cat.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL));

        if (adminRole != null) {
            builder = builder.addPermissionOverride(adminRole, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ATTACH_FILES), null);
        }

        builder.queue(channel -> {
            JsonObject ticket = new JsonObject();
            ticket.addProperty("ticket_id", ticketId);
            ticket.addProperty("user_id", user.getId());
            ticket.addProperty("channel_id", channel.getId());
            ticket.addProperty("subject", subject);
            ticket.addProperty("priority", priority);
            ticket.addProperty("type", type);
            if (details != null) ticket.addProperty("details", details);
            ticketCache.put(channel.getId(), ticket);
            
            channel.getManager().setTopic(subject + "|" + priority + "|" + type + "|" + user.getId()).queue();
            SupabaseClient.createTicket(ticketId, user.getId(), user.getEffectiveName(), channel.getId(), type, subject, priority);

            List<ContainerChildComponent> children = rebuildWelcomeComponents(ticket, false, channel, null);
            PanelService.reply(channel, Container.of(children));

            if (event != null) {
                if (event.isAcknowledged()) event.getHook().sendMessage("Your ticket has been opened: " + channel.getAsMention()).setEphemeral(true).queue();
                else event.reply("Your ticket has been opened: " + channel.getAsMention()).setEphemeral(true).queue();
            }
            LogManager.logEmbed(cat.getGuild(), Config.LOG_TICKETS, EmbedUtil.createOldLogEmbed("ticket-open", "Operation: Communications Initiative\nCase: #" + ticketId + "\nType: " + type + "\nSubject: " + subject + "\nChannel: " + channel.getAsMention(), null, user, member, EmbedUtil.SUCCESS));
        });
    }

    public static void createHighEndOrderTicket(Guild guild, User user, String pName, String cName, String contact, String eta, List<InvoiceService.OrderItem> items, String voucherCode) {
        String category = "Agency Projects";
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null) cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        if (cat == null) return;

        String ticketId = String.format("%04d", TICKET_SEQ.getAndIncrement());
        String name = "order-" + ticketId;

                Member member = guild.getMember(user);
                if (member == null) return;
                
                // 🎫 Voucher Validation & Discount Application
                double discountPercentage = 0;
                if (voucherCode != null && !voucherCode.isBlank()) {
                    JsonObject voucher = SupabaseClient.getVoucherByCode(voucherCode);
                    if (voucher != null && voucher.get("user_id").getAsString().equals(user.getId()) && !voucher.get("is_used").getAsBoolean()) {
                        discountPercentage = voucher.get("percentage").getAsInt();
                        double multiplier = (100.0 - discountPercentage) / 100.0;
                        for (var item : items) {
                            item.price = item.price * multiplier;
                        }
                        SupabaseClient.markVoucherAsUsed(voucherCode);
                    } else {
                        // If invalid voucher was entered, we null it so it doesn't show up in meta as "applied"
                        voucherCode = null;
                    }
                }

                guild.createTextChannel(name, cat)
                    .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)) 
                    .queue(channel -> {
                        JsonArray itemsArr = new JsonArray();
                        for (var i : items) {
                            JsonObject iObj = new JsonObject();
                            iObj.addProperty("name", i.name);
                            iObj.addProperty("price", i.price);
                            itemsArr.add(iObj);
                        }
        
                        JsonObject meta = new JsonObject();
                        meta.addProperty("client_name", cName);
                        meta.addProperty("project_name", pName);
                        meta.addProperty("contact", contact);
                        meta.addProperty("eta", eta);
                        meta.addProperty("category", category);
                        meta.addProperty("avatar_url", user.getEffectiveAvatarUrl());
                        meta.addProperty("display_name", user.getEffectiveName());
                        if (voucherCode != null && !voucherCode.isBlank()) {
                            meta.addProperty("voucher_code", voucherCode);
                            meta.addProperty("discount_applied", discountPercentage + "%");
                        }
                        meta.add("items", itemsArr);

                JsonObject ticket = new JsonObject();
                ticket.addProperty("ticket_id", ticketId);
                ticket.addProperty("user_id", user.getId());
                ticket.addProperty("channel_id", channel.getId());
                ticket.addProperty("subject", pName);
                ticket.addProperty("priority", "HIGH_END");
                ticket.addProperty("type", "ORDER");
                ticket.add("metadata", meta);
                ticketCache.put(channel.getId(), ticket);

                // Only set metadata in topic, remove description
                String initialTopic = "||META:" + meta.toString();
                channel.getManager().setTopic(initialTopic).queue();
                
                SupabaseClient.createTicket(ticketId, user.getId(), user.getEffectiveName(), channel.getId(), "ORDER", pName, "HIGH_END");

                List<ContainerChildComponent> children = rebuildWelcomeComponents(ticket, false, channel, null);
                channel.sendMessageComponents(Container.of(children)).useComponentsV2(true).queue();

                byte[] invoiceData = InvoiceService.generateInvoice(ticketId, cName, pName, items, false, user.getEffectiveAvatarUrl(), user.getEffectiveName(), category, contact);
                
                List<ContainerChildComponent> invChildren = new ArrayList<>();
                invChildren.add(TextDisplay.of("## 🧾 Invoice — Payment Required\nReview your order and choose a payment method."));
                invChildren.add(Separator.createDivider(Spacing.SMALL));
                invChildren.add(ActionRow.of(getPaymentButtons(ticketId)));

                String topicBase = "||META:";

                if (invoiceData != null) {
                    invChildren.add(0, MediaGallery.of(MediaGalleryItem.fromUrl("attachment://invoice-" + ticketId + ".png")));
                    
                    channel.sendMessageComponents(Container.of(invChildren)).useComponentsV2(true)
                        .addFiles(FileUpload.fromData(invoiceData, "invoice-" + ticketId + ".png"))
                        .queue(msg -> {
                            meta.addProperty("invoice_msg_id", msg.getId());
                            channel.getManager().setTopic(topicBase + meta.toString()).queue();
                        });
                } else {
                    log.warn("Invoice generation returned null. Sending fallback.");
                    channel.sendMessageComponents(Container.of(invChildren)).useComponentsV2(true).queue(msg -> {
                        meta.addProperty("invoice_msg_id", msg.getId());
                        channel.getManager().setTopic(topicBase + meta.toString()).queue();
                    });
                }
            });
    }

    public static void markAsPaid(TextChannel channel, String ticketId, Member staff) {
        JsonObject ticket = ticketCache.get(channel.getId());
        if (ticket == null) ticket = SupabaseClient.getTicketByChannel(channel.getId());
        
        JsonObject meta = null;
        if (ticket != null && ticket.has("metadata")) {
            meta = ticket.getAsJsonObject("metadata");
        } else if (channel.getTopic() != null && channel.getTopic().contains("||META:")) {
            try {
                String metaStr = channel.getTopic().substring(channel.getTopic().indexOf("||META:") + 7).trim();
                meta = com.google.gson.JsonParser.parseString(metaStr).getAsJsonObject();
            } catch (Exception ignored) {}
        }
        
        if (ticket == null || meta == null) return;

        String cName = meta.get("client_name").getAsString();
        String pName = meta.get("project_name").getAsString();
        String msgId = meta.has("invoice_msg_id") ? meta.get("invoice_msg_id").getAsString() : null;

        List<InvoiceService.OrderItem> items = new ArrayList<>();
        if (meta.has("items")) {
            for (JsonElement e : meta.getAsJsonArray("items")) {
                JsonObject i = e.getAsJsonObject();
                items.add(new InvoiceService.OrderItem(i.get("name").getAsString(), i.get("price").getAsDouble()));
            }
        }

        byte[] paidData = InvoiceService.generateInvoice(ticketId, cName, pName, items, true, 
                meta.get("avatar_url").getAsString(), meta.get("display_name").getAsString(), 
                meta.get("category").getAsString(), meta.get("contact").getAsString());

        if (paidData != null && msgId != null) {
            channel.retrieveMessageById(msgId).queue(msg -> {
                List<ContainerChildComponent> paidChildren = new ArrayList<>();
                paidChildren.add(MediaGallery.of(MediaGalleryItem.fromUrl("attachment://invoice-paid.png")));
                paidChildren.add(TextDisplay.of("## ✅ Payment Verified — Case #" + ticketId));
                paidChildren.add(Separator.createDivider(Spacing.SMALL));
                paidChildren.add(TextDisplay.of("Your payment has been successfully verified. The ticket is now unlocked.\nStaff: " + staff.getAsMention()));
                
                msg.editMessageComponents(Container.of(paidChildren)).useComponentsV2(true)
                    .setFiles(FileUpload.fromData(paidData, "invoice-paid.png"))
                    .queue();
            });
        }

        String ownerId = ticket.get("user_id").getAsString();
        Member owner = channel.getGuild().getMemberById(ownerId);
        if (owner != null) {
            channel.getPermissionOverride(owner).getManager().grant(Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES).queue();
        }
        PanelService.reply(channel, Container.of(TextDisplay.of("✅ **Payment Verified** · Ticket Unlocked")));
        LogManager.logEmbed(channel.getGuild(), Config.LOG_TICKETS, EmbedUtil.createOldLogEmbed("payment-verify", "Action: Financial Audit Finalized\nCase: #" + ticketId + "\nProject: " + pName + "\nChannel: " + channel.getAsMention(), staff, null, null, EmbedUtil.SUCCESS));
    }

    public static void claimTicket(TextChannel channel, Member claimer, net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        JsonObject ticket = ticketCache.get(channel.getId());
        if (ticket == null) {
            ticket = SupabaseClient.getTicketByChannel(channel.getId());
            if (ticket != null) ticketCache.put(channel.getId(), ticket);
        }
        
        if (ticket != null) {
            String tid = ticket.get("ticket_id").getAsString();
            SupabaseClient.claimTicket(tid, claimer.getEffectiveName());
            ticket.addProperty("status", "claimed");
            ticket.addProperty("claimer_id", claimer.getId());
        }

        channel.editMessageComponentsById(event.getMessageId(), Container.of(rebuildWelcomeComponents(ticket, true, channel, claimer.getAsMention()))).useComponentsV2(true).queue();
        
        // Notification
        PanelService.reply(channel, EmbedUtil.containerBranded("NOTICE", "Claimed", "📡 **Ticket Handled By:** " + claimer.getAsMention(), null));
        
        String tid = ticket != null ? ticket.get("ticket_id").getAsString() : "Unknown";
        LogManager.logEmbed(channel.getGuild(), Config.LOG_TICKETS, EmbedUtil.createOldLogEmbed("ticket-claim", "Action: Operational Authority Assignment\nCase: #" + tid + "\nChannel: " + channel.getAsMention(), claimer, null, null, EmbedUtil.SUCCESS));
    }

    public static void unclaimTicket(TextChannel channel, Member unclaimer, net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        JsonObject ticket = ticketCache.get(channel.getId());
        if (ticket != null && ticket.has("claimer_id") && !ticket.get("claimer_id").getAsString().equals(unclaimer.getId())) {
             event.getHook().sendMessage("⚠️ Only the staff who claimed this can unclaim.").setEphemeral(true).queue();
             return;
        }

        if (ticket != null) {
            String tid = ticket.get("ticket_id").getAsString();
            SupabaseClient.unclaimTicket(tid);
            ticket.addProperty("status", "open");
            ticket.remove("claimer_id");
        }

        channel.editMessageComponentsById(event.getMessageId(), Container.of(rebuildWelcomeComponents(ticket, false, channel, null))).useComponentsV2(true).queue();
        
        // Notification
        PanelService.reply(channel, EmbedUtil.containerBranded("NOTICE", "Unclaimed", "↩️ **Ticket Unclaimed By:** " + unclaimer.getAsMention(), null));
        
        String tid = ticket != null ? ticket.get("ticket_id").getAsString() : "Unknown";
        LogManager.logEmbed(channel.getGuild(), Config.LOG_TICKETS, EmbedUtil.createOldLogEmbed("ticket-unclaim", "Action: Authority Revocation\nCase: #" + tid + "\nChannel: " + channel.getAsMention(), unclaimer, null, null, EmbedUtil.WARNING));
    }

    public static List<ContainerChildComponent> rebuildWelcomeComponents(JsonObject ticket, boolean claimed, TextChannel channel, String claimerMention) {
        List<ContainerChildComponent> comps = new ArrayList<>();
        String ticketId = "0000", userId = "0", subject = "Active Session", priority = "MEDIUM", details = null;
        JsonObject metadata = null;

        if (ticket != null) {
            ticketId = ticket.has("ticket_id") ? ticket.get("ticket_id").getAsString() : ticketId;
            userId = ticket.has("user_id") ? ticket.get("user_id").getAsString() : userId;
            subject = ticket.has("subject") ? ticket.get("subject").getAsString() : subject;
            priority = ticket.has("priority") ? ticket.get("priority").getAsString() : priority;
            details = ticket.has("details") ? ticket.get("details").getAsString() : null;
            if (ticket.has("metadata")) metadata = ticket.getAsJsonObject("metadata");
        }
        
        // Fallback to topic for metadata if missing (after restart)
        if (metadata == null && channel.getTopic() != null && channel.getTopic().contains("||META:")) {
            try {
                String metaStr = channel.getTopic().substring(channel.getTopic().indexOf("||META:") + 7).trim();
                metadata = com.google.gson.JsonParser.parseString(metaStr).getAsJsonObject();
            } catch (Exception ignored) {}
        }
        
        String status = claimed ? "claimed" : "open";
        boolean isOrder = channel.getName().startsWith("order");

        String bannerUrl = isOrder ? EmbedUtil.BANNER_ORDER_TICKET : (subject.toLowerCase().contains("complaint") ? EmbedUtil.BANNER_COMPLAINT : EmbedUtil.BANNER_SUPPORT);
        String header = isOrder ? "Order Pipeline" : (subject.toLowerCase().contains("complaint") ? "Complaint Board" : "Support Center");

        comps.add(MediaGallery.of(MediaGalleryItem.fromUrl(bannerUrl)));
        comps.add(TextDisplay.of("## " + header + " | Case #" + ticketId + "\n<@&" + ADMIN_ROLE_ID + ">"));
        comps.add(Separator.createDivider(Spacing.SMALL));

        StringBuilder body = new StringBuilder();
        body.append("Welcome <@").append(userId).append("> \uD83D\uDC4B\n\n");
        body.append("**Priority:** `").append(priority).append("`\n");
        body.append("**Subject:** `").append(subject).append("`\n");

        if (details != null && !details.isBlank()) {
            body.append("**Details:** ").append(details).append("\n");
        }

        if (metadata != null) {
            body.append("\n### 📋 Questionnaire Data\n");
            if (metadata.has("project_name")) body.append("**Project:** `").append(metadata.get("project_name").getAsString()).append("`\n");
            if (metadata.has("client_name")) body.append("**Client:** `").append(metadata.get("client_name").getAsString()).append("`\n");
            if (metadata.has("contact")) body.append("**Contact:** `").append(metadata.get("contact").getAsString()).append("`\n");
            if (metadata.has("eta")) body.append("**ETA:** `").append(metadata.get("eta").getAsString()).append("`\n");
            
            if (metadata.has("items")) {
                body.append("**Services Requested:** ");
                List<String> itemList = new ArrayList<>();
                for (com.google.gson.JsonElement e : metadata.getAsJsonArray("items")) {
                    itemList.add(e.getAsJsonObject().get("name").getAsString());
                }
                body.append(String.join(", ", itemList)).append("\n");
            }
            
            if (metadata.has("voucher_code")) {
                body.append("**Voucher Applied:** `").append(metadata.get("voucher_code").getAsString()).append("` \uD83C\uDFAB\n");
                body.append("**Discount Benefit:** `").append(metadata.get("discount_applied").getAsString()).append(" OFF` \u2705\n");
            }
        }

        comps.add(TextDisplay.of(body.toString()));
        
        if (claimed && claimerMention != null) {
            comps.add(Separator.createDivider(Spacing.SMALL));
            comps.add(TextDisplay.of("\uD83D\uDCE1 **Ticket Handled By:** " + claimerMention));
        }
        
        comps.add(Separator.createDivider(Spacing.SMALL));

        if (isOrder && !claimed) {
            comps.add(TextDisplay.of("\u26A0\ufe0f **Your ticket is locked** — it will be unlocked once payment is confirmed."));
        } else if (claimed) {
            comps.add(TextDisplay.of("Our staff is now reviewing your request. Please wait a moment while we assist you."));
        } else {
            comps.add(TextDisplay.of("A staff member will be with you shortly — please describe your issue in full detail."));
        }

        List<Button> btns = new ArrayList<>(getTicketButtons(status));
        if (isOrder && !claimed) {
            btns.add(0, Button.success("ticket_mark_paid_" + ticketId, "Verify Payment").withEmoji(Emoji.fromUnicode("\u2705")));
        }
        comps.add(ActionRow.of(btns));

        return comps;
    }

    public static void reopenTicket(TextChannel channel, Member reopener) {
        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of("🔄 **Ticket Reopened** · " + reopener.getAsMention() + "\n`Back in queue — ready to assist.`"));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(getTicketButtons("open")));
        PanelService.reply(channel, Container.of(children));
        LogManager.logEmbed(channel.getGuild(), Config.LOG_TICKETS, EmbedUtil.createOldLogEmbed("ticket-reopen", "Action: Service Restoration\nChannel: " + channel.getAsMention(), reopener, null, null, EmbedUtil.SUCCESS));
    }

    public static void closeTicket(TextChannel channel, Member closer) {
        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of("⚠️ **Close Ticket** — requested by " + closer.getAsMention() + "\n\nChoose an action below:"));
        children.add(ActionRow.of(
            Button.success("order_status_update_TRANSCRIPT", "Save Transcript").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC")),
            Button.secondary("ticket_reopen", "Reopen Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDD04")),
            Button.danger("ticket_delete", "Delete Immediately").withEmoji(Emoji.fromUnicode("\uD83D\uDDD1\ufe0f"))
        ));
        PanelService.reply(channel, Container.of(children));
        LogManager.logEmbed(channel.getGuild(), Config.LOG_TICKETS, EmbedUtil.createOldLogEmbed("ticket-close-req", "Operation: Decommissioning Requested\nChannel: " + channel.getAsMention(), closer, null, null, EmbedUtil.WARNING));
    }

    public static void finalizeClose(TextChannel channel, Member closer, String status) {
        channel.getPermissionOverrides().stream().filter(po -> !Config.isStaff(po.getMember())).forEach(po -> po.delete().queue());
        List<ContainerChildComponent> closed = new ArrayList<>();
        closed.add(TextDisplay.of("🔒 **Ticket Closed** · `Channel is now locked.`"));
        closed.add(Separator.createDivider(Spacing.SMALL));
        closed.add(ActionRow.of(getTicketButtons("closed")));
        PanelService.reply(channel, Container.of(closed));

        if (status.equals("TRANSCRIPT")) {
            JsonObject ticket = SupabaseClient.getTicketByChannel(channel.getId());
            String ticketId = "0000";
            String openerId = "0";
            String openerName = "Unknown";
            
            if (ticket != null) {
                ticketId = ticket.get("ticket_id").getAsString();
                openerId = ticket.get("user_id").getAsString();
                openerName = ticket.get("user_name").getAsString();
            } else {
                // Fallback to topic: subject|priority|type|userId
                String topic = channel.getTopic();
                if (topic != null && topic.contains("|")) {
                    String[] parts = topic.split("\\|");
                    if (parts.length >= 4) {
                        openerId = parts[3];
                        // Try to find member for name
                        Member m = channel.getGuild().getMemberById(openerId);
                        if (m != null) openerName = m.getEffectiveName();
                        // For ticketId, we might not have it in topic, but we can try to guess from channel name
                        String name = channel.getName();
                        if (name.contains("-")) ticketId = name.split("-")[1];
                    }
                }
            }
            
            TextChannel logCh = channel.getGuild().getTextChannelById(TRANSCRIPT_CHANNEL_ID);
            if (logCh != null) {
                String url = "https://high-core-dc-bot-production.up.railway.app/view/transcript/" + ticketId;
                logCh.sendMessageComponents(EmbedUtil.containerBranded("TRANSCRIPT", "Archive — Case #" + ticketId, 
                    "**Opener:** <@" + openerId + "> (" + openerName + ")\n" +
                    "**Closed By:** " + closer.getAsMention(), 
                    EmbedUtil.BANNER_SUPPORT,
                    ActionRow.of(Button.link(url, "View Web Transcript").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC"))))).useComponentsV2(true).queue();
            }
            LogManager.logEmbed(channel.getGuild(), Config.LOG_TICKETS, EmbedUtil.createOldLogEmbed("ticket-finalize", "Operation: Terminal Session Terminated\nCase: #" + ticketId + "\nChannel: " + channel.getName() + " (Archived)\nOpener: <@" + openerId + ">", closer, null, null, EmbedUtil.DANGER));
        }
    }

    public static List<Button> getTicketButtons(String status) {
        if (status.equals("open")) return List.of(
            Button.primary("ticket_claim", "Claim Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDCE1")),
            Button.danger("ticket_close", "Close Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDD12"))
        );
        if (status.equals("claimed")) return List.of(
            Button.secondary("ticket_unclaim", "Unclaim Ticket").withEmoji(Emoji.fromUnicode("↩️")),
            Button.danger("ticket_close", "Close Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDD12"))
        );
        return List.of(
            Button.success("ticket_reopen", "Reopen Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDD04")),
            Button.danger("ticket_delete", "Delete Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDDD1\ufe0f"))
        );
    }

    public static List<Button> getPaymentButtons(String id) {
        return List.of(
            Button.secondary("pay_paypal_" + id, "PayPal").withEmoji(Emoji.fromUnicode("\uD83D\uDCB3")),
            Button.secondary("pay_stripe_" + id, "Stripe").withEmoji(Emoji.fromUnicode("\uD83C\uDF10")),
            Button.secondary("pay_bank_" + id, "Bank Transfer").withEmoji(Emoji.fromUnicode("\uD83C\uDFE6")),
            Button.secondary("pay_usdt_" + id, "USDT").withEmoji(Emoji.fromUnicode("\uD83D\uDCB0")),
            Button.secondary("pay_stcpay_" + id, "STC Pay").withEmoji(Emoji.fromUnicode("\uD83D\uDCF1"))
        );
    }
}
