package com.cc_rc.item;

import net.minecraft.world.item.Item;

/**
 * 门禁卡物品
 * 不可堆叠（stacksTo(1) 由注册时属性控制）
 * grade 字段标记门禁卡等级：'A' / 'B' / 'C' / 'D' / 'E'
 */
public class CardItem extends Item {
    private final char grade;

    public CardItem(char grade, Properties properties) {
        super(properties);
        this.grade = grade;
    }

    public char getGrade() {
        return grade;
    }
}
