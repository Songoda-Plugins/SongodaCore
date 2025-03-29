package com.songoda.core.chat;

import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.utils.ColorUtils;

import java.awt.Color;

public class ColorContainer {
    private ColorCode colorCode;
    private String hexCode;

    public ColorContainer(ColorCode colorCode) {
        this.colorCode = colorCode;
        this.hexCode = null;
    }

    public ColorContainer(String hexCode, boolean noHex) {
        this.hexCode = hexCode;

        if (noHex || ServerVersion.isServerVersionBelow(ServerVersion.V1_16)) {
            this.colorCode = getColor();
            this.hexCode = null;
        }
    }

    public ColorCode getColorCode() {
        return this.colorCode;
    }

    public String getHexCode() {
        return this.hexCode;
    }

    public ColorCode getColor() {
        if (this.colorCode != null) {
            return this.colorCode;
        }

        if (this.hexCode == null) {
            return null;
        }

        java.awt.Color jColor = new Color(
                Integer.valueOf(this.hexCode.substring(0, 2), 16),
                Integer.valueOf(this.hexCode.substring(2, 4), 16),
                Integer.valueOf(this.hexCode.substring(4, 6), 16));

        return ColorUtils.fromRGB(jColor.getRed(), jColor.getGreen(), jColor.getBlue());
    }
}
