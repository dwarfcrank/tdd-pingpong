package pingis.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import pingis.entities.Challenge;
import pingis.entities.ChallengeType;
import pingis.entities.Task;
import pingis.entities.TaskInstance;
import pingis.entities.TaskPair;
import pingis.entities.User;
import pingis.services.ChallengeService;
import pingis.services.GameplayService;
import pingis.services.GameplayService.TurnType;
import pingis.services.TaskInstanceService;
import pingis.services.TaskService;
import pingis.services.UserService;

/**
 * @author authority
 */
@RunWith(SpringRunner.class)
@WebMvcTest(ChallengeController.class)
public class ChallengeControllerTest {

  @Autowired
  MockMvc mvc;

  @MockBean
  ChallengeService challengeService;
  @MockBean
  TaskService taskService;
  @MockBean
  TaskInstanceService taskInstanceService;
  @MockBean
  UserService userService;
  @MockBean
  GameplayService gameplayService;

  @Test
  @WithMockUser
  public void newChallengeReturnsOk() throws Exception {
    mvc.perform(get("/newchallenge")
        .flashAttr("challenge", Mockito.mock(Challenge.class)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  public void cantGetCreateChallenge() throws Exception {
    mvc.perform(get("/createChallenge"))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  @WithMockUser
  public void creatingChallengeRedirectsToNewTaskPair()
      throws Exception {
    Long challengeId = 123L;

    User user = Mockito.mock(User.class);
    when(user.getId()).thenReturn(10L);
    when(userService.getCurrentUser()).thenReturn(user);

    Challenge challenge = Mockito.mock(Challenge.class);
    when(challengeService.save(any())).thenReturn(challenge);
    when(challenge.toString()).thenReturn("");
    when(challenge.getId()).thenReturn(challengeId);

    Challenge challengeFromForm = new Challenge("validName", user, "validDesc");

    mvc.perform(post("/createChallenge")
        .with(csrf())
        .flashAttr("challenge", challengeFromForm))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/playChallenge/" + challengeId));

    verify(challengeService, times(1))
        .save(any());
  }

  @Test
  @WithMockUser
  public void newTaskpairReturnsNewTaskPairView() throws Exception {
    when(challengeService.findOne(any())).thenReturn(Mockito.mock(Challenge.class));
    mvc.perform(get("/newtaskpair/0"))
        .andExpect(status().isOk())
        .andExpect(view().name("newtaskpair"));
  }

  @Test
  @WithMockUser
  public void createArcadeTaskPairRedirectsToPlayChallenge() throws Exception {
    Long challengeId = 345L;
    Long taskId = 567L;
    Long taskInstanceId = 123L;
    Challenge challenge = Mockito.mock(Challenge.class);
    when(challenge.getName()).thenReturn("haaste");
    when(challenge.getType()).thenReturn(ChallengeType.ARCADE);
    TaskInstance taskInstance = Mockito.mock(TaskInstance.class);
    when(taskInstance.getId()).thenReturn(taskInstanceId);
    when(challengeService.findOne(any()))
        .thenReturn(challenge);

    TaskPair taskPairFromForm = new TaskPair("validClassName",
        "validName", "validName", "validDesc",
        "validDesc");
    mvc.perform(post("/createTaskPair")
        .with(csrf())
        .flashAttr("taskPair", taskPairFromForm))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/playChallenge/" + challenge.getId()));
  }

  @Test
  @WithMockUser
  public void cantGetCreateTaskpair() throws Exception {
    mvc.perform(get("/createChallenge"))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  @WithMockUser
  public void playOpenChallengeWithNonPlayerUserRedirectsToError() throws Exception {
    User user = Mockito.mock(User.class);

    Challenge challenge = Mockito.mock(Challenge.class);
    when(challenge.getIsOpen()).thenReturn(true);
    when(challenge.getSecondPlayer()).thenReturn(user);

    when(challengeService.findOne(any())).thenReturn(challenge);

    when(gameplayService.isParticipating(any())).thenReturn(false);

    mvc.perform(get("/playChallenge/0")
        .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/error"));

    verify(challengeService, never())
        .save(any());
  }

  @Test
  @WithMockUser
  public void playOpenChallengeWithUnfinishedTaskInstanceOwnedByCurrentUserRedirectsToTask()
      throws Exception {
    Long userId = 723L;
    Long taskInstanceId = 992L;
    Long challengeId = 123L;

    User user = Mockito.mock(User.class);
    when(user.getId()).thenReturn(userId);

    TaskInstance taskInstance = Mockito.mock(TaskInstance.class);
    when(taskInstance.getId()).thenReturn(992L);
    when(taskInstance.getUser()).thenReturn(user);

    Challenge challenge = Mockito.mock(Challenge.class);
    when(challengeService.findOne(challengeId)).thenReturn(challenge);
    when(userService.getCurrentUser()).thenReturn(user);
    when(taskInstanceService.getUnfinishedInstanceInChallenge(challenge, user)).thenReturn(
                                                                                taskInstance);

    mvc.perform(get("/playChallenge/" + challengeId)
        .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/task/" + taskInstanceId));
  }

  @Test
  @WithMockUser
  public void playOpenChallengeWithUnfinishedTaskInstanceNotOwnedByCurrentUserRedirectsToUser()
      throws Exception {
    User user = Mockito.mock(User.class);
    when(user.getId()).thenReturn(10L);

    TaskInstance taskInstance = Mockito.mock(TaskInstance.class);
    when(taskInstance.getUser()).thenReturn(user);

    Challenge challenge = Mockito.mock(Challenge.class);
    when(challenge.getIsOpen()).thenReturn(true);
    when(challenge.getSecondPlayer()).thenReturn(null);

    when(challengeService.findOne(any())).thenReturn(challenge);
    when(gameplayService.isParticipating(any())).thenReturn(false);

    when(challengeService.getUnfinishedTaskInstance(any())).thenReturn(taskInstance);
    when(userService.getCurrentUser()).thenReturn(null);

    mvc.perform(get("/playChallenge/0")
        .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/user"));

    verify(challengeService, times(1))
        .save(challenge);
    verify(gameplayService, never())
        .getTurnType(any());
  }

  @Test
  @WithMockUser
  public void playOpenChallengeNotOnTheUsersTurnRedirectsToUser() throws Exception {
    User user = Mockito.mock(User.class);
    when(user.getId()).thenReturn(10L);

    TaskInstance taskInstance = Mockito.mock(TaskInstance.class);
    when(taskInstance.getUser()).thenReturn(user);

    Challenge challenge = Mockito.mock(Challenge.class);
    when(challenge.getIsOpen()).thenReturn(true);
    when(challenge.getSecondPlayer()).thenReturn(null);

    when(challengeService.findOne(any())).thenReturn(challenge);
    when(gameplayService.isParticipating(any())).thenReturn(false);
    when(challengeService.getUnfinishedTaskInstance(any())).thenReturn(null);

    when(gameplayService.getTurnType(any())).thenReturn(TurnType.NONE);

    mvc.perform(get("/playChallenge/0")
        .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/user"));

    verify(gameplayService, times(1))
        .getTurnType(any());
  }

  @Test
  @WithMockUser
  public void playOpenChallengeOnImplementationTurnRedirectsToTask() throws Exception {
    Long testTaskId = 444L;
    Long implTaskId = 333L;
    Long taskInstanceId = 313L;
    Long newTaskInstanceId = 131L;
    Long challengeId = 123L;
    Long userId = 321L;

    User user = Mockito.mock(User.class);
    when(user.getId()).thenReturn(userId);

    TaskInstance taskInstance = Mockito.mock(TaskInstance.class);
    when(taskInstance.getId()).thenReturn(taskInstanceId);
    TaskInstance newTaskInstance = Mockito.mock(TaskInstance.class);
    when(newTaskInstance.getId()).thenReturn(newTaskInstanceId);

    Challenge challenge = Mockito.mock(Challenge.class);
    when(challenge.getId()).thenReturn(challengeId);
    when(challenge.getIsOpen()).thenReturn(true);

    Task testTask = Mockito.mock(Task.class);
    when(testTask.getId()).thenReturn(testTaskId);
    when(testTask.getAuthor()).thenReturn(user);
    Task implTask = Mockito.mock(Task.class);
    when(implTask.getId()).thenReturn(implTaskId);

    when(challengeService.findOne(any())).thenReturn(challenge);
    when(challenge.getType()).thenReturn(ChallengeType.PROJECT);
    when(gameplayService.getNumberOfTasks(challenge)).thenReturn(2);
    when(gameplayService.isParticipating(challenge)).thenReturn(true);
    when(challengeService.getUnfinishedTaskInstance(challenge)).thenReturn(null);
    when(gameplayService.getTurnType(challenge)).thenReturn(TurnType.IMPLEMENTATION);
    when(gameplayService.getTopmostImplementationTask(challenge)).thenReturn(implTask);
    when(gameplayService.getTopmostTestTask(challenge)).thenReturn(testTask);
    when(taskInstanceService.getByTaskAndUser(testTask, user)).thenReturn(taskInstance);
    when(userService.getCurrentUser()).thenReturn(user);
    when(taskInstanceService.createEmpty(user, implTask)).thenReturn(newTaskInstance);
    when(newTaskInstance.getChallenge()).thenReturn(challenge);

    mvc.perform(get("/playChallenge/0")
        .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/task/" + newTaskInstanceId));
  }

  @Test
  @WithMockUser
  public void playTurnOnTestTurnRedirectsToNewTaskInstance() throws Exception {
    Long taskId = 0L;
    Long challengeId = 877L;

    User user = Mockito.mock(User.class);
    when(user.getId()).thenReturn(0L);

    TaskInstance taskInstance = Mockito.mock(TaskInstance.class);
    when(taskInstance.getUser()).thenReturn(user);

    Challenge challenge = Mockito.mock(Challenge.class);
    when(challenge.getId()).thenReturn(challengeId);
    when(challenge.getIsOpen()).thenReturn(true);
    when(challenge.getSecondPlayer()).thenReturn(null);

    Task task = Mockito.mock(Task.class);
    when(task.toString()).thenReturn("");
    when(task.getId()).thenReturn(taskId);
    when(task.getAuthor()).thenReturn(user);

    when(challengeService.findOne(any())).thenReturn(challenge);
    when(gameplayService.isParticipating(any())).thenReturn(false);
    when(challengeService.getUnfinishedTaskInstance(any())).thenReturn(null);
    when(gameplayService.getTurnType(any())).thenReturn(TurnType.TEST);

    when(gameplayService.getTopmostImplementationTask(any())).thenReturn(task);
    when(gameplayService.getTopmostTestTask(any())).thenReturn(task);
    when(taskInstanceService.getByTaskAndUser(any(), any())).thenReturn(taskInstance);

    mvc.perform(get("/playChallenge/" + challengeId)
        .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/newtaskpair/" + challengeId));
  }

  @Test
  @WithMockUser
  public void closingChallengeNotOwnedByCurrentUserRedirectsToError() throws Exception {
    when(gameplayService.isParticipating(any())).thenReturn(false);

    mvc.perform(post("/closeChallenge/0")
        .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/error"));

    verify(challengeService, never())
        .save(any());
  }

  @Test
  @WithMockUser
  public void closingChallengeOwnedByCurrentUserRedirectsToUser() throws Exception {
    Challenge challenge = Mockito.mock(Challenge.class);
    when(challenge.getId()).thenReturn(20L);

    when(challengeService.findOne(any())).thenReturn(challenge);
    when(gameplayService.isParticipating(any())).thenReturn(true);

    mvc.perform(post("/closeChallenge/0")
        .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/user"));

    verify(challengeService, times(1))
        .save(challenge);
  }

}
