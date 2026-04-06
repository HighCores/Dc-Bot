package com.highcore.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import java.awt.Color;
import java.time.Instant;

public class EmbedUtil {
    public static final Color PRIMARY = new Color(0x5865F2);
    public static final Color SUCCESS = new Color(0x57F287);
    public static final Color WARNING = new Color(0xFEE75C);
    public static final Color DANGER = new Color(0xED4245);
    public static final Color INFO = new Color(0x3498DB);
    public static final Color BRAND = new Color(0x2B2D31);
    public static final Color ACCENT_PURPLE = new Color(0x9B59B6);
    public static final Color ACCENT_TEAL = new Color(0x1ABC9C);
    public static final Color GOLD = new Color(0xF1C40F);

    private static final String LOGO = env("BOT_LOGO_URL","");
    private static final String BANNER = env("BOT_BANNER_URL","");
    private static final String THUMB = env("BOT_THUMBNAIL_URL","");
    private static final String FICON = env("BOT_FOOTER_ICON_URL","");
    private static final String FOOTER = "Highcore Agency \u2022 IMY";
    private static final String INV = "\u200B";

    private static String env(String k,String d){String v=System.getenv(k);return(v!=null&&!v.isEmpty())?v:d;}

    public static EmbedBuilder base(){
        EmbedBuilder e=new EmbedBuilder().setTimestamp(Instant.now());
        if(!FICON.isEmpty())e.setFooter(FOOTER,FICON);else e.setFooter(FOOTER);return e;
    }
    public static EmbedBuilder branded(){
        EmbedBuilder e=base();if(!LOGO.isEmpty())e.setAuthor("Highcore Agency",null,LOGO);if(!THUMB.isEmpty())e.setThumbnail(THUMB);return e;
    }
    public static EmbedBuilder withBanner(){EmbedBuilder e=branded();if(!BANNER.isEmpty())e.setImage(BANNER);return e;}

    public static MessageEmbed success(String t,String d){return base().setColor(SUCCESS).setDescription("### \u2705 "+t+"\n"+d).build();}
    public static MessageEmbed error(String t,String d){return base().setColor(DANGER).setDescription("### \u274C "+t+"\n"+d).build();}
    public static MessageEmbed warning(String t,String d){return base().setColor(WARNING).setDescription("### \u26A0\uFE0F "+t+"\n"+d).build();}
    public static MessageEmbed info(String t,String d){return base().setColor(INFO).setDescription("### \u2139\uFE0F "+t+"\n"+d).build();}

    public static MessageEmbed mainMenu(){
        return withBanner().setColor(BRAND).setDescription("""
                ## Welcome to Highcore Agency
                Need help or want to place an order? Click a button below! \uD83D\uDCA1
                **Support Hours:**
                > \uD83D\uDD50 **Friday:** Closed
                > \uD83D\uDD50 **Saturday:** 9 AM \u2013 1 PM
                > \uD83D\uDD50 **Other Days:** 9 AM \u2013 6 PM
                > \uD83C\uDF10 All times are **UTC+3**
                Our team is ready to assist you!
                """).build();
    }

    public static MessageEmbed services(){
        return branded().setColor(ACCENT_PURPLE).setDescription("## \uD83D\uDE80 Our Services\nWe offer a full range of digital services.")
                .addField("\uD83D\uDCBB Web Dev","> Modern websites\n> Dashboards",true)
                .addField("\uD83D\uDCF1 Apps","> iOS & Android\n> Cross-platform",true).addField(INV,INV,true)
                .addField("\uD83C\uDFA8 Design","> Logos & branding\n> UI/UX",true)
                .addField("\uD83E\uDD16 Bots","> Custom Discord bots\n> Moderation",true).addField(INV,INV,true)
                .addField("\uD83D\uDCC8 Marketing","> Social media\n> Ads",true)
                .addField("\uD83D\uDEE1\uFE0F Security","> Server protection\n> DDoS mitigation",true).addField(INV,INV,true)
                .addField("","\uD83D\uDCE9 **Open a ticket for a quote!**",false).build();
    }

    public static MessageEmbed team(){
        return branded().setColor(ACCENT_TEAL).setDescription("""
                ## \uD83D\uDC65 Our Team
                \uD83C\uDFC6 **Management** \u2014 Direction & decisions
                \uD83D\uDEE0\uFE0F **Developers** \u2014 Building projects
                \uD83C\uDFA8 **Designers** \u2014 Crafting interfaces
                \uD83D\uDCCB **Support** \u2014 Handling tickets
                """).build();
    }

