package searchengine.dto.searching;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchingResponse {
    private boolean result;
    private String error;
    private Integer count;
    private List<Response> data;

    public SearchingResponse setNoResults () {
        this.setResult(true);
        this.setCount(0);
        this.setData(new ArrayList<>());
        this.setError("Поисковый запрос не дал результатов");

        return this;
    }
}
