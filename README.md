This repository contains Visual Studio projects for 3 noise reduction algorithms.  

xxxVNLMNRGL is the non local means algorithm, implmented using a compute shader.
xxxVNR2cGL is the "vanilla" non local means algorithm, implemented using a fragment shader.
xxxVNRGL is the diffusion algorithm.
xxxVTemporalNRGL is the temporal algorithm.  

There are 3 main classes:
MainActivity
MyGLRenderer
Triangle

The camX video stream is consumed by MainActivity.

The surface where the video frames reside, is updated in the MyGLRenderer class.

The draw method in the Triangle class, executes the GLSL algorithm.  

GPU pipeline buffers are defined in the onSurfaceChanged method in MyGLRenderer.

The GLSL source code resides in the init function of the Triangle class. 

