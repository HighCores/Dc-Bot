package com.highcore.bot;

import net.dv8tion.jda.api.components.textinput.TextInput;
import java.lang.reflect.Method;

public class TestMethods {
    public static void main(String[] args) {
        try {
            Class<?> builderClass = Class.forName("net.dv8tion.jda.api.components.textinput.TextInput$Builder");
            System.out.println("Methods in TextInput.Builder:");
            for (Method m : builderClass.getDeclaredMethods()) {
                System.out.println(m.getName() + "(" + java.util.Arrays.toString(m.getParameterTypes()) + ")");
            }
            
            Class<?> textInputClass = TextInput.class;
            System.out.println("\nMethods in TextInput:");
            for (Method m : textInputClass.getDeclaredMethods()) {
                System.out.println(m.getName() + "(" + java.util.Arrays.toString(m.getParameterTypes()) + ")");
            }

            Class<?> actionRowClass = Class.forName("net.dv8tion.jda.api.components.actionrow.ActionRow");
            System.out.println("\nMethods in ActionRow:");
            for (Method m : actionRowClass.getDeclaredMethods()) {
                System.out.println(m.getName() + "(" + java.util.Arrays.toString(m.getParameterTypes()) + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
