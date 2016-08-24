package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.balloon.BalloonBuilder;
import cn.yiiguxing.plugin.translate.balloon.BalloonImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.Consumer;
import com.intellij.util.ui.AnimatedIcon;
import com.intellij.util.ui.JBEmptyBorder;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class TranslationBalloon implements TranslationView {

    private static final Icon ICON_PIN = IconLoader.getIcon("/pin.png");

    private static final int MIN_BALLOON_WIDTH = JBUI.scale(200);
    private static final int MIN_BALLOON_HEIGHT = JBUI.scale(50);
    private static final int MAX_BALLOON_SIZE = JBUI.scale(600);
    private static final JBInsets BORDER_INSETS = JBUI.insets(20, 20, 20, 20);

    private final JBPanel contentPanel;
    private final GroupLayout layout;

    private Balloon myBalloon;

    private final TranslationPresenter mTranslationPresenter;

    private final Editor editor;
    private JPanel processPanel;
    private AnimatedIcon processIcon;

    public TranslationBalloon(@NotNull Editor editor) {
        this.editor = Objects.requireNonNull(editor, "editor cannot be null");

        contentPanel = new JBPanel<>();
        layout = new GroupLayout(contentPanel);
        contentPanel.setOpaque(false);
        contentPanel.setLayout(layout);

        processPanel.setOpaque(false);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(processPanel, MIN_BALLOON_WIDTH, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(processPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        contentPanel.add(processPanel);
        processIcon.resume();

        mTranslationPresenter = new TranslationPresenterImpl(this);
    }

    private void createUIComponents() {
        processIcon = new ProcessIcon();
    }

    @NotNull
    private BalloonBuilder buildBalloon() {
        return BalloonBuilder.builder(contentPanel, null)
                .setHideOnClickOutside(true)
                .setShadow(true)
                .setHideOnKeyOutside(true)
                .setBlockClicksThroughBalloon(true)
                .setBorderInsets(BORDER_INSETS);
    }

    public void showAndQuery(@NotNull String queryText) {
        myBalloon = buildBalloon().setCloseButtonEnabled(false).createBalloon();
        myBalloon.show(JBPopupFactory.getInstance().guessBestPopupLocation(editor), Balloon.Position.below);
        mTranslationPresenter.query(Objects.requireNonNull(queryText, "queryText cannot be null"));
    }

    @Override
    public void updateHistory() {
        // do nothing
    }

    @Override
    public void showResult(@NotNull String query, @NotNull QueryResult result) {
        if (this.myBalloon != null) {
            if (this.myBalloon.isDisposed()) {
                return;
            }

            this.myBalloon.hide(true);
        }

        contentPanel.remove(0);
        processIcon.suspend();
        processIcon.dispose();

        JTextPane resultText = new JTextPane();
        resultText.setEditable(false);
        resultText.setBackground(UIManager.getColor("Panel.background"));
        resultText.setFont(JBUI.Fonts.create("Microsoft YaHei", JBUI.scaleFontSize(14)));

        Utils.insertQueryResultText(resultText.getDocument(), result);
        resultText.setCaretPosition(0);

        JBScrollPane scrollPane = new JBScrollPane(resultText);
        scrollPane.setBorder(new JBEmptyBorder(0));
        scrollPane.setVerticalScrollBar(scrollPane.createVerticalScrollBar());
        scrollPane.setHorizontalScrollBar(scrollPane.createHorizontalScrollBar());

        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane, MIN_BALLOON_WIDTH, GroupLayout.DEFAULT_SIZE, MAX_BALLOON_SIZE));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane, MIN_BALLOON_HEIGHT, GroupLayout.DEFAULT_SIZE, MAX_BALLOON_SIZE));
        contentPanel.add(scrollPane);

        final BalloonImpl balloon = (BalloonImpl) buildBalloon().createBalloon();
        RelativePoint showPoint = JBPopupFactory.getInstance().guessBestPopupLocation(editor);
        createPinButton(balloon, showPoint);
        balloon.show(showPoint, Balloon.Position.below);

        // 再刷新一下，尽可能地消除滚动条
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                balloon.revalidate();
            }
        });
    }

    private void createPinButton(final BalloonImpl balloon, final RelativePoint showPoint) {
        balloon.setActionProvider(new BalloonImpl.ActionProvider() {
            private BalloonImpl.ActionButton myPinButton;

            @NotNull
            public List<BalloonImpl.ActionButton> createActions() {
                myPinButton = balloon.new ActionButton(ICON_PIN, ICON_PIN, null,
                        new Consumer<MouseEvent>() {
                            @Override
                            public void consume(MouseEvent mouseEvent) {
                                if (mouseEvent.getClickCount() == 1) {
                                    balloon.hide(true);
                                    TranslationDialogManager.getInstance().show(editor.getProject());
                                }
                            }
                        });

                return Collections.singletonList(myPinButton);
            }

            public void layout(@NotNull Rectangle lpBounds) {
                if (myPinButton.isVisible()) {
                    int iconWidth = ICON_PIN.getIconWidth();
                    int iconHeight = ICON_PIN.getIconHeight();
                    int margin = JBUI.scale(3);
                    int x = lpBounds.x + lpBounds.width - iconWidth - margin;
                    int y = lpBounds.y + margin;

                    Rectangle rectangle = new Rectangle(x, y, iconWidth, iconHeight);
                    Insets border = balloon.getShadowBorderInsets();
                    rectangle.x -= border.left;

                    int showX = showPoint.getPoint().x;
                    int showY = showPoint.getPoint().y;
                    // 误差
                    int offset = JBUI.scale(1);
                    boolean atRight = showX <= lpBounds.x + offset;
                    boolean atLeft = showX >= (lpBounds.x + lpBounds.width - offset);
                    boolean below = lpBounds.y >= showY;
                    boolean above = (lpBounds.y + lpBounds.height) <= showY;
                    if (atRight || atLeft || below || above) {
                        rectangle.y += border.top;
                    }

                    myPinButton.setBounds(rectangle);
                }
            }
        });
    }

    @Override
    public void showError(@NotNull String error) {
        if (myBalloon == null)
            return;

        contentPanel.remove(0);
        processIcon.suspend();
        processIcon.dispose();

        JBLabel label = new JBLabel();
        label.setFont(JBUI.Fonts.label(16));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setText("Querying...");

        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(label, MIN_BALLOON_WIDTH, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(label, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        label.setForeground(new JBColor(new Color(0xFF333333), new Color(0xFFFF2222)));
        label.setText(error);
        contentPanel.add(label);

        myBalloon.revalidate();
    }
}
