package io.github.blobanium.mcbrowser.screen;

import io.github.blobanium.mcbrowser.MCBrowser;
import io.github.blobanium.mcbrowser.feature.BrowserUtil;
import io.github.blobanium.mcbrowser.feature.specialbutton.SpecialButtonActions;
import io.github.blobanium.mcbrowser.feature.specialbutton.SpecialButtonHelper;
import io.github.blobanium.mcbrowser.util.BrowserScreenHelper;
import io.github.blobanium.mcbrowser.util.TabHolder;
import io.github.blobanium.mcbrowser.util.button.NewTabButton;
import io.github.blobanium.mcbrowser.util.button.ReloadButton;
import io.github.blobanium.mcbrowser.util.button.TabButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static io.github.blobanium.mcbrowser.MCBrowser.*;


public class BrowserScreen extends Screen {
    private static final int BROWSER_DRAW_OFFSET = 50;

    //Ui
    public TextFieldWidget urlBox;
    public ButtonWidget forwardButton;
    public ButtonWidget backButton;
    public ReloadButton reloadButton;
    private ButtonWidget homeButton;
    private PressableWidget[] navigationButtons;
    private ClickableWidget[] uiElements;
    public ArrayList<TabButton> tabButtons = new ArrayList<>();
    private NewTabButton newTabButton = null;

    private ButtonWidget specialButton;

    private ButtonWidget openInBrowserButton;

    public BrowserScreen(Text title) {
        super(title);

    }

    public void initTabs() {
        for (TabHolder tab : tabs) {
            int index = tabs.indexOf(tab);
            TabButton tabButton = new TabButton(BROWSER_DRAW_OFFSET, BROWSER_DRAW_OFFSET - 40, 100, 15, index);
            tabButtons.add(tabButton);
        }
        for (TabButton tabButton : tabButtons) {
            addSelectableChild(tabButton);
        }
    }

    public void removeTab(int index) {
        remove(tabButtons.get(index));
        tabButtons.get(index).resetIco();
        tabButtons.remove(index);
        updateTabButtonsIndexes(index);
    }

    public void addTab(int index) {
        TabButton tabButton = new TabButton(BROWSER_DRAW_OFFSET, BROWSER_DRAW_OFFSET - 40, 100, 15, index);
        tabButtons.add(index, tabButton);
        updateTabButtonsIndexes(index + 1);
        addSelectableChild(tabButton);
    }

    private void updateTabButtonsIndexes(int i) {
        while (i < tabButtons.size()) {
            tabButtons.get(i).setTab(i);
            i++;
        }
    }

