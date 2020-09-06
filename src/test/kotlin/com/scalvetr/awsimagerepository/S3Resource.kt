package com.scalvetr.awsimagerepository
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.net.URI
import java.util.*
import java.util.function.Consumer
import javax.enterprise.inject.Default
import javax.inject.Inject

class S3Resource :QuarkusTestResourceLifecycleManager{

    val bucketName = "quarkus.test.bucket"

    @Inject
    @field: Default
    lateinit var s3: LocalStackContainer

    override fun start(): kotlin.collections.Map<String, String>? {
        DockerClientFactory.instance().client()
        try {
            s3 = LocalStackContainer().withServices(Service.S3)
            s3.start()
            var client = S3Client.builder()
                    .endpointOverride(URI(endpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("accesskey", "secretKey")))
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .region(Region.US_EAST_1).build()
            client.createBucket(Consumer { b: CreateBucketRequest.Builder -> b.bucket(bucketName) })
        } catch (e: Exception) {
            throw RuntimeException("Could not start S3 localstack server", e)
        }
        val properties: MutableMap<String, String> = HashMap()
        properties["quarkus.s3.endpoint-override"] = endpoint()
        properties["quarkus.s3.aws.region"] = "us-east-1"
        properties["quarkus.s3.aws.credentials.type"] = "static"
        properties["quarkus.s3.aws.credentials.static-provider.access-key-id"] = "accessKey"
        properties["quarkus.s3.aws.credentials.static-provider.secret-access-key"] = "secretKey"
        properties["bucket.name"] = bucketName
        return properties
    }

    override fun stop() {
        if (s3 != null) {
            s3.close()
        }
    }

    private fun endpoint(): String {
        return java.lang.String.format("http://%s:%s", s3.containerIpAddress, s3.getMappedPort(Service.S3.port))
    }
}