package com.FluffyTerror.Umagramm_Backend.responses.attachment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;

@Getter
@Setter
@AllArgsConstructor
public class ResourceData {
    private String filename;
    private String contentType;
    private Resource resource;
}
