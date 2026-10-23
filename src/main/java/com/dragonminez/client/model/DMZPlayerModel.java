package com.dragonminez.client.model;

import com.dragonminez.client.render.effects.CandyEffect;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.Reference;
import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.render.util.RenderUtil;
import com.dragonminez.client.systems.kishare.KiSharePose;
import com.dragonminez.server.events.players.combat.StrikeCombos.MeteorBurstCombo;
import com.dragonminez.server.events.players.combat.StrikeCombos.MysticBallCombo;
import com.dragonminez.server.events.players.combat.StrikeCombos.DarknessEyeBeamCombo;
import com.dragonminez.common.init.entities.ki.GumBindEntity;
import com.dragonminez.server.events.players.combat.EvasionAttackHandler;
import com.dragonminez.server.events.players.combat.StrikeCombos.AndroidCombo;
import com.dragonminez.server.events.players.combat.StrikeCombos.BeamSwordSlashCombo;
import com.dragonminez.server.events.players.combat.StrikeCombos.DeathChaserCombo;
import com.dragonminez.server.events.players.combat.StrikeCombos.PerfectRushCombo;
import com.dragonminez.server.events.players.combat.StrikeCombos.SpiritBreakerCombo;
import com.dragonminez.client.util.SaiyanTailRules;
import com.dragonminez.client.util.SkinGathererProvider;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.geckolib.cache.object.BakedGeoModel;
import com.geckolib.constant.DataTickets;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.cache.object.GeoBone;
import com.geckolib.animation.AnimationState;
import com.geckolib.loading.math.MolangQueries;
import com.geckolib.model.GeoModel;
import com.geckolib.model.data.EntityModelData;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DMZPlayerModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoModel<T> {

    private static final String EXPLODE_ANIMATION = "base.explodecell";
    private static final float EXPLODE_RESET_EASE = 0.25f;
    private static final float EXPLODE_RESET_EPSILON = 0.01f;
    private static final Identifier BASE_DEFAULT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/human.geo.json");
    private static final Identifier BASE_SLIM = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/human_slim.geo.json");
    private static final Identifier MAJIN_FAT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majin.geo.json");
    private static final Identifier MAJIN_SLIM = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majin_slim.geo.json");
    private static final Identifier JANEMBA_SUPER = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/janemba_super.geo.json");
    private static final Identifier JANEMBA_IMPERFECT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/janemba_imperfect.geo.json");
    private static final Identifier JANEMBA_FAT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/janemba_fat.geo.json");

    private static final Identifier FROST_DEMON = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon.geo.json");
    private static final Identifier FROST_DEMON_SECOND = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_second.geo.json");
    private static final Identifier FROST_DEMON_THIRD = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_third.geo.json");
    private static final Identifier FROST_DEMON_FIFTH = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_fifth.geo.json");
    private static final Identifier FROSTDEMON_BUFFED = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_fp.geo.json");
    private static final Identifier FROSTDEMON_METALCORE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_metalcore.geo.json");

    private static final Identifier BIO_ANDROID = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid.geo.json");
    private static final Identifier BIO_ANDROID_SEMI = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_semi.geo.json");
    private static final Identifier BIO_ANDROID_PERFECT = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_perfect.geo.json");
    private static final Identifier BIO_ANDROID_ULTRA = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_ultra.geo.json");
    private static final Identifier BIO_ANDROID_XENO = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_xeno.geo.json");
    private static final Identifier BIO_ANDROID_XENO_FP = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_xenofp.geo.json");

    private static final Identifier OOZARU = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/oozaru.geo.json");
    private static final Identifier HUMAN_SAIYAN_BUFFED = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/hbuffed.geo.json");
    private static final Identifier HUMAN_SAIYAN_SLIM_BUFFED = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/hbuffed_slim.geo.json");
    private static final Identifier HUMAN_SAIYAN_FEMALE_BUFFED = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/hbuffed_fem.geo.json");
    private static final Identifier HUMAN_SAIYAN_BUFFED_G3 = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/hbuffedg3.geo.json");
    private static final Identifier HUMAN_SAIYAN_SLIM_BUFFED_G3 = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/hbuffed_slimg3.geo.json");
    private static final Identifier HUMAN_SAIYAN_FEMALE_BUFFED_G3 = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/hbuffed_femg3.geo.json");
    private static final Identifier GLIND_TRUE_FORM = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/glindtrueform.geo.json");
    private static final Identifier GLIND_TRANSCENDED = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/trascended.geo.json");
    private static final Identifier GLIND_TRANSCENDED_FEMALE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/trascended_fem.geo.json");
    private static final Identifier GLIND_DARK = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/glinddark.geo.json");

    private static final Identifier HUMAN_SAIYAN_4ARMS = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/h4arms.geo.json");
    private static final Identifier HUMAN_SAIYAN_4ARMS_SLIM = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/h4armsslim.geo.json");
    private static final Identifier HUMAN_SAIYAN_4ARMS_FEM = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/h4armsfem.geo.json");

    private static final Identifier CANDY_MODEL = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/candy.geo.json");
    private static final Identifier CANDY_TEXTURE = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/candy.png");

    private static final Map<Identifier, Boolean> FILE_EXISTS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Identifier> MODEL_RESOLUTION_CACHE = new ConcurrentHashMap<>();

    private static final Identifier ANIM_PRIMARY = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/movement.animation.json");
    private static final Identifier[] ANIM_FALLBACKS = {
        Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/combat.animation.json"),
        Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/ki.animation.json"),
        Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/transf.animation.json"),
        Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/skp.animation.json"),
        Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/evs.animation.json")
    };

    private static final Identifier PUPPET_ANIM_PRIMARY = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/sagas/saga_base.animation.json");
    private static final Identifier[] PUPPET_ANIM_FALLBACKS = {
        ANIM_PRIMARY,
        ANIM_FALLBACKS[0],
        ANIM_FALLBACKS[1],
        ANIM_FALLBACKS[2],
        ANIM_FALLBACKS[3],
        ANIM_FALLBACKS[4]
    };

    private final Identifier textureLocation;
    private final String customModel;
    private BakedGeoModel activeModel;

    public DMZPlayerModel(String raceName, String customModel) {
        this.customModel = customModel;
        this.textureLocation = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");
    }

    @Override
    public Identifier getModelResource(T player) {
        if (CandyEffect.isCandy(player)) {
            return CANDY_MODEL;
        }

        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
            Character character = data.getCharacter();
            Identifier current = resolveModel(player, data, character.getActiveForm(), customModelOf(character.getActiveFormData(), character.getActiveStackFormData()));
            return ModelMorph.select(player, data, current, look -> resolveModel(player, data, look.form(), customModelOf(look.formData(), look.stackData())));
        }).orElse(BASE_DEFAULT);
    }

    @Override
    public BakedGeoModel getBakedModel(Identifier location) {
        BakedGeoModel morph = ModelMorph.baked(location);
        BakedGeoModel model = morph != null ? morph : super.getBakedModel(ModelMorph.source(location, BASE_DEFAULT));
        if (model != this.activeModel) {
            this.getAnimationProcessor().setActiveModel(model);
            this.activeModel = model;
        }
        return model;
    }

    private static String customModelOf(FormConfig.FormData formData, FormConfig.FormData stackFormData) {
        if (stackFormData != null && Boolean.TRUE.equals(stackFormData.hasCustomModel()) && !stackFormData.getCustomModel().isEmpty()) {
            return stackFormData.getCustomModel().toLowerCase();
        }
        if (formData != null && Boolean.TRUE.equals(formData.hasCustomModel()) && !formData.getCustomModel().isEmpty()) {
            return formData.getCustomModel().toLowerCase();
        }
        return "";
    }

    private Identifier resolveModel(T player, StatsData data, String currentForm, String activeCustomModel) {
        Character character = data.getCharacter();
        String race = character.getRaceName().toLowerCase();
        String gender = character.getGender().toLowerCase();
        String playerModelName = player.getSkin().model().id();

        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
        String raceCustomModel = (raceConfig != null && raceConfig.hasCustomModel()) ? raceConfig.getCustomModel().toLowerCase() : "";
        boolean partnerModel = data.getFusedData() != null && data.getFusedData().usesPartnerModel(character);
        String ownSkinKey = raceCustomModel.isEmpty() ? race : raceCustomModel;
        int bodyType = partnerModel && character.getBodyType() == 0 && !SkinGathererProvider.isHumanoidKey(ownSkinKey) ? 1 : character.getBodyType();
        String fallbackCustomModel = this.customModel != null ? this.customModel.toLowerCase() : "";
        String formKey = currentForm != null ? currentForm.toLowerCase() : "";
        boolean isSlimSkin = playerModelName.contains("slim");
        boolean slimBody = bodyType == 0 ? isSlimSkin : raceConfig != null && raceConfig.isSlimBodyType(bodyType);
        String stateKey = String.join("|",
                race,
                gender,
                formKey,
                Integer.toString(bodyType),
                playerModelName,
                activeCustomModel,
                raceCustomModel,
                fallbackCustomModel,
                Boolean.toString(raceConfig != null && raceConfig.getHasGender()),
                Boolean.toString(slimBody)
        );

        return MODEL_RESOLUTION_CACHE.computeIfAbsent(stateKey, ignored -> {
            boolean isMale = gender.equals(Character.GENDER_MALE);
            boolean isBaseForm = currentForm == null || currentForm.isEmpty() || currentForm.equalsIgnoreCase("base");

            if (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) {
                return OOZARU;
            }

            String modelKey = "";
            if (!activeCustomModel.isEmpty()) {
                modelKey = activeCustomModel;
            } else if (!raceCustomModel.isEmpty()) {
                modelKey = raceCustomModel;
            } else if (!fallbackCustomModel.isEmpty()) {
                modelKey = fallbackCustomModel;
            }

            if (!modelKey.isEmpty()) {
                String customRaceGender = (raceConfig != null && raceConfig.getHasGender()) ? gender : "";
                if (modelKey.equals("finalbase")) {
                    switch (raceCustomModel.isEmpty() ? race : raceCustomModel) {
                        case "human", "saiyan":
                            if (!isMale) return MAJIN_SLIM;
                            return slimBody ? BASE_SLIM : BASE_DEFAULT;
                        case "majin": return isMale ? BASE_DEFAULT : MAJIN_SLIM;
                        case "namekian": return BASE_DEFAULT;
                        case "frostdemon": return FROST_DEMON;
                        case "bioandroid": return BIO_ANDROID_PERFECT;
                    }
                }
                return resolveCustomModel(modelKey, isSlimSkin, slimBody, isMale, bodyType, customRaceGender);
            }

            if (race.equals("bioandroid")) return isBaseForm ? BIO_ANDROID : BIO_ANDROID_PERFECT;
            if (race.equals("frostdemon")) return FROST_DEMON;
            if (race.equals("namekian")) return BASE_DEFAULT;

            if (race.equals("majin")) {
                if (bodyType == 2) return isMale ? BASE_DEFAULT : MAJIN_SLIM;
                if (!isMale) return MAJIN_SLIM;
                return MAJIN_FAT;
            }

            if (race.equals("human") || race.equals("saiyan")) {
                if (!isMale) return MAJIN_SLIM;
                return slimBody ? BASE_SLIM : BASE_DEFAULT;
            }

            if (!isMale) return MAJIN_SLIM;
            return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
        });
    }

    private static Identifier humanoidVariant(boolean slimBody, boolean isMale, int bodyType, Identifier wide, Identifier slim, Identifier female) {
        if (bodyType != 0 && !isMale) return female;
        return slimBody ? slim : wide;
    }

    private Identifier resolveCustomModel(String modelName, boolean isSlimSkin, boolean slimBody, boolean isMale, int bodyType, String customRaceGender) {
        String key = modelName.toLowerCase();

        switch (key) {
            // HUMAN & SAIYAN
            case "human":
            case "saiyan":
                if (!isMale) return MAJIN_SLIM;
                return slimBody ? BASE_SLIM : BASE_DEFAULT;
            case "oozaru": return OOZARU;
            case "ssj4gt":
            case "buffed":
                return humanoidVariant(slimBody, isMale, bodyType, HUMAN_SAIYAN_BUFFED, HUMAN_SAIYAN_SLIM_BUFFED, HUMAN_SAIYAN_FEMALE_BUFFED);
            case "ssj4d":
            case "buffedg3":
                return humanoidVariant(slimBody, isMale, bodyType, HUMAN_SAIYAN_BUFFED_G3, HUMAN_SAIYAN_SLIM_BUFFED_G3, HUMAN_SAIYAN_FEMALE_BUFFED_G3);
            case "4arms":
                return humanoidVariant(slimBody, isMale, bodyType, HUMAN_SAIYAN_4ARMS, HUMAN_SAIYAN_4ARMS_SLIM, HUMAN_SAIYAN_4ARMS_FEM);
            case "glindtrueform": return GLIND_TRUE_FORM;
            case "trascended": return isMale ? GLIND_TRANSCENDED : GLIND_TRANSCENDED_FEMALE;
            case "glinddark": return GLIND_DARK;

            // NAMEKIAN
            case "namekian": return BASE_DEFAULT;
            case "namekian_orange": case "namekian_buffed": return HUMAN_SAIYAN_BUFFED;

            // MAJIN
            case "majin":
                if (bodyType == 2) return isMale ? BASE_DEFAULT : MAJIN_SLIM;
                if(!isMale) return MAJIN_SLIM;
                return MAJIN_FAT;
            case "majin_super": return isMale ? BASE_DEFAULT : MAJIN_SLIM;
            case "majin_ultra": return isMale ? HUMAN_SAIYAN_BUFFED : HUMAN_SAIYAN_FEMALE_BUFFED;
            case "majin_evil": case "majin_kid": return isMale ? BASE_SLIM : MAJIN_SLIM;
            case "janemba_imperfect": return JANEMBA_IMPERFECT;
            case "janemba_fat": return JANEMBA_FAT;
            case "janemba_super": return JANEMBA_SUPER;

            // FROSTDEMON
            case "frostdemon": case "frostdemon_final": case "frostdemon_mecha": return FROST_DEMON;
            case "frostdemon_second": return FROST_DEMON_SECOND;
            case "frostdemon_fifth": return FROST_DEMON_FIFTH;
            case "frostdemon_fp": return FROSTDEMON_BUFFED;
            case "frostdemon_third": return FROST_DEMON_THIRD;
            case "frostdemon_metalcore": return FROSTDEMON_METALCORE;

            // BIOANDROID
            case "bioandroid": return BIO_ANDROID;
            case "bioandroid_base": return BIO_ANDROID;
            case "bioandroid_semi": return BIO_ANDROID_SEMI;
            case "bioandroid_perfect": return BIO_ANDROID_PERFECT;
            case "bioandroid_ultra": return BIO_ANDROID_ULTRA;
            case "bioandroid_xeno": return BIO_ANDROID_XENO;
            case "bioandroid_xenofp": return BIO_ANDROID_XENO_FP;

        }

        String suffix = (customRaceGender != null && !customRaceGender.isEmpty()) ? "_" + customRaceGender : "";
        Identifier customLoc = Identifier.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/" + modelName + suffix + ".geo.json");

        if (fileExists(customLoc)) return customLoc;
        return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
    }

    @Override
    public Identifier getTextureResource(T t) {
        if (CandyEffect.isCandy(t)) {
            return CANDY_TEXTURE;
        }
        return textureLocation;
    }

    @Override
    public Identifier getAnimationResource(T t) {
        if (t instanceof DMZRendererCache.NpcPuppet) return PUPPET_ANIM_PRIMARY;
        return ANIM_PRIMARY;
    }

    @Override
    public Identifier[] getAnimationResourceFallbacks(T t) {
        if (t instanceof DMZRendererCache.NpcPuppet) return PUPPET_ANIM_FALLBACKS;
        return ANIM_FALLBACKS;
    }

    private static boolean clipOwnsHead(String anim) {
        return MysticBallCombo.ANIMATION.equals(anim) || MeteorBurstCombo.ANIMATION.equals(anim)
                || DarknessEyeBeamCombo.ANIMATION.equals(anim) || DeathChaserCombo.ANIMATION.equals(anim)
                || AndroidCombo.ANIMATION.equals(anim)
                || EvasionAttackHandler.CANDY_BEAM_ANIMATION.equals(anim)
                || EvasionAttackHandler.GUM_BIND_ANIMATION.equals(anim) || GumBindEntity.BOUND_ANIMATION.equals(anim)
                || BeamSwordSlashCombo.ANIMATION.equals(anim) || PerfectRushCombo.ANIMATION.equals(anim) || SpiritBreakerCombo.ANIMATION.equals(anim);
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        boolean transfAnimPlaying = animatable instanceof IPlayerAnimatable pa && pa.dragonminez$getCurrentPlayingAnimation().startsWith("transf.");
        boolean actuallyTransforming = StatsProvider.get(StatsCapability.INSTANCE, animatable)
                .map(data -> data.getStatus().isActionCharging()).orElse(false);
        String playingAnim = animatable instanceof IPlayerAnimatable playing ? playing.dragonminez$getCurrentPlayingAnimation() : "";
        String kiAnim = animatable instanceof IPlayerAnimatable playing ? playing.dragonminez$getCurrentKiAnimation() : "";
        boolean ownsHead = clipOwnsHead(playingAnim) || clipOwnsHead(kiAnim);
        boolean skipHead = (transfAnimPlaying && actuallyTransforming) || ownsHead;

        float partialTick = animationState.getPartialTick();
        float bodyYaw = Mth.lerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
        float headYawDeg = Mth.wrapDegrees(Mth.lerp(partialTick, animatable.yHeadRotO, animatable.yHeadRot) - bodyYaw);
        float lookYaw = -headYawDeg * Mth.DEG_TO_RAD;
        float lookPitch = -Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot()) * Mth.DEG_TO_RAD;

        GeoBone head = this.getAnimationProcessor().getBone("head");
        GeoBone waist = this.getAnimationProcessor().getBone("waist");
        GeoBone root = this.getAnimationProcessor().getBone("root");
        GeoBone rightArm = this.getAnimationProcessor().getBone("right_arm");
        GeoBone leftArm = this.getAnimationProcessor().getBone("left_arm");

        if (head != null && !skipHead) {
            EntityModelData entityModelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            float lookPitchRad = entityModelData.headPitch() * Mth.DEG_TO_RAD;
            float lookYawRad = entityModelData.netHeadYaw() * Mth.DEG_TO_RAD;

            float waistRotX = (waist != null) ? waist.getRotX() : 0.0F;
            float waistRotY = (waist != null) ? waist.getRotY() : 0.0F;
            float rootRotX = (root != null) ? root.getRotX() : 0.0F;
            float rootRotY = (root != null) ? root.getRotY() : 0.0F;

            head.setRotX(Mth.clamp(lookPitchRad - waistRotX - rootRotX, -Mth.HALF_PI, Mth.HALF_PI));
            head.setRotY(lookYawRad - waistRotY - rootRotY);
        }

        if (animatable instanceof IPlayerAnimatable playerAnim && playerAnim.dragonminez$isShootingKi()) {
            GeoBone shootingArm = playerAnim.dragonminez$isShootingKiLeftHand() ? leftArm : rightArm;
            if (shootingArm != null) {
                shootingArm.setRotX(lookPitch + 1.5708F);
                shootingArm.setRotY(lookYaw);
            }
        }

        boolean isInCombat = animatable instanceof IPlayerAnimatable pa && pa.dragonminez$isPlayingCombatAnimation();

        if (isInCombat) {
            float walkSpeed = animatable.walkAnimation.speed(partialTick);
            float walkPos   = animatable.walkAnimation.position(partialTick);

            if (walkSpeed > 0.01f) {
                float amplitude = Mth.clamp(walkSpeed, 0f, 3f) * 0.15f;

                if (rightArm != null) rightArm.setRotX(rightArm.getRotX() + Mth.sin(walkPos + Mth.PI) * amplitude);
                if (leftArm != null) leftArm.setRotX(leftArm.getRotX() + Mth.sin(walkPos) * amplitude);
            }
        }

        float ageInTicks = (float) animatable.getTick(animatable);

        try {
            if (rightArm != null) RenderUtil.animateHand(animatable, rightArm, partialTick, ageInTicks);
            if (leftArm != null) RenderUtil.animateHand(animatable, leftArm, partialTick, ageInTicks);
        } catch (Exception ignored) {}

        StatsProvider.get(StatsCapability.INSTANCE, animatable).ifPresent(data -> {
            if (SaiyanTailRules.hasTail(data.getCharacter())) {
                SaiyanTailRules.applyWrapCurl(animatable, data.getStatus().isTailVisible(), this.getAnimationProcessor()::getBone);
            }
        });
        KiSharePose.apply(animatable, rightArm, leftArm, partialTick);
        applyBoobScale(animatable);
        clearExplosionHeadOffset(animatable);
    }

    private void clearExplosionHeadOffset(T animatable) {
        if (animatable instanceof IPlayerAnimatable playerAnim
                && EXPLODE_ANIMATION.equals(playerAnim.dragonminez$getCurrentPlayingAnimation())) return;

        GeoBone head = this.getAnimationProcessor().getBone("head");
        if (head == null) return;

        boolean settled = Math.abs(head.getPosX()) < EXPLODE_RESET_EPSILON
                && Math.abs(head.getPosY()) < EXPLODE_RESET_EPSILON
                && Math.abs(head.getPosZ()) < EXPLODE_RESET_EPSILON
                && Math.abs(head.getScaleX() - 1.0f) < EXPLODE_RESET_EPSILON
                && Math.abs(head.getScaleY() - 1.0f) < EXPLODE_RESET_EPSILON
                && Math.abs(head.getScaleZ() - 1.0f) < EXPLODE_RESET_EPSILON;
        if (settled) return;

        head.setPosX(Mth.lerp(EXPLODE_RESET_EASE, head.getPosX(), 0.0f));
        head.setPosY(Mth.lerp(EXPLODE_RESET_EASE, head.getPosY(), 0.0f));
        head.setPosZ(Mth.lerp(EXPLODE_RESET_EASE, head.getPosZ(), 0.0f));
        head.setScaleX(Mth.lerp(EXPLODE_RESET_EASE, head.getScaleX(), 1.0f));
        head.setScaleY(Mth.lerp(EXPLODE_RESET_EASE, head.getScaleY(), 1.0f));
        head.setScaleZ(Mth.lerp(EXPLODE_RESET_EASE, head.getScaleZ(), 1.0f));
    }


    private void applyBoobScale(T animatable) {
        GeoBone boobas = this.getAnimationProcessor().getBone("boobas");
        if (boobas == null) return;

        float factor = StatsProvider.get(StatsCapability.INSTANCE, animatable).map(data -> {
            Character c = data.getCharacter();
            String gender = c.getGender() != null ? c.getGender().toLowerCase() : "";
            boolean isFemale = gender.equals(Character.GENDER_FEMALE) || c.getBodyType() == 1;
            return isFemale ? Mth.clamp(c.getBoobScale(), 0.75f, 1.25f) : 1.0f;
        }).orElse(1.0f);

        float[] axis = computeBoobAxisScale(factor);
        boobas.setScaleX(axis[0]);
        boobas.setScaleY(axis[1]);
        boobas.setScaleZ(axis[2]);
    }

    public static float[] computeBoobAxisScale(float factor) {
        float delta = factor - 1.0f;
        return new float[]{
                1.0f + delta * 0.15f,
                1.0f + delta * 0.3f,
                1.0f + delta * 1.4f
        };
    }

    private boolean fileExists(Identifier location) {
        return FILE_EXISTS_CACHE.computeIfAbsent(location, loc ->
                Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()
        );
    }

    @Override
    public void applyMolangQueries(AnimationState<T> animationState, double animTime) {
        super.applyMolangQueries(animationState, animTime);
        T animatable = animationState.getAnimatable();
        boolean headAnimPlaying = animatable instanceof IPlayerAnimatable pa && (pa.dragonminez$getCurrentPlayingAnimation().startsWith("transf.")
                || pa.dragonminez$getCurrentPlayingAnimation().startsWith("ki."));
        boolean actuallyBusy = StatsProvider.get(StatsCapability.INSTANCE, animatable)
                .map(data -> data.getStatus().isActionCharging() || data.getStatus().isChargingKi()).orElse(false)
                || (animatable instanceof IPlayerAnimatable pa2 && pa2.dragonminez$isShootingKi());
        boolean skipHead = headAnimPlaying && actuallyBusy;

        // In GUI screens the O-fields (yBodyRotO, xRotO) are stale world values — the screen
        // only sets the current-tick fields. Using pt=1.0 makes lerp return the current value
        // directly, so the O-fields are ignored and there is no jitter.
        float pt = Minecraft.getInstance().screen != null ? 1.0f : Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float pitch   = Mth.lerp(pt, animatable.xRotO,     animatable.getXRot());
        float bodyYaw = Mth.lerp(pt, animatable.yBodyRotO, animatable.yBodyRot);
        float headYaw = Mth.lerp(pt, animatable.yHeadRotO, animatable.yHeadRot);

        float clampedPitch = skipHead ? 0F : -Mth.clamp(pitch, -85F, 85F);
        float relativeYaw  = -Mth.wrapDegrees(headYaw - bodyYaw);
        float clampedYaw   = skipHead ? 0F : Mth.clamp(relativeYaw, -90F, 90F);

        // GeckoLib 4.9: per-frame Molang values go through actor variables (structure preserved).
        final double headX = clampedPitch;
        final double headY = clampedYaw;
        MolangQueries.setActorVariable("query.head_x_rotation", actor -> headX);
        MolangQueries.setActorVariable("query.head_y_rotation", actor -> headY);
        MolangQueries.setActorVariable("query.head_pitch", actor -> headX);
        MolangQueries.setActorVariable("query.head_yaw", actor -> headY);
    }
}
