package com.github.scalvet

import software.amazon.awssdk.services.s3.model.S3Object


class FileObject {
    var objectKey: String? = null
        private set
    var size: Long? = null
        private set

    fun setObjectKey(objectKey: String?): FileObject {
        this.objectKey = objectKey
        return this
    }

    fun setSize(size: Long?): FileObject {
        this.size = size
        return this
    }

}
