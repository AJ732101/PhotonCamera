package com.particlesdevs.photoncamera.processing.opengl.postpipeline;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.processing.opengl.GLTexture;
import com.particlesdevs.photoncamera.processing.opengl.nodes.Node;
import com.particlesdevs.photoncamera.settings.annotations.Tunable;
import com.particlesdevs.photoncamera.util.FileManager;

public class Demosaic3 extends Node {
    public  Demosaic3() {
        super("", "Demosaic");
    }

    @Override
    public void Compile() {}
    
    @Tunable(title = "Grad Size", category = "Demosaic", max = 5.0f, defaultValue = 1.5f)
    float gradSize = 1.5f;
    
    @Tunable(title = "Fuse Min", category = "Demosaic", max = 1.0f, defaultValue = 0.0f)
    float fuseMin = 0.f;
    
    @Tunable(title = "Fuse Max", category = "Demosaic", max = 2.0f, defaultValue = 1.0f)
    float fuseMax = 1.f;
    
    @Tunable(title = "Fuse Shift", category = "Demosaic", min = -2.0f, max = 2.0f, defaultValue = -0.5f)
    float fuseShift = -0.5f;
    
    @Tunable(title = "Fuse Multiply", category = "Demosaic", max = 20.0f, defaultValue = 6.0f)
    float fuseMpy = 6.0f;
    
    @Tunable(title = "Green Min", category = "Demosaic", max = 0.001f, defaultValue = 0.00000001f, step = 0.00000001f)
    float greenMin = 1e-8f;
    
    @Tunable(title = "Green Max", category = "Demosaic", max = 2.0f, defaultValue = 1.0f)
    float greenMax = 1.0f;
    
    @Override
    public void Run() {
        // Values are automatically injected in BeforeRun()!
        GLTexture glTexture;
        glTexture = previousNode.WorkingTexture;
        //Gradients
        GLTexture outp;
        int tile = 8;
        startT();
        WorkingTexture = basePipeline.main3;
        glProg.setLayout(tile,tile,1);
        glProg.useAssetProgram("demosaic/demosaicp0ig",true);
        glProg.setTextureCompute("inTexture", glTexture,false);
        glProg.setTextureCompute("outTexture", WorkingTexture,true);
        glProg.computeManual(WorkingTexture.mSize.x/tile,WorkingTexture.mSize.y/tile,1);
        endT("demosaicp0ig");

        //Colour channels
        startT();
        outp = basePipeline.getMain();
        glProg.setLayout(tile,tile,1);
        glProg.useAssetProgram("demosaic/demosaicp12ec",true);
        glProg.setTextureCompute("inTexture",glTexture, false);
        glProg.setTextureCompute("igTexture",basePipeline.main3, false);
        glProg.setTextureCompute("outTexture",outp, true);
        glProg.computeManual(WorkingTexture.mSize.x/tile,WorkingTexture.mSize.y/tile,1);
        endT("demosaicp12ec");

        startT();
        glProg.setLayout(tile,tile,1);
        glProg.useAssetProgram("demosaic/demosaicp12fc",true);
        glProg.setTextureCompute("inTexture",glTexture, false);
        glProg.setTextureCompute("igTexture",basePipeline.main3, false);
        glProg.setTextureCompute("greenTexture",outp, false);
        glProg.setTextureCompute("outTexture",outp, true);
        glProg.computeManual(WorkingTexture.mSize.x/tile,WorkingTexture.mSize.y/tile,1);
        endT("demosaicp12fc");
        //glProg.drawBlocks(WorkingTexture);

        startT();
        WorkingTexture = basePipeline.main3;
        glProg.setDefine("greenmin",greenMin);
        glProg.setDefine("greenmax",greenMax);
        glProg.setLayout(tile,tile,1);
        //glProg.useFileProgram(FileManager.sPHOTON_TUNING_DIR + "demosaicp2ec.glsl",true);
        glProg.useAssetProgram("demosaic/demosaicp2ed2",true);
        glProg.setTextureCompute("inTexture", glTexture,false);
        glProg.setTextureCompute("greenTexture", outp,false);
        glProg.setTextureCompute("igTexture", basePipeline.main3,false);
        glProg.setTextureCompute("outTexture", WorkingTexture,true);
        glProg.setVar("neutral", basePipeline.mParameters.whitePoint[0], basePipeline.mParameters.whitePoint[1], basePipeline.mParameters.whitePoint[1], basePipeline.mParameters.whitePoint[2]);
        //glProg.setVar("neutral", 1.f, 1.f, 1.f, 1.f);
        glProg.computeManual(WorkingTexture.mSize.x/tile,WorkingTexture.mSize.y/tile,1);
        glProg.close();
        endT("demosaicp2ec");
        WorkingTexture = basePipeline.swap3();
    }
}