    @Override
    protected void init() {
        super.init();
        BrowserScreenHelper.instance = this;

        newTabButton = new NewTabButton(BROWSER_DRAW_OFFSET, BROWSER_DRAW_OFFSET - 40, 15, 15, Text.of("+"));
        initTabs();

        initUrlBox();

        backButton = BrowserScreenHelper.initButton(Text.of("\u25C0"), button -> getCurrentTab().goBack(), BROWSER_DRAW_OFFSET, BROWSER_DRAW_OFFSET);
        forwardButton = BrowserScreenHelper.initButton(Text.of("\u25B6"), button -> getCurrentTab().goForward(), BROWSER_DRAW_OFFSET + 20, BROWSER_DRAW_OFFSET);
        reloadButton = new ReloadButton(BROWSER_DRAW_OFFSET + 40, BROWSER_DRAW_OFFSET - 20, 15, 15);
        homeButton = BrowserScreenHelper.initButton(Text.of("\u2302"), button -> {
            String prediffyedHomePage = BrowserUtil.prediffyURL(MCBrowser.getConfig().homePage);
            urlBox.setText(prediffyedHomePage);
            getCurrentTab().loadURL(prediffyedHomePage);
        }, BROWSER_DRAW_OFFSET + 60, BROWSER_DRAW_OFFSET);
        specialButton = ButtonWidget.builder(Text.of(""), button -> SpecialButtonHelper.onPress(getCurrentUrl())).dimensions(BROWSER_DRAW_OFFSET, height - BROWSER_DRAW_OFFSET + 5, 150, 15).build();
        openInBrowserButton = ButtonWidget.builder(Text.of("Open In External Browser"), button -> {
            try {
                Util.getOperatingSystem().open(new URL(getCurrentUrl()));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).dimensions(width - 200, height - BROWSER_DRAW_OFFSET + 5, 150, 15).build();

        navigationButtons = new PressableWidget[]{forwardButton, backButton, reloadButton, homeButton};
        uiElements = new ClickableWidget[]{forwardButton, backButton, reloadButton, homeButton, urlBox, specialButton, openInBrowserButton, newTabButton};
        for (ClickableWidget widget : uiElements) {
            addSelectableChild(widget);
        }
        updateWidgets();
    }

    public void updateWidgets() {
        urlBox.setText(getCurrentTab().getURL());
        backButton.active = getCurrentTab().canGoBack();
        forwardButton.active = getCurrentTab().canGoForward();
        reloadButton.setMessage(Text.of(getCurrentTab().isLoading() ? "\u274C" : "\u27F3"));
        SpecialButtonActions action = SpecialButtonActions.getFromUrlConstantValue(getCurrentUrl());
        if (action != null) {
            specialButton.setMessage(action.getButtonText());
        }
        getCurrentTab().resize(BrowserScreenHelper.scaleX(width, BROWSER_DRAW_OFFSET), BrowserScreenHelper.scaleY(height, BROWSER_DRAW_OFFSET));
    }

    @Override
    public void resize(MinecraftClient minecraft, int i, int j) {
        ArrayList<TabButton> tempList = new ArrayList<>(tabButtons);
        tabButtons.clear();
        super.resize(minecraft, i, j);
        resizeBrowser();
        updateWidgets();
        for (TabButton tabButton : tabButtons) {
            remove(tabButton);
        }
        tabButtons = tempList;
        for (TabButton tabButton : tabButtons) {
            addSelectableChild(tabButton);
        }

        for (ClickableWidget widget : uiElements) {
            if (!children().contains(widget)) {
                addSelectableChild(widget);
            }
        }
    }

    @Override
    public void close() {
        BrowserScreenHelper.instance = null;
        for (TabButton tabButton : tabButtons) {
            tabButton.resetIco();
        }
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (getCurrentTabHolder().isInit()) {
            getCurrentTab().render(BROWSER_DRAW_OFFSET, BROWSER_DRAW_OFFSET, this.width - BROWSER_DRAW_OFFSET * 2, this.height - BROWSER_DRAW_OFFSET * 2);
        } else {
            getCurrentTabHolder().init();
            resizeBrowser();
        }
        urlBox.renderButton(context, mouseX, mouseY, delta);
        for (PressableWidget button : navigationButtons) {
            button.render(context, mouseX, mouseY, delta);
        }
        if (SpecialButtonHelper.isOnCompatableSite(getCurrentUrl())) {
            specialButton.render(context, mouseX, mouseY, delta);
        }
        if (BrowserScreenHelper.tooltipText != null && BrowserScreenHelper.tooltipText.getBytes().length != 0) {
            setTooltip(Text.of(BrowserScreenHelper.tooltipText));
        }
        for (TabButton tabButton : tabButtons) {
            tabButton.render(context, mouseX, mouseY, delta);
        }
        newTabButton.render(context, mouseX, mouseY, delta);
        openInBrowserButton.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseButtonControl(mouseX, mouseY, button, true);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseButtonControl(mouseX, mouseY, button, false);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (MCBrowser.getConfig().asyncBrowserInput) {
            CompletableFuture.runAsync(() -> getCurrentTab().sendMouseMove(BrowserScreenHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserScreenHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET)));
        } else {
            getCurrentTab().sendMouseMove(BrowserScreenHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserScreenHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        BrowserScreenHelper.updateMouseLocation(mouseX, mouseY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (MCBrowser.getConfig().asyncBrowserInput) {
            CompletableFuture.runAsync(() -> getCurrentTab().sendMouseWheel(BrowserScreenHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserScreenHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET), delta, 0));
        } else {
            getCurrentTab().sendMouseWheel(BrowserScreenHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserScreenHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET), delta, 0);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    boolean tabPressFuse = false;

    private boolean isTabJustPressed() {
        if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_TAB)) {
            if (!tabPressFuse) {
                tabPressFuse = true;
                return true;
            }
        } else {
            tabPressFuse = false;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Screen.hasControlDown()) {
            if (isTabJustPressed()) {
                if (Screen.hasShiftDown()) {
                    if (activeTab == 0) {
                        setActiveTab(tabs.size() - 1);
                    } else {
                        setActiveTab(activeTab - 1);
                    }
                } else {
                    if (activeTab == tabs.size() - 1) {
                        setActiveTab(0);
                    } else {
                        setActiveTab(activeTab + 1);
                    }
                }
                setFocus();
                return true;
            } else if (tabPressFuse) {
                setFocus();
                return true;
            }
        }
        if (!urlBox.isFocused()) {
            if (MCBrowser.getConfig().asyncBrowserInput) {
                CompletableFuture.runAsync(() -> getCurrentTab().sendKeyPress(keyCode, scanCode, modifiers));
            } else {
                getCurrentTab().sendKeyPress(keyCode, scanCode, modifiers);
            }
        }

        //Set Focus
        setFocus();

        // Make sure screen isn't sending the enter key if the buttons aren't focused.
        if (!isButtonsFocused() && keyCode == GLFW.GLFW_KEY_ENTER) {
            return true;
        }

        if (keyCode == 256 && this.shouldCloseOnEsc()) { //Removed tab selection functional
            this.close();
            return true;
        } else {
            return this.getFocused() != null && this.getFocused().keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            tabPressFuse = false;
        } //have to set tabPressFuse to false manually cause of one tricky bug
        if (!Screen.hasControlDown() || keyCode != GLFW.GLFW_KEY_TAB) {
            if (MCBrowser.getConfig().asyncBrowserInput) {
                CompletableFuture.runAsync(() -> getCurrentTab().sendKeyRelease(keyCode, scanCode, modifiers));
            } else {
                getCurrentTab().sendKeyRelease(keyCode, scanCode, modifiers);
            }
        }
        setFocus();
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint == (char) 0) return false;
        if (MCBrowser.getConfig().asyncBrowserInput) {
            CompletableFuture.runAsync(() -> getCurrentTab().sendKeyTyped(codePoint, modifiers));
        } else {
            getCurrentTab().sendKeyTyped(codePoint, modifiers);
        }
        setFocus();
        return super.charTyped(codePoint, modifiers);
    }


    //Multi Override Util Methods
    public void setFocus() {
        boolean browserFocus = true;
        for (ClickableWidget widget : uiElements) {
            boolean mouseOver = widget.isMouseOver(BrowserScreenHelper.lastMouseX, BrowserScreenHelper.lastMouseY);
            widget.setFocused(mouseOver);
            if (mouseOver) {
                browserFocus = false;
            }
        }
        getCurrentTab().setFocus(browserFocus);
    }

    private void resizeBrowser() {
        if (width > 100 && height > 100) {
            for (TabHolder tab : tabs) {
                tab.getBrowser().resize(BrowserScreenHelper.scaleX(width, BROWSER_DRAW_OFFSET), BrowserScreenHelper.scaleY(height, BROWSER_DRAW_OFFSET));
            }
        }
        if (this.urlBox != null) {
            urlBox.setWidth(BrowserScreenHelper.getUrlBoxWidth(width, BROWSER_DRAW_OFFSET));
        }

        if (this.specialButton != null) {
            specialButton.setPosition(BROWSER_DRAW_OFFSET, height - BROWSER_DRAW_OFFSET + 5);
        }

        if (this.openInBrowserButton != null) {
            openInBrowserButton.setPosition(width - 200, height - BROWSER_DRAW_OFFSET + 5);
        }

    }

    private void mouseButtonControl(double mouseX, double mouseY, int button, boolean isClick) {
        if (mouseX > BROWSER_DRAW_OFFSET && mouseX < this.width - BROWSER_DRAW_OFFSET && mouseY > BROWSER_DRAW_OFFSET && mouseY < this.height - BROWSER_DRAW_OFFSET) {
            if (MCBrowser.getConfig().asyncBrowserInput) {
                CompletableFuture.runAsync(() -> sendMousePressOrRelease(mouseX, mouseY, button, isClick));
            } else {
                sendMousePressOrRelease(mouseX, mouseY, button, isClick);
            }
        }
        setFocus();
    }

    private void sendMousePressOrRelease(double mouseX, double mouseY, int button, boolean isClick) {
        if (isClick) {
            getCurrentTab().sendMousePress(BrowserScreenHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserScreenHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET), button);
        } else {
            getCurrentTab().sendMouseRelease(BrowserScreenHelper.mouseX(mouseX, BROWSER_DRAW_OFFSET), BrowserScreenHelper.mouseY(mouseY, BROWSER_DRAW_OFFSET), button);
        }
    }

