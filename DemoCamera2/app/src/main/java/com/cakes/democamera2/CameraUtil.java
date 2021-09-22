package com.cakes.democamera2;

import android.app.Activity;
import android.hardware.camera2.CameraCharacteristics;
import android.view.Surface;

import com.cakes.democamera2.utils.LogUtil;

public class CameraUtil {

    public static int getCameraDisplayOrientation(Activity activity, CameraCharacteristics characteristics) {
        if (null == characteristics) {
            LogUtil.w("CameraUtil", "getCameraDisplayOrientation() -- error: CameraCharacteristics is null, and will default value:0");
            return 0;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;
        Integer face = characteristics.get(CameraCharacteristics.LENS_FACING);
        Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);  // 相机的方向

        if (face == CameraCharacteristics.LENS_FACING_FRONT) {
//            result = (sensorOrientation + degrees) % 360;
            result = sensorOrientation - 90;
            LogUtil.d("CameraUtil", "getCameraDisplayOrientation() -- result = " + result);
        //    result = (360 - result) % 360;  // compensate the mirror

        } else if (face == CameraCharacteristics.LENS_FACING_BACK) {  // back-facing
            result = (sensorOrientation - degrees + 360) % 360;
        }
        LogUtil.d("CameraUtil", "getCameraDisplayOrientation() -- rotation = " + rotation
                + ", degrees = " + degrees + ", face = " + face + ", sensorOrientation = " + sensorOrientation
                + ", result = " + result);

        return result;
    }

}
