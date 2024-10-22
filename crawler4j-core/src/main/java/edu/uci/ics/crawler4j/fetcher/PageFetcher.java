/*-
 * #%L
 * de.hs-heilbronn.mi:crawler4j-core
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
package edu.uci.ics.crawler4j.fetcher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import crawlercommons.filters.basic.BasicURLNormalizer;
import edu.uci.ics.crawler4j.PolitenessServer;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.authentication.AuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.CredentialsProvider;
import edu.uci.ics.crawler4j.crawler.authentication.FormAuthInfo;
import edu.uci.ics.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;
import edu.uci.ics.crawler4j.fetcher.politeness.CachedPolitenessServer;
import edu.uci.ics.crawler4j.url.UrlResolver;
import edu.uci.ics.crawler4j.url.WebURL;

/**
 * @author Yasser Ganjisaffar
 */
public class PageFetcher {
    protected static final Logger logger = LoggerFactory.getLogger(PageFetcher.class);
    protected CrawlConfig config;
    protected BasicURLNormalizer normalizer;
    protected PolitenessServer politenessServer;
    protected PoolingHttpClientConnectionManager connectionManager;
    protected CloseableHttpClient httpClient;
    protected IdleConnectionMonitorThread connectionMonitorThread = null;

    public PageFetcher(CrawlConfig config, BasicURLNormalizer normalizer) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this(config, normalizer, new CachedPolitenessServer(config));
    }

    public PageFetcher(CrawlConfig config, BasicURLNormalizer normalizer, PolitenessServer politenessServer) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        this.config = config;
        this.normalizer = normalizer;
        this.politenessServer = politenessServer;

        RequestConfig requestConfig = RequestConfig.custom()
                .setExpectContinueEnabled(false)
                .setCookieSpec(config.getCookiePolicy())
                .setRedirectsEnabled(false)
                .setResponseTimeout(Timeout.ofMilliseconds(config.getSocketTimeout()))
                .build();

        RegistryBuilder<ConnectionSocketFactory> connRegistryBuilder = RegistryBuilder.create();
        connRegistryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE);
        if (config.isIncludeHttpsPages()) {
            try { // Fixing: https://code.google.com/p/crawler4j/issues/detail?id=174
                // By always trusting the ssl certificate
                SSLContext sslContext =
                        SSLContexts.custom().loadTrustMaterial(null, (chain, authType) -> true).build();
                SSLConnectionSocketFactory sslsf =
                        new SniSSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
                connRegistryBuilder.register("https", sslsf);
            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | RuntimeException e) {
                if (config.isHaltOnError()) {
                    throw e;
                } else {
                    logger.warn("Exception thrown while trying to register https");
                    logger.debug("Stacktrace", e);
                }
            }
        }

        Registry<ConnectionSocketFactory> connRegistry = connRegistryBuilder.build();
        connectionManager =
                new SniPoolingHttpClientConnectionManager(connRegistry, config.getDnsResolver());
        connectionManager.setMaxTotal(config.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(config.getMaxConnectionsPerHost());
        connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(config.getConnectionTimeout()))
                .build());

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        if (config.getCookieStore() != null) {
            clientBuilder.setDefaultCookieStore(config.getCookieStore());
        }
        clientBuilder.setDefaultRequestConfig(requestConfig);
        clientBuilder.setConnectionManager(connectionManager);
        clientBuilder.setUserAgent(config.getUserAgentString());
        clientBuilder.setDefaultHeaders(config.getDefaultHeaders());

        Map<AuthScope, Credentials> credentialsMap = new HashMap<>();
        if (config.getProxyHost() != null) {
            if (config.getProxyUsername() != null) {
                AuthScope authScope = new AuthScope(config.getProxyHost(), config.getProxyPort());
                Credentials credentials = new UsernamePasswordCredentials(config.getProxyUsername(),
                        config.getProxyPassword().toCharArray());
                credentialsMap.put(authScope, credentials);
            }

            HttpHost proxy = new HttpHost(config.getProxyHost(), config.getProxyPort());
            clientBuilder.setProxy(proxy);
            logger.debug("Working through Proxy: {}", proxy.getHostName());
        }

        List<AuthInfo> authInfos = config.getAuthInfos();
        if (authInfos != null) {
            for (AuthInfo authInfo : authInfos) {
                if (authInfo instanceof CredentialsProvider) {
                    CredentialsProvider credentialsProvider = (CredentialsProvider) authInfo;
                    credentialsProvider.addCredentials(credentialsMap);
                }
            }

            if (!credentialsMap.isEmpty()) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsMap.forEach(credentialsProvider::setCredentials);
                clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            httpClient = clientBuilder.build();

            authInfos.stream()
                    .filter(info ->
                            AuthInfo.AuthenticationType.FORM_AUTHENTICATION.equals(info.getAuthenticationType()))
                    .map(FormAuthInfo.class::cast)
                    .forEach(t -> t.doFormLogin(httpClient));
        } else {
            httpClient = clientBuilder.build();
        }

        if (connectionMonitorThread == null) {
            connectionMonitorThread = new IdleConnectionMonitorThread(connectionManager);
        }
        connectionMonitorThread.start();
    }

    public PageFetchResult fetchPage(WebURL webUrl)
            throws InterruptedException, IOException, PageBiggerThanMaxSizeException, URISyntaxException {
        // Getting URL, setting headers & content
        PageFetchResult fetchResult = new PageFetchResult(config.isHaltOnError());
        String toFetchURL = webUrl.getURL();
        HttpUriRequest request = null;
        try {
            request = newHttpUriRequest(toFetchURL);

            final long politenessDelay = politenessServer.applyPoliteness(webUrl);
            if (politenessDelay != CachedPolitenessServer.NO_POLITENESS_APPLIED) {
                Thread.sleep(politenessDelay);
            }

            CloseableHttpResponse response = httpClient.execute(request);
            fetchResult.setEntity(response.getEntity());
            fetchResult.setResponseHeaders(response.getHeaders());

            // Setting HttpStatus
            int statusCode = response.getCode();

            // If Redirect ( 3xx )
            if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY ||
                    statusCode == HttpStatus.SC_MOVED_TEMPORARILY ||
                    statusCode == HttpStatus.SC_MULTIPLE_CHOICES ||
                    statusCode == HttpStatus.SC_SEE_OTHER ||
                    statusCode == HttpStatus.SC_TEMPORARY_REDIRECT ||
                    statusCode == HttpStatus.SC_PERMANENT_REDIRECT) { // todo follow
                // https://issues.apache.org/jira/browse/HTTPCORE-389

                Header header = response.getFirstHeader(HttpHeaders.LOCATION);
                if (header != null) {
                    String movedToUrl = normalizer.filter(UrlResolver.resolveUrl(toFetchURL, header.getValue()));
                    fetchResult.setMovedToUrl(movedToUrl);
                }
            } else if (statusCode >= 200 && statusCode <= 299) { // is 2XX, everything looks ok
                fetchResult.setFetchedUrl(toFetchURL);
                String uri = request.getUri().toString();
                if (!uri.equals(toFetchURL)) {
                    if (!normalizer.filter(uri).equals(toFetchURL)) {
                        fetchResult.setFetchedUrl(uri);
                    }
                }

                // Checking maximum size
                if (fetchResult.getEntity() != null) {
                    long size = fetchResult.getEntity().getContentLength();
                    if (size == -1) {
                        Header length = response.getLastHeader(HttpHeaders.CONTENT_LENGTH);
                        if (length == null) {
                            length = response.getLastHeader("Content-length");
                        }
                        if (length != null) {
                            size = Integer.parseInt(length.getValue());
                        }
                    }
                    if (size > config.getMaxDownloadSize()) {
                        //fix issue #52 - consume entity
                        response.close();
                        throw new PageBiggerThanMaxSizeException(size);
                    }
                }
            }

            fetchResult.setStatusCode(statusCode);
            return fetchResult;

        } finally { // occurs also with thrown exceptions
            if ((fetchResult.getEntity() == null) && (request != null)) {
                request.abort();
            }
        }
    }

    public synchronized void shutDown() {
        if (connectionMonitorThread != null) {
            connectionManager.close();
            connectionMonitorThread.shutdown();
        }
    }

    /**
     * Creates a new HttpUriRequest for the given url. The default is to create a HttpGet without
     * any further configuration. Subclasses may override this method and provide their own logic.
     *
     * @param url the url to be fetched
     * @return the HttpUriRequest for the given url
     */
    protected HttpUriRequest newHttpUriRequest(String url) {
        return new HttpGet(url);
    }

    protected CrawlConfig getConfig() {
        return config;
    }

    protected edu.uci.ics.crawler4j.PolitenessServer getPolitenessServer() {
        return politenessServer;
    }
}