    public static MessageEmbed ticketPanel(){
        return withBanner().setColor(PRIMARY).setDescription("""
                ## \uD83C\uDFAB Highcore Agency \u2014 Support Center

                Need assistance? You're in the right place! Select a category below to open a ticket.

                > \uD83D\uDD27 **Technical Support** \u2014 Having a technical issue? We'll fix it
                > \u2753 **General Inquiry** \u2014 Questions about services, pricing, or anything else
                > \uD83D\uDED2 **Purchase Service** \u2014 Ready to buy? Let's get you started
                > \uD83D\uDCE6 **Order Status** \u2014 Check on your existing order
                > \uD83D\uDC54 **Management Application** \u2014 Want to lead? Apply here
                > \uD83D\uDC65 **Team Application** \u2014 Join the Highcore team

                **Before opening a ticket:**
                > \u25B8 Be respectful and patient
                > \u25B8 Provide clear details about your request
                > \u25B8 Don't open duplicate tickets
                > \u25B8 Our team typically responds within a few hours

                **Select a category from the dropdown below \uD83D\uDC47**
                """).build();
    }

    public static MessageEmbed ticketClaimed(String id,String by){
        return base().setColor(WARNING).setDescription("### \u270B Ticket #"+id+" Claimed\n> Claimed by **"+by+"**\n> Response coming soon! \uD83D\uDE80").build();
    }
    public static MessageEmbed ticketClosed(String id,String by){
        return base().setColor(DANGER).setDescription("### \uD83D\uDD12 Ticket #"+id+" Closed\n> Closed by **"+by+"**\n> Team can reopen if needed.").build();
    }
    public static MessageEmbed ticketReopened(String id,String by){
        return base().setColor(SUCCESS).setDescription("### \uD83D\uDD13 Ticket #"+id+" Reopened\n> Reopened by **"+by+"**").build();
    }

    public static MessageEmbed stats(int total,int open,int closed,int claimed,String top){
        return branded().setColor(BRAND).setDescription("## \uD83D\uDCCA Statistics")
                .addField("\uD83C\uDFAB Total","```"+total+"```",true).addField("\uD83D\uDFE2 Open","```"+open+"```",true)
                .addField("\uD83D\uDFE1 Claimed","```"+claimed+"```",true).addField("\uD83D\uDD34 Closed","```"+closed+"```",true)
                .addField("\uD83C\uDFC6 Top Staff",top!=null?top:"`No data`",true).addField(INV,INV,true).build();
    }

    public static MessageEmbed transcriptLog(String id,String user,String sub,String status,int msgs){
        String st=switch(status){case "open"->"\uD83D\uDFE2 Open";case "claimed"->"\uD83D\uDFE1 Claimed";case "closed"->"\uD83D\uDD34 Closed";default->"\u26AA Unknown";};
        return base().setColor(ACCENT_PURPLE).setDescription("## \uD83D\uDCDC Transcript #"+id+"\n> File attached below")
                .addField("\uD83D\uDC64 Client",user,true).addField("\uD83D\uDCCB Status",st,true)
                .addField("\uD83D\uDCAC Messages","**"+msgs+"**",true).addField("\uD83D\uDCDD Subject",sub,false).build();
    }

    public static MessageEmbed help(){
        return branded().setColor(PRIMARY).setDescription("""
                ## \uD83D\uDCD6 Bot Commands
                ### \uD83D\uDC64 General
                > `/menu` \u2014 Main menu
                > `/help` \u2014 This message
                ### \uD83C\uDFAB Tickets
                > `/stats` \u2014 Ticket stats
                ### \uD83D\uDCAC Auto-Reply *(Staff)*
                > `/autoreply add/remove/list`
                ### \u2699\uFE0F Admin
                > `/setup` \u2014 Bot panel
                > `/panel` \u2014 Custom panel
                > `/embed` \u2014 Custom embed
                > `/startup` \u2014 Startup guide
                > `/rename` \u2014 Rename channel
                > `/setchannel` \u2014 Set channel purpose
                ### \uD83E\uDD16 Auto
                > \u2705 Auto-role \u2022 \uD83C\uDF89 Welcome \u2022 \uD83D\uDD14 Logs \u2022 \u2B50 Rating \u2022 \u23F0 Reminders
                ### \uD83D\uDCDD Text
                > `!ai` / `!stop`
                """).build();
    }

