package lesson6;

import com.github.javafaker.Faker;
import db.model.Categories;
import db.model.Products;
import lesson5.api.ProductService;
import lesson5.dto.Product;
import lesson5.utils.RetrofitUtils;
import lombok.SneakyThrows;
import okhttp3.ResponseBody;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import retrofit2.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CreateProductTest {

    static ProductService productService;
    Faker faker = new Faker();
    List<Integer> ids = new ArrayList<>();
    static SqlSession session = null;
    static db.dao.ProductsMapper productsMapper;
    static db.dao.CategoriesMapper categoriesMapper;

    @BeforeAll
    static void beforeAll() {
        productService = RetrofitUtils.getRetrofit()
                .create(ProductService.class);

        String resource = "mybatis-config.xml";
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        session = sqlSessionFactory.openSession();

        productsMapper = session.getMapper(db.dao.ProductsMapper.class);
        categoriesMapper = session.getMapper(db.dao.CategoriesMapper.class);
    }

    @SneakyThrows
    @AfterEach
    void tearDown() {
        for (int id : ids) {
            productsMapper.deleteByPrimaryKey((long) id);
        }
    }

    @AfterAll
    static void close() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    void createProductTest() throws IOException {
        Product product = new Product()
                .withTitle(faker.food().ingredient())
                .withCategoryTitle("Food")
                .withPrice((int) (Math.random() * 10000));

        Response<Product> response = productService.createProduct(product).execute();
        ids.add(response.body().getId());
        assertThat(response.isSuccessful(), CoreMatchers.is(true));

        db.model.ProductsExample example = new db.model.ProductsExample();
        example.createCriteria().andIdEqualTo(response.body().getId().longValue());

        long count = productsMapper.countByExample(example);
        assertThat(count, equalTo(1));
    }

    @Test
    void deleteProductTest() throws IOException {
        Product product = createRandomProduct();

        Response<ResponseBody> response = productService.deleteProduct(product.getId()).execute();
        assertThat(response.isSuccessful(), CoreMatchers.is(true));

        db.model.ProductsExample example = new db.model.ProductsExample();
        example.createCriteria().andIdEqualTo(product.getId().longValue());

        List<db.model.Products> list = productsMapper.selectByExample(example);
        assertThat(list.size(),equalTo(0));
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

        Products dbProduct = productsMapper.selectByPrimaryKey(product.getId().longValue());
        Categories dbCategory = categoriesMapper.selectByPrimaryKey(dbProduct.getCategory_id());

        assertThat(dbProduct.getId(), equalTo(modifyProductRequest.getId().longValue()));
        assertThat(dbProduct.getTitle(), equalTo(modifyProductRequest.getTitle()));
        assertThat(dbProduct.getPrice(), equalTo(modifyProductRequest.getPrice()));
        assertThat(dbCategory.getTitle(), equalTo(modifyProductRequest.getCategoryTitle()));
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
