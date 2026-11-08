package vn.luma.config;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.*;

@Configuration
public class OAuthCauHinh {
    @Value("${app.oauth.enabled:false}") boolean enabled;
    @Value("${app.oauth.google.client-id:}") String googleId;
    @Value("${app.oauth.google.client-secret:}") String googleSecret;
    @Value("${app.oauth.facebook.client-id:}") String facebookId;
    @Value("${app.oauth.facebook.client-secret:}") String facebookSecret;
    @Value("${app.oauth.facebook.api-version:}") String facebookVersion;
    public List<String> nhaCungCap(){if(!enabled)return List.of();var result=new ArrayList<String>();if(!facebookId.isBlank()&&!facebookSecret.isBlank())result.add("facebook");return List.copyOf(result);}
    @Bean @ConditionalOnProperty(name="app.oauth.enabled",havingValue="true")
    ClientRegistrationRepository oauthRegistrations(){
        var registrations=new ArrayList<ClientRegistration>();
        if(googleId.isBlank()!=googleSecret.isBlank()||facebookId.isBlank()!=facebookSecret.isBlank())throw new IllegalStateException("Mỗi nhà cung cấp OAuth cần đủ Client ID và Client Secret.");
        if(!googleId.isBlank())registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId(googleId).clientSecret(googleSecret).scope("openid","profile","email").build());
        if(!facebookId.isBlank()){
            if(!facebookVersion.matches("v[0-9]{1,3}\\.0"))throw new IllegalStateException("Điền phiên bản Graph API được Meta hỗ trợ trong app.oauth.facebook.api-version.");
            registrations.add(CommonOAuth2Provider.FACEBOOK.getBuilder("facebook").clientId(facebookId).clientSecret(facebookSecret).scope("public_profile","email")
                .authorizationUri("https://www.facebook.com/"+facebookVersion+"/dialog/oauth").tokenUri("https://graph.facebook.com/"+facebookVersion+"/oauth/access_token").userInfoUri("https://graph.facebook.com/"+facebookVersion+"/me?fields=id,name,email").build());
        }
        if(registrations.isEmpty())throw new IllegalStateException("Đã bật OAuth nhưng chưa cấu hình nhà cung cấp.");return new InMemoryClientRegistrationRepository(registrations);
    }
    @Bean @ConditionalOnProperty(name="app.oauth.enabled",havingValue="true")
    OAuth2AuthorizedClientService oauthClients(ClientRegistrationRepository repository){return new InMemoryOAuth2AuthorizedClientService(repository);}
}
