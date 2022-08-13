package lesson3;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class SpoonacularTest extends AbstractTest{

    @BeforeAll
    static void setUp(){
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void getSearchRecipesWithoutAddedInformationAboutIngredients() {
        System.out.println(getBaseUrl());

        given()
                .queryParam("apiKey", getApiKey())
                .queryParam("query", "burger")
                .queryParam("number", "1")
                .queryParam("fillIngredients", "false")
                .response()
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/json")
                .expect()
                .body("results[0].title", equalTo("Falafel Burger"))
                .body("results[0]", not(hasKey("missedIngredients")))
                .when()
                .get(getBaseUrl()+"recipes/complexSearch")
                .then()
                .statusCode(200);
    }

    @Test
    void getSearchRecipesWithAddedInformationAboutIngredients() {
        JsonPath response = given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("query", "burger")
                .queryParam("number", "1")
                .queryParam("fillIngredients", "true")
                .when()
                .get("https://api.spoonacular.com/recipes/complexSearch")
                .then()
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/json")
                .statusCode(200)
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
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("query", "pasta")
                .queryParam("number", "1")
                .queryParam("minZinc", 10)
                .queryParam("maxZinc", 100)
                .response()
                .contentType(ContentType.JSON)
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
                .statusCode(200);
    }

    @Test
    void getSearchRecipesSortDirectionAscending() {
        JsonPath response = given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("query", "burger")
                .queryParam("number", "3")
                .queryParam("sort", "calories")
                .queryParam("sortDirection", "asc")
                .when()
                .get("https://api.spoonacular.com/recipes/complexSearch")
                .then()
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/json")
                .statusCode(200)
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
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("number", "1")
                .queryParam("cuisine", "Chinese")
                .queryParam("addRecipeInformation", true)
                .response()
                .contentType(ContentType.JSON)
                .header("Content-Type", "application/json")
                .expect()
                .body("results[0]", hasKey("cuisines"))
                .body("results[0].cuisines[0]", equalTo("Chinese"))
                .when()
                .get("https://api.spoonacular.com/recipes/complexSearch")
                .then()
                .statusCode(200);
    }

    @Test
    void postClassifyCuisineByTitle() {
        given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("language", "en")
                .formParam("title","Mango Fried Rice")
                .contentType("application/x-www-form-urlencoded")
                .when()
                .post("https://api.spoonacular.com/recipes/cuisine")
                .then()
                .header("Content-Type", "application/json")
//                .expect()
                .body("cuisine", equalTo("Chinese"))
                .body("cuisines[0]", equalTo("Chinese"))
                .body("cuisines[1]", equalTo("Asian"))
                .body("confidence", greaterThanOrEqualTo(0.1F))
                .body("confidence", equalTo(0.85F))
                .statusCode(200);
    }

    @Test
    void postCClassifyCuisineByIngredientsAndTitle() {
        given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("language", "en")
                .formParam("title","Mango Fried Rice")
                .formParam("ingredientList","cauliflower\n" +
                        "rice\n" + "couscous")
                .contentType("application/x-www-form-urlencoded")
                .when()
                .post("https://api.spoonacular.com/recipes/cuisine")
                .then()
                .header("Content-Type", "application/json")
                .body("cuisine", equalTo("Chinese"))
                .body("cuisines[0]", equalTo("Chinese"))
                .body("cuisines[1]", equalTo("Asian"))
                .body("confidence", greaterThanOrEqualTo(0.1F))
                .body("confidence", equalTo(0.85F))
                .statusCode(200);
    }

    @Test
    void postClassifyCuisineByIngredientsWithoutTitle() {
        given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("language", "en")
                .formParam("ingredientList","cauliflower\n" +
                        "rice\n" + "couscous")
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
                .statusCode(200);
    }

    @Test
    void postClassifyCuisineByTitleInDE() {
        given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("language", "de")
                .formParam("title","Mango gebratener Reis")
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
                .statusCode(200);
    }

    @Test
    void postСheckIfLanguageValueIsRU() {
        given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("language", "ru")
                .formParam("title","Mango Fried Rice")
                .contentType("application/x-www-form-urlencoded")
                .when()
                .post("https://api.spoonacular.com/recipes/cuisine")
                .then()
                .header("Content-Type", "text/html;charset=utf-8")
                .statusCode(500);
    }


    @Test
    void addMealPlanTest() {
        JsonPath response = given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .body("{\n" +
                        "    \"username\": \"your user's name\",\n" +
                        "    \"firstName\": \"your user's first name\",\n" +
                        "    \"lastName\": \"your user's last name\",\n" +
                        "    \"email\": \"your user's email\"\n" +
                        "}")
                .when()
                .post(getBaseUrl() + "users/connect")
                .then()
                .header("Content-Type", "application/json")
                .statusCode(200)
                .extract()
                .jsonPath();
var name = response.getString("username");
var hash = response.getString("hash");

        System.out.println(name);
        System.out.println(hash);

        given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("hash", hash)
                .pathParam("username",name)
                .pathParam("start-date","01.09.2022")
                .pathParam("end-date","07.09.2022")
                .when()
                .post(getBaseUrl() + "mealplanner/{username}/shopping-list/{start-date}/{end-date}")
                .then()
                .header("Content-Type", "application/json")
                .statusCode(200);

        var id = given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("hash", hash)
                .pathParam("username",name)
                .body("{\n" +
                        "\t\"item\": \"1 package baking powder\",\n" +
                        "\t\"aisle\": \"Baking\",\n" +
                        "\t\"parse\": true\n" +
                        "}")
                .when()
                .post(getBaseUrl() + "mealplanner/{username}/shopping-list/items")
                .then()
                .header("Content-Type", "application/json")
                .statusCode(200)
                .extract()
                .jsonPath()
//                .prettyPeek()
                .getInt("id");

       given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("hash", hash)
                .pathParam("username",name)
                .when()
                .get(getBaseUrl() + "mealplanner/{username}/shopping-list")
                .then()
//                .header("Content-Type", "application/json")
               .contentType(ContentType.JSON)
                .body("aisles[0].items[0].id",equalTo(id))
               .body("aisles[0].items[0].name",equalTo("baking powder"))
               .body("aisles[0].items[0].measures.original.amount",equalTo(1.0F))
               .body("aisles[0].items[0].measures.original.unit",equalTo("package"))
                .statusCode(200);

        given()
                .queryParam("apiKey", "4fd5337dc2894f22881490576092b2d4")
                .queryParam("hash", hash)
                .pathParam("username",name)
                .pathParam("id",id)
                .when()
                .delete(getBaseUrl() + "mealplanner/{username}/shopping-list/items/{id}")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("status",equalTo("success"))
                .extract().jsonPath().prettyPeek();

    }



}
