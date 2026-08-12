package edu.tlu.klgd.infracstructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI teachingManagementOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title(OpenApiConstant.API_TITLE)
                .version(OpenApiConstant.API_VERSION)
                .description(OpenApiConstant.API_DESCRIPTION))
            .addSecurityItem(new SecurityRequirement().addList(OpenApiConstant.SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(OpenApiConstant.SECURITY_SCHEME_NAME, new SecurityScheme()
                    .name(OpenApiConstant.SECURITY_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme(OpenApiConstant.SECURITY_SCHEME)
                    .bearerFormat(OpenApiConstant.BEARER_FORMAT)));
    }
}
