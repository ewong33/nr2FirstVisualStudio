package com.example.mylogivnlmnrgl

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
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES31
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

//ok
object SharedDataGamma{
    var gammaAll: Float = 1.0f
}

object SharedDataScale{
    var scaleAll: Float = 0.01f
}

object SharedDataImage{
    var selectResolution = 2 // 0 = 3840, 2160, 1 = 1920, 1080, 2 = 1080, 720

    var widthInput: Int = 1280 //1920 //3840
    var heightInput: Int = 720 //1080 //2160
}

//ok
class MyGLRenderer(private val glSurfaceView:GLSurfaceView) : GLSurfaceView.Renderer {
    private lateinit var mTriangle: Triangle

    var texture = IntArray(1)

    var numTextureFrames: Int = 1 //5
    var textureFrames = IntArray(numTextureFrames)
    var fboFrame = IntArray(1)

    var outputTexture = IntArray(1)

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

        //ok
        GLES31.glGenTextures(1, texture, 0)
        GLES31.glActiveTexture(GLES31.GL_TEXTURE10)
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_REPEAT) //GL_REPEAT)//GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_REPEAT)

        val width = SharedDataImage.widthInput
        val height = SharedDataImage.heightInput
        val pixelCount = width * height
        val channels = 4

        val BufferInput = ByteBuffer.allocateDirect(pixelCount * channels)
            .order(ByteOrder.nativeOrder())

        for (i in 0 until pixelCount) {
            BufferInput.put((0.0f * 255).toInt().toByte())   //red
            BufferInput.put((0.0f * 255).toInt().toByte())   //green
            BufferInput.put((1.0f * 255).toInt().toByte())   //blue
            BufferInput.put((1.0f * 255).toInt().toByte())   //alpha
        }

        BufferInput.position(0)

        //ok
        GLES31.glGenTextures(numTextureFrames, textureFrames, 0)

        for (i in 0..numTextureFrames - 1) {
            GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + i)
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[i])

            GLES31.glTexStorage2D(GLES31.GL_TEXTURE_2D, 1, GLES31.GL_RGBA8, SharedDataImage.widthInput, SharedDataImage.heightInput)
            //GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA32F, SharedDataImage.widthInput, SharedDataImage.heightInput, 0, GLES31.GL_RGBA, GLES31.GL_FLOAT, floatBuffer)

            GLES31.glTexSubImage2D(GLES31.GL_TEXTURE_2D, 0, 0, 0, SharedDataImage.widthInput, SharedDataImage.heightInput, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE, BufferInput)
            //GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA, SharedDataImage.widthInput, SharedDataImage.heightInput, 0, GLES31.GL_RGBA, GL_UNSIGNED_BYTE, null as Buffer?);

            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)

            GLES31.glGenFramebuffers(1, fboFrame, 0);
        }

        //ok
        val BufferOutput = ByteBuffer.allocateDirect(pixelCount * channels)
            .order(ByteOrder.nativeOrder())

        for (i in 0 until pixelCount) {
            BufferOutput.put((1.0f * 255).toInt().toByte())   //red
            BufferOutput.put((0.0f * 255).toInt().toByte())   //green
            BufferOutput.put((0.0f * 255).toInt().toByte())   //blue
            BufferOutput.put((1.0f * 255).toInt().toByte())   //alpha
        }

        BufferOutput.position(0)

        GLES31.glGenTextures(1, outputTexture, 0)
        GLES31.glActiveTexture(GLES31.GL_TEXTURE5)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, outputTexture[0])

        GLES31.glTexStorage2D(GLES31.GL_TEXTURE_2D, 1, GLES31.GL_RGBA8, SharedDataImage.widthInput, SharedDataImage.heightInput)
        //GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA32F, SharedDataImage.widthInput, SharedDataImage.heightInput, 0, GLES31.GL_RGBA, GLES31.GL_FLOAT, floatBuffer)

        GLES31.glTexSubImage2D(GLES31.GL_TEXTURE_2D, 0, 0, 0, width, height, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE, BufferOutput)

        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
        GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)

        mTriangle = Triangle(texture[0], fboFrame[0], textureFrames, outputTexture[0])

        surfaceTexture = SurfaceTexture(texture[0])
        surfaceTexture.setOnFrameAvailableListener {
            glSurfaceView.requestRender()
        }
        surfaceTextureReadyCallback?.invoke(surfaceTexture)

        Log.d("TEST_TAG_OUTPUT","MyGLRenderer.onSurfaceCreated Exit")
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

        val ratio: Float = width.toFloat() / height.toFloat()

        //this projection matrix is applied to object coordinates
        //in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

        Log.d("TEST_TAG_OUTPUT", "MyGLRenderer.onSurfaceChanged Exit")
    }
}

