package lesson4;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateItemResponse {

    @JsonProperty("id")
    private int id;

    public CreateItemResponse(int id) {
        this.id=id;
    }

    public CreateItemResponse() {
    }
}
