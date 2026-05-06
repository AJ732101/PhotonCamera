package com.particlesdevs.photoncamera.processing.opengl;
import com.particlesdevs.photoncamera.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;


import static android.opengl.GLES31.*;
import static com.particlesdevs.photoncamera.processing.opengl.GLCoreBlockProcessing.checkEglError;

public class GLBuffer implements AutoCloseable {
    public int mBufferID;
    public int size;
    public int byteSize;
    public int mode;
    public GLFormat mFormat;
    public ByteBuffer byteBuffer;
    public GLBuffer(int size,GLFormat mFormat){
        this(size,mFormat,GL_STATIC_DRAW);
    }
    public GLBuffer(int size,GLFormat mFormat,int mode){
        this.mFormat = new GLFormat(mFormat);
        this.size = size;
        this.byteSize = size*mFormat.mFormat.mSize;
        this.mode = mode;
        byteBuffer = ByteBuffer.allocateDirect(byteSize);
        byteBuffer.order(ByteOrder.nativeOrder());
        int [] IDPointer = new int[1];
        glGenBuffers(1, IDPointer,0);
        mBufferID = IDPointer[0];
        Bind();
        Buffering();
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, mBufferID, mBufferID);
        //UnBind();
        //BindBase();
    }
    public void Bind(){
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, mBufferID);
        checkEglError("bind buffer:"+mBufferID);
    }
    public void UnBind(){
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        checkEglError("unbind buffer:"+mBufferID);
    }
    public void BindBase(int number,int type){
        Bind();
        glBindBufferBase(type, number, mBufferID);
        checkEglError("bind buffer base:"+mBufferID);
    }
    public void Buffering(){
        Buffering(size);
    }
    public void Buffering(int size){
        glBufferData(GL_SHADER_STORAGE_BUFFER, byteSize, byteBuffer.asIntBuffer(), mode);
        checkEglError("clear buffer:"+mBufferID);
    }
    public int[] readBufferIntegers(boolean clear)
    {
        int[] value = null;
        Bind();
        checkEglError("bind SSBO");
        ByteBuffer buffer = (ByteBuffer)glMapBufferRange(GL_SHADER_STORAGE_BUFFER,0,byteSize,GL_MAP_READ_BIT);
        checkEglError("getByteBuffer");
        if (buffer != null)
        {
            buffer.order(ByteOrder.nativeOrder());
            IntBuffer intbuf = buffer.asIntBuffer();
            value = toArray(intbuf);
        }
        glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
        checkEglError("unmap buffer:"+mBufferID);
        if (clear) {
            Buffering();
        }
        UnBind();
        return value;
    }
    public int[] toArray(IntBuffer b) {
        if(b.hasArray()) {
            if(b.arrayOffset() == 0)
                return b.array();
            return Arrays.copyOfRange(b.array(), b.arrayOffset(), b.array().length);
        }
        b.rewind();
        int[] output = new int[b.remaining()];
        b.get(output);
        return output;
    }
    public byte[] toArray(ByteBuffer b) {
        if(b.hasArray()) {
            if(b.arrayOffset() == 0)
                return b.array();
            return Arrays.copyOfRange(b.array(), b.arrayOffset(), b.array().length);
        }
        b.rewind();
        byte[] output = new byte[b.remaining()];
        b.get(output);
        return output;
    }
    public void Clear(){
        byteBuffer.clear();
    }

    public void uploadBuffer(int[] data, int count) {
        Bind();
        int uploadSize = Math.min(count, size) * mFormat.mFormat.mSize;
        ByteBuffer buffer = (ByteBuffer)glMapBufferRange(GL_SHADER_STORAGE_BUFFER, 0, uploadSize, GL_MAP_WRITE_BIT);
        checkEglError("map buffer for write");
        if (buffer != null) {
            buffer.order(ByteOrder.nativeOrder());
            IntBuffer intbuf = buffer.asIntBuffer();
            intbuf.put(data, 0, Math.min(count, size));
        }
        glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
        checkEglError("unmap buffer after write:" + mBufferID);
        UnBind();
    }

    @Override
    public void close() {
        glDeleteBuffers(1, new int[]{mBufferID}, 0);
    }
}
