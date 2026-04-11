package com.highcore.bot;

import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;

public class TestBuild {
    public void test() {
        TextInput ti = TextInput.create("a", TextInputStyle.SHORT).build();
        Modal.create("c", "d").addComponents(Label.of("Field", ti)).build();
    }
}
