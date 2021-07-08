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
package fr.insee.sugoi.core.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import fr.insee.sugoi.core.event.publisher.SugoiEventPublisher;
import fr.insee.sugoi.core.realm.RealmProvider;
import fr.insee.sugoi.core.service.impl.UserServiceImpl;
import fr.insee.sugoi.core.store.ReaderStore;
import fr.insee.sugoi.core.store.StoreProvider;
import fr.insee.sugoi.model.Realm;
import fr.insee.sugoi.model.User;
import fr.insee.sugoi.model.UserStorage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = UserServiceImpl.class)
@TestPropertySource(locations = "classpath:/application.properties")
public class UserServiceTest {

  @MockBean private StoreProvider storeProvider;

  @MockBean private RealmProvider realmProvider;

  @MockBean private SugoiEventPublisher sugoiEventPublisher;

  @MockBean private ReaderStore readerStore;

  @Autowired private UserServiceImpl userService;

  private User user1;

  private Realm realm;

  @BeforeEach
  public void setup() {
    user1 = new User();
    user1.setUsername("Toto");
    user1.setMail("toto@insee.fr");

    realm = new Realm();
    realm.setName("realm");
    UserStorage us1 = new UserStorage();
    us1.setName("us1");
    UserStorage us2 = new UserStorage();
    us2.setName("us2");
    realm.setUserStorages(List.of(us1, us2));
  }

  @Test
  public void getUserOnMultipleStorage() {
    Mockito.when(realmProvider.load("realm")).thenReturn(Optional.of(realm));
    Mockito.when(storeProvider.getReaderStore(Mockito.any(), Mockito.any()))
        .thenReturn(readerStore);
    Mockito.when(readerStore.getUser(Mockito.anyString()))
        .thenThrow(new RuntimeException("lol"))
        .thenReturn(user1);
    assertThat("get user Toto", userService.findById("realm", null, "Toto").get(), is(user1));
  }
}
