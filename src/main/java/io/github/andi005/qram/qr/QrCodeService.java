package io.github.andi005.qram.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.github.andi005.qram.common.AppProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Makes QR code images with ZXing. A label holds only {@code <APP_BASE_URL>/a/<TAG>}: no personal
 * or secret data (see ADR 003). Images are generated on request and never stored.
 */
@Service
public class QrCodeService {

  private final AppProperties properties;

  public QrCodeService(AppProperties properties) {
    this.properties = properties;
  }

  /** The address printed into the QR code of an asset's label. */
  public String labelUrl(String tag) {
    return properties.baseUrl() + "/a/" + tag;
  }

  /** A square PNG image of the QR code for {@code content}, {@code size} pixels wide. */
  public byte[] png(String content, int size) {
    Map<EncodeHintType, Object> hints =
        Map.of(
            // M recovers ~15% damage: enough for a scratched sticker, keeps the code simple.
            EncodeHintType.ERROR_CORRECTION,
            ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN,
            1,
            EncodeHintType.CHARACTER_SET,
            "UTF-8");
    try {
      BitMatrix matrix =
          new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", out);
      return out.toByteArray();
    } catch (WriterException | IOException e) {
      throw new IllegalStateException("Could not create QR code", e);
    }
  }
}
