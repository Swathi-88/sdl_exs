package com.parth.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UrlRequest {

    @NotBlank
    private String url;
}

