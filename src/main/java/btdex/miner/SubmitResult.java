
package btdex.miner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // prevent future incompatible values
public class SubmitResult {
  private String result;
  private long deadline;

  // pool response
  private long block;

  // one or the other is used
  private String deadlineString;
  private String deadlineText;

  private long targetDeadline;
  
  private long requestProcessingTime;

  public long getRequestProcessingTime()
  {
    return requestProcessingTime;
  }

  public String getResult()
  {
    return result;
  }

  public long getDeadline()
  {
    return deadline;
  }

  public long getBlock()
  {
    return block;
  }

  public String getDeadlineString()
  {
    return deadlineString;
  }

  public String getDeadlineText()
  {
    return deadlineText;
  }

  public long getTargetDeadline()
  {
    return targetDeadline;
  }
}
