package br.ufscar.advanse.xtractpluginintellij;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.ScrollingModel;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

public class RefactoringRecommendationToolWindowFactory implements ToolWindowFactory {

    public static final Key<HtmlPanel> REFACTORING_RECOMMENDATION_PANEL_OBJ_KEY = Key.create("REFACTORING_RECOMMENDATION_PANEL_OBJ_KEY");
    public static final Key<Boolean> REFACTORING_RECOMMENDATION_HIGHLIGHT_KEY = Key.create("REFACTORING_RECOMMENDATION_HIGHLIGHT_KEY");

    private boolean highlightsVisible = true;
    private final HighlightData highlightData = new HighlightData();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        HtmlPanel htmlPanel = new HtmlPanel(project);

        Content content = ContentFactory.getInstance().createContent(htmlPanel, "", false);

        // Saves the reference for the Action to use later
        content.putUserData(REFACTORING_RECOMMENDATION_PANEL_OBJ_KEY, htmlPanel);

        toolWindow.getContentManager().addContent(content);
    }

    public class HtmlPanel extends JPanel {
        // Static key to identify our highlights
        private final JEditorPane htmlEditorPane;
        private final Project project;
        private final JButton toggleButton;

        public HtmlPanel(@NotNull Project project) {
            super(new BorderLayout());
            this.project = project;

            // --- PART 1: TOOL BAR (BUTTON) ---
            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            toggleButton = new JButton("Hide Recommended Fragment");
            toggleButton.addActionListener(e -> handleToggleAction());
            toolbar.add(toggleButton);
            add(toolbar, BorderLayout.NORTH);

            // --- PART 2: HTML CONTENT WITH WIDTH ADJUSTMENT ---
            htmlEditorPane = new JEditorPane();
            htmlEditorPane.setContentType("text/html");
            htmlEditorPane.setEditable(false);
            htmlEditorPane.setEditorKit(new HTMLEditorKit());

            String htmlContent = "<p>No refactoring recommendation.</p>";
            htmlEditorPane.setText(htmlContent);

            // Custom JScrollPane to force word wrap
            JScrollPane scrollPane = new JBScrollPane(htmlEditorPane) {
                // Forces fitting to the window width
                public boolean getScrollableTracksViewportWidth() {
                    return true;
                }
            };

            add(scrollPane, BorderLayout.CENTER);
        }

        private void handleToggleAction() {
            if (highlightsVisible) {
                System.out.println("Removing code highlight");
                if (!removeHighlights()) return;
                toggleButton.setText("Show Recommended Fragment");
                highlightsVisible = false;
            } else {
                System.out.println("Applying code highlight");
                applyHighlights();
                toggleButton.setText("Hide Recommended Fragment");
                highlightsVisible = true;
            }
        }

        private void applyHighlights() {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null) return;

            MarkupModel markupModel = editor.getMarkupModel();

            RangeHighlighter rangeHighlighter = markupModel.addRangeHighlighter(
                    highlightData.marker.getStartOffset(),
                    highlightData.marker.getEndOffset(),
                    HighlighterLayer.SELECTION - 1,
                    highlightData.attributes,
                    HighlighterTargetArea.EXACT_RANGE
            );

            // Mark the highlight with custom key
            rangeHighlighter.putUserData(REFACTORING_RECOMMENDATION_HIGHLIGHT_KEY, true);

            CaretModel caretModel = editor.getCaretModel();
            ScrollingModel scrollingModel = editor.getScrollingModel();

            int targetOffset = highlightData.marker.getEndOffset();

            // Ensures the offset does not exceed the document size
            // Validating the offset is crucial to avoid an IndexOutOfBoundsException
            if (targetOffset < 0) {
                targetOffset = 0;
            }
            if (targetOffset > editor.getDocument().getTextLength()) {
                targetOffset = editor.getDocument().getTextLength();
            }

            // Moves the cursor to the absolute offset
            caretModel.moveToOffset(targetOffset);

            // Focus the screen on the cursor position
            scrollingModel.scrollToCaret(ScrollType.CENTER_DOWN);
        }

        private boolean removeHighlights() {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor == null) return false;

            boolean wasHighlightRemoved = false;

            MarkupModel markupModel = editor.getMarkupModel();
            for (RangeHighlighter highlighter : markupModel.getAllHighlighters()) {
                if (Boolean.TRUE.equals(highlighter.getUserData(REFACTORING_RECOMMENDATION_HIGHLIGHT_KEY))) {
                    highlightData.startOffset = highlighter.getStartOffset();
                    highlightData.endOffset = highlighter.getEndOffset();
                    highlightData.attributes = highlighter.getTextAttributes();
                    highlightData.marker = editor.getDocument().createRangeMarker(highlighter.getStartOffset(), highlighter.getEndOffset());
                    markupModel.removeHighlighter(highlighter);

                    wasHighlightRemoved = true;
                }
            }

            return wasHighlightRemoved;
        }

        /**
         * Update HTML content
         */
        public void updateContent(String html) {
            SwingUtilities.invokeLater(() -> {
                htmlEditorPane.setText(html);
                htmlEditorPane.setCaretPosition(0);
            });
        }
    }
}
