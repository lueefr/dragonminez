package com.dragonminez.common.init.entities.sagas;

import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class SagaDodoriaEntity extends DBSagasEntity{

    public SagaDodoriaEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
		if (this instanceof IBattlePower bp) {
			bp.setBattlePower(22000);
		}

        this.setCanFly(true);
        this.setKiBlastSpeed(1.3F);
        this.addKiSkill(KiSkillType.OOZARU_BEAM, 200, 1.1F, 0xFFFBE8, 0xFFFBE8);
        this.setAuraColor(0XFF52E2);
        this.setDBZStyle(2);
    }
}
