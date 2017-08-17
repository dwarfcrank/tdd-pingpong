package pingis.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import pingis.entities.sandbox.Submission;
import pingis.entities.sandbox.SubmissionStatus;
import pingis.entities.sandbox.TestOutput;
import pingis.repositories.sandbox.SubmissionRepository;
import pingis.services.sandbox.SubmissionResponse;

/**
 *
 * @author authority
 */
@Controller
public class FakeSandboxController {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private SubmissionRepository submissionRepository;

  @RequestMapping("/tasks.json")
  public ResponseEntity tasks(
        @RequestParam("token") String token) {

    logger.debug("TOKEN::::::" + token);

    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    HttpEntity<MultiValueMap<String, String>> request = buildResponseEntity(
          loadSubmission(token), headers);

    restTemplate.postForLocation("http://localhost:8080/submission-result", request, String.class);

    SubmissionResponse sr = new SubmissionResponse();
    sr.setStatus("ok");
    return ResponseEntity.status(HttpStatus.OK).body(sr);
  }

  //TODO: Actually load, don't generate
  private Submission loadSubmission(String token) {
    Submission submission = submissionRepository.findOne(UUID.fromString(token));
    submission.setId(UUID.fromString(token));
    submission.setStatus(SubmissionStatus.FINISHED);
    submission.setExitCode(0);
    submission.setStderr("");
    submission.setStdout("");
    try {
      submission.setTestOutput(new ObjectMapper().readValue(
            "{\"status\":\"PASSED\","
            + "\"testResults\":[],"
            + "\"logs\":{"
            + "\"stdout\":[0],"
            + "\"stderr\":[0]"
            + "}"
            + "}", TestOutput.class));
    } catch (IOException ex) {
      logger.debug("JSON serialization failed");
    }
    submission.setValidations("");
    submission.setVmLog("");

    return submission;
  }

  private HttpEntity<MultiValueMap<String, String>> buildResponseEntity(
        Submission submission, HttpHeaders headers) {
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();

    try {
      map.add("test_output", new ObjectMapper().writeValueAsString(
            submission.getTestOutput()));
    } catch (JsonProcessingException ex) {
      logger.debug("POJO deserialization failed");
    }

    map.add("stdout", submission.getStdout());
    map.add("stderr", submission.getStderr());
    map.add("validations", submission.getValidations());
    map.add("vm_log", submission.getVmLog());
    map.add("token", submission.getId().toString());
    map.add("status", submission.getStatus().toString());
    map.add("exit_code", submission.getExitCode().toString());

    return new HttpEntity<>(map, headers);
  }
}
