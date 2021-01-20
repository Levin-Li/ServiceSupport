package com.levin.commons.service.domain;


import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 服务请求
 * 请求格式不做具体的限制
 *
 * @author lilw
 */

@Schema(description = "服务请求类")
public interface ServiceRequest
        extends Serializable {

    @Schema(description = "请求签名串")
    default String getSign() {
        return "";
    }

}
