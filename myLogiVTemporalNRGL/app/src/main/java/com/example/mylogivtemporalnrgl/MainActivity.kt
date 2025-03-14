package com.example.mylogivtemporalnrgl

import  android.app.ActivityManager
import android.os.Bundle

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import androidx.camera.core.ImageAnalysis

import android.content.Context

import android.graphics.SurfaceTexture
import android.media.Image
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES31
import android.opengl.GLES20.GL_LUMINANCE
import android.opengl.GLES20.GL_RGB
import android.opengl.GLES20.GL_UNSIGNED_BYTE
import android.opengl.GLSurfaceView
import android.opengl.Matrix

import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface

import androidx.activity.ComponentActivity
import androidx.annotation.OptIn

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview

import java.nio.Buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

//ok
object SharedDataGamma {
    var gammaAll: Float = 1.0f
}

object SharedDataScale {
    var scaleAll: Float = 0.01f
}

object SharedDataImage {
    var selectResolution = 2 // 0 = 3840, 2160, 1 = 1920, 1080, 2 = 1080, 720

    var widthInput: Int = 1280 //3840 
    var heightInput: Int = 720 //2160 
}

//ok
class MyGLRenderer(private val glSurfaceView:GLSurfaceView) : GLSurfaceView.Renderer {
    private lateinit var mTriangle: Triangle

    var texture = IntArray(1)

    var numTextureFrames: Int = 3 //5, 7
    var textureFrames = IntArray(numTextureFrames)

    var fboFrame = IntArray(1)

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)

    @Volatile
    var angle: Float = 0f
    var gamma: Float = 0f
    var scale: Float = 0.01f

    fun updateGamma(newGamma: Float) {
        SharedDataGamma.gammaAll = newGamma
    }

    fun updateScale(newScale: Float) {
        SharedDataScale.scaleAll = newScale
    }

    private lateinit var surfaceTexture: SurfaceTexture
    private var surfaceTextureReadyCallback: ((SurfaceTexture) ->Unit)? = null

    fun setSurfaceTextureReadyCallback(callback: (SurfaceTexture) -> Unit) {
        surfaceTextureReadyCallback = callback
    }

    //ok
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d("TEST_TAG_OUTPUT","MyGLRenderer.onSurfaceCreated Enter")
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)


        val extensions = GLES31.glGetString(GLES31.GL_EXTENSIONS)
        if (!extensions.contains("GL_OES_EGL_image_external")) {
            Log.d("TEST_TAG_OUTPUT_EXCEPTION", "OES EGL issue")
            throw RuntimeException("GL_OES_EGL_image_external not supported!")
        }

        GLES31.glGenTextures(1, texture, 0)
        GLES31.glActiveTexture(GLES31.GL_TEXTURE10)
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_REPEAT) //GL_REPEAT)//GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_REPEAT)


        //ok
        GLES31.glGenTextures(numTextureFrames, textureFrames, 0)

        //for (id in textureFrames) {
        for (i in 0..numTextureFrames - 1) {
            GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + i)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[i])
            GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA, SharedDataImage.widthInput, SharedDataImage.heightInput, 0, GLES31.GL_RGBA, GL_UNSIGNED_BYTE, null as Buffer?);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)

            GLES31.glGenFramebuffers(1, fboFrame, 0);

            //GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboFrames[i]);
            //When you render, the output will be stored in this texture
            //GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureFrames[i], 0);
            //if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            //    Log.d("TTTEST_TAG_OUTPUT", "frame buffer down error")
            //}
        }

        mTriangle = Triangle(texture[0], fboFrame[0], numTextureFrames, textureFrames) //, fboFrames)

        surfaceTexture = SurfaceTexture(texture[0])
        surfaceTexture.setOnFrameAvailableListener {
            glSurfaceView.requestRender()
        }

        surfaceTextureReadyCallback?.invoke(surfaceTexture)
    }

    //ok
    override fun onDrawFrame(unused: GL10) {
        Log.d("TEST_TAG_OUTPUT", "MyGLRenderer.onDrawFrame Enter")
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        val scratch = FloatArray(16)

        // Create a rotation transformation for the triangle
        //val time = SystemClock.uptimeMillis() % 4000L
        //val angle = 0.090f * time.toInt()
        //Matrix.setRotateM(rotationMatrix, 0, angle, 0f, 0f, -1.0f)
        Matrix.setRotateM(rotationMatrix, 0, 0f, 0f, 0f, -1.0f)

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0)

        surfaceTexture.updateTexImage()

        //Draw triangle
        updateGamma(gamma)
        updateScale(scale)
        mTriangle.draw(scratch)
        // Draw shape
        //mTriangle.draw(vPMatrix)

        Log.d("TEST_TAG_OUTPUT", "MyGLRenderer.onDrawFrame Exit")
    }

    //ok
    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        Log.d("TTEST_TAG_OUTPUT", "MyGLRenderer.onSurfaceChanged Enter")

        Log.d("TTEST_TAG_OUTPUT - onSurfaceChanged", "Width: " + width.toString());
        Log.d("TTEST_TAG_OUTPUT - onSurfaceChanged", "Height: " + height.toString());
        //GLES20.glViewport(0, 0, (0.7 * width).toInt(), height) //3840, 2160) //width, height)
        //GLES20.glViewport(0, 0, width, height) //3840, 2160) //width, height)
        //GLES20.glViewport(0, 0, width, height) //(480 * 8) / 4, (270 * 8) / 4)

        val ratio: Float = width.toFloat() / height.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

        Log.d("TEST_TAG_OUTPUT", "MyGLRenderer.onSurfaceChanged Exit")
    }
}


//ok
class Triangle(private var textureId: Int, private var fboFrame: Int, private var numTextureFrames: Int, private var textureFrames: IntArray) { //}, private var fboFrames: IntArray) { //}, private var texturePing: Int, private var texturePong: Int,
    //               private var fboPing: Int, private var fboPong: Int) {
    //ok
    var mProgram: Int = 0
    var dPositionHandle: Int = 0
    var dTextureHandle: Int = 0
    var dTextureDefaultHandle: Int = 0

    var dSelectFragmentHandle: Int = 0
    var dIncomingFrameHandle: Int = 0
    var dTexture10UniformHandle: Int = 0

    var dTexture0UniformHandle: Int = 0
    var dTexture1UniformHandle: Int = 0
    var dTexture2UniformHandle: Int = 0
    var dTexture3UniformHandle: Int = 0
    var dTexture4UniformHandle: Int = 0
    var dTexture5UniformHandle: Int = 0
    var dTexture6UniformHandle: Int = 0
    var dTexture7UniformHandle: Int = 0
    var dTexture8UniformHandle: Int = 0

    var dScaleHandle: Int = 0

    private var frameCount = 0

