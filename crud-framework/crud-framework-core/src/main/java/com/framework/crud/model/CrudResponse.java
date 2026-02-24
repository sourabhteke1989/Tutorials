package com.framework.crud.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Generic CRUD response returned by the framework.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrudResponse {

    private boolean success;
    private String message;

    /** Single entity result (for GET by id, CREATE, UPDATE). */
    private Map<String, Object> data;

    /** List result (for GET list). */
    private List<Map<String, Object>> dataList;

    /** Validation or field-level error details. */
    private Map<String, String> errors;

    /** Total record count for paginated results. */
    private Long totalCount;

    /** Current page number. */
    private Integer page;

    /** Page size. */
    private Integer size;

    /** Server timestamp. */
    private Instant timestamp;

    // ---- Constructors ----

    public CrudResponse() {
        this.timestamp = Instant.now();
    }

    // ---- Static factory methods ----

    public static CrudResponse success(String message) {
        CrudResponse r = new CrudResponse();
        r.success = true;
        r.message = message;
        return r;
    }

    public static CrudResponse success(String message, Map<String, Object> data) {
        CrudResponse r = success(message);
        r.data = data;
        return r;
    }

    public static CrudResponse successList(String message, List<Map<String, Object>> dataList,
                                           Long totalCount, Integer page, Integer size) {
        CrudResponse r = success(message);
        r.dataList = dataList;
        r.totalCount = totalCount;
        r.page = page;
        r.size = size;
        return r;
    }

    public static CrudResponse error(String message) {
        CrudResponse r = new CrudResponse();
        r.success = false;
        r.message = message;
        return r;
    }

    public static CrudResponse error(String message, Map<String, String> errors) {
        CrudResponse r = error(message);
        r.errors = errors;
        return r;
    }

    // ---- Getters & Setters ----

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public List<Map<String, Object>> getDataList() {
        return dataList;
    }

    public void setDataList(List<Map<String, Object>> dataList) {
        this.dataList = dataList;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
