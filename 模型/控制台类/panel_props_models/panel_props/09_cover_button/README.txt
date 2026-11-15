带玻璃罩的按钮（五面玻璃罩把按钮整个包住，玻璃真半透明）
  cover_button_closed.obj    罩子合上（0°），隔着玻璃能看到按钮
  cover_button_open.obj      罩子翻开 100°，按钮弹起
  cover_button_pressed.obj   罩子翻开 100°，按钮按下（压入 0.62）
  罩绕 (0.5000, 0.7125, -0.1813) 绕 X 轴转；合上凸出 0.29 格，翻开 0.60 格
  注意: obj 里有两个材质 —— usemtl prop(不透明) / usemtl glass(半透明, 用 glass.png)
        glass 要放进半透明批次(开混合、后画、不写深度)

贴图: knob_switch.png（.mtl 里的 map_Kd 已指向它；本文件夹自包含，直接丢进资源包即可）
坐标: 1 单位 = 1 方块, +Y 向上, 正面朝 -Z
