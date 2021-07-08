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
package fr.insee.sugoi.jms;

import fr.insee.sugoi.jms.writer.JmsWriter;
import fr.insee.sugoi.model.Realm;
import fr.insee.sugoi.model.UserStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

@Configuration
public class JmsStoreBeans {

  @Autowired JmsWriter jmsWriter;

  @Value("${fr.insee.sugoi.jms.queue.requests.name:}")
  private String queueRequestName;

  @Value("${fr.insee.sugoi.jms.queue.response.name:}")
  private String queueResponseName;

  @Value("${fr.insee.sugoi.jms.queue.requests.name:}")
  private String queueUrgentRequestName;

  @Value("${fr.insee.sugoi.jms.queue.response.name:}")
  private String queueUrgentResponseName;

  @Bean("JMSWriterStore")
  @Lazy
  @Scope("prototype")
  public JmsWriterStore JmsWriterStore(Realm realm, UserStorage userStorage) {
    return new JmsWriterStore(
        jmsWriter,
        queueRequestName,
        queueResponseName,
        queueUrgentRequestName,
        queueUrgentResponseName,
        realm,
        userStorage);
  }
}
