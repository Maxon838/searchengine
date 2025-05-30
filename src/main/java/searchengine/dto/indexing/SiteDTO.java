package searchengine.dto.indexing;

import lombok.Data;
import searchengine.model.Status;

import java.time.Instant;

@Data
public class SiteDTO {

    private int id;
    private Status status;
    private Instant statusTime;
    private String lastError;
    private String mainPageURL;
    private String name;

}
