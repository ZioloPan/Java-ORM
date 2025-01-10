package orm;

import java.io.InputStream;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

import static java.lang.Integer.parseInt;

public class Config {
    private static Config instance; // Statyczne pole przechowujące jedyną instancję klasy

    private String url;
    private String user;
    private String password;
    private int poolSize;

    // Prywatny konstruktor, aby uniemożliwić tworzenie instancji z zewnątrz
    private Config() {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("Properties.yml");
        if (inputStream == null) {
            throw new RuntimeException("Nie znaleziono pliku Properties.yml");
        }

        Map<String, Object> obj = yaml.load(inputStream);

        this.url = obj.get("url").toString();
        this.user = obj.get("user").toString();
        this.password = obj.get("password").toString();
        this.poolSize = parseInt(obj.get("poolSize").toString());
    }

    // Publiczna metoda zwracająca jedyną instancję klasy
    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config(); // Tworzymy instancję tylko raz
        }
        return instance;
    }

    // Gettery
    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public int getPoolSize() {
        return poolSize;
    }
}
