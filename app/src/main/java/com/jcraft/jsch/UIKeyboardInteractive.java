package com.jcraft.jsch;

public interface UIKeyboardInteractive {
    String[] promptKeyboardInteractive(String destination,
                                       String name,
                                       String instruction,
                                       String[] prompt,
                                       boolean[] echo);
}
