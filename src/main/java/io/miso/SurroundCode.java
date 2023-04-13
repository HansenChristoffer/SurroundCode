package io.miso;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * @author Christoffer "Miso" Hansen
 * @version 1.0
 * @implNote Surrounds selected text in the editor with user-defined code. Use $1 to tell plugin to use your selected text.
 * @see com.intellij.openapi.actionSystem.AnAction
 */
public class SurroundCode extends AnAction {

    /**
     * Called when the action is triggered, either by clicking on the action's icon or using the assigned shortcut.
     * This method surrounds the selected text in the editor with the code provided by the user.
     *
     * @param e AnActionEvent object providing context for the action, such as the editor and selected text.
     */
    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        final String selectedText = editor.getSelectionModel().getSelectedText();

        if (selectedText != null) {
            final String promptCode = showPopupCodeBlock(project).replace("$1", selectedText);

            WriteCommandAction.runWriteCommandAction(project, () -> {
                final Document document = editor.getDocument();
                document.replaceString(editor.getSelectionModel().getSelectionStart(),
                        editor.getSelectionModel().getSelectionEnd(), promptCode);
            });

        }
    }

    /**
     * Displays a dialog for the user to input a code block and returns the code block with a placeholder
     * for the selected text.
     *
     * @return The code block with a placeholder for the selected text.
     */
    private String showPopupCodeBlock(final Project project) {
        final StringBuilder stringBuilder = new StringBuilder();

        ApplicationManager.getApplication().invokeAndWait(() -> {
            final CodeBlockDialog codeBlockDialog = new CodeBlockDialog(project, "$1");
            if (codeBlockDialog.showAndGet()) {
                stringBuilder.setLength(0);
                stringBuilder.append(codeBlockDialog.getInputText());
            }
        });

        return stringBuilder.toString();
    }

    /**
     * A custom dialog to prompt the user for a code block to surround the selected text in the editor.
     */
    private static class CodeBlockDialog extends DialogWrapper {
        private JTextArea inputTextArea;

        /**
         * Constructs a new CodeBlockDialog with the given project and initial value for the input text area.
         *
         * @param project      The project in which the dialog is displayed.
         * @param initialValue The initial value for the input text area.
         */
        public CodeBlockDialog(@Nullable final Project project, final String initialValue) {
            super(project, true);
            init();
            setTitle("Code Block");

            inputTextArea.setText(initialValue);
            inputTextArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "insert-break");
            inputTextArea.getActionMap().put("insert-break", new AbstractAction() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    inputTextArea.append("\n");
                }
            });
        }

        /**
         * Creates the center panel for the dialog containing the input text area.
         *
         * @return The created center panel.
         */
        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            final JPanel panel = new JPanel(new BorderLayout());

            inputTextArea = new JTextArea();
            inputTextArea.setRows(10);
            inputTextArea.setColumns(40);
            inputTextArea.setLineWrap(true);
            inputTextArea.setWrapStyleWord(true);
            inputTextArea.setPreferredSize(new Dimension(400, 200));
            panel.add(inputTextArea, BorderLayout.CENTER);

            return panel;
        }

        /**
         * Creates actions for the dialog, including the OK action and a custom Enter key action.
         *
         * @return An array of actions for the dialog.
         */
        @Override
        protected Action @NotNull [] createActions() {
            final Action okAction = super.createActions()[0];

            final Action enterAction = new AbstractAction() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (isOKActionEnabled()) {
                        doOKAction();
                    }
                }
            };

            inputTextArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
            inputTextArea.getActionMap().put("enter", enterAction);

            return new Action[]{okAction};
        }

        /**
         * Specifies the component to receive focus when the dialog is displayed.
         *
         * @return The preferred focused component (inputTextArea).
         */
        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
            return inputTextArea;
        }

        /**
         * Gets the text entered by the user in the input text area.
         *
         * @return The text from the input text area.
         */
        public String getInputText() {
            return inputTextArea.getText();
        }
    }
}
