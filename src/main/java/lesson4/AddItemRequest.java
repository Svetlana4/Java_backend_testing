package lesson4;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class AddItemRequest {

    @JsonProperty("item")
    private String item;

    @JsonProperty("aisle")
    private String aisle;

    @JsonProperty("parse")
    private String parse;

    public AddItemRequest(String item, String aisle, String parse) {
        this.item = item;
        this.aisle = aisle;
        this.parse = parse;
    }
    public AddItemRequest() {
    }

}


