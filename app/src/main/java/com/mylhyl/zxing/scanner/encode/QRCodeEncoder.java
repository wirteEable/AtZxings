/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mylhyl.zxing.scanner.encode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Display;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * This class does the work of decoding the user's request and extracting all
 * the data to be encoded in a barcode.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class QRCodeEncoder {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private Context context;
    private QREncode.Builder encodeBuild;

    private static Bitmap getRoundedBitmap(Bitmap bitmap, float roundPx, int color) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private static Bitmap getCircleBitmap(Bitmap bitmap, int color) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    QRCodeEncoder(QREncode.Builder build, Context context) {
        this.context = context;
        this.encodeBuild = build;
        if (encodeBuild.getColor() == 0) encodeBuild.setColor(BLACK);

        // This assumes the view is full screen, which is a good assumption
        if (encodeBuild.getSize() == 0) {
            int smallerDimension = getSmallerDimension(context.getApplicationContext());
            encodeBuild.setSize(smallerDimension);
        }
        encodeContentsFromZXing(build);
    }

    private static List<String> getAllBundleValues(Bundle bundle, String[] keys) {
        List<String> values = new ArrayList<>(keys.length);
        for (String key : keys) {
            Object value = bundle.get(key);
            values.add(value == null ? null : value.toString());
        }
        return values;
    }

    private static int getSmallerDimension(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        int width = displaySize.x;
        int height = displaySize.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 7 / 8;
        return smallerDimension;
    }

    private static Bitmap addBackground(Bitmap qrBitmap, Bitmap background) {
        int bgWidth = background.getWidth();
        int bgHeight = background.getHeight();
        int fgWidth = qrBitmap.getWidth();
        int fgHeight = qrBitmap.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(bgWidth, bgHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(background, 0, 0, null);
        if (!background.isRecycled()) {
            background.recycle();
        }
        //???????????????????????????
        float left = (bgWidth - fgWidth) / 2;
        float top = (bgHeight - fgHeight) / 2;
        canvas.drawBitmap(qrBitmap, left, top, null);
        if (!qrBitmap.isRecycled()) {
            qrBitmap.recycle();
        }
        canvas.save();
        canvas.restore();
        return bitmap;
    }

    private Bitmap addLogo(Bitmap src) {
        Bitmap srcLogo = encodeBuild.getLogoBitmap();
        int logoSize = encodeBuild.getLogoSize();

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = srcLogo.getWidth();
        int logoHeight = srcLogo.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(src, 0, 0, null);

        Matrix matrix = new Matrix();
        float scaleWidth = srcWidth * 1.0f / logoWidth / 5;
        float scaleHeight = srcHeight * 1.0f / logoHeight / 5;
        float left = (srcWidth - (logoWidth * scaleWidth)) / 2;
        float top = (srcHeight - (logoHeight * scaleHeight)) / 2;

        if (logoSize > 0) {
            scaleWidth = logoSize * 1.0f / logoWidth;
            scaleHeight = logoSize * 1.0f / logoHeight;
            left = (srcWidth - logoSize) / 2;
            top = (srcHeight - logoSize) / 2;
        }

        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bitmapLogo = Bitmap.createBitmap(srcLogo, 0, 0, logoWidth, logoHeight, matrix, false);

        float logoBorder = encodeBuild.getLogoBorder();
        // ????????????
        if (logoBorder > 0) {
            int borderColor = encodeBuild.getLogoBorderColor() == -1 ? Color.WHITE : encodeBuild.getLogoBorderColor();
            Bitmap borderBitmap = Bitmap.createBitmap((int) (bitmapLogo.getWidth() + logoBorder),
                    (int) (bitmapLogo.getHeight() + logoBorder), Bitmap.Config.ARGB_8888);
            Canvas borderCanvas = new Canvas(borderBitmap);
            borderCanvas.drawARGB(0, 0, 0, 0);

            Paint borderPaint = new Paint();
            borderPaint.setAntiAlias(true);
            borderPaint.setColor(borderColor);

            QRLogoBorderType logoBorderType = encodeBuild.getLogoBorderType();
            Rect rectBorder = borderCanvas.getClipBounds();
            // ??????
            if (logoBorderType == QRLogoBorderType.RECTANGLE) {
                borderCanvas.drawRect(rectBorder, borderPaint);
            }
            // ??????
            else if (logoBorderType == QRLogoBorderType.CIRCLE) {
                borderCanvas.drawOval(new RectF(rectBorder), borderPaint);
                borderPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

                bitmapLogo = getCircleBitmap(bitmapLogo, borderColor);
            }
            // ????????????
            else {
                float logoBorderRadius = encodeBuild.getLogoBorderRadius();
                borderCanvas.drawRoundRect(new RectF(rectBorder), logoBorderRadius, logoBorderRadius, borderPaint);
                borderPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

                // ???logo?????????????????????
                bitmapLogo = getRoundedBitmap(bitmapLogo, logoBorderRadius, borderColor);
            }

            canvas.drawBitmap(borderBitmap, left - (logoBorder / 2), top - (logoBorder / 2), null);
            if (!borderBitmap.isRecycled()) {
                borderBitmap.recycle();
            }
        }

        canvas.drawBitmap(bitmapLogo, left, top, null);
        if (!bitmapLogo.isRecycled()) {
            bitmapLogo.recycle();
        }

        if (!srcLogo.isRecycled()) {
            srcLogo.recycle();
        }
        if (!src.isRecycled()) {
            src.recycle();
        }

        canvas.save();
        canvas.restore();
        return bitmap;
    }

    Bitmap encodeAsBitmap() throws WriterException {
        String content = encodeBuild.getEncodeContents();
        BarcodeFormat barcodeFormat = encodeBuild.getBarcodeFormat();
        int qrColor = encodeBuild.getColor();
        int size = encodeBuild.getSize();
        if (content == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, Charset.forName("UTF-8").name());
        hints.put(EncodeHintType.MARGIN, encodeBuild.getMargin());
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(content, barcodeFormat, size, size, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                // ?????????????????????
                if (result.get(x, y)) {
                    int[] colors = encodeBuild.getColors();
                    if (colors != null) {
                        if (x < size / 2 && y < size / 2) {
                            pixels[y * size + x] = colors[0];// ??????
                        } else if (x < size / 2 && y > size / 2) {
                            pixels[y * size + x] = colors[1];// ??????
                        } else if (x > size / 2 && y > size / 2) {
                            pixels[y * size + x] = colors[2];// ??????
                        } else {
                            pixels[y * size + x] = colors[3];// ??????
                        }
                    } else {
                        pixels[offset + x] = qrColor;
                    }
                } else {
                    int qrBgColor = encodeBuild.getQrBackgroundColor();
                    pixels[offset + x] = qrBgColor == 0 ? WHITE : qrBgColor;
                }
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        if (encodeBuild.getQrBackground() != null) {
            bitmap = addBackground(bitmap, encodeBuild.getQrBackground());
        }
        Bitmap logoBitmap = encodeBuild.getLogoBitmap();
        if (logoBitmap != null) {
            bitmap = addLogo(bitmap);
        }
        return bitmap;
    }

    private void encodeContentsFromZXing(QREncode.Builder build) {
        if (build.getBarcodeFormat() == null || build.getBarcodeFormat() == BarcodeFormat.QR_CODE) {
            build.setBarcodeFormat(BarcodeFormat.QR_CODE);
            encodeQRCodeContents(build);
        }
    }

    private void encodeQRCodeContents(QREncode.Builder build) {
        switch (build.getParsedResultType()) {
            case WIFI:
                encodeBuild.setEncodeContents(build.getContents());
                break;
            case CALENDAR:
                encodeBuild.setEncodeContents(build.getContents());
                break;
            case ISBN:
                encodeBuild.setEncodeContents(build.getContents());
                break;
            case PRODUCT:
                encodeBuild.setEncodeContents(build.getContents());
                break;
            case VIN:
                encodeBuild.setEncodeContents(build.getContents());
                break;
            case URI:
                encodeBuild.setEncodeContents(build.getContents());
                break;
            case TEXT:
                encodeBuild.setEncodeContents(build.getContents());
                break;
            case EMAIL_ADDRESS:
                encodeBuild.setEncodeContents("mailto:" + build.getContents());
                break;
            case TEL:
                encodeBuild.setEncodeContents("tel:" + build.getContents());
                break;
            case SMS:
                encodeBuild.setEncodeContents("sms:" + build.getContents());
                break;
            case ADDRESSBOOK:
                Bundle contactBundle = null;
                //uri??????
                Uri addressBookUri = build.getAddressBookUri();
                if (addressBookUri != null)
                    contactBundle = new ParserUriToVCard().parserUri(context, addressBookUri);
                //Bundle??????
                if ((contactBundle != null && contactBundle.isEmpty()) || contactBundle == null)
                    contactBundle = build.getBundle();
                if (contactBundle != null) {
                    String name = contactBundle.getString(ContactsContract.Intents.Insert.NAME);
                    String organization = contactBundle.getString(ContactsContract.Intents.Insert.COMPANY);
                    String address = contactBundle.getString(ContactsContract.Intents.Insert.POSTAL);
                    List<String> phones = getAllBundleValues(contactBundle, ParserUriToVCard.PHONE_KEYS);
                    List<String> phoneTypes = getAllBundleValues(contactBundle, ParserUriToVCard.PHONE_TYPE_KEYS);
                    List<String> emails = getAllBundleValues(contactBundle, ParserUriToVCard.EMAIL_KEYS);
                    String url = contactBundle.getString(ParserUriToVCard.URL_KEY);
                    List<String> urls = url == null ? null : Collections.singletonList(url);
                    String note = contactBundle.getString(ParserUriToVCard.NOTE_KEY);
                    ContactEncoder encoder = build.isUseVCard() ?
                            new VCardContactEncoder() : new MECARDContactEncoder();
                    String[] encoded = encoder.encode(Collections.singletonList(name), organization
                            , Collections.singletonList(address), phones, phoneTypes, emails, urls, note);
                    // Make sure we've encoded at least one field.
                    if (!encoded[1].isEmpty()) {
                        encodeBuild.setEncodeContents(encoded[0]);
                    }
                }
                break;
            case GEO:
                Bundle locationBundle = build.getBundle();
                if (locationBundle != null) {
                    float latitude = locationBundle.getFloat("LAT", Float.MAX_VALUE);
                    float longitude = locationBundle.getFloat("LONG", Float.MAX_VALUE);
                    if (latitude != Float.MAX_VALUE && longitude != Float.MAX_VALUE) {
                        encodeBuild.setEncodeContents("geo:" + latitude + ',' + longitude);
                    }
                }
                break;
        }
    }

}
