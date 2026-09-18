package com.adobe.acrobatsign.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.adobe.acrobatsign.model.MultiUserAgreementDetails;
import com.adobe.acrobatsign.model.MultiUserWidgetDetails;
import com.adobe.acrobatsign.model.UserGroups;
import com.adobe.acrobatsign.model.UserWorkflows;
import com.adobe.acrobatsign.service.AdobeSignService;
import com.adobe.acrobatsign.service.GroupService;
import com.adobe.acrobatsign.service.LibraryTemplateService;
import com.adobe.acrobatsign.service.UserService;
import com.adobe.acrobatsign.service.WebformService;
import com.adobe.acrobatsign.service.WorkflowService;

@WebMvcTest
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.security.user.name=local-operator",
        "spring.security.user." + "password" + "=local-test-password"
})
class AccessSecurityTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AdobeSignService adobe;
    @MockitoBean
    GroupService groups;
    @MockitoBean
    LibraryTemplateService templates;
    @MockitoBean
    UserService users;
    @MockitoBean
    WebformService webforms;
    @MockitoBean
    WorkflowService workflows;

    @ParameterizedTest
    @ValueSource(strings = { "/", "/form", "/agreements", "/agreements/example",
            "/workflows", "/workflow/example", "/widgets", "/widgets/example/user@example.test",
            "/libraryTemplateSearch", "/send", "/sendsignature", "/helpx",
            "/output/agreements.zip", "/output/allAgreements.json", "/output/security-fixture.txt", "/styles/js/validation.js",
            "/styles/css/homeLayout.css", "/actuator", "/v3/api-docs", "/swagger-ui/index.html" })
    void readsRequireAnAuthorizedOperator(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get(path).with(user("reader").roles("USER"))).andExpect(status().isForbidden());
        verifyNoAdobeCalls();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/sendagreement", "/sendsignature", "/manageagreements",
            "/cancelagreements", "/hideAgreements", "/cancelReminders", "/checkReminders",
            "/downloadList", "/downloadAgreements", "/downloadformfields",
            "/downloadworkflows", "/downloadWebforms", "/agreements", "/multiuseragreements",
            "/multiUserAllAgreements", "/agreementListForIds", "/fetchUsersAgreement",
            "/fetchUsersTemplate", "/userWidgets", "/workflowsForAgreements",
            "/manageMultiuserAgreements" })
    void allMutationsRequireAuthenticationRoleAndCsrf(String path) throws Exception {
        mvc.perform(post(path).with(csrf())).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(post(path).with(user("reader").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("operator").roles("OPERATOR")))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("operator").roles("OPERATOR")).with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        verifyNoAdobeCalls();
    }

    @ParameterizedTest
    @ValueSource(strings = { "OPERATOR", "ADMIN" })
    void authorizedOperatorsCanReadAndMutateWithCsrf(String role) throws Exception {
        mvc.perform(get("/").with(user("authorized").roles(role))).andExpect(status().isOk());
        mvc.perform(get("/styles/js/validation.js").with(user("authorized").roles(role)))
                .andExpect(status().isOk());
        mvc.perform(get("/output/security-fixture.txt").with(user("authorized").roles(role)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Non-sensitive test export.")));
        mvc.perform(post("/cancelReminders").with(user("authorized").roles(role)).with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON).content("[]")).andExpect(status().isOk());
        verify(adobe).cancelReminders(List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = { "/sendagreement", "/manageagreements", "/cancelagreements",
            "/hideAgreements", "/cancelReminders", "/multiUserAllAgreements", "/workflowsForAgreements" })
    void mutationsCannotBeTriggeredWithGet(String path) throws Exception {
        mvc.perform(get(path).with(user("operator").roles("OPERATOR")))
                .andExpect(status().isMethodNotAllowed());
        verifyNoAdobeCalls();
    }

    @Test
    void localLoginAndLogoutAreUsableAndCsrfProtected() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(post("/login").param("username", "local-operator").param("password", "local-test-password"))
                .andExpect(status().isForbidden()).andExpect(unauthenticated());
        mvc.perform(formLogin().user("local-operator").password("wrong-password"))
                .andExpect(unauthenticated());
        MvcResult login = mvc.perform(formLogin().user("local-operator").password("local-test-password"))
                .andExpect(authenticated().withUsername("local-operator").withRoles("OPERATOR")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(unauthenticated());
        verifyNoAdobeCalls();
    }

    @Test
    void renderedFormsAndAjaxUseRealSessionCsrfTokens() throws Exception {
        UserGroups emptyGroups = new UserGroups();
        emptyGroups.setGroupInfoList(List.of());
        when(groups.getGroups()).thenReturn(emptyGroups);
        when(users.activeUsers()).thenReturn(List.of());
        MvcResult page = mvc.perform(get("/form").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk()).andReturn();
        String html = page.getResponse().getContentAsString();
        var forms = Pattern.compile("(?is)<form\\b[^>]*>.*?</form>").matcher(html);
        int count = 0;
        while (forms.find()) {
            assertThat(forms.group()).contains("name=\"_csrf\"");
            count++;
        }
        assertThat(count).isEqualTo(3);
        var token = Pattern.compile("<meta name=\"_csrf\" content=\"([^\"]+)\"").matcher(html);
        assertThat(token.find()).isTrue();
        String renderedToken = token.group(1);
        var header = Pattern.compile("<meta name=\"_csrf_header\" content=\"([^\"]+)\"").matcher(html);
        assertThat(header.find()).isTrue();
        MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
        mvc.perform(post("/cancelReminders").session(session).with(user("operator").roles("OPERATOR"))
                .header(header.group(1), renderedToken).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk());
        mvc.perform(post("/cancelReminders").with(user("operator").roles("OPERATOR"))
                .header(header.group(1), renderedToken).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void multipartFormsRequireAndAcceptCsrf() throws Exception {
        MultiUserWidgetDetails result = new MultiUserWidgetDetails();
        result.setWidgetList(List.of());
        result.setUserEmails(List.of());
        when(webforms.getWebforms(anyList())).thenReturn(result);
        mvc.perform(multipart("/userWidgets").file("file1", "email\nuser@example.test".getBytes(StandardCharsets.UTF_8))
                .with(user("operator").roles("OPERATOR"))).andExpect(status().isForbidden());
        verifyNoInteractions(webforms);
        mvc.perform(multipart("/userWidgets").file("file1", "email\nuser@example.test".getBytes(StandardCharsets.UTF_8))
                .with(user("operator").roles("OPERATOR")).with(csrf())).andExpect(status().isOk());
        verify(webforms).getWebforms(List.of("user@example.test"));
    }

    @Test
    void paginationBindsOnlyUserIdsAndPreservesPlusSignsWithCsrf() throws Exception {
        String email = "operator+agreements@example.test";
        MultiUserAgreementDetails result = new MultiUserAgreementDetails();
        result.setAgreementList(List.of());
        result.setTotalAgreements(0L);
        result.setUserEmails(List.of(email));
        result.setNextIndexMap(Map.of(email, 0L));
        when(adobe.searchMultiUserAgreements(List.of(email), "2026-01-01", "2026-09-18",
                "ABC", Map.of(email, 0), 1)).thenReturn(result);

        MvcResult page = mvc.perform(get("/widgets").with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk()).andReturn();
        var token = Pattern.compile("<meta name=\"_csrf\" content=\"([^\"]+)\"")
                .matcher(page.getResponse().getContentAsString());
        assertThat(token.find()).isTrue();
        mvc.perform(post("/multiuseragreements")
                .session((MockHttpSession) page.getRequest().getSession(false))
                .with(user("operator").roles("OPERATOR"))
                .formField("_csrf", token.group(1))
                .formField("userIds", "[\"" + email + "\"]")
                .formField("startDate", "2026-01-01")
                .formField("beforeDate", "2026-09-18")
                .formField("size", "{\"" + email + "\":0}")
                .formField("page", "1")).andExpect(status().isOk());
        verify(adobe).searchMultiUserAgreements(List.of(email), "2026-01-01", "2026-09-18",
                "ABC", Map.of(email, 0), 1);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/send", "/sendsignature", "/widgets", "/libraryTemplateSearch", "/workflows" })
    void renderedUploadAndWorkflowFormsContainCsrf(String path) throws Exception {
        UserWorkflows result = new UserWorkflows();
        result.setUserWorkflowList(List.of());
        when(workflows.getWorkflows()).thenReturn(result);
        mvc.perform(get(path).with(user("operator").roles("OPERATOR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void everyPostFormUsesThymeleafCsrfIntegrationAndEveryAjaxPageIncludesTokenMetadata() throws Exception {
        try (var paths = Files.list(Path.of("src/main/resources/templates"))) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".html")).toList()) {
                String html = Files.readString(path);
                assertThat(html).as(path.toString()).doesNotContain("bootstrap.min.js");
                var externalScripts = Pattern.compile("(?is)<script\\b[^>]*\\bsrc=\"https://[^>]*>")
                        .matcher(html);
                while (externalScripts.find()) {
                    assertThat(externalScripts.group()).as(path.toString())
                            .contains("integrity=\"sha384-", "crossorigin=\"anonymous\"");
                }
                var forms = Pattern.compile("(?is)<form\\b[^>]*>").matcher(html);
                while (forms.find()) {
                    String form = forms.group();
                    if (Pattern.compile("(?i)method\\s*=\\s*['\"]post['\"]").matcher(form).find()) {
                        assertThat(form).as(path.toString()).contains("th:action=");
                    }
                }
                if (html.contains("validation.js")) {
                    assertThat(html).as(path.toString()).contains("th:replace=\"~{csrf :: meta}\"");
                    assertThat(html.indexOf("jquery.min.js")).as(path.toString()).isNotNegative()
                            .isLessThan(html.indexOf("validation.js"));
                    assertThat(html.indexOf("jquery.min.js")).isEqualTo(html.lastIndexOf("jquery.min.js"));
                    assertThat(html.indexOf("validation.js")).isEqualTo(html.lastIndexOf("validation.js"));
                }
            }
        }
        String javascript = Files.readString(Path.of("src/main/resources/static/styles/js/validation.js"));
        assertThat(javascript).contains("target.origin === window.location.origin",
                "xhr.setRequestHeader(header.content, token.content)",
                "addInput(form, document.querySelector('meta[name=\"_csrf_parameter\"]').content");
    }

    private void verifyNoAdobeCalls() {
        verifyNoInteractions(adobe, groups, templates, users, webforms, workflows);
    }
}
