package cn.yiiguxing.plugin.translate;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public class TranslationDialogManager {

    private final static TranslationDialogManager sInstance = new TranslationDialogManager();

    private TranslationDialog myShowingDialog;

    private TranslationDialogManager() {
    }

    public static TranslationDialogManager getInstance() {
        return sInstance;
    }

    /**
     * 显示对话框
     */
    public void showTranslationDialog(@Nullable Project project) {
        if (myShowingDialog == null) {
            myShowingDialog = new TranslationDialog(project);
            myShowingDialog.setOnDisposeListener(new TranslationDialog.OnDisposeListener() {
                @Override
                public void onDispose() {
                    myShowingDialog = null;
                }
            });
        }

        myShowingDialog.show();
    }

    /**
     * 更新当前显示的对话框
     */
    public void updateCurrentShowingTranslationDialog() {
        if (myShowingDialog != null) {
            myShowingDialog.update();
        }
    }

}
