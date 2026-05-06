package com.particlesdevs.photoncamera.processing.opengl.scripts;

import android.graphics.Point;

import com.particlesdevs.photoncamera.processing.opengl.GLDrawParams;
import com.particlesdevs.photoncamera.processing.render.Parameters;
import com.particlesdevs.photoncamera.util.Log;

import com.particlesdevs.photoncamera.processing.processor.UnlimitedProcessor;
import com.particlesdevs.photoncamera.processing.opengl.GLCoreBlockProcessing;
import com.particlesdevs.photoncamera.processing.opengl.GLFormat;
import com.particlesdevs.photoncamera.processing.opengl.GLOneScript;
import com.particlesdevs.photoncamera.processing.opengl.GLProg;
import com.particlesdevs.photoncamera.processing.opengl.GLTexture;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_NEAREST;

public class AverageRaw extends GLOneScript {
    GLTexture first,second,stack,stack2,finalTex,input;
    private GLProg glProg;
    int frameCounter = 1;
    private boolean stacked = false;
    private boolean firstEmpty = true;
    private Point rawHalf;
    private Parameters parameters;
    public AverageRaw(Point size, String name) {
        super(size, new GLCoreBlockProcessing(size,new GLFormat(GLFormat.DataType.UNSIGNED_16), GLDrawParams.Allocate.Direct), "merge/average", name);
        rawHalf = new Point(size.x/2,size.y/2);
    }
    float[] wBalance;
    float[] bLevel;
    public void Init(){
        stacked = false;
        firstEmpty = true;
        first = new GLTexture(rawHalf,new GLFormat(GLFormat.DataType.FLOAT_16,4));
        second = new GLTexture(rawHalf,new GLFormat(GLFormat.DataType.FLOAT_16,4));
        stack = new GLTexture(rawHalf,new GLFormat(GLFormat.DataType.FLOAT_16,4));
        stack2 = new GLTexture(rawHalf,new GLFormat(GLFormat.DataType.FLOAT_16,4));
        finalTex = new GLTexture(size,new GLFormat(GLFormat.DataType.UNSIGNED_16), GL_NEAREST, GL_CLAMP_TO_EDGE);
        input = new GLTexture(size,new GLFormat(GLFormat.DataType.UNSIGNED_16), GL_NEAREST, GL_CLAMP_TO_EDGE);
        parameters = ((AverageParams) additionalParams).parameters;
        wBalance = new float[4];
        bLevel = new float[4];
        for(int i = 0; i<4;i++){
            bLevel[i] = parameters.blackLevel[i]/((float)parameters.whiteLevel);
            bLevel[i] /= 1.4f;
        }
        switch (parameters.cfaPattern){
            case 0: // RGGB
                wBalance[0] = 1.0f/parameters.whitePoint[0];
                wBalance[1] = 1.0f/parameters.whitePoint[1];
                wBalance[2] = 1.0f/parameters.whitePoint[1];
                wBalance[3] = 1.0f/parameters.whitePoint[2];
                break;
            case 1: // GRBG
                wBalance[0] = 1.0f/parameters.whitePoint[1];
                wBalance[1] = 1.0f/parameters.whitePoint[0];
                wBalance[2] = 1.0f/parameters.whitePoint[2];
                wBalance[3] = 1.0f/parameters.whitePoint[1];
                break;
            case 2: // GBRG
                wBalance[0] = 1.0f/parameters.whitePoint[1];
                wBalance[1] = 1.0f/parameters.whitePoint[2];
                wBalance[2] = 1.0f/parameters.whitePoint[0];
                wBalance[3] = 1.0f/parameters.whitePoint[1];
                break;
            case 3: // BGGR
                wBalance[0] = 1.0f/parameters.whitePoint[2];
                wBalance[1] = 1.0f/parameters.whitePoint[1];
                wBalance[2] = 1.0f/parameters.whitePoint[1];
                wBalance[3] = 1.0f/parameters.whitePoint[0];
                break;
        }
    }
    private int cnt2 = 1;
    @Override
    public void Run() {
        //Stage 1 average alternate texture
        glProg = glOne.glProgram;
        if (first == null)
            Init();

        Compile();
        AverageParams scriptParams = (AverageParams) additionalParams;
        scriptParams.inp2.position(0);
        input.loadData(scriptParams.inp2);
        glProg.setVar("first", firstEmpty ? 1 : 0);
        glProg.setTexture("InputBuffer", first);
        glProg.setTexture("InputBuffer2", input);
        glProg.setVar("CfaPattern",parameters.cfaPattern);
        glProg.setVar("blackLevel", bLevel);
        glProg.setVar("WhiteBalance", wBalance);
        glProg.setVar("whiteLevel", (float) parameters.whiteLevel);
        if(!firstEmpty)
            glProg.setVar("unlimitedWeight", 1.0f/frameCounter);
        else
            glProg.setVar("unlimitedWeight", 1.0f);

        //WorkingTexture.BufferLoad();
        GLTexture t = first;
        first = second;
        second = t;
        glProg.drawBlocks(first);

        if(frameCounter > 2 && firstEmpty) {
            firstEmpty = false;
            frameCounter = 0;
        }

        frameCounter++;
        //glOne.glProcessing.drawBlocksToOutput();
        //Stage 2 average stack
        if(frameCounter > 60) {
            AverageStack();
        }
    }
    private void AverageStack(){
        glProg.useAssetProgram("merge/averageff");
        GLTexture alIn = first;
        glProg.setTexture("InputBuffer",alIn);
        if (stacked) {
            glProg.setTexture("InputBuffer2", stack);
        } else {
            glProg.setTexture("InputBuffer2", alIn);
        }
        glProg.setVar("unlimitedWeight",1.0f/Math.min(cnt2,250));
        GLTexture t = stack;
        stack = stack2;
        stack2 = t;
        glProg.drawBlocks(stack);
        Log.d(Name,"AverageShift:"+Math.min(cnt2,250));
        stacked = true;
        cnt2++;
        frameCounter = 1;
    }
    public void FinalScript(){
        //AverageStack();
        glProg = glOne.glProgram;
        glProg.useAssetProgram("merge/medianfilterhotpixeltoraw");
        glProg.setVar("CfaPattern",parameters.cfaPattern);
        Log.d(Name,"CFAPattern:"+parameters.cfaPattern);
        if(stacked) {
            glProg.setTexture("InputBuffer", stack);
        } else {
            glProg.setTexture("InputBuffer", first);
        }
        glProg.setVar("whiteLevel",UnlimitedProcessor.FAKE_WL);
        glProg.setVar("blackLevel", bLevel);
        glProg.setVar("WhiteBalance", 1.0f/wBalance[0], 1.0f/wBalance[1], 1.0f/wBalance[2], 1.0f/wBalance[3]);
        //in1 = WorkingTexture;
        finalTex.BufferLoad();
        glOne.glProcessing.drawBlocksToOutput();
        first.close();
        second.close();
        stack.close();
        stack2.close();
        finalTex.close();
        glProg.close();
        input.close();
        first = null;
        Output = glOne.glProcessing.mOutBuffer;
    }

}