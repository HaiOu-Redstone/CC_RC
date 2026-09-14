package com.cc_rc.entity;

import com.cc_rc.CcRc;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 盖金蜗牛（gajin）渲染器：MobRenderer + GajinModel。
 * 贴图 textures/entity/gajin.png（重排后的 64×64 蜗牛贴图）。
 */
public class GajinRenderer extends MobRenderer<GajinSnail, GajinModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CcRc.MODID, "textures/entity/gajin.png");

    public GajinRenderer(EntityRendererProvider.Context context) {
        super(context, new GajinModel(context.bakeLayer(GajinModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(GajinSnail entity) {
        return TEXTURE;
    }
}