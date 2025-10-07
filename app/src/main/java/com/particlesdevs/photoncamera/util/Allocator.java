package com.particlesdevs.photoncamera.util;

import java.nio.ByteBuffer;
public class Allocator{
    static {
        System.loadLibrary("allocator");
    }
    public native static ByteBuffer allocate(int capacity);

    public native static ByteBuffer allocateAndCopy(int capacity, ByteBuffer origin);
    public native static ByteBuffer allocateAndCopyConvert(int capacity, ByteBuffer origin, int width, int row_stride);

    public native static void free(ByteBuffer buffer);
    public native static long getMemoryCount();
}
