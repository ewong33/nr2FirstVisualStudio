package com.example.mylogivnrgl

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage

import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions


import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.Image
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES20.GL_LUMINANCE
import android.opengl.GLES20.GL_RGB
import android.opengl.GLES20.GL_UNSIGNED_BYTE
import android.opengl.GLES31
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface

import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import com.google.android.gms.tasks.Task

import org.json.JSONObject.NULL
import java.nio.Buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.sql.Types.NULL

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import java.util.Locale


//ok
object SharedDataGamma {
    var gammaAll: Float = 1.0f
}

object SharedDataScale {
    var scaleAll: Float = 1.0f
}

object SharedDataImage {
    //var widthInput: Int = 767
    //var heightInput: Int = 914
    //var imageName: String = "bondcropfinal"

    //var widthInput: Int = 3840 //1080 //3840 //793
    //var heightInput: Int = 2160 //2201 //2160 //835
    //var imageName: String = "hanks"

    //var widthInput: Int = 1355
    //var heightInput: Int = 1564
    //var imageName: String = "hepburn"
    
    var selectResolution = 2 // 0 = 3840, 2160, 1 = 1920, 1080, 2 = 1280, 720

    var widthInput: Int = 1280 
    var heightInput: Int = 720 
}

//ok
class MyGLRenderer(private val glSurfaceView:GLSurfaceView) : GLSurfaceView.Renderer {
    private lateinit var mTriangle: Triangle

    var texture = IntArray(1)
    var texturePing = IntArray(1)
    var texturePong = IntArray(1)

