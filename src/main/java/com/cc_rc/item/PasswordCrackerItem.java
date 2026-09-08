package com.cc_rc.item;

import net.minecraft.world.item.Item;

/**
 * 破解器物品
 * 手持本物品右键密码输入器（PasswordInputerBlock）可开始破解；
 * 破解计时/中断逻辑由 com.cc_rc.block.password_inputer.PasswordCrackManager 管理（服务端）。
 */
public class PasswordCrackerItem extends Item {
    public PasswordCrackerItem(Properties properties) {
        super(properties);
    }
}