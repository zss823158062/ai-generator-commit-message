package com.github.jdami.aicommit.vcs;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CommitMessageGeneratorCheckinHandlerFactory extends CheckinHandlerFactory {

    @NotNull
    @Override
    public CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
        return new CheckinHandler() {
            @Override
            public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
                return new RefreshableOnComponent() {
                    private JPanel panel;

                    @Override
                    public JComponent getComponent() {
                        if (panel == null) {
                            panel = new JPanel();
                        }
                        return panel;
                    }

                    @Override
                    public void refresh() {}

                    @Override
                    public void saveState() {}

                    @Override
                    public void restoreState() {}
                };
            }
        };
    }
}
