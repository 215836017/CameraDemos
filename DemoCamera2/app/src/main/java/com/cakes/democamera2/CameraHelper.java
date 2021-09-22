package com.cakes.democamera2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.cakes.democamera2.utils.LogUtil;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@SuppressLint("MissingPermission")  // 所以，测试时先手动在系统设置中开启权限
public class CameraHelper {

    private final String TAG = "CameraHelper";

    private Activity context;
    private TextureView textureView;
    private Point previewSize;

    private CameraManager cameraManager;
    private boolean isBackCamera;

    private String currCameraId;
    private CameraCharacteristics currCamCharacteristics;

    private HandlerThread cameraHandlerThread;
    private Handler cameraHandler;

    private CameraDevice currCameraDevice;
    private Size supportedSize;

    private CameraCaptureSession cameraCaptureSession;

    public CameraHelper(Activity context, TextureView textureView, int previewWidth, int previewHeight) {
        this.context = context;
        this.textureView = textureView;
        previewSize = new Point(previewWidth, previewHeight);

        isBackCamera = true;
    }

    public void useCamera() {

        // 第一步：找到相机的ID(前置、后置、或其他)
        setupCamera();

        // 第二步：尝试打开相机，打开前先创建一个子线程，使其打开、预览相机等的其他操作都在子线程中进行
        cameraHandlerThread = new HandlerThread("camera2");
        cameraHandlerThread.start();
        cameraHandler = new Handler(cameraHandlerThread.getLooper());

        openCamera(); // 打开相机

        // 第三步：打开相机的结果是通过回调返回的， 打开成功后就可以进行预览等操作了。

    }

