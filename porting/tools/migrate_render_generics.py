from pathlib import Path
import re
# Explicit state types for Minecraft's extract/submit rendering pipeline.
# This only updates declarations; rendering method migrations remain separately reviewable.
for p in Path('src/main/java').rglob('*.java'):
 s=p.read_text();original=s
 types={
 'GeoEntityRenderer': ', net.minecraft.client.renderer.entity.state.LivingEntityRenderState',
 'GeoBlockRenderer': ', net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState',
 'GeoArmorRenderer': ', net.minecraft.client.renderer.entity.state.HumanoidRenderState',
 'GeoRenderLayer': ', Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState',
 'GeoRenderer': ', Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState',
 'ItemArmorGeoLayer': ', net.minecraft.client.renderer.entity.state.LivingEntityRenderState',
 'BlockAndItemGeoLayer': ', Void, net.minecraft.client.renderer.entity.state.LivingEntityRenderState',
 }
 for name,suffix in types.items():
  s=re.sub(r'\b'+name+r'<([^<>,]+)>',lambda m:name+'<'+m[1]+suffix+'>',s)
 # Vanilla renderer references carry their render-state type independently.
 s=re.sub(r'\bEntityRenderer<([^<>,]+)>',lambda m:'EntityRenderer<'+m[1]+', net.minecraft.client.renderer.entity.state.EntityRenderState>',s)
 s=s.replace('import net.minecraft.client.renderer.RenderType;', 'import net.minecraft.client.renderer.rendertype.RenderType;')
 if s!=original:p.write_text(s)
