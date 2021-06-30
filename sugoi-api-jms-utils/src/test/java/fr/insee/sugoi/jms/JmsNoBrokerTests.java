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

import fr.insee.sugoi.jms.exception.BrokerException;
import fr.insee.sugoi.jms.writer.JmsWriter;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("withoutBroker")
public class JmsNoBrokerTests {

  @Autowired private JmsWriter jmsWriter;

  @Test
  public void testWithNoBroker() throws Exception {
    Assertions.assertThrows(
        BrokerException.class,
        () -> {
          Map<String, Object> params = new HashMap<>();
          jmsWriter.writeRequestInQueueSynchronous("queueTests", "toto", params);
        });
  }
}
