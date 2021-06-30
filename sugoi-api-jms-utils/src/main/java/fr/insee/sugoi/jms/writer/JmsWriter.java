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

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;

import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import fr.insee.sugoi.jms.exception.BrokerException;
import fr.insee.sugoi.jms.model.BrokerRequest;
import fr.insee.sugoi.jms.model.BrokerResponse;

@Component
public class JmsWriter {

  private static final Logger logger = LogManager.getLogger(JmsWriter.class);

  @Autowired
  @Qualifier("synchronous")
  JmsTemplate jmsTemplateSynchronous;

  @Autowired
  @Qualifier("asynchronous")
  JmsTemplate jmsTemplateAsynchronous;

  public String writeRequestInQueueSynchronous(String queueName, String methodName, Map<String, Object> methodParams) {
    BrokerRequest request = new BrokerRequest();
    String correlationId = UUID.randomUUID().toString();
    request.setMethod(methodName);
    for (String key : new ArrayList<>(methodParams.keySet())) {
      request.setmethodParams(key, methodParams.get(key));
    }
    try {
      ActiveMQObjectMessage ac = new ActiveMQObjectMessage();
      ac.setObject(request);
      jmsTemplateSynchronous.convertAndSend(queueName, ac, m -> {
        m.setJMSCorrelationID(correlationId);
        return m;
      });
      logger.debug("Send synchronous request with correlationID: {}, request: {} in queue {}", ac.getJMSCorrelationID(),
          request, queueName);
      return correlationId;
    } catch (JMSException e) {
      logger.debug("Error when sending synchronous message {} to broker in queue {}", request, queueName);
      throw new BrokerException(e.getMessage());
    }
  }

  public BrokerResponse checkResponseInQueueSynchronous(String queueName, String correlationId) {
    try {
      return (BrokerResponse) ((ActiveMQObjectMessage) jmsTemplateSynchronous.receiveSelected(queueName,
          "JMSCorrelationID= '" + correlationId + "'")).getObject();
    } catch (JmsException | JMSException e) {
      throw new RuntimeException(e);
    }
  }

  public String writeRequestInQueueAsynchronous(String queueName, String methodName, Map<String, Object> methodParams) {
    BrokerRequest request = new BrokerRequest();
    String correlationId = UUID.randomUUID().toString();
    request.setMethod(methodName);
    for (String key : new ArrayList<>(methodParams.keySet())) {
      request.setmethodParams(key, methodParams.get(key));
    }
    try {
      ActiveMQObjectMessage ac = new ActiveMQObjectMessage();
      ac.setObject(request);
      jmsTemplateAsynchronous.convertAndSend(queueName, ac, m -> {
        m.setJMSCorrelationID(correlationId);
        return m;
      });
      logger.debug("Send asynchronous request with correlationID: {}, request: {} in queue {}",
          ac.getJMSCorrelationID(), request, queueName);
      return correlationId;
    } catch (JMSException e) {
      logger.debug("Error when sending asynchronous message {} to broker in queue {}", request, queueName);
      throw new BrokerException(e.getMessage());
    }
  }

  public BrokerResponse checkResponseInQueueAsynchronous(String queueName, String correlationId) {

    try {
      return (BrokerResponse) ((ActiveMQObjectMessage) jmsTemplateAsynchronous.receiveSelected(queueName,
          "JMSCorrelationID= '" + correlationId + "'")).getObject();
    } catch (JmsException | JMSException e) {
      throw new RuntimeException(e);
    }
  }
}
