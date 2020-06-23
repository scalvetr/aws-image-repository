package com.github.scalvet

import org.jboss.resteasy.annotations.providers.multipart.PartType
import java.io.InputStream
import javax.ws.rs.FormParam
import javax.ws.rs.core.MediaType


class FormData {
    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    var data: InputStream? = null

    @FormParam("filename")
    @PartType(MediaType.TEXT_PLAIN)
    var fileName: String? = null

    @FormParam("mimetype")
    @PartType(MediaType.TEXT_PLAIN)
    var mimeType: String? = null
}