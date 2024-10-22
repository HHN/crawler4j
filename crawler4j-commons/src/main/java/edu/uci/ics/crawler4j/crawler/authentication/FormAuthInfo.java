/*-
 * #%L
 * de.hs-heilbronn.mi:crawler4j-commons
 * %%
 * Copyright (C) 2010 - 2021 crawler4j-fork (pre-fork: Yasser Ganjisaffar)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package edu.uci.ics.crawler4j.crawler.authentication;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.html.FormSubmitEvent.MethodType;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Avi Hayun on 11/25/2014.
 *
 * FormAuthInfo contains the authentication information needed for FORM authentication (extending
 * AuthInfo which has
 * all common auth info in it)
 * Basically, this is the most common authentication, where you will get to a site and you will
 * need to enter a
 * username and password into an HTML form
 */
public class FormAuthInfo extends AuthInfo {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FormAuthInfo.class);
    
    private String usernameFormStr;
    private String passwordFormStr;

    /**
     * Constructor
     *
     * @param username Username to login with
     * @param password Password to login with
     * @param loginUrl Full login URL, starting with "http"... ending with the full URL
     * @param usernameFormStr "Name" attribute of the username form field
     * @param passwordFormStr "Name" attribute of the password form field
     */
    public FormAuthInfo(String username, String password, String loginUrl, String usernameFormStr,
                        String passwordFormStr) {
        super(AuthenticationType.FORM_AUTHENTICATION, MethodType.POST, loginUrl, username, password);

        this.usernameFormStr = usernameFormStr;
        this.passwordFormStr = passwordFormStr;
    }

    /**
     * @return username html "name" form attribute
     */
    public String getUsernameFormStr() {
        return usernameFormStr;
    }

    /**
     * @param usernameFormStr username html "name" form attribute
     */
    public void setUsernameFormStr(String usernameFormStr) {
        this.usernameFormStr = usernameFormStr;
    }

    /**
     * @return password html "name" form attribute
     */
    public String getPasswordFormStr() {
        return passwordFormStr;
    }

    /**
     * @param passwordFormStr password html "name" form attribute
     */
    public void setPasswordFormStr(String passwordFormStr) {
        this.passwordFormStr = passwordFormStr;
    }
    
    
    /**
     * FORM authentication<br/>
     * Official Example: https://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org
     * /apache/http/examples/client/ClientFormLogin.java
     */
    public void doFormLogin(final CloseableHttpClient httpClient) {
        LOGGER.info("FORM authentication for: {}", getLoginTarget());
        String fullUri = getProtocol() + "://" + getHost() + ":" + getPort() + getLoginTarget();
        HttpPost httpPost = new HttpPost(fullUri);
        List<NameValuePair> formParams = createFormParams();
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8);
        httpPost.setEntity(entity);

        try {
            httpClient.execute(httpPost, new BasicHttpClientResponseHandler());
            LOGGER.debug("Successfully request to login in with user: {} to: {}", getUsername(), getHost());
        } catch (ClientProtocolException e) {
            LOGGER.error("While trying to login to: {} - Client protocol not supported", getHost(), e);
        } catch (IOException e) {
            LOGGER.error("While trying to login to: {} - Error making request", getHost(), e);
        }
    }

    /**
     * Open for extension.
     */
    protected List<NameValuePair> createFormParams() {
        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair(getUsernameFormStr(), getUsername()));
        formParams.add(new BasicNameValuePair(getPasswordFormStr(), getPassword()));
        return formParams;
    }
}
