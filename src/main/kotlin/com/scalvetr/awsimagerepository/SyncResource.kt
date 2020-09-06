package com.scalvetr.awsimagerepository

import org.jboss.resteasy.annotations.jaxrs.PathParam
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.S3Object
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.*
import javax.enterprise.inject.Default
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.StreamingOutput
import kotlin.streams.toList

@Path("/images")
class SyncResource : CommonResource() {
    @Inject
    @field: Default
    lateinit var s3: S3Client

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadFile(@MultipartForm formData: FormData): Response {
        return when {
            formData.fileName.isNullOrEmpty() -> Response.status(Status.BAD_REQUEST).build()
            formData.mimeType.isNullOrEmpty() -> Response.status(Status.BAD_REQUEST).build()
            else -> {
                val putResponse = s3.putObject(
                        buildPutRequest(formData),
                        uploadToTemp(formData.data).toPath()
                )
                if (putResponse != null) {
                    Response.ok().status(Status.CREATED).build()
                } else {
                    Response.serverError().build()
                }
            }
        }

        @GET
        @Path("/download/{objectKey}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        fun downloadFile(@PathParam("objectKey") objectKey: String): Response {
            val baos = ByteArrayOutputStream()

            val o = s3.getObject(buildGetRequest(objectKey), ResponseTransformer.toOutputStream(baos))
            val response = Response.ok(StreamingOutput { output: OutputStream -> baos.writeTo(output) })
            response.header("Content-Disposition", "attachment;filename=$objectKey")
            response.header("Content-Type", o.contentType())
            return response.build()
        }

        @GET
        @Path("")
        @Produces(MediaType.APPLICATION_JSON)
        fun listFiles(): List<FileObject> {
            val listRequest = ListObjectsRequest.builder().bucket(bucketName).build()

            //HEAD S3 objects to get metadata
            return s3.listObjects(listRequest)
                    .contents().stream().sorted(Comparator.comparing { obj: S3Object -> obj.lastModified() }.reversed())
                    .map { FileObject(objectKey = it.key(), size = it.size()) }.toList()
        }
    }
}