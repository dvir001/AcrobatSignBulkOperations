package com.adobe.acrobatsign.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.utils.SpringDocUtils;

import com.adobe.acrobatsign.controller.AdobeSignController;
import com.adobe.acrobatsign.controller.AgreementController;
import com.adobe.acrobatsign.controller.LibraryTemplateController;
import com.adobe.acrobatsign.controller.ManageAgreementsController;
import com.adobe.acrobatsign.controller.WebformController;
import com.adobe.acrobatsign.controller.WorkFlowController;

@Configuration
public class SpringFoxConfig {

	@Bean
    public GroupedOpenApi api() {
        // These operations use MVC controllers, not @RestController.
        SpringDocUtils.getConfig().addRestControllers(AdobeSignController.class,
                AgreementController.class, LibraryTemplateController.class,
                ManageAgreementsController.class, WebformController.class, WorkFlowController.class);
        return GroupedOpenApi.builder()
          .group("acrobatsign")
          .packagesToScan("com.adobe.acrobatsign.controller")
          .pathsToMatch("/**")
          .build();
    }
}
