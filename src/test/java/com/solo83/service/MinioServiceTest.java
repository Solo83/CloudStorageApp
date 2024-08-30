package com.solo83.service;


import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Testcontainers
class MinioServiceTest {

    private static final String accessKey = "minioadmin";
    private static final String secretKey = "minioadmin";
    private static final Integer defaultPort = 9100;

    @Container
    private static final GenericContainer<?> minioContainer =
            new GenericContainer<>(DockerImageName.parse("quay.io/minio/minio"))
                    .withExposedPorts(defaultPort)
                    .withEnv("MINIO_ROOT_USER", accessKey)
                    .withEnv("MINIO_ROOT_PASSWORD", secretKey)
                    .withCommand("server /data --address :9100")
                    .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                            new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(defaultPort), new ExposedPort(defaultPort)))));

    private MinioClient createMinioClient() {
        return MinioClient.builder()
                .endpoint(minioContainer.getContainerIpAddress(), minioContainer.getMappedPort(defaultPort), false)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Test
    void createEmptyFolder() {
        MinioClient minioClient = createMinioClient();

        assertDoesNotThrow(() -> {
            String bucketName = "test-bucket";
            String folderName = "test-folder/";
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(folderName)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .build()
            );
        });
    }
}