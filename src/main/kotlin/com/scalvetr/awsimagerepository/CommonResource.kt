package com.scalvetr.awsimagerepository

import org.eclipse.microprofile.config.inject.ConfigProperty
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*


abstract class CommonResource {

    @ConfigProperty(name = "bucket.name")
    lateinit var bucketName: String

    protected fun buildPutRequest(formData: FormData): PutObjectRequest {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(formData.fileName)
                .contentType(formData.mimeType)
                .build()
    }

    protected fun buildGetRequest(objectKey: String): GetObjectRequest {
        return GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build()
    }

    protected fun tempFilePath(): File {
        return File(TEMP_DIR, StringBuilder().append("s3AsyncDownloadedTemp")
                .append(Date().time).append(UUID.randomUUID())
                .append(".").append(".tmp").toString())
    }

    protected fun uploadToTemp(data: InputStream): File {
        val tempPath = File.createTempFile("uploadS3Tmp", ".tmp")
        Files.copy(data, tempPath.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return tempPath
    }


    companion object {
        private val TEMP_DIR = System.getProperty("java.io.tmpdir")
    }
}