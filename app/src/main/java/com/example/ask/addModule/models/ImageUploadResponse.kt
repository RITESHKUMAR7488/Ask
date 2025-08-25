    package com.example.ask.addModule.models


    import com.google.gson.annotations.Expose
    import com.google.gson.annotations.SerializedName

    data class ImageUploadResponse(
        @SerializedName("status_code") @Expose val statusCode: Int? = null,
        @SerializedName("success") @Expose val success: Success? = null,
        @SerializedName("image") @Expose val image: Image? = null,
        @SerializedName("status_txt") @Expose val statusText: String? = null
    )

    data class Success(
        @SerializedName("message") @Expose val message: String? = null,
        @SerializedName("code") @Expose val code: Int? = null
    )

    data class Image(
        @SerializedName("name") @Expose val name: String? = null,
        @SerializedName("extension") @Expose val extension: String? = null,
        @SerializedName("size") @Expose val size: Int? = null,
        @SerializedName("width") @Expose val width: Int? = null,
        @SerializedName("height") @Expose val height: Int? = null,
        @SerializedName("date") @Expose val date: String? = null,
        @SerializedName("storage_id") @Expose val storageId: String? = null,
        @SerializedName("nsfw") @Expose val nsfw: String? = null,
        @SerializedName("md5") @Expose val md5: String? = null,
        @SerializedName("storage") @Expose val storage: String? = null,
        @SerializedName("original_filename") @Expose val originalFileName: String? = null,
        @SerializedName("views") @Expose val views: String? = null,
        @SerializedName("url") @Expose val url: String? = null,
        @SerializedName("thumb") @Expose val thumb: Thumb? = null,
        @SerializedName("medium") @Expose val medium: Medium? = null,
        @SerializedName("display_url") @Expose val displayUrl: String? = null
    )

    data class Thumb(
        @SerializedName("filename") @Expose val filename: String? = null,
        @SerializedName("url") @Expose val url: String? = null
    )

    data class Medium(
        @SerializedName("filename") @Expose val filename: String? = null,
        @SerializedName("url") @Expose val url: String? = null
    )
