package com.mylhyl.zxing.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.mylhyl.zxing.scanner.camera.open.CameraFacing;
import com.mylhyl.zxing.scanner.common.Scanner;

/**
 * Created by hupei on 2016/7/1.
 */
public class ScannerView extends RelativeLayout {

    private static final String TAG = ScannerView.class.getSimpleName();

    private CameraSurfaceView mSurfaceView;
    private ViewfinderView mViewfinderView;

    private BeepManager mBeepManager;
    private OnScannerCompletionListener mScannerCompletionListener;

    private ScannerOptions mScannerOptions;
    private ScannerOptions.Builder mScannerOptionsBuilder;

    public ScannerView(Context context) {
        this(context, null);
    }

    public ScannerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScannerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(), scaleFactor * a.getY(), scaleFactor * b.getX(),
                    scaleFactor * b.getY(), paint);
        }
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        mSurfaceView = new CameraSurfaceView(context, this);
        mSurfaceView.setId(android.R.id.list);
        addView(mSurfaceView);

        mViewfinderView = new ViewfinderView(context, attrs);
        LayoutParams layoutParams = new LayoutParams(context, attrs);
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, mSurfaceView.getId());
        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, mSurfaceView.getId());
        addView(mViewfinderView, layoutParams);

        mScannerOptionsBuilder = new ScannerOptions.Builder();
        mScannerOptions = mScannerOptionsBuilder.build();
    }

    public void onResume() {
        mSurfaceView.onResume(mScannerOptions);
        mViewfinderView.setCameraManager(mSurfaceView.getCameraManager());
        mViewfinderView.setScannerOptions(mScannerOptions);
        mViewfinderView.setVisibility(mScannerOptions.isViewfinderHide() ? View.GONE : View.VISIBLE);
        if (mBeepManager != null) mBeepManager.updatePrefs();
    }

    public void onPause() {
        mSurfaceView.onPause();
        if (mBeepManager != null) mBeepManager.close();
        mViewfinderView.laserLineBitmapRecycle();
    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        //????????????
        if (mScannerCompletionListener != null) {
            //????????????
            mScannerCompletionListener.onScannerCompletion(rawResult, Scanner.parseResult(rawResult), barcode);
        }
        if (mScannerOptions.getMediaResId() != 0) {
            if (mBeepManager == null) {
                mBeepManager = new BeepManager(getContext());
                mBeepManager.setMediaResId(mScannerOptions.getMediaResId());
            }
            mBeepManager.playBeepSoundAndVibrate();
        }

        if (barcode != null && mScannerOptions.isShowQrThumbnail()) {
            mViewfinderView.drawResultBitmap(barcode);
            drawResultPoints(barcode, scaleFactor, rawResult);
        }
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of
     * the barcode.
     *
     * @param barcode     A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult   The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(Scanner.color.RESULT_POINTS);
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4
                    && (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A || rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and
                // metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    /**
     * ???????????????????????????
     *
     * @param listener
     * @return
     */
    public ScannerView setOnScannerCompletionListener(OnScannerCompletionListener listener) {
        this.mScannerCompletionListener = listener;
        return this;
    }

    public void setScannerOptions(ScannerOptions scannerOptions) {
        this.mScannerOptions = scannerOptions;
    }

    /**
     * ???????????????
     *
     * @param mode true??????false???
     */
    public ScannerView toggleLight(boolean mode) {
        mSurfaceView.setTorch(mode);
        return this;
    }

    /**
     * ??????????????????????????????????????????????????????????????? ??????????????????????????????????????????????????????????????????
     *
     * @param delayMS ??????
     */
    public void restartPreviewAfterDelay(long delayMS) {
        mSurfaceView.restartPreviewAfterDelay(delayMS);
    }

    /**
     * ?????????????????????
     *
     * @param color
     */
    @Deprecated
    public ScannerView setLaserColor(int color) {
        mScannerOptionsBuilder.setLaserLine(ScannerOptions.LaserStyle.COLOR_LINE, color);
        return this;
    }

    /**
     * ???????????????????????????
     *
     * @param resId resId
     */
    @Deprecated
    public ScannerView setLaserLineResId(int resId) {
        mScannerOptionsBuilder.setLaserLine(ScannerOptions.LaserStyle.RES_LINE, resId);
        return this;
    }

    /**
     * ???????????????????????????
     *
     * @param resId resId
     */
    @Deprecated
    public ScannerView setLaserGridLineResId(int resId) {
        mScannerOptionsBuilder.setLaserLine(ScannerOptions.LaserStyle.RES_GRID, resId);
        return this;
    }

    /**
     * ?????????????????????
     *
     * @param height dp
     */
    @Deprecated
    public ScannerView setLaserLineHeight(int height) {
        mScannerOptionsBuilder.setLaserLineHeight(height);
        return this;
    }

    /**
     * ???????????????4?????????
     *
     * @param color
     */
    @Deprecated
    public ScannerView setLaserFrameBoundColor(int color) {
        mScannerOptionsBuilder.setFrameCornerColor(color);
        return this;
    }

    /**
     * ???????????????4?????????
     *
     * @param length dp
     */
    @Deprecated
    public ScannerView setLaserFrameCornerLength(int length) {
        mScannerOptionsBuilder.setFrameCornerLength(length);
        return this;
    }

    /**
     * ???????????????4?????????
     *
     * @param width dp
     */
    @Deprecated
    public ScannerView setLaserFrameCornerWidth(int width) {
        mScannerOptionsBuilder.setFrameCornerWidth(width);
        return this;
    }

    /**
     * ??????????????????
     *
     * @param color ????????????
     */
    @Deprecated
    public ScannerView setDrawTextColor(int color) {
        mScannerOptionsBuilder.setTipTextColor(color);
        return this;
    }

    /**
     * ??????????????????
     *
     * @param size ???????????? sp
     */
    @Deprecated
    public ScannerView setDrawTextSize(int size) {
        mScannerOptionsBuilder.setTipTextSize(size);
        return this;
    }

    /**
     * ????????????
     *
     * @param text
     * @param bottom ????????????????????????
     */
    @Deprecated
    public ScannerView setDrawText(String text, boolean bottom) {
        mScannerOptionsBuilder.setTipText(text);
        mScannerOptionsBuilder.setTipTextToFrameTop(!bottom);
        return this;
    }

    /**
     * ????????????
     *
     * @param text
     * @param bottom ????????????????????????
     * @param margin ?????????????????? dp
     */
    @Deprecated
    public ScannerView setDrawText(String text, boolean bottom, int margin) {
        mScannerOptionsBuilder.setTipText(text);
        mScannerOptionsBuilder.setTipTextToFrameTop(!bottom);
        mScannerOptionsBuilder.setTipTextToFrameMargin(margin);
        return this;
    }

    /**
     * ????????????
     *
     * @param text
     * @param size   ???????????? sp
     * @param color  ????????????
     * @param bottom ????????????????????????
     * @param margin ?????????????????? dp
     */
    @Deprecated
    public ScannerView setDrawText(String text, int size, int color, boolean bottom, int margin) {
        mScannerOptionsBuilder.setTipText(text);
        mScannerOptionsBuilder.setTipTextSize(size);
        mScannerOptionsBuilder.setTipTextColor(color);
        mScannerOptionsBuilder.setTipTextToFrameTop(!bottom);
        mScannerOptionsBuilder.setTipTextToFrameMargin(margin);
        return this;
    }

    /**
     * ??????????????????????????????
     *
     * @param resId
     */
    @Deprecated
    public ScannerView setMediaResId(int resId) {
        mScannerOptionsBuilder.setMediaResId(resId);
        return this;
    }

    /**
     * ?????????????????????
     *
     * @param width  dp
     * @param height dp
     */
    @Deprecated
    public ScannerView setLaserFrameSize(int width, int height) {
        mScannerOptionsBuilder.setFrameSize(width, height);
        return this;
    }

    /**
     * ??????????????????????????????
     *
     * @param margin
     */
    @Deprecated
    public ScannerView setLaserFrameTopMargin(int margin) {
        mScannerOptionsBuilder.setFrameTopMargin(margin);
        return this;
    }

    /**
     * ??????????????????????????????????????????????????????????????????
     *
     * @param scanMode {@linkplain Scanner.ScanMode mode}
     * @return
     */
    @Deprecated
    public ScannerView setScanMode(String scanMode) {
        mScannerOptionsBuilder.setScanMode(scanMode);
        return this;
    }

    /**
     * ????????????????????????
     *
     * @param barcodeFormat
     * @return
     */
    @Deprecated
    public ScannerView setScanMode(BarcodeFormat... barcodeFormat) {
        mScannerOptionsBuilder.setScanMode(barcodeFormat);
        return this;
    }

    /**
     * ?????????????????????????????????
     *
     * @param showResThumbnail
     * @return
     */
    @Deprecated
    public ScannerView isShowResThumbnail(boolean showResThumbnail) {
        mScannerOptionsBuilder.setCreateQrThumbnail(showResThumbnail);
        return this;
    }

    /**
     * ???????????????????????????????????????????????? moveSpeed ??????
     *
     * @param moveSpeed px
     * @return
     */
    @Deprecated
    public ScannerView setLaserMoveSpeed(int moveSpeed) {
        mScannerOptionsBuilder.setLaserMoveSpeed(moveSpeed);
        return this;
    }

    /**
     * ????????????????????????????????????
     *
     * @param cameraFacing
     * @return
     */
    @Deprecated
    public ScannerView setCameraFacing(CameraFacing cameraFacing) {
        mScannerOptionsBuilder.setCameraFacing(cameraFacing);
        return this;
    }

    /**
     * ??????????????????
     *
     * @param scanFullScreen
     * @return
     */
    @Deprecated
    public ScannerView isScanFullScreen(boolean scanFullScreen) {
        mScannerOptionsBuilder.setScanFullScreen(scanFullScreen);
        return this;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param hide
     * @return
     */
    @Deprecated
    public ScannerView isHideLaserFrame(boolean hide) {
        mScannerOptionsBuilder.setViewfinderHide(hide);
        return this;
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param invertScan
     * @return
     */
    @Deprecated
    public ScannerView isScanInvert(boolean invertScan) {
        mScannerOptionsBuilder.setScanInvert(invertScan);
        return this;
    }

    void drawViewfinder() {
        mViewfinderView.drawViewfinder();
    }
}
