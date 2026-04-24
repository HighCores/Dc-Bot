package com.highcore.bot;

import com.highcore.bot.listeners.CentralInteractionListener;
import com.highcore.bot.commands.SlashCommands;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BotInteractionTest {

    @Mock private ButtonInteractionEvent buttonEvent;
    @Mock private SlashCommandInteractionEvent slashEvent;
    @Mock private ModalInteractionEvent modalEvent;
    @Mock private User mockUser;
    @Mock private Member mockMember;
    @Mock private Guild mockGuild;
    @Mock private InteractionHook mockHook;
    @Mock private ReplyCallbackAction mockReplyAction;

    @BeforeEach
    void setUp() {
        when(buttonEvent.getUser()).thenReturn(mockUser);
        when(buttonEvent.getMember()).thenReturn(mockMember);
        when(buttonEvent.getGuild()).thenReturn(mockGuild);
        when(buttonEvent.getHook()).thenReturn(mockHook);
        when(buttonEvent.reply(any(net.dv8tion.jda.api.utils.messages.MessageCreateData.class))).thenReturn(mockReplyAction);
        when(buttonEvent.reply(anyString())).thenReturn(mockReplyAction);
        when(mockReplyAction.setEphemeral(anyBoolean())).thenReturn(mockReplyAction);
        
        when(slashEvent.getUser()).thenReturn(mockUser);
        when(slashEvent.getMember()).thenReturn(mockMember);
        when(slashEvent.getGuild()).thenReturn(mockGuild);
        when(slashEvent.getHook()).thenReturn(mockHook);
        when(slashEvent.reply(anyString())).thenReturn(mockReplyAction);

        when(modalEvent.getUser()).thenReturn(mockUser);
        when(modalEvent.getMember()).thenReturn(mockMember);
        when(modalEvent.getGuild()).thenReturn(mockGuild);
        when(modalEvent.getHook()).thenReturn(mockHook);
        
        when(mockUser.getId()).thenReturn("1350531070222794804");
        when(mockMember.getUser()).thenReturn(mockUser);
    }

    @Test
    void testPanelServiceV2Stability() {
        // This is the CRITICAL test to prevent IllegalStateException
        // We simulate sending a Container through PanelService
        var container = EmbedUtil.containerBranded("TEST", "Stability Audit", "Verifying V2 Builder state.", null);
        
        assertDoesNotThrow(() -> {
            // Using a helper to call handleReply via reflection or just use the public reply
            PanelService.reply(buttonEvent, container);
        }, "PanelService.reply with Container MUST NOT throw IllegalStateException. Builder must be in V2 mode.");
        
        System.out.println("[AUDIT SUCCESS] PanelService V2 Stability verified. No more builder crashes.");
    }

    @Test
    void testStartupButtonInteraction() {
        when(buttonEvent.getComponentId()).thenReturn("btn_highcore");
        CentralInteractionListener listener = new CentralInteractionListener();
        assertDoesNotThrow(() -> listener.onButtonInteraction(buttonEvent));
    }

    @Test
    void testStartupSlashCommand() {
        when(slashEvent.getName()).thenReturn("startup");
        SlashCommands slashCmds = new SlashCommands();
        assertDoesNotThrow(() -> slashCmds.onSlashCommandInteraction(slashEvent));
    }
}
