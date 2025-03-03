package searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageDTO {

    private int id;
    private int siteId;
    private String pagePath;
    private int pageResponseHttpCode;
    private String pageContentHttpCode;

}