    //Event Methods
    public void onUrlChange() {
        if (urlBox.isFocused()) {
            urlBox.setFocused(false);
        }
        urlBox.setText(getCurrentUrl());
        urlBox.setCursorToStart();
        SpecialButtonActions action = SpecialButtonActions.getFromUrlConstantValue(getCurrentUrl());
        if (action != null) {
            specialButton.setMessage(action.getButtonText());
        }
    }

    //Init Override
    private void initUrlBox() {
        this.urlBox = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, BROWSER_DRAW_OFFSET + 80, BROWSER_DRAW_OFFSET - 20, BrowserScreenHelper.getUrlBoxWidth(width, BROWSER_DRAW_OFFSET), 15, Text.of("")) {
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (isFocused()) {
                    for (TabHolder tab : tabs) {
                        tab.getBrowser().setFocus(false);
                    }
                    if (keyCode == GLFW.GLFW_KEY_ENTER) {
                        getCurrentTab().loadURL(BrowserUtil.prediffyURL(getText()));
                        setFocused(false);
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        };
        urlBox.setMaxLength(2048); //Most browsers have a max length of 2048
    }

    private boolean isButtonsFocused() {
        for (ClickableWidget widget : uiElements) {
            if (widget.isFocused()) {
                return true;
            }
        }
        return false;
    }
}

