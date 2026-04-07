package com.highcore.bot;

import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;

public class TestBuild {
    public static void test() {
        // JDA 6.4.1 GOLDEN PATTERN: Label.of("Text", input)
        TextInput input = TextInput.create("test", "test", TextInputStyle.SHORT).build();

        Modal.create("test", "test")
            .addComponents(Label.of("Test Input", input))
            .build();
    }
}
