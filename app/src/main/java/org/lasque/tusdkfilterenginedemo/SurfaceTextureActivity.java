/**
 * TuSDKFilterEngine
 * MainActivity.java
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
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore.Images;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import org.lasque.tusdk.api.TuSDKFilterEngine;
import org.lasque.tusdk.api.TuSDKFilterEngine.TuSDKFilterEngineDelegate;
import org.lasque.tusdk.api.TuSDKMediaRecoder;
import org.lasque.tusdk.core.TuSdk;
import org.lasque.tusdk.core.TuSdkContext;
import org.lasque.tusdk.core.encoder.video.TuSDKVideoEncoderSetting;
import org.lasque.tusdk.core.face.FaceAligment;
import org.lasque.tusdk.core.seles.SelesEGLContextFactory;
import org.lasque.tusdk.core.seles.sources.SelesOutInput;
import org.lasque.tusdk.core.sticker.LiveStickerLoader;
import org.lasque.tusdk.core.struct.TuSdkSize;
import org.lasque.tusdk.core.utils.ContextUtils;
import org.lasque.tusdk.core.utils.FileHelper;
import org.lasque.tusdk.core.utils.RectHelper;
import org.lasque.tusdk.core.utils.StringHelper;
import org.lasque.tusdk.core.utils.TLog;
import org.lasque.tusdk.core.utils.ThreadHelper;
import org.lasque.tusdk.core.utils.TuSdkDate;
import org.lasque.tusdk.core.utils.hardware.CameraConfigs.CameraFacing;
import org.lasque.tusdk.core.utils.hardware.CameraHelper;
import org.lasque.tusdk.core.utils.hardware.InterfaceOrientation;
import org.lasque.tusdk.core.utils.image.BitmapHelper;
import org.lasque.tusdk.core.utils.image.ImageOrientation;
import org.lasque.tusdk.core.utils.sqllite.ImageSqlHelper;
import org.lasque.tusdk.core.utils.sqllite.ImageSqlInfo;
import org.lasque.tusdk.core.view.recyclerview.TuSdkTableView;
import org.lasque.tusdk.modules.view.widget.sticker.StickerGroup;
import org.lasque.tusdk.modules.view.widget.sticker.StickerLocalPackage;
import org.lasque.tusdkfilterenginedemo.gles.GLUtils;
import org.lasque.tusdkfilterenginedemo.gles.Texture2dProgram;
import org.lasque.tusdkfilterenginedemo.views.FilterCellView;
import org.lasque.tusdkfilterenginedemo.views.FilterConfigView;
import org.lasque.tusdkfilterenginedemo.views.FilterListView;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

import static org.lasque.tusdk.core.utils.hardware.CameraHelper.autoFocusModes;

//import static com.mediatek.camera.portability.CameraEx.MTK_CAMERA_MSG_EXT_DATA_RAW16;
//import static com.mediatek.camera.portability.CameraEx.MTK_CAMERA_MSG_EXT_NOTIFY_METADATA_DONE;

/**
 * TuSDK 滤镜引擎示例程序
 *
 * 使用 SurfaceTexture 作采集，滤镜引擎接收原始纹理 ID，返回处理好的纹理 ID，然后外部作渲染或编码
 *
 * @author Yanlin
 *
 */
