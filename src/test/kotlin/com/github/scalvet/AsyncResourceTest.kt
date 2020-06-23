package com.github.scalvet

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.*
import java.util.function.Consumer
import javax.ws.rs.core.Response.Status

@QuarkusTest
@QuarkusTestResource(S3Resource::class)
class AsyncResourceTest {


    private val FILE_NAME_PREFIX = "test-file-"
    private val FILE_MIMETYPE = "text/plain"

    @ParameterizedTest
    @ValueSource(strings = ["s3", "async-s3"])
    fun testResource(testedResource: String?) {
        val data = Arrays.asList("Cherry", "Pear")

        //Upload files
        data.forEach(Consumer { fruit: String ->
            given()
                    .pathParam("resource", testedResource)
                    .multiPart("file", fruit)
                    .multiPart("filename", FILE_NAME_PREFIX + fruit)
                    .multiPart("mimetype", FILE_MIMETYPE)
                    .`when`()
                    .post("/{resource}/upload")
                    .then()
                    .statusCode(Status.CREATED.statusCode)
        }
        )

        //List files
        given()
                .pathParam("resource", testedResource)
                .`when`()["/{resource}"]
                .then()
                .statusCode(200) //Objects are sorted by modified data, so the last added will be the first from list files
                .body("size()", equalTo(2))
                .body("[0].objectKey", equalTo(FILE_NAME_PREFIX + data[1]))
                .body("[0].size", equalTo(data[1].length))
                .body("[1].objectKey", equalTo(FILE_NAME_PREFIX + data[0]))
                .body("[1].size", equalTo(data[0].length))

        //Download file
        data.forEach(Consumer { fruit: String ->
            given()
                    .pathParam("resource", testedResource)
                    .pathParam("objectKey", FILE_NAME_PREFIX + fruit)
                    .`when`()["/{resource}/download/{objectKey}"]
                    .then()
                    .statusCode(200)
                    .body(equalTo(fruit))
        }
        )
    }
}