//ok
class Triangle(private var textureId: Int, private var fboFrame: Int, private var textureFrames: IntArray, private var outputTextureId: Int) {
    //ok
    var mShaderProgram: Int = 0
    var mComputeProgram: Int = 0

    var dPositionHandle: Int = 0
    var dTextureHandle: Int = 0
    var dTextureDefaultHandle: Int = 0

    var dSelectFragmentHandle: Int = 0
    var dTexture10UniformHandle: Int = 0

    var dTexture0UniformHandle: Int = 0

    var dScaleHandle: Int = 0

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
            #version 320 es
            precision mediump float; 
            
            layout(location = 0) in vec4 vPosition;
            layout(location = 1) in vec2 aTexCoord;
            layout(location = 2) in vec2 aTexCoordDefault; 
            
            //in vec4 vPosition;
            //in vec2 aTexCoord;
            //in vec2 aTexCoordDefault; 
            out vec2 texCoord;
            out vec2 texCoordDefault; 
            
            void main() {
                texCoord = aTexCoord; 
                texCoordDefault = aTexCoordDefault; 
                gl_Position = vPosition;      
            }
        """

        val fragmentShaderCode = """
            #version 320 es
            #extension GL_OES_EGL_image_external_essl3 : require
            
            precision highp float;
            uniform int selectFragment;
             
            uniform samplerExternalOES textureSampler;
            uniform sampler2D input0; 
            uniform sampler2D displayTexture; 
            
            uniform float k; 
            uniform float scale;
            
            in vec2 texCoord;
            in vec2 texCoordDefault;
            
            out vec4 fragColor;
            
