package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaNappaEntity extends DBSagasEntity{

    public SagaNappaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(4000);
		}
        this.setCanFly(true);
        this.setWildSense(true, 200);
        this.setKiBlastSpeed(1.3F);
        this.addKiSkill(KiSkillType.OOZARU_BEAM, 200, 1.5F, 0xC523DE, 0XAF23DE);
        this.setAuraColor(0XF527AD);
        this.setDBZStyle(1);

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.20D);
        this.setDefaultMovementSpeed(0.20D);
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.20D);

    }


}