    var fboPing = IntArray(1)
    var fboPong = IntArray(1)

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)

    @Volatile
    var angle: Float = 0f
    var gamma: Float = 0f
    var scale: Float = 0f

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
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS)
        val supportsLod = extensions?.contains("GL_EXT_shader_texture_lod") == true
        Log.d("EXTENSION", "SupportsLod: $supportsLod")


        GLES20.glGenTextures(1, texture, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE) //REPEAT) //GL_REPEAT)//GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE) //REPEAT)


        GLES20.glGenTextures(1, texturePing, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturePing[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
            GLES20.GL_RGB, SharedDataImage.widthInput, SharedDataImage.heightInput,0,
            GLES20.GL_RGB,
            GLES20.GL_UNSIGNED_BYTE, null as Buffer?);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE) //GL_REPEAT)//GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glGenFramebuffers(1, fboPing, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboPing[0]);
        //When you render, the output will be stored in this texture
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            texturePing[0], 0);
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.d("TTTEST_TAG_OUTPUT", "frame buffer down error")
        } else {
            Log.d("TTTEST_TAG_OUTPUT", "frame buffer ok")
        }


        GLES20.glGenTextures(1, texturePong, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturePong[0])
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
            GLES20.GL_RGB, SharedDataImage.widthInput, SharedDataImage.heightInput, 0,
            GLES20.GL_RGB,
            GLES20.GL_UNSIGNED_BYTE, null as Buffer?);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE) //GL_REPEAT)//GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glGenFramebuffers(1, fboPong, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboPong[0]);
        //When you render, the output will be stored in this texture
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            texturePong[0], 0);
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.d("TTTEST_TAG_OUTPUT", "frame buffer down error")
        } else {
            Log.d("TTTEST_TAG_OUTPUT", "frame buffer ok")
        }

        Log.d("TEST_TAG_OUTPUT_CREATE_PRE","MyGLRenderer.onSurfaceCreated Exit")
        Log.d("TEST_TAG_OUTPUT_PING_PONG", "ping: ${texturePing[0]} and ${fboPing[0]}, pong: ${texturePong[0]} and ${fboPong[0]}")
        //mTriangle = Triangle(texture[0], texturePing[0], texturePong[0], fboPing[0], fboPong[0])
        Log.d("TEST_TAG_OUTPUT_CREATE_POST","MyGLRenderer.onSurfaceCreated Exit")

        mTriangle = Triangle(texture[0], texturePing[0], texturePong[0], fboPing[0], fboPong[0])

        surfaceTexture = SurfaceTexture(texture[0])
        surfaceTexture.setOnFrameAvailableListener {
            // Notify the GLSurfaceView to render when a new frame is available
            //(context as GLSurfaceView).requestRender()
            glSurfaceView.requestRender()
        }

        surfaceTextureReadyCallback?.invoke(surfaceTexture)
    }

    //ok
    override fun onDrawFrame(unused: GL10) {
        Log.d("TEST_TAG_OUTPUT", "MyGLRenderer.onDrawFrame Enter")
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

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

        // Draw triangle
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
class Triangle(private var textureId: Int, private var texturePing: Int, private var texturePong: Int,
               private var fboPing: Int, private var fboPong: Int) {
    //ok
    var mProgram: Int = 0
    var dPositionHandle: Int = 0
    var dTextureHandle: Int = 0
    var dTextureUniformHandle: Int = 0
    var dTextureSizeHandle: Int = 0


    fun loadShader(type: Int, shaderCode: String): Int {
        Log.d("TEST_TAG_OUTPUT", "Triangle.loadShader Enter")

        var shader = GLES20.glCreateShader(type)
        Log.d("TEST_TAG_OUTPUT", "Triangle.loadShader, shader(0 = bad): $shader")

        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        var compileResult = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileResult, 0)
        //GL_NO_ERROR is 0
        var error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.compileShader")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.compileShader")
        }
        if (compileResult[0] != GLES20.GL_TRUE) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.compileShader via compileResult")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.compileShader via compileResult")
        }
        Log.d(
            "TEST_TAG_OUTPUT",
            "Triangle.compileShader compile ok (1,1) - compileResult[0]: ${compileResult[0]}, GL_TRUE is 1"
        )

        Log.d("TEST_TAG_OUTPUT", "Triangle.loadShader Exit")
        return shader
    }

    init {
        Log.d("TEST_TAG_OUTPUT", "Triangle.init")

        val vertexShaderCode = """
            #version 100
            precision mediump float; 
            attribute vec4 vPosition;
            attribute vec2 aTexCoord;
            varying vec2 texCoord;
            
            void main() {
                texCoord = aTexCoord; 
                gl_Position = vPosition;      
            }
        """

        val fragmentShaderCode = """
            #version 100
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES textureSampler;
            varying vec2 texCoord;
            uniform vec2 textureSize;
            
            uniform float k; 
            uniform float lambda;
            
            //vec3 fastExp(vec3 x) {
            //    return 1.0 / (1.0 + (x * x * 0.5) + (x * x * x * x * 0.25));
            //}
            
            
            vec3 expApprox1(vec3 x) {
                x = clamp(x, -10.0, 10.0);
                //float result = 1.0 + x * (1.0 + x * (0.5 + x * (0.166667 + x * 0.0416667))); 
                vec3 result =    1.0 + x * (1.0); // + x * (0.5 + x * (0.166667 + x * 0.0416667))); 
                //vec3 doubleX = x * x;
                //vec3 tripleX = doubleX * x; 
                //vec4 quadX = tripleX * x; 
                //vec3 result = 1.0 + x + ( doubleX / 2.0 ) + ( tripleX / 6.0 ) + (quadX / 24.0); 
                //vec3 result = 1.0 + x + (0.5 * doubleX) + (0.167 * tripleX) + (0.0417 * quadX); 
                
                return result;
            }
            
            vec3 expApprox4(vec3 x) {
                x = clamp(x, -10.0, 10.0);
                //float result = 1.0 + x * (1.0 + x * (0.5 + x * (0.166667 + x * 0.0416667))); 
                vec3 result =    1.0 + x * (1.0 + x * (0.5 + x * (0.166667 + x * 0.0416667))); 
                //vec3 doubleX = x * x;
                //vec3 tripleX = doubleX * x; 
                //vec4 quadX = tripleX * x; 
                //vec3 result = 1.0 + x + ( doubleX / 2.0 ) + ( tripleX / 6.0 ) + (quadX / 24.0); 
                //vec3 result = 1.0 + x + (0.5 * doubleX) + (0.167 * tripleX) + (0.0417 * quadX); 
                
                return result;
            }

            vec3 quad(vec3 x) {
                x = clamp(x, -10.0, 10.0);
                
                vec3 result = 1.0 / (1.0 + (x * x));
                return result; 
            }
            
            
            void main() {
                ////////////////////////////////////////////////////////////////////////////////////
                //*1
                vec2 texelSize = 1.0 / textureSize;

                // **Group texture fetches to reduce cache misses**
                vec4 row1[3], row2[3], row3[3];

                row1[0] = texture2D(textureSampler, texCoord + vec2(-texelSize.x, texelSize.y));
                row1[1] = texture2D(textureSampler, texCoord + vec2(0.0, texelSize.y));
                row1[2] = texture2D(textureSampler, texCoord + vec2(texelSize.x, texelSize.y));

                row2[0] = texture2D(textureSampler, texCoord + vec2(-texelSize.x, 0.0));
                row2[1] = texture2D(textureSampler, texCoord);
                row2[2] = texture2D(textureSampler, texCoord + vec2(texelSize.x, 0.0));

                row3[0] = texture2D(textureSampler, texCoord + vec2(-texelSize.x, -texelSize.y));
                row3[1] = texture2D(textureSampler, texCoord + vec2(0.0, -texelSize.y));
                row3[2] = texture2D(textureSampler, texCoord + vec2(texelSize.x, -texelSize.y));

                vec3 gradUp    = row1[1].rgb - row2[1].rgb;
                vec3 gradDown  = row3[1].rgb - row2[1].rgb;
                vec3 gradLeft  = row2[0].rgb - row2[1].rgb;
                vec3 gradRight = row2[2].rgb - row2[1].rgb;

                float kappa = 0.905; 
                float kappaInv = 1.0 / kappa;

                vec3 cUpNorm    = gradUp * kappaInv;
                vec3 cDownNorm  = gradDown * kappaInv;
                vec3 cLeftNorm  = gradLeft * kappaInv;
                vec3 cRightNorm = gradRight * kappaInv;

                // **Use approximate exponential to reduce ALU stalls**
                //vec3 cUp    = fastExp(cUpNorm);
                //vec3 cDown  = fastExp(cDownNorm);
                //vec3 cLeft  = fastExp(cLeftNorm);
                //vec3 cRight = fastExp(cRightNorm);
    
                vec3 cUp    = exp(-cUpNorm * cUpNorm);
                vec3 cDown  = exp(-cDownNorm * cDownNorm);
                vec3 cLeft  = exp(-cLeftNorm * cLeftNorm);
                vec3 cRight = exp(-cRightNorm * cRightNorm);

                vec3 diffusion = (cUp * gradUp) + (cDown * gradDown) + (cLeft * gradLeft) + (cRight * gradRight);

                gl_FragColor = row2[1] + (0.25 * vec4(diffusion, 0.0));
                
                
                
                ////////////////////////////////////////////////////////////////////////////////////
                /*2
                vec2 texelSize = 1.0 / textureSize;

                // **Precompute offsets for 3x3 fetch**
                vec2 offsets[9];
                offsets[0] = texCoord + vec2(-texelSize.x,  texelSize.y);  // Top-left
                offsets[1] = texCoord + vec2( 0.0,          texelSize.y);  // Top-center
                offsets[2] = texCoord + vec2( texelSize.x,  texelSize.y);  // Top-right
                offsets[3] = texCoord + vec2(-texelSize.x,  0.0);          // Mid-left
                offsets[4] = texCoord;                                     // Center
                offsets[5] = texCoord + vec2( texelSize.x,  0.0);          // Mid-right
                offsets[6] = texCoord + vec2(-texelSize.x, -texelSize.y);  // Bottom-left
                offsets[7] = texCoord + vec2( 0.0,         -texelSize.y);  // Bottom-center
                offsets[8] = texCoord + vec2( texelSize.x, -texelSize.y);  // Bottom-right

                // **Fetch all 3x3 pixels at once for better memory locality**
                vec4 pixels[9];
                for (int i = 0; i < 9; i++) {
                    pixels[i] = texture2D(textureSampler, offsets[i]);
                }

                // **Extract neighbors**
                vec4 centerP = pixels[4];
                vec3 gradUp    = pixels[1].rgb - centerP.rgb;
                vec3 gradDown  = pixels[7].rgb - centerP.rgb;
                vec3 gradLeft  = pixels[3].rgb - centerP.rgb;
                vec3 gradRight = pixels[5].rgb - centerP.rgb;

                float kappa = 0.905; 
                float kappaInv = 1.0 / kappa;

                // **Normalize gradients**
                vec3 cUpNorm    = gradUp * kappaInv;
                vec3 cDownNorm  = gradDown * kappaInv;
                vec3 cLeftNorm  = gradLeft * kappaInv;
                vec3 cRightNorm = gradRight * kappaInv;

                // **Apply diffusion**
                vec3 diffusion = exp(-cUpNorm * cUpNorm) * gradUp +
                     exp(-cDownNorm * cDownNorm) * gradDown +
                     exp(-cLeftNorm * cLeftNorm) * gradLeft +
                     exp(-cRightNorm * cRightNorm) * gradRight;

                // **Output optimized**
                gl_FragColor = centerP + (0.25 * vec4(diffusion, 1.0));
                */
                
                
                ////////////////////////////////////////////////////////////////////////////////////
                /*3
                vec2 texelSize = 1.0 / textureSize;

                // **Prefetch texture coordinates first to reduce dependent fetch stalls**
                vec2 upOffset    = texCoord + vec2(0.0, texelSize.y);
                vec2 downOffset  = texCoord + vec2(0.0, -texelSize.y);
                vec2 leftOffset  = texCoord + vec2(-texelSize.x, 0.0);
                vec2 rightOffset = texCoord + vec2(texelSize.x, 0.0);

                // **Texture fetch order optimized for memory access**
                vec4 centerP = texture2D(textureSampler, texCoord);
                vec4 upP     = texture2D(textureSampler, upOffset);
                vec4 downP   = texture2D(textureSampler, downOffset);
                vec4 leftP   = texture2D(textureSampler, leftOffset);
                vec4 rightP  = texture2D(textureSampler, rightOffset);

                // **Compute gradients**
                vec3 gradUp    = upP.rgb    - centerP.rgb;
                vec3 gradDown  = downP.rgb  - centerP.rgb;
                vec3 gradLeft  = leftP.rgb  - centerP.rgb;
                vec3 gradRight = rightP.rgb - centerP.rgb;

                float kappa = 0.905; 
                float kappaInv = 1.0 / kappa;

                // **Normalize gradients**
                vec3 cUpNorm    = gradUp * kappaInv;
                vec3 cDownNorm  = gradDown * kappaInv;
                vec3 cLeftNorm  = gradLeft * kappaInv;
                vec3 cRightNorm = gradRight * kappaInv;

                // **Minimize expensive exponentials**
                vec3 cUp    = exp(-cUpNorm * cUpNorm);
                vec3 cDown  = exp(-cDownNorm * cDownNorm);
                vec3 cLeft  = exp(-cLeftNorm * cLeftNorm);
                vec3 cRight = exp(-cRightNorm * cRightNorm);

                // **Reduce ALU overhead in diffusion computation**
                vec3 diffusion = (cUp * gradUp) + (cDown * gradDown) + (cLeft * gradLeft) + (cRight * gradRight);

                // **Reduce number of vector operations**
                gl_FragColor = centerP + (0.25 * vec4(diffusion, 0.0));
                //gl_FragColor = vec4(1.0, 1.0, 0.0, 1.0); 
                */


                ////////////////////////////////////////////////////////////////////////////////////
                /*4
                vec2 texelSize = 1.0 / textureSize;

                // Precompute offsets (to avoid dependent fetch stalls)
                vec2 upOffset    = clamp(texCoord + vec2(0.0, texelSize.y), 0.0, 1.0);
                vec2 downOffset  = clamp(texCoord + vec2(0.0, -texelSize.y), 0.0, 1.0);
                vec2 leftOffset  = clamp(texCoord + vec2(-texelSize.x, 0.0), 0.0, 1.0);
                vec2 rightOffset = clamp(texCoord + vec2(texelSize.x, 0.0), 0.0, 1.0);

                // Fetch texels in a cache-friendly order
                vec4 leftP   = texture2D(textureSampler, leftOffset);
                vec4 centerP = texture2D(textureSampler, texCoord);
                vec4 rightP  = texture2D(textureSampler, rightOffset);
                vec4 upP     = texture2D(textureSampler, upOffset);
                vec4 downP   = texture2D(textureSampler, downOffset);

                // Compute gradients
                vec3 gradUp    = upP.rgb    - centerP.rgb;
                vec3 gradDown  = downP.rgb  - centerP.rgb;
                vec3 gradLeft  = leftP.rgb  - centerP.rgb;
                vec3 gradRight = rightP.rgb - centerP.rgb;

                float kappa = 0.905; 
                float kappaInv = 1.0 / kappa;

                // Normalize gradients
                vec3 cUpNorm    = gradUp * kappaInv;
                vec3 cDownNorm  = gradDown * kappaInv;
                vec3 cLeftNorm  = gradLeft * kappaInv;
                vec3 cRightNorm = gradRight * kappaInv;

                // Optimize diffusion coefficient calculation
                vec3 cUp    = exp(-cUpNorm * cUpNorm);
                vec3 cDown  = exp(-cDownNorm * cDownNorm);
                vec3 cLeft  = exp(-cLeftNorm * cLeftNorm);
                vec3 cRight = exp(-cRightNorm * cRightNorm);

                // Compute diffusion
                vec3 diffusion = 
                    cUp * gradUp +
                    cDown * gradDown +
                    cLeft * gradLeft +
                    cRight * gradRight;

                // Optimize final computation
                vec4 centerPOut = centerP + (0.25 * vec4(diffusion, 0.0));
                gl_FragColor = vec4(centerPOut.rgb,1.0); //Out;
                //gl_FragColor = vec4(1.0, 1.0, 0.0, 1.0); 
                */
                   
            
                ////////////////////////////////////////////////////////////////////////////////////
                /*5
                vec2 texelSize = 1.0 / textureSize;    
                
                //Get the center and neighbor pixel intensities
                //vec4 color   = texture2D(textureSampler, texCoord);
                vec4 centerP = texture2D(textureSampler, texCoord);
                vec4 upP     = texture2D(textureSampler, clamp(texCoord + vec2(0.0, texelSize.y), 0.0 , 1.0));
                vec4 downP   = texture2D(textureSampler, clamp(texCoord + vec2(0.0, -texelSize.y), 0.0, 1.0));
                vec4 leftP   = texture2D(textureSampler, clamp(texCoord + vec2(-texelSize.x, 0.0), 0.0, 1.0));
                vec4 rightP  = texture2D(textureSampler, clamp(texCoord + vec2(texelSize.x, 0.0), 0.0, 1.0));

                //vec4 upLeftP = texture2D(textureSampler, clamp(texCoord + vec2(-texelSize.x, texelSize.y), 0.0 , 1.0));
                //vec4 upRightP = texture2D(textureSampler, clamp(texCoord + vec2(texelSize.x, texelSize.y), 0.0, 1.0));
                //vec4 downLeftP = texture2D(textureSampler, clamp(texCoord + vec2(-texelSize.x, -texelSize.y), 0.0, 1.0));
                //vec4 downRightP = texture2D(textureSampler, clamp(texCoord + vec2(texelSize.x, -texelSize.y), 0.0, 1.0));

                //Compute gradients
                vec3 gradUp    = upP.rgb    - centerP.rgb;
                vec3 gradDown  = downP.rgb  - centerP.rgb;
                vec3 gradLeft  = leftP.rgb  - centerP.rgb;
                vec3 gradRight = rightP.rgb - centerP.rgb;
                
                               
                //suppose kappa ~0, say 0.00001;  kappaInv is large 100000, cUpNorm is large, cUpNorm^2 is even larger,
                // exp(-cUpNorm^2) = 0 = no denoise
                // suppose kappa = 1.0  kappaInv is 1.0, cUpNorm is just grad. cUpNorm^2 is gradSq.  exp(.01) = ~ 1; 
                // Hence, as kappa increases, we get more and more smoothing.  as kappa decreases, we get less and less 
                
                float kappa = 0.905; //1000.001; //2.7; //1000.0; //250.0; //2.7; //0.1; //100.1; //0.1; //100.0; //2.7; //2.7; //10.0; //500.0; //1000.0; //2.0;
                float kappaInv = 1.0 / kappa; 
                
                //vec3 cUpNorm = gradUp / kappa;
                //vec3 cDownNorm = gradDown / kappa;
                //vec3 cLeftNorm = gradLeft / kappa;
                //vec3 cRightNorm = gradRight / kappa; 
                            
                vec3 cUpNorm = gradUp * kappaInv;
                vec3 cDownNorm = gradDown * kappaInv;
                vec3 cLeftNorm = gradLeft * kappaInv;
                vec3 cRightNorm = gradRight * kappaInv; 
                       
                //vec3 cUp = exp(-dot(cUpNorm, cUpNorm)); 
                //vec3 cDown = exp(-dot(cDownNorm, cDownNorm));
                //vec3 cLeft = exp(-dot(cLeftNorm, cLeftNorm)); 
                //vec3 cRight = exp(-dot(cRightNorm, cRightNorm));
                      

                vec3 cUp = exp(-(cUpNorm * cUpNorm)); 
                vec3 cDown = exp(-(cDownNorm * cDownNorm));
                vec3 cLeft = exp(-(cLeftNorm * cLeftNorm)); 
                vec3 cRight = exp(-(cRightNorm * cRightNorm));
                
                //vec3 cUp = expApprox1(-(cUpNorm * cUpNorm)); 
                //vec3 cDown = expApprox1(-(cDownNorm * cDownNorm));
                //vec3 cLeft = expApprox1(-(cLeftNorm * cLeftNorm)); 
                //vec3 cRight = expApprox1(-(cRightNorm * cRightNorm));
                
                //vec3 cUp = expApprox4(-(cUpNorm * cUpNorm)); 
                //vec3 cDown = expApprox4(-(cDownNorm * cDownNorm));
                //vec3 cLeft = expApprox4(-(cLeftNorm * cLeftNorm)); 
                //vec3 cRight = expApprox4(-(cRightNorm * cRightNorm));
                
                //vec3 cUp = quad(-(cUpNorm * cUpNorm)); 
                //vec3 cDown = quad(-(cDownNorm * cDownNorm));
                //vec3 cLeft = quad(-(cLeftNorm * cLeftNorm)); 
                //vec3 cRight = quad(-(cRightNorm * cRightNorm));
                         
           
                //vec3 cUp    = exp(-((gradUp / kappa) * (gradUp / kappa))); 
                //vec3 cDown  = exp(-((gradDown / kappa) * (gradDown / kappa)));
                //vec3 cLeft  = exp(-((gradLeft / kappa) * (gradLeft / kappa)));
                //vec3 cRight  = exp(-((gradRight / kappa) * (gradRight / kappa)));


                //Apply diffusion update
                vec3 diffusion = 
                    cUp * gradUp +
                    cDown * gradDown +
                    cLeft * gradLeft +
                    cRight * gradRight; 
                    
                vec4 diffusionAlpha = vec4(diffusion, 0.0);

                vec4 centerPOut = centerP + (0.25 * diffusionAlpha);
                gl_FragColor = centerPOut; 
                */
                
                //gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
           
                //vec3 diff = centerPOut.rgb - color.rgb; 
                //gl_FragColor = vec4(2.0 * diff, 1.0);
                
                //color.rgb = toneCurve(color.rgb);
                //gl_FragColor = color;
                
                //gl_FragColor = vec4(texCoord, 0.0, 1.0);  
            }
        """

        val vPositionAttribLocation = 10
        val aTexCoordAttribLocation = 11

        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        Log.d(
            "TEST_TAG_OUTPUT",
            "Triangle.init: vertexShader(0 = bad):$vertexShader, fragmentShader(0 = bad):$fragmentShader"
        )

        var programObject = GLES20.glCreateProgram()
        Log.d("TEST_TAG_OUTPUT", "Triangle.init: programObject(0 = bad, 1 = good): $programObject")

        GLES20.glAttachShader(programObject, vertexShader)
        GLES20.glAttachShader(programObject, fragmentShader)
        GLES20.glBindAttribLocation(programObject, vPositionAttribLocation, "vPosition")
        GLES20.glBindAttribLocation(programObject, aTexCoordAttribLocation, "aTexCoord")
        GLES20.glLinkProgram(programObject)

        var error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.init link: $error")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.init link: $error")
        }

        var linkResult = IntArray(1)
        linkResult[0] = -1

        GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linkResult, 0)
        error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.init link via Shaderiv error: $error")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.init link via Shaderiv error: $error")
        }
        if (linkResult[0] != GLES20.GL_TRUE) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.init link via linkResult")
        } else {
            Log.d("TEST_TAG_OUTPUT", "OK: Triangle.init link via  linkResult")
        }
        Log.d(
            "TEST_TAG_OUTPUT",
            "Triangle.init link ok (1,1) - linkResult[0]: ${linkResult[0]}, GL_TRUE is 1"
        )

        mProgram = programObject

