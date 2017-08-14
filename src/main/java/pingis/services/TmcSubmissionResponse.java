package pingis.services;

public class TmcSubmissionResponse {

  public static final String OK = "ok";
  public static final String BAD_REQUEST = "bad_request";

  private String status;

  public TmcSubmissionResponse() {
  }

  public String getStatus() {
    return status;
  }

  private void checkStatus(String status) {
    if (!status.equals(OK) && !status.equals(BAD_REQUEST)) {
      throw new IllegalArgumentException("Invalid status.");
    }
  }

  public void setStatus(String status) {
    checkStatus(status);

    this.status = status;
  }
}
