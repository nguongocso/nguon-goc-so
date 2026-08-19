package vn.nguongocso.trace.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import lombok.extern.slf4j.Slf4j;
import vn.nguongocso.trace.service.QRCodeService;

@Service
@Slf4j
/** Sinh và lưu file ảnh QR cho mã truy xuất. */
public class QRCodeServiceImpl implements QRCodeService {
	@Value("${qr.image.storage.path:./files/qr}")

	private String storagePath;

	private static final int QR_WIDTH = 300;
	private static final int QR_HEIGHT = 300;

	/** Tạo ảnh QR và trả về đường dẫn lưu trữ. */
	@Override
	public String generateQRCode(String codeValue, UUID organizationId, UUID productionLotId, UUID shipmentId) {
		try {
			Path dirPath = Paths.get(storagePath, organizationId.toString(), productionLotId.toString(),
					shipmentId.toString());
			if (!Files.exists(dirPath)) {
				Files.createDirectories(dirPath);
			}

			String fileName = codeValue + ".png";
			Path filePath = dirPath.resolve(fileName);

			QRCodeWriter qrCodeWriter = new QRCodeWriter();
			BitMatrix bitMatrix = qrCodeWriter.encode(codeValue, BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT);
			MatrixToImageWriter.writeToPath(bitMatrix, "PNG", filePath);

			return "/files/qr/" + organizationId + "/" + productionLotId + "/" + shipmentId + "/" + fileName;

		} catch (WriterException | IOException e) {
			log.error("Lỗi sinh mã QR cho code {}: {}", codeValue, e.getMessage());

			throw new RuntimeException("Không thể sinh ảnh QR cho mã: " + codeValue, e);
		}
	}
}
