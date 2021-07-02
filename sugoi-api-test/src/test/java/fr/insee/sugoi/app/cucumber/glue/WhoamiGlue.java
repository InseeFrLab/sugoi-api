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
package fr.insee.sugoi.app.cucumber.glue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.sugoi.app.cucumber.utils.StepData;
import fr.insee.sugoi.app.cucumber.utils.WhoamiView;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;

public class WhoamiGlue {
  private StepData stepData;

  private Scenario scenario;

  @Before
  public void before(Scenario scenario) {
    this.scenario = scenario;
  }

  public WhoamiGlue(StepData stepData) {
    this.stepData = stepData;
  }

  @Then("the client expect to receive his rights")
  public void expect_to_receive_his_rights() {
    ObjectMapper mapper = new ObjectMapper();
    try {
      mapper.readValue(stepData.getLatestResponse().getBody(), WhoamiView.class);
      assertThat("Data receive is a whoamiView", true, is(true));
    } catch (JsonProcessingException e) {
      assertThat("Data receive is a whoamiView", false, is(true));
      e.printStackTrace();
    }
  }
}
