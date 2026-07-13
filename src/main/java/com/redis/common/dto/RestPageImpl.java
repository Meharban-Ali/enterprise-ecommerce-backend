package com.redis.common.dto;

import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(value = {"pageable", "sort"}, ignoreUnknown = true)
public class RestPageImpl<T> extends PageImpl<T>{

     /**
     * @JsonCreator — Jackson ko batao yeh constructor use karo
     * deserialize karte waqt
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestPageImpl(
        @JsonProperty("content")        List<T> content,
        @JsonProperty("number")         int page,
        @JsonProperty("size")           int size,
        @JsonProperty("totalElements")  long totalElements) {
            
            super(content, PageRequest.of(page, size == 0 ? 10 : size), totalElements);
    }

    // Default constructor — Jackson ke liye zaroori
    public RestPageImpl(){
        super(List.of());
    }
    
}
 