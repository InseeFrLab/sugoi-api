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
package fr.insee.sugoi.jms.writer;

import fr.insee.sugoi.jms.exception.BrokerException;
import fr.insee.sugoi.jms.model.BrokerRequest;
import fr.insee.sugoi.jms.model.BrokerResponse;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import javax.jms.JMSException;
import javax.jms.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

@Component
public class JmsWriter {

  private static final Logger logger = LogManager.getLogger(JmsWriter.class);

  @Autowired
  @Qualifier("synchronous")
  JmsTemplate jmsTemplateSynchronous;

  @Autowired
  @Qualifier("asynchronous")
  JmsTemplate jmsTemplateAsynchronous;

  @Autowired MessageConverter messageConverter;

  public String writeRequestInQueueSynchronous(
      String queueName, String methodName, Map<String, Object> methodParams) {
    BrokerRequest request = new BrokerRequest();
    String correlationId = UUID.randomUUID().toString();
    request.setMethod(methodName);
    for (String key : new ArrayList<>(methodParams.keySet())) {
      request.setmethodParams(key, methodParams.get(key));
    }
    request.setCorrelationId(correlationId);
    try {
      jmsTemplateSynchronous.convertAndSend(queueName, request);
      logger.debug(
          "Send synchronous request with correlationID: {}, request: {} in queue {}",
          correlationId,
          request,
          queueName);
      return correlationId;
    } catch (JmsException e) {
      logger.debug(
          "Error when sending synchronous message {} to broker in queue {}", request, queueName);
      throw new BrokerException(e);
    }
  }

  public BrokerResponse checkResponseInQueueSynchronous(String queueName, String correlationId) {
    try {
      Message message =
          jmsTemplateSynchronous.receiveSelected(
              queueName, "JMSCorrelationID= '" + correlationId + "'");
      BrokerResponse response = (BrokerResponse) messageConverter.fromMessage(message);
      // How to get the broker response object from message !!
      return response;
    } catch (JmsException e) {
      throw new RuntimeException(e);
    } catch (JMSException e) {
      throw new RuntimeException(e);
    }
  }

  public String writeRequestInQueueAsynchronous(
      String queueName, String methodName, Map<String, Object> methodParams) {
    BrokerRequest request = new BrokerRequest();
    String correlationId = UUID.randomUUID().toString();
    request.setMethod(methodName);
    for (String key : new ArrayList<>(methodParams.keySet())) {
      request.setmethodParams(key, methodParams.get(key));
    }
    request.setCorrelationId(correlationId);
    try {
      jmsTemplateAsynchronous.convertAndSend(queueName, request);
      logger.debug(
          "Send asynchronous request with correlationID: {}, request: {} in queue {}",
          correlationId,
          request,
          queueName);
      return correlationId;
    } catch (JmsException e) {
      logger.debug(
          "Error when sending asynchronous message {} to broker in queue {}", request, queueName);
      throw new BrokerException(e.getMessage());
    }
  }

  public BrokerResponse checkResponseInQueueAsynchronous(String queueName, String correlationId) {

    try {
      Message message =
          jmsTemplateAsynchronous.receiveSelected(
              queueName, "JMSCorrelationID= '" + correlationId + "'");
      BrokerResponse response = (BrokerResponse) messageConverter.fromMessage(message);
      // How to get the broker response object from message !!
      return response;
    } catch (JmsException e) {
      throw new RuntimeException(e);
    } catch (JMSException e) {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }
  }
}
