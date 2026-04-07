package com.highcore.bot;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;

public class TestBuild {
    public static void probe() {
        TextInput ti = TextInput.create("test", "Test", TextInputStyle.SHORT).build();
        Modal.create("test", "Test").addActionRow(ti).build();
        System.out.println("JDA 6.4.1 Components: PROBE SUCCESSFUL");
    }
}
