package com.highcore.bot;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class TestBuild {
    public void test() {
        TextInput ti = TextInput.create("a", "b", TextInputStyle.SHORT).build();
        Modal.create("c", "d").addActionRow(ti).build();
    }
}