    private void setupCamera() {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            if (null != cameraIdList) {
                CameraCharacteristics cameraCharacteristics;
                for (String id : cameraIdList) {
                    cameraCharacteristics = cameraManager.getCameraCharacteristics(id);
                    if (null != cameraCharacteristics) {
                        Integer integer = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                        if (isBackCamera && integer == CameraCharacteristics.LENS_FACING_BACK) {
                            // 后置相机
                            currCamCharacteristics = cameraCharacteristics;
                            currCameraId = id;
                            break;

                        } else if (!isBackCamera && integer == CameraCharacteristics.LENS_FACING_FRONT) {
                            // 前置相机
                            currCamCharacteristics = cameraCharacteristics;
                            currCameraId = id;
                            break;
                        }
                    }
                }

                // ---------- test -----------
                LogUtil.d(TAG, "cameraIdList.length = " + cameraIdList.length);
                LogUtil.d(TAG, "currCameraId = " + currCameraId);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {

        if (TextUtils.isEmpty(currCameraId)) {
            LogUtil.w(TAG, "没有找到相机的ID，无法进行打开相机操作！");
            return;
        }

        LogUtil.d(TAG, "openCamera() -- try to open camera...");
        try {
            cameraManager.openCamera(currCameraId, cameraDeviceStateCallback, cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        LogUtil.d(TAG, "openCamera() -- end...");
    }

    CameraDevice.StateCallback cameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            LogUtil.i(TAG, "onOpened() -- 11111");
            currCameraDevice = camera;

            LogUtil.d(TAG, "相机打开成功了，可以进行预览等操作了...");

            // 创建预览会话
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            LogUtil.i(TAG, "onDisconnected() -- 11111 ");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            LogUtil.i(TAG, "onError() -- 11111111 ");

        }
    };

    private ImageReader imageReader;

    private void createCameraPreviewSession() {
        // 获取合适的尺寸
        supportedSize = getSupportedSize();
        LogUtil.d(TAG, "createCameraPreviewSession() -- supportedSize: width = " + supportedSize.getWidth()
                + ", height = " + supportedSize.getHeight());

        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(supportedSize.getWidth(), supportedSize.getHeight());
        Surface surface = new Surface(surfaceTexture);

        // 用于拍照
        imageReader = ImageReader.newInstance(supportedSize.getWidth(), supportedSize.getHeight(), ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(onImageAvailableListener, cameraHandler);

        try {
            // 创建请求体
            CaptureRequest.Builder captureRequest = currCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            captureRequest.addTarget(surface);  // 添加用户预览的Surface

            // 使用CameraDevice创建会话通道，然后通过回调返回创建会话的结果
            currCameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    LogUtil.d(TAG, "currCameraDevice.createCaptureSession -- onConfigured");
                    if (null == currCameraDevice) {
                        // the camera is already closed
                        LogUtil.d(TAG, "onConfigured() -- currCameraDevice is null");
                        return;
                    }

                    cameraCaptureSession = session;
                    // 使用会话通道把配置好的请求体发送出去
                    try {
                        session.setRepeatingRequest(captureRequest.build(), new CameraCaptureSession.CaptureCallback() {
                        }, cameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        private Image imagePhoto;
        private ByteBuffer buffer;
        private byte[] bytes;

        private FileWriter fileWriter;
        private FileOutputStream fos;

        @Override
        public void onImageAvailable(ImageReader reader) {
            imagePhoto = reader.acquireNextImage();

            buffer = imagePhoto.getPlanes()[0].getBuffer();
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            imagePhoto.close();
            reader.close();

            savePhoto(bytes);
        }

        private void savePhoto(byte[] data) {

            if (null == fos) {
                try {
                    fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/camera2_photo" + System.currentTimeMillis() + ".jpg", true);

                    fos.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (null != fos) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        fos = null;
                    }
                }
            }
        }
    };

    //Camera2拍照也是通过ImageReader来实现的, 首先先做些准备工作，设置拍照参数，如方向、尺寸等
    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    /**
     * 拍照
     */
    public void takePhoto() {
        try {
            CaptureRequest.Builder captureRequest = currCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequest.addTarget(imageReader.getSurface());

            int cameraDisplayOrientation = CameraUtil.getCameraDisplayOrientation(context, currCamCharacteristics);
            int rotation = context.getWindowManager().getDefaultDisplay().getRotation();
            captureRequest.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
//            captureRequest.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(cameraDisplayOrientation));

            if (null != cameraCaptureSession) {
                cameraCaptureSession.capture(captureRequest.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber);
                        LogUtil.d(TAG, "onCaptureStarted() -- 1111111");
                    }

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        LogUtil.d(TAG, "onCaptureCompleted() -- 1111111");
                    }
                }, cameraHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getSupportedSize() {
        if (null == currCamCharacteristics) {
            LogUtil.w(TAG, "can not get size for CamCharacteristics is null");
            return null;
        }

        StreamConfigurationMap map = currCamCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        List<Size> sizes = new ArrayList<Size>(Arrays.asList(map.getOutputSizes(SurfaceTexture.class)));
        Size defSize = sizes.get(0);

        Point maxPreviewSize = new Point(1920, 1080);
        Point minPreviewSize = new Point(1280, 720);

        Size[] tempSizes = sizes.toArray(new Size[0]);
        Arrays.sort(tempSizes, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                if (o1.getWidth() > o2.getWidth()) {
                    return -1;
                } else if (o1.getWidth() == o2.getWidth()) {
                    return o1.getHeight() > o2.getHeight() ? -1 : 1;
                } else {
                    return 1;
                }
            }
        });
        sizes = new ArrayList<>(Arrays.asList(tempSizes));

        for (int i = sizes.size() - 1; i > 0; i--) {
            if (null != maxPreviewSize) {
                if (sizes.get(i).getWidth() > maxPreviewSize.x || sizes.get(i).getHeight() > maxPreviewSize.y) {
                    sizes.remove(i);
                    continue;
                }
            }

            if (null != minPreviewSize) {
                if (sizes.get(i).getWidth() < minPreviewSize.x || sizes.get(i).getHeight() < minPreviewSize.y) {
                    sizes.remove(i);
                }
            }
        }
        if (sizes.size() == 0) {
            return defSize;
        }

        Size bestSize = sizes.get(0);
        float previewRatio;
        if (previewSize != null) {
            previewRatio = (float) previewSize.x / (float) previewSize.y;
        } else {
            previewRatio = (float) bestSize.getWidth() / (float) bestSize.getHeight();
        }

        if (previewRatio > 1) {
            previewRatio = 1 / previewRatio;
        }

        for (Size s : sizes) {
            float f1 = Math.abs((s.getHeight() / (float) s.getWidth()) - previewRatio);
            float f2 = Math.abs(bestSize.getHeight() / (float) bestSize.getWidth() - previewRatio);

            if (f1 < f2) {
                bestSize = s;
            }
        }

        return bestSize;
    }
}
