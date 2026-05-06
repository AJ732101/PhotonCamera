package com.particlesdevs.photoncamera.processing.opengl.postpipeline;

import com.particlesdevs.photoncamera.settings.annotations.Tunable;
import com.particlesdevs.photoncamera.util.Log;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.processing.opengl.GLTexture;
import com.particlesdevs.photoncamera.processing.opengl.nodes.Node;
import com.particlesdevs.photoncamera.processing.render.NoiseModeler;

public class Bilateral extends Node {

    public Bilateral() {
        super("", "Denoise");
    }
    @Tunable(title = "Enable", category = "Bilateral", defaultValue = 1, min = 0, max = 1, step = 1)
    boolean enable;

    @Override
    public void Compile() {
    }

    @Override
    public void Run() {
        if(!enable){
            WorkingTexture = previousNode.WorkingTexture;
            return;
        }
        NoiseModeler modeler = basePipeline.mParameters.noiseModeler;
        float noiseS = modeler.computeModel[0].first.floatValue()+
                modeler.computeModel[1].first.floatValue()+
                modeler.computeModel[2].first.floatValue();
        float noiseO = modeler.computeModel[0].second.floatValue()+
                modeler.computeModel[1].second.floatValue()+
                modeler.computeModel[2].second.floatValue();
        noiseS/=3.f;
        noiseO/=3.f;
        //GLTexture map = glUtils.medianDown(previousNode.WorkingTexture,4);
        Log.d(Name,"NoiseS:"+noiseS+", NoiseO:"+noiseO);
        glProg.setDefine("NOISES",noiseS);
        glProg.setDefine("NOISEO",noiseO);
        glProg.setDefine("INTENSE", (float) basePipeline.mSettings.noiseRstr);
        glProg.setDefine("INSIZE",previousNode.WorkingTexture.mSize);
        glProg.useAssetProgram("denoise/bilateral");
        //glProg.setTexture("NoiseMap",map);
        glProg.setTexture("InputBuffer",previousNode.WorkingTexture);
        WorkingTexture = basePipeline.getMain();
        glProg.drawBlocks(WorkingTexture);
        glProg.closed = true;
        //map.close();
    }
}
