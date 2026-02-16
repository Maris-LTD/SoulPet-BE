package com.mystictarot.backend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZaloPayCallbackDataDTO {

    @JsonProperty("app_id")
    private Long appId;

    @JsonProperty("app_trans_id")
    private String appTransId;

    @JsonProperty("app_time")
    private Long appTime;

    @JsonProperty("app_user")
    private String appUser;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("embed_data")
    private String embedData;

    @JsonProperty("item")
    private String item;

    @JsonProperty("zp_trans_id")
    private Long zpTransId;

    @JsonProperty("server_time")
    private Long serverTime;

    @JsonProperty("channel")
    private Integer channel;

    @JsonProperty("merchant_user_id")
    private String merchantUserId;
}
