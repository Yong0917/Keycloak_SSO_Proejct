package com.test.keycloak.api;

import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.test.keycloak.vo.*;
import lombok.SneakyThrows;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.Map;

@Service
public class KeycloakApi {


    @Value("${auth.server}")
    private String server;

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${auth.id}")
    private String authId;

    @Value("${auth.password}")
    private String authPassword;

    @Value("${auth.realm}")
    private String authRealm;

    @Value("${auth.clientId}")
    private String authClientId;

//    private static final Logger log = LoggerFactory.getLogger(KeycloakApi.class);
    private static final Logger log = LoggerFactory.getLogger(KeycloakApi.class);   //??????????????????

    //????????? ?????????(admin)
    public Object userList() {

        String token = getAccessToken();        //master AccessToken??? ??????

        String authServerUrl2 = server;

        HttpHeaders headers = getHeaders(token);        //header??? ?????? ?????????

        RestTemplate restTemplate = getRestTemplate();      //ssl ???????????? ?????? RestTemplate ??????

        Map<String, Object> getUserList = new HashMap<>();

        Gson var = new Gson();
        String json = var.toJson(getUserList);

        HttpEntity<String> entity = new HttpEntity<>(json,headers);

        try {
            ResponseEntity<String> result = restTemplate.exchange(authServerUrl2, HttpMethod.GET,entity,String.class);      //GET???????????? ??????
            return result;
        } catch (RestClientException e) {
            log.error("keycloakError -> " + e);
            return "ERROR";
        }
    }


    //????????? ??????(admin)
    public String createUser(userCreateForm param) {

        String token = getAccessToken();  //master AccessToken??? ??????

        String authServerUrl2 = server;

        HttpHeaders headers = getHeaders(token);  //Header??? ?????? ?????????

        RestTemplate restTemplate = getRestTemplate();  //ssl ???????????? ?????? RestTemplate ??????

        JsonObject password = new JsonObject();
        password.addProperty("temporary","false");      //?????? ???????????? ???????????? ?????? ????????????
        password.addProperty("type","password");
        password.addProperty("value",param.getPassword());

        JsonArray passwordArray = new JsonArray();
        passwordArray.add(password);        //passwordArray ??????

        Map<String, Object> registerUser = new HashMap<>();
        registerUser.put("username", param.getId());
        registerUser.put("firstName", param.getFirstName());
        registerUser.put("lastName", param.getLastName());
        registerUser.put("email",param.getEmail());
        if(param.getUseEnable().equals("enable"))
            registerUser.put("enabled", "true");
        else
            registerUser.put("enabled", "false");
        registerUser.put("credentials",passwordArray);      //?????? password ?????? ??????

        Gson var = new Gson();
        String json = var.toJson(registerUser);

        HttpEntity<String> entity = new HttpEntity<>(json,headers);

        try {
            restTemplate.exchange(authServerUrl2, HttpMethod.POST,entity,String.class);     //POST???????????? API ??????
            return "SUCCESS";
        } catch (RestClientException e) {
            log.error("keycloakError -> " + e.getMessage());
            if(e.getMessage().contains("409"))
                return "alreadyEmail";
            else
                return "ERROR";
        }

    }

    //????????? ??????(admin)
    public String modifyUser(userUpdateForm param) {

        String token = getAccessToken();        ////master AccessToken??? ??????

        String authServerUrl2 = server +  "/" + param.getId();

        HttpHeaders headers = getHeaders(token);        //Header??? ?????? ?????????

        RestTemplate restTemplate = getRestTemplate();      //ssl ???????????? ?????? RestTemplate ??????

        Map<String, Object> modifyUser = new HashMap<>();
        modifyUser.put("firstName", param.getFirstName());
        modifyUser.put("lastName", param.getLastName());
        modifyUser.put("email", param.getEmail());
        if(param.getUseEnable().equals("enable"))
            modifyUser.put("enabled", "true");
        else
            modifyUser.put("enabled", "false");

        Gson var = new Gson();
        String json = var.toJson(modifyUser);

        HttpEntity<String> entity = new HttpEntity<>(json,headers);

        try {
            restTemplate.exchange(authServerUrl2, HttpMethod.PUT,entity,String.class);      //PUT???????????? ??????
            return "SUCCESS";
        } catch (RestClientException e) {
            log.error("keycloakError -> " + e.getMessage());
            if(e.getMessage().contains("409"))
                return "alreadyEmail";
            else
                return "ERROR";
        }
    }

