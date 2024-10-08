package com.solo83.service;

import com.solo83.exception.MinioServiceException;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MinioService {

    private final MinioClient minioClient;
    private final Environment env;
    private final static String DIRECTORY_SUFFIX = "/";

    public MinioService(MinioClient minioClient, Environment env) {
        this.minioClient = minioClient;
        this.env = env;
        createDefaultBucket();
    }

    private void createDefaultBucket() {
        String bucketName = env.getProperty("minio.bucket.name");
        MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder().bucket(bucketName).build();
        BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();
        try {
            if (!minioClient.bucketExists(bucketExistsArgs)) {
                minioClient.makeBucket(makeBucketArgs);
                log.info("Bucket Created - {}", bucketName);
            }
            log.info("Bucket already exists");
        } catch (Exception e) {
            log.error("Error creating DefaultBucket {}", e.getMessage());
            throw new MinioServiceException("Error creating DefaultBucket, " +e.getMessage(), e);
        }
    }

    public void createEmptyFolder(String path) {
        if(!path.endsWith(DIRECTORY_SUFFIX)) {
            path += DIRECTORY_SUFFIX;
        }
        String[] split = path.split(DIRECTORY_SUFFIX);
        if(split.length > 0) {
            StringBuilder pathToObject = new StringBuilder();
            for (String s : split) {
                pathToObject.append(s).append(DIRECTORY_SUFFIX);
                if (!isObjectExist(pathToObject.toString())) {
                    try {
                        minioClient.putObject(
                                PutObjectArgs.builder()
                                        .object(pathToObject.toString())
                                        .bucket(env.getProperty("minio.bucket.name"))
                                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                                        .build());
                        log.info("Empty folder created successfully - {}", pathToObject);
                    } catch (Exception e) {
                        log.error("Error creating empty folder {} - {}", path, e.getMessage());
                        throw new MinioServiceException("Error creating empty folder, " + e.getMessage(), e);
                    }
                }
                log.info("Folder is already exists - {}", pathToObject);
            }
        }
    }

    public void downloadObject(String objectName, String path, HttpServletResponse response) {

        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(env.getProperty("minio.bucket.name"))
                        .object(path)
                        .build())) {

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(env.getProperty("minio.bucket.name"))
                            .object(path)
                            .build());


            response.setContentType("application/octet-stream");
            String encodedFileName = URLEncoder.encode(objectName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
            response.setContentLengthLong(stat.size());

            IOUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();

            log.info("File {} downloaded successfully", objectName);
        } catch (Exception e) {
            log.error("Error while downloading a file {} - {}", objectName, e.getMessage());
            throw new MinioServiceException("Error while downloading a file: " + e.getMessage(), e);
        }
    }

    public void removeObject(String path) {
        List<String> errorMessages = new ArrayList<>();
        Iterable<Result<Item>> objects = getObjects(path, true);
        for (Result<Item> object : objects) {
            try {
                String pathToRemove = object.get().objectName();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(env.getProperty("minio.bucket.name"))
                                .object(pathToRemove)
                                .build());
                log.info("Successfully removed object at path {}", pathToRemove);
            } catch (Exception e) {
                String errorMessage = String.format("Error removing object %s: %s", path, e.getMessage());
                log.error(errorMessage);
                errorMessages.add(errorMessage);
            }
        }
        if (!errorMessages.isEmpty()) {
            throw new MinioServiceException("Errors occurred while removing objects: " + String.join(", ", errorMessages));
        }
    }

    public void renameObject(String oldPath, String newPath) {
        if (isObjectExist(newPath)) {
            log.warn("Object at path {} already exists and cannot be renamed to same name.", newPath);
            return;
        }

        try {
            Iterable<Result<Item>> objects = getObjects(oldPath,true);

            for (Result<Item> result : objects) {
                Item object = result.get();
                String oldObjectName = object.objectName();
                String newObjectName = oldObjectName.replace(oldPath, newPath);

                CopySource copySource = CopySource.builder()
                        .bucket(env.getProperty("minio.bucket.name"))
                        .object(oldObjectName)
                        .build();

                CopyObjectArgs copyObjectArgs = CopyObjectArgs.builder()
                        .bucket(env.getProperty("minio.bucket.name"))
                        .object(newObjectName)
                        .source(copySource)
                        .build();

                minioClient.copyObject(copyObjectArgs);

                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(env.getProperty("minio.bucket.name"))
                                .object(oldObjectName)
                                .build()
                );
                log.info("Sucessfully renamed from {} to {}", oldPath, newPath);
            }
        } catch (Exception e) {
            log.error("Error renaming object from {} to {}", oldPath, newPath, e);
            throw new MinioServiceException("Error renaming object, " +e.getMessage(), e);
        }
    }

    public Iterable<Result<Item>> getObjects(String path, boolean recursive) {
        try {
            return minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(env.getProperty("minio.bucket.name"))
                            .prefix(path)
                            .recursive(recursive)
                            .build()
            );
        } catch (Exception e) {
            log.error("An error occurred while getObjects from path: {} - {}",path, e.getMessage());
            throw new MinioServiceException("Error while getObjects from path, " +e.getMessage(), e);
        }
    }

    public void uploadFile(MultipartFile file, String path) {
        String objectName = path+file.getOriginalFilename();

        Map<String, String> userMetaData = new HashMap<>();
        if (file.getOriginalFilename() != null)
            userMetaData.put("file-name", file.getOriginalFilename());

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(env.getProperty("minio.bucket.name"))
                    .object(objectName)
                    .contentType(file.getContentType())
                    .userMetadata(userMetaData)
                    .stream(file.getInputStream(), file.getInputStream().available(), -1)
                    .build());
            log.info("File successfully uploaded: {}", objectName);
        } catch (Exception e) {
            log.error("An error occurred while uploading file: {} - {}",file.getOriginalFilename(), e.getMessage());
            throw new MinioServiceException("Error while uploading file, " +e.getMessage(), e);
        }
    }

    public List<Item> searchFilesByName(String prefix, String query) {
        List<Item> matchedItems = new ArrayList<>();

        minioClient.listObjects(ListObjectsArgs.builder()
                        .bucket(env.getProperty("minio.bucket.name"))
                        .prefix(prefix)
                        .recursive(true)
                        .build())
                .forEach(item -> {
                    try {
                        String filename = item.get().objectName().substring(item.get().objectName().lastIndexOf("/") + 1);
                        if (filename.toLowerCase().contains(query.toLowerCase())) {
                            matchedItems.add(item.get());
                        }
                    } catch (Exception e) {
                        log.error("An error occurred while searching objects: {}", e.getMessage());
                        throw new MinioServiceException("Error while searching objects, " + e.getMessage(), e);
                    }
                });
        log.info("Search successful, objects quantity: {}", matchedItems.size());
        return matchedItems;
    }

    private boolean isObjectExist(String path) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(env.getProperty("minio.bucket.name"))
                    .object(path)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            log.warn("Object not found in Minio for path {}: {}", path, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("An error occurred while checking existence of object {} from Minio: {}", path, e.getMessage());
            return false;
        }
    }
}
