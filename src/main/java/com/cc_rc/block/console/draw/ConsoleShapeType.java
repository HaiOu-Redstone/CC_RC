package com.cc_rc.block.console.draw;

/**
 * 控制柜表面绘图工具的图形类型。
 *
 * 所有类型共用一组表面像素坐标（(u, v)，以表面矩形中心为原点，1 像素 = 1/16 格）：
 *  - RECT / RECT_OUTLINE：两点为矩形的对角；
 *  - LINE / ARROW：两点为端点（ARROW 的第二点为箭头尖端）；
 *  - PRESET：第一点为图案中心，图案由纯白 PNG 蒙版染色绘制。
 */
public enum ConsoleShapeType {
    /** 实心矩形（两点为对角）。 */
    RECT,
    /** 空心矩形（两点为对角，边框宽度由 width 决定）。 */
    RECT_OUTLINE,
    /** 线段（任意两端点）。 */
    LINE,
    /** 箭头：第一点为尾部，第二点为尖端（头部尺寸固定，只拉伸杆部）。 */
    ARROW,
    /** 预设图案（纯白 PNG 蒙版 + 顶点色染色）。 */
    PRESET;

    /** 按序号取值，越界时退回 RECT，避免损坏数据导致崩溃。 */
    public static ConsoleShapeType byOrdinal(int ordinal) {
        ConsoleShapeType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return RECT;
        }
        return values[ordinal];
    }

    /** 是否使用 width（线宽）字段。 */
    public boolean usesWidth() {
        return this == RECT_OUTLINE || this == LINE || this == ARROW;
    }
}