    fun loadShader(type: Int, shaderCode: String): Int {
        Log.d("TEST_TAG_OUTPUT", "Triangle.loadShader Enter")

        var shader = GLES31.glCreateShader(type)
        Log.d("TEST_TAG_OUTPUT", "Triangle.loadShader, shader(0 = bad): $shader")

        GLES31.glShaderSource(shader, shaderCode)
        GLES31.glCompileShader(shader)

        var compileResult = IntArray(1)
        GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, compileResult, 0)
        //GL_NO_ERROR is 0
        var error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT_COMPILE", "ERROR: Triangle.compileShader")
        } else {
            Log.d("TEST_TAG_OUTPUT_COMPILE", "OK: Triangle.compileShader")
        }
        if (compileResult[0] != GLES31.GL_TRUE) {
            Log.d("TEST_TAG_OUTPUT_COMPILE", "ERROR: Triangle.compileShader via compileResult")
        } else {
            Log.d("TEST_TAG_OUTPUT_COMPILE", "OK: Triangle.compileShader via compileResult")
        }
        Log.d(
            "TEST_TAG_OUTPUT_COMPILE",
            "Triangle.compileShader compile ok (1,1) - compileResult[0]: ${compileResult[0]}, GL_TRUE is 1"
        )


        Log.d("TEST_TAG_OUTPUT", "Triangle.loadShader Exit")
        return shader
    }

    init {
        Log.d("TEST_TAG_OUTPUT", "Triangle.init")

        val vertexShaderCode = """
            #version 310 es
            precision mediump float; 
            
            layout(location = 0) in vec4 vPosition;
            layout(location = 1) in vec2 aTexCoord;
            layout(location = 2) in vec2 aTexCoordDefault; 
            out vec2 texCoord;
            out vec2 texCoordDefault; 
            
            void main() {
                texCoord = aTexCoord; 
                texCoordDefault = aTexCoordDefault; 
                gl_Position = vPosition;      
            }
        """

        val fragmentShaderCode = """
            #version 310 es
            //#extension GL_OES_EGL_image_external : require
            #extension GL_OES_EGL_image_external_essl3 : require
            
            precision highp float;
             
            uniform int selectFragment;
            uniform int incomingFrame; 
             
            uniform samplerExternalOES textureSampler;
            //uniform sampler2D frame[9]; //0-8
            uniform sampler2D input0; 
            uniform sampler2D input1;
            uniform sampler2D input2;
            uniform sampler2D input3;
            uniform sampler2D input4; 
            uniform sampler2D input5;
            uniform sampler2D input6;
            uniform sampler2D input7;
            uniform sampler2D input8;
            
            uniform float k; 
            uniform float scale;
            
            in vec2 texCoord;
            in vec2 texCoordDefault;
            
            out vec4 fragColor;
            
            
            float calcStat2(float input1, float input2, float input3) {
                float av = (input1 + input2 + input3) / 3.0;
                
                float term1 = (input1 - av) * (input1 - av); 
                float term2 = (input2 - av) * (input2 - av);
                float term3 = (input3 - av) * (input3 - av);
                return (term1 + term2 + term3) / 3.0;
                
                //return vec4(1.0, 0.0, 0.0, 1.0); 
            }
            
            float calcStat3(float input1, float input2, float input3) {
                float av = (input1 + input2 + input3) / 3.0;
                
                float term1 = (input1 - av) * (input1 - av) * (input1 - av); 
                float term2 = (input2 - av) * (input2 - av) * (input2 - av);
                float term3 = (input3 - av) * (input3 - av) * (input3 - av);
                return (term1 + term2 + term3) / 3.0;
                
                //return vec4(1.0, 0.0, 0.0, 1.0); 
            }
            
            float calcStat4(float input1, float input2, float input3) {
                float av = (input1 + input2 + input3) / 3.0;
                
                float term1 = (input1 - av) * (input1 - av) * (input1 - av) * (input1 - av); 
                float term2 = (input2 - av) * (input2 - av) * (input2 - av) * (input2 - av);
                float term3 = (input3 - av) * (input3 - av) * (input3 - av) * (input3 - av);
                return (term1 + term2 + term3) / 3.0;
                
                //return vec4(1.0, 0.0, 0.0, 1.0); 
            }
            
            
            float calcAlpha(float inputVar, float K) {
                float alpha = 0.0; 
                
                if (inputVar >= 0.0 && inputVar < K) {
                    alpha = 1.0; 
                } else if (inputVar > 2.0 * K) {
                    alpha = 0.0; 
                } else {
                    alpha = (2.0 * K - inputVar) / K; 
                }
                
                return alpha;
            }
            
           
            void main() {
                if (selectFragment == 0) {
                    vec4 color = texture(textureSampler, texCoord);
                    fragColor = vec4(color.rgb, 1.0);   
                } else if (selectFragment == 1) {
                    vec2 rotatedTexCoord = vec2(1.0 - texCoordDefault.y, 1.0 - texCoordDefault.x);
                    vec4 color = texture(textureSampler, texCoord);
                    
                    /*
                    vec4 output0 = 255.0 * texture(input0, rotatedTexCoord); 
                    vec4 output1 = 255.0 * texture(input1, rotatedTexCoord); 
                    vec4 output2 = 255.0 * texture(input2, rotatedTexCoord); 
                    vec4 output3 = 255.0 * texture(input3, rotatedTexCoord); 
                    vec4 output4 = 255.0 * texture(input4, rotatedTexCoord); 
                    vec4 output5 = 255.0 * texture(input5, rotatedTexCoord); 
                    vec4 output6 = 255.0 * texture(input6, rotatedTexCoord); 
                    vec4 output7 = 255.0 * texture(input7, rotatedTexCoord); 
                    vec4 output8 = 255.0 * texture(input8, rotatedTexCoord); 
                    
                    float frame0Y =   0.299 * output0.r +  0.587 * output0.g +  0.114 * output0.b; 
                    float frame0Cb = -0.169 * output0.r + -0.331 * output0.g +  0.500 * output0.b + 128.0;
                    float frame0Cr =  0.500 * output0.r + -0.419 * output0.g + -0.081 * output0.b + 128.0;
                    
                    float frame1Y =   0.299 * output1.r +  0.587 * output1.g +  0.114 * output1.b; 
                    float frame1Cb = -0.169 * output1.r + -0.331 * output1.g +  0.500 * output1.b + 128.0;
                    float frame1Cr =  0.500 * output1.r + -0.419 * output1.g + -0.081 * output1.b + 128.0;
                    
                    float frame2Y =   0.299 * output2.r +  0.587 * output2.g +  0.114 * output2.b; 
                    float frame2Cb = -0.169 * output2.r + -0.331 * output2.g +  0.500 * output2.b + 128.0;
                    float frame2Cr =  0.500 * output2.r + -0.419 * output2.g + -0.081 * output2.b + 128.0;
                    
                    float frame3Y =   0.299 * output3.r +  0.587 * output3.g +  0.114 * output3.b; 
                    float frame3Cb = -0.169 * output3.r + -0.331 * output3.g +  0.500 * output3.b + 128.0;
                    float frame3Cr =  0.500 * output3.r + -0.419 * output3.g + -0.081 * output3.b + 128.0;
                    
                    float frame4Y =   0.299 * output4.r +  0.587 * output4.g +  0.114 * output4.b; 
                    float frame4Cb = -0.169 * output4.r + -0.331 * output4.g +  0.500 * output4.b + 128.0;
                    float frame4Cr =  0.500 * output4.r + -0.419 * output4.g + -0.081 * output4.b + 128.0;
                    
                    float frame5Y =   0.299 * output5.r +  0.587 * output5.g +  0.114 * output5.b; 
                    float frame5Cb = -0.169 * output5.r + -0.331 * output5.g +  0.500 * output5.b + 128.0;
                    float frame5Cr =  0.500 * output5.r + -0.419 * output5.g + -0.081 * output5.b + 128.0;
                    
                    float frame6Y =   0.299 * output6.r +  0.587 * output6.g +  0.114 * output6.b; 
                    float frame6Cb = -0.169 * output6.r + -0.331 * output6.g +  0.500 * output6.b + 128.0;
                    float frame6Cr =  0.500 * output6.r + -0.419 * output6.g + -0.081 * output6.b + 128.0;
                    
                    float frame7Y =   0.299 * output7.r +  0.587 * output7.g +  0.114 * output7.b; 
                    float frame7Cb = -0.169 * output7.r + -0.331 * output7.g +  0.500 * output7.b + 128.0;
                    float frame7Cr =  0.500 * output7.r + -0.419 * output7.g + -0.081 * output7.b + 128.0;
                    
                    float frame8Y =   0.299 * output8.r +  0.587 * output8.g +  0.114 * output8.b; 
                    float frame8Cb = -0.169 * output8.r + -0.331 * output8.g +  0.500 * output8.b + 128.0;
                    float frame8Cr =  0.500 * output8.r + -0.419 * output8.g + -0.081 * output8.b + 128.0;
                    */
                    
                    vec4 output0 = texture(input0, rotatedTexCoord); 
                    vec4 output1 = texture(input1, rotatedTexCoord); 
                    vec4 output2 = texture(input2, rotatedTexCoord); 
                    vec4 output3 = texture(input3, rotatedTexCoord); 
                    vec4 output4 = texture(input4, rotatedTexCoord); 
                    vec4 output5 = texture(input5, rotatedTexCoord); 
                    vec4 output6 = texture(input6, rotatedTexCoord); 
                    vec4 output7 = texture(input7, rotatedTexCoord); 
                    vec4 output8 = texture(input8, rotatedTexCoord); 
                    
                    //vec4 outputN = texture(input0, rotatedTexCoord); 
                    
                    float frame0Y =   0.299 * output0.r +  0.587 * output0.g +  0.114 * output0.b; 
                    float frame0Cb = -0.169 * output0.r + -0.331 * output0.g +  0.500 * output0.b + 0.5;
                    float frame0Cr =  0.500 * output0.r + -0.419 * output0.g + -0.081 * output0.b + 0.5;
                    
                    float frame1Y =   0.299 * output1.r +  0.587 * output1.g +  0.114 * output1.b; 
                    float frame1Cb = -0.169 * output1.r + -0.331 * output1.g +  0.500 * output1.b + 0.5;
                    float frame1Cr =  0.500 * output1.r + -0.419 * output1.g + -0.081 * output1.b + 0.5;
                    
                    float frame2Y =   0.299 * output2.r +  0.587 * output2.g +  0.114 * output2.b; 
                    float frame2Cb = -0.169 * output2.r + -0.331 * output2.g +  0.500 * output2.b + 0.5;
                    float frame2Cr =  0.500 * output2.r + -0.419 * output2.g + -0.081 * output2.b + 0.5;
                    
                    float frame3Y =   0.299 * output3.r +  0.587 * output3.g +  0.114 * output3.b; 
                    float frame3Cb = -0.169 * output3.r + -0.331 * output3.g +  0.500 * output3.b + 0.5;
                    float frame3Cr =  0.500 * output3.r + -0.419 * output3.g + -0.081 * output3.b + 0.5;
                    
                    float frame4Y =   0.299 * output4.r +  0.587 * output4.g +  0.114 * output4.b; 
                    float frame4Cb = -0.169 * output4.r + -0.331 * output4.g +  0.500 * output4.b + 0.5;
                    float frame4Cr =  0.500 * output4.r + -0.419 * output4.g + -0.081 * output4.b + 0.5;
                    
                    float frame5Y =   0.299 * output5.r +  0.587 * output5.g +  0.114 * output5.b; 
                    float frame5Cb = -0.169 * output5.r + -0.331 * output5.g +  0.500 * output5.b + 0.5;
                    float frame5Cr =  0.500 * output5.r + -0.419 * output5.g + -0.081 * output5.b + 0.5;
                    
                    float frame6Y =   0.299 * output6.r +  0.587 * output6.g +  0.114 * output6.b; 
                    float frame6Cb = -0.169 * output6.r + -0.331 * output6.g +  0.500 * output6.b + 0.5;
                    float frame6Cr =  0.500 * output6.r + -0.419 * output6.g + -0.081 * output6.b + 0.5;
                    
                    float frame7Y =   0.299 * output7.r +  0.587 * output7.g +  0.114 * output7.b; 
                    float frame7Cb = -0.169 * output7.r + -0.331 * output7.g +  0.500 * output7.b + 0.5;
                    float frame7Cr =  0.500 * output7.r + -0.419 * output7.g + -0.081 * output7.b + 0.5;
                    
                    float frame8Y =   0.299 * output8.r +  0.587 * output8.g +  0.114 * output8.b; 
                    float frame8Cb = -0.169 * output8.r + -0.331 * output8.g +  0.500 * output8.b + 0.5;
                    float frame8Cr =  0.500 * output8.r + -0.419 * output8.g + -0.081 * output8.b + 0.5;
                    
                    vec3 temporalOut = vec3(0.0); 
                    float YOut = 0.0; 
                    float K = 0.000085; //0.0; //0.000085; //0.02; //5.01; //scale;
                    
                    int refFrame = (incomingFrame - 4 + 9) % 9; 
                    
                    switch(refFrame) {
                        case 0: {
                        	float stat1 = calcStat3(frame8Y, frame0Y, frame1Y);
                            float stat2 = calcStat3(frame7Y, frame0Y, frame2Y);
                            float stat3 = calcStat3(frame6Y, frame0Y, frame3Y);
                            float stat4 = calcStat3(frame5Y, frame0Y, frame4Y);
                            
                            //float stat1a = calcStat2(frame8Y, frame0Y, frame1Y);
                            //float stat2a = calcStat2(frame7Y, frame0Y, frame2Y);
                            //float stat3a = calcStat2(frame6Y, frame0Y, frame3Y);
                            //float stat4a = calcStat2(frame5Y, frame0Y, frame4Y);
                            
                            //float stat1b = calcStat3(frame8Y, frame0Y, frame1Y);
                            //float stat2b = calcStat3(frame7Y, frame0Y, frame2Y);
                            //float stat3b = calcStat3(frame6Y, frame0Y, frame3Y);
                            //float stat4b = calcStat3(frame5Y, frame0Y, frame4Y);
                            
                            //float stat1c = calcStat4(frame8Y, frame0Y, frame1Y);
                            //float stat2c = calcStat4(frame7Y, frame0Y, frame2Y);
                            //float stat3c = calcStat4(frame6Y, frame0Y, frame3Y);
                            //float stat4c = calcStat4(frame5Y, frame0Y, frame4Y);
                            
                            float alpha1 = calcAlpha(stat1, K);
                            float alpha2 = calcAlpha(stat2, K);
                            float alpha3 = calcAlpha(stat3, K); 
                            float alpha4 = calcAlpha(stat4, K); 
                            
                            //float alpha1a = calcAlpha(stat1a, K);
                            //float alpha2a = calcAlpha(stat2a, K);
                            //float alpha3a = calcAlpha(stat3a, K); 
                            //float alpha4a = calcAlpha(stat4a, K); 
                            
                            //float alpha1b = calcAlpha(stat1b, K);
                            //float alpha2b = calcAlpha(stat2b, K);
                            //float alpha3b = calcAlpha(stat3b, K); 
                            //float alpha4b = calcAlpha(stat4b, K); 
                            
                            //float alpha1c = calcAlpha(stat1c, K);
                            //float alpha2c = calcAlpha(stat2c, K);
                            //float alpha3c = calcAlpha(stat3c, K); 
                            //float alpha4c = calcAlpha(stat4c, K); 
                    
                            float numerator = (frame5Y * alpha4 + frame6Y * alpha3 + frame7Y * alpha2 + frame8Y * alpha1 + frame0Y + frame1Y * alpha1 + frame2Y * alpha2 + frame3Y * alpha3 + frame4Y * alpha4); 
                            float denominator = (1.0 + 2.0 * alpha1 + 2.0 * alpha2 + 2.0 * alpha3 + 2.0 * alpha4);
                            
                            YOut = numerator / denominator;
                            
                            //float rOut = YOut + 1.4020 * (frame0Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame0Cb - 128.0) - 0.714136 * (frame0Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame0Cb - 128.0); 
                            float rOut = YOut + 1.4020 * (frame0Cr - 0.5); 
                            float gOut = YOut - 0.3441 * (frame0Cb - 0.5) - 0.714136 * (frame0Cr - 0.5);
                            float bOut = YOut + 1.7720 * (frame0Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3(rOut, gOut, bOut);
                            
                            
                            
                            /*
                            float numeratorA = (frame5Y * alpha4a + frame6Y * alpha3a + frame7Y * alpha2a + frame8Y * alpha1a + frame0Y + frame1Y * alpha1a + frame2Y * alpha2a + frame3Y * alpha3a + frame4Y * alpha4a); 
                            float denominatorA = (1.0 + 2.0 * alpha1a + 2.0 * alpha2a + 2.0 * alpha3a + 2.0 * alpha4a);
                            
                            YOutA = numeratorA / denominatorA;
                            
                            //float rOut = YOut + 1.4020 * (frame0Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame0Cb - 128.0) - 0.714136 * (frame0Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame0Cb - 128.0); 
                            float rOutA = YOutA + 1.4020 * (frame0Cr - 0.5); 
                            float gOutA = YOutA - 0.3441 * (frame0Cb - 0.5) - 0.714136 * (frame0Cr - 0.5);
                            float bOutA = YOutA + 1.7720 * (frame0Cb - 0.5); 
                            
                                 
                            
                            float numeratorB = (frame5Y * alpha4b + frame6Y * alpha3b + frame7Y * alpha2b + frame8Y * alpha1b + frame0Y + frame1Y * alpha1b + frame2Y * alpha2b + frame3Y * alpha3b + frame4Y * alpha4b); 
                            float denominatorB = (1.0 + 2.0 * alpha1b + 2.0 * alpha2b + 2.0 * alpha3b + 2.0 * alpha4b);
                            
                            YOutB = numeratorB / denominatorB;
                            
                            //float rOut = YOut + 1.4020 * (frame0Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame0Cb - 128.0) - 0.714136 * (frame0Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame0Cb - 128.0); 
                            float rOutB = YOutB + 1.4020 * (frame0Cr - 0.5); 
                            float gOutB = YOutB - 0.3441 * (frame0Cb - 0.5) - 0.714136 * (frame0Cr - 0.5);
                            float bOutB = YOutB + 1.7720 * (frame0Cb - 0.5); 
                            
                            
                            
                            float numeratorC = (frame5Y * alpha4a + frame6Y * alpha3a + frame7Y * alpha2a + frame8Y * alpha1a + frame0Y + frame1Y * alpha1a + frame2Y * alpha2a + frame3Y * alpha3a + frame4Y * alpha4a); 
                            float denominatorC = (1.0 + 2.0 * alpha1a + 2.0 * alpha2a + 2.0 * alpha3a + 2.0 * alpha4a);
                            
                            YOutC = numeratorC / denominatorC;
                            
                            //float rOut = YOut + 1.4020 * (frame0Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame0Cb - 128.0) - 0.714136 * (frame0Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame0Cb - 128.0); 
                            float rOutC = YOutC + 1.4020 * (frame0Cr - 0.5); 
                            float gOutC = YOutC - 0.3441 * (frame0Cb - 0.5) - 0.714136 * (frame0Cr - 0.5);
                            float bOutC = YOutC + 1.7720 * (frame0Cb - 0.5); 
                            
                        
                        
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3((rOutA + rOutB + rOutC) / 3.0, (gOutA + gOutB + gOutC) / 3.0, (bOutA + bOutB + bOutC) / 3.0));
                            */
    
                            break;
                        }
                        
                        case 1: {
                        	float stat1 = calcStat3(frame0Y, frame1Y, frame2Y);
                            float stat2 = calcStat3(frame8Y, frame1Y, frame3Y);
                            float stat3 = calcStat3(frame7Y, frame1Y, frame4Y);
                            float stat4 = calcStat3(frame6Y, frame1Y, frame5Y);
                        
                            //float stat1a = calcStat2(frame0Y, frame1Y, frame2Y);
                            //float stat2a = calcStat2(frame8Y, frame1Y, frame3Y);
                            //float stat3a = calcStat2(frame7Y, frame1Y, frame4Y);
                            //float stat4a = calcStat2(frame6Y, frame1Y, frame5Y);
                            
                            //float stat1b = calcStat3(frame0Y, frame1Y, frame2Y);
                            //float stat2b = calcStat3(frame8Y, frame1Y, frame3Y);
                            //float stat3b = calcStat3(frame7Y, frame1Y, frame4Y);
                            //float stat4b = calcStat3(frame6Y, frame1Y, frame5Y);
                            
                            //float stat1c = calcStat4(frame0Y, frame1Y, frame2Y);
                            //float stat2c = calcStat4(frame8Y, frame1Y, frame3Y);
                            //float stat3c = calcStat4(frame7Y, frame1Y, frame4Y);
                            //float stat4c = calcStat4(frame6Y, frame1Y, frame5Y);
                            
                            float alpha1 = calcAlpha(stat1, K);
                            float alpha2 = calcAlpha(stat2, K);
                            float alpha3 = calcAlpha(stat3, K); 
                            float alpha4 = calcAlpha(stat4, K); 
                            
                            //float alpha1a = calcAlpha(stat1a, K);
                            //float alpha2a = calcAlpha(stat2a, K);
                            //float alpha3a = calcAlpha(stat3a, K); 
                            //float alpha4a = calcAlpha(stat4a, K); 
                            
                            //float alpha1b = calcAlpha(stat1b, K);
                            //float alpha2b = calcAlpha(stat2b, K);
                            //float alpha3b = calcAlpha(stat3b, K); 
                            //float alpha4b = calcAlpha(stat4b, K); 
                            
                            //float alpha1c = calcAlpha(stat1c, K);
                            //float alpha2c = calcAlpha(stat2c, K);
                            //float alpha3c = calcAlpha(stat3c, K); 
                            //float alpha4c = calcAlpha(stat4c, K); 
                            
                            float numerator = (frame6Y * alpha4 + frame7Y * alpha3 + frame8Y * alpha2 + frame0Y * alpha1 + frame1Y + frame2Y * alpha1 + frame3Y * alpha2 + frame4Y * alpha3 + frame5Y * alpha4); 
                            float denominator = (1.0 + 2.0 * alpha1 + 2.0 * alpha2 + 2.0 * alpha3 + 2.0 * alpha4);
                            
                            YOut = numerator / denominator;
                            
                            //float rOut = YOut + 1.4020 * (frame1Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame1Cb - 128.0) - 0.714136 * (frame1Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame1Cb - 128.0); 
                            float rOut = YOut + 1.4020 * (frame1Cr - 0.5); 
                            float gOut = YOut - 0.3441 * (frame1Cb - 0.5) - 0.714136 * (frame1Cr - 0.5);
                            float bOut = YOut + 1.7720 * (frame1Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3(rOut, gOut, bOut);
             
             
             
                        	/*
                        	float numeratorA = (frame6Y * alpha4a + frame7Y * alpha3a + frame8Y * alpha2a + frame0Y * alpha1a + frame1Y + frame2Y * alpha1a + frame3Y * alpha2a + frame4Y * alpha3a + frame5Y * alpha4a); 
                            float denominatorA = (1.0 + 2.0 * alpha1a + 2.0 * alpha2a + 2.0 * alpha3a + 2.0 * alpha4a);
                            
                            YOutA = numeratorA / denominatorA;
                            
                            //float rOut = YOut + 1.4020 * (frame1Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame1Cb - 128.0) - 0.714136 * (frame1Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame1Cb - 128.0); 
                            float rOutA = YOutA + 1.4020 * (frame1Cr - 0.5); 
                            float gOutA = YOutA - 0.3441 * (frame1Cb - 0.5) - 0.714136 * (frame1Cr - 0.5);
                            float bOutA = YOutA + 1.7720 * (frame1Cb - 0.5); 
                            
                       
                       		float numeratorB = (frame6Y * alpha4b + frame7Y * alpha3b + frame8Y * alpha2b + frame0Y * alpha1b + frame1Y + frame2Y * alpha1b + frame3Y * alpha2b + frame4Y * alpha3b + frame5Y * alpha4b); 
                            float denominatorB = (1.0 + 2.0 * alpha1b + 2.0 * alpha2b + 2.0 * alpha3b + 2.0 * alpha4b);
                            
                            YOutB = numeratorB / denominatorB;
                            
                            //float rOut = YOut + 1.4020 * (frame1Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame1Cb - 128.0) - 0.714136 * (frame1Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame1Cb - 128.0); 
                            float rOutB = YOutB + 1.4020 * (frame1Cr - 0.5); 
                            float gOutB = YOutB - 0.3441 * (frame1Cb - 0.5) - 0.714136 * (frame1Cr - 0.5);
                            float bOutB = YOutB + 1.7720 * (frame1Cb - 0.5); 
                            
                         	
                         	float numeratorC = (frame6Y * alpha4c + frame7Y * alpha3c + frame8Y * alpha2c + frame0Y * alpha1c + frame1Y + frame2Y * alpha1c + frame3Y * alpha2c + frame4Y * alpha3c + frame5Y * alpha4c); 
                            float denominatorC = (1.0 + 2.0 * alpha1c + 2.0 * alpha2c + 2.0 * alpha3c + 2.0 * alpha4c);
                            
                            YOutC = numeratorC / denominatorC;
                            
                            //float rOut = YOut + 1.4020 * (frame1Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame1Cb - 128.0) - 0.714136 * (frame1Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame1Cb - 128.0); 
                            float rOutC = YOutC + 1.4020 * (frame1Cr - 0.5); 
                            float gOutC = YOutC - 0.3441 * (frame1Cb - 0.5) - 0.714136 * (frame1Cr - 0.5);
                            float bOutC = YOutC + 1.7720 * (frame1Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3((rOutA + rOutB + rOutC) / 3.0, (gOutA + gOutB + gOutC) / 3.0, (bOutA + bOutB + bOutC) / 3.0);
                        	*/
         
                            break; 
                        }
                        
                        case 2: {
                        	float stat1 = calcStat3(frame1Y, frame2Y, frame3Y);
                            float stat2 = calcStat3(frame0Y, frame2Y, frame4Y);
                            float stat3 = calcStat3(frame8Y, frame2Y, frame5Y);
                            float stat4 = calcStat3(frame7Y, frame2Y, frame6Y);
                            
                            //float stat1a = calcStat2(frame1Y, frame2Y, frame3Y);
                            //float stat2a = calcStat2(frame0Y, frame2Y, frame4Y);
                            //float stat3a = calcStat2(frame8Y, frame2Y, frame5Y);
                            //float stat4a = calcStat2(frame7Y, frame2Y, frame6Y);
                            
                            //float stat1b = calcStat3(frame1Y, frame2Y, frame3Y);
                            //float stat2b = calcStat3(frame0Y, frame2Y, frame4Y);
                            //float stat3b = calcStat3(frame8Y, frame2Y, frame5Y);
                            //float stat4b = calcStat3(frame7Y, frame2Y, frame6Y);
                            
                            //float stat1c = calcStat4(frame1Y, frame2Y, frame3Y);
                            //float stat2c = calcStat4(frame0Y, frame2Y, frame4Y);
                            //float stat3c = calcStat4(frame8Y, frame2Y, frame5Y);
                            //float stat4c = calcStat4(frame7Y, frame2Y, frame6Y);
                            
                            float alpha1 = calcAlpha(stat1, K);
                            float alpha2 = calcAlpha(stat2, K);
                            float alpha3 = calcAlpha(stat3, K); 
                            float alpha4 = calcAlpha(stat4, K); 
                            
                            //float alpha1a = calcAlpha(stat1a, K);
                            //float alpha2a = calcAlpha(stat2a, K);
                            //float alpha3a = calcAlpha(stat3a, K); 
                            //float alpha4a = calcAlpha(stat4a, K); 
                            
                            //float alpha1b = calcAlpha(stat1b, K);
                            //float alpha2b = calcAlpha(stat2b, K);
                            //float alpha3b = calcAlpha(stat3b, K); 
                            //float alpha4b = calcAlpha(stat4b, K); 
                            
                            //float alpha1c = calcAlpha(stat1c, K);
                            //float alpha2c = calcAlpha(stat2c, K);
                            //float alpha3c = calcAlpha(stat3c, K); 
                            //float alpha4c = calcAlpha(stat4c, K); 
                            
                            float numerator = (frame7Y * alpha4 + frame8Y * alpha3 + frame0Y * alpha2 + frame1Y * alpha1 + frame2Y + frame3Y * alpha1 + frame4Y * alpha2 + frame5Y * alpha3 + frame6Y * alpha4); 
                            float denominator = (1.0 + 2.0 * alpha1 + 2.0 * alpha2 + 2.0 * alpha3 + 2.0 * alpha4);
                            
                            YOut = numerator / denominator;
                            
                            //float rOut = YOut + 1.4020 * (frame2Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame2Cb - 128.0) - 0.714136 * (frame2Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame2Cb - 128.0); 
                            float rOut = YOut + 1.4020 * (frame2Cr - 0.5); 
                            float gOut = YOut - 0.3441 * (frame2Cb - 0.5) - 0.714136 * (frame2Cr - 0.5);
                            float bOut = YOut + 1.7720 * (frame2Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3(rOut, gOut, bOut);
                            
                              
                        	/*
                            float numeratorA = (frame7Y * alpha4a + frame8Y * alpha3a + frame0Y * alpha2a + frame1Y * alpha1a + frame2Y + frame3Y * alpha1a + frame4Y * alpha2a + frame5Y * alpha3a + frame6Y * alpha4a); 
                            float denominatorA = (1.0 + 2.0 * alpha1a + 2.0 * alpha2a + 2.0 * alpha3a + 2.0 * alpha4a);
                            
                            YOutA = numeratorA / denominatorA;
                            
                            //float rOut = YOut + 1.4020 * (frame2Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame2Cb - 128.0) - 0.714136 * (frame2Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame2Cb - 128.0); 
                            float rOutA = YOutA + 1.4020 * (frame2Cr - 0.5); 
                            float gOutA = YOutA - 0.3441 * (frame2Cb - 0.5) - 0.714136 * (frame2Cr - 0.5);
                            float bOutA = YOutA + 1.7720 * (frame2Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                        
                        
                        	float numeratorB = (frame7Y * alpha4b + frame8Y * alpha3b + frame0Y * alpha2b + frame1Y * alpha1b + frame2Y + frame3Y * alpha1b + frame4Y * alpha2b + frame5Y * alpha3b + frame6Y * alpha4b); 
                            float denominatorB = (1.0 + 2.0 * alpha1b + 2.0 * alpha2b + 2.0 * alpha3b + 2.0 * alpha4b);
                            
                            YOutB = numeratorB / denominatorB;
                            
                            //float rOut = YOut + 1.4020 * (frame2Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame2Cb - 128.0) - 0.714136 * (frame2Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame2Cb - 128.0); 
                            float rOutB = YOutB + 1.4020 * (frame2Cr - 0.5); 
                            float gOutB = YOutB - 0.3441 * (frame2Cb - 0.5) - 0.714136 * (frame2Cr - 0.5);
                            float bOutB = YOutB + 1.7720 * (frame2Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                      
                      	
                      		float numeratorC = (frame7Y * alpha4c + frame8Y * alpha3c + frame0Y * alpha2c + frame1Y * alpha1c + frame2Y + frame3Y * alpha1c + frame4Y * alpha2c + frame5Y * alpha3c + frame6Y * alpha4c); 
                            float denominatorC = (1.0 + 2.0 * alpha1c + 2.0 * alpha2c + 2.0 * alpha3c + 2.0 * alpha4c);
                            
                            YOutC = numeratorC / denominatorC;
                            
                            //float rOut = YOut + 1.4020 * (frame2Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame2Cb - 128.0) - 0.714136 * (frame2Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame2Cb - 128.0); 
                            float rOutC = YOutC + 1.4020 * (frame2Cr - 0.5); 
                            float gOutC = YOutC - 0.3441 * (frame2Cb - 0.5) - 0.714136 * (frame2Cr - 0.5);
                            float bOutC = YOutC + 1.7720 * (frame2Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3((rOutA + rOutB + rOutC) / 3.0, (gOutA + gOutB + gOutC) / 3.0, (bOutA + bOutB + bOutC) / 3.0);
                            */
                            
                            break;
                        }
                        
                        case 3: {
                        	float stat1 = calcStat3(frame2Y, frame3Y, frame4Y);
                            float stat2 = calcStat3(frame1Y, frame3Y, frame5Y);
                            float stat3 = calcStat3(frame0Y, frame3Y, frame6Y);
                            float stat4 = calcStat3(frame8Y, frame3Y, frame7Y);
                            
                            //float stat1a = calcStat2(frame2Y, frame3Y, frame4Y);
                            //float stat2a = calcStat2(frame1Y, frame3Y, frame5Y);
                            //float stat3a = calcStat2(frame0Y, frame3Y, frame6Y);
                            //float stat4a = calcStat2(frame8Y, frame3Y, frame7Y);
                            
                            //float stat1b = calcStat3(frame2Y, frame3Y, frame4Y);
                            //float stat2b = calcStat3(frame1Y, frame3Y, frame5Y);
                            //float stat3b = calcStat3(frame0Y, frame3Y, frame6Y);
                            //float stat4b = calcStat3(frame8Y, frame3Y, frame7Y);
                            
                            //float stat1c = calcStat4(frame2Y, frame3Y, frame4Y);
                            //float stat2c = calcStat4(frame1Y, frame3Y, frame5Y);
                            //float stat3c = calcStat4(frame0Y, frame3Y, frame6Y);
                            //float stat4c = calcStat4(frame8Y, frame3Y, frame7Y);
                            
                            float alpha1 = calcAlpha(stat1, K);
                            float alpha2 = calcAlpha(stat2, K);
                            float alpha3 = calcAlpha(stat3, K); 
                            float alpha4 = calcAlpha(stat4, K); 
                            
                            //float alpha1a = calcAlpha(stat1a, K);
                            //float alpha2a = calcAlpha(stat2a, K);
                            //float alpha3a = calcAlpha(stat3a, K); 
                            //float alpha4a = calcAlpha(stat4a, K); 
                            
                            //float alpha1b = calcAlpha(stat1b, K);
                            //float alpha2b = calcAlpha(stat2b, K);
                            //float alpha3b = calcAlpha(stat3b, K); 
                            //float alpha4b = calcAlpha(stat4b, K); 
                            
                            //float alpha1c = calcAlpha(stat1c, K);
                            //float alpha2c = calcAlpha(stat2c, K);
                            //float alpha3c = calcAlpha(stat3c, K); 
                            //float alpha4c = calcAlpha(stat4c, K); 
                            
                
                            float numerator = (frame8Y * alpha4 + frame0Y * alpha3 + frame1Y * alpha2 + frame2Y * alpha1 + frame3Y + frame4Y * alpha1 + frame5Y * alpha2 + frame6Y * alpha3 + frame7Y * alpha4); 
                            float denominator = (1.0 + 2.0 * alpha1 + 2.0 * alpha2 + 2.0 * alpha3 + 2.0 * alpha4);
                            
                            YOut = numerator / denominator;
                            
                            //float rOut = YOut + 1.4020 * (frame3Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame3Cb - 128.0) - 0.714136 * (frame3Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame3Cb - 128.0); 
                            float rOut = YOut + 1.4020 * (frame3Cr - 0.5); 
                            float gOut = YOut - 0.3441 * (frame3Cb - 0.5) - 0.714136 * (frame3Cr - 0.5);
                            float bOut = YOut + 1.7720 * (frame3Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3(rOut, gOut, bOut);
        
                            
                            /*
                            float numeratorA = (frame8Y * alpha4a + frame0Y * alpha3a + frame1Y * alpha2a + frame2Y * alpha1a + frame3Y + frame4Y * alpha1a + frame5Y * alpha2a + frame6Y * alpha3a + frame7Y * alpha4a); 
                            float denominatorA = (1.0 + 2.0 * alpha1a + 2.0 * alpha2a + 2.0 * alpha3a + 2.0 * alpha4a);
                            
                            YOutA = numeratorA / denominatorA;
                            
                            //float rOut = YOut + 1.4020 * (frame3Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame3Cb - 128.0) - 0.714136 * (frame3Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame3Cb - 128.0); 
                            float rOutA = YOutA + 1.4020 * (frame3Cr - 0.5); 
                            float gOutA = YOutA - 0.3441 * (frame3Cb - 0.5) - 0.714136 * (frame3Cr - 0.5);
                            float bOutA = YOutA + 1.7720 * (frame3Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            
                            
                            float numeratorB = (frame8Y * alpha4b + frame0Y * alpha3b + frame1Y * alpha2b + frame2Y * alpha1b + frame3Y + frame4Y * alpha1b + frame5Y * alpha2b + frame6Y * alpha3b + frame7Y * alpha4b); 
                            float denominatorB = (1.0 + 2.0 * alpha1b + 2.0 * alpha2b + 2.0 * alpha3b + 2.0 * alpha4b);
                            
                            YOutB = numeratorB / denominatorB;
                            
                            //float rOut = YOut + 1.4020 * (frame3Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame3Cb - 128.0) - 0.714136 * (frame3Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame3Cb - 128.0); 
                            float rOutB = YOutB + 1.4020 * (frame3Cr - 0.5); 
                            float gOutB = YOutB - 0.3441 * (frame3Cb - 0.5) - 0.714136 * (frame3Cr - 0.5);
                            float bOutB = YOutB + 1.7720 * (frame3Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            
                            
                            float numeratorC = (frame8Y * alpha4c + frame0Y * alpha3c + frame1Y * alpha2c + frame2Y * alpha1c + frame3Y + frame4Y * alpha1c + frame5Y * alpha2c + frame6Y * alpha3c + frame7Y * alpha4c); 
                            float denominatorC = (1.0 + 2.0 * alpha1c + 2.0 * alpha2c + 2.0 * alpha3c + 2.0 * alpha4c);
                            
                            YOutC = numeratorC / denominatorC;
                            
                            //float rOut = YOut + 1.4020 * (frame3Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame3Cb - 128.0) - 0.714136 * (frame3Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame3Cb - 128.0); 
                            float rOutC = YOutC + 1.4020 * (frame3Cr - 0.5); 
                            float gOutC = YOutC - 0.3441 * (frame3Cb - 0.5) - 0.714136 * (frame3Cr - 0.5);
                            float bOutC = YOutC + 1.7720 * (frame3Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3((rOutA + rOutB + rOutC) / 3.0, (gOutA + gOutB + gOutC) / 3.0, (bOutA + bOutB + bOutC) / 3.0); 
                            */
                    
                            break;
                        }
                        
                        case 4: {
                        	float stat1 = calcStat3(frame3Y, frame4Y, frame5Y);
                            float stat2 = calcStat3(frame2Y, frame4Y, frame6Y);
                            float stat3 = calcStat3(frame1Y, frame4Y, frame7Y);
                            float stat4 = calcStat3(frame0Y, frame4Y, frame8Y);
                            
                            //float stat1a = calcStat2(frame3Y, frame4Y, frame5Y);
                            //float stat2a = calcStat2(frame2Y, frame4Y, frame6Y);
                            //float stat3a = calcStat2(frame1Y, frame4Y, frame7Y);
                            //float stat4a = calcStat2(frame0Y, frame4Y, frame8Y);
                            
                            //float stat1b = calcStat3(frame3Y, frame4Y, frame5Y);
                            //float stat2b = calcStat3(frame2Y, frame4Y, frame6Y);
                            //float stat3b = calcStat3(frame1Y, frame4Y, frame7Y);
                            //float stat4b = calcStat3(frame0Y, frame4Y, frame8Y);
                            
                            //float stat1c = calcStat4(frame3Y, frame4Y, frame5Y);
                            //float stat2c = calcStat4(frame2Y, frame4Y, frame6Y);
                            //float stat3c = calcStat4(frame1Y, frame4Y, frame7Y);
                            //float stat4c = calcStat4(frame0Y, frame4Y, frame8Y);
                            
    
                            float alpha1 = calcAlpha(stat1, K);
                            float alpha2 = calcAlpha(stat2, K);
                            float alpha3 = calcAlpha(stat3, K); 
                            float alpha4 = calcAlpha(stat4, K); 
                            
                            //float alpha1a = calcAlpha(stat1a, K);
                            //float alpha2a = calcAlpha(stat2a, K);
                            //float alpha3a = calcAlpha(stat3a, K); 
                            //float alpha4a = calcAlpha(stat4a, K); 
                            
                            //float alpha1b = calcAlpha(stat1b, K);
                            //float alpha2b = calcAlpha(stat2b, K);
                            //float alpha3b = calcAlpha(stat3b, K); 
                            //float alpha4b = calcAlpha(stat4b, K); 
                            
                            //float alpha1c = calcAlpha(stat1c, K);
                            //float alpha2c = calcAlpha(stat2c, K);
                            //float alpha3c = calcAlpha(stat3c, K); 
                            //float alpha4c = calcAlpha(stat4c, K); 
                            
                            
                            float numerator = (frame0Y * alpha4 + frame1Y * alpha3 + frame2Y * alpha2 + frame3Y * alpha1 + frame4Y + frame5Y * alpha1 + frame6Y * alpha2 + frame7Y * alpha3 + frame8Y * alpha4); 
                            float denominator = (1.0 + 2.0 * alpha1 + 2.0 * alpha2 + 2.0 * alpha3 + 2.0 * alpha4);
                            
                            YOut = numerator / denominator;
                            
                            //float rOut = YOut + 1.4020 * (frame4Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame4Cb - 128.0) - 0.714136 * (frame4Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame4Cb - 128.0); 
                            float rOut = YOut + 1.4020 * (frame4Cr - 0.5); 
                            float gOut = YOut - 0.3441 * (frame4Cb - 0.5) - 0.714136 * (frame4Cr - 0.5);
                            float bOut = YOut + 1.7720 * (frame4Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3(rOut, gOut, bOut);
                            
                            
                            /*
                            float numeratorA = (frame0Y * alpha4a + frame1Y * alpha3a + frame2Y * alpha2a + frame3Y * alpha1a + frame4Y + frame5Y * alpha1a + frame6Y * alpha2a + frame7Y * alpha3a + frame8Y * alpha4a); 
                            float denominatorA = (1.0 + 2.0 * alpha1a + 2.0 * alpha2a + 2.0 * alpha3a + 2.0 * alpha4a);
                            
                            YOutA = numeratorA / denominatorA;
                            
                            //float rOut = YOut + 1.4020 * (frame4Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame4Cb - 128.0) - 0.714136 * (frame4Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame4Cb - 128.0); 
                            float rOutA = YOutA + 1.4020 * (frame4Cr - 0.5); 
                            float gOutA = YOutA - 0.3441 * (frame4Cb - 0.5) - 0.714136 * (frame4Cr - 0.5);
                            float bOutA = YOutA + 1.7720 * (frame4Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                  
                  
                  			float numeratorB = (frame0Y * alpha4b + frame1Y * alpha3b + frame2Y * alpha2b + frame3Y * alpha1b + frame4Y + frame5Y * alpha1b + frame6Y * alpha2b + frame7Y * alpha3b + frame8Y * alpha4b); 
                            float denominatorB = (1.0 + 2.0 * alpha1b + 2.0 * alpha2b + 2.0 * alpha3b + 2.0 * alpha4b);
                            
                            YOutB = numeratorB / denominatorB;
                            
                            //float rOut = YOut + 1.4020 * (frame4Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame4Cb - 128.0) - 0.714136 * (frame4Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame4Cb - 128.0); 
                            float rOutB = YOutB + 1.4020 * (frame4Cr - 0.5); 
                            float gOutB = YOutB - 0.3441 * (frame4Cb - 0.5) - 0.714136 * (frame4Cr - 0.5);
                            float bOutB = YOutB + 1.7720 * (frame4Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                  
                  
                  			float numeratorC = (frame0Y * alpha4c + frame1Y * alpha3c + frame2Y * alpha2c + frame3Y * alpha1c + frame4Y + frame5Y * alpha1c + frame6Y * alpha2c + frame7Y * alpha3c + frame8Y * alpha4c); 
                            float denominatorC = (1.0 + 2.0 * alpha1c + 2.0 * alpha2c + 2.0 * alpha3c + 2.0 * alpha4c);
                            
                            YOutC = numeratorC / denominatorC;
                            
                            //float rOut = YOut + 1.4020 * (frame4Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame4Cb - 128.0) - 0.714136 * (frame4Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame4Cb - 128.0); 
                            float rOutC = YOutC + 1.4020 * (frame4Cr - 0.5); 
                            float gOutC = YOutC - 0.3441 * (frame4Cb - 0.5) - 0.714136 * (frame4Cr - 0.5);
                            float bOutC = YOutC + 1.7720 * (frame4Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3((rOutA + rOutB + rOutC) / 3.0, (gOutA + gOutB + gOutC) / 3.0, (bOutA + bOutB + bOutC) / 3.0);
                            */
                  
                            break;
                        }
                        
                        case 5: {
                        	float stat1 = calcStat3(frame4Y, frame5Y, frame6Y);
                            float stat2 = calcStat3(frame3Y, frame5Y, frame7Y);
                            float stat3 = calcStat3(frame2Y, frame5Y, frame8Y);
                            float stat4 = calcStat3(frame1Y, frame5Y, frame0Y);
                            
                            //float stat1a = calcStat2(frame4Y, frame5Y, frame6Y);
                            //float stat2a = calcStat2(frame3Y, frame5Y, frame7Y);
                            //float stat3a = calcStat2(frame2Y, frame5Y, frame8Y);
                            //float stat4a = calcStat2(frame1Y, frame5Y, frame0Y);
                            
                            //float stat1b = calcStat3(frame4Y, frame5Y, frame6Y);
                            //float stat2b = calcStat3(frame3Y, frame5Y, frame7Y);
                            //float stat3b = calcStat3(frame2Y, frame5Y, frame8Y);
                            //float stat4b = calcStat3(frame1Y, frame5Y, frame0Y);
                            
                            //float stat1c = calcStat4(frame4Y, frame5Y, frame6Y);
                            //float stat2c = calcStat4(frame3Y, frame5Y, frame7Y);
                            //float stat3c = calcStat4(frame2Y, frame5Y, frame8Y);
                            //float stat4c = calcStat4(frame1Y, frame5Y, frame0Y);
                            
                            float alpha1 = calcStat(stat1, K);
                            float alpha2 = calcStat(stat2, K);
                            float alpha3 = calcStat(stat3, K); 
                            float alpha4 = calcStat(stat4, K); 
                            
                            //float alpha1a = calcStat(stat1a, K);
                            //float alpha2a = calcStat(stat2a, K);
                            //float alpha3a = calcStat(stat3a, K); 
                            //float alpha4a = calcStat(stat4a, K); 
                            
                            //float alpha1b = calcStat(stat1b, K);
                            //float alpha2b = calcStat(stat2b, K);
                            //float alpha3b = calcStat(stat3b, K); 
                            //float alpha4b = calcStat(stat4b, K); 
                            
                            //float alpha1c = calcStat(stat1c, K);
                            //float alpha2c = calcStat(stat2c, K);
                            //float alpha3c = calcStat(stat3c, K); 
                            //float alpha4c = calcStat(stat4c, K); 
    
                            float numerator = (frame1Y * alpha4 + frame2Y * alpha3 + frame3Y * alpha2 + frame4Y * alpha1 + frame5Y + frame6Y * alpha1 + frame7Y * alpha2 + frame8Y * alpha3 + frame0Y * alpha4); 
                            float denominator = (1.0 + 2.0 * alpha1 + 2.0 * alpha2 + 2.0 * alpha3 + 2.0 * alpha4);
                            
                            YOut = numerator / denominator;
                            
                            //float rOut = YOut + 1.4020 * (frame5Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame5Cb - 128.0) - 0.714136 * (frame5Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame5Cb - 128.0); 
                            float rOut = YOut + 1.4020 * (frame5Cr - 0.5); 
                            float gOut = YOut - 0.3441 * (frame5Cb - 0.5) - 0.714136 * (frame5Cr - 0.5);
                            float bOut = YOut + 1.7720 * (frame5Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3(rOut, gOut, bOut);
                            
                            
                            
                            /*
                            float numeratorA = (frame1Y * alpha4a + frame2Y * alpha3a + frame3Y * alpha2a + frame4Y * alpha1a + frame5Y + frame6Y * alpha1a + frame7Y * alpha2a + frame8Y * alpha3a + frame0Y * alpha4a); 
                            float denominatorA = (1.0 + 2.0 * alpha1a + 2.0 * alpha2a + 2.0 * alpha3a + 2.0 * alpha4a);
                            
                            YOutA = numeratorA / denominatorA;
                            
                            //float rOut = YOut + 1.4020 * (frame5Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame5Cb - 128.0) - 0.714136 * (frame5Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame5Cb - 128.0); 
                            float rOutA = YOutA + 1.4020 * (frame5Cr - 0.5); 
                            float gOutA = YOutA - 0.3441 * (frame5Cb - 0.5) - 0.714136 * (frame5Cr - 0.5);
                            float bOutA = YOutA + 1.7720 * (frame5Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            
                            
                            float numeratorB = (frame1Y * alpha4b + frame2Y * alpha3b + frame3Y * alpha2b + frame4Y * alpha1b + frame5Y + frame6Y * alpha1b + frame7Y * alpha2b + frame8Y * alpha3b + frame0Y * alpha4b); 
                            float denominatorB = (1.0 + 2.0 * alpha1b + 2.0 * alpha2b + 2.0 * alpha3b + 2.0 * alpha4b);
                            
                            YOutB = numeratorB / denominatorB;
                            
                            //float rOut = YOut + 1.4020 * (frame5Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame5Cb - 128.0) - 0.714136 * (frame5Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame5Cb - 128.0); 
                            float rOutB = YOutB + 1.4020 * (frame5Cr - 0.5); 
                            float gOutB = YOutB - 0.3441 * (frame5Cb - 0.5) - 0.714136 * (frame5Cr - 0.5);
                            float bOutB = YOutB + 1.7720 * (frame5Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            
                            
                            float numeratorC = (frame1Y * alpha4c + frame2Y * alpha3c + frame3Y * alpha2c + frame4Y * alpha1c + frame5Y + frame6Y * alpha1c + frame7Y * alpha2c + frame8Y * alpha3c + frame0Y * alpha4c); 
                            float denominatorC = (1.0 + 2.0 * alpha1c + 2.0 * alpha2c + 2.0 * alpha3c + 2.0 * alpha4c);
                            
                            YOutC = numeratorC / denominatorC;
                            
                            //float rOut = YOut + 1.4020 * (frame5Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame5Cb - 128.0) - 0.714136 * (frame5Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame5Cb - 128.0); 
                            float rOutC = YOutC + 1.4020 * (frame5Cr - 0.5); 
                            float gOutC = YOutC - 0.3441 * (frame5Cb - 0.5) - 0.714136 * (frame5Cr - 0.5);
                            float bOutC = YOutC + 1.7720 * (frame5Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                        	temporalOut = vec3((rOutA + rOutB + rOutC) / 3.0, (gOutA + gOutB + gOutC) / 3.0, (bOutA + bOutB + bOutC) / 3.0);
                         
                            */
                       
                            break;
                        }
                        
                        case 6: {
                        	float stat1 = calcStat3(frame5Y, frame6Y, frame7Y);
                            float stat2 = calcStat3(frame4Y, frame6Y, frame8Y);
                            float stat3 = calcStat3(frame3Y, frame6Y, frame0Y);
                            float stat4 = calcStat3(frame2Y, frame6Y, frame1Y);
                            
                            //float stat1a = calcStat2(frame5Y, frame6Y, frame7Y);
                            //float stat2a = calcStat2(frame4Y, frame6Y, frame8Y);
                            //float stat3a = calcStat2(frame3Y, frame6Y, frame0Y);
                            //float stat4a = calcStat2(frame2Y, frame6Y, frame1Y);
                            
                            //float stat1b = calcStat3(frame5Y, frame6Y, frame7Y);
                            //float stat2b = calcStat3(frame4Y, frame6Y, frame8Y);
                            //float stat3b = calcStat3(frame3Y, frame6Y, frame0Y);
                            //float stat4b = calcStat3(frame2Y, frame6Y, frame1Y);
                            
                            //float stat1c = calcStat4(frame5Y, frame6Y, frame7Y);
                            //float stat2c = calcStat4(frame4Y, frame6Y, frame8Y);
                            //float stat3c = calcStat4(frame3Y, frame6Y, frame0Y);
                            //float stat4c = calcStat4(frame2Y, frame6Y, frame1Y);
                            
                            float alpha1 = calcAlpha(stat1, K);
                            float alpha2 = calcAlpha(stat2, K);
                            float alpha3 = calcAlpha(stat3, K); 
                            float alpha4 = calcAlpha(stat4, K); 
                            
                            //float alpha1a = calcAlpha(stat1a, K);
                            //float alpha2a = calcAlpha(stat2a, K);
                            //float alpha3a = calcAlpha(stat3a, K); 
                            //float alpha4a = calcAlpha(stat4a, K); 
                            
                            //float alpha1b = calcAlpha(stat1b, K);
                            //float alpha2b = calcAlpha(stat2b, K);
                            //float alpha3b = calcAlpha(stat3b, K); 
                            //float alpha4b = calcAlpha(stat4b, K); 
                            
                            //float alpha1c = calcAlpha(stat1c, K);
                            //float alpha2c = calcAlpha(stat2c, K);
                            //float alpha3c = calcAlpha(stat3c, K); 
                            //float alpha4c = calcAlpha(stat4c, K); 
                            
                            float numerator = (frame2Y * alpha4 + frame3Y * alpha3 + frame4Y * alpha2 + frame5Y * alpha1 + frame6Y + frame7Y * alpha1 + frame8Y * alpha2 + frame0Y * alpha3 + frame1Y * alpha4); 
                            float denominator = (1.0 + 2.0 * alpha1 + 2.0 * alpha2 + 2.0 * alpha3 + 2.0 * alpha4);
                            
                            YOut = numerator / denominator;
                            
                            //float rOut = YOut + 1.4020 * (frame6Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame6Cb - 128.0) - 0.714136 * (frame6Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame6Cb - 128.0); 
                            float rOut = YOut + 1.4020 * (frame6Cr - 0.5); 
                            float gOut = YOut - 0.3441 * (frame6Cb - 0.5) - 0.714136 * (frame6Cr - 0.5);
                            float bOut = YOut + 1.7720 * (frame6Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3(rOut, gOut, bOut);
                            
                            
                            
                            /*
                            float numeratorA = (frame2Y * alpha4a + frame3Y * alpha3a + frame4Y * alpha2a + frame5Y * alpha1a + frame6Y + frame7Y * alpha1a + frame8Y * alpha2a + frame0Y * alpha3a + frame1Y * alpha4a); 
                            float denominatorA = (1.0 + 2.0 * alpha1a + 2.0 * alpha2a + 2.0 * alpha3a + 2.0 * alpha4a);
                            
                            YOutA = numeratorA / denominatorA;
                            
                            //float rOut = YOut + 1.4020 * (frame6Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame6Cb - 128.0) - 0.714136 * (frame6Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame6Cb - 128.0); 
                            float rOutA = YOutA + 1.4020 * (frame6Cr - 0.5); 
                            float gOutA = YOutA - 0.3441 * (frame6Cb - 0.5) - 0.714136 * (frame6Cr - 0.5);
                            float bOutA = YOutA + 1.7720 * (frame6Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            
                            
                            float numeratorB = (frame2Y * alpha4b + frame3Y * alpha3b + frame4Y * alpha2b + frame5Y * alpha1b + frame6Y + frame7Y * alpha1b + frame8Y * alpha2b + frame0Y * alpha3b + frame1Y * alpha4b); 
                            float denominatorB = (1.0 + 2.0 * alpha1b + 2.0 * alpha2b + 2.0 * alpha3b + 2.0 * alpha4b);
                            
                            YOutB = numeratorB / denominatorB;
                            
                            //float rOut = YOut + 1.4020 * (frame6Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame6Cb - 128.0) - 0.714136 * (frame6Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame6Cb - 128.0); 
                            float rOutB = YOutB + 1.4020 * (frame6Cr - 0.5); 
                            float gOutB = YOutB - 0.3441 * (frame6Cb - 0.5) - 0.714136 * (frame6Cr - 0.5);
                            float bOutB = YOutB + 1.7720 * (frame6Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            
                            
                            float numeratorC = (frame2Y * alpha4c + frame3Y * alpha3c + frame4Y * alpha2c + frame5Y * alpha1c + frame6Y + frame7Y * alpha1c + frame8Y * alpha2c + frame0Y * alpha3c + frame1Y * alpha4c); 
                            float denominatorC = (1.0 + 2.0 * alpha1c + 2.0 * alpha2c + 2.0 * alpha3c + 2.0 * alpha4c);
                            
                            YOutC = numeratorC / denominatorC;
                            
                            //float rOut = YOut + 1.4020 * (frame6Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame6Cb - 128.0) - 0.714136 * (frame6Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame6Cb - 128.0); 
                            float rOutC = YOutC + 1.4020 * (frame6Cr - 0.5); 
                            float gOutC = YOutC - 0.3441 * (frame6Cb - 0.5) - 0.714136 * (frame6Cr - 0.5);
                            float bOutC = YOutC + 1.7720 * (frame6Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                           	temporalOut = vec3((rOutA + rOutB + rOutC) / 3.0, (gOutA + gOutB + gOutC) / 3.0, (bOutA + bOutB + bOutC) / 3.0);
                         
                            */
                            
                            break;
                        }
                        
                        case 7: {
                            float stat1 = calcStat3(frame6Y, frame7Y, frame8Y);
                            float stat2 = calcStat3(frame5Y, frame7Y, frame0Y);
                            float stat3 = calcStat3(frame4Y, frame7Y, frame1Y);
                            float stat4 = calcStat3(frame3Y, frame7Y, frame2Y);
                            
                            //float stat1a = calcStat2(frame6Y, frame7Y, frame8Y);
                            //float stat2a = calcStat2(frame5Y, frame7Y, frame0Y);
                            //float stat3a = calcStat2(frame4Y, frame7Y, frame1Y);
                            //float stat4a = calcStat2(frame3Y, frame7Y, frame2Y);
                            
                            //float stat1b = calcStat3(frame6Y, frame7Y, frame8Y);
                            //float stat2b = calcStat3(frame5Y, frame7Y, frame0Y);
                            //float stat3b = calcStat3(frame4Y, frame7Y, frame1Y);
                            //float stat4b = calcStat3(frame3Y, frame7Y, frame2Y);
                            
                            //float stat1c = calcStat4(frame6Y, frame7Y, frame8Y);
                            //float stat2c = calcStat4(frame5Y, frame7Y, frame0Y);
                            //float stat3c = calcStat4(frame4Y, frame7Y, frame1Y);
                            //float stat4c = calcStat4(frame3Y, frame7Y, frame2Y);
                            
                            float alpha1 = calcAlpha(stat1, K);
                            float alpha2 = calcAlpha(stat2, K);
                            float alpha3 = calcAlpha(stat3, K); 
                            float alpha4 = calcAlpha(stat4, K); 
                            
                            //float alpha1a = calcAlpha(stat1a, K);
                            //float alpha2a = calcAlpha(stat2a, K);
                            //float alpha3a = calcAlpha(stat3a, K); 
                            //float alpha4a = calcAlpha(stat4a, K); 
                            
                            //float alpha1b = calcAlpha(stat1b, K);
                            //float alpha2b = calcAlpha(stat2b, K);
                            //float alpha3b = calcAlpha(stat3b, K); 
                            //float alpha4b = calcAlpha(stat4b, K); 
                            
                            //float alpha1c = calcAlpha(stat1c, K);
                            //float alpha2c = calcAlpha(stat2c, K);
                            //float alpha3c = calcAlpha(stat3c, K); 
                            //float alpha4c = calcAlpha(stat4c, K); 
                            
                            float numeratorC = (frame3Y * alpha4c + frame4Y * alpha3c + frame5Y * alpha2c + frame6Y * alpha1c + frame7Y + frame8Y * alpha1c + frame0Y * alpha2c + frame1Y * alpha3c + frame2Y * alpha4c); 
                            float denominatorC = (1.0 + 2.0 * alpha1c + 2.0 * alpha2c + 2.0 * alpha3c + 2.0 * alpha4c);
                            
                            YOutC = numeratorC / denominatorC;
                            
                            //float rOut = YOut + 1.4020 * (frame7Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame7Cb - 128.0) - 0.714136 * (frame7Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame7Cb - 128.0); 
                            float rOutC = YOutC + 1.4020 * (frame7Cr - 0.5); 
                            float gOutC = YOutC - 0.3441 * (frame7Cb - 0.5) - 0.714136 * (frame7Cr - 0.5);
                            float bOutC = YOutC + 1.7720 * (frame7Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
     						temporalOut = vec3((rOutA + rOutB + rOutC) / 3.0, (gOutA + gOutB + gOutC) / 3.0, (bOutA + bOutB + bOutC) / 3.0);
                      
                            break;
                        }
                        
                        case 8: {
                        	float stat1 = calcStat3(frame7Y, frame8Y, frame0Y);
                            float stat2 = calcStat3(frame6Y, frame8Y, frame1Y);
                            float stat3 = calcStat3(frame5Y, frame8Y, frame2Y);
                            float stat4 = calcStat3(frame4Y, frame8Y, frame3Y);
                        
                            //float stat1a = calcStat2(frame7Y, frame8Y, frame0Y);
                            //float stat2a = calcStat2(frame6Y, frame8Y, frame1Y);
                            //float stat3a = calcStat2(frame5Y, frame8Y, frame2Y);
                            //float stat4a = calcStat2(frame4Y, frame8Y, frame3Y);
                            
                            //float stat1b = calcStat3(frame7Y, frame8Y, frame0Y);
                            //float stat2b = calcStat3(frame6Y, frame8Y, frame1Y);
                            //float stat3b = calcStat3(frame5Y, frame8Y, frame2Y);
                            //float stat4b = calcStat3(frame4Y, frame8Y, frame3Y);
                            
                            //float stat1c = calcStat4(frame7Y, frame8Y, frame0Y);
                            //float stat2c = calcStat4(frame6Y, frame8Y, frame1Y);
                            //float stat3c = calcStat4(frame5Y, frame8Y, frame2Y);
                            //float stat4c = calcStat4(frame4Y, frame8Y, frame3Y);
                            
                            float alpha1 = calcAlpha(stat1, K);
                            float alpha2 = calcAlpha(stat2, K);
                            float alpha3 = calcAlpha(stat3, K); 
                            float alpha4 = calcAlpha(stat4, K); 
                            
                            //float alpha1a = calcAlpha(stat1a, K);
                            //float alpha2a = calcAlpha(stat2a, K);
                            //float alpha3a = calcAlpha(stat3a, K); 
                            //float alpha4a = calcAlpha(stat4a, K); 
                            
                            //float alpha1b = calcAlpha(stat1b, K);
                            //float alpha2b = calcAlpha(stat2b, K);
                            //float alpha3b = calcAlpha(stat3b, K); 
                            //float alpha4b = calcAlpha(stat4b, K); 
                            
                            //float alpha1c = calcAlpha(stat1c, K);
                            //float alpha2c = calcAlpha(stat2c, K);
                            //float alpha3c = calcAlpha(stat3c, K); 
                            //float alpha4c = calcAlpha(stat4c, K); 
                            
                            float numerator = (frame4Y * alpha4 + frame5Y * alpha3 + frame6Y * alpha2 + frame7Y * alpha1 + frame8Y + frame0Y * alpha1 + frame1Y * alpha2 + frame2Y * alpha3 + frame3Y * alpha4); 
                            float denominator = (1.0 + 2.0 * alpha1 + 2.0 * alpha2 + 2.0 * alpha3 + 2.0 * alpha4);
                            
                            YOut = numerator / denominator;
                            
                            //float rOut = YOut + 1.4020 * (frame8Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame8Cb - 128.0) - 0.714136 * (frame8Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame8Cb - 128.0); 
                            float rOut = YOut + 1.4020 * (frame8Cr - 0.5); 
                            float gOut = YOut - 0.3441 * (frame8Cb - 0.5) - 0.714136 * (frame8Cr - 0.5);
                            float bOut = YOut + 1.7720 * (frame8Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3(rOut, gOut, bOut);
                            
                            
                            /*
                            float numeratorA = (frame4Y * alpha4a + frame5Y * alpha3a + frame6Y * alpha2a + frame7Y * alpha1a + frame8Y + frame0Y * alpha1a + frame1Y * alpha2a + frame2Y * alpha3a + frame3Y * alpha4a); 
                            float denominatorA = (1.0 + 2.0 * alpha1a + 2.0 * alpha2a + 2.0 * alpha3a + 2.0 * alpha4a);
                            
                            YOutA = numeratorA / denominatorA;
                            
                            //float rOut = YOut + 1.4020 * (frame8Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame8Cb - 128.0) - 0.714136 * (frame8Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame8Cb - 128.0); 
                            float rOutA = YOutA + 1.4020 * (frame8Cr - 0.5); 
                            float gOutA = YOutA - 0.3441 * (frame8Cb - 0.5) - 0.714136 * (frame8Cr - 0.5);
                            float bOutA = YOutA + 1.7720 * (frame8Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            
                            
                            float numeratorB = (frame4Y * alpha4b + frame5Y * alpha3b + frame6Y * alpha2b + frame7Y * alpha1b + frame8Y + frame0Y * alpha1b + frame1Y * alpha2b + frame2Y * alpha3b + frame3Y * alpha4b); 
                            float denominatorB = (1.0 + 2.0 * alpha1b + 2.0 * alpha2b + 2.0 * alpha3b + 2.0 * alpha4b);
                            
                            YOutB = numeratorB / denominatorB;
                            
                            //float rOut = YOut + 1.4020 * (frame8Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame8Cb - 128.0) - 0.714136 * (frame8Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame8Cb - 128.0); 
                            float rOutB = YOutB + 1.4020 * (frame8Cr - 0.5); 
                            float gOutB = YOutB - 0.3441 * (frame8Cb - 0.5) - 0.714136 * (frame8Cr - 0.5);
                            float bOutB = YOutB + 1.7720 * (frame8Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            
                            
                            float numeratorC = (frame4Y * alpha4c + frame5Y * alpha3c + frame6Y * alpha2c + frame7Y * alpha1c + frame8Y + frame0Y * alpha1c + frame1Y * alpha2c + frame2Y * alpha3c + frame3Y * alpha4c); 
                            float denominatorC = (1.0 + 2.0 * alpha1c + 2.0 * alpha2c + 2.0 * alpha3c + 2.0 * alpha4c);
                            
                            YOutC = numeratorC / denominatorC;
                            
                            //float rOut = YOut + 1.4020 * (frame8Cr - 128.0); 
                            //float gOut = YOut - 0.3441 * (frame8Cb - 128.0) - 0.714136 * (frame8Cr - 128.0);
                            //float bOut = YOut + 1.7720 * (frame8Cb - 128.0); 
                            float rOutC = YOutC + 1.4020 * (frame8Cr - 0.5); 
                            float gOutC = YOutC - 0.3441 * (frame8Cb - 0.5) - 0.714136 * (frame8Cr - 0.5);
                            float bOutC = YOutC + 1.7720 * (frame8Cb - 0.5); 
                            
                            //temporalOut = vec3(rOut, gOut, bOut) / 255.0;
                            temporalOut = vec3((rOutA + rOutB + rOutC) / 3.0, (gOutA + gOutB + gOutC) / 3.0, (bOutA + bOutB + bOutC) / 3.0);

                            */
                        
                            break;
                        }
                        
                    }
                    
                    fragColor = vec4(temporalOut.rgb, 1.0);
                    //fragColor = vec4(1.0, 1.0, 0.0, 1.0); 
                    //fragColor = vec4(color.rgb, 1.0);
                    //fragColor = vec4(outputN.rgb, 1.0); 
                    
                    //vec4 sum = vec4(0.0);
            
                    //for (int i = 0; i < 1; i++) {
                        //vec2 texCoords = gl_FragCoord.xy / uTextureSize;
                        //sum += texture(frame[i], scaledTextureCoord);
                        //sum += texture(textureSampler, texCoord);
                        
                    //    sum += texture(frame0, rotatedTexCoord); 
                    //    sum += texture(frame1, rotatedTexCoord); 
                    //    sum += texture(frame2, rotatedTexCoord); 
                    //    sum += texture(frame3, rotatedTexCoord); 
                    //    sum += texture(frame4, rotatedTexCoord); 
                    //    sum += texture(frame5, rotatedTexCoord); 
                    //    sum += texture(frame6, rotatedTexCoord); 
                    //    sum += texture(frame7, rotatedTexCoord); 
                    //    sum += texture(frame8, rotatedTexCoord); 
                        
                        //sum += texture(frame[i], texCoord); 
                        //sum += sum1; 
                    //}
                   
                    //vec4 imageOut = sum / 9.0;
                    //vec4 imageOut = sum1; 
                    
                    //fragColor = vec4(imageOut.rgb, 1.0);
                    //fragColor = vec4(1.0, 1.0, 0.0, 1.0); 
                }
            }
        """

        val vPositionAttribLocation = 10
        val aTexCoordAttribLocation = 11

        val vertexShader: Int = loadShader(GLES31.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES31.GL_FRAGMENT_SHADER, fragmentShaderCode)
        Log.d(
            "TEST_TAG_OUTPUT_VERTEX_FRAGMENT",
            "Triangle.init: vertexShader(0 = bad):$vertexShader, fragmentShader(0 = bad):$fragmentShader"
        )

        var programObject = GLES31.glCreateProgram()
        Log.d("TEST_TAG_OUTPUT", "Triangle.init: programObject(0 = bad, 1 = good): $programObject")

        GLES31.glAttachShader(programObject, vertexShader)
        GLES31.glAttachShader(programObject, fragmentShader)
        GLES31.glBindAttribLocation(programObject, vPositionAttribLocation, "vPosition")
        GLES31.glBindAttribLocation(programObject, aTexCoordAttribLocation, "aTexCoord")
        GLES31.glLinkProgram(programObject)

        var error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.init link: $error")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.init link: $error")
        }

        var linkResult = IntArray(1)
        linkResult[0] = -1

        GLES31.glGetProgramiv(programObject, GLES31.GL_LINK_STATUS, linkResult, 0)
        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.init link via Shaderiv error: $error")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.init link via Shaderiv error: $error")
        }
        if (linkResult[0] != GLES31.GL_TRUE) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.init link via linkResult")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.init link via  linkResult")
        }
        Log.d(
            "TEST_TAG_OUTPUT",
            "Triangle.init link ok (1,1) - linkResult[0]: ${linkResult[0]}, GL_TRUE is 1"
        )


        //val linkStatus = IntArray(1)
        //GLES31.glGetProgramiv(program, GLES31.GL_LINK_STATUS, linkStatus, 0)
        //if (linkStatus[0] == 0) {
        //    val log = GLES31.glGetProgramInfoLog(program)
        //    Log.e("TEST_TAG_OUTPUT", "ERROR: Program linking failed: $log")
        //    GLES31.glDeleteProgram(program)
        //    throw RuntimeException("Program linking failed")

        // Clean up shaders (no longer needed once linked)
        //GLES31.glDeleteShader(vertexShader)
        //GLES31.glDeleteShader(fragmentShader)

        mProgram = programObject
    }


    /*
    fun updateFrame(vertexCount: Int) {
        //val currentTexture = textureFrames[0] //frameCount % 9]
        //GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fboFrame)
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fboFrames[0])

        //GLES31.glFramebufferTexture2D(
        //    GLES31.GL_FRAMEBUFFER,
        //    GLES31.GL_COLOR_ATTACHMENT0,
        //    GLES31.GL_TEXTURE_2D,
        //    currentTexture,
        //    0
        //)

        // Check if framebuffer is complete
        if (GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER) != GLES31.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer not complete")
        }

        // Attach the input texture (e.g., GL_TEXTURE_EXTERNAL_OES) to the framebuffer
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)


        GLES20.glUniform1i(dSelectFragmentHandle, 0)
        // Render the input texture to the framebuffer
        GLES31.glViewport(0, 0, 3840, 2160)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT or GLES31.GL_DEPTH_BUFFER_BIT)
        //GLES31.glBindVertexArray(vao[0])
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, vertexCount)

        // Unbind framebuffer
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)

        //GLES31.glCopyTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGB, 0, 0, 3840, 2160, 0)
        frameCount++
    }
    */


    fun draw(mvpMatrix: FloatArray) {
        Log.d("TEST_TAG_OUTPUT", "fun draw")

        val vao = IntArray(1)
        GLES31.glGenVertexArrays(1, vao, 0)
        GLES31.glBindVertexArray(vao[0])

        var quadCoords = floatArrayOf( // in counterclockwise order:
            -1.0f, -1.0f, 0.0f, // Top left vertex
            1.0f, -1.0f, 0.0f, // Bottom left vertex
            -1.0f, 1.0f, 0.0f, // Bottom right vertex
            1.0f, 1.0f, 0.0f  // Top right vertex
        )

        val COORDS_PER_VERTEX = 3
        val vertexCount: Int = quadCoords.size / COORDS_PER_VERTEX
        val vertexStride: Int = COORDS_PER_VERTEX * 4

        var cropLeft : Float = 0.0f
        var cropRight : Float = 1.0f
        var cropTop : Float = 0.0f
        var cropBottom : Float = 1.0f

        if (SharedDataImage.selectResolution == 2) {
            cropLeft = 0.333f  //0.250f
            cropRight = 0.666f  //0.750f
            cropTop = 0.333f  //0.250f
            cropBottom = 0.666f  //0.750f
        } else if (SharedDataImage.selectResolution == 1) {
            cropLeft = 0.250f  //0.250f
            cropRight = 0.750f  //0.750f
            cropTop = 0.250f  //0.250f
            cropBottom = 0.750f  //0.750f
        }
        else if (SharedDataImage.selectResolution == 0) {
            cropLeft = 0.0f  //0.250f
            cropRight = 1.0f  //0.750f
            cropTop = 0.0f  //0.250f
            cropBottom = 1.0f  //0.750f
        }

        var textureCoords = floatArrayOf(
            cropRight, cropBottom, //cropLeft, cropBottom, // Bottom left vertex
            cropRight, cropTop, //cropRight, cropBottom, // Bottom right vertex
            cropLeft, cropBottom, //cropLeft, cropTop, // Top left vertex
            cropLeft, cropTop, //cropRight, cropTop // Top right vertex
        )

        var textureCoordsDefault = floatArrayOf(
            1.0f, 1.0f, // Bottom left vertex
            1.0f, 0.0f, // Bottom right vertex
            0.0f, 1.0f, // Top left vertex
            0.0f, 0.0f // Top right vertex
        )

        val vbo = IntArray(3)
        GLES31.glGenBuffers(3, vbo, 0)

        val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadCoords)
            .position(0)

        val textureBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(textureCoords)
            .position(0)

        val textureBufferDefault = ByteBuffer.allocateDirect(textureCoordsDefault.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(textureCoordsDefault)
            .position(0)


        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, vbo[0])
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, quadCoords.size * 4, vertexBuffer, GLES31.GL_STATIC_DRAW)
        dPositionHandle = GLES31.glGetAttribLocation(mProgram, "vPosition")
        GLES31.glVertexAttribPointer(dPositionHandle, COORDS_PER_VERTEX, GLES31.GL_FLOAT, false, vertexStride, 0)
        GLES31.glEnableVertexAttribArray(dPositionHandle)

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, vbo[1])
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, textureCoords.size * 4, textureBuffer, GLES31.GL_STATIC_DRAW)
        dTextureHandle = GLES31.glGetAttribLocation(mProgram, "aTexCoord")
        GLES31.glVertexAttribPointer(dTextureHandle, 2, GLES31.GL_FLOAT, false, 0, 0)
        GLES31.glEnableVertexAttribArray(dTextureHandle)

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, vbo[2])
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, textureCoordsDefault.size * 4, textureBufferDefault, GLES31.GL_STATIC_DRAW)
        dTextureDefaultHandle = GLES31.glGetAttribLocation(mProgram, "aTexCoordDefault")
        GLES31.glVertexAttribPointer(dTextureDefaultHandle, 2, GLES31.GL_FLOAT, false, 0, 0)
        GLES31.glEnableVertexAttribArray(dTextureDefaultHandle)
        //GLES31.glBindVertexArray(0)



        dTexture10UniformHandle = GLES31.glGetUniformLocation(mProgram, "textureSampler")
        var error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation textureSampler")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTexture10UniformHandle: $dTexture10UniformHandle"
            )
        }
        //GLES31.glUniform1i(dTexture10UniformHandle, 0)

        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d(
                "TEST_TAG_OUTPUT",
                "ERROR: Triangle.draw.glUniform1i: dTexture10UniformHandle: $error"
            )
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glUniform1i: dTexture10UniformHandle: $dTexture10UniformHandle"
            )
        }


        dTexture0UniformHandle = GLES31.glGetUniformLocation(mProgram, "input0")
        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation input0")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTexture0UniformHandle: $dTexture0UniformHandle"
            )
        }

        dTexture1UniformHandle = GLES31.glGetUniformLocation(mProgram, "input1")
        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation input1")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTexture1UniformHandle: $dTexture1UniformHandle"
            )
        }

        dTexture2UniformHandle = GLES31.glGetUniformLocation(mProgram, "input2")
        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation input2")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTexture2UniformHandle: $dTexture2UniformHandle"
            )
        }

        dTexture3UniformHandle = GLES31.glGetUniformLocation(mProgram, "input3")
        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation input3")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTexture3UniformHandle: $dTexture3UniformHandle"
            )
        }

        dTexture4UniformHandle = GLES31.glGetUniformLocation(mProgram, "input4")
        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation input4")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTexture4UniformHandle: $dTexture4UniformHandle"
            )
        }

        dTexture5UniformHandle = GLES31.glGetUniformLocation(mProgram, "input5")
        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation input5")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTexture1UniformHandle: $dTexture5UniformHandle"
            )
        }

        dTexture6UniformHandle = GLES31.glGetUniformLocation(mProgram, "input6")
        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation input6")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTexture6UniformHandle: $dTexture6UniformHandle"
            )
        }

        dTexture7UniformHandle = GLES31.glGetUniformLocation(mProgram, "input7")
        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation input7")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTexture7UniformHandle: $dTexture7UniformHandle"
            )
        }

        dTexture8UniformHandle = GLES31.glGetUniformLocation(mProgram, "input8")
        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation input8")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTexture8UniformHandle: $dTexture8UniformHandle"
            )
        }


        dSelectFragmentHandle = GLES31.glGetUniformLocation(mProgram, "selectFragment")
        dIncomingFrameHandle = GLES31.glGetUniformLocation(mProgram, "incomingFrame")

        dScaleHandle = GLES20.glGetUniformLocation(mProgram, "scale")
        GLES20.glUniform1f(dScaleHandle, SharedDataScale.scaleAll)
        Log.d("Angle scale draw", "scale: $SharedDataScale.scaleAll")
        error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation scale")
        }
        else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.getUniformLocation dScaleUniformHandle: $dScaleHandle"
            )
        }

        //renderFrameToFramebuffer(vertexCount, currentTexture)
        //updateFrame(vertexCount)

        //fun draw average frame
        //Add program to OpenGL ES environment
        GLES31.glUseProgram(mProgram)


        val GL_TIME_ELAPSED_EXT = 0x88BF
        val GL_QUERY_RESULT_AVAILABLE = 0x8867
        val GL_QUERY_RESULT = 0x8866

        val queries = IntArray(1)
        GLES31.glGenQueries(1, queries, 0)
        //GLES31.glBeginQuery(GL_TIME_ELAPSED_EXT, queries[0])

        val startTime = System.nanoTime()
        GLES31.glBeginQuery(GL_TIME_ELAPSED_EXT, queries[0])

        //GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + (frameCount % numTextureFrames) )
        //GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[frameCount % numTextureFrames])
        //GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GL_RGB, SharedDataImage.widthInput, SharedDataImage.heightInput, 0, GL_RGB, GL_UNSIGNED_BYTE, null as Buffer?);
        //GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GL_RGB, SharedDataImage.widthInput, SharedDataImage.heightInput, 0, GL_RGB, GL_UNSIGNED_BYTE, null as Buffer?);
        //GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        //GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        //GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
        //GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)

        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fboFrame);
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, textureFrames[frameCount % numTextureFrames], 0);

        //GLES31.glEnable(GLES31.GL_SCISSOR_TEST)
        //GLES31.glScissor(640, 360, 1280, 720) // Define center crop region
        //GLES31.glViewport(0, 0, 1280, 720)    // Set the viewport to match
        //GLES31.glDisable(GLES31.GL_SCISSOR_TEST)

        // Attach the input texture (e.g., GL_TEXTURE_EXTERNAL_OES) to the framebuffer
        GLES31.glActiveTexture(GLES31.GL_TEXTURE10)
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES31.glUniform1i(dTexture10UniformHandle, 10)

        GLES31.glUniform1i(dSelectFragmentHandle, 0)

