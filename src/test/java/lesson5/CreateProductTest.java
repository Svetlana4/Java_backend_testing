package lesson5;

import com.github.javafaker.Faker;
import lesson5.api.ProductService;
import lesson5.dto.GetCategoryResponse;
import lesson5.dto.Product;
import lesson5.utils.RetrofitUtils;
import lombok.SneakyThrows;
import okhttp3.ResponseBody;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class CreateProductTest {

    static ProductService productService;
    Faker faker = new Faker();
    List<Integer> ids = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        productService = RetrofitUtils.getRetrofit()
                .create(ProductService.class);
    }

    @SneakyThrows
    @AfterEach
    void tearDown() {
        for (int id : ids) {
            productService.deleteProduct(id).execute();
        }
    }

    @Test
    void createProductInFoodCategoryTest() throws IOException {
        Product product = new Product()
                .withTitle(faker.food().ingredient())
                .withCategoryTitle("Food")
                .withPrice((int) (Math.random() * 10000));

        Response<Product> response = productService.createProduct(product).execute();
        ids.add(response.body().getId());
        assertThat(response.isSuccessful(), CoreMatchers.is(true));
    }

    @Test
    void deleteProductTest() throws IOException {
        Product product = createRandomProduct();

        Response<ResponseBody> response = productService.deleteProduct(product.getId()).execute();
        assertThat(response.isSuccessful(), CoreMatchers.is(true));

    }

    @Test
    void getProductsTest() throws IOException {
        Product product1 = createRandomProduct();
        Product product2 = createRandomProduct();
        Response<List<Product>> response = productService.getProducts().execute();

        assertThat(response.body(), hasItems(product1, product2));
        assertThat(response.isSuccessful(), CoreMatchers.is(true));
    }
    @Test
    void getProductByIdTest() throws IOException {
        Product product = createRandomProduct();

        Response<Product> response = productService.getProductById(product.getId()).execute();

        assertThat(response.body(), equalTo(product));
        assertThat(response.isSuccessful(), CoreMatchers.is(true));

    }

    @Test
    void modifyProductTest() throws IOException {
        Product product = createRandomProduct();
        Product modifyProductRequest = new Product(product.getId(), "TV", 15, "Electronic");
        Response<Product> response = productService.modifyProduct(modifyProductRequest).execute();

        assertThat(response.body(), equalTo(modifyProductRequest));
        assertThat(response.isSuccessful(), CoreMatchers.is(true));
    }

    private Product createRandomProduct() throws IOException {
        Product product = new Product()
                .withTitle(faker.food().ingredient())
                .withCategoryTitle("Food")
                .withPrice((int) (Math.random() * 10000));

        Response<Product> response = productService.createProduct(product).execute();
        ids.add(response.body().getId());

        return response.body();
    }
}
