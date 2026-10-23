package com.dragonminez.client.init.entities.model.bioandroid;

import com.dragonminez.Reference;
import com.dragonminez.client.init.entities.model.sagas.DBSagaModel;
import com.dragonminez.common.init.entities.bioandroid.CellJrEntity;
import net.minecraft.resources.Identifier;

public class CellJrModel extends DBSagaModel<CellJrEntity> {
	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/sagas/saga_cell_jr.png");

	@Override
	public Identifier getTextureResource(CellJrEntity animatable) {
		return TEXTURE;
	}
}
