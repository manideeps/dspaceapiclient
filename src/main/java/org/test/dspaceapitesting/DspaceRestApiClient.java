package org.test.dspaceapitesting;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DspaceRestApiClient {

    private static final String AUTH_STATUS_URI = "/api/authn/status";
    private static final String AUTH_LOGIN_URI = "/api/authn/login";

    private static final String GET_CONTENT_URI = "/api/core/bitstreams/{id}/content";

    private final RestTemplate restTemplate;
    private final String dspaceApiServer;

    public DspaceRestApiClient(RestTemplate restTemplate, String dspaceApiServer) {
        this.restTemplate = restTemplate;
        this.dspaceApiServer = dspaceApiServer;
    }


    public String doAuthAndGetToken(final String email, final String password) {
        ResponseEntity<String> authStatusResponse = this.restTemplate.getForEntity(dspaceApiServer + AUTH_STATUS_URI, String.class);
        String csrfToken = authStatusResponse.getHeaders().get("DSPACE-XSRF-TOKEN").get(0);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("user", email);
        formData.add("password", password);

        HttpHeaders authRequestHeaders = new HttpHeaders();
        authRequestHeaders.add("X-XSRF-TOKEN", csrfToken);
        authRequestHeaders.addAll(HttpHeaders.COOKIE, authStatusResponse.getHeaders().get("Set-Cookie"));
        authRequestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> authRequest = new HttpEntity<>(formData, authRequestHeaders);
        ResponseEntity<String> authResponse = this.restTemplate.postForEntity(dspaceApiServer + AUTH_LOGIN_URI, authRequest, String.class);

        return authResponse.getHeaders().get("Authorization").get(0);
    }

    public void downloadContent(String bearerToken, String id, OutputStream outputStream) throws IOException {
        Map<String, String> uriVariables = Map.of("id", id);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", bearerToken);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response
                = restTemplate.exchange(dspaceApiServer + GET_CONTENT_URI, HttpMethod.GET, requestEntity, byte[].class, uriVariables);
        outputStream.write(response.getBody());
    }


    public static void main(String[] args) throws Exception {

        String dspaceServer = ""; // Replace with ur dspace server
        DspaceRestApiClient dspaceRestApiClient = new DspaceRestApiClient(restTemplate(), dspaceServer);
        String token = dspaceRestApiClient.doAuthAndGetToken("--Replace with login email--", "-- Replace with your password");
        String fileId = "Replace url file Id";
        String fileNameToSave = "--Replace with Full filepath to save";

        // Ex. C:\Users\dpsaceuser\test-image.jpg
        // String fileNameToSave = "--Replace with Full filepath to save";


        Path filePath = Path.of(fileNameToSave);
        Files.deleteIfExists(filePath);
        Files.createFile(filePath);
        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            dspaceRestApiClient.downloadContent(token, fileId, outputStream);
        }

    }

    	public static RestTemplate restTemplate() throws Exception {
    		TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
 
    		SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                    		.loadTrustMaterial(null, acceptingTrustStrategy)
                    		.build();
 
    		SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
 
    		CloseableHttpClient httpClient = HttpClients.custom()
                    		.setSSLSocketFactory(csf)
                    		.build();
 
    		HttpComponentsClientHttpRequestFactory requestFactory =
                    		new HttpComponentsClientHttpRequestFactory();
 
    		requestFactory.setHttpClient(httpClient);
    		RestTemplate restTemplate = new RestTemplate(requestFactory);
   		return restTemplate;
 	}



}
