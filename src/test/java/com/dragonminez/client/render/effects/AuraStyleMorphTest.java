package com.dragonminez.client.render.effects;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraStyleMorphTest {
    @Test void meshAndDeformationParametersInterpolateTogetherAndCopiesAreDetached() {
        var from=new AuraStyle();from.meshWeights[0]=1;from.waveAmplitude=.2f;from.sizeY=1;
        var to=new AuraStyle();to.meshWeights[0]=0;to.meshWeights[2]=1;to.waveAmplitude=.8f;to.sizeY=2;
        var middle=AuraStyle.mix(from,to,.5f);
        assertArrayEquals(new float[]{.5f,0,.5f},middle.meshWeights,1e-6f);
        assertEquals(.5f,middle.waveAmplitude,1e-6f);assertEquals(1.5f,middle.sizeY,1e-6f);
        var bloom=new AuraStyle().copyFrom(middle);middle.meshWeights[0]=0;
        assertEquals(.5f,bloom.meshWeights[0],1e-6f);assertEquals(1,from.meshWeights[0]);assertEquals(1,to.meshWeights[2]);
    }
}
