package com.highcore.bot;

import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.Label;
import net.dv8tion.jda.api.modals.Modal;

public class TestBuild {
    public static void test() {
        // JDA 6.4.1 GOLDEN PATTERN: TextInput -> Label wrapper
        TextInput input = TextInput.create("test", TextInputStyle.SHORT).build();
        Label label = Label.of(input).withLabel("Test").build();

        Modal.create("test", "test")
            .addActionRow(label)
            .build();
    }
}
