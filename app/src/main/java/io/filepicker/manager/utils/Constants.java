package io.filepicker.manager.utils;

/**
 * Created by maciejwitowski on 11/4/14.
 */
public final class Constants {

    // Ensure noninstantiability
    private Constants(){}

    public static final String API_KEY = "PUT YOUR API KEY HERE";

    // File types
    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String IMAGE_JPG = "image/jpg";
    public static final String IMAGE_PNG = "image/png";
    public static final String DOCUMENT_PDF = "application/pdf";

    // Content providers' authorities (each authority must be unique)
    public static final String CONTENT_DATA_AUTHORITY = "io.filepicker.manager.data";
    public static final String CONTENT_FILES_AUTHORITY = "io.filepicker.manager.files";

}