/*
        val cropWidth = (3840 - SharedDataImage.widthInput) / 2;
        val cropHeight = (2160 - SharedDataImage.heightInput) / 2;

        GLES31.glEnable(GLES31.GL_SCISSOR_TEST)
        GLES31.glScissor(cropWidth, cropHeight, SharedDataImage.widthInput, SharedDataImage.heightInput)
        // Render the input texture to the framebuffer
        //GLES31.glViewport(0, 0, 3840, 2160)
        //GLES31.glViewport(0, 0, SharedDataImage.widthInput, SharedDataImage.heightInput)
        GLES31.glDisable(GLES31.GL_SCISSOR_TEST)
*/

        //GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT or GLES31.GL_DEPTH_BUFFER_BIT)
        //GLES31.glBindVertexArray(vao[0])
        //GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT or GLES31.GL_DEPTH_BUFFER_BIT)
        GLES31.glViewport(0, 0, SharedDataImage.widthInput, SharedDataImage.heightInput)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, vertexCount)
        //GLES31.glBindVertexArray(0)
        //GLES31.glFlush()


        //for (i in 0 until numTextureFrames) {
        //    GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        //    GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[i])
        //    GLES31.glUniform1i(GLES31.glGetUniformLocation(mProgram, "frame[$i]"), i)
        //}


        //for (i in 0 until numTextureFrames) {
        //    val location = GLES31.glGetUniformLocation(mProgram, "frame[$i]")
        //    if (location != -1) {
        //        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + i)
        //        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[i])
        //        GLES31.glUniform1i(location, i)
        //        Log.d("TEST_TAG_OUTPUT_SHADER", "SUCCESS")
        //    } else {
        //        Log.e("Shader", "Uniform frame[$i] not found!")
        //   }
        //}

        val maxTextureUnits = IntArray(1)
        GLES31.glGetIntegerv(GLES31.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, maxTextureUnits, 0)
        Log.d("GLInfo", "Max texture units: ${maxTextureUnits[0]}")


        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0) //fboFrames[0])
        GLES31.glUniform1i(dSelectFragmentHandle, 1)
        GLES31.glUniform1i(dIncomingFrameHandle, frameCount % numTextureFrames)


        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[0])
        GLES31.glUniform1i(dTexture0UniformHandle, 0)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE1);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[1])
        GLES31.glUniform1i(dTexture1UniformHandle, 1)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE2);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[2])
        GLES31.glUniform1i(dTexture2UniformHandle, 2)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE3);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[3])
        GLES31.glUniform1i(dTexture3UniformHandle, 3)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE4);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[4])
        GLES31.glUniform1i(dTexture4UniformHandle, 4)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE5);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[5])
        GLES31.glUniform1i(dTexture5UniformHandle, 5)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE6);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[6])
        GLES31.glUniform1i(dTexture6UniformHandle, 6)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE7);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[7])
        GLES31.glUniform1i(dTexture7UniformHandle, 7)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE8);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[8])
        GLES31.glUniform1i(dTexture8UniformHandle, 8)


        //GLES31.glUniform1i(GLES31.glGetUniformLocation(mProgram, "frame[$0]"), 0)

        /*
        for (i in 0 until numTextureFrames) {
            GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + i)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[i])
            GLES31.glUniform1i(GLES31.glGetUniformLocation(mProgram, "frame[$i]"), i)

            val location = GLES31.glGetUniformLocation(mProgram, "frame[$i]")
            if (location != -1) { // Check if the uniform location is valid
                GLES31.glUniform1i(location, i)
            } else {
                Log.e("GL_ERROR", "Uniform location for frame[$i] not found! === $location")
            }
        }
        */

        GLES31.glViewport(0, 0, 1080, 2201);
        //GLES31.glBindVertexArray(vao[0])
        //GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT or GLES31.GL_DEPTH_BUFFER_BIT)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, vertexCount)
        //GLES31.glBindVertexArray(0)
        //GLES31.glDeleteProgram(mProgram)
        frameCount++


        GLES31.glFinish()
        GLES31.glEndQuery(GL_TIME_ELAPSED_EXT)

        val endTime = System.nanoTime()
        val eTimeMS = (endTime - startTime) / 1_000_000.0
        //Log.d("EXIST-CPU-END", "Approx. Time taken: $eTimeMS ms")

        //GLES31.glEndQuery(GL_TIME_ELAPSED_EXT)

        val available = IntArray(1)
        do {
            GLES31.glGetQueryObjectuiv(queries[0], GL_QUERY_RESULT_AVAILABLE, available, 0)
        } while (available[0] == GLES31.GL_FALSE)

        val elapsedTime = IntArray(1)
        GLES31.glGetQueryObjectuiv(queries[0], GL_QUERY_RESULT, elapsedTime, 0)

        val elapsedTimeMs = elapsedTime[0] / 1_000_000.0
        Log.d("EXIST-GPU-END","Time taken: $elapsedTimeMs ms")

        GLES31.glDeleteQueries(1, queries, 0);

    }  //draw
}


