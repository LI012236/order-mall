package com.example.mall.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 在线接口文档配置：http://127.0.0.1:18091/swagger-ui.html
 * 右上角 Authorize 填入登录返回的 token，即可直接调需要鉴权的接口。
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearer";

    @Bean
    public OpenAPI mallOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("order-mall 接口文档")
                        .version("1.0.0")
                        .description("订单商城后端：24 个接口。除注册/登录/商品/回调外，均需请求头 "
                                + "Authorization: Bearer <token>；统一响应 {code, message, data}。"))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("token")
                                .description("登录接口返回的 token")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME));
    }
}
