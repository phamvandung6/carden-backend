package com.loopy.carden.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.utils.StringUtils;
import java.net.URI;

@Configuration
public class CloudflareR2Config {

	@Bean
	@ConfigurationProperties(prefix = "r2")
	public R2Properties r2Properties() {
		return new R2Properties();
	}

	@Bean
	public S3Client r2S3Client(R2Properties props) {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(props.getAccessKeyId(), props.getSecretAccessKey());
		S3ClientBuilder builder = S3Client.builder()
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.region(Region.US_EAST_1);
		if (StringUtils.isNotBlank(props.getEndpoint())) {
			builder = builder.endpointOverride(URI.create(props.getEndpoint()));
		}
		return builder.build();
	}

	@Bean
	public S3Presigner r2Presigner(R2Properties props) {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(props.getAccessKeyId(), props.getSecretAccessKey());
		S3Presigner.Builder builder = S3Presigner.builder()
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.region(Region.US_EAST_1);
		if (StringUtils.isNotBlank(props.getEndpoint())) {
			builder = builder.endpointOverride(URI.create(props.getEndpoint()));
		}
		return builder.build();
	}

	@Getter
	@Setter
	public static class R2Properties {
		private String endpoint; // https://<accountid>.r2.cloudflarestorage.com
		private String bucket;
		private String accessKeyId;
		private String secretAccessKey;
		private String publicBaseUrl; // e.g., https://pub-....r2.dev or CDN domain
	}
}


