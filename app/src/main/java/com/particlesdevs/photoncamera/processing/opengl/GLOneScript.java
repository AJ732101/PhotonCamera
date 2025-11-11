package com.particlesdevs.photoncamera.processing.opengl;

import static com.particlesdevs.photoncamera.util.FileManager.sPHOTON_TUNING_DIR;

import android.graphics.Bitmap;
import android.graphics.Point;
import com.particlesdevs.photoncamera.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Properties;

public class GLOneScript implements AutoCloseable {
    public GLTexture WorkingTexture;
    public GLOneParams glOne;
    public final String Name;
    public ByteBuffer Output;
    public final String Rid;
    private long timeStart;
    public Point size;
    public GLFormat glFormat;
    GLImage outbit;
    public Object additionalParams;
    public boolean hiddenScript = false;
    private boolean isTuningEnabled = false;
    private final boolean loggedTuning = false;
    private Properties mProp;

    public GLOneScript(Point size, GLImage output, GLFormat glFormat, String rid, String name) {
        outbit = output;
        this.glFormat = glFormat;
        this.size = size;
        Name = name;
        Rid = rid;
        mProp = new Properties();
    }

    public GLOneScript(Point size, GLCoreBlockProcessing glCoreBlockProcessing, String rid, String name) {
        this.size = size;
        glOne = new GLOneParams(glCoreBlockProcessing);
        Name = name;
        Rid = rid;
        mProp = new Properties();
    }

    public GLOneScript(Point size, GLCoreBlockProcessing glCoreBlockProcessing, String rid, String name, boolean isTuningEnabled) {
        this.size = size;
        glOne = new GLOneParams(glCoreBlockProcessing);
        Name = name;
        Rid = rid;
        this.isTuningEnabled = isTuningEnabled;
        if(isTuningEnabled) {
            Log.d("GLOneScript", "Tuning enabled for script: " + name);
            Properties properties = new Properties();
            try {
                File init = new File(sPHOTON_TUNING_DIR, "PhotonCameraTuning.ini");
                if(!init.exists()) {
                    init.createNewFile();
                }
                properties.load(new FileInputStream(init));
            } catch (Exception e) {
                Log.e("GLOneScript","Error at loading properties:" + Log.getStackTraceString(e));
            }
            mProp = properties;
        }
    }

    private void tuningLog(String name, String value){
        if(loggedTuning) Log.d("Tuning",name+" = "+ value);
    }
    public boolean getTuning(String name, boolean Default){
        tuningLog(Name+"_"+name,String.valueOf(Default));
        return Boolean.parseBoolean(mProp.getProperty(Name+"_"+name,String.valueOf(Default)));
    }
    public float getTuning(String name,float Default){
        tuningLog(Name+"_"+name,String.valueOf(Default));
        return Float.parseFloat(mProp.getProperty(Name+"_"+name,String.valueOf(Default)));
    }
    public float[] getTuning(String name,float[] Default){
        String ins = Arrays.toString(Default).replace("[","").replace("]","");
        tuningLog(Name+"_"+name,ins);
        String inp = mProp.getProperty(Name+"_"+name, ins);
        String[] divided = inp.split(",");
        float[] output = new float[Default.length];
        for(int i = 0; i<divided.length;i++){
            output[i] = Float.parseFloat(divided[i]);
        }
        return output;
    }
    public double getTuning(String name,double Default){
        tuningLog(Name+"_"+name,String.valueOf(Default));
        return Double.parseDouble(mProp.getProperty(Name+"_"+name,String.valueOf(Default)));
    }
    public short getTuning(String name,short Default){
        tuningLog(Name+"_"+name,String.valueOf(Default));
        return Short.parseShort(mProp.getProperty(Name+"_"+name,String.valueOf(Default)));
    }
    public int getTuning(String name,int Default){
        tuningLog(Name+"_"+name,String.valueOf(Default));
        return Integer.parseInt(mProp.getProperty(Name+"_"+name,String.valueOf(Default)));
    }

    public void StartScript() {
    }

    public void Run() {
        Point sizeo = new Point(size);
        if (glFormat == null) {
            hiddenScript = true;
            glFormat = new GLFormat(GLFormat.DataType.UNSIGNED_8, 4);
            sizeo = new Point(1, 1);
        }
        if (Output == null)
            glOne = new GLOneParams(sizeo, outbit, glFormat);
        else {
            glOne = new GLOneParams(sizeo, outbit, glFormat, Output);
        }
        Compile();
        startT();
        StartScript();
        if (!hiddenScript) {
            //glOne.glProgram.drawBlocks(WorkingTexture);
            WorkingTexture.BufferLoad();
            glOne.glProcessing.drawBlocksToOutput();

        } else {
            glOne.glProgram.drawBlocks(WorkingTexture);
        }
        AfterRun();
        endT();
        Output = glOne.glProcessing.mOutBuffer;
    }
    public void AfterRun(){

    }

    public void startT() {
        timeStart = System.currentTimeMillis();
    }

    public void endT() {
        Log.d("OneScript", "Name:" + Name + " elapsed:" + (System.currentTimeMillis() - timeStart) + " ms");
    }

    public void Compile() {
        glOne.glProgram.useAssetProgram(Rid);
    }

    @Override
    public void close() {
        glOne.glProgram.close();
        glOne.glProcessing.close();
    }
}
