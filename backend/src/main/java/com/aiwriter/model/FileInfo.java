package com.aiwriter.model;

import java.util.List;

public class FileInfo {
    private String id;
    private String name;
    private String path;
    private String type;
    private List<FileInfo> children;

    public FileInfo() {
    }

    public FileInfo(String id, String name, String path, String type, List<FileInfo> children) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.type = type;
        this.children = children;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<FileInfo> getChildren() {
        return children;
    }

    public void setChildren(List<FileInfo> children) {
        this.children = children;
    }

    public static class Builder {
        private String id;
        private String name;
        private String path;
        private String type;
        private List<FileInfo> children;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder children(List<FileInfo> children) {
            this.children = children;
            return this;
        }

        public FileInfo build() {
            return new FileInfo(id, name, path, type, children);
        }
    }
}
