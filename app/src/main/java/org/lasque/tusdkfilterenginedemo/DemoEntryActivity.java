package org.lasque.tusdkfilterenginedemo;

/**
 * TuSDKVideoDemo
 * DemoEntryActivity.java
 *
 * @author  XiaShengCui
 * @Date  Jun 1, 2017 7:34:44 PM
 * @Copyright: (c) 2017 tusdk.com. All rights reserved.
 *
 */

import android.content.Intent;
import android.view.View;
import android.widget.RelativeLayout;

import org.lasque.tusdk.core.TuSdk;
import org.lasque.tusdk.core.seles.tusdk.FilterManager;
import org.lasque.tusdk.core.seles.tusdk.FilterManager.FilterManagerDelegate;
import org.lasque.tusdk.core.utils.TLog;
import org.lasque.tusdk.core.utils.hardware.CameraHelper;
import org.lasque.tusdk.impl.activity.TuFragmentActivity;

/**
 * 首页界面
 */
public class DemoEntryActivity extends TuFragmentActivity
{
    /** 布局ID */
    public static final int layoutId = R.layout.activity_demo_entry;

    public DemoEntryActivity()
    {

    }

    /**
     * 初始化控制器
     */
    @Override
    protected void initActivity()
    {
        super.initActivity();
        this.setRootView(layoutId, 0);

        // 设置应用退出信息ID 一旦设置将触发连续点击两次退出应用事件
        this.setAppExitInfoId(R.string.lsq_exit_info);
    }

    /**
     * 初始化视图
     */
    @Override
    protected void initView()
    {
        super.initView();

        // 异步方式初始化滤镜管理器 (注意：如果需要一开启应用马上执行SDK组件，需要做该检测，反之可选)
        // 需要等待滤镜管理器初始化完成，才能使用所有功能
        TuSdk.checkFilterManager(mFilterManagerDelegate);

        RelativeLayout recordLayout = (RelativeLayout) findViewById(R.id.lsq_surface_layout);
        recordLayout.setOnClickListener(mClickListener);

        RelativeLayout componentLayout= (RelativeLayout) findViewById(R.id.lsq_yuv_layout);
        componentLayout.setOnClickListener(mClickListener);
    }

    /**
     * 滤镜管理器委托
     */
    private FilterManagerDelegate mFilterManagerDelegate = new FilterManagerDelegate()
    {
        @Override
        public void onFilterManagerInited(FilterManager manager)
        {
            TLog.d("Filter inited successfully");
        }
    };

    /**
     * 点击事件监听
     */
    private View.OnClickListener mClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.lsq_surface_layout:
                    handleSurfaceTextureSampleButton();
                    break;

                case R.id.lsq_yuv_layout:
                    handleYUVDataSampleButton();
                    break;
            }
        }
    };

    /**
     * 开启录制相机
     */
    private void handleSurfaceTextureSampleButton()
    {
        // 如果不支持摄像头显示警告信息
        if (CameraHelper.showAlertIfNotSupportCamera(this, true)) return;

        Intent intent = new Intent(this, SurfaceTextureActivity.class);
        this.startActivity(intent);
    }

    /**
     * 打开示例列表界面
     */
    private void handleYUVDataSampleButton()
    {
        Intent intent = new Intent(this, PreviewDataActivity.class);
        startActivity(intent);
    }
}
