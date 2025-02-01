/*******************************************************************************
 * Copyright  (c) 2013, 2025 James M. ZHOU
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.tinystruct.system.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import org.tinystruct.ApplicationException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

public final class Matrix {
    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;
    private Matrix(){}

    public static BufferedImage toQRImage(String data, int width, int height) throws ApplicationException {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();

        Map<EncodeHintType, String> hints = new HashMap<EncodeHintType, String>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        try {
            BitMatrix matrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
                }
            }

            return image;
        } catch (WriterException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }

    public static String getBase64Image(RenderedImage image) throws ApplicationException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return new String(Base64.getEncoder().encode(output.toByteArray()));
        } catch (IOException e) {
            throw new ApplicationException(e.getMessage(), e);
        }
    }
}
