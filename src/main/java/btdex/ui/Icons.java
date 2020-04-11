package btdex.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;

import btdex.core.Constants;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.icons.font_awesome.FontAwesomeBrands;
import jiconfont.swing.IconFontSwing;

public class Icons {
    private Color COLOR;

    private Icon ICON_CONNECTED;
    private Icon ICON_DISCONNECTED;
    private Icon ICON_TESTNET;

    private Icon versionIcon;
    private Icon resetPinIcon;
    private Icon copyIcon;
    private Icon expIcon ;
    private Icon signoutIcon;
    private Icon githubIcon;
    private Icon discordIcon;
    private Icon iconBtdex;
    private Icon settingsIcon;
    private Icon sendIcon;
    private Icon langIcon;
    private Icon orderIcon;
    private Icon tradeIcon;
    private Icon accountIcon;
    private Icon chatIcon;
    private Icon transactionsIcon ;

    private int ICON_SIZE = Constants.ICON_SIZE;

    private Image icon, iconMono;

    //it is bad for re-usability
    public Icons() {
        try {
            icon = ImageIO.read(Main.class.getResourceAsStream("/icon.png"));
            iconMono = ImageIO.read(Main.class.getResourceAsStream("/icon-mono.png"));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Icons(Color COLOR) {
        this.COLOR = COLOR;
        initIconsDependedFromColor();
    }

    public void setColor(Color COLOR) {
       this.COLOR = COLOR;
       initIconsDependedFromColor();
    }

    private void initIconsDependedFromColor() {
        iconBtdex = IconFontSwing.buildIcon(FontAwesome.HEART, ICON_SIZE, COLOR);
        discordIcon = IconFontSwing.buildIcon(FontAwesomeBrands.DISCORD, ICON_SIZE, COLOR);
        resetPinIcon = IconFontSwing.buildIcon(FontAwesome.LOCK, ICON_SIZE, COLOR);
        versionIcon = IconFontSwing.buildIcon(FontAwesome.CODE_FORK, ICON_SIZE, COLOR);
        signoutIcon = IconFontSwing.buildIcon(FontAwesome.SIGN_OUT, ICON_SIZE, COLOR);
        githubIcon = IconFontSwing.buildIcon(FontAwesomeBrands.GITHUB, ICON_SIZE, COLOR);
        ICON_CONNECTED = IconFontSwing.buildIcon(FontAwesome.WIFI, ICON_SIZE, COLOR);
        ICON_TESTNET = IconFontSwing.buildIcon(FontAwesome.FLASK, ICON_SIZE, COLOR);
        ICON_DISCONNECTED = IconFontSwing.buildIcon(FontAwesome.EXCLAMATION, ICON_SIZE, COLOR);
        copyIcon = IconFontSwing.buildIcon(FontAwesome.CLONE, ICON_SIZE, COLOR);
        expIcon = IconFontSwing.buildIcon(FontAwesome.EXTERNAL_LINK, ICON_SIZE, COLOR);
        settingsIcon = IconFontSwing.buildIcon(FontAwesome.COG, ICON_SIZE, COLOR);
        langIcon = IconFontSwing.buildIcon(FontAwesome.LANGUAGE, ICON_SIZE, COLOR);
        sendIcon = IconFontSwing.buildIcon(FontAwesome.PAPER_PLANE, ICON_SIZE, COLOR);
        orderIcon = IconFontSwing.buildIcon(FontAwesome.BOOK, ICON_SIZE, COLOR);
        tradeIcon = IconFontSwing.buildIcon(FontAwesome.LINE_CHART, ICON_SIZE, COLOR);
        accountIcon = IconFontSwing.buildIcon(FontAwesome.USER_CIRCLE, ICON_SIZE, COLOR);
        chatIcon = IconFontSwing.buildIcon(FontAwesome.COMMENT, ICON_SIZE, COLOR);
        transactionsIcon = IconFontSwing.buildIcon(FontAwesome.LINK, ICON_SIZE, COLOR);
    }

    public Icon getOrderIcon() {
        return orderIcon;
    }

    public Icon getTradeIcon() {
        return tradeIcon;
    }

    public Icon getChatIcon() {
        return chatIcon;
    }

    public Icon getTransactionsIcon() {
        return transactionsIcon;
    }

    public Icon getAccountIcon() {
        return accountIcon;
    }

    public Icon getSettingsIcon() {
        return settingsIcon;
    }

    public Icon getLangIcon() {
        return langIcon;
    }

    public Icon getSendIcon() {
        return sendIcon;
    }

    public Icon getCopyIcon() {
        return copyIcon;
    }

    public Icon getExpIcon() {
        return expIcon;
    }

    public Icon getGithubIcon() {
        return githubIcon;
    }

    public Icon getICON_CONNECTED() {
        return ICON_CONNECTED;
    }

    public Icon getICON_DISCONNECTED() {
        return ICON_DISCONNECTED;
    }

    public Icon getICON_TESTNET() {
        return ICON_TESTNET;
    }

    public Icon getResetPinIcon() {
        return resetPinIcon;
    }

    public Icon getDiscordIcon() {
        return discordIcon;
    }

    public Image getIcon() {
        return icon;
    }

    public Icon getIconBtdex() {
        return iconBtdex;
    }

    public Image getIconMono() {
        return iconMono;
    }

    public Icon getVersionIcon() {
        return versionIcon;
    }

    public Icon getSignoutIcon() {
        return signoutIcon;
    }

    public int getIconSize() {
        return ICON_SIZE;
    }
}
