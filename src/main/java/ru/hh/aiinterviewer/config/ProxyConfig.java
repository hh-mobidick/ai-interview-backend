package ru.hh.aiinterviewer.config;

import jakarta.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;

@Configuration
@ConditionalOnProperty(name = "proxy")
public class ProxyConfig {

  private final String proxyHost;
  private final int proxyPort;

  public ProxyConfig(@Value("${proxy}") String proxy) {
    this.proxyHost = getProxyHost(proxy);
    this.proxyPort = getProxyPort(proxy);
  }

  @PostConstruct
  public void setupSystemProxy() {
    System.setProperty("http.proxyHost", proxyHost);
    System.setProperty("http.proxyPort", String.valueOf(proxyPort));
    System.setProperty("https.proxyHost", proxyHost);
    System.setProperty("https.proxyPort", String.valueOf(proxyPort));
    System.setProperty("http.nonProxyHosts", String.join("|", "*.ru", "*.hh.ru"));
  }

  @Bean
  public WebClientCustomizer openAiProxyCustomizer() {
    ProxySelector selector = new ConditionalProxySelector(
        new InetSocketAddress(proxyHost, proxyPort),
        List.of()
    );
    HttpClient httpClient = HttpClient.newBuilder().proxy(selector).build();
    JdkClientHttpConnector connector = new JdkClientHttpConnector(httpClient);
    return builder -> builder.clientConnector(connector);
  }

  @Bean
  public RestClientCustomizer openAiRestProxyCustomizer() {
    ProxySelector selector = new ConditionalProxySelector(
        new InetSocketAddress(proxyHost, proxyPort),
        List.of("*.ru", "*.hh.ru")
    );
    HttpClient httpClient = HttpClient.newBuilder().proxy(selector).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    return builder -> builder.requestFactory(requestFactory);
  }

  private static String getProxyHost(String proxy) {
    return proxy.split(":")[0];
  }

  private static int getProxyPort(String proxy) {
    return Integer.parseInt(proxy.split(":")[1]);
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


