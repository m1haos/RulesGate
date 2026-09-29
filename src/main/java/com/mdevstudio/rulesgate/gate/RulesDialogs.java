package com.mdevstudio.rulesgate.gate;

import com.mdevstudio.rulesgate.config.Rules;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.function.Consumer;
import net.kyori.adventure.text.event.ClickCallback;

public final class RulesDialogs {

    private static final int BUTTON_WIDTH = 150;

    private RulesDialogs() {
    }

    /**
     * Rules with accept and decline buttons. The answer is passed on once; the dialog can't be closed with Escape.
     */
    public static Dialog confirmation(Rules.Text text, Duration lifetime, Consumer<Boolean> answer) {
        ClickCallback.Options once = ClickCallback.Options.builder().uses(1).lifetime(lifetime).build();
        ActionButton accept = ActionButton.create(text.accept(), null, BUTTON_WIDTH,
                DialogAction.customClick((response, audience) -> answer.accept(true), once));
        ActionButton decline = ActionButton.create(text.decline(), null, BUTTON_WIDTH,
                DialogAction.customClick((response, audience) -> answer.accept(false), once));
        return Dialog.create(builder -> builder.empty()
                .base(base(text).canCloseWithEscape(false).build())
                .type(DialogType.confirmation(accept, decline)));
    }

    /**
     * Rules to read, with a single close button.
     */
    public static Dialog reading(Rules.Text text) {
        return Dialog.create(builder -> builder.empty()
                .base(base(text).build())
                .type(DialogType.notice(ActionButton.builder(text.close()).width(BUTTON_WIDTH).build())));
    }

    private static DialogBase.Builder base(Rules.Text text) {
        return DialogBase.builder(text.title())
                .body(text.body().stream().map(DialogBody::plainMessage).toList());
    }
}