class MainActivity : ComponentActivity() {
    private lateinit var surfaceTextureG : SurfaceTexture
    private lateinit var surfaceG: Surface

    private lateinit var glView: MyGLSurfaceView
    private lateinit var cameraExecutor: ExecutorService

    lateinit var renderer: MyGLRenderer

    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TEST_TAG_OUTPUT","MainActivity.onCreate0")

        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        glView = MyGLSurfaceView(this)
        setContentView(glView)

        glView.setSurfaceTextureReadyCallback { surfaceTexture ->
            this.surfaceTextureG = surfaceTexture
            startCamera()
        }

        Log.d("TEST_TAG_OUTPUT", "MainActivity.onCreate1")
    }


    //ok
    inner class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
        private val renderer: MyGLRenderer

        init {
            Log.d("TEST_TAG_OUTPUT", "MainActivity.MyGLSurfaceView.init0")

            setEGLContextClientVersion(3)
            renderer = MyGLRenderer(this)
            setRenderer(renderer)
            renderMode = RENDERMODE_WHEN_DIRTY //CONTINUOUSLY

            Log.d("TEST_TAG_OUTPUT", "MainActivity.MyGLSurfaceView.init1")
        }

        fun setSurfaceTextureReadyCallback(callback: (SurfaceTexture) -> Unit) {
            renderer.setSurfaceTextureReadyCallback(callback)
        }

        private var TOUCH_SCALE_FACTOR: Float = 180.0f / 320f
        private var previousX: Float = 0f
        private var previousY: Float = 0f

        override fun onTouchEvent(e: MotionEvent): Boolean {
            // MotionEvent reports input details from the touch screen
            // and other input controls. In this case, you are only
            // interested in events where the touch position changed.

            val x: Float = e.x
            val y: Float = e.y

            when (e.action) {
                MotionEvent.ACTION_MOVE -> {

                    var dx: Float = x - previousX
                    var dy: Float = y - previousY

                    // reverse direction of rotation above the mid-line
                    if (y > height / 2) {
                        dx *= -1
                    }

                    // reverse direction of rotation to left of the mid-line
                    if (x < width / 2) {
                        dy *= -1
                    }

                    //renderer.angle += (dx + dy) * TOUCH_SCALE_FACTOR
                    renderer.angle += (dx + dy) * TOUCH_SCALE_FACTOR

                    if (renderer.angle <= 0.0f) {
                        renderer.gamma = 0.1f
                        renderer.scale = 0.01f
                    }
                    else if (renderer.angle > 90.0f) {
                        renderer.gamma = 3.1f
                        renderer.scale = 25.0f //25.0f
                    }
                    else {
                        renderer.gamma = 0.1f + ((3.0f) * renderer.angle / 90.0f)
                        renderer.scale = 1.0f  + ((24.0f) * renderer.angle / 90.0f)
                    }


                    Log.d("Angle", "renderer.angle: ${renderer.angle}")
                    Log.d("Angle gamma", "renderer.gamma: ${renderer.gamma}")
                    Log.d("Angle scale", "renderer.scale: ${renderer.scale}")

                    requestRender()
                }
            }

            previousX = x
            previousY = y
            return true
        }
    }

    @OptIn(ExperimentalGetImage::class) private fun startCamera() {
        Log.d("TEST_TAG_OUTPUT", "startCamera0")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Build the Preview use case
            //val preview = Preview.Builder().build()
            val previewWidth = 3840; //3840, 480
            val previewHeight = 2160; //2160, 270

            //val preview = Preview.Builder().setTargetResolution(Size(previewWidth, previewHeight)).build()
            //val preview = Preview.Builder().setTargetRotation(Surface.ROTATION_90)
            //    .build()
            val preview = Preview.Builder()//.setTargetRotation(Surface.ROTATION_90)
                .setTargetResolution(Size(previewWidth, previewHeight))
                .build()

            surfaceTextureG.setDefaultBufferSize(previewWidth, previewHeight)
            surfaceG = Surface(surfaceTextureG)

            //Preview
            preview.setSurfaceProvider { request ->
                //request.provideSurface(surfaceG, cameraExecutor) {
                // Surface is ready, handle any post-processing here
                //    Log.d("CameraX", "Surface is ready")
                //}

                //surfaceTextureG.setDefaultBufferSize(previewWidth, previewHeight)
                //surfaceG = Surface(surfaceTextureG)

                // You can use a SurfaceTexture to obtain the dimensions
                //val surfaceTexture = surfaceTextureG
                //if (surfaceTexture != null) {
                //val width = surfaceTexture.width
                //val height = surfaceTexture.height
                //Log.d("TEST_DIM", "Preview dimensions: width=$width, height=$height")
                //}

                // Provide the surface to the SurfaceRequest
                request.provideSurface(surfaceG, cameraExecutor) {
                    Log.d("CameraX", "Surface is ready")
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, { imageProxy ->
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                        imageProxy.image?.let { image ->
                            //processFaceImage(image, rotationDegrees)
                            //processSelfieImage(image, rotationDegrees)
                        }

                        imageProxy.close()
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                // Handle exception
            }
        }, ContextCompat.getMainExecutor(this))

        Log.d("TEST_TAG_OUTPUT", "startCamera1")
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

