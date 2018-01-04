/**
 * TuSDKFilterEngine
 * PreviewDataActivity.java
 *
 * @author		Yanlin
 * @Date		4:54:37 PM
 * @Copright	(c) 2015 tusdk.com. All rights reserved.
 *
 */
package org.lasque.tusdkfilterenginedemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.lasque.tusdk.api.TuSDKFilterProcessor;
import org.lasque.tusdk.core.struct.TuSdkSize;
import org.lasque.tusdk.core.utils.ContextUtils;
import org.lasque.tusdk.core.utils.TLog;
import org.lasque.tusdk.core.utils.hardware.CameraHelper;

import java.io.IOException;

/**
 * TuSDK 滤镜引擎示例程序
 * 
 * 滤镜引擎接收 PreviewCallback 的数据，处理完毕后，返回修改的数据
 * 
 * @author Yanlin
 *
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN) 
public class PreviewDataActivity extends Activity implements Camera.PreviewCallback, SurfaceHolder.Callback
{
	// 相机对象
	private Camera mCamera;
	
	private int mCameraId;
	
	private SurfaceView mPreviewSurface;
	
	private SurfaceHolder mSurfaceHolder;
	
	private boolean mSurfaceReady;
	
	// 预览画面尺寸
	private Size mPreviewSize;
	
	/** Preview Buffer Length */
	private int mPreviewBufferLength;
	
	// TuSDK 滤镜引擎
	private TuSDKFilterProcessor mFilterProcessor;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preview_data);
		
		mPreviewSurface = (SurfaceView)findViewById(R.id.previewSurface);
		
		mSurfaceHolder = mPreviewSurface.getHolder();  
		// translucent半透明 transparent透明  
		mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
		mSurfaceHolder.addCallback(this);   
	}
	
	@Override
	protected void onPause() 
	{
		super.onPause();

		stopCamera();
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		
		// 是否有权限访问相机
		// Android SDK 23 +，运行时请求权限
		if (hasRequiredPermission())
		{
			startCamera();
		}
		else
		{
			requestRequiredPermissions();
		}
	}
	
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		
		stopCamera();
	}
	
	//--------------------------------- Permission -----------------------------------------
	/**
	 * 授予权限的结果，在对话结束后调用
	 * 
	 * @param permissionGranted
	 *            true or false, 用户是否授予相应权限
	 */
	private void onPermissionGrantedResult(boolean permissionGranted)
	{
		if (permissionGranted)
		{
			startCamera();
		}
	}
	
	private String[] getRequiredPermissions()
	{
		String[] permissions = new String[]{
			Manifest.permission.CAMERA,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.ACCESS_NETWORK_STATE
		};

		return permissions;
	}
	
	private boolean hasRequiredPermission()
	{
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
		
		String[] permissions = getRequiredPermissions();
		
		if (permissions != null && permissions.length > 0)
		{
			for (String key : permissions)
			{
				if (checkSelfPermission(key) != PackageManager.PERMISSION_GRANTED )
				{
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * 请求权限
	 */
	public void requestRequiredPermissions()
	{
		String[] permissions = getRequiredPermissions();
		
		if (permissions != null && permissions.length > 0)
		{
			requestPermissions(permissions, 1);
		}
	}
	
	/**
	 * 处理用户的许可结果
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		
		if (requestCode == 1)
		{
			boolean isOK = true;
			
			for (int value : grantResults)
			{
				if (value != PackageManager.PERMISSION_GRANTED)
				{
					isOK = false;
					break;
				}
			}
			
			isOK = hasRequiredPermission();
			
			onPermissionGrantedResult(isOK);
		}
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) 
	{
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0)
	{
		mSurfaceReady = true;
		
		tryStartPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) 
	{
		mSurfaceReady = false;
	}
	
	//--------------------------------- Camera -----------------------------------------
	
	private void startCamera()
	{
		if (mCamera != null) {
            return;
        }

        CameraInfo info = new CameraInfo();

        // Try to find a front-facing camera
        int numCameras = Camera.getNumberOfCameras();
        int i = 0;
        for (i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                
                mCameraId = i;
                break;
            }
        }
        if (mCamera == null) {
            TLog.d("No front-facing camera found; opening front-back camera");
            for (i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    mCamera = Camera.open(i);
                    
                    mCameraId = i;
                    break;
                }
            }   
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        
        Camera.Parameters params = mCamera.getParameters();
        
        TuSdkSize screenSize = ContextUtils.getScreenSize(getBaseContext());
        // 选择和屏幕比例适配的预览尺寸
        CameraHelper.setPreviewSize(this.getBaseContext(), params, screenSize.maxSide(), 1.0f);
        
        // 设置相机朝向，和 Activity 保持一致
//        setCameraDisplayOrientation(this, mCameraId, mCamera);
        
        // leave the frame rate set to default
        mCamera.setParameters(params);
        
        mPreviewSize = params.getPreviewSize();
        
        int totalSize = mPreviewSize.width * mPreviewSize.height;
		int bitsPerPixel = ImageFormat.getBitsPerPixel(params.getPreviewFormat());
		mPreviewBufferLength = (totalSize * bitsPerPixel) / 8;
        
        addCameraCallbackBuffer();
		
        // 开始相机预览
        mCamera.setPreviewCallbackWithBuffer(this);
        
        // 初始化滤镜引擎
        initProcessor(mPreviewSize);
        
		if (mSurfaceHolder != null)
		{
			tryStartPreview();
		}
	}

	/** add Camera Callback Buffer */
	private void addCameraCallbackBuffer()
	{
		if (mCamera == null) return;
		mCamera.addCallbackBuffer(new byte[mPreviewBufferLength]);
		mCamera.addCallbackBuffer(new byte[mPreviewBufferLength]);
	}
	
	
	private void initProcessor(Size previewSize)
	{
		mFilterProcessor = new TuSDKFilterProcessor();
		
		// 预览尺寸传递给引擎
        mFilterProcessor.init(TuSdkSize.create(previewSize.width, previewSize.height));
        
        // 美颜滤镜
        mFilterProcessor.switchFilter("Fair01");
	}
	
	private void destroyProcessor()
	{
		if (mFilterProcessor == null) return;
		
		mFilterProcessor.destroy();
		mFilterProcessor = null;
	}
	
	/**
	 * 相机默认采集画面为横屏，底部在右。根据 Activity 的朝向，对采集画面进行旋转
	 * 
	 * @param activity
	 * @param cameraId
	 * @param camera
	 */
	public void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera)
	{
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation)
		{
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

		int result;
		if (info.facing == CameraInfo.CAMERA_FACING_FRONT)
		{
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		}
		else
		{ // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		
		camera.setDisplayOrientation(result);
	}
	
	/**
	 * 是否为竖屏
	 * @return
	 */
	private boolean isPortraitMode()
	{
		int rotation = this.getWindowManager().getDefaultDisplay()
				.getRotation();
		boolean ret = false;
		switch (rotation)
		{
		// 竖屏
		case Surface.ROTATION_0:
			ret = true;
			break;
		case Surface.ROTATION_90:
			ret = false;
			break;
		case Surface.ROTATION_180:
			ret = true;
			break;
		case Surface.ROTATION_270:
			ret = false;
			break;
		}
		return ret;
	}
	
	/**
	 * 相机预览前，需要设置渲染容器 或者 SurfaceTexture
	 */
	private void tryStartPreview()
	{
		if (!mSurfaceReady) return;
		
		try {
			
			// 设置预览渲染容器， 注意：SurfaceView 内部渲染的是原始画面
			mCamera.setPreviewDisplay(mSurfaceHolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mCamera.startPreview();
	}
	
	private void stopCamera()
	{
		if (mCamera != null) {
			mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

		destroyProcessor();
	}
	
	//--------------------------------- Preview callback -----------------------------------------
	@Override
	public void onPreviewFrame(byte[] bytes, Camera camera) 
	{
		if (mFilterProcessor != null)
		{
			mFilterProcessor.processData(bytes);
		}
		
		// 处理完毕，请自行处理渲染
	}
}
