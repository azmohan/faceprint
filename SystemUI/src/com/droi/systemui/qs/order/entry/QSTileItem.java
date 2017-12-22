package com.droi.systemui.qs.order.entry;

import java.io.Serializable;

public class QSTileItem implements Serializable {

    private static final long serialVersionUID = -6465237897027410019L;

    private String tileSpec;

    private String tileLabel;

    private Integer orderId;

    private Integer iconId;

    public QSTileItem() {
    }

    public QSTileItem(String tileSpec, String name, int iconId) {
        this(tileSpec, name, iconId, 0);
    }

    public QSTileItem(String tileSpec, String name, int iconId, int orderId) {
        this.tileSpec = tileSpec;
        this.tileLabel = name;
        this.iconId = Integer.valueOf(iconId);
        this.orderId = Integer.valueOf(orderId);
    }


    public String getTileSpec() {
        return this.tileSpec;
    }

    public String getTileLabel() {
        return this.tileLabel;
    }

    public int getOrderId() {
        return this.orderId.intValue();
    }

    public Integer getIconId() {
        return this.iconId;
    }

    public void setTileSpec(String spec) {
        this.tileSpec = spec;
    }

    public void setTileLabel(String paramString) {
        this.tileLabel = paramString;
    }

    public void setOrderId(int paramInt) {
        this.orderId = Integer.valueOf(paramInt);
    }
    public void setIconId(Integer iconId) {
        this.iconId = iconId;
    }

    public String toString() {
        return "QSTileItem [tileSpec =" + this.tileSpec + ", tileLabel=" + this.tileLabel
            + ", ordered =" + this.orderId + "]";
    }

}


