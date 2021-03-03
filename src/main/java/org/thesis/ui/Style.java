package org.thesis.ui;

public enum Style {

    LIGHT_STYLE(Style.class.getResource("styles.css").toExternalForm()),
    DARK_STYLE(Style.class.getResource("styles.css").toExternalForm());


    public final String mStylesheet;

    Style(String mStylesheet) {
        this.mStylesheet = mStylesheet;
    }

    public static void invertStyle() {
        if (CURRENT_STYLE == LIGHT_STYLE)
            CURRENT_STYLE = DARK_STYLE;
        else
            CURRENT_STYLE = LIGHT_STYLE;
    }

    private static Style CURRENT_STYLE = LIGHT_STYLE;

    public static String getStyle() {
        return CURRENT_STYLE.mStylesheet;
    }
}