    //????????? ??????(admin)
    public String deleteUser(String id) {

        String token = getAccessToken();        //master AccessToken??? ??????

        String authServerUrl2 = server + "/" + id;

        HttpHeaders headers = getHeaders(token);        //Header??? ?????? ?????????

        RestTemplate restTemplate = getRestTemplate();      //ssl ???????????? ?????? RestTemplate ??????

        Map<String, Object> deleteUser = new HashMap<>();
        deleteUser.put("enabled", "false");

        Gson var = new Gson();
        String json = var.toJson(deleteUser);

        HttpEntity<String> entity = new HttpEntity<>(json,headers);

        try {
            restTemplate.exchange(authServerUrl2, HttpMethod.PUT,entity,String.class);      //PUT???????????? ??????
            return "SUCCESS";
        } catch (RestClientException e) {
            log.error("keycloakError -> " + e.getMessage());
            return "ERROR";
        }

    }

    //????????? ????????????(admin)
    //keycloak????????? api??? ????????? ?????? ????????? ???????????? ???????????? ???????????? ???????????? ??????
    public String duplicateCheck(UserVO param) throws ParseException {

        String token = getAccessToken();        //master AccessToken??? ??????

        String authServerUrl2 = server;

        HttpHeaders headers = getHeaders(token);        //Header??? ?????? ?????????

        RestTemplate restTemplate = getRestTemplate();      //ssl ???????????? ?????? RestTemplate ??????

        Map<String, Object> duplicateUser = new HashMap<>();
        Gson var = new Gson();
        String json = var.toJson(duplicateUser);

        HttpEntity<String> entity = new HttpEntity<>(json,headers);
        JSONParser jsonparser2 = new JSONParser();

        try {

            String result = "SUCCESS";

            String userList = restTemplate.exchange(authServerUrl2, HttpMethod.GET,entity,String.class).getBody();      //userlist ??????
            JSONArray arr = (JSONArray) jsonparser2.parse(userList);
            for(int i = 0; i < arr.toArray().length; i++){      //username??? ????????? ????????? ???????????? ?????? ????????? DUPLICATE ??????
                JSONObject obj = (JSONObject) arr.get(i);
                if(param.getId().equals(obj.get("username"))){
                    result = "Duplicate";
                    break;
                }
            }
            return result;

        } catch (RestClientException e) {
            log.error("keycloakError -> " + e.getMessage());
            if(e.getMessage().contains("409"))
                return "alreadyId";
            else
                return "ERROR";
        }
    }

    //??? ?????? ?????????
    public UserVO userInfoList(String uuid, String userName) {

        String token = getAccessToken();        //master AccessToken??? ??????

        String authServerUrl2 = server + "/" + uuid;

        HttpHeaders headers = getHeaders(token);        //Header??? ?????? ?????????

        RestTemplate restTemplate = getRestTemplate();      //ssl ???????????? ?????? RestTemplate ??????

        Map<String, Object> modifyInfo = new HashMap<>();
        modifyInfo.put("search", userName);

        Gson var = new Gson();
        String json = var.toJson(modifyInfo);

        HttpEntity<String> entity = new HttpEntity<>(json,headers);
        JSONParser jsonparser2 = new JSONParser();
        UserVO userVO = new UserVO();

        try {
            String result = restTemplate.exchange(authServerUrl2, HttpMethod.GET, entity, String.class).getBody();      //GET???????????? ??????

            JSONObject obj = (JSONObject) jsonparser2.parse(result);

            //userVO ??????
            userVO.setId(String.valueOf(obj.get("username")));
            userVO.setFirstName(String.valueOf(obj.get("firstName")));
            userVO.setLastName(String.valueOf(obj.get("lastName")));
            userVO.setEmail(String.valueOf(obj.get("email")));


            return userVO;

        } catch (RestClientException e) {
            e.printStackTrace();
            log.error("keycloakError -> " + e.getMessage());
        } catch (ParseException e) {
            e.printStackTrace();
            log.error("keycloakError -> " + e.getMessage());
        }


        return userVO;
    }


