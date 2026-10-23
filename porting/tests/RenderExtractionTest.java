import com.dragonminez.platform.client.model.ExtractedGeoModel;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.Map;

public final class RenderExtractionTest {
    private static final class Animated implements GeoAnimatable {
        String variant = "first";
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { throw new AssertionError("Resource extraction requested an animation cache"); }
    }
    private static final class State implements GeoRenderState {
        private final Map<DataTicket<?>, Object> data = new HashMap<>();
        @Override public Map<DataTicket<?>, Object> getDataMap() { return data; }
    }
    private static final class Model extends ExtractedGeoModel<Animated> {
        int resolutions;
        @Override public Identifier getModelResource(Animated entity) { resolutions++; return Identifier.parse("test:geo/" + entity.variant); }
        @Override public Identifier getTextureResource(Animated entity) { return Identifier.parse("test:texture/" + entity.variant); }
        @Override public Identifier getAnimationResource(Animated entity) { return Identifier.parse("test:animation"); }
    }
    public static void main(String[] args) {
        var entity = new Animated(); var model = new Model(); var first = new State(); var second = new State();
        try { model.getModelResource(first); throw new AssertionError("Unextracted state accepted"); }
        catch (IllegalStateException expected) {}
        model.addAdditionalStateData(entity, null, first);
        entity.variant = "second";
        model.addAdditionalStateData(entity, null, second);
        if (!model.getModelResource(first).toString().equals("test:geo/first")) throw new AssertionError("Later state changed an earlier frame");
        if (!model.getTextureResource(first).toString().equals("test:texture/first")) throw new AssertionError("Texture did not follow its model snapshot");
        if (!model.getModelResource(second).toString().equals("test:geo/second")) throw new AssertionError("Variant change not captured");
        entity.variant = "third";
        for (int i = 0; i < 100; i++) model.getModelResource(second);
        if (model.resolutions != 2) throw new AssertionError("Rendering read live animatable data");
        System.out.println("Render extraction: model/texture snapshots, variant changes, frame isolation and render-stage independence passed.");
    }
}
