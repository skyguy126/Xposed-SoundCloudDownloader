package com.skyguy126.soundclouddownloader;

import java.io.File;

public class DownloadPayload {
    private String url;
    private String fileName;
    private File saveDirectory;
    private String title;
    private String artistName;
    private String genre;
    private String imageUrl;
    private boolean includeMetadata;

    public DownloadPayload(String url, String fileName, File saveDirectory, String title, String artistName, String genre, String imageUrl, boolean includeMetadata) {
        this.url = url;
        this.fileName = fileName;
        this.saveDirectory = saveDirectory;
        this.title = title;
        this.artistName = artistName;
        this.genre = genre;
        this.imageUrl = imageUrl;
        this.includeMetadata = includeMetadata;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }

    public File getSaveDirectory() {
        return saveDirectory;
    }

    public String getTitle() {
        return title;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getGenre() {
        return genre;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean includeMetadata() {
        return includeMetadata;
    }
}
