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
import fr.insee.sugoi.model.Group;
import fr.insee.sugoi.model.paging.PageResult;
import io.cucumber.java.en.Then;
import java.util.stream.Collectors;

public class GroupGlue {

  private StepData stepData;

  public GroupGlue(StepData stepData) {
    this.stepData = stepData;
  }

  @Then("the client expect to receive a list of groups")
  @SuppressWarnings("unchecked")
  public void expect_to_receive_a_list_of_groups() {
    Boolean isGroups = false;
    ObjectMapper mapper = new ObjectMapper();
    try {
      PageResult<Group> groups =
          mapper.readValue(stepData.getLatestResponse().getBody(), PageResult.class);
      stepData.setGroups(groups.getResults());
      isGroups = true;
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    assertThat("Data receive is a list of groups", isGroups, is(true));
  }

  @Then("the client expect to receive a group")
  public void expect_to_receive_a_group() {
    Boolean isGroup = false;
    ObjectMapper mapper = new ObjectMapper();
    Group group;
    try {
      group = mapper.readValue(stepData.getLatestResponse().getBody(), Group.class);
      stepData.setGroup(group);
      isGroup = true;
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    assertThat("Data receive is a group", isGroup, is(true));
  }

  @Then("the client expect the name of group to be {}")
  public void expect_name_of_group_to_be(String groupName) {
    assertThat("name of group is", stepData.getGroup().getName(), is(groupName));
  }

  @Then("the client expect the group contains user {}")
  public void expect_group_contains(String user) {
    Boolean in =
        stepData.getGroup().getUsers().stream()
            .map(u -> u.getUsername().toUpperCase())
            .collect(Collectors.toList())
            .contains(user);
    assertThat("the client expect the group contains user " + user, in, is(true));
  }
}
