package ru.hh.aiinterviewer.config;

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.Proxy;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import jakarta.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(name = "proxy", havingValue = "true")
public class ProxyConfig {

  @PostConstruct
  public void setupSystemProxy() {
    // Global fallback to ensure all HTTP stacks (including ones we don't control) use the proxy
    System.setProperty("http.proxyHost", "fwdproxy.pyn.ru");
    System.setProperty("http.proxyPort", String.valueOf(4443));
    System.setProperty("https.proxyHost", "fwdproxy.pyn.ru");
    System.setProperty("https.proxyPort", String.valueOf(4443));
    // Bypass similar to FoxyProxy config
    String nonProxy = String.join("|",
        "*.ru",
        "*.hh.ru",
        "*.pyn.ru"
    );
    System.setProperty("http.nonProxyHosts", nonProxy);
  }

  @Bean
  public WebClientCustomizer openAiProxyCustomizer() {
    ProxySelector selector = new ConditionalProxySelector(
        new InetSocketAddress("fwdproxy.pyn.ru", 4443),
        List.of()
    );
    HttpClient httpClient = HttpClient.newBuilder().proxy(selector).build();
    JdkClientHttpConnector connector = new JdkClientHttpConnector(httpClient);
    return builder -> builder.clientConnector(connector);
  }

  @Bean
  public RestClientCustomizer openAiRestProxyCustomizer() {
    ProxySelector selector = new ConditionalProxySelector(
        new InetSocketAddress("fwdproxy.pyn.ru", 4443),
        List.of(
            "*.ru",
            "*.hh.ru",
            "*.pyn.ru"
        )
    );
    HttpClient httpClient = HttpClient.newBuilder().proxy(selector).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    return builder -> builder.requestFactory(requestFactory);
  }

  static class ConditionalProxySelector extends ProxySelector {
    private final InetSocketAddress proxyAddress;
    private final List<String> bypassPatterns;

    ConditionalProxySelector(InetSocketAddress proxyAddress, List<String> bypassPatterns) {
      this.proxyAddress = proxyAddress;
      this.bypassPatterns = bypassPatterns;
    }

    @Override
    public List<Proxy> select(URI uri) {
      String host = uri != null ? uri.getHost() : null;
      if (host == null || shouldBypass(host)) {
        return List.of(Proxy.NO_PROXY);
      }
      return List.of(new Proxy(Proxy.Type.HTTP, proxyAddress));
    }

    private boolean shouldBypass(String host) {
      String h = host.toLowerCase(Locale.ROOT);
      for (String pattern : bypassPatterns) {
        String p = pattern.toLowerCase(Locale.ROOT);
        // support patterns like *.domain.tld and bare domains
        if (p.startsWith("*.") && h.endsWith(p.substring(1))) {
          return true;
        }
        if (!p.startsWith("*.") && (h.equals(p) || h.endsWith("." + p))) {
          return true;
        }
      }
      return false;
    }

    @Override
    public void connectFailed(URI uri, java.net.SocketAddress sa, java.io.IOException ioe) {
      // no-op: let WebClient handle errors
    }
  }
}


