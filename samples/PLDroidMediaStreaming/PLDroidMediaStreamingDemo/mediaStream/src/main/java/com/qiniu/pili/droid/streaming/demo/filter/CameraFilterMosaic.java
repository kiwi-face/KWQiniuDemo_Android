package com.qiniu.pili.droid.streaming.demo.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.qiniu.pili.droid.streaming.demo.R;
import com.qiniu.pili.droid.streaming.demo.gles.GlUtil;

import java.nio.FloatBuffer;

public class CameraFilterMosaic extends CameraFilter {
    private int muTexsizeLoc;

    private static final float Texsize_array[] = {
            255, 255,
    };

    public CameraFilterMosaic(Context context) {
        super(context);
    }

    @Override
    protected int createProgram(Context applicationContext) {
        return GlUtil.createProgram(applicationContext, R.raw.vertex_shader,
                R.raw.fragment_shader_mosaic);
    }

    @Override
    protected void getGLSLValues() {
        super.getGLSLValues();

        muTexsizeLoc = GLES20.glGetUniformLocation(mProgramHandle, "TexSize");
    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex,
                                  int vertexStride, FloatBuffer texBuffer, int texStride) {
        super.bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride,
                texBuffer, texStride);

        GLES20.glUniform2fv(muTexsizeLoc, 1, Texsize_array, 0);
    }
}