            void main() {
                if (selectFragment == 0) {
                    vec4 color = texture(textureSampler, texCoord);
                    fragColor = vec4(color.rgb, 1.0);           
                } else if (selectFragment == 1) {
                    vec2 rotatedTexCoord = vec2(1.0 - texCoordDefault.y, 1.0 - texCoordDefault.x);
                    //vec4 color = texture(input0, rotatedTexCoord);
                    vec4 color = texture(displayTexture, rotatedTexCoord); 
 
                    fragColor = vec4(color.rgb, 1.0); 
                }
            }
        """

        /*
        val computeShaderCode = """
            #version 320 es
            precision mediump float;
            layout(local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

            layout(binding = 4, rgba8) readonly uniform mediump image2D inputImage;
            layout(binding = 5, rgba8) writeonly uniform mediump image2D outputImage; 

            void main() {
                ivec2 texCoord = ivec2(gl_GlobalInvocationID.xy);
                ivec2 texSize = imageSize(outputImage); 
                
                if (texCoord.x >= texSize.x || texCoord.y >= texSize.y) {
                    imageStore(outputImage, texCoord, vec4(0.8, 0.5, 0.7, 1.0));
                    barrier();
                    return;
                }

                vec4 color = imageLoad(inputImage, texCoord); 
                //color = 1.0 - color;
           
                //imageStore(outputImage, texCoord, vec4(0.3, 0.5, 0.7, 1.0));
                imageStore(outputImage, texCoord, vec4(color.rgb, 1.0));
            }
        """
        */



        val computeShaderCode = """
            #version 320 es
            precision mediump float;
            layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;
            //layout(local_size_x = 32, local_size_y = 32, local_size_z = 1) in;

            layout(binding = 4, rgba8) readonly uniform mediump image2D inputImage;  
            layout(binding = 5, rgba8) writeonly uniform mediump image2D outputImage; 

            //higher h means less denoising, lower h means more denoising.
            const float h = 1.1; 
            const int searchRadius = 3; 
            const int patchRadius = 1; 
            const int workgroupDim = 8; 
            
            const int searchSize = searchRadius + patchRadius;
            const int sharedSize = workgroupDim + (2 * searchSize);
            
            shared vec4 sharedMemory[sharedSize * sharedSize];
    
            //16 + 2*(5+2) = 30;
            //MAX_COMPUTE_SHARED_MEMORY_SIZE 
            //needs to be 1d array also
            //shared mediump vec4 sharedMemory[sharedSize][sharedSize];
            
            #define SHARED_INDEX(x, y) ((y) * sharedSize + (x))


            ////////////////////////////////////////////////////////////////////////////////////////
            /*
            void loadPatch(inout vec4 patch[25], int x, int y) { 
                //const int patchRadius = 2; 
                
                for (int dy = -patchRadius; dy <= patchRadius; dy++) {
                    for (int dx = -patchRadius; dx <= patchRadius; dx++) {
                        int idx = (dy + patchRadius) * 5 + (dx + patchRadius);
                        
                        int sx = clamp(x + dx, 0, sharedSize - 1);
                        int sy = clamp(y + dy, 0, sharedSize - 1);

                        patch[idx] = sharedMemory[SHARED_INDEX(sx, sy)];
                    }
                }
            }
            */

            /*
            float computePatchDistance(vec4 patchA[9], vec4 patchB[9]) {
                float patchDistance = 0.0;
                for (int i = 0; i < 9; i++) {
                    vec4 patchDifference = patchA[i] - patchB[i];
                    patchDistance += dot(patchDifference, patchDifference);
                }
                return patchDistance;
            }
            */
            ////////////////////////////////////////////////////////////////////////////////////////
            
            
                  
            mediump float computePatchDistance(vec4 patchA[9], vec4 patchB[9]) {
                //mediump vec4 sum = vec4(0.0); 
                mediump float sum = 0.0; 
                
                for (int i = 0; i < 9; i++) {
                    mediump vec4 diff = patchA[i] - patchB[i]; 
                    
                    //sum += diff * diff;
                    sum += dot(diff, diff);   
                }
                
                //return sum.r + sum.g + sum.b + sum.a; 
                return sum; 
                
                /*
                vec4 diff0 = patchA[0] - patchB[0]; vec4 diff1 = patchA[1] - patchB[1];
                vec4 diff2 = patchA[2] - patchB[2]; vec4 diff3 = patchA[3] - patchB[3];
                vec4 diff4 = patchA[4] - patchB[4]; vec4 diff5 = patchA[5] - patchB[5];
                vec4 diff6 = patchA[6] - patchB[6]; vec4 diff7 = patchA[7] - patchB[7];
                vec4 diff8 = patchA[8] - patchB[8];

                vec4 sum = diff0 * diff0 + diff1 * diff1 + diff2 * diff2 +
                    diff3 * diff3 + diff4 * diff4 + diff5 * diff5 +
                    diff6 * diff6 + diff7 * diff7 + diff8 * diff8;

                return sum.r + sum.g + sum.b + sum.a;
                */ 
            }


            float computeWeight(float distance) {
                return exp(-distance / (h * h));
            }
            
         
            //(0,0) to (15,15) + (searchSize = searchRadius + patchRadius)
            vec4 denoisePixel(int x, int y) {
                int patchSize = 3; 

                vec4 centerPatch[9];              
                vec4 neighborPatch[9]; 
               
                //loadPatch(centerPatch, x, y);
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
                for (int dy = -patchRadius; dy <= patchRadius; dy++) {
                    for (int dx = -patchRadius; dx <= patchRadius; dx++) {
                        int idx = (dy + patchRadius) * patchSize + (dx + patchRadius);
                        
                        int sx = clamp(x + dx, 0, sharedSize - 1);
                        int sy = clamp(y + dy, 0, sharedSize - 1);
                        
                        int sharedIndex = SHARED_INDEX(sx, sy) + (sx % 1); 
                        centerPatch[idx] = sharedMemory[sharedIndex]; //SHARED_INDEX(sx, sy)];

                        //centerPatch[idx] = sharedMemory[SHARED_INDEX(sx, sy)];
                    }
                }
                
                
                ////////////////////////////////////////////////////////////////////////////////////
                /*               
                vec4 row1 = sharedMemory[SHARED_INDEX(x-1, y-1)];
                vec4 row2 = sharedMemory[SHARED_INDEX(x, y-1)];
                vec4 row3 = sharedMemory[SHARED_INDEX(x+1, y-1)];

                vec4 row4 = sharedMemory[SHARED_INDEX(x-1, y)];
                vec4 row5 = sharedMemory[SHARED_INDEX(x, y)];
                vec4 row6 = sharedMemory[SHARED_INDEX(x+1, y)];

                vec4 row7 = sharedMemory[SHARED_INDEX(x-1, y+1)];
                vec4 row8 = sharedMemory[SHARED_INDEX(x, y+1)];
                vec4 row9 = sharedMemory[SHARED_INDEX(x+1, y+1)];

                centerPatch[0] = row1;
                centerPatch[1] = row2;
                centerPatch[2] = row3;
                centerPatch[3] = row4;
                centerPatch[4] = row5;
                centerPatch[5] = row6;
                centerPatch[6] = row7;
                centerPatch[7] = row8;
                centerPatch[8] = row9;
                */

                /*
                vec4 row1[3], row2[3], row3[3];

                //full rows into registers first
                for (int i = -1; i <= 1; i++) {
                    row1[i + 1] = sharedMemory[SHARED_INDEX(x + i, y - 1)];
                    row2[i + 1] = sharedMemory[SHARED_INDEX(x + i, y)];
                    row3[i + 1] = sharedMemory[SHARED_INDEX(x + i, y + 1)];
                }

                //store centerPatch in row-major order
                for (int i = 0; i < 3; i++) {
                    centerPatch[i]     = row1[i];
                    centerPatch[i + 3] = row2[i];
                    centerPatch[i + 6] = row3[i];
                }
                */
                ////////////////////////////////////////////////////////////////////////////////////
                
                
                vec4 sumColor = vec4(0.0);
                float sumWeights = 0.0;
                
                for (int sy = -searchRadius; sy <= searchRadius; sy++) {
                    for (int sx = -searchRadius; sx <= searchRadius; sx++) {
                        //loadPatch(neighborPatch, x + sx, y + sy); 
                       
                        int intX = x + sx;
                        int intY = y + sy;  
                      
                        for (int dy = -patchRadius; dy <= patchRadius; dy++) {
                            for (int dx = -patchRadius; dx <= patchRadius; dx++) {
                                int idx = (dy + patchRadius) * patchSize + (dx + patchRadius);
                        
                                int sx2 = clamp(intX + dx, 0, sharedSize - 1);
                                int sy2 = clamp(intY + dy, 0, sharedSize - 1);


                                int sharedIndex = SHARED_INDEX(sx2, sy2) + (sx2 % 1); 
                                neighborPatch[idx] = sharedMemory[sharedIndex]; 


                                //neighborPatch[idx] = sharedMemory[SHARED_INDEX(sx2, sy2)];
                                //patch[idx] = sharedMemory[SHARED_INDEX(x + dx, y + dy)];
                            }
                        }
                        
           
                        float patchDistance = computePatchDistance(centerPatch, neighborPatch) + 0.01;
                        //float weight = 1.0; 
                        float weight = computeWeight(patchDistance);
            
                        sumWeights += weight;
                        sumColor += weight * neighborPatch[4]; //12]; 
                        //sumColor += centerPatch[4]; //12]; 
                    }
                }

                //return vec4(0.5, 0.5, 0.0, 1.0); //sumColor / sumWeights;
                return sumColor / sumWeights; 
            }  //denoise end
            
            
            
            ////////////////////////////////////////////////////////////////////////////////////////
            /*
            float gaussianWeight(mediump float distance, mediump float h) {
                    return exp(-distance * distance / (2.0 * h * h));
            }   

            float patchSimilarity(ivec2 center, ivec2 neighbor) {
                mediump float dist = 0.0;
                for (int dy = -patchRadius; dy <= patchRadius; dy++) {
                    for (int dx = -patchRadius; dx <= patchRadius; dx++) {
                        //reduces redundant color channel memory access, by not using distance function.  eliminates need for sqrt.
                        //need to adjust h function value in nlm to compensate, since we are now returning the squared value.
                        //vec4 patchDifference = sharedMemory[center.y + dy][center.x + dx] - sharedMemory[neighbor.y + dy][neighbor.x + dx];
                        //vec4 patchDistance += dot(patchDifference.rgb, patchDifference.rgb);
                        
                        int centerIdx = (center.y + dy) * sharedSize + (center.x + dx);
                        int neighborIdx = (neighbor.y + dy) * sharedSize + (neighbor.x + dx);
            
                        vec4 patchDistance += distance(sharedMemory[centerIdx], sharedMemory[neighborIdx]); 
                        //vec4 patchDifference = sharedMemory[centerIdx] - sharedMemory[neighborIdx];
                        //vec4 patchDistance += dot(patchDifference.rgb, patchDifference.rgb);
                    }
                }
                return patchDistance; 
            }
            
            //(x,y) means column, row
                //ivec2 globalId = ivec2(gl_GlobalInvocationID.xy);
                //ivec2 localId = ivec2(gl_LocalInvocationID.xy);

                //ivec2 groupBase = ivec2(gl_WorkGroupID.xy) * 16 - ivec2(searchRadius + patchRadius);
                //Each thread loads a single pixel into sharedMemory; loadPosition is referenced over the range of globalId.
                //ivec2 loadPosition = clamp(groupBase + localId, ivec2(0), min(imageSize, groupBase + ivec2(sharedSize)) - ivec2(1));
                //sharedMemory is indexed using row major ordering = y means row,
                //Shared Memory (row-major)
                //Row 0 → [ 00  01  02  03  ... 15 ]
                //Row 1 → [ 16  17  18  19  ... 31 ]
                //Row 2 → [ 32  33  34  35  ... 47 ]
                //...
                //Row 15 → [ 240  241  242  ... 255 ]

                //Memory Address = Base Address + (i * row_size) + j
                //sharedMemory[rows][cols]
                //sharedMemory[localId.y][localId.x]; // Row-major indexing
                //row first access gives better thread efficiency
                //swap, and you get poor cache access
                //Thread IDs → [  (0,0) (1,0) (2,0) (3,0) ... (15,0)  ]
                //Memory Addr → [   0    16    32    48  ...  240  ]
            */        
            ////////////////////////////////////////////////////////////////////////////////////////
            
            
            void main() {
                ivec2 globalId = ivec2(gl_GlobalInvocationID.xy);                     
                ivec2 localId = ivec2(gl_LocalInvocationID.xy);
                ivec2 imageSize = imageSize(inputImage); 
                
                ivec2 groupBase = ivec2(gl_WorkGroupID.xy) * workgroupDim - ivec2(searchSize); 
                ivec2 loadPosition = clamp(groupBase + localId, ivec2(0), min(imageSize, groupBase + ivec2(sharedSize)) - ivec2(1));
  
               
                for (int offsetY = 0; offsetY < sharedSize; offsetY += workgroupDim) {
                    for (int offsetX = 0; offsetX < sharedSize; offsetX += workgroupDim) {
                        ivec2 loadPos = clamp(groupBase + localId + ivec2(offsetX, offsetY), ivec2(0), imageSize - ivec2(1));
                        sharedMemory[SHARED_INDEX(localId.x + offsetX, localId.y + offsetY)] = imageLoad(inputImage, loadPos);
                    }
                }
                barrier(); 
                   
            
                ////////////////////////////////////////////////////////////////////////////////////
                /*
                if (localId.x < sharedSize && localId.y < sharedSize) { // sharedSize && localId.y < sharedSize) {
                    sharedMemory[SHARED_INDEX(localId.x, localId.y)] = imageLoad(inputImage, loadPosition);
                }
              
                if (localId.y < 2 * sharedSize) {
                    ivec2 extraY = clamp(groupBase + localId + ivec2(workgroupDim, 0), ivec2(0), imageSize - ivec2(1));
                    sharedMemory[SHARED_INDEX(localId.x, localId.y + workgroupDim)] = imageLoad(inputImage, extraY);
                }

                if (localId.x < 2 * sharedSize) {
                    ivec2 extraX = clamp(groupBase + localId + ivec2(0, workgroupDim), ivec2(0), imageSize - ivec2(1));
                    sharedMemory[SHARED_INDEX(localId.x + workgroupDim, localId.y)] = imageLoad(inputImage, extraX);
                }

                if (localId.x < 2 * sharedSize  && localId.y < 2 * sharedSize) {
                    ivec2 extraXY = clamp(groupBase + localId + ivec2(workgroupDim), ivec2(0), imageSize - ivec2(1));
                    sharedMemory[SHARED_INDEX(localId.x + workgroupDim, localId.y + workgroupDim)] = imageLoad(inputImage, extraXY);
                }
                barrier();
                */
                ////////////////////////////////////////////////////////////////////////////////////
                

                
                if (localId.x < workgroupDim && localId.y < workgroupDim) {
                    vec4 denoisedPixel = denoisePixel(localId.x + searchSize, localId.y + searchSize);
                    //vec4 denoisedPixel = vec4(0.3, 0.5, 0.7, 1.0); 
                    //barrier(); 
                    imageStore(outputImage, globalId, denoisedPixel);
                }


            /*
            //// Define middle 16x16 region in a 30x30 workgroup
            //bool isMiddle16x16 = (localId.x >= 7 && localId.x < 23) && (localId.y >= 7 && localId.y < 23);

            // Load the entire image into shared memory (30x30 region)
            //sharedMemory[localId.y][localId.x] = texelFetch(inputImage, globalId, 0);
            //barrier();  // Wait for all threads to finish loading the image

            // Perform non-local means only on the middle 16x16 area
            //if (isMiddle16x16) {
            // Apply non-local means filter here for the middle region
            // Example: sharedMemory[localId.y][localId.x] = nonLocalMeans(sharedMemory, localId);
            //}

            //barrier();


            //Calculate weights and sum of weighted pixels
            mediump vec3 nlmFilteredColor = vec3(0.0);
            mediump float weightSum = 1.0e-6;
                
            int minX = max(-searchRadius, -globalId.x);
            int maxX = min(searchRadius, imageSize.x - globalId.x - 1);
            int minY = max(-searchRadius, -globalId.y);
            int maxY = min(searchRadius, imageSize.y - globalId.y - 1);


            for (int dy = minY; dy <= maxY; dy++) {
                for (int dx = minX; dx <= maxX; dx++) {
                    ivec2 neighborPosition = globalId + ivec2(dx, dy);
                        
               //for (int dy = -searchRadius; dy <= searchRadius; dy++) {
               //  for (int dx = -searchRadius; dx <= searchRadius; dx++) {
               //      ivec2 neighborPosition = globalId + ivec2(dx, dy);

                       if (neighborPosition.x >= 0 && neighborPosition.x < imageSize.x &&
                            neighborPosition.y >= 0 && neighborPosition.y < imageSize.y) {
                                //ivec2 clampedPosition = clamp(ivec2(localId.y + dy, localId.x + dx), ivec2(0), ivec2(30));
                                ivec2 clampedPosition = clamp(ivec2(localId) + ivec2(dy + searchSize, dx + searchSize), ivec2(0), ivec2(sharedSize - 1));

                                //mediump vec4 neighborColor = sharedMemory[localId.y + dy][localId.x + dx];
                                //mediump vec4 neighborColor = sharedMemory[clampedPosition.y][clampedPosition.x];
                                mediump vec4 neighborColor = sharedMemory[SHARED_INDEX(clampedPosition.x, clampedPosition.y)];
                                
                                //float similarityNonSqrt = gaussianWeight(patchSimilarity(localId + searchRadius + patchRadius, localId + ivec2(dx, dy) + searchRadius + patchRadius), h); 
                                //float similarityNonSqrt = gaussianWeight(patchSimilarity(globalId, neighborPosition), h);
                                const float similarityNonSqrt = 1.0;

                                nlmFilteredColor += neighborColor.rgb * similarityNonSqrt;
                                weightSum += similarityNonSqrt;
                        }
                    }
               }
               barrier();

                
               // Normalize and write the result to outputTexture
               vec3 result = nlmFilteredColor / weightSum;
               //vec3 result = vec3(0.3, 0.5, 0.7);
               imageStore(outputImage, globalId, vec4(result, 1.0));  
                    
               barrier();
            */
 
            }
        """


        val vPositionAttribLocation = 10
        val aTexCoordAttribLocation = 11

        val vertexShader: Int = loadShader(GLES31.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES31.GL_FRAGMENT_SHADER, fragmentShaderCode)

        Log.d(
            "TEST_TAG_OUTPUT_VERTEX_FRAGMENT",
            "Triangle.init: vertexShader(0 = bad):$vertexShader, fragmentShader(0 = bad):$fragmentShader")

        var shaderProgram = GLES31.glCreateProgram()
        Log.d("TEST_TAG_OUTPUT", "Triangle.init: programObject(0 = bad, 1 = good): $shaderProgram")

        GLES31.glAttachShader(shaderProgram, vertexShader)
        GLES31.glAttachShader(shaderProgram, fragmentShader)
        GLES31.glBindAttribLocation(shaderProgram, vPositionAttribLocation, "vPosition")
        GLES31.glBindAttribLocation(shaderProgram, aTexCoordAttribLocation, "aTexCoord")
        GLES31.glLinkProgram(shaderProgram)

        var error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.init link: $error")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.init link: $error")
        }

        var linkResult = IntArray(1)
        linkResult[0] = -1

        GLES31.glGetProgramiv(shaderProgram, GLES31.GL_LINK_STATUS, linkResult, 0)
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

        mShaderProgram = shaderProgram;

        val computeShader: Int = loadShader(GLES31.GL_COMPUTE_SHADER, computeShaderCode)
        val computeProgram = GLES31.glCreateProgram();
        GLES31.glAttachShader(computeProgram, computeShader);
        GLES31.glLinkProgram(computeProgram);

        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.init compute link: $error")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.init compute link: $error")
        }

        linkResult = IntArray(1)
        linkResult[0] = -1

        GLES31.glGetProgramiv(shaderProgram, GLES31.GL_LINK_STATUS, linkResult, 0)
        error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.init compute link via Shaderiv error: $error")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.init compute link via Shaderiv error: $error")
        }
        if (linkResult[0] != GLES31.GL_TRUE) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.init compute link via linkResult")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.init compute link via linkResult")
        }
        Log.d(
            "TEST_TAG_OUTPUT",
            "Triangle.init compute link ok (1,1) - linkResult[0]: ${linkResult[0]}, GL_TRUE is 1"
        )

        mComputeProgram = computeProgram;
    }

    fun draw(mvpMatrix: FloatArray) {
        Log.d("TEST_TAG_OUTPUT", "fun draw")

        val vao = IntArray(1)
        GLES31.glGenVertexArrays(1, vao, 0)
        GLES31.glBindVertexArray(vao[0])

        var quadCoords = floatArrayOf( // in counterclockwise order:
            -1.0f, -1.0f, 0.0f, //top left
            1.0f, -1.0f, 0.0f, //bottom left
            -1.0f, 1.0f, 0.0f, //bottom right
            1.0f, 1.0f, 0.0f  //top right
        )

        val COORDS_PER_VERTEX = 3
        val vertexCount: Int = quadCoords.size / COORDS_PER_VERTEX
        val vertexStride: Int = COORDS_PER_VERTEX * 4

        var cropLeft : Float = 0.0f
        var cropRight : Float = 1.0f
        var cropTop : Float = 0.0f
        var cropBottom : Float = 1.0f

        if (SharedDataImage.selectResolution == 2) {
            cropLeft = 0.333f
            cropRight = 0.666f
            cropTop = 0.333f
            cropBottom = 0.666f
        } else if (SharedDataImage.selectResolution == 1) {
            cropLeft = 0.250f
            cropRight = 0.750f
            cropTop = 0.250f
            cropBottom = 0.750f
        }
        else if (SharedDataImage.selectResolution == 0) {
            cropLeft = 0.0f
            cropRight = 1.0f
            cropTop = 0.0f
            cropBottom = 1.0f
        }

        var textureCoords = floatArrayOf(
            cropRight, cropBottom, //cropLeft, cropBottom, //bottom left vertex
            cropRight, cropTop, //cropRight, cropBottom, //bottom right vertex
            cropLeft, cropBottom, //cropLeft, cropTop, //top left vertex
            cropLeft, cropTop, //cropRight, cropTop //top right vertex
        )

        var textureCoordsDefault = floatArrayOf(
            1.0f, 1.0f, //bottom left vertex
            1.0f, 0.0f, //bottom right vertex
            0.0f, 1.0f, //top left vertex
            0.0f, 0.0f //top right vertex
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
        dPositionHandle = GLES31.glGetAttribLocation(mShaderProgram, "vPosition")
        GLES31.glVertexAttribPointer(dPositionHandle, COORDS_PER_VERTEX, GLES31.GL_FLOAT, false, vertexStride, 0)
        GLES31.glEnableVertexAttribArray(dPositionHandle)

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, vbo[1])
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, textureCoords.size * 4, textureBuffer, GLES31.GL_STATIC_DRAW)
        dTextureHandle = GLES31.glGetAttribLocation(mShaderProgram, "aTexCoord")
        GLES31.glVertexAttribPointer(dTextureHandle, 2, GLES31.GL_FLOAT, false, 0, 0)
        GLES31.glEnableVertexAttribArray(dTextureHandle)

        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, vbo[2])
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, textureCoordsDefault.size * 4, textureBufferDefault, GLES31.GL_STATIC_DRAW)
        dTextureDefaultHandle = GLES31.glGetAttribLocation(mShaderProgram, "aTexCoordDefault")
        GLES31.glVertexAttribPointer(dTextureDefaultHandle, 2, GLES31.GL_FLOAT, false, 0, 0)
        GLES31.glEnableVertexAttribArray(dTextureDefaultHandle)


        dTexture10UniformHandle = GLES31.glGetUniformLocation(mShaderProgram, "textureSampler")
        var error = GLES31.glGetError()
        if (error != GLES31.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation textureSampler")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTexture10UniformHandle: $dTexture10UniformHandle"
            )
        }
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


        //dTexture0UniformHandle = GLES31.glGetUniformLocation(mShaderProgram, "input0")
        //error = GLES31.glGetError()
        //if (error != GLES31.GL_NO_ERROR) {
        //    Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation input0")
        //} else {
        //    Log.d(
        //        "TEST_TAG_OUTPUT",
        //        "OK: Triangle.draw.glGetUniformLocation: dTexture0UniformHandle: $dTexture0UniformHandle"
        //    )
        //}


        dSelectFragmentHandle = GLES31.glGetUniformLocation(mShaderProgram, "selectFragment")
        dScaleHandle = GLES20.glGetUniformLocation(mShaderProgram, "scale")
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


        GLES31.glUseProgram(mShaderProgram)
        //GLES31.glUseProgram(mComputeProgram)

        // 16x16 workgroups
        //val width = SharedDataImage.widthInput //3840
        //val height = SharedDataImage.heightInput //2160  // 4K Resolution
        //GLES31.glDispatchCompute((width + 15) / 16, (height + 15) / 16, 1)
        //GLES31.glMemoryBarrier(GLES31.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)


        Log.d("EXIST-GPU-BEGIN", "xxx")

        val GL_TIME_ELAPSED_EXT = 0x88BF
        val GL_QUERY_RESULT_AVAILABLE = 0x8867
        val GL_QUERY_RESULT = 0x8866

        val queries = IntArray(1)
        GLES31.glGenQueries(1, queries, 0)

        val startTime = System.nanoTime()
        GLES31.glBeginQuery(GL_TIME_ELAPSED_EXT, queries[0])

        //GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fboFrame);
        //GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, textureFrames[0], 0);

        //GLES31.glEnable(GLES31.GL_SCISSOR_TEST)
        //GLES31.glScissor(640, 360, 1280, 720) // Define center crop region
        //GLES31.glViewport(0, 0, 1280, 720)    // Set the viewport to match
        //GLES31.glDisable(GLES31.GL_SCISSOR_TEST)

        //GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, fboFrame);
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, textureFrames[0], 0);

        GLES31.glActiveTexture(GLES31.GL_TEXTURE10)
        GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES31.glUniform1i(dTexture10UniformHandle, 10)
        GLES31.glUniform1i(dSelectFragmentHandle, 0)

        GLES31.glViewport(0, 0, SharedDataImage.widthInput, SharedDataImage.heightInput)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, vertexCount)


        /*
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[0])

        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
        GLES31.glUniform1i(dSelectFragmentHandle, 1)

        // Draw the quad to render the texture
        GLES31.glViewport(0,0, 1080, 2201)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, vertexCount)
        */

        GLES31.glUseProgram(mComputeProgram)
        GLES31.glBindImageTexture(4, textureFrames[0], 0, false, 0, GLES31.GL_READ_ONLY, GLES31.GL_RGBA8)
        GLES31.glBindImageTexture(5, outputTextureId, 0, false, 0, GLES31.GL_WRITE_ONLY, GLES31.GL_RGBA8)

        val workgroupDim = 8 //032 //16
        val width = SharedDataImage.widthInput //+ (workgroupDim) //3840
        val height = SharedDataImage.heightInput //+ (workgroupDim) //2160
        val workgroupWidth = (width + (workgroupDim - 1)) / workgroupDim //width / workgroupDim //(width + 15) / 16
        val workgroupHeight = (height + (workgroupDim - 1)) / workgroupDim //height / workgroupDim //(height + 15) / 16

        GLES31.glDispatchCompute(workgroupWidth, workgroupHeight, 1)

        GLES31.glMemoryBarrier(GLES31.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT)// or GLES31.GL_TEXTURE_FETCH_BARRIER_BIT)
        //GLES31.glMemoryBarrier(GLES31.GL_TEXTURE_UPDATE_BARRIER_BIT)
        //GLES31.glMemoryBarrier(GLES31.GL_ALL_BARRIER_BITS)
        GLES31.glFinish()


        GLES31.glUseProgram(mShaderProgram)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE4)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureFrames[0])
        dTexture0UniformHandle = GLES31.glGetUniformLocation(mShaderProgram, "input0")
        GLES31.glUniform1i(dTexture0UniformHandle, 4)

        GLES31.glActiveTexture(GLES31.GL_TEXTURE5)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, outputTextureId)
        val displayTextureLocation = GLES31.glGetUniformLocation(mShaderProgram, "displayTexture")
        GLES31.glUniform1i(displayTextureLocation, 5)

        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
        GLES31.glUniform1i(dSelectFragmentHandle, 1)

        GLES31.glViewport(0,0, 1080, 2201)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, vertexCount)

        /*
        GLES31.glActiveTexture(GLES31.GL_TEXTURE1)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, outputTextureId)

        val displayTextureLocation = GLES31.glGetUniformLocation(mShaderProgram, "displayTexture")
        GLES31.glUniform1i(displayTextureLocation, 1)

        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
        GLES31.glUniform1i(dSelectFragmentHandle, 1)

        GLES31.glViewport(0,0, 1080, 2201)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, vertexCount)
        */


        val maxTextureUnits = IntArray(1)
        GLES31.glGetIntegerv(GLES31.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, maxTextureUnits, 0)
        Log.d("GLInfo", "Max texture units: ${maxTextureUnits[0]}")

        //GLES31.glDeleteProgram(mProgram)

        GLES31.glFinish()
        GLES31.glEndQuery(GL_TIME_ELAPSED_EXT)

        val endTime = System.nanoTime()
        val eTimeMS = (endTime - startTime) / 1_000_000.0

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

    //lateinit var renderer: MyGLRenderer

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

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val configurationInfo = activityManager.deviceConfigurationInfo

            if (configurationInfo.reqGlEsVersion >= 0x30001) {
                Log.d("TEST_TAG_OUTPUT_Support", "openGL 3.1 ES is supported")
            } else {
                // Handle the case where the device does not support OpenGL ES 3.1
                Log.d("TEST_TAG_OUTPUT_Support", "openGL 3.1 ES is NOT supported")
            }

            Log.d("TEST_TAG_OUTPUT", "MainActivity.MyGLSurfaceView.init1")
        }

        fun setSurfaceTextureReadyCallback(callback: (SurfaceTexture) -> Unit) {
            renderer.setSurfaceTextureReadyCallback(callback)
        }

        private var TOUCH_SCALE_FACTOR: Float = 180.0f / 320f
        private var previousX: Float = 0f
        private var previousY: Float = 0f

        override fun onTouchEvent(e: MotionEvent): Boolean {

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
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val previewWidth = 3840;
            val previewHeight = 2160;

            //val preview = Preview.Builder().setTargetResolution(Size(previewWidth, previewHeight)).build()
            //val preview = Preview.Builder().setTargetRotation(Surface.ROTATION_90)
            //    .build()
            val preview = Preview.Builder()//.setTargetRotation(Surface.ROTATION_90)
                .setTargetResolution(Size(previewWidth, previewHeight))
                .build()

            surfaceTextureG.setDefaultBufferSize(previewWidth, previewHeight)
            surfaceG = Surface(surfaceTextureG)

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

                request.provideSurface(surfaceG, cameraExecutor) {
                    Log.d("CameraX", "Surface is ready")
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST

            //previous experimentation
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

        Log.d("TEST_TAG_OUTPUT", "startCamera")
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}