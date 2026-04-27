package com.FluffyTerror.Umagramm_Backend.requests.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MessageTextRequest {
    String content;
    String initialisationVector;
    Long keyVersion;
}
