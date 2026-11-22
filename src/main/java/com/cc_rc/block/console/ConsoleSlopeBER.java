package com.cc_rc.block.console;

import com.cc_rc.block.console.draw.ConsoleShape;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 斜面控制台渲染器。
 *
 * 斜面本身不含任何数据：渲染时由斜面方块实体找到所属总线与宿主格子，
 * 把该格子内的组件 / 文字 / 绘图按斜面坐标系重画一遍（见 {@link ConsoleSlopeGeometry}）。
 * 因此总线那边看到的仍是「宿主表面上的组件」，斜面只是换了一个观察角度。
 */
public class ConsoleSlopeBER implements BlockEntityRenderer<ConsoleSlopeBlockEntity> {

    public ConsoleSlopeBER(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ConsoleSlopeBlockEntity slope, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockPos busPos = slope.getBusPos();
        BlockPos hostPos = slope.getHostPos();
        Direction hostFace = slope.getHostFace();
        if (busPos == null || hostPos == null || hostFace == null) {
            return;
        }
        if (!(slope.getLevel() != null
                && slope.getLevel().getBlockEntity(busPos) instanceof ConsoleBusBlockEntity bus)) {
            return;
        }
        // 未成型时没有表面坐标系，无法映射
        if (bus.getFormState() == ConsoleBusBlockEntity.State.UNFORMED || bus.getOrigin() == null) {
            return;
        }
        ConsoleFrame frame = ConsoleSlopeGeometry.frame(slope, bus);
        if (frame == null) {
            return;
        }

        // 只取宿主格子内的内容（其余部分由总线渲染器在真实表面上绘制）
        List<ConsoleBusBlockEntity.CompEntry> comps = new ArrayList<>();
        for (ConsoleBusBlockEntity.CompEntry comp : bus.getComps()) {
            if (comp.face == hostFace && ConsoleSlopeGeometry.inCell(bus, hostPos, hostFace,
                    comp.u, comp.v)) {
                comps.add(comp);
            }
        }
        List<ConsoleBusBlockEntity.TextEntry> texts = new ArrayList<>();
        for (ConsoleBusBlockEntity.TextEntry text : bus.getTexts()) {
            if (text.face == hostFace && ConsoleSlopeGeometry.inCell(bus, hostPos, hostFace,
                    text.u, text.v)) {
                texts.add(text);
            }
        }
        List<ConsoleShape> shapes = new ArrayList<>();
        for (ConsoleShape shape : bus.getDrawShapes(hostFace)) {
            if (ConsoleSlopeGeometry.shapeInCell(bus, hostPos, hostFace, shape)) {
                shapes.add(shape);
            }
        }

        ConsoleBusBER.renderMapped(bus, frame, comps, texts, shapes, poseStack, bufferSource);
    }
}