@SuppressWarnings("deprecation")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class SurfaceTextureActivity extends Activity implements Renderer, SurfaceTexture.OnFrameAvailableListener
{
	/**
	 * 滤镜 filterCode 列表
	 */
	public static String[] VIDEOFILTERS = {"None","SkinNature01","SkinPink04", "Jelly01", "SkinNoir01", "SkinRuddy01",
			"SkinSugar01", "SkinPowder01"};
	// 相机对象
	private Camera mCamera;

	private CameraFacing mCameraFacing;

	// 是否需要启动相机
	private boolean mNeedStartCamera;

	// 相机是否已启动
	private boolean mCameraStarted;

	// 页面是否暂停
	private boolean mCapturePaused;

	// 预览画面尺寸
	private Size mPreviewSize;

	// 存放相机采集画面对应的纹理索引
	private int mOESTextureId;

	private android.opengl.EGLContext mEGLContext;

	// 获取相机采集纹理
	private SurfaceTexture mSurfaceTexture;

	// GLSurfaceView
	private GLSurfaceView mSurfaceView;

	// Capture image
	private ImageView mCaptureImage;

	private ImageView mSwitchImage;

	private ImageView mCloseImage;

	/** 人脸检测框包装视图 */
	private RelativeLayout mFaceRectWrapView;

	// 录制按钮
	private Button mRecordButton;
	// 暂停录制
	private Button mPauseRecodButton;

	// TuSDK 滤镜引擎
	private TuSDKFilterEngine mFilterEngine;

	// GL 渲染脚本程序
	private Texture2dProgram textureProgram;

	// 是否开启了滤镜
	private boolean mFilterActived;

	// 手机设备朝向
	private InterfaceOrientation mCaptureOrientation;

	private TuSdkDate mCaptureStartTime;

	//========== 滤镜

	private ImageView mFilterImageView;
	/** 参数调节视图 */
	protected FilterConfigView mConfigView;
	/** 滤镜栏视图 */
	protected FilterListView mFilterListView;
	/** 滤镜底部栏 */
	private View mFilterBottomView;
	// 用于记录焦点位置
	private int mFocusPostion = 1;

	// 录制 TuSDKMediaRecoder

	/** 音视频录制对象  */
	private TuSDKMediaRecoder mMediaRecorder;

	/** 标记切换摄像时是否需要恢复录制 */
	private boolean mResumeRecord = false;

	private ImageView mRawImageView;
	private ImageView mCapturedImageView;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_surface_texture);

		mSurfaceView = (GLSurfaceView)findViewById(R.id.lsqPreviewSurfaceView);

		if (mSurfaceView == null)
		{
			TLog.e("GLSurface view not exist");
			return;
		}

		// 默认显示后置摄像头
		mCameraFacing = CameraFacing.Front;
		mFilterActived = true;

		// openGL ES 2.0
		mSurfaceView.setEGLContextClientVersion(2);
		mSurfaceView.setEGLContextFactory(new TuSDKEGLContextFactory());
		// 指定自定义渲染器
		mSurfaceView.setRenderer(this);
		mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		// 点击屏幕，开启／关闭美颜
		mSurfaceView.setOnTouchListener(mTouchListener);

		// 拍照按钮
		mCaptureImage = (ImageView)findViewById(R.id.lsqCaptureImage);
		mCaptureImage.setOnClickListener(mClickListener);

		mSwitchImage = (ImageView)findViewById(R.id.lsqToggleButton);
		mSwitchImage.setOnClickListener(mClickListener);

		mCloseImage = (ImageView)findViewById(R.id.lsqCloseButton);
		mCloseImage.setOnClickListener(mClickListener);

		mRecordButton = (Button)findViewById(R.id.lsqRecordbtn);
		mRecordButton.setOnClickListener(mClickListener);

		mPauseRecodButton = (Button)findViewById(R.id.lsqPauseRecordbtn);
		mPauseRecodButton.setOnClickListener(mClickListener);

		mFaceRectWrapView = (RelativeLayout) findViewById(R.id.lsq_imageWrapView);

		mRawImageView = (ImageView) findViewById(R.id.lsq_raw_imageView);
		mCapturedImageView = (ImageView) findViewById(R.id.lsq_catpured_imageView);

		// init FilterEngine
		initTuSDKEngine();

		initFilterViews();
	}

	private void initTuSDKEngine()
	{
		// 初始化滤镜引擎
		mFilterEngine = new TuSDKFilterEngine(this.getBaseContext(), true, true);

		// 设置预览处理完成后是否输出图像的原始朝向 true:输出朝向和输入朝向一致   false：将图像朝向纠正后输出
		mFilterEngine.setOutputOriginalImageOrientation(true);

		// 拍摄的数据是否是原始朝向 默认: true
		// mFilterEngine.setIsOriginalCaptureOrientation(false);

		// 设置前置拍照后输出的照片是否镜像 默认：false
		// mFilterEngine.setIsOutputCaptureMirrorEnabled(false);

		// 如果使用后置摄像头，这里要做对应修改，避免图片朝向出错
		mFilterEngine.setCameraFacing(mCameraFacing);

		// 是否开启人脸检测/动态贴纸 (默认: false)
		mFilterEngine.setEnableLiveSticker(true);

		mFilterEngine.setDelegate(mFilterDelegate);


		// 使用 VideoFair 美颜滤镜
		// 在 assets/TuSDK.bundle/others/lsq_tusdk_configs.json 中，可以看到可用的滤镜代码
		// mFilterEngine.switchFilter("SkinPink020");

		//	List<StickerGroup> smartStickerGroups = StickerLocalPackage.shared().getSmartStickerGroups(true);
		//	mFilterEngine.showGroupSticker(smartStickerGroups.get(0));


	}

	// TuSDKFilterEngine 事件回调
	private TuSDKFilterEngineDelegate mFilterDelegate = new TuSDKFilterEngineDelegate()
	{
		/**
		 * 滤镜更改事件，每次调用 switchFilter 切换滤镜后即触发该事件
		 *
		 * @param filter
		 *            新的滤镜对象
		 */
		@Override
		public void onFilterChanged(SelesOutInput filter)
		{

			// 更新 MediaRecorder 滤镜，应用至录制结果 (务必调用)
			if (mMediaRecorder != null)
				updateMediaRecorderFilter();



//			// 获取滤镜参数列表
//			List<FilterArg> args = filter.getParameter().getArgs();
//
//			for (FilterArg arg : args)
//			{
//		    	/*
//		    	 * 参数为 Key - Value 模式，不同的滤镜，参数数目不一样。
//		    	 *
//		    	 * 常见的参数如下：
//		    	 *
//		    	 * smoothing 润滑
//		    	 * mixed     效果
//		    	 * eyeSize   大眼
//		    	 * chinSize  瘦脸
//		    	 *
//		    	 */
//				if (arg.equalsKey("smoothing"))
//				{
//					// 设置新的参数值，取值范围： 0 ~ 1.0
//					// arg.setPrecentValue(0.9f);
//				}
//			}
//
//			// 提交参数使之生效
//			filter.submitParameter();
			if (getFilterConfigView() != null)
			{
				getFilterConfigView().setSelesFilter(filter);
			}

		}

		/**
		 * 相机拍摄数据处理完毕，返回处理后的结果
		 *
		 * @param imageBuffer
		 *            处理后的 RGBA 数据
		 *
		 * @param imageSize
		 *            图片尺寸
		 */
		public void onPictureDataCompleted(final IntBuffer imageBuffer,final TuSdkSize imageSize)
		{
			if (imageBuffer == null) return;

			// 保存图片操作务必放到子线程，以免影响预览
			ThreadHelper.runThread(new Runnable()
			{
				@Override
				public void run() {

					TLog.d("拍摄处理总耗时: %d ms", mCaptureStartTime.diffOfMillis());

					TuSdkDate date = TuSdkDate.create();

					Bitmap mBitmap = Bitmap.createBitmap(imageSize.width, imageSize.height, Bitmap.Config.ARGB_8888);
					mBitmap.copyPixelsFromBuffer(imageBuffer);

//
//					ThreadHelper.post(new Runnable() {
//						@Override
//						public void run() {
//							mCapturedImageView.setImageBitmap(mBitmap);
//						}
//					});

					long s1 = date.diffOfMillis();

					TLog.d("buffer -> bitmap taken: %s", s1);

					date = TuSdkDate.create();

					// convert bitmap to jpeg data
					byte[] jpegData = BitmapHelper.bitmap2byteArrayTurbo(mBitmap, 100);

					// 保存文件
//					File imageFile = new File(TuSdk.getAppTempPath(), String.format("lsq_%s.jpg", StringHelper.timeStampString()));

					File imageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),String.format("lsq_%s.jpg", StringHelper.timeStampString()));

					FileHelper.saveFile(imageFile, jpegData);

					BitmapHelper.recycled(mBitmap);
					mBitmap = null;


					s1 = date.diffOfMillis();

					TLog.d("save file taken: %s, path: %s", s1, imageFile.getAbsolutePath());

				}
			});

		}

		/**
		 * 预览抓拍完成，返回抓拍结果，运行在主线程。
         *
		 * @param bitmap
		 * 		 	图像数据
		 */
		@Override
		public void onPreviewScreenShot(Bitmap bitmap)
		{
			// 保存文件
			File imageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/test.jpg");

			BitmapHelper.saveBitmap(imageFile,bitmap,100);

		}
	};

	private void updateImageOrientation(ContentValues values)
	{
		// 写入图片朝向
		switch(mCaptureOrientation)
		{
			case Portrait:
				values.put(Images.Media.ORIENTATION, 0);
				break;
			case LandscapeRight:
				values.put(Images.Media.ORIENTATION, 90);
				break;
			case PortraitUpsideDown:
				values.put(Images.Media.ORIENTATION, 180);
				break;
			case LandscapeLeft:
				values.put(Images.Media.ORIENTATION, 270);
				break;

			default:
				values.put(Images.Media.ORIENTATION, 0);
				break;
		}
	}

	//--------------------------------- Permission -----------------------------------------

	private OnTouchListener mTouchListener = new OnTouchListener()
	{
		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			if (MotionEvent.ACTION_DOWN == event.getAction())
			{
				mFilterBottomView.setVisibility(View.GONE);
			}
			return false;
		}
	};

	private OnClickListener mClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View view) {
			if (view.equals(mCaptureImage)) {
				takePicture();
			}
			else if(view.equals(mSwitchImage))
			{
				switchCamera();
			}
			else if(view.equals(mCloseImage))
			{
				closeActivity();

			}else if(view.equals(mRecordButton))
			{
				if (isRecording() || isPaused())
				{
					stopRecording();

				}else
				{
					startRecording();
				}
			}else if(view.equals(mPauseRecodButton))
			{
				if (isPaused())
				{
					startRecording();

				}else if(isRecording())
				{
					pausedRecording();
				}
			}
		}

	};


	int rotation = 0;

	/**
	 * 执行拍照动作
	 */
	private void takePicture()
	{
		if (mCapturePaused) return;

		mCapturePaused = true;

		// 准备拍照，记录当前手机朝向，用来纠正图片朝向
		mCaptureOrientation = mFilterEngine.getDeviceOrient();


//		Camera.Parameters parameters = mCamera.getParameters();
//		parameters.setRotation(rotation);
//		mCamera.setParameters(parameters);

		takePictureAfterFocus();
	}

	private void takePictureAfterFocus()
	{

		mCaptureStartTime = TuSdkDate.create();

		//pauseCamera();
		mCamera.takePicture(null,null, new PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {

				final Bitmap bitmap = BitmapHelper.imageDecode(data, false);

//				ThreadHelper.post(new Runnable() {
//					@Override
//					public void run() {
//						mRawImageView.setImageBitmap(bitmap);
//					}
//				});


				TLog.i("takePicture 耗时 ：" + mCaptureStartTime.diffOfMillis());
				mCaptureStartTime = TuSdkDate.create();

				// 处理 JPEG 数据，处理结果通过回调返回
				mFilterEngine.asyncProcessPictureData(data, mCaptureOrientation);

				// 获取到数后恢复预览
				resumeCamera();

				ThreadHelper.post(new Runnable() {
					@Override
					public void run() {
						mCapturePaused = false;
					}
				});
			}

		});
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		pauseCamera();

	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (mCapturePaused)
		{
			resumeCamera();
			requestRender();
			return;
		}

		// 是否有权限访问相机
		// Android SDK 23 +，运行时请求权限
		if (hasRequiredPermission())
		{
			prepareStartCamera();
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

		// 页面销毁时停止录制
		stopRecording();


		if (mFilterEngine != null)
		{
			mFilterEngine.destroy();
		}
	}

	private void closeActivity()
	{
		this.finish();
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
			prepareStartCamera();
		}
	}

	private String[] getRequiredPermissions()
	{
		String[] permissions = new String[]{
				Manifest.permission.CAMERA,
				Manifest.permission.READ_EXTERNAL_STORAGE,
				Manifest.permission.WRITE_EXTERNAL_STORAGE,
				Manifest.permission.ACCESS_NETWORK_STATE,
				Manifest.permission.RECORD_AUDIO,
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

	//--------------------------------- Camera -----------------------------------------

	private void prepareStartCamera()
	{
		mNeedStartCamera = true;

		requestRender();
	}

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

			int cameraFacing = this.mCameraFacing == CameraFacing.Front? CameraInfo.CAMERA_FACING_FRONT: CameraInfo.CAMERA_FACING_BACK;
			if (info.facing == cameraFacing) {
				mCamera = Camera.open(i);
				break;
			}
		}

		if (mCamera == null) {
			throw new RuntimeException("Unable to open camera");
		}

		Camera.Parameters params = mCamera.getParameters();

		// 设置预览尺寸
		TuSdkSize screenSize = ContextUtils.getScreenSize(getBaseContext());
		// 选择和屏幕比例适配的预览尺寸
		CameraHelper.setPreviewSize(this.getBaseContext(), params, screenSize.maxSide(), 1.0f);

		listPreviewSize(params);

		// 设置拍照尺寸
		setPictureSize(params);

		CameraHelper.setFocusMode(params, CameraHelper.focusModes);

		// leave the frame rate set to default

		mPreviewSize = params.getPreviewSize();

		mCamera.setParameters(params);

		initSurfaceTexture();

		// 开始相机预览
		tryStartPreview();
	}

	private int getSizeRatio(int width, int height)
	{
		int maxSide = Math.max(width, height);
		int minSide = Math.min(width, height);
		return (int) Math.floor((maxSide / (float) minSide) * 100);
	}

	private boolean isSameSize(Size size, TuSdkSize srcSize)
	{
		int maxSide = Math.max(size.width, size.height);
		int minSide = Math.min(size.width, size.height);

		int srcMaxSide = srcSize.maxSide();
		int srcMinSide = srcSize.minSide();

		if (maxSide == srcMaxSide && minSide == srcMinSide)
		{
			return true;
		}

		return false;
	}

	private boolean canSupportAutofocus()
	{
		if (mCamera != null)
		{
			ArrayList<String> autoFocusModes = new ArrayList<String>();

			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR2)
			{
				autoFocusModes.add(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
				autoFocusModes.add(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			}
			autoFocusModes.add(Camera.Parameters.FOCUS_MODE_AUTO);
			autoFocusModes.add(Camera.Parameters.FOCUS_MODE_MACRO);

		}
		return autoFocusModes.contains(mCamera.getParameters().getFocusMode());
	}

	private void listPreviewSize(Camera.Parameters params)
	{
		TuSdkSize screenSize = ContextUtils.getScreenSize(getBaseContext());
		TLog.d("screenSize: %d * %d", screenSize.width, screenSize.height);

		List<Size> list = params.getSupportedPreviewSizes();
		list = CameraHelper.sortSizeList(list);

		for (Size size : list)
		{
			TLog.d("supported previewSize: %d * %d", size.width, size.height);
		}
		
		TLog.d("previewSize: %d * %d", params.getPreviewSize().width, params.getPreviewSize().height);
	}

	private void setPictureSize(Camera.Parameters params)
	{
		List<Size> picSizeList = params.getSupportedPictureSizes();
		picSizeList = CameraHelper.sortSizeList(picSizeList);

        /*
         *  选择一个输出尺寸
         *
         *  注意：如果选择的尺寸过大，设备的 GPU 硬件性能不够的话，滤镜处理过程中可能会崩溃。尺寸越大，滤镜处理时间也越长。
         *
         *  建议使用 TuSdkGPU.getGpuType().getSize() 获取该设备支持的最大边，然后从列表选择一个最合适的尺寸。
         *
         *  具体运行结果以真机测试数据为准
         *
         */
		Size resultSize = picSizeList.get(0);

        /*
        int maxSide = TuSdkGPU.getGpuType().getSize();

        for (Size size : picSizeList)
        {
        	if (size.width < maxSide && size.height < maxSide)
        	{
        		resultSize = size;
        		break;
        	}
        }
        */

		params.setPictureSize(resultSize.width, resultSize.height);

		TLog.d("pictureSize: %d * %d", params.getPictureSize().width, params.getPictureSize().height);
	}

	private void tryStartPreview()
	{
		if (mCameraStarted) return;

		mCapturePaused = false;

		if (mCamera != null && mSurfaceTexture != null)
		{
			try {

				mCamera.setPreviewTexture(mSurfaceTexture);

				mCamera.startPreview();

				mCameraStarted = true;

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void pauseCamera()
	{

		// 暂停相机时停止录制
		stopRecording();

		if (mCamera != null)
		{
			mCamera.stopPreview();
		}

		mCapturePaused = true;
	}

	private void resumeCamera()
	{
		if (mCamera != null)
		{
			mCamera.startPreview();
		}

		mCapturePaused = false;
	}

	private void stopCamera()
	{
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}

		if (mSurfaceTexture != null)
		{
			mSurfaceTexture.release();
			mSurfaceTexture.setOnFrameAvailableListener(null);
			mSurfaceTexture = null;
		}

		mCameraStarted = false;
	}

	private void switchCamera()
	{

		/** 切换摄像头时必须要停止编码器，切换完成后需调用 startVideoDataEncoder */
		if(isRecording())
		{
			// 停止视频编码器
			mMediaRecorder.stopVideoDataEncoder();
			// 标记需要恢复录制
			mResumeRecord = true;
		}

		stopCamera();

		this.mCameraFacing = mCameraFacing == CameraFacing.Front?CameraFacing.Back: CameraFacing.Front;

		mFilterEngine.setCameraFacing(mCameraFacing);

		prepareStartCamera();
	}

	// 请求渲染预览画面，触发 onDrawFrame 方法
	private void requestRender()
	{
		mSurfaceView.requestRender();
	}

	/**
	 * 初始化 SurfaceTexture，必须运行在 GL 线程
	 */
	private void initSurfaceTexture()
	{
		mOESTextureId = GLUtils.createOESTexture();

		mSurfaceTexture = new SurfaceTexture(mOESTextureId);
		mSurfaceTexture.setOnFrameAvailableListener(this);

		if (textureProgram != null)
		{
			textureProgram.release();
		}
		// 初始化渲染脚本程序
		textureProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D);
	}

	//--------------------------------- Renderer -----------------------------------------

	/**
	 * 每次绘制时都会调用。在这里进行滤镜处理，以及绘制预览画面
	 *
	 * 注意：GLSurfaceView Renderer 回调方法运行在 GL 线程
	 */
	@Override
	public void onDrawFrame(GL10 gl)
	{
		if (mNeedStartCamera)
		{
			// 简化逻辑，在 GL 线程启动相机
			startCamera();

			mNeedStartCamera = false;

			// 当相机启动完成后，验证是否需要恢复视频录制
			if(mMediaRecorder != null && mResumeRecord)
			{
				updateMediaRecorderFilter();
				mMediaRecorder.startVideoDataEncoder(mEGLContext, mSurfaceTexture);
			}
		}

		GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		// 更新帧
		mSurfaceTexture.updateTexImage();

		// 滤镜引擎处理，返回的 textureID 为 TEXTURE_2D 类型
		final int textureId = mFilterEngine.processFrame(mOESTextureId, mPreviewSize.width, mPreviewSize.height);

//
//		// 判断是否检测到人脸
//		final FaceAligment[]  faceAligments =  mFilterEngine.getFaceFeatures();
//
//		if (faceAligments != null)
//		{
//			// 设备角度
//			float deviceAngle = mFilterEngine.getDeviceAngle();
//
//            TLog.i("DeviceAngle : " + deviceAngle);
//
//			for (FaceAligment faceAligment : faceAligments)
//			{
//				TLog.i("FaceAngle : " + (deviceAngle - faceAligment.roll));
//			}
//		}
//
//		 TLog.i("faceAligments result : "+(faceAligments != null && faceAligments.length > 0));
//
//		// 对人脸做标点
//		ThreadHelper.post(new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				markFaces(faceAligments);
//			}
//		});

		/**
		 * 当 setOutputOriginalImageOrientation(true) 时，FilterEngine 输出的图片朝向与 Android Y 坐标一致
		 * 渲染时，直接绘制即可。
		 *
		 */
		textureProgram.draw(textureId);


	}

	/**
	 * 对人脸做标点
	 *
	 * @param faces
	 */
	private void markFaces(FaceAligment[] faces)
	{
		if (mFaceRectWrapView != null)
			mFaceRectWrapView.removeAllViews();

		if (faces == null || faces.length == 0) return;

		TuSdkSize imgSize = mFilterEngine.getOutputImageSize();
		Rect midRect =   RectHelper.makeRectWithAspectRatioInsideRect(imgSize, new Rect(0, 0,this.mFaceRectWrapView.getWidth(),this.mFaceRectWrapView.getHeight()));


		// 对齐到图片
		for (FaceAligment faceAligment : faces)
		{
			float left = faceAligment.rect.left * midRect.width() + midRect.left;
			float top =  faceAligment.rect.top * midRect.height() + midRect.top;
			float right = faceAligment.rect.right * midRect.width() + midRect.left;
			float bottom = faceAligment.rect.bottom * midRect.height() + midRect.top;

			RectF faceRect = new RectF(left,top,right,bottom);

			// 这里可以做下优化 重用视图
			View rectView = new View(this);
			rectView.setBackgroundColor(Color.argb(128, 128, 128, 128));

			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) faceRect.width(), (int) faceRect.height());
			params.leftMargin = (int) faceRect.left;
			params.topMargin = (int) faceRect.top;

			mFaceRectWrapView.addView(rectView, params);


			// 标记人脸点
			if (faceAligment.marks != null)
			{

				// 左眼
				float leftEyeX = faceAligment.marks[36].x + ((faceAligment.marks[39].x-faceAligment.marks[36].x)/2);
				PointF leftEyePoint = new PointF(leftEyeX,faceAligment.marks[36].y);

				int x = (int) (leftEyePoint.x * midRect.width() + midRect.left) - 4;
				int y = (int) (leftEyePoint.y * midRect.height() + midRect.top) - 4;

				View markView = buildFaceMarkView(new Point(x, y));

				mFaceRectWrapView.addView(markView);


				// 右眼
				float rightEyeX = faceAligment.marks[42].x + ((faceAligment.marks[45].x-faceAligment.marks[42].x)/2);
				PointF rightEyePoint = new PointF(rightEyeX,faceAligment.marks[42].y);

				x = (int) (rightEyePoint.x * midRect.width() + midRect.left) - 4;
				y = (int) (rightEyePoint.y * midRect.height() + midRect.top) - 4;

				markView = buildFaceMarkView(new Point(x, y));

				mFaceRectWrapView.addView(markView);


				for (int i = 0 ; i < faceAligment.marks.length; i++)
				{
					if (i == 33 || // 鼻子
						i == 59 || // 嘴巴左
					    i == 55    // 嘴巴右
							)
					{
						PointF point = faceAligment.marks[i];

						 x = (int) (point.x * midRect.width() + midRect.left) - 4;
						 y = (int) (point.y * midRect.height() + midRect.top) - 4;

						markView = buildFaceMarkView(new Point(x, y));

						mFaceRectWrapView.addView(markView);
					}
				}
			}


		}
	}

	private View buildFaceMarkView(Point point)
	{
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(8, 8);
		params.leftMargin = point.x;
		params.topMargin = point.y;

		View mark = new View(this);
		mark.setBackgroundColor(Color.argb(255, 0, 255, 0));
		mark.setLayoutParams(params);

		return mark;
	}

	/**
	 * 注意：GLSurfaceView Renderer 回调方法运行在 GL 线程
	 */
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		if (mFilterEngine != null)
			mFilterEngine.onSurfaceChanged(width, height);
	}

	/**
	 * 注意：GLSurfaceView Renderer 回调方法运行在 GL 线程
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig eglConfig)
	{
		mEGLContext = EGL14.eglGetCurrentContext();

		if (mFilterEngine != null)
		{
			mFilterEngine.onSurfaceCreated();
		}
	}

	//-------------------- OnFrameAvailableListener ----------

	/**
	 * 相机采集到新的帧数据。
	 *
	 * 注：此回调方法运行在主线程或其他分线程，不能在这里执行绘制动作
	 */
	public void onFrameAvailable(SurfaceTexture surfaceTexture)
	{
		// 请求刷新页面, GLSurfaceView.onDrawFrame 将被调用
		requestRender();
	}

	//
	/**
	 * 初始化滤镜栏视图
	 */
	private void initFilterViews()
	{
		getFilterListView();

		mFilterBottomView = findViewById(R.id.lsq_filter_group_bottom_view);

		mFilterImageView = (ImageView) findViewById(R.id.lsq_smart_beauty_btn);
		mFilterImageView.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mFilterBottomView.setVisibility(View.VISIBLE);
			}
		});

		this.mFilterListView.setModeList(Arrays.asList(VIDEOFILTERS));
		mFilterListView.reloadData();

	}

	/**
	 * 滤镜栏视图
	 *
	 * @return
	 */
	private FilterListView getFilterListView()
	{
		if (mFilterListView == null)
		{
			mFilterListView = (FilterListView) findViewById(R.id.lsq_filter_list_view);
			mFilterListView.loadView();
			mFilterListView.setCellLayoutId(R.layout.filter_list_cell_view);
			mFilterListView.setCellWidth(TuSdkContext.dip2px(80));
			mFilterListView.setItemClickDelegate(mFilterTableItemClickDelegate);
			mFilterListView.reloadData();
			mFilterListView.selectPosition(mFocusPostion);
		}

		return mFilterListView;
	}

	/**
	 * 滤镜配置视图
	 *
	 * @return
	 */
	private FilterConfigView getFilterConfigView()
	{
		if (mConfigView == null)
		{
			mConfigView = (FilterConfigView) findViewById(R.id.lsq_filter_config_view);
		}

		return mConfigView;
	}

	/** 滤镜组列表点击事件 */
	private TuSdkTableView.TuSdkTableViewItemClickDelegate<String, FilterCellView> mFilterTableItemClickDelegate = new TuSdkTableView.TuSdkTableViewItemClickDelegate<String, FilterCellView>()
	{
		@Override
		public void onTableViewItemClick(String itemData, FilterCellView itemView, int position) {
			onFilterGroupSelected(itemData, itemView, position);
		}
	};

	/**
	 * 滤镜组选择事件
	 *
	 * @param itemData
	 * @param itemView
	 * @param position
	 */
	protected void onFilterGroupSelected(String itemData, FilterCellView itemView, int position)
	{
		FilterCellView prevCellView = (FilterCellView) mFilterListView.findViewWithTag(mFocusPostion);
		mFocusPostion = position;
		changeVideoFilterCode(itemData);
		mFilterListView.selectPosition(mFocusPostion);
		deSelectLastFilter(prevCellView);
		selectFilter(itemView, position);
		getFilterConfigView().setVisibility((position == 0) ? View.INVISIBLE : View.VISIBLE);
	}

	/**
	 * 滤镜选中状态
	 *
	 * @param itemView
	 * @param position
	 */
	private void selectFilter(FilterCellView itemView, int position)
	{
		updateFilterBorderView(itemView, false);
		itemView.setFlag(position);

		TextView titleView = itemView.getTitleView();
		titleView.setBackground(TuSdkContext.getDrawable("tusdk_view_filter_selected_text_roundcorner"));
	}

	/**
	 * 设置滤镜单元边框是否可见
	 *
	 * @param lastFilter
	 * @param isHidden
	 */
	private void updateFilterBorderView(FilterCellView lastFilter, boolean isHidden)
	{
		RelativeLayout filterBorderView = lastFilter.getBorderView();
		filterBorderView.setVisibility(isHidden ? View.GONE : View.VISIBLE);
	}

	/**
	 * 切换滤镜
	 *
	 * @param code
	 */
	protected void changeVideoFilterCode(final String code)
	{

		if (mFilterEngine == null)
			return;

		mFilterEngine.switchFilter(code);
	}

	/**
	 * 取消上一个滤镜的选中状态
	 *
	 * @param lastFilter
	 */
	private void deSelectLastFilter(FilterCellView lastFilter)
	{
		if (lastFilter == null)
			return;

		updateFilterBorderView(lastFilter, true);
		lastFilter.getTitleView()
				.setBackground(TuSdkContext.getDrawable("tusdk_view_filter_unselected_text_roundcorner"));
		lastFilter.getImageView().invalidate();
	}

	private class TuSDKEGLContextFactory extends SelesEGLContextFactory
	{
		public TuSDKEGLContextFactory()
		{
			super(2);
		}

		@Override
		public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context)
		{
			TLog.d("egl factory destroy: %s", Thread.currentThread().getName());

			if (mFilterEngine != null)
			{
				mFilterEngine.onSurfaceDestroy();
			}

			super.destroyContext(egl, display, context);
		}
	}

	//========================  TuSDKMediaRecoder 视频录制对象 ======================== //

	/**
	 * 获取 TuSDKMediaRecoder 视频录制对象
	 *
	 * @return TuSDKMediaRecoder
	 */
	private TuSDKMediaRecoder getMediaRecorder()
	{
		if(mMediaRecorder == null)
		{
			mMediaRecorder = new TuSDKMediaRecoder();

			TuSDKVideoEncoderSetting defaultRecordSetting = TuSDKVideoEncoderSetting.getDefaultRecordSetting();

			// 自定义视频宽高
			// defaultRecordSetting.videoSize = TuSdkSize.create(320,480);

			mMediaRecorder.setVideoEncoderSetting(defaultRecordSetting)
					.setOutputFilePath(new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/test111.mp4")); // 自定义输出路径

			// 设置 TuSDKMediaRecoder 委托事件
			mMediaRecorder.setDelegate(mMediaRecoderDelegate);

		}

		return mMediaRecorder;
	}

	/**
	 * TuSDKMediaRecoder 委托事件，可监听 TuSDKMediaRecoder 状态及进度信息。
	 */
	private TuSDKMediaRecoder.TuSDKMediaRecoderDelegate mMediaRecoderDelegate = new TuSDKMediaRecoder.TuSDKMediaRecoderDelegate()
	{
		/**
		 * TuSDKMediaRecoder 进度改变事件
		 *
		 * @param durationTime 当前录制持续时间 单位：/s
		 */
		@Override
		public void onMediaRecoderProgressChanged(float durationTime)
		{
			TLog.i("TuSDKMediaRecoder durationTime : "+durationTime);

		}

		/**
		 * TuSDKMediaRecoder 状态改变事件
		 *
		 * @param state TuSDKMediaRecoder 当前状态信息
		 */
		@Override
		public void onMediaRecoderStateChanged(TuSDKMediaRecoder.State state)
		{
			TLog.i("TuSDKMediaRecoder State : "+state);

			// 监听录制完成事件
			if(state == TuSDKMediaRecoder.State.RecordCompleted)
			{
				// 将视频信息保存到相册
				ImageSqlInfo videoSqlInfo = ImageSqlHelper.saveMp4ToAlbum(SurfaceTextureActivity.this,mMediaRecorder.getOutputFilePath());
				ImageSqlHelper.notifyRefreshAblum(SurfaceTextureActivity.this,videoSqlInfo);
			}
		}
	};

	/**
	 * 开始录制视频，录制过程中可调用 stopRecording 完成录制， pausedRecording 实现暂停录制。
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public void startRecording()
	{

		if (mEGLContext == null) return;

		// 准备录制应用滤镜效果
		updateMediaRecorderFilter();

		// 启动录制
		getMediaRecorder().startRecording(mEGLContext,mSurfaceTexture);

		mCaptureImage.setEnabled(false);

		mRecordButton.setText(R.string.lsq_record_stop);
		mPauseRecodButton.setText(R.string.lsq_record_pause);
	}

	/**
	 * 停止录制视频，视频将被保存。
	 */
	public void stopRecording()
	{
		if(mMediaRecorder != null)
			mMediaRecorder.stopRecording();

		mCaptureImage.setEnabled(true);
		mRecordButton.setText(R.string.lsq_record_start);

	}

	/**
	 * 暂停录制，调用 startRecording 恢复录制
	 */
	public void pausedRecording()
	{
		if(mMediaRecorder != null)
			mMediaRecorder.pauseRecording();

		mPauseRecodButton.setText(R.string.lsq_record_restart);

	}

	/**
	 * 是否正在录制中
	 *
	 * @return true：正在录制
	 */
	public boolean isRecording()
	{
		return mMediaRecorder != null && mMediaRecorder.isRecording();
	}

	/**
	 * 是否已暂停
	 *
	 * @return true : 当前已暂停录制
	 */
	public boolean isPaused()
	{
		return mMediaRecorder != null && mMediaRecorder.isPaused();
	}

	/**
	 * 更新 MediaRecorder 滤镜，应用至录制结果
	 */
	private void updateMediaRecorderFilter()
	{
		if (getMediaRecorder() == null || mFilterEngine == null) return;

		// 更新 MediaRecorder 滤镜，应用至录制结果
		getMediaRecorder().updateFilter(mFilterEngine.getFilterWrap().getFilter());
	}

}
