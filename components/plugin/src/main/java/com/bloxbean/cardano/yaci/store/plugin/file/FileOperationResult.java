package com.bloxbean.cardano.yaci.store.plugin.file;

public class FileOperationResult {
    private final boolean success;
    private final String errorMessage;
    private final Object data;

    public FileOperationResult(boolean success, String errorMessage, Object data) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.data = data;
    }

    public static FileOperationResult success(Object data) {
        return new FileOperationResult(true, null, data);
    }

    public static FileOperationResult success() {
        return new FileOperationResult(true, null, null);
    }

    public static FileOperationResult error(String errorMessage) {
        return new FileOperationResult(false, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isError() {
        return !success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Object getData() {
        return data;
    }

    public String getAsString() {
        if (data == null) return null;
        return data.toString();
    }

    @SuppressWarnings("unchecked")
    public <T> T getAs(Class<T> type) {
        if (data == null) return null;
        try {
            return (T) data;
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        if (success) {
            return "FileOperationResult{success=true, data=" + data + "}";
        } else {
            return "FileOperationResult{success=false, error='" + errorMessage + "'}";
        }
    }
}