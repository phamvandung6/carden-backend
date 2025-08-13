package com.loopy.carden.service.storage;

import com.loopy.carden.config.CloudflareR2Config.R2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudflareR2Service {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final R2Properties props;

	public String uploadAvatar(Long userId, MultipartFile file) {
		validateImage(file);
		String original = StringUtils.cleanPath(file.getOriginalFilename());
		String ext = getExtension(original);
		String key = "avatars/" + userId + "/" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID() + ext;
		try {
			PutObjectRequest putReq = PutObjectRequest.builder()
					.bucket(props.getBucket())
					.key(key)
					.contentType(file.getContentType())
					.acl("private")
					.build();
			s3Client.putObject(putReq, RequestBody.fromBytes(file.getBytes()));
			return publicUrl(key);
		} catch (Exception e) {
			throw new RuntimeException("Failed to upload to R2", e);
		}
	}

	public String publicUrl(String key) {
		String base = props.getPublicBaseUrl();
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		return base + "/" + key;
	}

    public PresignedUpload createAvatarPresignedUpload(Long userId, String contentType) {
        String ext = extFromContentType(contentType);
        String key = "avatars/" + userId + "/" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID() + ext;
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(put)
                .signatureDuration(java.time.Duration.ofMinutes(10))
                .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        return new PresignedUpload(presigned.url().toString(), key, publicUrl(key), java.time.Instant.now().plus(java.time.Duration.ofMinutes(10)));
    }

    private String extFromContentType(String contentType) {
        if (contentType == null) return "";
        String ct = contentType.toLowerCase(java.util.Locale.ROOT);
        if (ct.contains("png")) return ".png";
        if (ct.contains("jpeg") || ct.contains("jpg")) return ".jpg";
        if (ct.contains("gif")) return ".gif";
        return "";
    }

    public record PresignedUpload(String uploadUrl, String key, String publicUrl, java.time.Instant expiresAt) {}

	private void validateImage(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is empty");
		}
		if (file.getSize() > 5L * 1024 * 1024) { // 5MB
			throw new IllegalArgumentException("File too large");
		}
		String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
		if (!(contentType.contains("png") || contentType.contains("jpeg") || contentType.contains("jpg"))) {
			throw new IllegalArgumentException("Unsupported content type");
		}
	}

	private String getExtension(String filename) {
		int idx = filename.lastIndexOf('.')
				;
		if (idx < 0) return "";
		String ext = filename.substring(idx).toLowerCase(Locale.ROOT);
		if (!ext.matches("\\.(png|jpg|jpeg|gif)")) {
			return "";
		}
		return ext;
	}
}


