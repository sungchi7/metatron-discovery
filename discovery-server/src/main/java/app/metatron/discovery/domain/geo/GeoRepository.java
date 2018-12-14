/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specic language governing permissions and
 * limitations under the License.
 */

package app.metatron.discovery.domain.geo;

import com.google.common.collect.Lists;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import app.metatron.discovery.common.GlobalObjectMapper;
import app.metatron.discovery.domain.datasource.data.QueryTimeExcetpion;
import app.metatron.discovery.domain.engine.EngineException;

@Component
public class GeoRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoRepository.class);

  @Value("${polaris.geoserver.timeout:120000}")
  Integer timeout;

  @Autowired
  GeoServerProperties geoServerProperties;

  RestTemplate restTemplate;

  @PostConstruct
  private void setUpRestTemplate() {

    StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
    stringHttpMessageConverter.setWriteAcceptCharset(false);

    List<HttpMessageConverter<?>> converters = Lists.newArrayList(
        stringHttpMessageConverter,
        new MappingJackson2HttpMessageConverter(GlobalObjectMapper.getDefaultMapper()));


    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(timeout);
    factory.setReadTimeout(timeout);

    restTemplate = new RestTemplate(converters);
    restTemplate.setRequestFactory(factory);
    restTemplate.setErrorHandler(new GeoResponseErrorHandler());

    if (StringUtils.isNotEmpty(geoServerProperties.getUsername())) {
      restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(geoServerProperties.getUsername(),
                                                                           geoServerProperties.getPassword()));
    }
  }

  public String create(String url, String requestBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

    Optional<String> result = call(url, HttpMethod.POST, entity, String.class);

    return result.orElse("Result is empty");
  }

  public String delete(String url) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> entity = new HttpEntity<>(null, headers);

    Optional<String> result = call(url, HttpMethod.DELETE, entity, String.class);

    return result.orElse("Result is empty");
  }

  public String query(String requestBody, String viewParam) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_XML);
    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

    String newUrl = null;
    try {
      newUrl = geoServerProperties.getWfsUrl() + "?viewParams=" + URLEncoder.encode(viewParam, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    Optional<String> result = call(newUrl, HttpMethod.POST, entity, String.class);

    return result.orElseThrow(() -> new EngineException("Result not found."));
  }

  public <T> T query(String requestBody, Class<T> clz) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_XML);
    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

    Optional<T> result = call(geoServerProperties.getWfsUrl(), HttpMethod.POST, entity, clz);

    return result.orElseThrow(() -> new EngineException("Result not found."));
  }

  private <T> Optional<T> call(String url, HttpMethod method, HttpEntity<?> entity, Class<T> clazz) {

    LOGGER.debug("Request to geoserver : {}, {} > {}", method, url, entity == null ? "{}" : entity.getBody());

    ResponseEntity<T> result;
    try {
      result = restTemplate.exchange(url, method, entity, clazz);
    } catch (ResourceAccessException e) {
      LOGGER.error("Fail to access Geoserver : {}", e.getMessage());
      throw new EngineException("Fail to access Geoserver : " + e.getMessage(), e);
    } catch (Exception e) {
      LOGGER.error("Fail to process response : {}", e.getMessage());
      throw new EngineException("Fail to process response : " + e.getMessage(), e);
    }

    return Optional.ofNullable(result.getBody());
  }


  private class GeoResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
      if (response.getStatusCode() == HttpStatus.OK
          || response.getStatusCode() == HttpStatus.NO_CONTENT
          || response.getStatusCode() == HttpStatus.CREATED) {
        return false;
      }

      return true;
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
      throw new QueryTimeExcetpion(String.valueOf(IOUtils.readLines(response.getBody())));
    }
  }
}
