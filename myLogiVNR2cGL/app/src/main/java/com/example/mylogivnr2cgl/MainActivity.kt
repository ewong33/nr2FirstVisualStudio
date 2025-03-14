package com.example.mylogivnr2cgl

import android.os.Bundle


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

import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface

import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
//import androidx.camera.core.*
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview

import java.nio.Buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

//ok
object SharedDataGamma {
    var gammaAll: Float = 1.0f
}

object SharedDataScale {
    var scaleAll: Float = 1.0f
}

object SharedDataImage {
    var selectResolution = 2 // 0 = 3840, 2160, 1 = 1920, 1080, 2 = 1280, 720

    var widthInput: Int = 1280  
    var heightInput: Int = 720 
}

//ok
class MyGLRenderer(private val glSurfaceView:GLSurfaceView) : GLSurfaceView.Renderer {
    private lateinit var mTriangle: Triangle

    var texture = IntArray(1)

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
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT) //GL_REPEAT)//GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

        mTriangle = Triangle(texture[0])  //}, texturePing[0], texturePong[0], fboPing[0], fboPong[0])

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

        val ratio: Float = width.toFloat() / height.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

        Log.d("TEST_TAG_OUTPUT", "MyGLRenderer.onSurfaceChanged Exit")
    }
}


//ok
class Triangle(private var textureId: Int) { //}, private var texturePing: Int, private var texturePong: Int,
//               private var fboPing: Int, private var fboPong: Int) {
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
            precision highp float;
            uniform samplerExternalOES textureSampler;
            varying vec2 texCoord;
            uniform vec2 textureSize;  //720p, 1080p
            
            uniform float k; 
            uniform float lambda;
            
            float computeNLMWeight(float similarity, float sigma) {
                return exp(-similarity / (2.0 * sigma * sigma));
            }

            float computePatchDifference(vec2 texCoord, vec2 neighborPatchCoord, vec2 texelSize, int patchRadius) {
                float difference = 0.0; 
                     
                for (int col = -patchRadius; col <= patchRadius; col++) {
                    for (int row = -patchRadius; row <= patchRadius; row++) {
                        vec2 shift = vec2(float(col), float(row)) * texelSize; 
                        
                        vec2 refCoord = clamp(texCoord + shift, vec2(0.0), vec2(1.0)); 
                        vec4 ref = texture2D(textureSampler, refCoord); //texCoord + shift); 
                        //vec2 patchCoord = clamp(neighborPatchCoord + shift, vec2(0.0), vec2(1.0)); 
                        //vec4 patch = texture2D(textureSampler, patchCoord); //neighborPatchCoord + shift); 
                        difference += distance(ref.rgb, patch.rgb); 
                        //difference += dot(ref.rgb - patch.rgb, ref.rgb - patch.rgb); // Avoid sqrt used in distance
                    }
                }
                
                return difference; 
            }
 
            void main() {
                vec2 texelSize = 1.0 / textureSize;  
                int patchSearchRadius = 3; 
                int patchMatchRadius = 1; 
                float sigma = 0.25; 
                
                float totalWeight = 0.0;
                vec4 weightedSum = vec4(0.0); 
                   
                float cropWidth = 1.0; //1280.0 / 3840.0; //textureCropSize.x / textureFullSize.x;  // 1080px / 3840px -> normalized width
                float cropHeight = 1.0; //720.0 / 2160.0; //textureCropSize.y / textureFullSize.y; // 720px / 2160px -> normalized height

                //Rescale the texture coordinates to match the 1080x720 cropped region
                vec2 scaledTextureCoord = vec2(texCoord.x * cropWidth + (1.0 - cropWidth) / 2.0,  // Rescale x coordinate
                   texCoord.y * cropHeight + (1.0 - cropHeight) / 2.0); // Rescale y coordinate);
                //vec2 scaledTextureCoord = vec2(0.0, 0.0);    
                    
                vec4 colorCrop = texture2D(textureSampler, scaledTextureCoord); 
                gl_FragColor = vec4(colorCrop.rgb, 1.0);
                   
           
                for (int col = -patchSearchRadius; col <= patchSearchRadius; col++) {
                    for (int row = -patchSearchRadius; row <= patchSearchRadius; row++) {
                        //[.250, .750], for 1/1080
                        //[.333, .666], for 1/720
                        vec2 offset = vec2(float(col), float(row)) * texelSize; 
                        vec2 neighborPatchCoord = scaledTextureCoord + offset; 
                        float difference = computePatchDifference(scaledTextureCoord, neighborPatchCoord, texelSize, patchMatchRadius); 
                            
                        float weight = computeNLMWeight(difference, sigma); 
                        totalWeight = totalWeight + weight; 
                        weightedSum = weightedSum + weight * texture2D(textureSampler, neighborPatchCoord);
                        
                        //weightedSum = texture2D(textureSampler, scaledTextureCoord);
                    }
                }
                
                vec4 imageOut = weightedSum / totalWeight; 
                gl_FragColor = vec4(imageOut.rgb, 1.0);


                //vec4 colorCrop = texture2D(textureSampler, texCropCoord); 
                //gl_FragColor = vec4(colorCrop.r, colorCrop.g, colorCrop.b, 1.0); 
                //gl_FragColor = vec4(1.0, 1.0, 0.0, 1.0);   
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
//461w x 518h
        if (SharedDataImage.selectResolution == 3) {
            cropLeft = 0.4400f  //0.250f
            cropRight = 0.5600f  //0.750f
            cropTop = 0.3800f  //0.250f
            cropBottom = 0.6200f  //0.750f
        } else if (SharedDataImage.selectResolution == 2) {
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

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES20.glViewport(0, 0, 1080, 2201)

        val GL_TIME_ELAPSED_EXT = 0x88BF
        val GL_QUERY_RESULT_AVAILABLE = 0x8867
        val GL_QUERY_RESULT = 0x8866

        val queries = IntArray(1)
        GLES31.glGenQueries(1, queries, 0)
        GLES31.glBeginQuery(GL_TIME_ELAPSED_EXT, queries[0])

        val startTime = System.nanoTime()
        
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, vertexCount)

        GLES31.glFinish()
        
        val endTime = System.nanoTime()
        val eTimeMS = (endTime - startTime) / 1_000_000.0
        //Log.d("EXIST-CPU", "Approx. Time taken: $eTimeMS ms")

        GLES31.glEndQuery(GL_TIME_ELAPSED_EXT)

        val available = IntArray(1)
        do {
            GLES31.glGetQueryObjectuiv(queries[0], GL_QUERY_RESULT_AVAILABLE, available, 0)
        } while (available[0] == GLES31.GL_FALSE)

        val elapsedTime = IntArray(1)
        GLES31.glGetQueryObjectuiv(queries[0], GL_QUERY_RESULT, elapsedTime, 0)

        val elapsedTimeMs = elapsedTime[0] / 1_000_000.0
        Log.d("EXIST-GPU","Time taken: $elapsedTimeMs ms")

    }  //draw
}


class MainActivity : ComponentActivity() {
    private lateinit var surfaceTextureG : SurfaceTexture
    private lateinit var surfaceG: Surface

    private lateinit var glView: MyGLSurfaceView
    private lateinit var cameraExecutor: ExecutorService

    lateinit var renderer: MyGLRenderer
    lateinit var detector: FaceDetector

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

