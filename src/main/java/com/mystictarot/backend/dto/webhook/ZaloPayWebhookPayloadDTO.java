package com.mystictarot.backend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZaloPayWebhookPayloadDTO {

    @JsonProperty("data")
    private String data;

    @JsonProperty("mac")
    private String mac;

    @JsonProperty("type")
    private Integer type;
}
