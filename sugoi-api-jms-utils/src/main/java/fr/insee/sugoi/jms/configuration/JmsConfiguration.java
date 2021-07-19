/*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fr.insee.sugoi.jms.configuration;

import javax.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/** ConsumerConfiguration */
@ConditionalOnProperty(
    name = "fr.insee.sugoi.jms.broker.url",
    havingValue = "",
    matchIfMissing = false)
@Configuration
public class JmsConfiguration {

  private static final Logger logger = LogManager.getLogger(JmsConfiguration.class);

  @Value("${fr.insee.sugoi.jms.broker.url}")
  private String url;

  @Value("${fr.insee.sugoi.jms.broker.username:}")
  private String username;

  @Value("${fr.insee.sugoi.jms.broker.password:}")
  private String password;

  @Value("${fr.insee.sugoi.jms.broker.timeout:5000}")
  private Integer timeout;

  @Bean
  public ActiveMQConnectionFactory connectionFactory() {
    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
    logger.info("Configure jms connection for uri: {}", url);
    connectionFactory.setBrokerURL(url);
    if (username != null) {
      connectionFactory.setUserName(username);
    }
    if (password != null) {
      connectionFactory.setPassword(password);
    }

    return connectionFactory;
  }

  @Bean
  @Qualifier("asynchronous")
  public JmsTemplate getJmsTemplate() {
    JmsTemplate template = new JmsTemplate();
    template.setConnectionFactory(connectionFactory());
    template.setMessageConverter(messageConverter());
    return template;
  }

  @Bean
  @Qualifier("synchronous")
  public JmsTemplate JmsTemplateWithTimeout() {
    JmsTemplate template = new JmsTemplate();
    template.setConnectionFactory(connectionFactory());
    template.setMessageConverter(messageConverter());
    template.setReceiveTimeout(timeout);
    return template;
  }

  @Bean
  public JmsListenerContainerFactory<?> myFactory(
      ConnectionFactory connectionFactory,
      DefaultJmsListenerContainerFactoryConfigurer configurer) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    factory.setMessageConverter(messageConverter());
    configurer.configure(factory, connectionFactory);
    return factory;
  }

  @Bean
  public static MessageConverter messageConverter() {
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    converter.setTargetType(MessageType.TEXT);
    converter.setTypeIdPropertyName("_type");
    return converter;
  }
}
