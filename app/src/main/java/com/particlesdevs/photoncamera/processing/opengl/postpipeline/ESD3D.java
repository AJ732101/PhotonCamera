package com.particlesdevs.photoncamera.processing.opengl.postpipeline;

import com.particlesdevs.photoncamera.util.Log;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.processing.opengl.GLTexture;
import com.particlesdevs.photoncamera.processing.opengl.nodes.Node;
import com.particlesdevs.photoncamera.processing.render.NoiseModeler;

public class ESD3D extends Node {
    boolean needClose = false;
    public ESD3D(boolean closing) {
        super("", "ES3D");
        needClose = closing;
    }

    @Override
    public void Compile() {
    }
    float moire = 1.5f;
    float luma = 0.8f;
    float noiseToKernelSize = 24.0f;
    float noiseTarget = 1.0f/256.f;
    int maxSize = 21;
    int minSize = 7;

    @Override
    public void Run() {
        //moire = getTuning("MoireRemoveMpy",moire);
        noiseToKernelSize = getTuning("NoiseToKernelSize",noiseToKernelSize);
        noiseTarget = getTuning("NoiseTarget",noiseTarget);
        luma = getTuning("Luma",luma);
        maxSize = getTuning("MaxKernel",maxSize);
        minSize = getTuning("MinKernel",minSize);
        //if(basePipeline.main4 == null)
        //    basePipeline.main4 = glUtils.medianDown(previousNode.WorkingTexture,4);
        //GLTexture grad;
        /*
        if(previousNode.WorkingTexture != basePipeline.main3){
            //grad = basePipeline.main3;
            WorkingTexture = basePipeline.getMain();
        }
        else {
            //grad = basePipeline.getMain();
            WorkingTexture = basePipeline.main3;
        }*/
        //glUtils.ConvDiff(previousNode.WorkingTexture, grad, 0.f);
        WorkingTexture = basePipeline.getMain();

        {
            Log.d(Name, "NoiseS:" + basePipeline.noiseS + ", NoiseO:" + basePipeline.noiseO);
            glProg.setDefine("NOISES", basePipeline.noiseS);
            glProg.setDefine("NOISEO", basePipeline.noiseO);
            glProg.setDefine("MOIRE", moire);
            glProg.setDefine("LUMA", luma);

            glProg.setDefine("INSIZE", basePipeline.mParameters.rawSize);
            //float ks = 1.0f + Math.min((basePipeline.noiseS+basePipeline.noiseO) * 3.0f * noiseToKernelSize, 34.f);
            //int msize = 7 + (int)ks - (int)ks%2;
            double noiseMpy = Math.max((basePipeline.noiseS+basePipeline.noiseO)/noiseTarget, 0.0000001);
            double kernelSize = 1.0f + Math.sqrt(noiseMpy) * noiseToKernelSize;
            int msize = Math.min(minSize + (int)kernelSize - (int)kernelSize%2, maxSize);
            Log.d("ESD3D", "KernelSize: "+kernelSize+" MSIZE: "+msize);
            glProg.setDefine("KERNELSIZE", (float)(kernelSize));
            glProg.setDefine("MSIZE", msize);
            glProg.useAssetProgram("denoise/esd3d2");
            //glProg.setTexture("NoiseMap", basePipeline.main4);
            glProg.setTexture("InputBuffer", previousNode.WorkingTexture);
            //glProg.setTexture("GradBuffer", grad);
            glProg.drawBlocks(WorkingTexture);
        }
        glProg.closed = true;
        /*if(needClose) {
            basePipeline.main4.close();
            basePipeline.main4 = null;
        }*/
    }
}
