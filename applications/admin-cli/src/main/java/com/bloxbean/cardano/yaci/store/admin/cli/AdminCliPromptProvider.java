package com.bloxbean.cardano.yaci.store.admin.cli;

import lombok.RequiredArgsConstructor;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminCliPromptProvider implements PromptProvider {
    @Override
    public AttributedString getPrompt() {
        return new AttributedString("yaci-store:>",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold());
    }
}
