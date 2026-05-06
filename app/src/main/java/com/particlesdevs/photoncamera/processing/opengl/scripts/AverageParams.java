package com.particlesdevs.photoncamera.processing.opengl.scripts;

import com.particlesdevs.photoncamera.processing.render.Parameters;

import java.nio.ByteBuffer;

public class AverageParams {
    ByteBuffer inp1;
    ByteBuffer inp2;
    Parameters parameters;

    public AverageParams(ByteBuffer in1, ByteBuffer in2, Parameters parameters) {
        inp1 = in1;
        inp2 = in2;
        this.parameters = parameters;
    }
}
