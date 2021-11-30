package com.example.lib;

public interface AHSurfaceTextureCallback {
    void onSurfaceCreated();

    void onSurfaceChanged(int var1, int var2);

    void onSurfaceDestroyed();

    int onDrawFrame(int var1, int var2, int var3, float[] var4);
}