    //????????? ??????
    public String ModifyInfo(myInfoForm param, String uuid){

        String token = getAccessToken();        //master AccessToken??? ??????

        String authServerUrl2 = server + "/" + uuid;

        HttpHeaders headers = getHeaders(token);        //Header??? ?????? ?????????

        RestTemplate restTemplate = getRestTemplate();      //ssl ???????????? ?????? RestTemplate ??????

        Map<String, Object> modifyInfo = new HashMap<>();

        modifyInfo.put("firstName", param.getFirstName());
        modifyInfo.put("lastName", param.getLastName());
        modifyInfo.put("email", param.getEmail());

        Gson var = new Gson();
        String json = var.toJson(modifyInfo);

        HttpEntity<String> entity = new HttpEntity<>(json,headers);

        try {
            restTemplate.exchange(authServerUrl2, HttpMethod.PUT,entity,String.class);      //PUT???????????? ??????
            return "SUCCESS";
        } catch (RestClientException e) {
            log.error("keycloakError -> " + e.getMessage());
            if(e.getMessage().contains("409"))
                return "alreadyEmail";
            else
                return "ERROR";

        }
    }

    //??? ???????????? ??????
    public String ModifyPassword(pwInfoForm param, String uuid) {

        String token = getAccessToken();        //master AccessToken??? ??????

        String authServerUrl2 = server + "/" + uuid + "/reset-password";

        HttpHeaders headers = getHeaders(token);        //Header??? ?????? ?????????

        RestTemplate restTemplate = getRestTemplate();      //ssl ???????????? ?????? RestTemplate ??????

        Map<String, Object> modifyPassword = new HashMap<>();
        modifyPassword.put("temporary", "false");       //????????? ??????????????? ???????????? ??????????????????
        modifyPassword.put("type", "PASSWORD");
        modifyPassword.put("value", param.getPassword());

        Gson var = new Gson();
        String json = var.toJson(modifyPassword);

        HttpEntity<String> entity = new HttpEntity<>(json,headers);

        try {
            restTemplate.exchange(authServerUrl2, HttpMethod.PUT,entity,String.class);      //PUT???????????? ??????

            return "SUCCESS";
        } catch (RestClientException e) {
            log.error("keycloakError -> " + e.getMessage());
            return "ERROR";
        }

    }

    //token ???
    public String getAccessToken(){

        Keycloak keycloak = KeycloakBuilder.builder().serverUrl(serverUrl)
                .grantType(OAuth2Constants.PASSWORD).realm(authRealm).clientId(authClientId)
                .username(authId).password(authPassword)
                .resteasyClient(new ResteasyClientBuilder().connectionPoolSize(10).disableTrustManager().build()).build();

        String token = keycloak.tokenManager().getAccessTokenString();      // ??????????????? master ????????? Access token??? ?????? ??? ??????.
        return token;
    }

    private HttpHeaders getHeaders(String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);         // HEADER??? JSON????????????
        headers.set("Authorization", "Bearer " + token);        //header??? ??????????????? ???????????????

        return headers;
    }

    @SneakyThrows
    public RestTemplate getRestTemplate(){

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return restTemplate;

    }

}