    public static MessageEmbed ticket(String id,String user,String sub,String status,String pri){
        String st=switch(status){case "open"->"\uD83D\uDFE2 Open";case "claimed"->"\uD83D\uDFE1 Claimed";case "closed"->"\uD83D\uDD34 Closed";default->"\u26AA Unknown";};
        String pr=switch(pri){case "high"->"\uD83D\uDD34 High";case "medium"->"\uD83D\uDFE1 Medium";case "low"->"\uD83D\uDFE2 Low";default->"\u26AA Normal";};
        return branded().setColor(BRAND).setDescription("## \uD83C\uDFAB Ticket #"+id)
                .addField("\uD83D\uDC64 Client",user,true).addField("\uD83D\uDCCB Status",st,true)
                .addField("\u26A1 Priority",pr,true).addField("\uD83D\uDCDD Subject",sub,false).build();
    }

    // ===== RATING =====
    public static MessageEmbed ratingRequest(String ticketId){
        return base().setColor(GOLD).setDescription("## \u2B50 Rate Your Experience!\n> Ticket **#"+ticketId+"** has been closed.\n> How was our support?\n\nChoose your rating below \uD83D\uDC47").build();
    }
    public static MessageEmbed ratingThanks(int stars){
        return base().setColor(GOLD).setDescription("## Thank You! \uD83D\uDC9B\n> Rating: "+"\u2B50".repeat(stars)+" (**"+stars+"/5**)\n> We appreciate your feedback!").build();
    }

    // ===== CUSTOM EMBED =====
    public static MessageEmbed custom(String title,String desc,String color,String img,String thumb,
            String aName,String aIcon,String fText,String fIcon,
            String f1n,String f1v,Boolean f1i,String f2n,String f2v,Boolean f2i,String f3n,String f3v,Boolean f3i){
        EmbedBuilder e=new EmbedBuilder().setTimestamp(Instant.now());
        if(color!=null&&!color.isEmpty())try{e.setColor(new Color(Integer.parseInt(color.startsWith("#")?color.substring(1):color,16)));}catch(Exception x){e.setColor(BRAND);}else e.setColor(BRAND);
        if(title!=null&&!title.isEmpty())e.setTitle(title);
        if(desc!=null&&!desc.isEmpty())e.setDescription(desc.replace("\\n","\n"));
        if(img!=null&&!img.isEmpty())e.setImage(img);
        if(thumb!=null&&!thumb.isEmpty())e.setThumbnail(thumb);
        if(aName!=null&&!aName.isEmpty()){if(aIcon!=null&&!aIcon.isEmpty())e.setAuthor(aName,null,aIcon);else e.setAuthor(aName);}
        if(fText!=null&&!fText.isEmpty()){if(fIcon!=null&&!fIcon.isEmpty())e.setFooter(fText,fIcon);else e.setFooter(fText);}else e.setFooter(FOOTER);
        if(f1n!=null&&!f1n.isEmptycfdcd()&&f1v!=null)e.addField(f1n,f1v.replace("\\n","\n"),f1i!=null&&f1i);
        if(f2n!=null&&!f2n.isEmpty()&&f2v!=null)e.addField(f2n,f2v.replace("\\n","\n"),f2i!=null&&f2i);
        if(f3n!=null&&!f3n.isEmpty()&&f3v!=null)e.addField(f3n,f3v.replace("\\n","\n"),f3i!=null&&f3i);
        return e.build();
    }

    public static Color parseColor(String i){
        if(i==null||i.isEmpty())return BRAND;
        return switch(i.toLowerCase()){case "red","danger"->DANGER;case "green","success"->SUCCESS;case "blue","info"->INFO;
            case "yellow","warning"->WARNING;case "purple"->ACCENT_PURPLE;case "teal"->ACCENT_TEAL;case "gold"->GOLD;
            case "brand","dark"->BRAND;case "blurple","primary"->PRIMARY;
            default->{try{yield new Color(Integer.parseInt(i.startsWith("#")?i.substring(1):i,16));}catch(Exception e){yield BRAND;}}};
    }
}
