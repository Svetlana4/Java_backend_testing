package lesson4;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class SpoonacularTest extends AbstractTest {


    @BeforeAll
    static void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void getSearchRecipesWithoutAddedInformationAboutIngredients() {
        System.out.println(getBaseUrl());

        given()
                .spec(requestSpecification)
                .queryParam("query", "burger")
                .queryParam("number", "1")
                .queryParam("fillIngredients", "false")
                .response()
                .header("Content-Type", "application/json")
                .expect()
                .body("results[0].title", equalTo("Falafel Burger"))
                .body("results[0]", not(hasKey("missedIngredients")))
                .when()
                .get(getBaseUrl() + "recipes/complexSearch")
                .then()
                .spec(responseSpecification);
    }

    @Test
    void getSearchRecipesWithAddedInformationAboutIngredients() {
        JsonPath response = given()
                .spec(requestSpecification)
                .queryParam("query", "burger")
                .queryParam("number", "1")
                .queryParam("fillIngredients", "true")
                .when()
                .get("https://api.spoonacular.com/recipes/complexSearch")
                .then()
                .header("Content-Type", "application/json")
                .spec(responseSpecification)
                .body("results[0]", hasKey("missedIngredients"))
                .extract()
                .jsonPath();

        assertThat(response.get("results[0].missedIngredientCount"), equalTo(10));
        assertThat(response.get("results[0].usedIngredientCount"), equalTo(0));
        assertThat(response.get("results[0].title"), equalTo("Falafel Burger"));
        assertThat(response.get("results[0].missedIngredients[0].name"), equalTo("canned chickpeas"));
        assertThat(response.get("results[0].missedIngredients[0].aisle"), equalTo("Canned and Jarred"));
        assertThat(response.get("results[0].missedIngredients[0].amount"), equalTo(540.0F));
        assertThat(response.get("results[0].missedIngredients[0].unit"), equalTo("ml"));
        assertThat(response.get("results[0].missedIngredients[0].originalName"),
                equalTo("chickpeas, drained and rinsed"));
    }

    @Test
    void getSearchRecipesByZincAmount() {
        given()
                .spec(requestSpecification)
                .queryParam("query", "pasta")
                .queryParam("number", "1")
                .queryParam("minZinc", 10)
                .queryParam("maxZinc", 100)
                .response()
                .header("Content-Type", "application/json")
                .expect()
                .body("results[0].title", equalTo("Extra Large Homemade Meatballs w Pasta"))
                .body("results[0].nutrition", hasKey("nutrients"))
                .body("results[0].nutrition.nutrients[0].name", equalTo("Zinc"))
                .body("results[0].nutrition.nutrients[0].amount", equalTo(11.0972F))
                .body("results[0].nutrition.nutrients[0].amount", lessThanOrEqualTo(100F))
                .body("results[0].nutrition.nutrients[0].amount", greaterThanOrEqualTo(10F))
                .when()
                .get("https://api.spoonacular.com/recipes/complexSearch")
//                .prettyPeek()
                .then()
                .spec(responseSpecification);
    }

    @Test
    void getSearchRecipesSortDirectionAscending() {
        JsonPath response = given()
                .spec(requestSpecification)
                .queryParam("query", "burger")
                .queryParam("number", "3")
                .queryParam("sort", "calories")
                .queryParam("sortDirection", "asc")
                .when()
                .get("https://api.spoonacular.com/recipes/complexSearch")
                .then()
                .header("Content-Type", "application/json")
                .spec(responseSpecification)
                .extract()
                .jsonPath();

        assertThat(response.get("results[0].title"), equalTo("Lentil Veggie Burgers"));
        assertThat(response.get("results[1].title"), equalTo("Itty Bitty Burgers"));
        assertThat(response.get("results[2].title"), equalTo("Walnut Lentil Burgers with Tarragon"));
        assertThat(response.get("results[0].nutrition.nutrients[0].amount"), equalTo(83.2112F));
        assertThat(response.get("results[1].nutrition.nutrients[0].amount"), equalTo(88.3702F));
        assertThat(response.get("results[2].nutrition.nutrients[0].amount"), equalTo(120.371F));
        var results = response.getList("results");
        for (int i = 1; i < response.getInt("number"); i++) {
            var prevCalories = response.getFloat("results[" + (i - 1) + "].nutrition.nutrients[0].amount");
            var curCalories = response.getFloat("results[" + i + "].nutrition.nutrients[0].amount");
            assertThat(curCalories, greaterThanOrEqualTo(prevCalories));
        }
    }

    @Test
    void getSearchRecipesByCuisine() {
        given()
                .spec(requestSpecification)
                .queryParam("number", "1")
                .queryParam("cuisine", "Chinese")
                .queryParam("addRecipeInformation", true)
                .response()
                .header("Content-Type", "application/json")
                .expect()
                .body("results[0]", hasKey("cuisines"))
                .body("results[0].cuisines[0]", equalTo("Chinese"))
                .when()
                .get("https://api.spoonacular.com/recipes/complexSearch")
                .then()
                .spec(responseSpecification);
    }



    @Test
    void postClassifyCuisineByTitle() {
        given()
                .spec(enSpecification)
                .formParam("title", "Mango Fried Rice")
                .when()
                .post("https://api.spoonacular.com/recipes/cuisine")
                .then()
                .header("Content-Type", "application/json")
                .body("cuisine", equalTo("Chinese"))
                .body("cuisines[0]", equalTo("Chinese"))
                .body("cuisines[1]", equalTo("Asian"))
                .body("confidence", greaterThanOrEqualTo(0.1F))
                .body("confidence", equalTo(0.85F))
                .spec(responseSpecification);
    }


    @Test
    void postCClassifyCuisineByIngredientsAndTitle() {
        given()
                .spec(enSpecification)
                .formParam("title", "Mango Fried Rice")
                .formParam("ingredientList", "cauliflower\n" +
                        "rice\n" + "couscous")
                .when()
                .post("https://api.spoonacular.com/recipes/cuisine")
                .then()
                .header("Content-Type", "application/json")
                .body("cuisine", equalTo("Chinese"))
                .body("cuisines[0]", equalTo("Chinese"))
                .body("cuisines[1]", equalTo("Asian"))
                .body("confidence", greaterThanOrEqualTo(0.1F))
                .body("confidence", equalTo(0.85F))
                .spec(responseSpecification);
    }

    @Test
    void postClassifyCuisineByIngredientsWithoutTitle() {
        given()
                .spec(enSpecification)
                .formParam("ingredientList", "cauliflower\n" +
                        "rice\n" + "couscous")
                .when()
                .post("https://api.spoonacular.com/recipes/cuisine")
                .then()
                .header("Content-Type", "application/json")
                .body("cuisine", equalTo("Mediterranean"))
                .body("cuisines[0]", equalTo("Mediterranean"))
                .body("cuisines[1]", equalTo("European"))
                .body("cuisines[2]", equalTo("Italian"))
                .body("confidence", equalTo(0F))
                .spec(responseSpecification);
    }

    @Test
    void postClassifyCuisineByTitleInDE() {
        given()
                .spec(requestSpecification)
                .queryParam("language", "de")
                .formParam("title", "Mango gebratener Reis")
                .contentType("application/x-www-form-urlencoded")
                .when()
                .post("https://api.spoonacular.com/recipes/cuisine")
                .then()
                .header("Content-Type", "application/json")
                .body("cuisine", equalTo("Mediterranean"))
                .body("cuisines[0]", equalTo("Mediterranean"))
                .body("cuisines[1]", equalTo("European"))
                .body("cuisines[2]", equalTo("Italian"))
                .body("confidence", equalTo(0F))
                .spec(responseSpecification);
    }

    @Test
    void postСheckIfLanguageValueIsRU() {
        given()
                .spec(requestSpecification)
                .queryParam("language", "ru")
                .formParam("title", "Mango Fried Rice")
                .contentType("application/x-www-form-urlencoded")
                .when()
                .post("https://api.spoonacular.com/recipes/cuisine")
                .then()
                .header("Content-Type", "text/html;charset=utf-8")
                .statusCode(500);
    }

    @Test
    void addMealPlanTest() {
        CreateUserResponse response = given()
                .spec(requestSpecification)
                .body(new CreateUserRequest(
                        "your user's name", "your user's first name", "your user's last name", "your user's email"
                ))
                .when()
                .post(getBaseUrl() + "users/connect")
                .then()
                .spec(responseSpecification)
                .extract()
                .as(CreateUserResponse.class);
        var name = response.getName();
        var hash = response.getHash();

        System.out.println(name);
        System.out.println(hash);

        RequestSpecification hashSpecification = new RequestSpecBuilder()
                .addRequestSpecification(requestSpecification)
                .addQueryParam("hash", hash)
                .addPathParam("username", name)
                .build();

        given()
                .spec(hashSpecification)
                .pathParam("start-date", "01.09.2022")
                .pathParam("end-date", "07.09.2022")
                .when()
                .post(getBaseUrl() + "mealplanner/{username}/shopping-list/{start-date}/{end-date}")
                .then()
                .spec(responseSpecification);

        var id = given()
                .spec(requestSpecification)
                .spec(hashSpecification)
                .body(new AddItemRequest("1 package baking powder", "Baking", "true"))
                .when()
                .post(getBaseUrl() + "mealplanner/{username}/shopping-list/items")
                .then()
                .spec(responseSpecification)
                .extract()
                .as(CreateItemResponse.class)
                .getId();
        System.out.println(id);

        given()
                .spec(requestSpecification)
                .spec(hashSpecification)
                .when()
                .get(getBaseUrl() + "mealplanner/{username}/shopping-list")
                .then()
                .body("aisles[0].items[0].id", equalTo(id))
                .body("aisles[0].items[0].name", equalTo("baking powder"))
                .body("aisles[0].items[0].measures.original.amount", equalTo(1.0F))
                .body("aisles[0].items[0].measures.original.unit", equalTo("package"))
                .spec(responseSpecification);

        given()
                .spec(requestSpecification)
                .spec(hashSpecification)
                .pathParam("id", id)
                .when()
                .delete(getBaseUrl() + "mealplanner/{username}/shopping-list/items/{id}")
                .then()
                .spec(responseSpecification)
                .body("status", equalTo("success"))
                .extract().jsonPath().prettyPeek();
    }

}
