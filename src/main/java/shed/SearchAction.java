package shed;

import com.intellij.ide.BrowserUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;

import java.util.Locale;

public class SearchAction extends AnAction
{
    /**
     * Convert selected text to a URL friendly string.
     * @param e the event
     */
    @Override
    public void actionPerformed(AnActionEvent e)
    {
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        final CaretModel caretModel = editor.getCaretModel();
        if (!caretModel.getCurrentCaret().hasSelection()) {
            return;
        }

        final String query = caretModel.getCurrentCaret().getSelectedText();
        if (query == null || query.isBlank()) {
            return;
        }

        // For searches from the editor, we should also get file type information
        // to help add scope to the search using the Stack overflow search syntax.
        //
        // https://stackoverflow.com/help/searching

        final String languageTag = buildLanguageTag(e);

        BrowserUtil.browse(
                "https://stackoverflow.com/search?q=" + query.replace(' ', '+') + languageTag
        );
    }

    private static String buildLanguageTag(AnActionEvent e)
    {
        final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        if (file == null) {
            return "";
        }

        final Language language = file.getLanguage();
        final String displayName = language.getDisplayName();
        if (displayName.isBlank()) {
            return "";
        }

        return "+[" + displayName.toLowerCase(Locale.ROOT) + "]";
    }

    /**
     * Only make this action visible when text is selected.
     * @param e the event
     */
    @Override
    public void update(AnActionEvent e)
    {
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        CaretModel caretModel = editor.getCaretModel();
        e.getPresentation().setEnabledAndVisible(caretModel.getCurrentCaret().hasSelection());
    }
}