package com.github.scalvet

import org.jboss.resteasy.annotations.providers.multipart.PartType
import java.io.InputStream
import javax.ws.rs.FormParam
import javax.ws.rs.core.MediaType


data class FormData (
    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    val data: InputStream,

    @FormParam("filename")
    @PartType(MediaType.TEXT_PLAIN)
    val fileName: String,

    @FormParam("mimetype")
    @PartType(MediaType.TEXT_PLAIN)
    val mimeType: String
)