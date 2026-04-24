package com.highcore.bot;

import com.highcore.bot.listeners.CentralInteractionListener;
import com.highcore.bot.commands.SlashCommands;
import com.highcore.bot.config.Config;
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
        // Common mocks to prevent NPEs during interaction handling
        when(buttonEvent.getUser()).thenReturn(mockUser);
        when(buttonEvent.getMember()).thenReturn(mockMember);
        when(buttonEvent.getGuild()).thenReturn(mockGuild);
        when(buttonEvent.getHook()).thenReturn(mockHook);
        when(buttonEvent.reply(anyString())).thenReturn(mockReplyAction);
        
        when(slashEvent.getUser()).thenReturn(mockUser);
        when(slashEvent.getMember()).thenReturn(mockMember);
        when(slashEvent.getGuild()).thenReturn(mockGuild);
        when(slashEvent.getHook()).thenReturn(mockHook);
        when(slashEvent.reply(anyString())).thenReturn(mockReplyAction);

        when(modalEvent.getUser()).thenReturn(mockUser);
        when(modalEvent.getMember()).thenReturn(mockMember);
        when(modalEvent.getGuild()).thenReturn(mockGuild);
        when(modalEvent.getHook()).thenReturn(mockHook);
        
        when(mockUser.getId()).thenReturn("1350531070222794804"); // agent_omar.dev
        when(mockMember.getUser()).thenReturn(mockUser);
    }

    @Test
    void testStartupButtonInteraction() {
        when(buttonEvent.getComponentId()).thenReturn("btn_highcore");
        
        CentralInteractionListener listener = new CentralInteractionListener();
        assertDoesNotThrow(() -> listener.onButtonInteraction(buttonEvent), 
            "Button interaction btn_highcore should not crash");
            
        verify(buttonEvent, atLeastOnce()).getComponentId();
        System.out.println("[AUDIT SUCCESS] Button interaction 'btn_highcore' verified.");
    }

    @Test
    void testStartupSlashCommand() {
        when(slashEvent.getName()).thenReturn("startup");
        
        SlashCommands slashCmds = new SlashCommands();
        assertDoesNotThrow(() -> slashCmds.onSlashCommandInteraction(slashEvent),
            "Slash command /startup should not crash");
            
        verify(slashEvent, atLeastOnce()).getName();
        System.out.println("[AUDIT SUCCESS] Slash command '/startup' verified.");
    }

    @Test
    void testBroadcastModalSubmission() {
        when(modalEvent.getModalId()).thenReturn("modal_bc");
        
        CentralInteractionListener listener = new CentralInteractionListener();
        // This might attempt to access a session, which we mock as null or empty
        assertDoesNotThrow(() -> listener.onModalInteraction(modalEvent),
            "Modal interaction modal_bc should handle missing sessions gracefully");
            
        verify(modalEvent, atLeastOnce()).getModalId();
        System.out.println("[AUDIT SUCCESS] Modal submission 'modal_bc' verified.");
    }
}