/*
        var quadCoords = floatArrayOf( // in counterclockwise order:
            -1.0f, -1.0f, 0.0f, // Top left vertex
            1.0f, -1.0f, 0.0f, // Bottom left vertex
            -1.0f, 1.0f, 0.0f, // Bottom right vertex
            1.0f, 1.0f, 0.0f  // Top right vertex
        )

        val COORDS_PER_VERTEX = 3
        val vertexCount: Int = quadCoords.size / COORDS_PER_VERTEX
        val vertexStride: Int = COORDS_PER_VERTEX * 4

        var textureCoords = floatArrayOf(
            1.0f, 1.0f, // Bottom left vertex
            1.0f, 0.0f, // Bottom right vertex
            0.0f, 1.0f, // Top left vertex
            0.0f, 0.0f // Top right vertex
        )

        var vertexBuffer: FloatBuffer =
            // (number of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(quadCoords.size * 4).run {
                // use the device hardware's native byte order
                order(ByteOrder.nativeOrder())

                // create a floating point buffer from the ByteBuffer
                asFloatBuffer().apply {
                    // add the coordinates to the FloatBuffer
                    put(quadCoords)
                    // set the buffer to read the first coordinate
                    position(0)
                }
            }

        var textureBuffer: FloatBuffer =
            // (number of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(textureCoords.size * 4).run {
                // use the device hardware's native byte order
                order(ByteOrder.nativeOrder())

                // create a floating point buffer from the ByteBuffer
                asFloatBuffer().apply {
                    // add the coordinates to the FloatBuffer
                    put(textureCoords)
                    // set the buffer to read the first coordinate
                    position(0)
                }
            }

        dPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also { positionHandle ->
            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(positionHandle)

            Log.d(
                "TEST_TAG_OUTPUT",
                "Triangle.draw, mProgram: $mProgram, dPositionHandle: $dPositionHandle"
            )

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                positionHandle,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )
        }

        dTextureHandle =
            GLES20.glGetAttribLocation(mProgram, "aTexCoord").also { textureHandle ->

                GLES20.glEnableVertexAttribArray(textureHandle)

                Log.d(
                    "TEST_TAG_OUTPUT",
                    "Triangle.draw, mProgram: $mProgram, dTextureHandle: $dTextureHandle"
                )

                GLES20.glVertexAttribPointer(
                    textureHandle,
                    2, // Number of coordinates per vertex (2 for texture coordinates)
                    GLES20.GL_FLOAT,
                    false,
                    2 * 4, // 4 bytes per vertex
                    textureBuffer
                )
            }

        dTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "textureSampler")
        error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation textureSampler")
        } else {
            Log.d(
                "TEST_TAG_OUTPUT",
                "OK: Triangle.draw.glGetUniformLocation: dTextureUniformHandle: $dTextureUniformHandle"
            )
        }

        //Set the sampler uniform to the location of the texture unit. Samples from texture unit 0.
        //The next time the shader is accessed, it will use the data sitting here as input,
        //via texture unit 0

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(dTextureUniformHandle, 0)

        error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glUniform1i: textureUniformHandle: $error")
        }
        else {
            Log.d("TEST_TAG_OUTPUT","OK: Triangle.draw.glUniform1i: textureUniformHandle: $dTextureUniformHandle")
        }


        val textureWidth = SharedDataImage.widthInput
        val textureHeight = SharedDataImage.heightInput
        //Get the location of the uniform variable in the shader program
        dTextureSizeHandle = GLES20.glGetUniformLocation(mProgram, "textureSize")
        //Pass the texture size to the shader program
        GLES20.glUniform2f(dTextureSizeHandle, textureWidth.toFloat(), textureHeight.toFloat())

        Log.d(
            "TEST_TAG_OUTPUT",
            "Triangle.init, mProgram(0 = bad): $mProgram, linked: ${linkResult[0]}"
        )
 */
    }


    fun draw(mvpMatrix: FloatArray) {
        Log.d("TEST_TAG_OUTPUT", "fun draw")

        var quadCoords = floatArrayOf( // in counterclockwise order:
            -1.0f, -1.0f, 0.0f, // Top left vertex
            1.0f, -1.0f, 0.0f, // Bottom left vertex
            -1.0f, 1.0f, 0.0f, // Bottom right vertex
            1.0f, 1.0f, 0.0f  // Top right vertex
        )

        val COORDS_PER_VERTEX = 3
        val vertexCount: Int = quadCoords.size / COORDS_PER_VERTEX
        val vertexStride: Int = COORDS_PER_VERTEX * 4

        //var textureCoords = floatArrayOf(
        //    1.0f, 1.0f, // Bottom left vertex
        //    1.0f, 0.0f, // Bottom right vertex
        //    0.0f, 1.0f, // Top left vertex
        //    0.0f, 0.0f // Top right vertex
        //)
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


        var vertexBuffer: FloatBuffer =
            // (number of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(quadCoords.size * 4).run {
                // use the device hardware's native byte order
                order(ByteOrder.nativeOrder())

                // create a floating point buffer from the ByteBuffer
                asFloatBuffer().apply {
                    // add the coordinates to the FloatBuffer
                    put(quadCoords)
                    // set the buffer to read the first coordinate
                    position(0)
                }
            }

        var textureBuffer: FloatBuffer =
            // (number of coordinate values * 4 bytes per float)
            ByteBuffer.allocateDirect(textureCoords.size * 4).run {
                // use the device hardware's native byte order
                order(ByteOrder.nativeOrder())

                // create a floating point buffer from the ByteBuffer
                asFloatBuffer().apply {
                    // add the coordinates to the FloatBuffer
                    put(textureCoords)//put(textureCoords)
                    // set the buffer to read the first coordinate
                    position(0)
                }
            }

        dPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also { positionHandle ->

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(positionHandle)

            Log.d(
                "TEST_TAG_OUTPUT",
                "Triangle.draw, mProgram: $mProgram, dPositionHandle: $dPositionHandle"
            )

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                positionHandle,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )

            dTextureHandle =
                GLES20.glGetAttribLocation(mProgram, "aTexCoord").also { textureHandle ->

                    GLES20.glEnableVertexAttribArray(textureHandle)

                    Log.d(
                        "TEST_TAG_OUTPUT",
                        "Triangle.draw, mProgram: $mProgram, dTextureHandle: $dTextureHandle"
                    )

                    GLES20.glVertexAttribPointer(
                        textureHandle,
                        2, // Number of coordinates per vertex (2 for texture coordinates)
                        GLES20.GL_FLOAT,
                        false,
                        2 * 4, // 4 bytes per vertex
                        textureBuffer
                    )
                }

            dTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "textureSampler")
            var error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) {
                Log.d("TEST_TAG_OUTPUT", "ERROR: Triangle.draw.glGetUniformLocation textureSampler")
            } else {
                Log.d(
                    "TEST_TAG_OUTPUT",
                    "OK: Triangle.draw.glGetUniformLocation: dTextureUniformHandle: $dTextureUniformHandle"
                )
            }

            //Set the sampler uniform to the location of the texture unit. Samples from texture unit 0.
            //The next time the shader is accessed, it will use the data sitting here as input,
            //via texture unit 0
            GLES20.glUniform1i(dTextureUniformHandle, 0)

            error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) {
                Log.d(
                    "TEST_TAG_OUTPUT",
                    "ERROR: Triangle.draw.glUniform1i: textureUniformHandle: $error"
                )
            } else {
                Log.d(
                    "TEST_TAG_OUTPUT",
                    "OK: Triangle.draw.glUniform1i: textureUniformHandle: $dTextureUniformHandle"
                )
            }

            val textureWidth = SharedDataImage.widthInput
            val textureHeight = SharedDataImage.heightInput
            //Get the location of the uniform variable in the shader program
            dTextureSizeHandle = GLES20.glGetUniformLocation(mProgram, "textureSize")
            //Pass the texture size to the shader program
            GLES20.glUniform2f(dTextureSizeHandle, textureWidth.toFloat(), textureHeight.toFloat())
        }

        //Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboPing)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glViewport(0, 0, SharedDataImage.widthInput, SharedDataImage.heightInput)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount)


        val GL_TIME_ELAPSED_EXT = 0x88BF
        val GL_QUERY_RESULT_AVAILABLE = 0x8867
        val GL_QUERY_RESULT = 0x8866

        //Log.d("EXIST-GPU-BEGIN")

        val queries = IntArray(1)
        GLES31.glGenQueries(1, queries, 0)
        //GLES31.glBeginQuery(GL_TIME_ELAPSED_EXT, queries[0])

        val startTime = System.nanoTime()


        GLES31.glBeginQuery(GL_TIME_ELAPSED_EXT, queries[0])

        var pingPong: Int = 1

        for (iLoop in 0 until 13){ //13) {
            Log.d("TEST_TAG_OUTPUT_ITERATE", "iLoop: $iLoop")

            // 3. Clear the framebuffer before rendering (if needed)
            if (pingPong == 0) {
                //GLES20.glClearColor(0.0f, 1.0f, 1.0f, 1.0f);
                //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                //GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboPing)

                GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturePong)
                GLES20.glUniform1i(dTextureUniformHandle, 2)
                GLES20.glViewport(0, 0, SharedDataImage.widthInput, SharedDataImage.heightInput)

                //GLES31.glBeginQuery(GL_TIME_ELAPSED_EXT, queries[0])
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount)
                //GLES31.glEndQuery(GL_TIME_ELAPSED_EXT)
            } else if (pingPong == 1) {
                //GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
                //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                //GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboPong)

                GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturePing)
                GLES20.glUniform1i(dTextureUniformHandle, 1)

                GLES20.glViewport(0, 0, SharedDataImage.widthInput, SharedDataImage.heightInput) //1080, 2201)


                //GLES31.glBeginQuery(GL_TIME_ELAPSED_EXT, queries[0])
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount)
                //GLES31.glEndQuery(GL_TIME_ELAPSED_EXT)


                //GLES20.glReadPixels(0, 0, 1355, 1654, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
            }

    /*
            val available = IntArray(1)
            do {
                GLES31.glGetQueryObjectuiv(queries[0], GL_QUERY_RESULT_AVAILABLE, available, 0)
            } while (available[0] == GLES31.GL_FALSE)

            val elapsedTime = IntArray(1)
            GLES31.glGetQueryObjectuiv(queries[0], GL_QUERY_RESULT, elapsedTime, 0)

            val elapsedTimeMs = elapsedTime[0] / 1_000_000.0
            Log.d("EXIST-GPU: $pingPong","Time taken: $elapsedTimeMs ms")
    */

            pingPong = 1 - pingPong
        } //iLoop

        Log.d("TEST_TAG_OUTPUT_ITERATE_EXIT", "BindFB = 0")

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        if (pingPong == 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturePong)
            GLES20.glUniform1i(dTextureUniformHandle, 2)
        } else if (pingPong == 1) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturePing)
            GLES20.glUniform1i(dTextureUniformHandle, 1)
        }

        GLES20.glViewport(0, 0, 1080, 2201)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount)

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
    lateinit var detector: FaceDetector
    lateinit var segmenter: Segmenter

    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TEST_TAG_OUTPUT","MainActivity.onCreate0")

        super.onCreate(savedInstanceState)

        //setupFaceDetector()
        //setupSelfieMask()

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

            val supportsES31 = context.packageManager.hasSystemFeature(PackageManager.FEATURE_OPENGLES_EXTENSION_PACK)
            Log.d("COMPUTE SHADER SUPPORT", "Result: $supportsES31")

            setEGLContextClientVersion(2)
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
                        renderer.scale = 0.0f
                    }
                    else if (renderer.angle > 90.0f) {
                        renderer.gamma = 3.1f
                        renderer.scale = 25.0f
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


    private fun setupFaceDetector() {
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(realTimeOpts)
    }

    private fun setupSelfieMask() {
        val options =
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
                .enableRawSizeMask()
                .build()

        segmenter = Segmentation.getClient(options)
    }

    fun processFaceImage(mediaImage: Image, rotation: Int) {
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(image)
            .addOnSuccessListener { faces ->
                // Handle detected faces
                for (face in faces) {
                    // Get face bounds, landmarks, etc.
                    //left, top, right, bottom
                    val bounds = face.boundingBox
                    val left = bounds.left
                    val right = bounds.right + 500
                    val top = bounds.top
                    val bottom = bounds.bottom + 500

                    //
                    val width = 3840
                    val height = 2160
                    val bufferSize = width * height

                    val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
                    //val byteBufferC = ByteBuffer.allocateDirect(bufferSize)
                    byteBuffer.order(ByteOrder.nativeOrder())
                    //byteBufferC.order(ByteOrder.nativeOrder())

                    val byteArray = ByteArray(bufferSize) {1}
                    byteBuffer.put(byteArray)
                    byteBuffer.rewind()

                    //val byteArrayC = ByteArray(bufferSize){0}
                    //byteBufferC.put(byteArrayC)
                    //byteBufferC.rewind()

                    //for (y in top until bottom) {
                    //    for (x in left until right) {
                    //        val value = 0
                    //        byteBuffer.put(value.toByte())
                    //val valueC = 1
                    //byteBufferC.put(valueC.toByte())
                    //    }
                    //}
                    //byteBuffer.rewind()
                    //byteBufferC.rewind()

                    Log.d("FACE_PROCESS_BEFORE", "bounds (left, top, right, bottom): $bounds")
                    //renderer.setByteBuffer(byteBuffer) //, byteBufferC)
                    Log.d("FACE_PROCESS_AFTER", "bounds")

                    //Log.d("TEST_SET_FACE_OUT", "bounds (left, top, right, bottom): $bounds")
                    //Log.d("TEST_SET_FACE_OUT", "left: $left, right: $right, top: $top, bottom: $bottom")

                    //val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                    // Use the detected face features as needed...
                }
            }
            .addOnFailureListener { e ->
                // Handle error
            }
    }

    fun processSelfieImage(mediaImage: Image, rotation: Int) {
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        segmenter.process(image)
            .addOnSuccessListener { segmentationMask ->
                // Task completed successfully
                // ...

                val mask = segmentationMask.getBuffer()
                val maskWidth = segmentationMask.getWidth()
                val maskHeight = segmentationMask.getHeight()

                Log.d("SEGMENT_TAG_OUTPUT","maskWidth: $maskWidth, maskHeight: $maskHeight")

                for (y in 0 until maskHeight) {
                    for (x in 0 until maskWidth) {
                        // Gets the confidence of the (x, y) pixel in the mask being in the foreground.
                        val foregroundConfidence = mask.float
                        // Process the confidence value as needed...
                    }
                }
            }

            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
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

                        Log.d("FACE_ANALYZER","SET")

                        val width = 3840 //2160 //3840
                        val height = 2160 //3840 //2160
                        val bufferSize = width * height

                        val byteBuffer = ByteBuffer.allocateDirect(bufferSize)

                        byteBuffer.order(ByteOrder.nativeOrder())

                        val byteArray = ByteArray(bufferSize) {1}
                        byteBuffer.put(byteArray)
                        byteBuffer.rewind()

                        val top = 100
                        val bottom = 500
                        val left = 100
                        val right = 500

                        //for (y in top until bottom) {
                        // for (x in left until right) {
                        //        val value = 0
                        //        byteBuffer.put(value.toByte())
                        //val valueC = 1
                        //byteBufferC.put(valueC.toByte())
                        //    }
                        //}
                        //byteBuffer.rewind()
                        //byteBufferC.rewind()

                        Log.d("FACE_BEFORE_RENDERER", "bounds")
                        //renderer.setByteBuffer(byteBuffer) //, byteBufferC)
                        Log.d("FACE_AFTER_RENDERER", "bounds")

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
        detector.close()
        cameraExecutor.shutdown()
    }
}

