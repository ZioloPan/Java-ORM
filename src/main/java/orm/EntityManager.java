package orm;

import orm.annotations.Column;
import orm.annotations.Id;
import orm.annotations.Table;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Klasa EntityManager zarządzająca operacjami CRUD na encjach.
 */
public class EntityManager {

    private final ConnectionPool connectionPool;

    public EntityManager(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    /**
     * Zapisuje nową encję w bazie danych.
     *
     * @param entity obiekt do zapisania
     */
    public <T> void save(T entity) {
        Class<?> clazz = entity.getClass();

        // Pobieranie informacji o tabeli
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("Klasa " + clazz.getName() + " nie jest oznaczona jako @Table");
        }

        String tableName = table.name();
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();

        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    columns.append(column.name()).append(",");
                    values.append("'").append(field.get(entity)).append("',");
                }
            }

            String query = String.format("INSERT INTO %s (%s) VALUES (%s)",
                    tableName,
                    columns.substring(0, columns.length() - 1), // Usunięcie ostatniego przecinka
                    values.substring(0, values.length() - 1));  // Usunięcie ostatniego przecinka

            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Błąd podczas zapisywania encji: " + e.getMessage());
        }
    }

    /**
     * Wyszukuje encję w bazie danych po identyfikatorze.
     *
     * @param clazz klasa encji
     * @param id    identyfikator
     * @param <T>   typ encji
     * @return encja lub null, jeśli nie znaleziono
     */
    public <T> T find(Class<T> clazz, Object id) {
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("Klasa " + clazz.getName() + " nie jest oznaczona jako @Table");
        }

        String tableName = table.name();
        String idColumn = null;

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                Column column = field.getAnnotation(Column.class);
                idColumn = column.name();
                break;
            }
        }

        if (idColumn == null) {
            throw new RuntimeException("Klasa " + clazz.getName() + " nie zawiera pola oznaczonego jako @Id");
        }

        String query = String.format("SELECT * FROM %s WHERE %s = ?", tableName, idColumn);

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setObject(1, id);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                T entity = clazz.getDeclaredConstructor().newInstance();

                for (Field field : clazz.getDeclaredFields()) {
                    Column column = field.getAnnotation(Column.class);
                    if (column != null) {
                        field.setAccessible(true);
                        field.set(entity, resultSet.getObject(column.name()));
                    }
                }

                return entity;
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Błąd podczas wyszukiwania encji: " + e.getMessage());
        }

        return null;
    }

    /**
     * Aktualizuje istniejącą encję w bazie danych.
     *
     * @param entity encja do aktualizacji
     */
    public <T> void update(T entity) {
        Class<?> clazz = entity.getClass();
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("Klasa " + clazz.getName() + " nie jest oznaczona jako @Table");
        }

        String tableName = table.name();
        StringBuilder setClause = new StringBuilder();
        String idColumn = null;
        Object idValue = null;

        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    if (field.isAnnotationPresent(Id.class)) {
                        idColumn = column.name();
                        idValue = field.get(entity);
                    } else {
                        setClause.append(column.name()).append(" = '").append(field.get(entity)).append("',");
                    }
                }
            }

            if (idColumn == null || idValue == null) {
                throw new RuntimeException("Encja " + clazz.getName() + " nie zawiera poprawnego klucza głównego");
            }

            String query = String.format("UPDATE %s SET %s WHERE %s = ?",
                    tableName,
                    setClause.substring(0, setClause.length() - 1), // Usunięcie ostatniego przecinka
                    idColumn);

            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setObject(1, idValue);
                statement.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Błąd podczas aktualizacji encji: " + e.getMessage());
        }
    }

    /**
     * Usuwa encję z bazy danych.
     *
     * @param entity encja do usunięcia
     */
    public <T> void delete(T entity) {
        Class<?> clazz = entity.getClass();
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("Klasa " + clazz.getName() + " nie jest oznaczona jako @Table");
        }

        String tableName = table.name();
        String idColumn = null;
        Object idValue = null;

        try {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Id.class)) {
                    Column column = field.getAnnotation(Column.class);
                    idColumn = column.name();
                    idValue = field.get(entity);
                    break;
                }
            }

            if (idColumn == null || idValue == null) {
                throw new RuntimeException("Encja " + clazz.getName() + " nie zawiera poprawnego klucza głównego");
            }

            String query = String.format("DELETE FROM %s WHERE %s = ?", tableName, idColumn);

            try (Connection connection = connectionPool.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setObject(1, idValue);
                statement.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Błąd podczas usuwania encji: " + e.getMessage());
        }
    }
}
