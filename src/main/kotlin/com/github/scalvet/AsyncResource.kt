package com.github.scalvet

import io.smallrye.mutiny.Uni
import org.jboss.resteasy.annotations.jaxrs.PathParam
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.ListObjectsResponse
import software.amazon.awssdk.services.s3.model.S3Object
import java.io.File
import java.util.*
import java.util.function.Function
import javax.enterprise.inject.Default
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status
import kotlin.streams.toList


@Path("/async")
class AsyncResource : CommonResource() {
    @Inject
    @field: Default
    lateinit var s3: S3AsyncClient

    @POST
    @Path("upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadFile(@MultipartForm formData: FormData): Uni<Response> {
        return when {
            formData.fileName.isNullOrEmpty() -> Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build())
            formData.mimeType.isNullOrEmpty() -> Uni.createFrom().item(Response.status(Status.BAD_REQUEST).build())
            else -> {
                Uni.createFrom().completionStage(
                        s3.putObject(buildPutRequest(formData), AsyncRequestBody.fromFile(uploadToTemp(formData.data)))
                )
                        .onItem().ignore().andSwitchTo(Uni.createFrom().item(Response.created(null).build()))
                        .onFailure().recoverWithItem(Function { th: Throwable ->
                            th.printStackTrace()
                            Response.serverError().build()
                        })
            }
        }

        @GET
        @Path("download/{objectKey}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        @Throws(Exception::class)
        fun downloadFile(@PathParam("objectKey") objectKey: String): Uni<Response?>? {
            val tempFile: File = tempFilePath()
            return Uni.createFrom()
                    .completionStage(s3.getObject(buildGetRequest(objectKey), AsyncResponseTransformer.toFile(tempFile)))
                    .onItem()
                    .apply { o ->
                        Response.ok(tempFile)
                                .header("Content-Disposition", "attachment;filename=$objectKey")
                                .header("Content-Type", o.contentType()).build()
                    }
        }


        @GET
        @Produces(MediaType.APPLICATION_JSON)
        fun listFiles(): Uni<List<FileObject>> {
            val listRequest = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .build()
            return Uni.createFrom().completionStage { s3.listObjects(listRequest) }.onItem()
                    .apply { result: ListObjectsResponse -> toFileItems(result) }
        }
    }

    private fun toFileItems(response: ListObjectsResponse): List<FileObject> {
        return response.contents().stream()
                .sorted(Comparator.comparing { obj: S3Object -> obj.lastModified() }.reversed())
                .map { i -> FileObject(objectKey = i.key(), size = i.size()) }.toList()
    }

}


