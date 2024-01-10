/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.identity.integration.test.restclients;

import io.restassured.http.ContentType;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.testng.Assert;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.identity.integration.common.utils.ISIntegrationTest;
import org.wso2.identity.integration.test.rest.api.server.idp.v1.model.Claims;
import org.wso2.identity.integration.test.rest.api.server.idp.v1.model.IdentityProviderPOSTRequest;
import org.wso2.identity.integration.test.utils.OAuth2Constant;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class IdpMgtRestClient extends RestBaseClient {
    private static final String CLAIMS_PATH = "/claims";
    private static final String FEDERATED_AUTHENTICATORS_PATH = "/federated-authenticators/";
    private final String serverUrl;
    private final String IDENTITY_PROVIDER_BASE_PATH = "/api/server/v1/identity-providers";
    private final String tenantDomain;
    private final String username;
    private final String password;

    public IdpMgtRestClient(String serverUrl, Tenant tenantInfo) {

        this.serverUrl = serverUrl;
        this.tenantDomain = tenantInfo.getContextUser().getUserDomain();
        this.username = tenantInfo.getContextUser().getUserName();
        this.password = tenantInfo.getContextUser().getPassword();
    }

    /**
     * Create an Identity Provider.
     *
     * @param idpCreateReqObj Identity Provider request object.
     */
    public String createIdentityProvider(IdentityProviderPOSTRequest idpCreateReqObj) throws Exception {
        String jsonRequest = toJSONString(idpCreateReqObj);
        String endPointUrl = serverUrl + ISIntegrationTest.getTenantedRelativePath(IDENTITY_PROVIDER_BASE_PATH, tenantDomain);

        try (CloseableHttpResponse response = getResponseOfHttpPost(endPointUrl, jsonRequest, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_CREATED,
                    "Idp creation failed");
            JSONObject jsonResponse = getJSONObject(EntityUtils.toString(response.getEntity()));
            return jsonResponse.get("id").toString();
        }
    }

    /**
     * Get an Identity Provider's federated authenticator
     *
     * @param idpId identity provider id.
     * @param federatedAuthenticatorId Federated Authenticator id.
     * @return JSONObject with Federated Authenticator details.
     */
    public JSONObject getIdpFederatedAuthenticator(String idpId, String federatedAuthenticatorId) throws Exception {
        String endPointUrl = serverUrl + ISIntegrationTest.getTenantedRelativePath(IDENTITY_PROVIDER_BASE_PATH,
                tenantDomain) + PATH_SEPARATOR + idpId + FEDERATED_AUTHENTICATORS_PATH + federatedAuthenticatorId;

        try (CloseableHttpResponse response = getResponseOfHttpGet(endPointUrl, getHeaders())) {
            String responseBody = EntityUtils.toString(response.getEntity());
            return getJSONObject(responseBody);
        }
    }

    /**
     * Update an Identity Provider claim configurations.
     *
     * @param idpId Identity Provider Id
     * @param idpClaims Identity Provider claim request object.
     */
    public void updateIdpClaimConfig(String idpId, Claims idpClaims) throws IOException {
        String jsonRequest = toJSONString(idpClaims);
        String endPointUrl = serverUrl + ISIntegrationTest.getTenantedRelativePath(IDENTITY_PROVIDER_BASE_PATH,
                tenantDomain) + PATH_SEPARATOR + idpId + CLAIMS_PATH;

        try (CloseableHttpResponse response = getResponseOfHttpPut(endPointUrl, jsonRequest, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_OK,
                    "Idp claim configurations update, failed");
        }
    }

    /**
     * Delete an Identity Provider.
     *
     * @param idpId Identity Provider Id
     */
    public void deleteIdp(String idpId) throws IOException {
        String endPointUrl = serverUrl + ISIntegrationTest.getTenantedRelativePath(IDENTITY_PROVIDER_BASE_PATH,
                tenantDomain) + PATH_SEPARATOR + idpId;

        try (CloseableHttpResponse response = getResponseOfHttpDelete(endPointUrl, getHeaders())) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpServletResponse.SC_NO_CONTENT,
                    "Idp deletion failed");
        }
    }

    private Header[] getHeaders() {
        Header[] headerList = new Header[3];
        headerList[0] = new BasicHeader(USER_AGENT_ATTRIBUTE, OAuth2Constant.USER_AGENT);
        headerList[1] = new BasicHeader(AUTHORIZATION_ATTRIBUTE, BASIC_AUTHORIZATION_ATTRIBUTE +
                Base64.encodeBase64String((username + ":" + password).getBytes()).trim());
        headerList[2] = new BasicHeader(CONTENT_TYPE_ATTRIBUTE, String.valueOf(ContentType.JSON));

        return headerList;
    }

    /**
     * Close the HTTP client.
     */
    public void closeHttpClient() throws IOException {
        client.close();
    }
}
